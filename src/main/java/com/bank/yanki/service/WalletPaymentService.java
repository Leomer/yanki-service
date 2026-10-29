package com.bank.yanki.service;

import com.bank.yanki.dto.PaymentRequest;
import com.bank.yanki.dto.PaymentResponse;
import com.bank.yanki.dto.WalletMovementDto;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;

import java.math.BigDecimal;

/** Money operations on wallets. */
public interface WalletPaymentService {

    Single<WalletMovementDto> topUp(String phoneNumber, BigDecimal amount);
    Single<WalletMovementDto> withdraw(String phoneNumber, BigDecimal amount);
    Single<PaymentResponse> send(PaymentRequest request);
    Flowable<WalletMovementDto> movements(String phoneNumber);
}