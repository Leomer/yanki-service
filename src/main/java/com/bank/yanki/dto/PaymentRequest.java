package com.bank.yanki.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/** Request body to pay another wallet using its phone number. */
@Data
public class PaymentRequest {

    @NotBlank(message = "fromPhone is required")
    private String fromPhone;

    @NotBlank(message = "toPhone is required")
    private String toPhone;

    @NotNull(message = "amount is required")
    @DecimalMin(value = "0.01", message = "amount must be positive")
    private BigDecimal amount;
}
