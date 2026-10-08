package com.markettrust.wallet.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class WalletFrozenException extends RuntimeException {

    public WalletFrozenException(String message) {
        super(message);
    }
}
