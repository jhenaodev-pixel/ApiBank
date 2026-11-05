package com.henaobank.bankapplication.dto.response;

import com.henaobank.bankapplication.entity.enums.AssetType;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class InvestmentPositionResponse {

    private Long id;
    private String symbol;
    private AssetType assetType;
    private BigDecimal quantity;
    private BigDecimal averagePrice;
    private BigDecimal investedAmount;
    private BigDecimal currentPrice;
    private BigDecimal currentValue;
    private BigDecimal profitLoss;
    private BigDecimal profitLossPercent;
}
