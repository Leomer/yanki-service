package com.bank.yanki.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Result of a payment: the sender's and the receiver's movements. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponse {
    private WalletMovementDto sent;
    private WalletMovementDto received;
}
