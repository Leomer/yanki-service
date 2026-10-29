# yanki-service

Mobile wallet microservice ("Yanki"). Holders do not need to be bank customers:
a document, phone number, IMEI and e-mail are enough. Spring Boot, WebFlux,
RxJava 3, MongoDB.

## Design decisions
- **Own database** (`yanki_db`), following the database-per-service pattern.
- **No REST calls to other services.** This is a new Part III service, so the
  link with the bank (debit card, primary account) is done through Kafka events.
- **The phone number is the wallet identifier** in the URLs.
- **Atomic balance changes.** Debits use a conditional update in MongoDB
  (`balance >= amount`), so concurrent payments cannot overdraw a wallet.
- **Money stored as `Decimal128`** (`MongoConfig`) so `$inc` and comparisons work.
- **Injectable `Clock`** for testable timestamps.

## Requirements
- JDK 17, Maven, MongoDB
- config-server (:8888), eureka-server (:8761)
- api-gateway (:8080) and auth-service (:8086) to call it with a JWT

## Run
1. Start MongoDB, config-server and eureka-server
2. `mvn spring-boot:run`
- Port: 8088 (from config-repo)
- Database: `yanki_db`
- Swagger UI: http://localhost:8088/swagger-ui.html
- Through the gateway: http://localhost:8080/yanki/... (Bearer token required)

## Business rules
| Rule | Detail |
|---|---|
| Registration | Document type (DNI, CEX, PASSPORT), document number, 9-digit phone, 15-digit IMEI and e-mail |
| Uniqueness | One wallet per phone number and per document number |
| Opening balance | Always 0 |
| Update | Only e-mail and IMEI can change; phone, document and balance cannot |
| Deletion | Not allowed while the wallet has balance |
| Payments | Sent and received using only the phone number |
| Funds | A payment or withdrawal is rejected if the balance is not enough |
| Receiver | Checked before any money is taken from the sender |

## Endpoints
| Method | Path | Description |
|---|---|---|
| POST | /yanki/wallets | Register a wallet |
| GET | /yanki/wallets | List wallets |
| GET | /yanki/wallets/{phoneNumber} | Find a wallet |
| PUT | /yanki/wallets/{phoneNumber} | Update e-mail and IMEI |
| DELETE | /yanki/wallets/{phoneNumber} | Delete an empty wallet (ADMIN) |
| POST | /yanki/wallets/{phoneNumber}/top-ups | Add money |
| POST | /yanki/wallets/{phoneNumber}/withdrawals | Take money out |
| POST | /yanki/payments | Pay another wallet by phone number |
| GET | /yanki/wallets/{phoneNumber}/movements | Movement history, newest first |

Request examples:

    POST /yanki/wallets
    { "documentType": "DNI", "documentNumber": "12345678", "phoneNumber": "987654321",
      "imei": "123456789012345", "email": "ana@mail.com" }

    POST /yanki/wallets/987654321/top-ups
    { "amount": 100 }

    POST /yanki/payments
    { "fromPhone": "987654321", "toPhone": "912345678", "amount": 30 }

Movement types: `TOP_UP`, `WITHDRAWAL`, `SENT`, `RECEIVED`.

## Error handling
| Status | Cause |
|---|---|
| 400 | Validation error, duplicated phone or document, insufficient funds, payment to oneself, wallet with balance on delete |
| 401 | Missing, invalid or expired JWT (gateway) |
| 403 | DELETE with a USER token (gateway) |
| 404 | Wallet not found |

## Payment flow
1. Validate that the receiver exists.
2. Debit the sender (atomic, only if funds are enough).
3. Credit the receiver. If this fails, the sender is refunded and the original
   error is returned.
4. Register the `SENT` and `RECEIVED` movements.

## Link with the bank
Event driven, no REST:
- debit-card-service publishes its cards to `debit-card-events`; yanki-service
  keeps a local copy (`known_cards`).
- `PUT /yanki/wallets/{phoneNumber}/debit-card` links a card. The card must exist
  and its holder's document must match the wallet's document.
- `DELETE /yanki/wallets/{phoneNumber}/debit-card` unlinks it (ADMIN).
- If the card is deleted in the bank, the wallet is unlinked automatically.

### Not implemented
Moving the balance of the card's primary account when topping up or withdrawing
from a linked wallet. Planned design: yanki-service would publish a request to
`yanki-events`, transaction-service would deposit or withdraw on the primary
account (same pattern as debit payments), and publish the result. Today top-ups,
withdrawals and payments between wallets only change the wallet balance.

## Documentation
- OpenAPI contract: `docs/openapi.yaml`
- Sequence diagram: `docs/sequence-yanki.puml`
- Architecture diagram: see the `bank-docs` repository

## Known limitations
- Top-ups and withdrawals are simulated: there is no real cash-in channel.
- A payment is not a distributed transaction. If the service crashes between
  the debit and the credit, the refund does not run and the money stays
  debited. An outbox or saga would fix it.
- The IMEI is stored and validated only by format (15 digits); it is not
  checked against the device.
- Wallets are not linked to bank customers, by design.
- The primary-account top-up/withdrawal is pending (see above).

## Tests
`mvn clean test`. Coverage report: `target/site/jacoco/index.html`.