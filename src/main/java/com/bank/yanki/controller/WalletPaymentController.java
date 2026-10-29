package com.bank.yanki.controller;

import com.bank.yanki.dto.AmountRequest;
import com.bank.yanki.dto.PaymentRequest;
import com.bank.yanki.dto.PaymentResponse;
import com.bank.yanki.dto.WalletMovementDto;
import com.bank.yanki.service.WalletPaymentService;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Single;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/** REST endpoints for wallet money operations. */
@RestController
@RequestMapping("/yanki")
@RequiredArgsConstructor
public class WalletPaymentController {

    private final WalletPaymentService service;

    @PostMapping("/wallets/{phoneNumber}/top-ups")
    @ResponseStatus(HttpStatus.CREATED)
    public Single<WalletMovementDto> topUp(@PathVariable String phoneNumber,
                                           @Valid @RequestBody AmountRequest request) {
        return service.topUp(phoneNumber, request.getAmount());
    }

    @PostMapping("/wallets/{phoneNumber}/withdrawals")
    @ResponseStatus(HttpStatus.CREATED)
    public Single<WalletMovementDto> withdraw(@PathVariable String phoneNumber,
                                              @Valid @RequestBody AmountRequest request) {
        return service.withdraw(phoneNumber, request.getAmount());
    }

    @PostMapping("/payments")
    @ResponseStatus(HttpStatus.CREATED)
    public Single<PaymentResponse> send(@Valid @RequestBody PaymentRequest request) {
        return service.send(request);
    }

    @GetMapping("/wallets/{phoneNumber}/movements")
    public Flowable<WalletMovementDto> movements(@PathVariable String phoneNumber) {
        return service.movements(phoneNumber);
    }
}
