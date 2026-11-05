package com.henaobank.bankapplication.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class StockSearchResponse {

    private String symbol;
    private String displaySymbol;
    private String description;
    private String type;
}
