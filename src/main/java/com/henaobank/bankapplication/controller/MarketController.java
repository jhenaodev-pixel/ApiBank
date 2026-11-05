package com.henaobank.bankapplication.controller;

import com.henaobank.bankapplication.dto.response.AssetQuoteResponse;
import com.henaobank.bankapplication.dto.response.CandleResponse;
import com.henaobank.bankapplication.dto.response.StockSearchResponse;
import com.henaobank.bankapplication.service.MarketDataService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Consulta de mercado. Las criptomonedas se obtienen de la API pública de
 * Binance y las acciones de Finnhub. Estos endpoints son públicos.
 */
@RestController
@RequestMapping("/api/v1/market")
@RequiredArgsConstructor
@Tag(name = "Mercado", description = "Precios de criptomonedas (Binance) y acciones (Finnhub)")
public class MarketController {

    private final MarketDataService marketDataService;

    @GetMapping("/crypto")
    @Operation(summary = "Precios de las criptomonedas principales configuradas")
    public ResponseEntity<List<AssetQuoteResponse>> getCryptoPrices() {
        return ResponseEntity.ok(marketDataService.getCryptoQuotes());
    }

    @GetMapping("/crypto/{symbol}")
    @Operation(summary = "Precio de una criptomoneda",
            description = "Acepta el par completo (BTCUSDT) o solo el activo (BTC).")
    public ResponseEntity<AssetQuoteResponse> getCryptoPrice(@PathVariable String symbol) {
        return ResponseEntity.ok(marketDataService.getCryptoQuote(symbol));
    }

    @GetMapping("/crypto/{symbol}/history")
    @Operation(summary = "Velas históricas de una criptomoneda (Binance klines)",
            description = "Intervalos: 1m, 5m, 15m, 30m, 1h, 4h, 1d, 1w, 1M, entre otros.")
    public ResponseEntity<List<CandleResponse>> getCryptoHistory(
            @PathVariable String symbol,
            @RequestParam(defaultValue = "1d") String interval,
            @RequestParam(defaultValue = "30") @Min(1) @Max(1000) int limit) {
        return ResponseEntity.ok(marketDataService.getCryptoHistory(symbol, interval, limit));
    }

    @GetMapping("/stocks/search")
    @Operation(summary = "Busca acciones por nombre o símbolo en Finnhub")
    public ResponseEntity<List<StockSearchResponse>> searchStocks(
            @RequestParam("q") String query,
            @RequestParam(defaultValue = "10") @Min(1) @Max(50) int limit) {
        return ResponseEntity.ok(marketDataService.searchStocks(query, limit));
    }

    @GetMapping("/stocks/{symbol}")
    @Operation(summary = "Cotización actual de una acción (ejemplo: AAPL)")
    public ResponseEntity<AssetQuoteResponse> getStockPrice(@PathVariable String symbol) {
        return ResponseEntity.ok(marketDataService.getStockQuote(symbol));
    }
}
