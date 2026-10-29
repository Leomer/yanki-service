package com.bank.yanki.repository;

import com.bank.yanki.model.KnownCard;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;

/** Local copy of debit cards. */
public interface KnownCardRepository extends ReactiveMongoRepository<KnownCard, String> {
}