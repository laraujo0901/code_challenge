package com.wex.challenge.purchasetransaction.model;

import java.math.BigDecimal;

public record PurchaseTransactionConvertedDTO(
    Long id,
    String description,
    BigDecimal amount,
    String transactionDate,
    BigDecimal convertedAmount,
    String currency,
    BigDecimal exchangeRate
) {

}
