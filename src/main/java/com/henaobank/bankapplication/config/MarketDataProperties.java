package com.henaobank.bankapplication.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Configuración de los proveedores de datos de mercado.
 * La API pública de Binance no requiere clave; Finnhub sí (plan gratuito).
 */
@ConfigurationProperties(prefix = "app.market-data")
public record MarketDataProperties(Binance binance, Finnhub finnhub) {

    public record Binance(String baseUrl, List<String> defaultSymbols) {
    }

    public record Finnhub(String baseUrl, String apiKey) {
    }
}
