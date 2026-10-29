package com.bank.yanki.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;

/** Mobile wallet. The holder does not need to be a bank customer. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "wallets")
public class Wallet {

    @Id
    private String id;

    private DocumentType documentType;

    @Indexed(unique = true)
    private String documentNumber;

    @Indexed(unique = true)
    private String phoneNumber;

    private String imei;

    private String email;

    private BigDecimal balance;

    /** Debit card linked to the wallet (set in D3). */
    private String debitCardNumber;
}