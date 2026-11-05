package com.henaobank.bankapplication.service.impl;

import com.henaobank.bankapplication.client.BinanceClient;
import com.henaobank.bankapplication.client.FinnhubClient;
import com.henaobank.bankapplication.config.MarketDataProperties;
import com.henaobank.bankapplication.dto.external.BinanceTicker;
import com.henaobank.bankapplication.dto.external.FinnhubQuote;
import com.henaobank.bankapplication.dto.external.FinnhubSearchItem;
import com.henaobank.bankapplication.dto.external.FinnhubSearchResponse;
import com.henaobank.bankapplication.dto.response.AssetQuoteResponse;
import com.henaobank.bankapplication.dto.response.CandleResponse;
import com.henaobank.bankapplication.dto.response.StockSearchResponse;
import com.henaobank.bankapplication.entity.enums.AssetType;
import com.henaobank.bankapplication.exception.BadRequestException;
import com.henaobank.bankapplication.service.MarketDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MarketDataServiceImpl implements MarketDataService {

    /** Si el usuario escribe BTC (sin par), se asume BTCUSDT. */
    private static final String DEFAULT_CRYPTO_QUOTE = "USDT";

    private static final List<String> KNOWN_QUOTES = List.of(
            "USDT", "USDC", "FDUSD", "BUSD", "TUSD", "DAI",
            "BTC", "ETH", "BNB", "EUR", "TRY", "BRL");

    private static final Set<String> ALLOWED_INTERVALS = Set.of(
            "1m", "3m", "5m", "15m", "30m",
            "1h", "2h", "4h", "6h", "8h", "12h",
            "1d", "3d", "1w", "1M");

    private final BinanceClient binanceClient;
    private final FinnhubClient finnhubClient;
    private final MarketDataProperties properties;

    // ------------------------------------------------------------------ símbolos

    @Override
    public String normalizeSymbol(AssetType assetType, String symbol) {
        if (assetType == null) {
            throw new BadRequestException("El tipo de activo es obligatorio (CRYPTO o STOCK)");
        }
        return assetType == AssetType.CRYPTO
                ? normalizeCryptoSymbol(symbol)
                : normalizeStockSymbol(symbol);
    }

    private String normalizeCryptoSymbol(String symbol) {
        if (!StringUtils.hasText(symbol)) {
            throw new BadRequestException("El símbolo de la criptomoneda es obligatorio");
        }
        String normalized = symbol.trim().toUpperCase(Locale.ROOT);

        boolean alreadyHasQuote = KNOWN_QUOTES.stream()
                .anyMatch(quote -> normalized.length() > quote.length() && normalized.endsWith(quote));

        return alreadyHasQuote ? normalized : normalized + DEFAULT_CRYPTO_QUOTE;
    }

    private String normalizeStockSymbol(String symbol) {
        if (!StringUtils.hasText(symbol)) {
            throw new BadRequestException("El símbolo de la acción es obligatorio");
        }
        return symbol.trim().toUpperCase(Locale.ROOT);
    }

    // ------------------------------------------------------------------ criptomonedas

    @Override
    public AssetQuoteResponse getCryptoQuote(String symbol) {
        String normalized = normalizeCryptoSymbol(symbol);
        return mapCrypto(binanceClient.get24hTicker(normalized));
    }

    @Override
    public List<AssetQuoteResponse> getCryptoQuotes() {
        List<String> symbols = properties.binance().defaultSymbols();
        if (symbols == null || symbols.isEmpty()) {
            return List.of();
        }

        Map<String, BinanceTicker> bySymbol = binanceClient.get24hTickers(symbols).stream()
                .collect(Collectors.toMap(BinanceTicker::symbol, Function.identity(), (a, b) -> a));

        // Se respeta el orden configurado en application.yaml
        List<AssetQuoteResponse> quotes = new ArrayList<>();
        for (String symbol : symbols) {
            BinanceTicker ticker = bySymbol.get(symbol);
            if (ticker != null) {
                quotes.add(mapCrypto(ticker));
            }
        }
        return quotes;
    }

    @Override
    public List<CandleResponse> getCryptoHistory(String symbol, String interval, int limit) {
        String normalized = normalizeCryptoSymbol(symbol);
        String normalizedInterval = StringUtils.hasText(interval) ? interval.trim() : "1d";

        if (!ALLOWED_INTERVALS.contains(normalizedInterval)) {
            throw new BadRequestException("Intervalo no válido. Valores permitidos: "
                    + String.join(", ", ALLOWED_INTERVALS));
        }

        int safeLimit = Math.max(1, Math.min(limit, 1000));

        return binanceClient.getKlines(normalized, normalizedInterval, safeLimit).stream()
                .filter(kline -> kline.size() >= 7)
                .map(this::mapKline)
                .toList();
    }

    private AssetQuoteResponse mapCrypto(BinanceTicker ticker) {
        return AssetQuoteResponse.builder()
                .symbol(ticker.symbol())
                .assetType(AssetType.CRYPTO)
                .price(ticker.lastPrice())
                .change(ticker.priceChange())
                .changePercent(ticker.priceChangePercent())
                .openPrice(ticker.openPrice())
                .highPrice(ticker.highPrice())
                .lowPrice(ticker.lowPrice())
                .previousClose(ticker.prevClosePrice())
                .volume(ticker.volume())
                .quoteVolume(ticker.quoteVolume())
                .currency("USDT")
                .quotedAt(ticker.closeTime() != null
                        ? fromEpochMillis(ticker.closeTime())
                        : LocalDateTime.now(ZoneOffset.UTC))
                .build();
    }

    private CandleResponse mapKline(List<Object> kline) {
        return CandleResponse.builder()
                .openTime(fromEpochMillis(kline.get(0)))
                .open(toBigDecimal(kline.get(1)))
                .high(toBigDecimal(kline.get(2)))
                .low(toBigDecimal(kline.get(3)))
                .close(toBigDecimal(kline.get(4)))
                .volume(toBigDecimal(kline.get(5)))
                .closeTime(fromEpochMillis(kline.get(6)))
                .quoteVolume(kline.size() > 7 ? toBigDecimal(kline.get(7)) : null)
                .build();
    }

    // ------------------------------------------------------------------ acciones

    @Override
    public AssetQuoteResponse getStockQuote(String symbol) {
        String normalized = normalizeStockSymbol(symbol);
        return mapStock(normalized, finnhubClient.getQuote(normalized));
    }

    @Override
    public List<StockSearchResponse> searchStocks(String query, int limit) {
        if (!StringUtils.hasText(query)) {
            throw new BadRequestException("Debe indicar un texto para buscar acciones");
        }

        int safeLimit = Math.max(1, Math.min(limit, 50));
        FinnhubSearchResponse response = finnhubClient.search(query.trim());

        if (response.result() == null) {
            return List.of();
        }

        return response.result().stream()
                .limit(safeLimit)
                .map(this::mapSearchItem)
                .toList();
    }

    private AssetQuoteResponse mapStock(String symbol, FinnhubQuote quote) {
        BigDecimal change = quote.d();
        if (change == null && quote.pc() != null) {
            change = quote.c().subtract(quote.pc());
        }

        BigDecimal changePercent = quote.dp();
        if (changePercent == null && change != null && quote.pc() != null && quote.pc().signum() > 0) {
            changePercent = change.multiply(BigDecimal.valueOf(100))
                    .divide(quote.pc(), 4, RoundingMode.HALF_UP);
        }

        return AssetQuoteResponse.builder()
                .symbol(symbol)
                .assetType(AssetType.STOCK)
                .price(quote.c())
                .change(change)
                .changePercent(changePercent)
                .openPrice(quote.o())
                .highPrice(quote.h())
                .lowPrice(quote.l())
                .previousClose(quote.pc())
                .currency("USD")
                .quotedAt(quote.t() != null && quote.t() > 0
                        ? LocalDateTime.ofInstant(Instant.ofEpochSecond(quote.t()), ZoneOffset.UTC)
                        : LocalDateTime.now(ZoneOffset.UTC))
                .build();
    }

    private StockSearchResponse mapSearchItem(FinnhubSearchItem item) {
        return StockSearchResponse.builder()
                .symbol(item.symbol())
                .displaySymbol(item.displaySymbol())
                .description(item.description())
                .type(item.type())
                .build();
    }

    // ------------------------------------------------------------------ precios para invertir

    @Override
    public BigDecimal getPrice(AssetType assetType, String symbol) {
        if (assetType == AssetType.CRYPTO) {
            return getCryptoQuote(symbol).getPrice();
        }
        if (assetType == AssetType.STOCK) {
            return getStockQuote(symbol).getPrice();
        }
        throw new BadRequestException("El tipo de activo es obligatorio (CRYPTO o STOCK)");
    }

    @Override
    public Map<String, BigDecimal> getPrices(AssetType assetType, Collection<String> symbols) {
        Map<String, BigDecimal> prices = new LinkedHashMap<>();
        if (assetType == null || symbols == null || symbols.isEmpty()) {
            return prices;
        }

        if (assetType == AssetType.CRYPTO) {
            List<String> normalized = symbols.stream()
                    .map(this::normalizeCryptoSymbol)
                    .distinct()
                    .toList();
            try {
                for (BinanceTicker ticker : binanceClient.get24hTickers(normalized)) {
                    if (ticker.lastPrice() != null) {
                        prices.put(ticker.symbol(), ticker.lastPrice());
                    }
                }
            } catch (RuntimeException ex) {
                log.warn("No se pudo valorar el portafolio de criptomonedas: {}", ex.getMessage());
            }
            return prices;
        }

        for (String rawSymbol : symbols) {
            String symbol = normalizeStockSymbol(rawSymbol);
            try {
                prices.put(symbol, finnhubClient.getQuote(symbol).c());
            } catch (RuntimeException ex) {
                log.warn("No se pudo obtener el precio de {}: {}", symbol, ex.getMessage());
            }
        }
        return prices;
    }

    // ------------------------------------------------------------------ utilidades

    private BigDecimal toBigDecimal(Object value) {
        if (value == null) {
            return null;
        }
        return new BigDecimal(String.valueOf(value));
    }

    private LocalDateTime fromEpochMillis(Object value) {
        if (value == null) {
            return null;
        }
        return LocalDateTime.ofInstant(
                Instant.ofEpochMilli(Long.parseLong(String.valueOf(value))), ZoneOffset.UTC);
    }
}
