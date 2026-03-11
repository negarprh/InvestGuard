package com.example.riskanalyzer.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "investguard.risk")
public class RiskConfigProperties {

    private int tradingDaysPerYear = 252;
    private int historyDays = 365;
    private double riskFreeRate = 0.015;
    private double varPercentile = 0.05;
    private double lowVolatilityThreshold = 0.12;
    private double moderateVolatilityThreshold = 0.25;
    private double lowDrawdownThreshold = 0.15;
    private double moderateDrawdownThreshold = 0.3;
    private double lowSharpeThreshold = 1.0;
    private String benchmarkTicker = "SPY";
    private int quoteCacheTtlSeconds = 300;
    private int staleCacheMaxAgeMinutes = 180;
    private int providerRetryAttempts = 2;

    public int getTradingDaysPerYear() {
        return tradingDaysPerYear;
    }

    public void setTradingDaysPerYear(int tradingDaysPerYear) {
        this.tradingDaysPerYear = tradingDaysPerYear;
    }

    public int getHistoryDays() {
        return historyDays;
    }

    public void setHistoryDays(int historyDays) {
        this.historyDays = historyDays;
    }

    public double getRiskFreeRate() {
        return riskFreeRate;
    }

    public void setRiskFreeRate(double riskFreeRate) {
        this.riskFreeRate = riskFreeRate;
    }

    public double getVarPercentile() {
        return varPercentile;
    }

    public void setVarPercentile(double varPercentile) {
        this.varPercentile = varPercentile;
    }

    public double getLowVolatilityThreshold() {
        return lowVolatilityThreshold;
    }

    public void setLowVolatilityThreshold(double lowVolatilityThreshold) {
        this.lowVolatilityThreshold = lowVolatilityThreshold;
    }

    public double getModerateVolatilityThreshold() {
        return moderateVolatilityThreshold;
    }

    public void setModerateVolatilityThreshold(double moderateVolatilityThreshold) {
        this.moderateVolatilityThreshold = moderateVolatilityThreshold;
    }

    public double getLowDrawdownThreshold() {
        return lowDrawdownThreshold;
    }

    public void setLowDrawdownThreshold(double lowDrawdownThreshold) {
        this.lowDrawdownThreshold = lowDrawdownThreshold;
    }

    public double getModerateDrawdownThreshold() {
        return moderateDrawdownThreshold;
    }

    public void setModerateDrawdownThreshold(double moderateDrawdownThreshold) {
        this.moderateDrawdownThreshold = moderateDrawdownThreshold;
    }

    public double getLowSharpeThreshold() {
        return lowSharpeThreshold;
    }

    public void setLowSharpeThreshold(double lowSharpeThreshold) {
        this.lowSharpeThreshold = lowSharpeThreshold;
    }

    public String getBenchmarkTicker() {
        return benchmarkTicker;
    }

    public void setBenchmarkTicker(String benchmarkTicker) {
        this.benchmarkTicker = benchmarkTicker;
    }

    public int getQuoteCacheTtlSeconds() {
        return quoteCacheTtlSeconds;
    }

    public void setQuoteCacheTtlSeconds(int quoteCacheTtlSeconds) {
        this.quoteCacheTtlSeconds = quoteCacheTtlSeconds;
    }

    public int getStaleCacheMaxAgeMinutes() {
        return staleCacheMaxAgeMinutes;
    }

    public void setStaleCacheMaxAgeMinutes(int staleCacheMaxAgeMinutes) {
        this.staleCacheMaxAgeMinutes = staleCacheMaxAgeMinutes;
    }

    public int getProviderRetryAttempts() {
        return providerRetryAttempts;
    }

    public void setProviderRetryAttempts(int providerRetryAttempts) {
        this.providerRetryAttempts = providerRetryAttempts;
    }
}
