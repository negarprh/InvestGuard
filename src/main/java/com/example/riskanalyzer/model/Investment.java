package com.example.riskanalyzer.model;

import jakarta.persistence.*;
import java.util.List;

@Entity  // Marks this class as a database entity
public class Investment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Auto-increment ID
    private Long id;

    private String ticker;
    private double amount;

    @ElementCollection  // Stores past returns in a separate table
    private List<Double> pastReturns;

    // Default Constructor (Required by JPA)
    public Investment() {}

    // Parameterized Constructor
    public Investment(String ticker, double amount, List<Double> pastReturns) {
        this.ticker = ticker;
        this.amount = amount;
        this.pastReturns = pastReturns;
    }

    // Getter Methods
    public Long getId() { return id; }
    public String getTicker() { return ticker; }
    public double getAmount() { return amount; }
    public List<Double> getPastReturns() { return pastReturns; }
}
