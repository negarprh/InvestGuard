package com.example.riskanalyzer.service;

import com.example.riskanalyzer.config.MarketDataProviderProperties;
import com.example.riskanalyzer.config.RiskConfigProperties;
import com.example.riskanalyzer.dto.BacktestPoint;
import com.example.riskanalyzer.dto.BacktestRequest;
import com.example.riskanalyzer.dto.BacktestResult;
import com.example.riskanalyzer.dto.PortfolioSummary;
import com.example.riskanalyzer.dto.RiskSummary;
import com.example.riskanalyzer.model.Investment;
import com.example.riskanalyzer.model.MarketPriceSnapshot;
import com.example.riskanalyzer.repository.InvestmentRepository;
import com.example.riskanalyzer.repository.MarketPriceSnapshotRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

@Service
public class RiskService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RiskService.class);
    private static final long RETRY_BACKOFF_MILLIS = 450;
    private static final int MAX_BACKTEST_START_GAP_DAYS = 10;

    private final InvestmentRepository investmentRepository;
    private final MarketPriceSnapshotRepository marketPriceSnapshotRepository;
    private final RiskConfigProperties riskConfig;
    private final MarketDataProviderProperties providerConfig;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final ConcurrentMap<String, CacheEntry<MarketData>> marketDataCache = new ConcurrentHashMap<>();
    private volatile Boolean candleEndpointAccessible = null;

    public RiskService(InvestmentRepository investmentRepository,
                       MarketPriceSnapshotRepository marketPriceSnapshotRepository,
                       RiskConfigProperties riskConfig,
                       MarketDataProviderProperties providerConfig,
                       ObjectMapper objectMapper) {
        this.investmentRepository = investmentRepository;
        this.marketPriceSnapshotRepository = marketPriceSnapshotRepository;
        this.riskConfig = riskConfig;
        this.providerConfig = providerConfig;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(Math.max(5, providerConfig.getHttpTimeoutSeconds())))
                .build();
    }

    public Investment fetchStockData(String ticker, double amount) throws IOException {
        String normalisedTicker = normaliseTicker(ticker);
        validateAmount(amount);
        Optional<Investment> existing = investmentRepository.findByTickerIgnoreCase(normalisedTicker);

        MarketData marketData;
        try {
            marketData = fetchMarketData(normalisedTicker);
        } catch (IOException ioException) {
            if (existing.isPresent() && existing.get().getPastReturns() != null && !existing.get().getPastReturns().isEmpty()) {
                LOGGER.warn("Provider unavailable for {}. Reusing stored return history for risk calculations.", normalisedTicker, ioException);
                Investment fallback = existing.get();
                fallback.setAmount(amount);
                RiskMetrics fallbackMetrics = computeRiskMetrics(fallback.getPastReturns(), null, null, null, amount);
                applyMetrics(fallback, fallbackMetrics);
                return investmentRepository.save(fallback);
            }
            throw ioException;
        }

        Map<LocalDate, Double> benchmarkReturns = fetchBenchmarkReturns();
        RiskMetrics metrics = computeRiskMetrics(
                marketData.dailyReturns(),
                marketData.closingPrices(),
                marketData.dailyReturnsByDate(),
                benchmarkReturns,
                amount
        );

        Investment investment = existing.orElseGet(Investment::new);
        investment.setTicker(normalisedTicker);
        investment.setAmount(amount);
        investment.setPastReturns(marketData.dailyReturns());
        applyMetrics(investment, metrics);
        applyQuoteSnapshot(investment, marketData.quoteSnapshot());
        Investment saved = investmentRepository.save(investment);
        LOGGER.info("Stored live analytics for {} with {} return points", normalisedTicker, marketData.dailyReturns().size());
        return saved;
    }

    public List<RiskSummary> refreshRiskWithLiveData() {
        List<Investment> investments = investmentRepository.findAllByOrderByTickerAsc();
        if (investments.isEmpty()) {
            return List.of();
        }

        Map<LocalDate, Double> benchmarkReturns = fetchBenchmarkReturns();
        List<Investment> updated = new ArrayList<>();

        for (Investment investment : investments) {
            try {
                MarketData marketData = fetchMarketData(investment.getTicker());
                RiskMetrics metrics = computeRiskMetrics(
                        marketData.dailyReturns(),
                        marketData.closingPrices(),
                        marketData.dailyReturnsByDate(),
                        benchmarkReturns,
                        investment.getAmount()
                );
                investment.setPastReturns(marketData.dailyReturns());
                applyMetrics(investment, metrics);
                applyQuoteSnapshot(investment, marketData.quoteSnapshot());
                updated.add(investmentRepository.save(investment));
            } catch (IOException ioException) {
                LOGGER.warn("Unable to refresh live data for {}. Keeping previous snapshot.", investment.getTicker(), ioException);
                updated.add(investment);
            }
        }

        return updated.stream()
                .sorted(Comparator.comparing(Investment::getTicker))
                .map(this::toRiskSummary)
                .collect(Collectors.toList());
    }

    public Investment saveInvestment(Investment investment) {
        String normalisedTicker = normaliseTicker(investment.getTicker());
        validateAmount(investment.getAmount());
        if (investment.getPastReturns() == null || investment.getPastReturns().isEmpty()) {
            throw new IllegalArgumentException("Past returns are required to compute risk metrics.");
        }

        RiskMetrics metrics = computeRiskMetrics(investment.getPastReturns(), null, null, null, investment.getAmount());
        investment.setTicker(normalisedTicker);
        applyMetrics(investment, metrics);
        return investmentRepository.save(investment);
    }

    public List<Investment> getAllInvestments() {
        return investmentRepository.findAllByOrderByTickerAsc();
    }

    public List<RiskSummary> calculateRisk() {
        return investmentRepository.findAllByOrderByTickerAsc().stream()
                .filter(inv -> inv.getPastReturns() != null && !inv.getPastReturns().isEmpty())
                .map(this::toRiskSummary)
                .collect(Collectors.toList());
    }

    public PortfolioSummary calculatePortfolioSummary() {
        return buildPortfolioSummary(calculateRisk());
    }

    public PortfolioSummary calculatePortfolioSummary(List<RiskSummary> riskSummaries) {
        return buildPortfolioSummary(riskSummaries);
    }

    public String benchmarkTicker() {
        return normaliseTicker(riskConfig.getBenchmarkTicker());
    }

    public RiskSummary toRiskSummary(Investment investment) {
        Instant updatedAt = investment.getLastUpdated() != null ? investment.getLastUpdated() : Instant.now();
        return new RiskSummary(
                investment.getTicker(),
                investment.getAmount(),
                investment.getAverageReturn(),
                investment.getVolatility(),
                investment.getSharpeRatio(),
                investment.getValueAtRisk(),
                investment.getMaxDrawdown(),
                investment.getExpectedShortfall(),
                investment.getBetaVsBenchmark(),
                investment.getRiskLevel(),
                investment.getCurrentPrice(),
                investment.getDayChange(),
                investment.getDayChangePercent(),
                investment.getDayHigh(),
                investment.getDayLow(),
                investment.getVolume(),
                investment.getMarketCap(),
                investment.getQuoteCurrency(),
                updatedAt
        );
    }

    public BacktestResult backtestPortfolio(BacktestRequest request) throws IOException {
        if (request == null) {
            throw new IllegalArgumentException("Backtest request is required.");
        }

        String ticker = normaliseTicker(request.getTicker());
        validateAmount(request.getAmount());
        LocalDate startDate = parseBacktestDate(request.getStartDate(), "startDate");
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        LocalDate endDate = hasText(request.getEndDate()) ? parseBacktestDate(request.getEndDate(), "endDate") : today;

        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("startDate must be on or before endDate.");
        }
        if (endDate.isAfter(today)) {
            throw new IllegalArgumentException("endDate cannot be in the future.");
        }

        HistoricalSeries series = loadHistoricalSeriesForRange(ticker, startDate, endDate, true);
        List<PricePoint> points = series.points();
        if (points.size() < 2) {
            throw new IllegalArgumentException("Backtest requires at least 2 daily prices in the selected range.");
        }

        LocalDate actualStartDate = points.get(0).date();
        LocalDate actualEndDate = points.get(points.size() - 1).date();
        boolean strictCoverageRequired = !"LOCAL_SNAPSHOTS".equalsIgnoreCase(series.source());
        if (strictCoverageRequired && actualStartDate.isAfter(startDate.plusDays(MAX_BACKTEST_START_GAP_DAYS))) {
            throw new IllegalArgumentException(
                    "Performance Replay cannot run for requested start " + startDate
                            + ". Earliest available price in this source is "
                            + actualStartDate + " (" + series.source() + ")."
            );
        }
        double startPrice = points.get(0).price();
        double endPrice = points.get(points.size() - 1).price();
        if (startPrice <= 0) {
            throw new IllegalArgumentException("Invalid start price for backtest.");
        }

        double shares = request.getAmount() / startPrice;
        List<BacktestPoint> history = new ArrayList<>();
        List<Double> portfolioValues = new ArrayList<>();
        double peakValue = Double.NEGATIVE_INFINITY;

        for (PricePoint point : points) {
            double value = shares * point.price();
            peakValue = Math.max(peakValue, value);
            double drawdown = peakValue > 0 ? (value - peakValue) / peakValue : 0;
            history.add(new BacktestPoint(point.date(), point.price(), value, drawdown));
            portfolioValues.add(value);
        }

        double currentValue = portfolioValues.get(portfolioValues.size() - 1);
        double totalReturn = request.getAmount() > 0 ? (currentValue / request.getAmount()) - 1 : 0;

        double days = Math.max(1, ChronoUnit.DAYS.between(actualStartDate, actualEndDate));
        double years = days / 365.25;
        double cagr;
        if (years <= 0 || request.getAmount() <= 0 || currentValue <= 0) {
            cagr = 0;
        } else {
            cagr = Math.pow(currentValue / request.getAmount(), 1.0 / years) - 1;
        }

        List<Double> dailyReturns = dailyReturnsFromPricePoints(points);
        double annualizedVolatility = 0;
        double annualizedReturn = 0;
        double sharpeRatio = 0;
        double valueAtRisk = 0;
        double expectedShortfall = 0;
        if (!dailyReturns.isEmpty()) {
            int tradingDays = Math.max(riskConfig.getTradingDaysPerYear(), 1);
            double meanDailyReturn = dailyReturns.stream().mapToDouble(Double::doubleValue).average().orElse(0);
            annualizedReturn = Math.pow(1 + meanDailyReturn, tradingDays) - 1;
            annualizedVolatility = calculateStandardDeviation(dailyReturns) * Math.sqrt(tradingDays);
            sharpeRatio = annualizedVolatility > 0 ? (annualizedReturn - riskConfig.getRiskFreeRate()) / annualizedVolatility : 0;
            valueAtRisk = computeValueAtRisk(dailyReturns, request.getAmount());
            expectedShortfall = computeExpectedShortfall(dailyReturns, request.getAmount());
        }

        double maxDrawdown = computeMaxDrawdown(portfolioValues);
        Map<LocalDate, Double> stockReturnsByDate = dailyReturnsByDateFromPricePoints(points);
        String benchmark = benchmarkTicker();
        double beta = 0;
        try {
            HistoricalSeries benchmarkSeries = loadHistoricalSeriesForRange(benchmark, actualStartDate, actualEndDate, false);
            Map<LocalDate, Double> benchmarkReturns = dailyReturnsByDateFromPricePoints(benchmarkSeries.points());
            beta = computeBeta(stockReturnsByDate, benchmarkReturns);
        } catch (Exception exception) {
            LOGGER.warn("Benchmark data unavailable for backtest {}. Beta will be 0.", benchmark, exception);
        }

        return new BacktestResult(
                ticker,
                actualStartDate,
                actualEndDate,
                request.getAmount(),
                shares,
                startPrice,
                endPrice,
                currentValue,
                totalReturn,
                cagr,
                annualizedVolatility,
                sharpeRatio,
                valueAtRisk,
                expectedShortfall,
                maxDrawdown,
                beta,
                benchmark,
                series.source(),
                history
        );
    }

    private PortfolioSummary buildPortfolioSummary(List<RiskSummary> summaries) {
        if (summaries == null || summaries.isEmpty()) {
            return new PortfolioSummary(0, 0, 0, 0, 0, "Unrated", 0, null, null, benchmarkTicker(), Instant.now());
        }

        double totalExposure = summaries.stream().mapToDouble(RiskSummary::getAmount).sum();
        double weightedReturn = weightedAverage(summaries, totalExposure, RiskSummary::getAverageReturn);
        double weightedVolatility = weightedAverage(summaries, totalExposure, RiskSummary::getVolatility);
        double weightedSharpe = weightedAverage(summaries, totalExposure, RiskSummary::getSharpeRatio);
        double totalVaR = summaries.stream().mapToDouble(RiskSummary::getValueAtRisk).sum();
        double worstDrawdown = summaries.stream().mapToDouble(RiskSummary::getMaxDrawdown).max().orElse(0);
        String riskLevel = classifyRisk(weightedVolatility, worstDrawdown, weightedSharpe);

        RiskSummary topMover = summaries.stream()
                .filter(item -> item.getDayChangePercent() != null)
                .max(Comparator.comparingDouble(item -> Math.abs(item.getDayChangePercent())))
                .orElse(null);

        Instant latestUpdate = summaries.stream()
                .map(RiskSummary::getLastUpdated)
                .filter(instant -> instant != null)
                .max(Comparator.naturalOrder())
                .orElse(Instant.now());

        return new PortfolioSummary(
                totalExposure,
                weightedReturn,
                weightedVolatility,
                totalVaR,
                worstDrawdown,
                riskLevel,
                summaries.size(),
                topMover != null ? topMover.getTicker() : null,
                topMover != null ? topMover.getDayChangePercent() : null,
                benchmarkTicker(),
                latestUpdate
        );
    }

    private double weightedAverage(List<RiskSummary> summaries, double totalExposure, MetricExtractor extractor) {
        if (totalExposure <= 0) {
            return summaries.stream().mapToDouble(extractor::extract).average().orElse(0);
        }
        double total = 0;
        for (RiskSummary summary : summaries) {
            total += extractor.extract(summary) * summary.getAmount();
        }
        return total / totalExposure;
    }

    private LocalDate parseBacktestDate(String rawValue, String fieldName) {
        if (!hasText(rawValue)) {
            throw new IllegalArgumentException(fieldName + " is required. Use format YYYY-MM-DD.");
        }
        try {
            return LocalDate.parse(rawValue.trim());
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException(fieldName + " must use format YYYY-MM-DD.");
        }
    }

    private HistoricalSeries loadHistoricalSeriesForRange(String ticker,
                                                          LocalDate startDate,
                                                          LocalDate endDate,
                                                          boolean allowQuoteSeeding) throws IOException {
        if (!providerConfig.isFinnhubEnabled()) {
            throw new IOException("Finnhub provider is disabled.");
        }
        if (!hasText(providerConfig.getFinnhubApiKey())) {
            throw new IOException("FINNHUB_API_KEY is missing.");
        }

        String base = trimTrailingSlash(providerConfig.getFinnhubBaseUrl());
        String token = urlEncode(providerConfig.getFinnhubApiKey());

        if (!Boolean.FALSE.equals(candleEndpointAccessible)) {
            try {
                CandleSeries candleSeries = fetchCandleSeries(base, ticker, startDate, endDate, token);
                candleEndpointAccessible = Boolean.TRUE;
                persistPriceSnapshots(ticker, candleSeries.pricePoints());
                return new HistoricalSeries(candleSeries.pricePoints(), "FINNHUB_CANDLE");
            } catch (IOException exception) {
                if (isCandleAccessDenied(exception)) {
                    candleEndpointAccessible = Boolean.FALSE;
                    LOGGER.warn("Finnhub candle endpoint unavailable for current key. Falling back to local snapshots.");
                } else {
                    throw exception;
                }
            }
        }

        if (allowQuoteSeeding && endDate.equals(LocalDate.now(ZoneOffset.UTC))) {
            seedSnapshotFromQuote(ticker, base, token, endDate);
        }

        List<PricePoint> localPoints = loadLocalPricePoints(ticker, startDate, endDate);
        if (localPoints.size() < 2) {
            throw buildCoverageException(ticker, startDate, endDate);
        }
        return new HistoricalSeries(localPoints, "LOCAL_SNAPSHOTS");
    }

    private IllegalArgumentException buildCoverageException(String ticker, LocalDate requestedStart, LocalDate requestedEnd) {
        List<MarketPriceSnapshot> allSnapshots = marketPriceSnapshotRepository.findAllByTickerIgnoreCaseOrderByTradingDateAsc(ticker);
        if (allSnapshots.isEmpty()) {
            return new IllegalArgumentException(
                    "Performance Replay cannot run yet for " + ticker
                            + ". No local price history exists for this ticker, and Finnhub candle access is unavailable for your key."
            );
        }

        LocalDate availableStart = allSnapshots.get(0).getTradingDate();
        LocalDate availableEnd = allSnapshots.get(allSnapshots.size() - 1).getTradingDate();
        return new IllegalArgumentException(
                "Performance Replay cannot run for requested range "
                        + requestedStart + " to " + requestedEnd
                        + ". Available local history for " + ticker
                        + " is " + availableStart + " to " + availableEnd
                        + ". Use a date range within that window, or use a Finnhub plan with /stock/candle access."
        );
    }

    private List<PricePoint> loadLocalPricePoints(String ticker, LocalDate startDate, LocalDate endDate) {
        return marketPriceSnapshotRepository
                .findAllByTickerIgnoreCaseAndTradingDateBetweenOrderByTradingDateAsc(ticker, startDate, endDate)
                .stream()
                .map(snapshot -> new PricePoint(snapshot.getTradingDate(), snapshot.getClosePrice()))
                .collect(Collectors.toList());
    }

    private void seedSnapshotFromQuote(String ticker, String base, String token, LocalDate endDate) {
        try {
            JsonNode quoteNode = executeJsonGet(base + "/quote?symbol=" + urlEncode(ticker) + "&token=" + token);
            Double currentPrice = nullableDouble(quoteNode.path("c"));
            Double previousClose = nullableDouble(quoteNode.path("pc"));
            if (currentPrice != null && currentPrice > 0) {
                persistPriceSnapshot(ticker, endDate, currentPrice);
            }
            if (previousClose != null && previousClose > 0) {
                persistPriceSnapshot(ticker, endDate.minusDays(1), previousClose);
            }
        } catch (IOException exception) {
            LOGGER.warn("Unable to seed local quote snapshots for {}", ticker, exception);
        }
    }

    private List<Double> dailyReturnsFromPricePoints(List<PricePoint> points) {
        List<Double> returns = new ArrayList<>();
        for (int i = 1; i < points.size(); i++) {
            double previous = points.get(i - 1).price();
            double current = points.get(i).price();
            if (previous <= 0) {
                continue;
            }
            returns.add((current - previous) / previous);
        }
        return returns;
    }

    private Map<LocalDate, Double> dailyReturnsByDateFromPricePoints(List<PricePoint> points) {
        Map<LocalDate, Double> returnsByDate = new LinkedHashMap<>();
        for (int i = 1; i < points.size(); i++) {
            double previous = points.get(i - 1).price();
            double current = points.get(i).price();
            if (previous <= 0) {
                continue;
            }
            returnsByDate.put(points.get(i).date(), (current - previous) / previous);
        }
        return returnsByDate;
    }

    private MarketData fetchMarketData(String ticker) throws IOException {
        String normalisedTicker = normaliseTicker(ticker);
        CacheEntry<MarketData> cachedEntry = marketDataCache.get(normalisedTicker);
        if (isFresh(cachedEntry)) {
            return cachedEntry.value();
        }

        try {
            MarketData freshData = fetchMarketDataFromProviderWithRetry(normalisedTicker);
            marketDataCache.put(normalisedTicker, new CacheEntry<>(freshData, Instant.now()));
            return freshData;
        } catch (IOException exception) {
            if (isWithinStaleWindow(cachedEntry)) {
                LOGGER.warn("Using stale cached market data for {} because provider is unavailable", normalisedTicker, exception);
                return cachedEntry.value();
            }
            throw exception;
        }
    }

    private MarketData fetchMarketDataFromProviderWithRetry(String ticker) throws IOException {
        int attempts = Math.max(1, riskConfig.getProviderRetryAttempts());
        IOException lastException = null;
        for (int attempt = 1; attempt <= attempts; attempt++) {
            try {
                return fetchMarketDataFromProvider(ticker);
            } catch (IOException exception) {
                lastException = exception;
                if (attempt >= attempts) {
                    break;
                }
                LOGGER.warn("Market data fetch failed for {} (attempt {}/{}). Retrying...", ticker, attempt, attempts, exception);
                try {
                    Thread.sleep(RETRY_BACKOFF_MILLIS);
                } catch (InterruptedException interruptedException) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        throw new IOException("Unable to reach market data provider for ticker: " + ticker, lastException);
    }

    private MarketData fetchMarketDataFromProvider(String ticker) throws IOException {
        LocalDate endDate = LocalDate.now(ZoneOffset.UTC);
        LocalDate startDate = endDate.minusDays(Math.max(riskConfig.getHistoryDays(), 30));
        if (!providerConfig.isFinnhubEnabled()) {
            throw new IOException("Finnhub provider is disabled.");
        }
        if (!hasText(providerConfig.getFinnhubApiKey())) {
            throw new IOException("FINNHUB_API_KEY is missing.");
        }
        return fetchFromFinnhub(ticker, startDate, endDate);
    }

    private MarketData fetchFromFinnhub(String ticker, LocalDate startDate, LocalDate endDate) throws IOException {
        String base = trimTrailingSlash(providerConfig.getFinnhubBaseUrl());
        String token = urlEncode(providerConfig.getFinnhubApiKey());

        if (Boolean.FALSE.equals(candleEndpointAccessible)) {
            return fetchFromFinnhubUsingQuoteOnly(ticker, base, token, endDate);
        }

        try {
            CandleSeries candleSeries = fetchCandleSeries(base, ticker, startDate, endDate, token);
            List<PricePoint> pricePoints = candleSeries.pricePoints();

            JsonNode quoteNode = executeJsonGet(base + "/quote?symbol=" + urlEncode(ticker) + "&token=" + token);
            JsonNode profileNode = fetchProfileNode(base, ticker, token);
            Double currentPrice = nullableDouble(quoteNode.path("c"));
            if (currentPrice == null || currentPrice <= 0) {
                currentPrice = pricePoints.get(pricePoints.size() - 1).price();
            }

            Double dayChange = nullableDouble(quoteNode.path("d"));
            Double dayChangePercent = toRatio(nullableDouble(quoteNode.path("dp")));

            QuoteSnapshot quoteSnapshot = new QuoteSnapshot(
                    currentPrice,
                    dayChange,
                    dayChangePercent,
                    nullableDouble(quoteNode.path("h")),
                    nullableDouble(quoteNode.path("l")),
                    extractLatestVolume(candleSeries.volumeArray()),
                    marketCapFromProfile(profileNode),
                    currencyFromProfile(profileNode, ticker)
            );

            candleEndpointAccessible = Boolean.TRUE;
            persistPriceSnapshots(ticker, pricePoints);
            return buildMarketDataFromPricePoints(pricePoints, quoteSnapshot);
        } catch (IOException exception) {
            if (isCandleAccessDenied(exception)) {
                candleEndpointAccessible = Boolean.FALSE;
                LOGGER.warn("Finnhub candle endpoint unavailable for current key. Falling back to quote-based local history mode.");
                return fetchFromFinnhubUsingQuoteOnly(ticker, base, token, endDate);
            }
            throw exception;
        }
    }

    private CandleSeries fetchCandleSeries(String base,
                                           String ticker,
                                           LocalDate startDate,
                                           LocalDate endDate,
                                           String token) throws IOException {
        long from = startDate.atStartOfDay().toEpochSecond(ZoneOffset.UTC);
        long to = endDate.plusDays(1).atStartOfDay().toEpochSecond(ZoneOffset.UTC) - 1;
        JsonNode candleNode = executeJsonGet(
                base + "/stock/candle?symbol=" + urlEncode(ticker) + "&resolution=D&from=" + from + "&to=" + to + "&token=" + token
        );
        if (candleNode.has("error")) {
            throw new IOException("Finnhub error: " + candleNode.path("error").asText());
        }
        if (!"ok".equalsIgnoreCase(candleNode.path("s").asText())) {
            throw new IOException("Finnhub candle data unavailable");
        }

        JsonNode closeArray = candleNode.path("c");
        JsonNode timeArray = candleNode.path("t");
        JsonNode volumeArray = candleNode.path("v");
        if (!closeArray.isArray() || !timeArray.isArray() || closeArray.isEmpty() || timeArray.isEmpty()) {
            throw new IOException("Finnhub returned invalid candle payload");
        }

        int size = Math.min(closeArray.size(), timeArray.size());
        List<PricePoint> pricePoints = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            JsonNode closeNode = closeArray.get(i);
            JsonNode epochNode = timeArray.get(i);
            if (closeNode == null || closeNode.isNull() || epochNode == null || epochNode.isNull()) {
                continue;
            }
            double close = closeNode.asDouble(Double.NaN);
            long epoch = epochNode.asLong(Long.MIN_VALUE);
            if (!Double.isFinite(close) || close <= 0 || epoch == Long.MIN_VALUE) {
                continue;
            }
            LocalDate date = Instant.ofEpochSecond(epoch).atZone(ZoneOffset.UTC).toLocalDate();
            pricePoints.add(new PricePoint(date, close));
        }

        if (pricePoints.size() < 2) {
            throw new IOException("Not enough Finnhub history to compute returns");
        }
        return new CandleSeries(pricePoints, volumeArray);
    }

    private MarketData fetchFromFinnhubUsingQuoteOnly(String ticker, String base, String token, LocalDate endDate) throws IOException {
        JsonNode quoteNode = executeJsonGet(base + "/quote?symbol=" + urlEncode(ticker) + "&token=" + token);
        JsonNode profileNode = fetchProfileNode(base, ticker, token);

        Double currentPrice = nullableDouble(quoteNode.path("c"));
        Double previousClose = nullableDouble(quoteNode.path("pc"));
        if (currentPrice == null || currentPrice <= 0) {
            throw new IOException("Finnhub quote payload is missing current price");
        }

        persistPriceSnapshot(ticker, endDate, currentPrice);
        if (previousClose != null && previousClose > 0) {
            persistPriceSnapshot(ticker, endDate.minusDays(1), previousClose);
        }

        List<PricePoint> points = marketPriceSnapshotRepository.findAllByTickerIgnoreCaseOrderByTradingDateAsc(ticker).stream()
                .map(snapshot -> new PricePoint(snapshot.getTradingDate(), snapshot.getClosePrice()))
                .collect(Collectors.toList());

        if (points.size() < 2 && previousClose != null && previousClose > 0) {
            points = List.of(
                    new PricePoint(endDate.minusDays(1), previousClose),
                    new PricePoint(endDate, currentPrice)
            );
        }
        if (points.size() < 2) {
            throw new IOException("Not enough quote history to compute risk metrics yet");
        }

        QuoteSnapshot quoteSnapshot = new QuoteSnapshot(
                currentPrice,
                nullableDouble(quoteNode.path("d")),
                toRatio(nullableDouble(quoteNode.path("dp"))),
                nullableDouble(quoteNode.path("h")),
                nullableDouble(quoteNode.path("l")),
                null,
                marketCapFromProfile(profileNode),
                currencyFromProfile(profileNode, ticker)
        );

        return buildMarketDataFromPricePoints(points, quoteSnapshot);
    }

    private void persistPriceSnapshots(String ticker, List<PricePoint> points) {
        for (PricePoint point : points) {
            persistPriceSnapshot(ticker, point.date(), point.price());
        }
    }

    private void persistPriceSnapshot(String ticker, LocalDate date, double closePrice) {
        if (!Double.isFinite(closePrice) || closePrice <= 0 || date == null) {
            return;
        }
        MarketPriceSnapshot snapshot = marketPriceSnapshotRepository
                .findByTickerIgnoreCaseAndTradingDate(ticker, date)
                .orElseGet(MarketPriceSnapshot::new);
        snapshot.setTicker(ticker);
        snapshot.setTradingDate(date);
        snapshot.setClosePrice(closePrice);
        snapshot.setCapturedAt(Instant.now());
        marketPriceSnapshotRepository.save(snapshot);
    }

    private JsonNode fetchProfileNode(String base, String ticker, String token) {
        try {
            return executeJsonGet(base + "/stock/profile2?symbol=" + urlEncode(ticker) + "&token=" + token);
        } catch (IOException exception) {
            LOGGER.debug("Unable to fetch Finnhub profile data for {}", ticker, exception);
            return null;
        }
    }

    private boolean isCandleAccessDenied(IOException exception) {
        String message = exception.getMessage();
        if (!hasText(message)) {
            return false;
        }
        String lowered = message.toLowerCase(Locale.US);
        return lowered.contains("http 403") || lowered.contains("don't have access");
    }

    private Long marketCapFromProfile(JsonNode profileNode) {
        if (profileNode == null) {
            return null;
        }
        Double inMillions = nullableDouble(profileNode.path("marketCapitalization"));
        if (inMillions == null || inMillions < 0) {
            return null;
        }
        double absolute = inMillions * 1_000_000d;
        if (!Double.isFinite(absolute) || absolute > Long.MAX_VALUE) {
            return null;
        }
        return Math.round(absolute);
    }

    private String currencyFromProfile(JsonNode profileNode, String ticker) {
        if (profileNode != null) {
            String currency = profileNode.path("currency").asText("");
            if (hasText(currency)) {
                return currency;
            }
        }
        return inferCurrency(ticker);
    }

    private Double toRatio(Double percentValue) {
        if (percentValue == null) {
            return null;
        }
        return percentValue / 100.0;
    }

    private MarketData buildMarketDataFromPricePoints(List<PricePoint> sourcePoints, QuoteSnapshot quoteSnapshot) throws IOException {
        List<PricePoint> points = new ArrayList<>(sourcePoints);
        points.sort(Comparator.comparing(PricePoint::date));

        int sampleSize = Math.min(points.size(), Math.max(riskConfig.getTradingDaysPerYear(), 30));
        points = points.subList(points.size() - sampleSize, points.size());
        if (points.size() < 2) {
            throw new IOException("Not enough sampled points to compute returns");
        }

        List<Double> closingPrices = points.stream().map(PricePoint::price).collect(Collectors.toList());
        Map<LocalDate, Double> returnsByDate = new LinkedHashMap<>();
        List<Double> returns = new ArrayList<>();

        for (int i = 1; i < points.size(); i++) {
            double previous = points.get(i - 1).price();
            double current = points.get(i).price();
            if (previous <= 0) {
                continue;
            }
            double dailyReturn = (current - previous) / previous;
            returns.add(dailyReturn);
            returnsByDate.put(points.get(i).date(), dailyReturn);
        }

        if (returns.isEmpty()) {
            throw new IOException("Price series did not produce daily returns");
        }

        return new MarketData(closingPrices, returns, returnsByDate, quoteSnapshot);
    }

    private JsonNode executeJsonGet(String url) throws IOException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .GET()
                .header("Accept", "application/json")
                .header("User-Agent", "InvestGuard/1.0")
                .timeout(Duration.ofSeconds(Math.max(5, providerConfig.getHttpTimeoutSeconds())))
                .build();

        HttpResponse<String> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
            throw new IOException("Provider request interrupted", interruptedException);
        }

        if (response.statusCode() >= 400) {
            String upstreamError = extractUpstreamError(response.body());
            if (upstreamError != null) {
                throw new IOException("Provider returned HTTP " + response.statusCode() + ": " + upstreamError);
            }
            throw new IOException("Provider returned HTTP " + response.statusCode());
        }

        try {
            return objectMapper.readTree(response.body());
        } catch (Exception parseException) {
            throw new IOException("Provider returned invalid JSON payload", parseException);
        }
    }

    private String extractUpstreamError(String body) {
        if (!hasText(body)) {
            return null;
        }
        try {
            JsonNode parsed = objectMapper.readTree(body);
            String error = parsed.path("error").asText("");
            return hasText(error) ? error : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    private boolean isFresh(CacheEntry<MarketData> entry) {
        if (entry == null) {
            return false;
        }
        long ttlSeconds = Math.max(1, riskConfig.getQuoteCacheTtlSeconds());
        Duration age = Duration.between(entry.fetchedAt(), Instant.now());
        return age.getSeconds() <= ttlSeconds;
    }

    private boolean isWithinStaleWindow(CacheEntry<MarketData> entry) {
        if (entry == null) {
            return false;
        }
        long maxStaleMinutes = Math.max(1, riskConfig.getStaleCacheMaxAgeMinutes());
        Duration age = Duration.between(entry.fetchedAt(), Instant.now());
        return age.toMinutes() <= maxStaleMinutes;
    }

    private Map<LocalDate, Double> fetchBenchmarkReturns() {
        String benchmark = benchmarkTicker();
        try {
            return fetchMarketData(benchmark).dailyReturnsByDate();
        } catch (IOException ioException) {
            LOGGER.warn("Could not fetch benchmark data for {}. Beta values will be set to 0.", benchmark, ioException);
            return Collections.emptyMap();
        }
    }

    private RiskMetrics computeRiskMetrics(List<Double> dailyReturns,
                                           List<Double> priceSeries,
                                           Map<LocalDate, Double> stockReturnsByDate,
                                           Map<LocalDate, Double> benchmarkReturnsByDate,
                                           double amount) {
        if (dailyReturns == null || dailyReturns.isEmpty()) {
            throw new IllegalArgumentException("Unable to compute metrics with no returns");
        }

        int tradingDays = Math.max(riskConfig.getTradingDaysPerYear(), 1);
        double meanDailyReturn = dailyReturns.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        double dailyStdDev = calculateStandardDeviation(dailyReturns);

        double annualisedReturn = Math.pow(1 + meanDailyReturn, tradingDays) - 1;
        double annualisedVolatility = dailyStdDev * Math.sqrt(tradingDays);
        double sharpeRatio = annualisedVolatility > 0 ? (annualisedReturn - riskConfig.getRiskFreeRate()) / annualisedVolatility : 0;
        double valueAtRisk = computeValueAtRisk(dailyReturns, amount);
        double expectedShortfall = computeExpectedShortfall(dailyReturns, amount);

        List<Double> equityCurve = priceSeries != null ? priceSeries : buildEquityCurve(dailyReturns);
        double maxDrawdown = computeMaxDrawdown(equityCurve);
        double betaVsBenchmark = computeBeta(stockReturnsByDate, benchmarkReturnsByDate);
        String riskLevel = classifyRisk(annualisedVolatility, maxDrawdown, sharpeRatio);

        return new RiskMetrics(annualisedReturn, annualisedVolatility, sharpeRatio, valueAtRisk, maxDrawdown, expectedShortfall, betaVsBenchmark, riskLevel);
    }

    public double calculateStandardDeviation(List<Double> returns) {
        double mean = returns.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        double variance = returns.stream().mapToDouble(r -> Math.pow(r - mean, 2)).average().orElse(0);
        return Math.sqrt(variance);
    }

    private double computeValueAtRisk(List<Double> returns, double amount) {
        List<Double> sorted = new ArrayList<>(returns);
        Collections.sort(sorted);
        int index = percentileIndex(sorted.size());
        double percentileReturn = sorted.get(index);
        double lossPercentage = Math.min(0, percentileReturn);
        return Math.abs(lossPercentage * amount);
    }

    private double computeExpectedShortfall(List<Double> returns, double amount) {
        List<Double> sorted = new ArrayList<>(returns);
        Collections.sort(sorted);
        int index = percentileIndex(sorted.size());
        List<Double> tail = sorted.subList(0, index + 1);
        double averageTail = tail.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        double lossPercentage = Math.min(0, averageTail);
        return Math.abs(lossPercentage * amount);
    }

    private int percentileIndex(int size) {
        if (size <= 1) {
            return 0;
        }
        double percentile = riskConfig.getVarPercentile();
        int index = (int) Math.floor(percentile * (size - 1));
        return Math.max(0, Math.min(index, size - 1));
    }

    private List<Double> buildEquityCurve(List<Double> returns) {
        List<Double> equity = new ArrayList<>();
        double value = 1.0;
        equity.add(value);
        for (double dailyReturn : returns) {
            value *= (1 + dailyReturn);
            equity.add(value);
        }
        return equity;
    }

    private double computeMaxDrawdown(List<Double> priceSeries) {
        if (priceSeries == null || priceSeries.isEmpty()) {
            return 0;
        }
        double peak = priceSeries.get(0);
        double maxDrawdown = 0;
        for (double value : priceSeries) {
            if (value > peak) {
                peak = value;
            }
            if (peak > 0) {
                double drawdown = (value - peak) / peak;
                if (drawdown < maxDrawdown) {
                    maxDrawdown = drawdown;
                }
            }
        }
        return Math.abs(maxDrawdown);
    }

    private double computeBeta(Map<LocalDate, Double> stockReturnsByDate, Map<LocalDate, Double> benchmarkReturnsByDate) {
        if (stockReturnsByDate == null || benchmarkReturnsByDate == null || stockReturnsByDate.isEmpty() || benchmarkReturnsByDate.isEmpty()) {
            return 0;
        }

        List<Double> stockSeries = new ArrayList<>();
        List<Double> benchmarkSeries = new ArrayList<>();
        for (Map.Entry<LocalDate, Double> entry : stockReturnsByDate.entrySet()) {
            Double benchmarkValue = benchmarkReturnsByDate.get(entry.getKey());
            if (benchmarkValue != null) {
                stockSeries.add(entry.getValue());
                benchmarkSeries.add(benchmarkValue);
            }
        }

        if (stockSeries.size() < 2) {
            return 0;
        }

        double stockMean = stockSeries.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        double benchmarkMean = benchmarkSeries.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        double covariance = 0;
        double variance = 0;

        for (int i = 0; i < stockSeries.size(); i++) {
            double stockDiff = stockSeries.get(i) - stockMean;
            double benchmarkDiff = benchmarkSeries.get(i) - benchmarkMean;
            covariance += stockDiff * benchmarkDiff;
            variance += benchmarkDiff * benchmarkDiff;
        }

        return variance == 0 ? 0 : covariance / variance;
    }

    private String classifyRisk(double annualVolatility, double maxDrawdown, double sharpeRatio) {
        if (annualVolatility < riskConfig.getLowVolatilityThreshold() && maxDrawdown < riskConfig.getLowDrawdownThreshold() && sharpeRatio >= riskConfig.getLowSharpeThreshold()) {
            return "Low";
        }
        if (annualVolatility < riskConfig.getModerateVolatilityThreshold() && maxDrawdown < riskConfig.getModerateDrawdownThreshold()) {
            return "Moderate";
        }
        return "High";
    }

    private void applyMetrics(Investment investment, RiskMetrics metrics) {
        investment.setAverageReturn(metrics.averageReturn());
        investment.setVolatility(metrics.volatility());
        investment.setSharpeRatio(metrics.sharpeRatio());
        investment.setValueAtRisk(metrics.valueAtRisk());
        investment.setMaxDrawdown(metrics.maxDrawdown());
        investment.setExpectedShortfall(metrics.expectedShortfall());
        investment.setBetaVsBenchmark(metrics.betaVsBenchmark());
        investment.setRiskLevel(metrics.riskLevel());
        investment.setLastUpdated(Instant.now());
    }

    private void applyQuoteSnapshot(Investment investment, QuoteSnapshot quoteSnapshot) {
        if (quoteSnapshot == null) {
            return;
        }
        investment.setCurrentPrice(quoteSnapshot.currentPrice());
        investment.setDayChange(quoteSnapshot.dayChange());
        investment.setDayChangePercent(quoteSnapshot.dayChangePercent());
        investment.setDayHigh(quoteSnapshot.dayHigh());
        investment.setDayLow(quoteSnapshot.dayLow());
        investment.setVolume(quoteSnapshot.volume());
        investment.setMarketCap(quoteSnapshot.marketCap());
        investment.setQuoteCurrency(quoteSnapshot.quoteCurrency());
    }

    private void validateAmount(double amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero");
        }
    }

    private String normaliseTicker(String ticker) {
        if (ticker == null || ticker.isBlank()) {
            throw new IllegalArgumentException("Ticker is required");
        }
        return ticker.trim().toUpperCase(Locale.US);
    }

    private String trimTrailingSlash(String value) {
        if (value == null) {
            return "";
        }
        String result = value.trim();
        while (result.endsWith("/")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }

    private String urlEncode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private Double nullableDouble(JsonNode node) {
        if (node == null || node.isNull() || node.asText().isBlank()) {
            return null;
        }
        double value = node.asDouble(Double.NaN);
        return Double.isFinite(value) ? value : null;
    }

    private Long extractLatestVolume(JsonNode volumeArray) {
        if (volumeArray == null || !volumeArray.isArray() || volumeArray.isEmpty()) {
            return null;
        }
        for (int i = volumeArray.size() - 1; i >= 0; i--) {
            JsonNode node = volumeArray.get(i);
            if (node != null && !node.isNull()) {
                long value = node.asLong(Long.MIN_VALUE);
                if (value != Long.MIN_VALUE && value >= 0) {
                    return value;
                }
            }
        }
        return null;
    }

    private String inferCurrency(String symbol) {
        if (symbol == null || symbol.isBlank()) {
            return "USD";
        }
        String upper = symbol.toUpperCase(Locale.US);
        if (upper.endsWith(".TO") || upper.endsWith(".V")) {
            return "CAD";
        }
        if (upper.endsWith(".L")) {
            return "GBP";
        }
        if (upper.endsWith(".PA") || upper.endsWith(".DE") || upper.endsWith(".AS")) {
            return "EUR";
        }
        if (upper.endsWith(".T")) {
            return "JPY";
        }
        return "USD";
    }

    @FunctionalInterface
    private interface MetricExtractor {
        double extract(RiskSummary summary);
    }

    private record PricePoint(LocalDate date, double price) { }

    private record CandleSeries(List<PricePoint> pricePoints, JsonNode volumeArray) { }

    private record MarketData(List<Double> closingPrices,
                              List<Double> dailyReturns,
                              Map<LocalDate, Double> dailyReturnsByDate,
                              QuoteSnapshot quoteSnapshot) { }

    private record HistoricalSeries(List<PricePoint> points, String source) { }

    private record CacheEntry<T>(T value, Instant fetchedAt) { }

    private record QuoteSnapshot(Double currentPrice,
                                 Double dayChange,
                                 Double dayChangePercent,
                                 Double dayHigh,
                                 Double dayLow,
                                 Long volume,
                                 Long marketCap,
                                 String quoteCurrency) { }

    private record RiskMetrics(double averageReturn,
                               double volatility,
                               double sharpeRatio,
                               double valueAtRisk,
                               double maxDrawdown,
                               double expectedShortfall,
                               double betaVsBenchmark,
                               String riskLevel) { }
}
