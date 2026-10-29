package com.bank.yanki.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** Request to link a debit card to a wallet. */
@Data
public class LinkCardRequest {

    @NotBlank(message = "cardNumber is required")
    private String cardNumber;
}
