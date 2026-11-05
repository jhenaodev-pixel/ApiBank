package com.henaobank.bankapplication.service.impl;

import com.henaobank.bankapplication.dto.request.InvestmentTradeRequest;
import com.henaobank.bankapplication.dto.response.InvestmentPositionResponse;
import com.henaobank.bankapplication.dto.response.InvestmentTradeResponse;
import com.henaobank.bankapplication.dto.response.PortfolioResponse;
import com.henaobank.bankapplication.entity.InvestmentPosition;
import com.henaobank.bankapplication.entity.InvestmentTrade;
import com.henaobank.bankapplication.entity.User;
import com.henaobank.bankapplication.entity.enums.AssetType;
import com.henaobank.bankapplication.exception.BadRequestException;
import com.henaobank.bankapplication.exception.MarketDataException;
import com.henaobank.bankapplication.exception.ResourceNotFoundException;
import com.henaobank.bankapplication.repository.InvestmentPositionRepository;
import com.henaobank.bankapplication.repository.InvestmentTradeRepository;
import com.henaobank.bankapplication.repository.UserRepository;
import com.henaobank.bankapplication.service.InvestmentService;
import com.henaobank.bankapplication.service.MarketDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InvestmentServiceImpl implements InvestmentService {

    private static final int QUANTITY_SCALE = 8;
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final UserRepository userRepository;
    private final InvestmentPositionRepository positionRepository;
    private final InvestmentTradeRepository tradeRepository;
    private final MarketDataService marketDataService;
    private final InvestmentTradeExecutor tradeExecutor;

    // ------------------------------------------------------------------ comprar / vender

    @Override
    public InvestmentTradeResponse buy(InvestmentTradeRequest request, String email) {
        User user = findUser(email);
        AssetType assetType = requireAssetType(request.getAssetType());
        String symbol = marketDataService.normalizeSymbol(assetType, request.getSymbol());

        if (request.getQuantity() != null && request.getAmount() != null) {
            throw new BadRequestException("Indique quantity (unidades) o amount (dinero a invertir), pero no ambos");
        }
        if (request.getQuantity() == null && request.getAmount() == null) {
            throw new BadRequestException("Debe indicar quantity o amount para realizar la compra");
        }

        BigDecimal price = currentPrice(assetType, symbol);

        BigDecimal quantity;
        BigDecimal total;

        if (request.getQuantity() != null) {
            quantity = request.getQuantity().setScale(QUANTITY_SCALE, RoundingMode.HALF_UP);
            total = money(price.multiply(quantity));
        } else {
            total = money(request.getAmount());
            quantity = total.divide(price, QUANTITY_SCALE, RoundingMode.HALF_DOWN);
        }

        if (quantity.signum() <= 0) {
            throw new BadRequestException("La cantidad calculada es demasiado pequeña para " + symbol);
        }
        if (total.compareTo(new BigDecimal("0.01")) < 0) {
            throw new BadRequestException("El total de la compra debe ser de al menos 0.01");
        }

        return tradeExecutor.buy(user.getId(), request.getAccountNumber(), symbol, assetType,
                quantity, price, total, request.getDescription());
    }

    @Override
    public InvestmentTradeResponse sell(InvestmentTradeRequest request, String email) {
        User user = findUser(email);
        AssetType assetType = requireAssetType(request.getAssetType());
        String symbol = marketDataService.normalizeSymbol(assetType, request.getSymbol());

        if (request.getAmount() != null) {
            throw new BadRequestException("Para vender indique quantity (unidades). El campo amount no aplica.");
        }
        if (request.getQuantity() == null || request.getQuantity().signum() <= 0) {
            throw new BadRequestException("Debe indicar una quantity mayor que cero para vender");
        }

        BigDecimal quantity = request.getQuantity().setScale(QUANTITY_SCALE, RoundingMode.HALF_UP);

        InvestmentPosition position = positionRepository
                .findByUserIdAndSymbolAndAssetType(user.getId(), symbol, assetType)
                .orElseThrow(() -> new BadRequestException("No tienes una posición abierta en " + symbol));

        if (position.getQuantity().compareTo(quantity) < 0) {
            throw new BadRequestException("Solo tienes "
                    + position.getQuantity().stripTrailingZeros().toPlainString()
                    + " " + symbol + " disponibles para vender");
        }

        BigDecimal price = currentPrice(assetType, symbol);
        BigDecimal total = money(price.multiply(quantity));

        if (total.signum() <= 0) {
            throw new BadRequestException("El total de la venta debe ser mayor que cero");
        }

        return tradeExecutor.sell(user.getId(), request.getAccountNumber(), symbol, assetType,
                quantity, price, total, request.getDescription());
    }

    // ------------------------------------------------------------------ portafolio

    /**
     * No se anota con @Transactional a propósito: primero se obtienen las
     * posiciones y luego se consultan los precios externos, sin mantener una
     * transacción de base de datos abierta durante las llamadas HTTP.
     */
    @Override
    public PortfolioResponse getPortfolio(String email) {
        User user = findUser(email);
        List<InvestmentPosition> positions =
                positionRepository.findByUserIdOrderBySymbolAsc(user.getId());

        if (positions.isEmpty()) {
            return PortfolioResponse.builder()
                    .totalInvested(money(BigDecimal.ZERO))
                    .totalCurrentValue(money(BigDecimal.ZERO))
                    .totalProfitLoss(money(BigDecimal.ZERO))
                    .fullyValued(true)
                    .valuedAt(LocalDateTime.now(ZoneOffset.UTC))
                    .positions(List.of())
                    .build();
        }

        Map<String, BigDecimal> prices = loadPrices(positions);

        BigDecimal totalInvested = BigDecimal.ZERO;
        BigDecimal totalCurrentValue = BigDecimal.ZERO;
        BigDecimal investedValued = BigDecimal.ZERO;
        boolean fullyValued = true;

        List<InvestmentPositionResponse> responses = new ArrayList<>();

        for (InvestmentPosition position : positions) {
            BigDecimal invested = position.getInvestedAmount();
            totalInvested = totalInvested.add(invested);

            BigDecimal currentPrice = prices.get(position.getSymbol());
            BigDecimal currentValue = null;
            BigDecimal profitLoss = null;
            BigDecimal profitLossPercent = null;

            if (currentPrice != null) {
                currentValue = money(currentPrice.multiply(position.getQuantity()));
                profitLoss = money(currentValue.subtract(invested));
                profitLossPercent = percent(profitLoss, invested);

                totalCurrentValue = totalCurrentValue.add(currentValue);
                investedValued = investedValued.add(invested);
            } else {
                fullyValued = false;
            }

            responses.add(InvestmentPositionResponse.builder()
                    .id(position.getId())
                    .symbol(position.getSymbol())
                    .assetType(position.getAssetType())
                    .quantity(position.getQuantity())
                    .averagePrice(position.getAveragePrice())
                    .investedAmount(invested)
                    .currentPrice(currentPrice)
                    .currentValue(currentValue)
                    .profitLoss(profitLoss)
                    .profitLossPercent(profitLossPercent)
                    .build());
        }

        // La rentabilidad se calcula solo sobre las posiciones que se pudieron valorizar
        BigDecimal totalProfitLoss = money(totalCurrentValue.subtract(investedValued));

        return PortfolioResponse.builder()
                .totalInvested(money(totalInvested))
                .totalCurrentValue(money(totalCurrentValue))
                .totalProfitLoss(totalProfitLoss)
                .totalProfitLossPercent(percent(totalProfitLoss, investedValued))
                .fullyValued(fullyValued)
                .valuedAt(LocalDateTime.now(ZoneOffset.UTC))
                .positions(responses)
                .build();
    }

    @Override
    public List<InvestmentPositionResponse> getPositions(String email) {
        return getPortfolio(email).getPositions();
    }

    @Override
    @Transactional(readOnly = true)
    public List<InvestmentTradeResponse> getHistory(String email, String symbol, AssetType assetType) {
        User user = findUser(email);

        List<InvestmentTrade> trades;
        if (StringUtils.hasText(symbol)) {
            String normalized = assetType != null
                    ? marketDataService.normalizeSymbol(assetType, symbol)
                    : symbol.trim().toUpperCase(Locale.ROOT);
            trades = tradeRepository.findByUserIdAndSymbolOrderByTimestampDesc(user.getId(), normalized);
        } else {
            trades = tradeRepository.findByUserIdOrderByTimestampDesc(user.getId());
        }

        return trades.stream().map(this::mapToResponse).toList();
    }

    // ------------------------------------------------------------------ utilidades

    private Map<String, BigDecimal> loadPrices(List<InvestmentPosition> positions) {
        Map<String, BigDecimal> prices = new HashMap<>();

        positions.stream()
                .collect(Collectors.groupingBy(InvestmentPosition::getAssetType))
                .forEach((assetType, group) -> prices.putAll(marketDataService.getPrices(
                        assetType,
                        group.stream().map(InvestmentPosition::getSymbol).toList())));

        return prices;
    }

    private BigDecimal currentPrice(AssetType assetType, String symbol) {
        BigDecimal price = marketDataService.getPrice(assetType, symbol);
        if (price == null || price.signum() <= 0) {
            throw new MarketDataException("No hay un precio disponible para " + symbol);
        }
        return price;
    }

    private BigDecimal money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal percent(BigDecimal part, BigDecimal base) {
        if (base == null || base.signum() == 0) {
            return null;
        }
        return part.multiply(HUNDRED).divide(base, 2, RoundingMode.HALF_UP);
    }

    private AssetType requireAssetType(AssetType assetType) {
        if (assetType == null) {
            throw new BadRequestException("El tipo de activo es obligatorio (CRYPTO o STOCK)");
        }
        return assetType;
    }

    private User findUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Usuario no encontrado con el email: " + email));
    }

    private InvestmentTradeResponse mapToResponse(InvestmentTrade trade) {
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
                .description(trade.getDescription())
                .timestamp(trade.getTimestamp())
                .build();
    }
}
