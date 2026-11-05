package com.henaobank.bankapplication.repository;

import com.henaobank.bankapplication.entity.InvestmentTrade;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InvestmentTradeRepository extends JpaRepository<InvestmentTrade, Long> {

    List<InvestmentTrade> findByUserIdOrderByTimestampDesc(Long userId);

    List<InvestmentTrade> findByUserIdAndSymbolOrderByTimestampDesc(Long userId, String symbol);
}
