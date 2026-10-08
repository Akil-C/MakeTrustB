package com.markettrust.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InsufficientCreditsException extends RuntimeException {

    private final int requiredCredits;
    private final int availableCredits;

    public InsufficientCreditsException(int requiredCredits, int availableCredits) {
        super(String.format(
            "Insufficient credits. Required: %d, Available: %d",
            requiredCredits, availableCredits
        ));
        this.requiredCredits = requiredCredits;
        this.availableCredits = availableCredits;
    }

    public InsufficientCreditsException(String message) {
        super(message);
        this.requiredCredits = 0;
        this.availableCredits = 0;
    }

    public int getRequiredCredits() {
        return requiredCredits;
    }

    public int getAvailableCredits() {
        return availableCredits;
    }
}
