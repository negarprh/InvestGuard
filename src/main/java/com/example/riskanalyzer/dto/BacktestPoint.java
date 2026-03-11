package com.example.riskanalyzer.dto;

import java.time.LocalDate;

public class BacktestPoint {

    private final LocalDate date;
    private final double closePrice;
    private final double portfolioValue;
    private final double drawdown;

    public BacktestPoint(LocalDate date, double closePrice, double portfolioValue, double drawdown) {
        this.date = date;
        this.closePrice = closePrice;
        this.portfolioValue = portfolioValue;
        this.drawdown = drawdown;
    }

    public LocalDate getDate() {
        return date;
    }

    public double getClosePrice() {
        return closePrice;
    }

    public double getPortfolioValue() {
        return portfolioValue;
    }

    public double getDrawdown() {
        return drawdown;
    }
}
