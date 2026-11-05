package com.henaobank.bankapplication.dto.request;

import com.henaobank.bankapplication.entity.enums.AssetType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Para comprar se puede indicar {@code quantity} (unidades del activo) o
 * {@code amount} (dinero a invertir), pero no ambos. Para vender solo se
 * permite {@code quantity}.
 */
@Getter
@Setter
public class InvestmentTradeRequest {

    @NotBlank(message = "La cuenta bancaria es obligatoria")
    private String accountNumber;

    @NotBlank(message = "El símbolo es obligatorio")
    @Size(max = 30, message = "El símbolo no puede superar 30 caracteres")
    private String symbol;

    @NotNull(message = "El tipo de activo es obligatorio (CRYPTO o STOCK)")
    private AssetType assetType;

    @DecimalMin(value = "0.00000001", message = "La cantidad debe ser mayor que cero")
    private BigDecimal quantity;

    @DecimalMin(value = "0.01", message = "El monto a invertir debe ser mayor que cero")
    private BigDecimal amount;

    @Size(max = 255, message = "La descripción no puede superar 255 caracteres")
    private String description;
}
