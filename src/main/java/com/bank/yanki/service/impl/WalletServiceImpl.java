package com.bank.yanki.service.impl;

import com.bank.yanki.dto.WalletDto;
import com.bank.yanki.exception.BusinessException;
import com.bank.yanki.exception.NotFoundException;
import com.bank.yanki.mapper.WalletMapper;
import com.bank.yanki.model.Wallet;
import com.bank.yanki.repository.WalletRepository;
import com.bank.yanki.service.WalletService;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Maybe;
import io.reactivex.rxjava3.core.Single;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/** Default implementation of {@link WalletService}. */
@Slf4j
@Service
@RequiredArgsConstructor
public class WalletServiceImpl implements WalletService {

    private final WalletRepository repository;

    @Override
    public Single<WalletDto> create(WalletDto dto) {
        log.info("Registering wallet for phone {}", dto.getPhoneNumber());
        return Single.fromPublisher(repository.existsByPhoneNumber(dto.getPhoneNumber()))
                .flatMap(phoneUsed -> Boolean.TRUE.equals(phoneUsed)
                        ? Single.<Boolean>error(new BusinessException(
                        "A wallet already exists for that phone number"))
                        : Single.fromPublisher(repository.existsByDocumentNumber(dto.getDocumentNumber())))
                .flatMap(documentUsed -> Boolean.TRUE.equals(documentUsed)
                        ? Single.<Wallet>error(new BusinessException(
                        "A wallet already exists for that document number"))
                        : Single.fromPublisher(repository.save(WalletMapper.toNewEntity(dto))))
                .map(WalletMapper::toDto);
    }

    @Override
    public Flowable<WalletDto> findAll() {
        return Flowable.fromPublisher(repository.findAll()).map(WalletMapper::toDto);
    }

    @Override
    public Single<WalletDto> findByPhoneNumber(String phoneNumber) {
        return findEntity(phoneNumber).map(WalletMapper::toDto);
    }

    @Override
    public Single<WalletDto> update(String phoneNumber, WalletDto dto) {
        log.info("Updating wallet {}", phoneNumber);
        return findEntity(phoneNumber).flatMap(wallet -> {
            // phone, document and balance cannot be changed here
            wallet.setEmail(dto.getEmail());
            wallet.setImei(dto.getImei());
            return Single.fromPublisher(repository.save(wallet));
        }).map(WalletMapper::toDto);
    }

    @Override
    public Completable delete(String phoneNumber) {
        log.info("Deleting wallet {}", phoneNumber);
        return findEntity(phoneNumber).flatMapCompletable(wallet -> {
            if (wallet.getBalance() != null && wallet.getBalance().compareTo(BigDecimal.ZERO) > 0) {
                return Completable.error(new BusinessException(
                        "Wallet still has balance; withdraw it before deleting"));
            }
            return Completable.fromPublisher(repository.delete(wallet));
        });
    }

    private Single<Wallet> findEntity(String phoneNumber) {
        return Maybe.fromPublisher(repository.findByPhoneNumber(phoneNumber))
                .switchIfEmpty(Single.error(
                        new NotFoundException("Wallet not found: " + phoneNumber)));
    }
}
