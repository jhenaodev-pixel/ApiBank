package com.henaobank.bankapplication.entity;

import com.henaobank.bankapplication.entity.enums.AssetType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Posición de inversión de un usuario en un activo (criptomoneda o acción).
 * Se guarda el costo total invertido y el precio promedio de compra para poder
 * calcular la rentabilidad cuando se consulte el portafolio.
 */
@Entity
@Table(name = "investment_positions",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_position_user_asset",
                columnNames = {"user_id", "symbol", "asset_type"}))
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class InvestmentPosition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 30)
    private String symbol;

    @Enumerated(EnumType.STRING)
    @Column(name = "asset_type", nullable = false, length = 20)
    private AssetType assetType;

    @Column(nullable = false, precision = 24, scale = 10)
    private BigDecimal quantity;

    @Column(nullable = false, precision = 24, scale = 10)
    private BigDecimal averagePrice;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal investedAmount;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
