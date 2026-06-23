package com.wex.challenge.purchasetransaction.model;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record PurchaseTransactionConvertedDTO(
    Long id,
    String description,
    BigDecimal amount,
    String transactionDate,
    BigDecimal convertedAmount,
    String currency,
    BigDecimal exchangeRate,
    String message
) {

}
