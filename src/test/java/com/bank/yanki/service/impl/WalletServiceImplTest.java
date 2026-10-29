package com.bank.yanki.service.impl;

import com.bank.yanki.dto.WalletDto;
import com.bank.yanki.exception.BusinessException;
import com.bank.yanki.exception.NotFoundException;
import com.bank.yanki.model.DocumentType;
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

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Unit tests for {@link WalletServiceImpl}. */
@ExtendWith(MockitoExtension.class)
class WalletServiceImplTest {

    @Mock
    private WalletRepository repository;
    @Mock
    private KnownCardRepository knownCardRepository;

    @InjectMocks
    private WalletServiceImpl service;

    private WalletDto request() {
        return WalletDto.builder().documentType(DocumentType.DNI).documentNumber("12345678")
                .phoneNumber("987654321").imei("123456789012345").email("a@mail.com").build();
    }

    private Wallet wallet(String balance) {
        return Wallet.builder().id("1").documentType(DocumentType.DNI).documentNumber("12345678")
                .phoneNumber("987654321").imei("123456789012345").email("a@mail.com")
                .balance(new BigDecimal(balance)).build();
    }

    @Test
    void createShouldStartWithZeroBalance() {
        when(repository.existsByPhoneNumber("987654321")).thenReturn(Mono.just(false));
        when(repository.existsByDocumentNumber("12345678")).thenReturn(Mono.just(false));
        when(repository.save(any(Wallet.class))).thenAnswer(i -> Mono.just(i.getArgument(0)));

        service.create(request())
                .test()
                .assertValue(w -> BigDecimal.ZERO.compareTo(w.getBalance()) == 0
                        && w.getDebitCardNumber() == null)
                .assertComplete();
    }

    @Test
    void createShouldFailWhenPhoneIsAlreadyRegistered() {
        when(repository.existsByPhoneNumber("987654321")).thenReturn(Mono.just(true));

        service.create(request()).test().assertError(BusinessException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void createShouldFailWhenDocumentIsAlreadyRegistered() {
        when(repository.existsByPhoneNumber("987654321")).thenReturn(Mono.just(false));
        when(repository.existsByDocumentNumber("12345678")).thenReturn(Mono.just(true));

        service.create(request()).test().assertError(BusinessException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void findByPhoneNumberShouldFailWhenMissing() {
        when(repository.findByPhoneNumber("000")).thenReturn(Mono.empty());

        service.findByPhoneNumber("000").test().assertError(NotFoundException.class);
    }

    @Test
    void findAllShouldReturnWallets() {
        when(repository.findAll()).thenReturn(Flux.just(wallet("0"), wallet("5")));

        service.findAll().test().assertValueCount(2).assertComplete();
    }

    @Test
    void updateShouldOnlyChangeEmailAndImei() {
        when(repository.findByPhoneNumber("987654321")).thenReturn(Mono.just(wallet("50")));
        when(repository.save(any(Wallet.class))).thenAnswer(i -> Mono.just(i.getArgument(0)));

        WalletDto dto = WalletDto.builder().documentNumber("99999999").phoneNumber("111111111")
                .email("new@mail.com").imei("999999999999999").balance(new BigDecimal("1000")).build();

        service.update("987654321", dto)
                .test()
                .assertValue(w -> "new@mail.com".equals(w.getEmail())
                        && "999999999999999".equals(w.getImei())
                        && "12345678".equals(w.getDocumentNumber())
                        && "987654321".equals(w.getPhoneNumber())
                        && new BigDecimal("50").compareTo(w.getBalance()) == 0)
                .assertComplete();
    }

    @Test
    void deleteShouldFailWhenWalletHasBalance() {
        when(repository.findByPhoneNumber("987654321")).thenReturn(Mono.just(wallet("10")));

        service.delete("987654321").test().assertError(BusinessException.class);
        verify(repository, never()).delete(any());
    }

    @Test
    void deleteShouldRemoveEmptyWallet() {
        Wallet w = wallet("0");
        when(repository.findByPhoneNumber("987654321")).thenReturn(Mono.just(w));
        when(repository.delete(w)).thenReturn(Mono.empty());

        service.delete("987654321").test().assertComplete();
        verify(repository).delete(w);
    }

    private KnownCard card(String customer) {
        return KnownCard.builder().cardNumber("DC1").customerDocument(customer)
                .primaryAccount("ACC1").build();
    }

    @Test
    void linkCardShouldAssociateWhenCardBelongsToTheHolder() {
        when(repository.findByPhoneNumber("987654321")).thenReturn(Mono.just(wallet("0")));
        when(knownCardRepository.findById("DC1")).thenReturn(Mono.just(card("12345678")));
        when(repository.save(any(Wallet.class))).thenAnswer(i -> Mono.just(i.getArgument(0)));

        service.linkCard("987654321", "DC1")
                .test()
                .assertValue(w -> "DC1".equals(w.getDebitCardNumber()))
                .assertComplete();
    }

    @Test
    void linkCardShouldFailWhenCardBelongsToAnotherPerson() {
        when(repository.findByPhoneNumber("987654321")).thenReturn(Mono.just(wallet("0")));
        when(knownCardRepository.findById("DC1")).thenReturn(Mono.just(card("99999999")));

        service.linkCard("987654321", "DC1").test().assertError(BusinessException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void linkCardShouldFailWhenCardIsUnknown() {
        when(repository.findByPhoneNumber("987654321")).thenReturn(Mono.just(wallet("0")));
        when(knownCardRepository.findById("X")).thenReturn(Mono.empty());

        service.linkCard("987654321", "X").test().assertError(BusinessException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void unlinkCardShouldClearTheCard() {
        Wallet linked = wallet("0");
        linked.setDebitCardNumber("DC1");
        when(repository.findByPhoneNumber("987654321")).thenReturn(Mono.just(linked));
        when(repository.save(any(Wallet.class))).thenAnswer(i -> Mono.just(i.getArgument(0)));

        service.unlinkCard("987654321")
                .test()
                .assertValue(w -> w.getDebitCardNumber() == null)
                .assertComplete();
    }
}
