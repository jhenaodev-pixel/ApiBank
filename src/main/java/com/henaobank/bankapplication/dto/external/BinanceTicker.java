package com.henaobank.bankapplication.dto.external;

import java.math.BigDecimal;

/**
 * Respuesta de Binance para /api/v3/ticker/24hr. Los precios llegan como
 * texto en el JSON, por eso se deserializan directamente a BigDecimal.
 */
public record BinanceTicker(
        String symbol,
        BigDecimal priceChange,
        BigDecimal priceChangePercent,
        BigDecimal weightedAvgPrice,
        BigDecimal prevClosePrice,
        BigDecimal lastPrice,
        BigDecimal bidPrice,
        BigDecimal askPrice,
        BigDecimal openPrice,
        BigDecimal highPrice,
        BigDecimal lowPrice,
        BigDecimal volume,
        BigDecimal quoteVolume,
        Long openTime,
        Long closeTime) {
}
