package com.henaobank.bankapplication.dto.response;

import com.henaobank.bankapplication.entity.enums.AssetType;
import com.henaobank.bankapplication.entity.enums.TradeType;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
public class InvestmentTradeResponse {

    private Long id;
    private String reference;
    private String symbol;
    private AssetType assetType;
    private TradeType tradeType;
    private BigDecimal quantity;
    private BigDecimal price;
    private BigDecimal totalAmount;
    private BigDecimal realizedProfit;
    private String accountNumber;
    private BigDecimal accountBalance;
    private String description;
    private LocalDateTime timestamp;
}
