package com.henaobank.bankapplication.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class PortfolioResponse {

    private BigDecimal totalInvested;
    private BigDecimal totalCurrentValue;
    private BigDecimal totalProfitLoss;
    private BigDecimal totalProfitLossPercent;

    /** true si todos los activos tienen precio de mercado y la valoración está completa. */
    private boolean fullyValued;

    private LocalDateTime valuedAt;
    private List<InvestmentPositionResponse> positions;
}
