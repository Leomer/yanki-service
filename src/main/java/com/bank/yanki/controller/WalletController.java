package com.bank.yanki.controller;

import com.bank.yanki.dto.LinkCardRequest;
import com.bank.yanki.dto.WalletDto;
import com.bank.yanki.service.WalletService;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/** REST endpoints for Yanki wallets. */
@RestController
@RequestMapping("/yanki/wallets")
@RequiredArgsConstructor
public class WalletController {

    private final WalletService service;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Single<WalletDto> create(@Valid @RequestBody WalletDto dto) {
        return service.create(dto);
    }

    @GetMapping
    public Flowable<WalletDto> findAll() {
        return service.findAll();
    }

    @GetMapping("/{phoneNumber}")
    public Single<WalletDto> findByPhoneNumber(@PathVariable String phoneNumber) {
        return service.findByPhoneNumber(phoneNumber);
    }

    @PutMapping("/{phoneNumber}")
    public Single<WalletDto> update(@PathVariable String phoneNumber,
                                    @Valid @RequestBody WalletDto dto) {
        return service.update(phoneNumber, dto);
    }

    @DeleteMapping("/{phoneNumber}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Completable delete(@PathVariable String phoneNumber) {
        return service.delete(phoneNumber);
    }

    @PutMapping("/{phoneNumber}/debit-card")
    public Single<WalletDto> linkCard(@PathVariable String phoneNumber,
                                      @Valid @RequestBody LinkCardRequest request) {
        return service.linkCard(phoneNumber, request.getCardNumber());
    }

    @DeleteMapping("/{phoneNumber}/debit-card")
    public Single<WalletDto> unlinkCard(@PathVariable String phoneNumber) {
        return service.unlinkCard(phoneNumber);
    }
}
