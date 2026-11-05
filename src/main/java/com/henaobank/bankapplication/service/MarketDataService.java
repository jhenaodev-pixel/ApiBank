package com.henaobank.bankapplication.service;

import com.henaobank.bankapplication.dto.response.AssetQuoteResponse;
import com.henaobank.bankapplication.dto.response.CandleResponse;
import com.henaobank.bankapplication.dto.response.StockSearchResponse;
import com.henaobank.bankapplication.entity.enums.AssetType;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface MarketDataService {

    /** Normaliza el símbolo según el tipo: BTC -> BTCUSDT, aapl -> AAPL. */
    String normalizeSymbol(AssetType assetType, String symbol);

    AssetQuoteResponse getCryptoQuote(String symbol);

    List<AssetQuoteResponse> getCryptoQuotes();

    List<CandleResponse> getCryptoHistory(String symbol, String interval, int limit);

    AssetQuoteResponse getStockQuote(String symbol);

    List<StockSearchResponse> searchStocks(String query, int limit);

    /** Precio actual de un activo, usado al comprar o vender. */
    BigDecimal getPrice(AssetType assetType, String symbol);

    /**
     * Precios actuales de varios activos. Si un símbolo falla se omite del mapa,
     * para que el portafolio pueda mostrarse parcialmente valorado.
     */
    Map<String, BigDecimal> getPrices(AssetType assetType, Collection<String> symbols);
}
