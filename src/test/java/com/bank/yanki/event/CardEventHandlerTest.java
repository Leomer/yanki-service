package com.bank.yanki.event;

import com.bank.yanki.model.KnownCard;
import com.bank.yanki.model.Wallet;
import com.bank.yanki.repository.KnownCardRepository;
import com.bank.yanki.repository.WalletRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Unit tests for {@link CardEventHandler}. */
@ExtendWith(MockitoExtension.class)
class CardEventHandlerTest {

    @Mock
    private KnownCardRepository cardRepository;

    @Mock
    private WalletRepository walletRepository;

    @InjectMocks
    private CardEventHandler handler;

    @Test
    void cardEventShouldStoreTheCard() {
        when(cardRepository.save(any(KnownCard.class))).thenAnswer(i -> Mono.just(i.getArgument(0)));

        handler.handle("CARD|DC1|123|ACC1").block();

        verify(cardRepository).save(argThat(c -> "DC1".equals(c.getCardNumber())
                && "123".equals(c.getCustomerDocument()) && "ACC1".equals(c.getPrimaryAccount())));
    }

    @Test
    void deletedCardShouldUnlinkWalletsAndRemoveTheCard() {
        Wallet linked = Wallet.builder().phoneNumber("111").debitCardNumber("DC1").build();
        when(walletRepository.findByDebitCardNumber("DC1")).thenReturn(Flux.just(linked));
        when(walletRepository.save(any(Wallet.class))).thenAnswer(i -> Mono.just(i.getArgument(0)));
        when(cardRepository.deleteById("DC1")).thenReturn(Mono.empty());

        handler.handle("CARD_DELETED|DC1").block();

        verify(walletRepository).save(argThat(w -> w.getDebitCardNumber() == null));
        verify(cardRepository).deleteById("DC1");
    }

    @Test
    void unknownOrMalformedEventShouldBeIgnored() {
        handler.handle("CARD|DC1").block();
        handler.handle("WHATEVER|X").block();

        verify(cardRepository, never()).save(any());
    }
}
