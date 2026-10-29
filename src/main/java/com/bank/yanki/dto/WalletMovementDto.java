package com.bank.yanki.dto;

import com.bank.yanki.model.MovementType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Wallet movement returned by the API. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalletMovementDto {
    private String id;
    private String phoneNumber;
    private String counterpartPhone;
    private MovementType type;
    private BigDecimal amount;
    private BigDecimal balanceAfter;
    private LocalDateTime date;
}
