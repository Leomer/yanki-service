package com.bank.yanki.service.impl;

import com.bank.yanki.dto.PaymentRequest;
import com.bank.yanki.dto.PaymentResponse;
import com.bank.yanki.dto.WalletMovementDto;
import com.bank.yanki.exception.BusinessException;
import com.bank.yanki.exception.NotFoundException;
import com.bank.yanki.model.MovementType;
import com.bank.yanki.model.Wallet;
import com.bank.yanki.model.WalletMovement;
import com.bank.yanki.repository.WalletMovementRepository;
import com.bank.yanki.repository.WalletRepository;
import com.bank.yanki.service.WalletPaymentService;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

import java.time.Clock;
import java.time.LocalDateTime;

/** Default implementation of {@link WalletPaymentService}. */
@Slf4j
@Service
@RequiredArgsConstructor
public class WalletPaymentServiceImpl implements WalletPaymentService {

    private final WalletRepository walletRepository;
    private final WalletMovementRepository movementRepository;
    private final ReactiveMongoTemplate mongoTemplate;
    private final Clock clock;

    @Override
    public Single<WalletMovementDto> topUp(String phoneNumber, BigDecimal amount) {
        log.info("Top-up of {} to wallet {}", amount, phoneNumber);
        return adjust(phoneNumber, amount)
                .flatMap(w -> saveMovement(phoneNumber, null, MovementType.TOP_UP, amount, w.getBalance()))
                .map(this::toDto);
    }

    @Override
    public Single<WalletMovementDto> withdraw(String phoneNumber, BigDecimal amount) {
        log.info("Withdrawal of {} from wallet {}", amount, phoneNumber);
        return adjust(phoneNumber, amount.negate())
                .flatMap(w -> saveMovement(phoneNumber, null, MovementType.WITHDRAWAL, amount, w.getBalance()))
                .map(this::toDto);
    }

    @Override
    public Single<PaymentResponse> send(PaymentRequest request) {
        String from = request.getFromPhone();
        String to = request.getToPhone();
        BigDecimal amount = request.getAmount();
        log.info("Payment of {} from {} to {}", amount, from, to);
        if (from.equals(to)) {
            return Single.error(new BusinessException("Sender and receiver must be different"));
        }
        // the receiver is checked BEFORE taking any money from the sender
        return findWallet(to).flatMap(target ->
                adjust(from, amount.negate()).flatMap(debited ->
                        adjust(to, amount)
                                .onErrorResumeNext(ex -> refund(from, amount, ex))
                                .flatMap(credited -> registerPayment(from, to, amount,
                                        debited.getBalance(), credited.getBalance()))));
    }

    @Override
    public Flowable<WalletMovementDto> movements(String phoneNumber) {
        return Flowable.fromPublisher(movementRepository.findByPhoneNumberOrderByDateDesc(phoneNumber))
                .map(this::toDto);
    }

    /** Atomic balance change; a negative amount only applies when there are enough funds. */
    private Single<Wallet> adjust(String phoneNumber, BigDecimal amount) {
        Query query = new Query(Criteria.where("phoneNumber").is(phoneNumber));
        if (amount.signum() < 0) {
            query.addCriteria(Criteria.where("balance").gte(amount.negate()));
        }
        return Maybe.fromPublisher(mongoTemplate.findAndModify(query,
                        new Update().inc("balance", amount),
                        FindAndModifyOptions.options().returnNew(true), Wallet.class))
                .switchIfEmpty(Single.defer(() -> findWallet(phoneNumber)
                        .flatMap(w -> Single.<Wallet>error(new BusinessException("Insufficient funds")))));
    }

    /** Compensation: returns the money to the sender and keeps the original error. */
    private Single<Wallet> refund(String from, BigDecimal amount, Throwable cause) {
        log.warn("Credit failed, refunding {} to {}: {}", amount, from, cause.toString());
        return adjust(from, amount)
                .onErrorResumeNext(e -> {
                    log.error("CRITICAL: refund of {} to {} failed, manual review required", amount, from, e);
                    return Single.error(cause);
                })
                .flatMap(refunded -> Single.<Wallet>error(cause));
    }

    private Single<PaymentResponse> registerPayment(String from, String to, BigDecimal amount,
                                                    BigDecimal senderBalance, BigDecimal receiverBalance) {
        return saveMovement(from, to, MovementType.SENT, amount, senderBalance)
                .flatMap(sent -> saveMovement(to, from, MovementType.RECEIVED, amount, receiverBalance)
                        .map(received -> PaymentResponse.builder()
                                .sent(toDto(sent)).received(toDto(received)).build()));
    }

    private Single<WalletMovement> saveMovement(String phone, String counterpart, MovementType type,
                                                BigDecimal amount, BigDecimal balanceAfter) {
        return Single.fromPublisher(movementRepository.save(WalletMovement.builder()
                .phoneNumber(phone).counterpartPhone(counterpart).type(type)
                .amount(amount).balanceAfter(balanceAfter)
                .date(LocalDateTime.now(clock)).build()));
    }

    private Single<Wallet> findWallet(String phoneNumber) {
        return Maybe.fromPublisher(walletRepository.findByPhoneNumber(phoneNumber))
                .switchIfEmpty(Single.error(new NotFoundException("Wallet not found: " + phoneNumber)));
    }

    private WalletMovementDto toDto(WalletMovement m) {
        return WalletMovementDto.builder()
                .id(m.getId()).phoneNumber(m.getPhoneNumber())
                .counterpartPhone(m.getCounterpartPhone()).type(m.getType())
                .amount(m.getAmount()).balanceAfter(m.getBalanceAfter()).date(m.getDate())
                .build();
    }
}