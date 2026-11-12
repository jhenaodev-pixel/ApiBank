package com.henaobank.bankapplication.repository;

import com.henaobank.bankapplication.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Long> {

    Optional<Account> findByAccountNumber(String accountNumber);

    List<Account> findByUserId(Long userId);

    boolean existsByAccountNumber(String accountNumber);

    /**
     * Bloquea la fila de la cuenta durante la transacción.
     *
     * SQL nativo a propósito: con {@code @Lock(PESSIMISTIC_WRITE)} y JPQL,
     * Hibernate 7 genera un alias ("... from accounts a1_0 ... for update") y
     * TiDB Cloud responde "Table 'db_bank.a1_0' doesn't exist", tumbando
     * depósitos, retiros, transferencias y compras. Sin alias no hay problema.
     */
    @Query(value = "SELECT * FROM accounts WHERE account_number = :accountNumber FOR UPDATE",
            nativeQuery = true)
    Optional<Account> findByAccountNumberForUpdate(@Param("accountNumber") String accountNumber);

}
