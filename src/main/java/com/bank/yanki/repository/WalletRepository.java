package com.bank.yanki.repository;

import com.bank.yanki.model.Wallet;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Mono;

/** Reactive repository for wallets (derived queries only). */
public interface WalletRepository extends ReactiveMongoRepository<Wallet, String> {
    Mono<Wallet> findByPhoneNumber(String phoneNumber);
    Mono<Boolean> existsByPhoneNumber(String phoneNumber);
    Mono<Boolean> existsByDocumentNumber(String documentNumber);
}