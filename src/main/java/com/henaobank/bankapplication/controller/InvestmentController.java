package com.henaobank.bankapplication.controller;

import com.henaobank.bankapplication.dto.request.InvestmentTradeRequest;
import com.henaobank.bankapplication.dto.response.InvestmentPositionResponse;
import com.henaobank.bankapplication.dto.response.InvestmentTradeResponse;
import com.henaobank.bankapplication.dto.response.PortfolioResponse;
import com.henaobank.bankapplication.entity.enums.AssetType;
import com.henaobank.bankapplication.service.InvestmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Apartado de inversión: comprar y vender criptomonedas/acciones, y consultar
 * el portafolio con su rentabilidad usando precios de mercado en vivo.
 */
@RestController
@RequestMapping("/api/v1/investments")
@RequiredArgsConstructor
@Tag(name = "Inversiones", description = "Compra y venta de criptomonedas y acciones")
public class InvestmentController {

    private final InvestmentService investmentService;

    @PostMapping("/buy")
    @Operation(summary = "Compra un activo con dinero de una cuenta bancaria",
            description = "Envíe quantity (unidades) o amount (dinero), pero no ambos.")
    public ResponseEntity<InvestmentTradeResponse> buy(@Valid @RequestBody InvestmentTradeRequest request,
                                                       Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(investmentService.buy(request, authentication.getName()));
    }

    @PostMapping("/sell")
    @Operation(summary = "Vende unidades de un activo y abona el dinero en la cuenta")
    public ResponseEntity<InvestmentTradeResponse> sell(@Valid @RequestBody InvestmentTradeRequest request,
                                                        Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(investmentService.sell(request, authentication.getName()));
    }

    @GetMapping("/portfolio")
    @Operation(summary = "Portafolio valorizado: inversión, valor actual y ganancia/pérdida")
    public ResponseEntity<PortfolioResponse> getPortfolio(Authentication authentication) {
        return ResponseEntity.ok(investmentService.getPortfolio(authentication.getName()));
    }

    @GetMapping("/positions")
    @Operation(summary = "Posiciones abiertas del usuario")
    public ResponseEntity<List<InvestmentPositionResponse>> getPositions(Authentication authentication) {
        return ResponseEntity.ok(investmentService.getPositions(authentication.getName()));
    }

    @GetMapping("/history")
    @Operation(summary = "Historial de compras y ventas",
            description = "Se puede filtrar por symbol y/o assetType.")
    public ResponseEntity<List<InvestmentTradeResponse>> getHistory(
            @RequestParam(required = false) String symbol,
            @RequestParam(required = false) AssetType assetType,
            Authentication authentication) {
        return ResponseEntity.ok(investmentService.getHistory(authentication.getName(), symbol, assetType));
    }
}
