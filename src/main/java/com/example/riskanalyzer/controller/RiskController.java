package com.example.riskanalyzer.controller;

import com.example.riskanalyzer.dto.InvestmentRequest;
import com.example.riskanalyzer.dto.PortfolioSummary;
import com.example.riskanalyzer.dto.RiskSummary;
import com.example.riskanalyzer.dto.BacktestRequest;
import com.example.riskanalyzer.dto.BacktestResult;
import com.example.riskanalyzer.model.Investment;
import com.example.riskanalyzer.service.RiskService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class RiskController {

    private final RiskService riskService;

    public RiskController(RiskService riskService) {
        this.riskService = riskService;
    }

    @PostMapping("/add-stock")
    public ResponseEntity<RiskSummary> addStockInvestment(@RequestParam String ticker, @RequestParam double amount) throws IOException {
        try {
            Investment investment = riskService.fetchStockData(ticker, amount);
            return ResponseEntity.status(HttpStatus.CREATED).body(riskService.toRiskSummary(investment));
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage(), exception);
        }
    }

    @PostMapping("/add-manual")
    public ResponseEntity<RiskSummary> addManualInvestment(@RequestBody InvestmentRequest request) {
        try {
            Investment investment = new Investment(request.getTicker(), request.getAmount(), request.getPastReturns());
            Investment saved = riskService.saveInvestment(investment);
            return ResponseEntity.status(HttpStatus.CREATED).body(riskService.toRiskSummary(saved));
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage(), exception);
        }
    }

    @GetMapping("/investments")
    public List<Investment> getInvestments() {
        return riskService.getAllInvestments();
    }

    @GetMapping("/risk")
    public List<RiskSummary> calculateRisk() {
        return riskService.calculateRisk();
    }

    @GetMapping("/risk/live")
    public List<RiskSummary> refreshAndCalculateRisk() {
        return riskService.refreshRiskWithLiveData();
    }

    @GetMapping("/portfolio/summary")
    public PortfolioSummary portfolioSummary() {
        return riskService.calculatePortfolioSummary();
    }

    @GetMapping("/portfolio/summary/live")
    public PortfolioSummary livePortfolioSummary() {
        List<RiskSummary> liveRisk = riskService.refreshRiskWithLiveData();
        return riskService.calculatePortfolioSummary(liveRisk);
    }

    @PostMapping("/portfolio/backtest")
    public BacktestResult runBacktest(@RequestBody BacktestRequest request) throws IOException {
        try {
            return riskService.backtestPortfolio(request);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage(), exception);
        }
    }
}
