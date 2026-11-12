package com.henaobank.bankapplication.repository;

import com.henaobank.bankapplication.entity.InvestmentPosition;
import com.henaobank.bankapplication.entity.enums.AssetType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface InvestmentPositionRepository extends JpaRepository<InvestmentPosition, Long> {

    List<InvestmentPosition> findByUserIdOrderBySymbolAsc(Long userId);

    Optional<InvestmentPosition> findByUserIdAndSymbolAndAssetType(Long userId, String symbol, AssetType assetType);

    /**
     * Bloquea la posición durante la transacción.
     *
     * SQL nativo por el mismo motivo que en {@link AccountRepository}: el alias
     * que genera Hibernate con {@code @Lock} hace que TiDB Cloud responda
     * "Table 'db_bank.xxx' doesn't exist". El enum viaja como texto porque la
     * columna es VARCHAR.
     */
    @Query(value = "SELECT * FROM investment_positions "
            + "WHERE user_id = :userId AND symbol = :symbol AND asset_type = :assetType FOR UPDATE",
            nativeQuery = true)
    Optional<InvestmentPosition> findForUpdate(@Param("userId") Long userId,
                                               @Param("symbol") String symbol,
                                               @Param("assetType") String assetType);
}
