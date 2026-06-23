package com.wex.challenge.purchasetransaction.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public record RatesOfExchangeDTO(
    String country,
    String currency,
    BigDecimal exchangeRate,
    LocalDate effectiveDate
) {

}
