package com.wex.challenge.purchasetransaction.exception;

import java.util.List;

public class InvalidTransactionDataException extends RuntimeException {

    private static final String GENERAL_MESSAGE = "Some attributes are invalid, check details";

    private final List<String> details;

    public InvalidTransactionDataException(List<String> details) {
        super(GENERAL_MESSAGE);
        this.details = details;
    }

    public String getGeneralMessage() {
        return GENERAL_MESSAGE;
    }

    public List<String> getDetails() {
        return details;
    }
}
