package com.example.riskanalyzer.controller;

import com.example.riskanalyzer.model.Investment;
import com.example.riskanalyzer.service.RiskService;
import com.example.riskanalyzer.repository.InvestmentRepository;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.*;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*") // ✅ Allows frontend to call API from different domains
public class RiskController {
    private final RiskService riskService;
    private final InvestmentRepository investmentRepository;

    // ✅ Constructor to inject dependencies
    public RiskController(RiskService riskService, InvestmentRepository investmentRepository) {
        this.riskService = riskService;
        this.investmentRepository = investmentRepository;
    }

    // ✅ Fetch stock data and add investment
    @PostMapping("/add-stock")
    public String addStockInvestment(@RequestParam String ticker, @RequestParam double amount) {
        try {
            Investment investment = riskService.fetchStockData(ticker, amount);
            return "Investment added: " + ticker;
        } catch (IOException e) {
            return "Error fetching stock data: " + e.getMessage();
        }
    }

    // ✅ Manually add investment to database
    @PostMapping("/add-manual")
    public String addManualInvestment(@RequestBody Investment investment) {
        riskService.saveInvestment(investment);
        return "Investment added: " + investment.getTicker();
    }

    // ✅ Retrieve all investments from database
    @GetMapping("/investments")
    public List<Investment> getInvestments() {
        return riskService.getAllInvestments();
    }

    // ✅ Correct `/risk` endpoint - fetches all risk data from DB
    @GetMapping("/risk")
    public List<Map<String, Object>> calculateRisk() {
        return riskService.calculateRisk();
    }
}
