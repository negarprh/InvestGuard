package com.example.riskanalyzer.model;

import java.util.List;

public class Investment {
    private String ticker;
    private double amount;
    private List<Double> pastReturns;

    // Default Constructor (Required for Object Mapping)
    public Investment() {}

    // Parameterized Constructor
    public Investment(String ticker, double amount, List<Double> pastReturns) {
        this.ticker = ticker;
        this.amount = amount;
        this.pastReturns = pastReturns;
    }

    // Getter Methods
    public String getTicker() {
        return ticker;
    }

    public double getAmount() {
        return amount;
    }

    public List<Double> getPastReturns() {
        return pastReturns;
    }
}