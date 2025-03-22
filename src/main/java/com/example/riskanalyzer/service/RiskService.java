package com.example.riskanalyzer.service;

import com.example.riskanalyzer.model.Investment;
import com.example.riskanalyzer.repository.InvestmentRepository;
import org.springframework.stereotype.Service;
import yahoofinance.Stock;
import yahoofinance.YahooFinance;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;

@Service
public class RiskService {

    private final InvestmentRepository investmentRepository;

    public RiskService(InvestmentRepository investmentRepository) {
        this.investmentRepository = investmentRepository;
    }

    // Calculate Standard Deviation for Risk
    public double calculateStandardDeviation(List<Double> returns) {
        double mean = returns.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        double variance = returns.stream().mapToDouble(r -> Math.pow(r - mean, 2)).average().orElse(0);
        return Math.sqrt(variance);
    }

    // Analyze Risk for a Given Investment
    public Map<String, Object> analyzeRisk(Investment investment) {
        double stdDev = calculateStandardDeviation(investment.getPastReturns());
        Map<String, Object> result = new HashMap<>();
        result.put("Investment", investment.getTicker());
        result.put("Risk (Std Dev)", stdDev);
        return result;
    }

    // Fetch Stock Data from Yahoo Finance API
    public Investment fetchStockData(String ticker, double amount) throws IOException {
        Stock stock = YahooFinance.get(ticker);
        if (stock == null || stock.getQuote().getPrice() == null) {
            throw new IOException("Stock not found!");
        }

        // Simulate past returns using historical price data
        List<Double> pastReturns = new ArrayList<>();
        BigDecimal prevClose = stock.getQuote().getPreviousClose();

        if (prevClose != null) {
            for (int i = 0; i < 5; i++) {  // Simulating 5 days of returns
                BigDecimal price = stock.getQuote().getPrice();
                if (price != null && prevClose.compareTo(BigDecimal.ZERO) > 0) {
                    double returnRate = price.subtract(prevClose).divide(prevClose, BigDecimal.ROUND_HALF_UP).doubleValue();
                    pastReturns.add(returnRate);
                }
                prevClose = price;
            }
        }

        Investment investment = new Investment(ticker, amount, pastReturns);
        return investmentRepository.save(investment); // ✅ Saves investment to H2 database
    }

    // Retrieve All Investments from Database
    public List<Investment> getAllInvestments() {
        return investmentRepository.findAll();
    }

    public Investment saveInvestment(Investment investment) {
        return investmentRepository.save(investment);
    }

    public List<Map<String, Object>> calculateRisk() {
        List<Map<String, Object>> riskList = new ArrayList<>();
        List<Investment> investments = investmentRepository.findAll(); // ✅ Fetch from DB

        System.out.println("🔍 Found Investments: " + investments.size()); // ✅ Debugging

        for (Investment inv : investments) {
            System.out.println("🔎 Checking Investment: " + inv.getTicker());

            if (inv.getPastReturns() == null || inv.getPastReturns().isEmpty()) {
                System.out.println("⚠ Investment " + inv.getTicker() + " has NO past returns.");
                continue; // Skip investments without past returns
            }

            double stdDev = calculateStandardDeviation(inv.getPastReturns());

            System.out.println("📊 Risk for " + inv.getTicker() + ": " + stdDev);

            Map<String, Object> riskData = new HashMap<>();
            riskData.put("Investment", inv.getTicker());
            riskData.put("Risk (Std Dev)", stdDev);
            riskList.add(riskData);
        }

        System.out.println("✅ Risk Calculation Complete: " + riskList);
        return riskList;
    }




}
