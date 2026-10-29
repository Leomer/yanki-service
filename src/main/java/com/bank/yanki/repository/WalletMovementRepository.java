package com.bank.yanki.repository;

import com.bank.yanki.model.WalletMovement;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;

/** Reactive repository for wallet movements (derived queries only). */
public interface WalletMovementRepository extends ReactiveMongoRepository<WalletMovement, String> {
    Flux<WalletMovement> findByPhoneNumberOrderByDateDesc(String phoneNumber);
}