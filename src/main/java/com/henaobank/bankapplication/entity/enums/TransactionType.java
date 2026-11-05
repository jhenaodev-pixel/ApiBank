package com.henaobank.bankapplication.entity.enums;

public enum TransactionType {
    DEPOSIT, // transaccion deposito
    WITHDRAWAL, //transaccion retiro
    TRANSFER, //transaccion transferencia
    INVESTMENT_BUY, // compra de criptomoneda o accion (sale dinero de la cuenta)
    INVESTMENT_SELL // venta de criptomoneda o accion (entra dinero a la cuenta)
}
