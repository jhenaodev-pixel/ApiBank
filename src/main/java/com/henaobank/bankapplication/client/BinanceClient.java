package com.henaobank.bankapplication.client;

import com.henaobank.bankapplication.config.MarketDataProperties;
import com.henaobank.bankapplication.dto.external.BinanceTicker;
import com.henaobank.bankapplication.exception.BadRequestException;
import com.henaobank.bankapplication.exception.MarketDataException;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.time.Duration;
import java.util.List;

/**
 * Cliente de la API pública de Binance (sin API key).
 * Documentación: https://developers.binance.com/docs/binance-spot-api-docs
 */
@Component
public class BinanceClient {

    private static final String TICKER_24H_PATH = "/api/v3/ticker/24hr";
    private static final String KLINES_PATH = "/api/v3/klines";

    private final RestClient restClient;

    public BinanceClient(MarketDataProperties properties) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setReadTimeout(Duration.ofSeconds(15));

        this.restClient = RestClient.builder()
                .baseUrl(properties.binance().baseUrl())
                .requestFactory(factory)
                .build();
    }

    /** Cotización de las últimas 24 horas para un par, por ejemplo BTCUSDT. */
    public BinanceTicker get24hTicker(String symbol) {
        try {
            BinanceTicker ticker = restClient.get()
                    .uri(uri -> uri.path(TICKER_24H_PATH).queryParam("symbol", symbol).build())
                    .retrieve()
                    .body(BinanceTicker.class);

            if (ticker == null || ticker.lastPrice() == null) {
                throw new MarketDataException("Binance no devolvió un precio válido para " + symbol);
            }
            return ticker;
        } catch (RestClientResponseException ex) {
            throw translate(symbol, ex);
        } catch (ResourceAccessException ex) {
            throw new MarketDataException("No se pudo conectar con Binance. Intente nuevamente.", ex);
        }
    }

    /** Cotizaciones de varios pares en una sola llamada. */
    public List<BinanceTicker> get24hTickers(List<String> symbols) {
        String symbolsJson = symbols.stream()
                .map(s -> "\"" + s + "\"")
                .reduce((a, b) -> a + "," + b)
                .map(s -> "[" + s + "]")
                .orElse("[]");

        try {
            List<BinanceTicker> tickers = restClient.get()
                    .uri(uri -> uri.path(TICKER_24H_PATH).queryParam("symbols", symbolsJson).build())
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<BinanceTicker>>() {
                    });

            return tickers == null ? List.of() : tickers;
        } catch (RestClientResponseException ex) {
            throw translate("lista de símbolos", ex);
        } catch (ResourceAccessException ex) {
            throw new MarketDataException("No se pudo conectar con Binance. Intente nuevamente.", ex);
        }
    }

    /**
     * Velas históricas. Cada vela es un arreglo con:
     * [openTime, open, high, low, close, volume, closeTime, quoteVolume, trades, ...]
     */
    public List<List<Object>> getKlines(String symbol, String interval, int limit) {
        try {
            List<List<Object>> klines = restClient.get()
                    .uri(uri -> uri.path(KLINES_PATH)
                            .queryParam("symbol", symbol)
                            .queryParam("interval", interval)
                            .queryParam("limit", limit)
                            .build())
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<List<Object>>>() {
                    });

            return klines == null ? List.of() : klines;
        } catch (RestClientResponseException ex) {
            throw translate(symbol, ex);
        } catch (ResourceAccessException ex) {
            throw new MarketDataException("No se pudo conectar con Binance. Intente nuevamente.", ex);
        }
    }

    private RuntimeException translate(String symbol, RestClientResponseException ex) {
        int status = ex.getStatusCode().value();

        if (status == 418 || status == 429) {
            return new MarketDataException("Binance alcanzó el límite de peticiones. Espere unos segundos.");
        }
        if (ex.getStatusCode().is4xxClientError()) {
            return new BadRequestException("Binance rechazó el símbolo '" + symbol
                    + "'. Use el par completo, por ejemplo BTCUSDT.");
        }
        return new MarketDataException("Binance respondió con un error (" + status + "). Intente más tarde.", ex);
    }
}
