package com.henaobank.bankapplication.entity;

import com.henaobank.bankapplication.entity.enums.AssetType;
import com.henaobank.bankapplication.entity.enums.TradeType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Historial de operaciones de inversión (compra/venta). Se conserva aunque la
 * posición se cierre, para poder mostrar el historial completo del usuario.
 */
@Entity
@Table(name = "investment_trades")
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class InvestmentTrade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, updatable = false, length = 36)
    private String reference;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 30)
    private String symbol;

    @Enumerated(EnumType.STRING)
    @Column(name = "asset_type", nullable = false, length = 20)
    private AssetType assetType;

    @Enumerated(EnumType.STRING)
    @Column(name = "trade_type", nullable = false, length = 10)
    private TradeType tradeType;

    @Column(nullable = false, precision = 24, scale = 10)
    private BigDecimal quantity;

    @Column(nullable = false, precision = 24, scale = 10)
    private BigDecimal price;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal totalAmount;

    /** Ganancia o pérdida realizada en la venta (null en las compras). */
    @Column(precision = 19, scale = 2)
    private BigDecimal realizedProfit;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @Column(length = 255)
    private String description;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime timestamp;
}
