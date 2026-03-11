package com.example.riskanalyzer.dto;

import java.util.Locale;

public class BacktestRequest {

    private String ticker = "";
    private double amount;
    private String startDate = "";
    private String endDate = "";

    public String getTicker() {
        return ticker;
    }

    public void setTicker(String ticker) {
        if (ticker == null) {
            this.ticker = "";
            return;
        }
        this.ticker = ticker.trim().toUpperCase(Locale.US);
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public String getStartDate() {
        return startDate;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate == null ? "" : startDate.trim();
    }

    public String getEndDate() {
        return endDate;
    }

    public void setEndDate(String endDate) {
        this.endDate = endDate == null ? "" : endDate.trim();
    }
}
