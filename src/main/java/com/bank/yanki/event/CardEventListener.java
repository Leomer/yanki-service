package com.bank.yanki.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/** Listens to the debit-card-events topic. */
@Slf4j
@Component
@RequiredArgsConstructor
public class CardEventListener {

    private final CardEventHandler handler;

    @KafkaListener(topics = "debit-card-events")
    public void onMessage(String message) {
        handler.handle(message)
                .subscribe(v -> { }, e -> log.error("Could not process {}: {}", message, e.toString()));
    }
}
