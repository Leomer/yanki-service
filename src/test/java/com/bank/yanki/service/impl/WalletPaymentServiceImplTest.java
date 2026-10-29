package com.bank.yanki.service.impl;

import com.bank.yanki.dto.PaymentRequest;
import com.bank.yanki.exception.BusinessException;
import com.bank.yanki.exception.NotFoundException;
import com.bank.yanki.model.MovementType;
import com.bank.yanki.model.Wallet;
import com.bank.yanki.model.WalletMovement;
import com.bank.yanki.repository.WalletMovementRepository;
import com.bank.yanki.repository.WalletRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.UpdateDefinition;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** Unit tests for {@link WalletPaymentServiceImpl}. */
@ExtendWith(MockitoExtension.class)
class WalletPaymentServiceImplTest {

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private WalletMovementRepository movementRepository;

    @Mock
    private ReactiveMongoTemplate mongoTemplate;

    @Spy
    private Clock clock = Clock.fixed(Instant.parse("2026-10-15T10:00:00Z"), ZoneOffset.UTC);

    @InjectMocks
    private WalletPaymentServiceImpl service;

    // ---------- helpers ----------

    private Wallet wallet(String phone, String balance) {
        return Wallet.builder().phoneNumber(phone).balance(new BigDecimal(balance)).build();
    }

    private PaymentRequest payment(String from, String to, String amount) {
        PaymentRequest r = new PaymentRequest();
        r.setFromPhone(from);
        r.setToPhone(to);
        r.setAmount(new BigDecimal(amount));
        return r;
    }

    private void movementSaveReturnsSameEntity() {
        when(movementRepository.save(any(WalletMovement.class)))
                .thenAnswer(i -> Mono.just(i.getArgument(0)));
    }

    private org.mockito.stubbing.OngoingStubbing<Mono<Wallet>> atomicUpdate() {
        return when(mongoTemplate.findAndModify(any(Query.class), any(UpdateDefinition.class),
                any(FindAndModifyOptions.class), eq(Wallet.class)));
    }

    // ---------- top-up and withdrawal ----------

    @Test
    void topUpShouldRegisterMovementWithNewBalance() {
        atomicUpdate().thenReturn(Mono.just(wallet("111", "150")));
        movementSaveReturnsSameEntity();

        service.topUp("111", new BigDecimal("50"))
                .test()
                .assertValue(m -> m.getType() == MovementType.TOP_UP
                        && new BigDecimal("150").compareTo(m.getBalanceAfter()) == 0)
                .assertComplete();
    }

    @Test
    void topUpShouldFailWhenWalletDoesNotExist() {
        atomicUpdate().thenReturn(Mono.empty());
        when(walletRepository.findByPhoneNumber("000")).thenReturn(Mono.empty());

        service.topUp("000", BigDecimal.TEN).test().assertError(NotFoundException.class);
        verify(movementRepository, never()).save(any());
    }

    @Test
    void withdrawShouldRegisterMovement() {
        atomicUpdate().thenReturn(Mono.just(wallet("111", "70")));
        movementSaveReturnsSameEntity();

        service.withdraw("111", new BigDecimal("30"))
                .test()
                .assertValue(m -> m.getType() == MovementType.WITHDRAWAL)
                .assertComplete();
    }

    @Test
    void withdrawShouldFailWithInsufficientFunds() {
        atomicUpdate().thenReturn(Mono.empty());
        when(walletRepository.findByPhoneNumber("111")).thenReturn(Mono.just(wallet("111", "5")));

        service.withdraw("111", new BigDecimal("999")).test().assertError(BusinessException.class);
        verify(movementRepository, never()).save(any());
    }

    // ---------- payments ----------

    @Test
    void sendShouldMoveMoneyAndRegisterBothMovements() {
        when(walletRepository.findByPhoneNumber("222")).thenReturn(Mono.just(wallet("222", "0")));
        atomicUpdate().thenReturn(Mono.just(wallet("111", "50")))
                .thenReturn(Mono.just(wallet("222", "50")));
        movementSaveReturnsSameEntity();

        service.send(payment("111", "222", "50"))
                .test()
                .assertValue(r -> r.getSent().getType() == MovementType.SENT
                        && r.getReceived().getType() == MovementType.RECEIVED
                        && "222".equals(r.getSent().getCounterpartPhone())
                        && new BigDecimal("50").compareTo(r.getSent().getBalanceAfter()) == 0
                        && new BigDecimal("50").compareTo(r.getReceived().getBalanceAfter()) == 0)
                .assertComplete();
    }

    @Test
    void sendShouldFailWhenSenderAndReceiverAreTheSame() {
        service.send(payment("111", "111", "50")).test().assertError(BusinessException.class);
        verifyNoInteractions(mongoTemplate);
    }

    @Test
    void sendShouldNotTouchBalancesWhenReceiverDoesNotExist() {
        when(walletRepository.findByPhoneNumber("222")).thenReturn(Mono.empty());

        service.send(payment("111", "222", "50")).test().assertError(NotFoundException.class);
        verifyNoInteractions(mongoTemplate);
    }

    @Test
    void sendShouldNotCreditReceiverWhenSenderHasInsufficientFunds() {
        when(walletRepository.findByPhoneNumber("222")).thenReturn(Mono.just(wallet("222", "0")));
        when(walletRepository.findByPhoneNumber("111")).thenReturn(Mono.just(wallet("111", "10")));
        atomicUpdate().thenReturn(Mono.empty());

        service.send(payment("111", "222", "50")).test().assertError(BusinessException.class);

        verify(mongoTemplate, times(1)).findAndModify(any(Query.class),
                any(UpdateDefinition.class), any(FindAndModifyOptions.class), eq(Wallet.class));
        verify(movementRepository, never()).save(any());
    }

    @Test
    void sendShouldRefundSenderWhenCreditToReceiverFails() {
        when(walletRepository.findByPhoneNumber("222")).thenReturn(Mono.just(wallet("222", "0")));
        atomicUpdate().thenReturn(Mono.just(wallet("111", "50")))           // debit
                .thenReturn(Mono.error(new RuntimeException("db down")))    // credit fails
                .thenReturn(Mono.just(wallet("111", "100")));               // refund

        service.send(payment("111", "222", "50")).test().assertError(RuntimeException.class);

        verify(mongoTemplate, times(3)).findAndModify(any(Query.class),
                any(UpdateDefinition.class), any(FindAndModifyOptions.class), eq(Wallet.class));
        verify(movementRepository, never()).save(any());
    }

    // ---------- history ----------

    @Test
    void movementsShouldReturnTheHistoryOfTheWallet() {
        when(movementRepository.findByPhoneNumberOrderByDateDesc("111"))
                .thenReturn(Flux.just(new WalletMovement(), new WalletMovement()));

        service.movements("111").test().assertValueCount(2).assertComplete();
    }
}
