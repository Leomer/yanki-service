package com.bank.yanki.event;

import com.bank.yanki.model.KnownCard;
import com.bank.yanki.repository.KnownCardRepository;
import com.bank.yanki.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/** Applies debit card events to the local copy of cards. */
@Slf4j
@Component
@RequiredArgsConstructor
public class CardEventHandler {

    private final KnownCardRepository repository;
    private final WalletRepository walletRepository;

    /** Handles CARD|number|customer|primaryAccount and CARD_DELETED|number. */
    public Mono<Void> handle(String message) {
        String[] parts = message.split("\\|", -1);
        if ("CARD".equals(parts[0]) && parts.length == 4) {
            log.info("Card event: {} (primary account {})", parts[1], parts[3]);
            return repository.save(KnownCard.builder()
                    .cardNumber(parts[1])
                    .customerDocument(parts[2])
                    .primaryAccount(parts[3])
                    .build()).then();
        }
        if ("CARD_DELETED".equals(parts[0]) && parts.length == 2) {
            log.info("Card deleted event: {}", parts[1]);
            // wallets that used the card are unlinked
            return walletRepository.findByDebitCardNumber(parts[1])
                    .flatMap(wallet -> {
                        wallet.setDebitCardNumber(null);
                        return walletRepository.save(wallet);
                    })
                    .then(repository.deleteById(parts[1]));
        }
        log.warn("Unknown or malformed card event ignored: {}", message);
        return Mono.empty();
    }
}
