package com.henaobank.bankapplication.service;

import com.henaobank.bankapplication.dto.request.InvestmentTradeRequest;
import com.henaobank.bankapplication.dto.response.InvestmentPositionResponse;
import com.henaobank.bankapplication.dto.response.InvestmentTradeResponse;
import com.henaobank.bankapplication.dto.response.PortfolioResponse;
import com.henaobank.bankapplication.entity.enums.AssetType;

import java.util.List;

public interface InvestmentService {

    /** Compra un activo con dinero de una cuenta bancaria del usuario. */
    InvestmentTradeResponse buy(InvestmentTradeRequest request, String email);

    /** Vende unidades de un activo y abona el dinero en la cuenta bancaria. */
    InvestmentTradeResponse sell(InvestmentTradeRequest request, String email);

    /** Portafolio valorizado con los precios actuales del mercado. */
    PortfolioResponse getPortfolio(String email);

    List<InvestmentPositionResponse> getPositions(String email);

    List<InvestmentTradeResponse> getHistory(String email, String symbol, AssetType assetType);
}
