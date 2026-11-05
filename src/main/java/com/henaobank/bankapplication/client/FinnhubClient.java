package com.henaobank.bankapplication.client;

import com.henaobank.bankapplication.config.MarketDataProperties;
import com.henaobank.bankapplication.dto.external.FinnhubQuote;
import com.henaobank.bankapplication.dto.external.FinnhubSearchResponse;
import com.henaobank.bankapplication.exception.BadRequestException;
import com.henaobank.bankapplication.exception.MarketDataException;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.time.Duration;

/**
 * Cliente de Finnhub (acciones). Requiere una API key gratuita:
 * https://finnhub.io/register
 */
@Component
public class FinnhubClient {

    private static final String QUOTE_PATH = "/quote";
    private static final String SEARCH_PATH = "/search";

    private final RestClient restClient;
    private final String apiKey;

    public FinnhubClient(MarketDataProperties properties) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setReadTimeout(Duration.ofSeconds(15));

        this.restClient = RestClient.builder()
                .baseUrl(properties.finnhub().baseUrl())
                .requestFactory(factory)
                .build();
        this.apiKey = properties.finnhub().apiKey();
    }

    /** Cotización actual de una acción, por ejemplo AAPL. */
    public FinnhubQuote getQuote(String symbol) {
        requireApiKey();
        try {
            FinnhubQuote quote = restClient.get()
                    .uri(uri -> uri.path(QUOTE_PATH)
                            .queryParam("symbol", symbol)
                            .queryParam("token", apiKey)
                            .build())
                    .retrieve()
                    .body(FinnhubQuote.class);

            if (quote == null || quote.c() == null || quote.c().signum() <= 0) {
                throw new BadRequestException("Acción no encontrada en Finnhub: " + symbol);
            }
            return quote;
        } catch (RestClientResponseException ex) {
            throw translate(ex);
        } catch (ResourceAccessException ex) {
            throw new MarketDataException("No se pudo conectar con Finnhub. Intente nuevamente.", ex);
        }
    }

    /** Busca acciones por nombre o símbolo (por ejemplo "apple" o "AAPL"). */
    public FinnhubSearchResponse search(String query) {
        requireApiKey();
        try {
            FinnhubSearchResponse response = restClient.get()
                    .uri(uri -> uri.path(SEARCH_PATH)
                            .queryParam("q", query)
                            .queryParam("token", apiKey)
                            .build())
                    .retrieve()
                    .body(FinnhubSearchResponse.class);

            return response == null ? new FinnhubSearchResponse(0, java.util.List.of()) : response;
        } catch (RestClientResponseException ex) {
            throw translate(ex);
        } catch (ResourceAccessException ex) {
            throw new MarketDataException("No se pudo conectar con Finnhub. Intente nuevamente.", ex);
        }
    }

    private void requireApiKey() {
        if (!StringUtils.hasText(apiKey)) {
            throw new MarketDataException("Finnhub no está configurado. Defina la variable de entorno "
                    + "FINNHUB_API_KEY o la propiedad app.market-data.finnhub.api-key.");
        }
    }

    private RuntimeException translate(RestClientResponseException ex) {
        int status = ex.getStatusCode().value();

        if (status == 401 || status == 403) {
            return new MarketDataException("La API key de Finnhub no es válida o no tiene permisos.");
        }
        if (status == 429) {
            return new MarketDataException("Finnhub alcanzó el límite de peticiones del plan gratuito. "
                    + "Espere un minuto e intente nuevamente.");
        }
        if (ex.getStatusCode().is4xxClientError()) {
            return new BadRequestException("Finnhub rechazó la solicitud. Revise el símbolo o la búsqueda.");
        }
        return new MarketDataException("Finnhub respondió con un error (" + status + "). Intente más tarde.", ex);
    }
}
