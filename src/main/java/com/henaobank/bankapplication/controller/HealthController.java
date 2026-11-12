package com.henaobank.bankapplication.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Endpoint de salud para el healthcheck del hosting (Render).
 *
 * A propósito no toca la base de datos ni servicios externos: si dependiera de
 * Binance o de Finnhub, un corte del proveedor marcaría el despliegue como
 * fallido aunque la aplicación estuviera levantada.
 */
@RestController
@RequestMapping("/api/v1/health")
@Tag(name = "Salud", description = "Estado del servicio para el hosting")
public class HealthController {

    @GetMapping
    @Operation(summary = "Comprueba que la aplicación responde")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "UP"));
    }
}
