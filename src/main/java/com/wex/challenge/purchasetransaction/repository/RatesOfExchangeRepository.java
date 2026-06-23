package com.wex.challenge.purchasetransaction.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.wex.challenge.purchasetransaction.model.RatesOfExchange;

public interface RatesOfExchangeRepository extends JpaRepository<RatesOfExchange, Long> {
    List<RatesOfExchange> findByCurrencyIgnoreCase(String currency);

    Optional<RatesOfExchange> findFirstByCurrencyIgnoreCaseAndEffectiveDateLessThanEqualOrderByEffectiveDateDesc(
        String currency,
        LocalDate effectiveDate
    );
}
