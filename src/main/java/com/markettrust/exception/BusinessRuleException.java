package com.markettrust.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class BusinessRuleException extends RuntimeException {

    private final String ruleCode;

    public BusinessRuleException(String message) {
        super(message);
        this.ruleCode = "BUSINESS_RULE_VIOLATION";
    }

    public BusinessRuleException(String ruleCode, String message) {
        super(message);
        this.ruleCode = ruleCode;
    }

    public BusinessRuleException(String message, Throwable cause) {
        super(message, cause);
        this.ruleCode = "BUSINESS_RULE_VIOLATION";
    }

    public String getRuleCode() {
        return ruleCode;
    }
}
