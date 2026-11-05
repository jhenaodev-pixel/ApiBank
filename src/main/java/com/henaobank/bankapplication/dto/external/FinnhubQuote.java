package com.henaobank.bankapplication.dto.external;

import java.math.BigDecimal;

/**
 * Respuesta de Finnhub para /quote.
 * c = precio actual, d = cambio, dp = cambio porcentual, h = máximo del día,
 * l = mínimo del día, o = apertura, pc = cierre anterior, t = timestamp (segundos).
 */
public record FinnhubQuote(
        BigDecimal c,
        BigDecimal d,
        BigDecimal dp,
        BigDecimal h,
        BigDecimal l,
        BigDecimal o,
        BigDecimal pc,
        Long t) {
}
