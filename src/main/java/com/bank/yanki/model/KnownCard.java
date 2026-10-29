package com.bank.yanki.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/** Local copy of a debit card announced by debit-card-service. The id is the card number. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "known_cards")
public class KnownCard {

    @Id
    private String cardNumber;

    private String customerDocument;

    private String primaryAccount;
}