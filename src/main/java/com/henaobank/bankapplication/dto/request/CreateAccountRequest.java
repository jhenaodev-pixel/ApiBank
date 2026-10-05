package com.henaobank.bankapplication.dto.request;

import com.henaobank.bankapplication.entity.enums.AccountType;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateAccountRequest {

    @NotNull(message = "El tipo de cuenta es obligatorio ahorros o corriente")
    private AccountType accountType;
}
