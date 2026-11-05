package com.henaobank.bankapplication.repository;

import com.henaobank.bankapplication.entity.InvestmentPosition;
import com.henaobank.bankapplication.entity.enums.AssetType;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface InvestmentPositionRepository extends JpaRepository<InvestmentPosition, Long> {

    List<InvestmentPosition> findByUserIdOrderBySymbolAsc(Long userId);

    Optional<InvestmentPosition> findByUserIdAndSymbolAndAssetType(Long userId, String symbol, AssetType assetType);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM InvestmentPosition p "
            + "WHERE p.user.id = :userId AND p.symbol = :symbol AND p.assetType = :assetType")
    Optional<InvestmentPosition> findForUpdate(@Param("userId") Long userId,
                                               @Param("symbol") String symbol,
                                               @Param("assetType") AssetType assetType);
}
