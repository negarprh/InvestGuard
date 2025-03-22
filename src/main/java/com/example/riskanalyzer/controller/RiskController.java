package com.example.riskanalyzer.controller;

import com.example.riskanalyzer.model.Investment;
import com.example.riskanalyzer.service.RiskService;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api")
public class RiskController {
    private final List<Investment> portfolio = new ArrayList<>();
    private final RiskService riskService;

    public RiskController(RiskService riskService) {
        this.riskService = riskService;
    }

    @PostMapping("/add")
    public String addInvestment(@RequestParam String ticker,
                                @RequestParam double amount,
                                @RequestParam List<Double> pastReturns) {
        portfolio.add(new Investment(ticker, amount, pastReturns));
        return "Investment added: " + ticker;
    }

    @GetMapping("/risk")
    public List<Map<String, Object>> calculateRisk() {
        List<Map<String, Object>> riskList = new ArrayList<>();
        for (Investment inv : portfolio) {
            riskList.add(riskService.analyzeRisk(inv));
        }
        return riskList;
    }
}
