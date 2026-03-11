package com.example.riskanalyzer.repository;

import com.example.riskanalyzer.model.MarketPriceSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface MarketPriceSnapshotRepository extends JpaRepository<MarketPriceSnapshot, Long> {
    Optional<MarketPriceSnapshot> findByTickerIgnoreCaseAndTradingDate(String ticker, LocalDate tradingDate);
    List<MarketPriceSnapshot> findAllByTickerIgnoreCaseOrderByTradingDateAsc(String ticker);
    List<MarketPriceSnapshot> findAllByTickerIgnoreCaseAndTradingDateBetweenOrderByTradingDateAsc(String ticker, LocalDate from, LocalDate to);
}
