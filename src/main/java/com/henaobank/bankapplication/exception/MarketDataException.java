package com.henaobank.bankapplication.exception;

/**
 * Se lanza cuando un proveedor externo de precios (Binance o Finnhub) falla,
 * no está configurado o responde con un error inesperado.
 */
public class MarketDataException extends RuntimeException {

    public MarketDataException(String message) {
        super(message);
    }

    public MarketDataException(String message, Throwable cause) {
        super(message, cause);
    }
}
