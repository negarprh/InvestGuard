package com.example.riskanalyzer.repository;

import com.example.riskanalyzer.model.Investment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InvestmentRepository extends JpaRepository<Investment, Long> {
    Optional<Investment> findByTickerIgnoreCase(String ticker);
    List<Investment> findAllByOrderByTickerAsc();
}
