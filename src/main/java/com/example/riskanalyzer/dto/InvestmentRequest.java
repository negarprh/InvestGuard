package com.example.riskanalyzer.dto;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class InvestmentRequest {

    private String ticker = "";
    private double amount;
    private List<Double> pastReturns = new ArrayList<>();

    public InvestmentRequest() {
    }

    public InvestmentRequest(String ticker, double amount, List<Double> pastReturns) {
        setTicker(ticker);
        setAmount(amount);
        setPastReturns(pastReturns);
    }

    public String getTicker() {
        return ticker;
    }

    public void setTicker(String ticker) {
        if (ticker == null) {
            this.ticker = "";
        } else {
            this.ticker = ticker.trim().toUpperCase(Locale.US);
        }
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
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
