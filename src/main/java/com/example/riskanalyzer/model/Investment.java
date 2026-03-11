package com.example.riskanalyzer.model;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "investment")
public class Investment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String ticker;

    @Column(nullable = false)
    private double amount;

    @Column(name = "average_return")
    private double averageReturn;

    @Column(name = "volatility")
    private double volatility;

    @Column(name = "sharpe_ratio")
    private double sharpeRatio;

    @Column(name = "value_at_risk")
    private double valueAtRisk;

    @Column(name = "max_drawdown")
    private double maxDrawdown;

    @Column(name = "risk_level")
    private String riskLevel;

    @Column(name = "last_updated")
    private Instant lastUpdated;

    @Column(name = "current_price")
    private Double currentPrice;

    @Column(name = "day_change")
    private Double dayChange;

    @Column(name = "day_change_percent")
    private Double dayChangePercent;

    @Column(name = "day_high")
    private Double dayHigh;

    @Column(name = "day_low")
    private Double dayLow;

    @Column(name = "volume")
    private Long volume;

    @Column(name = "market_cap")
    private Long marketCap;

    @Column(name = "quote_currency")
    private String quoteCurrency;

    @Column(name = "beta_vs_benchmark")
    private double betaVsBenchmark;

    @Column(name = "expected_shortfall")
    private double expectedShortfall;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "investment_returns", joinColumns = @JoinColumn(name = "investment_id"))
    @Column(name = "daily_return")
    private List<Double> pastReturns = new ArrayList<>();

    public Investment() {
    }

    public Investment(String ticker, double amount, List<Double> pastReturns) {
        this.ticker = ticker;
        this.amount = amount;
        setPastReturns(pastReturns);
    }

    public Long getId() {
        return id;
    }

    public String getTicker() {
        return ticker;
    }

    public void setTicker(String ticker) {
        this.ticker = ticker;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public double getAverageReturn() {
        return averageReturn;
    }

    public void setAverageReturn(double averageReturn) {
        this.averageReturn = averageReturn;
    }

    public double getVolatility() {
        return volatility;
    }

    public void setVolatility(double volatility) {
        this.volatility = volatility;
    }

    public double getSharpeRatio() {
        return sharpeRatio;
    }

    public void setSharpeRatio(double sharpeRatio) {
        this.sharpeRatio = sharpeRatio;
    }

    public double getValueAtRisk() {
        return valueAtRisk;
    }

    public void setValueAtRisk(double valueAtRisk) {
        this.valueAtRisk = valueAtRisk;
    }

    public double getMaxDrawdown() {
        return maxDrawdown;
    }

    public void setMaxDrawdown(double maxDrawdown) {
        this.maxDrawdown = maxDrawdown;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
    }

    public Instant getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(Instant lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public Double getCurrentPrice() {
        return currentPrice;
    }

    public void setCurrentPrice(Double currentPrice) {
        this.currentPrice = currentPrice;
    }

    public Double getDayChange() {
        return dayChange;
    }

    public void setDayChange(Double dayChange) {
        this.dayChange = dayChange;
    }

    public Double getDayChangePercent() {
        return dayChangePercent;
    }

    public void setDayChangePercent(Double dayChangePercent) {
        this.dayChangePercent = dayChangePercent;
    }

    public Double getDayHigh() {
        return dayHigh;
    }

    public void setDayHigh(Double dayHigh) {
        this.dayHigh = dayHigh;
    }

    public Double getDayLow() {
        return dayLow;
    }

    public void setDayLow(Double dayLow) {
        this.dayLow = dayLow;
    }

    public Long getVolume() {
        return volume;
    }

    public void setVolume(Long volume) {
        this.volume = volume;
    }

    public Long getMarketCap() {
        return marketCap;
    }

    public void setMarketCap(Long marketCap) {
        this.marketCap = marketCap;
    }

    public String getQuoteCurrency() {
        return quoteCurrency;
    }

    public void setQuoteCurrency(String quoteCurrency) {
        this.quoteCurrency = quoteCurrency;
    }

    public double getBetaVsBenchmark() {
        return betaVsBenchmark;
    }

    public void setBetaVsBenchmark(double betaVsBenchmark) {
        this.betaVsBenchmark = betaVsBenchmark;
    }

    public double getExpectedShortfall() {
        return expectedShortfall;
    }

    public void setExpectedShortfall(double expectedShortfall) {
        this.expectedShortfall = expectedShortfall;
    }

    public List<Double> getPastReturns() {
        return pastReturns;
    }

    public void setPastReturns(List<Double> pastReturns) {
        if (pastReturns == null) {
            this.pastReturns = new ArrayList<>();
        } else {
            this.pastReturns = new ArrayList<>(pastReturns);
        }
    }
}
