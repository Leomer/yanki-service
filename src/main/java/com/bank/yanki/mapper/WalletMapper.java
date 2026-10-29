package com.bank.yanki.mapper;

import com.bank.yanki.dto.WalletDto;
import com.bank.yanki.model.Wallet;

import java.math.BigDecimal;

/** Converts between Wallet entities and DTOs. */
public final class WalletMapper {

    private WalletMapper() {
    }

    public static WalletDto toDto(Wallet e) {
        return WalletDto.builder()
                .id(e.getId())
                .documentType(e.getDocumentType())
                .documentNumber(e.getDocumentNumber())
                .phoneNumber(e.getPhoneNumber())
                .imei(e.getImei())
                .email(e.getEmail())
                .balance(e.getBalance())
                .debitCardNumber(e.getDebitCardNumber())
                .build();
    }

    /** Builds a new wallet: balance starts at zero and no card is linked. */
    public static Wallet toNewEntity(WalletDto d) {
        return Wallet.builder()
                .documentType(d.getDocumentType())
                .documentNumber(d.getDocumentNumber())
                .phoneNumber(d.getPhoneNumber())
                .imei(d.getImei())
                .email(d.getEmail())
                .balance(BigDecimal.ZERO)
                .build();
    }
}
