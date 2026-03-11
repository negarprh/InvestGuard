package com.example.riskanalyzer.dto;

import java.time.Instant;

public class RiskSummary {

    private final String ticker;
    private final double amount;
    private final double averageReturn;
    private final double volatility;
    private final double sharpeRatio;
    private final double valueAtRisk;
    private final double maxDrawdown;
    private final double expectedShortfall;
    private final double betaVsBenchmark;
    private final String riskLevel;
    private final Double currentPrice;
    private final Double dayChange;
    private final Double dayChangePercent;
    private final Double dayHigh;
    private final Double dayLow;
    private final Long volume;
    private final Long marketCap;
    private final String quoteCurrency;
    private final Instant lastUpdated;

    public RiskSummary(String ticker,
                       double amount,
                       double averageReturn,
                       double volatility,
                       double sharpeRatio,
                       double valueAtRisk,
                       double maxDrawdown,
                       double expectedShortfall,
                       double betaVsBenchmark,
                       String riskLevel,
                       Double currentPrice,
                       Double dayChange,
                       Double dayChangePercent,
                       Double dayHigh,
                       Double dayLow,
                       Long volume,
                       Long marketCap,
                       String quoteCurrency,
                       Instant lastUpdated) {
        this.ticker = ticker;
        this.amount = amount;
        this.averageReturn = averageReturn;
        this.volatility = volatility;
        this.sharpeRatio = sharpeRatio;
        this.valueAtRisk = valueAtRisk;
        this.maxDrawdown = maxDrawdown;
        this.expectedShortfall = expectedShortfall;
        this.betaVsBenchmark = betaVsBenchmark;
        this.riskLevel = riskLevel;
        this.currentPrice = currentPrice;
        this.dayChange = dayChange;
        this.dayChangePercent = dayChangePercent;
        this.dayHigh = dayHigh;
        this.dayLow = dayLow;
        this.volume = volume;
        this.marketCap = marketCap;
        this.quoteCurrency = quoteCurrency;
        this.lastUpdated = lastUpdated;
    }

    public String getTicker() {
        return ticker;
    }

    public double getAmount() {
        return amount;
    }

    public double getAverageReturn() {
        return averageReturn;
    }

    public double getVolatility() {
        return volatility;
    }

    public double getSharpeRatio() {
        return sharpeRatio;
    }

    public double getValueAtRisk() {
        return valueAtRisk;
    }

    public double getMaxDrawdown() {
        return maxDrawdown;
    }

    public double getExpectedShortfall() {
        return expectedShortfall;
    }

    public double getBetaVsBenchmark() {
        return betaVsBenchmark;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public Double getCurrentPrice() {
        return currentPrice;
    }

    public Double getDayChange() {
        return dayChange;
    }

    public Double getDayChangePercent() {
        return dayChangePercent;
    }

    public Double getDayHigh() {
        return dayHigh;
    }

    public Double getDayLow() {
        return dayLow;
    }

    public Long getVolume() {
        return volume;
    }

    public Long getMarketCap() {
        return marketCap;
    }

    public String getQuoteCurrency() {
        return quoteCurrency;
    }

    public Instant getLastUpdated() {
        return lastUpdated;
    }
}
