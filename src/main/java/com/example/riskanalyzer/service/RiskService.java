package com.example.riskanalyzer.service;

import com.example.riskanalyzer.model.Investment;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class RiskService {

    public double calculateStandardDeviation(List<Double> returns) {
        double mean = returns.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        double variance = returns.stream().mapToDouble(r -> Math.pow(r - mean, 2)).average().orElse(0);
        return Math.sqrt(variance);
    }

    public Map<String, Object> analyzeRisk(Investment investment) {
        double stdDev = calculateStandardDeviation(investment.getPastReturns());
        Map<String, Object> result = new HashMap<>();
        result.put("Investment", investment.getTicker());
        result.put("Risk (Std Dev)", stdDev);
        return result;
    }
}
