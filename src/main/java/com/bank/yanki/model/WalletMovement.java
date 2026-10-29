package com.bank.yanki.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Movement registered against a wallet. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "wallet_movements")
public class WalletMovement {

    @Id
    private String id;

    @Indexed
    private String phoneNumber;

    /** Other wallet in a payment (null for top-ups and withdrawals). */
    private String counterpartPhone;

    private MovementType type;

    private BigDecimal amount;

    /** Wallet balance right after the operation. */
    private BigDecimal balanceAfter;

    private LocalDateTime date;
}
