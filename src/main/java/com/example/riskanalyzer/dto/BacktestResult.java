package com.example.riskanalyzer.dto;

import java.time.LocalDate;
import java.util.List;

public class BacktestResult {

    private final String ticker;
    private final LocalDate startDate;
    private final LocalDate endDate;
    private final double initialInvestment;
    private final double shares;
    private final double startPrice;
    private final double endPrice;
    private final double currentValue;
    private final double totalReturn;
    private final double cagr;
    private final double annualizedVolatility;
    private final double sharpeRatio;
    private final double valueAtRisk;
    private final double expectedShortfall;
    private final double maxDrawdown;
    private final double betaVsBenchmark;
    private final String benchmarkTicker;
    private final String dataSource;
    private final List<BacktestPoint> history;

    public BacktestResult(String ticker,
                          LocalDate startDate,
                          LocalDate endDate,
                          double initialInvestment,
                          double shares,
                          double startPrice,
                          double endPrice,
                          double currentValue,
                          double totalReturn,
                          double cagr,
                          double annualizedVolatility,
                          double sharpeRatio,
                          double valueAtRisk,
                          double expectedShortfall,
                          double maxDrawdown,
                          double betaVsBenchmark,
                          String benchmarkTicker,
                          String dataSource,
                          List<BacktestPoint> history) {
        this.ticker = ticker;
        this.startDate = startDate;
        this.endDate = endDate;
        this.initialInvestment = initialInvestment;
        this.shares = shares;
        this.startPrice = startPrice;
        this.endPrice = endPrice;
        this.currentValue = currentValue;
        this.totalReturn = totalReturn;
        this.cagr = cagr;
        this.annualizedVolatility = annualizedVolatility;
        this.sharpeRatio = sharpeRatio;
        this.valueAtRisk = valueAtRisk;
        this.expectedShortfall = expectedShortfall;
        this.maxDrawdown = maxDrawdown;
        this.betaVsBenchmark = betaVsBenchmark;
        this.benchmarkTicker = benchmarkTicker;
        this.dataSource = dataSource;
        this.history = history;
    }

    public String getTicker() {
        return ticker;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public double getInitialInvestment() {
        return initialInvestment;
    }

    public double getShares() {
        return shares;
    }

    public double getStartPrice() {
        return startPrice;
    }

    public double getEndPrice() {
        return endPrice;
    }

    public double getCurrentValue() {
        return currentValue;
    }

    public double getTotalReturn() {
        return totalReturn;
    }

    public double getCagr() {
        return cagr;
    }

    public double getAnnualizedVolatility() {
        return annualizedVolatility;
    }

    public double getSharpeRatio() {
        return sharpeRatio;
    }

    public double getValueAtRisk() {
        return valueAtRisk;
    }

    public double getExpectedShortfall() {
        return expectedShortfall;
    }

    public double getMaxDrawdown() {
        return maxDrawdown;
    }

    public double getBetaVsBenchmark() {
        return betaVsBenchmark;
    }

    public String getBenchmarkTicker() {
        return benchmarkTicker;
    }

    public String getDataSource() {
        return dataSource;
    }

    public List<BacktestPoint> getHistory() {
        return history;
    }
}
