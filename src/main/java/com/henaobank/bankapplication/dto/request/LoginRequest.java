package com.henaobank.bankapplication.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginRequest {

    @Email(message = "Debe proporcionar un email valido")
    @NotBlank(message = "El email no puede estar vacio")
    private String email;

    @NotBlank(message = "La contraseña no puede estar vacia")
    private String password;
}
