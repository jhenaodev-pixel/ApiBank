package com.henaobank.bankapplication.dto.response;

import com.henaobank.bankapplication.entity.enums.AssetType;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
public class AssetQuoteResponse {

    private String symbol;
    private AssetType assetType;
    private BigDecimal price;
    private BigDecimal change;
    private BigDecimal changePercent;
    private BigDecimal openPrice;
    private BigDecimal highPrice;
    private BigDecimal lowPrice;
    private BigDecimal previousClose;
    private BigDecimal volume;
    private BigDecimal quoteVolume;
    private String currency;
    private LocalDateTime quotedAt;
}
