package com.bank.yanki.service;

import com.bank.yanki.dto.WalletDto;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;

/** Wallet operations. */
public interface WalletService {
    Single<WalletDto> create(WalletDto dto);
    Flowable<WalletDto> findAll();
    Single<WalletDto> findByPhoneNumber(String phoneNumber);
    Single<WalletDto> update(String phoneNumber, WalletDto dto);
    Completable delete(String phoneNumber);
    Single<WalletDto> linkCard(String phoneNumber, String cardNumber);
    Single<WalletDto> unlinkCard(String phoneNumber);
}
