package com.wex.challenge.purchasetransaction.exception;

/**
 * Exception thrown when currency conversion is not possible due to 
 * failures in fetching exchange rates from the Treasury API.
 */
public class CurrencyConversionUnavailableException extends RuntimeException {

    public CurrencyConversionUnavailableException(String message) {
        super(message);
    }

    public CurrencyConversionUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
