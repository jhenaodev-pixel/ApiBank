package com.henaobank.bankapplication.service.impl;

import com.henaobank.bankapplication.dto.response.InvestmentTradeResponse;
import com.henaobank.bankapplication.entity.Account;
import com.henaobank.bankapplication.entity.InvestmentPosition;
import com.henaobank.bankapplication.entity.InvestmentTrade;
import com.henaobank.bankapplication.entity.Transaction;
import com.henaobank.bankapplication.entity.enums.AssetType;
import com.henaobank.bankapplication.entity.enums.TradeType;
import com.henaobank.bankapplication.entity.enums.TransactionType;
import com.henaobank.bankapplication.exception.BadRequestException;
import com.henaobank.bankapplication.exception.InsufficientBalanceException;
import com.henaobank.bankapplication.exception.ResourceNotFoundException;
import com.henaobank.bankapplication.repository.AccountRepository;
import com.henaobank.bankapplication.repository.InvestmentPositionRepository;
import com.henaobank.bankapplication.repository.InvestmentTradeRepository;
import com.henaobank.bankapplication.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

/**
 * Ejecuta las compras y ventas dentro de una transacción de base de datos.
 * Se separó del servicio principal para que la llamada a Binance/Finnhub
 * (lenta y externa) ocurra antes de abrir la transacción y bloquear filas.
 */
@Service
@RequiredArgsConstructor
public class InvestmentTradeExecutor {

    private static final int PRICE_SCALE = 10;

    private final AccountRepository accountRepository;
    private final InvestmentPositionRepository positionRepository;
    private final InvestmentTradeRepository tradeRepository;
    private final TransactionRepository transactionRepository;

    @Transactional
    public InvestmentTradeResponse buy(Long userId, String accountNumber, String symbol, AssetType assetType,
                                       BigDecimal quantity, BigDecimal price, BigDecimal total, String description) {

        Account account = lockAccount(accountNumber, userId);

        if (account.getBalance().compareTo(total) < 0) {
            throw new InsufficientBalanceException("Saldo insuficiente en la cuenta " + accountNumber
                    + ". Requerido: " + total + ", disponible: " + account.getBalance());
        }
        account.setBalance(account.getBalance().subtract(total));

        InvestmentPosition position = positionRepository.findForUpdate(userId, symbol, assetType.name())
                .orElseGet(() -> InvestmentPosition.builder()
                        .user(account.getUser())
                        .symbol(symbol)
                        .assetType(assetType)
                        .quantity(BigDecimal.ZERO)
                        .averagePrice(BigDecimal.ZERO)
                        .investedAmount(BigDecimal.ZERO)
                        .build());

        BigDecimal newQuantity = position.getQuantity().add(quantity);
        BigDecimal newInvested = position.getInvestedAmount().add(total);

        position.setQuantity(newQuantity);
        position.setInvestedAmount(newInvested);
        position.setAveragePrice(newInvested.divide(newQuantity, PRICE_SCALE, RoundingMode.HALF_UP));
        positionRepository.save(position);

        transactionRepository.save(Transaction.builder()
                .reference(UUID.randomUUID().toString())
                .sourceAccount(account)
                .amount(total)
                .transactionType(TransactionType.INVESTMENT_BUY)
                .description(withNote("Compra de " + quantityText(quantity) + " " + symbol, description))
                .build());

        InvestmentTrade trade = tradeRepository.save(InvestmentTrade.builder()
                .reference(UUID.randomUUID().toString())
                .user(account.getUser())
                .symbol(symbol)
                .assetType(assetType)
                .tradeType(TradeType.BUY)
                .quantity(quantity)
                .price(price)
                .totalAmount(total)
                .account(account)
                .description(description)
                .build());

        return mapToResponse(trade, account.getBalance());
    }

    @Transactional
    public InvestmentTradeResponse sell(Long userId, String accountNumber, String symbol, AssetType assetType,
                                        BigDecimal quantity, BigDecimal price, BigDecimal total, String description) {

        Account account = lockAccount(accountNumber, userId);

        InvestmentPosition position = positionRepository.findForUpdate(userId, symbol, assetType.name())
                .orElseThrow(() -> new BadRequestException("No tienes una posición abierta en " + symbol));

        if (position.getQuantity().compareTo(quantity) < 0) {
            throw new BadRequestException("Solo tienes " + quantityText(position.getQuantity())
                    + " " + symbol + " disponibles para vender");
        }

        // Costo de las unidades vendidas según el precio promedio de compra
        BigDecimal costBasis = position.getAveragePrice()
                .multiply(quantity)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal realizedProfit = total.subtract(costBasis);

        BigDecimal remainingQuantity = position.getQuantity().subtract(quantity);
        if (remainingQuantity.signum() == 0) {
            positionRepository.delete(position);
        } else {
            BigDecimal remainingInvested = position.getInvestedAmount()
                    .subtract(costBasis)
                    .max(BigDecimal.ZERO);

            position.setQuantity(remainingQuantity);
            position.setInvestedAmount(remainingInvested);
            position.setAveragePrice(remainingInvested.divide(remainingQuantity, PRICE_SCALE, RoundingMode.HALF_UP));
            positionRepository.save(position);
        }

        account.setBalance(account.getBalance().add(total));

        transactionRepository.save(Transaction.builder()
                .reference(UUID.randomUUID().toString())
                .destinationAccount(account)
                .amount(total)
                .transactionType(TransactionType.INVESTMENT_SELL)
                .description(withNote("Venta de " + quantityText(quantity) + " " + symbol, description))
                .build());

        InvestmentTrade trade = tradeRepository.save(InvestmentTrade.builder()
                .reference(UUID.randomUUID().toString())
                .user(account.getUser())
                .symbol(symbol)
                .assetType(assetType)
                .tradeType(TradeType.SELL)
                .quantity(quantity)
                .price(price)
                .totalAmount(total)
                .realizedProfit(realizedProfit)
                .account(account)
                .description(description)
                .build());

        return mapToResponse(trade, account.getBalance());
    }

    private Account lockAccount(String accountNumber, Long userId) {
        Account account = accountRepository.findByAccountNumberForUpdate(accountNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Cuenta no encontrada: " + accountNumber));

        if (!account.getUser().getId().equals(userId)) {
            // No revelamos que la cuenta existe si pertenece a otro usuario
            throw new ResourceNotFoundException("Cuenta no encontrada: " + accountNumber);
        }
        return account;
    }

    private InvestmentTradeResponse mapToResponse(InvestmentTrade trade, BigDecimal accountBalance) {
        return InvestmentTradeResponse.builder()
                .id(trade.getId())
                .reference(trade.getReference())
                .symbol(trade.getSymbol())
                .assetType(trade.getAssetType())
                .tradeType(trade.getTradeType())
                .quantity(trade.getQuantity())
                .price(trade.getPrice())
                .totalAmount(trade.getTotalAmount())
                .realizedProfit(trade.getRealizedProfit())
                .accountNumber(trade.getAccount().getAccountNumber())
                .accountBalance(accountBalance)
                .description(trade.getDescription())
                .timestamp(trade.getTimestamp())
                .build();
    }

    private String quantityText(BigDecimal quantity) {
        return quantity.stripTrailingZeros().toPlainString();
    }

    private String withNote(String base, String note) {
        if (!StringUtils.hasText(note)) {
            return base;
        }
        String result = base + " - " + note.trim();
        return result.length() > 255 ? result.substring(0, 255) : result;
    }
}
