package com.example.riskanalyzer.dto;

import java.time.Instant;

public class PortfolioSummary {

    private final double totalExposure;
    private final double weightedReturn;
    private final double weightedVolatility;
    private final double totalValueAtRisk;
    private final double worstDrawdown;
    private final String portfolioRiskLevel;
    private final int positions;
    private final String topMoverTicker;
    private final Double topMoverPercent;
    private final String benchmarkTicker;
    private final Instant updatedAt;

    public PortfolioSummary(double totalExposure,
                            double weightedReturn,
                            double weightedVolatility,
                            double totalValueAtRisk,
                            double worstDrawdown,
                            String portfolioRiskLevel,
                            int positions,
                            String topMoverTicker,
                            Double topMoverPercent,
                            String benchmarkTicker,
                            Instant updatedAt) {
        this.totalExposure = totalExposure;
        this.weightedReturn = weightedReturn;
        this.weightedVolatility = weightedVolatility;
        this.totalValueAtRisk = totalValueAtRisk;
        this.worstDrawdown = worstDrawdown;
        this.portfolioRiskLevel = portfolioRiskLevel;
        this.positions = positions;
        this.topMoverTicker = topMoverTicker;
        this.topMoverPercent = topMoverPercent;
        this.benchmarkTicker = benchmarkTicker;
        this.updatedAt = updatedAt;
    }

    public double getTotalExposure() {
        return totalExposure;
    }

    public double getWeightedReturn() {
        return weightedReturn;
    }

    public double getWeightedVolatility() {
        return weightedVolatility;
    }

    public double getTotalValueAtRisk() {
        return totalValueAtRisk;
    }

    public double getWorstDrawdown() {
        return worstDrawdown;
    }

    public String getPortfolioRiskLevel() {
        return portfolioRiskLevel;
    }

    public int getPositions() {
        return positions;
    }

    public String getTopMoverTicker() {
        return topMoverTicker;
    }

    public Double getTopMoverPercent() {
        return topMoverPercent;
    }

    public String getBenchmarkTicker() {
        return benchmarkTicker;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
