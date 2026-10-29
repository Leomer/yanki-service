package com.bank.yanki.dto;

import com.bank.yanki.model.DocumentType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/** Data transfer object for wallets. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalletDto {

    private String id;

    @NotNull(message = "documentType is required")
    private DocumentType documentType;

    @NotBlank(message = "documentNumber is required")
    private String documentNumber;

    @NotBlank(message = "phoneNumber is required")
    @Pattern(regexp = "\\d{9}", message = "phoneNumber must have 9 digits")
    private String phoneNumber;

    @NotBlank(message = "imei is required")
    @Pattern(regexp = "\\d{15}", message = "imei must have 15 digits")
    private String imei;

    @NotBlank(message = "email is required")
    @Email(message = "email is invalid")
    private String email;

    private BigDecimal balance;

    private String debitCardNumber;
}