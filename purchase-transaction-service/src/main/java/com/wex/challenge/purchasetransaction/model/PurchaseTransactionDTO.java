package com.wex.challenge.purchasetransaction.model;

import java.math.BigDecimal;

public record PurchaseTransactionDTO(
    Long id,
    String description,
    BigDecimal amount,
    String transactionDate
) {

}
