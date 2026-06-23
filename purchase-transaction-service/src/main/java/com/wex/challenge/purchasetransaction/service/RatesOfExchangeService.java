package com.wex.challenge.purchasetransaction.service;

import java.math.BigDecimal;
import java.net.URI;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.wex.challenge.purchasetransaction.exception.CurrencyConversionUnavailableException;
import com.wex.challenge.purchasetransaction.model.RatesOfExchange;
import com.wex.challenge.purchasetransaction.model.RatesOfExchangeDTO;
import com.wex.challenge.purchasetransaction.repository.RatesOfExchangeRepository;

@Service
public class RatesOfExchangeService {

    private final RestTemplate restTemplate;
    private final RatesOfExchangeRepository repository;

    @Value("${rates.of.exchange.base-url:https://api.fiscaldata.treasury.gov/services/api/fiscal_service/v1/accounting/od/rates_of_exchange}")
    private String BASE_URL;

    @Value("${rates.of.exchange.fields:record_date,country,currency,exchange_rate,effective_date}")
    private String fields;

    @Value("${rates.of.exchange.page-size:100}")
    private int PAGE_SIZE;

    @Value("${rates.of.exchange.months-to-look-back:6}")
    private int monthsToLookBack;

    public RatesOfExchangeService(RestTemplateBuilder restTemplateBuilder,
                                  RatesOfExchangeRepository repository) {
        this.restTemplate = restTemplateBuilder.build();
        this.repository = repository;
    }

    @SuppressWarnings("null")
    @Transactional
    public void fetchAndPersistAll() {
        int pageNumber = 1;
        int totalPages = Integer.MAX_VALUE;

        while (pageNumber <= totalPages) {
            RatesOfExchangeResponse response = fetchPage(pageNumber, null);
            if (response == null || response.data() == null || response.data().isEmpty()) {
                break;
            }

            repository.saveAll(toEntities(response.data()));

            totalPages = response.meta() != null && response.meta().totalPages() != null
                ? response.meta().totalPages()
                : 0;
            pageNumber++;
        }
    }

    @Transactional(readOnly = true)
    public Optional<RatesOfExchange> findRateForCurrency(String currency) {
        if (currency == null || currency.isBlank()) {
            return Optional.empty();
        }

        return repository.findByCurrencyIgnoreCase(currency).stream()
            .max(Comparator.comparing(RatesOfExchange::getEffectiveDate)
                .thenComparing(RatesOfExchange::getRecordDate));
    }

    @Transactional(readOnly = true)
    public Optional<RatesOfExchange> findRateForCurrencyAndDate(
            String currency,
            LocalDate transactionDate) {
        if (currency == null || currency.isBlank() || transactionDate == null) {
            return Optional.empty();
        }

        return repository.findFirstByCurrencyIgnoreCaseAndEffectiveDateLessThanEqualOrderByEffectiveDateDesc(
            currency,
            transactionDate
        );
    }

    @Transactional
    public Optional<RatesOfExchange> findRateForCurrencyOrFetch(String currency) {
        Optional<RatesOfExchange> cached = findRateForCurrency(currency);
        if (cached.isPresent()) {
            return cached;
        }

        Optional<RatesOfExchange> fetched = fetchRateForCurrency(currency);
        fetched.ifPresent(repository::save);
        return fetched;
    }

    @Transactional
    public Optional<RatesOfExchange> findRateForCurrencyAndDateOrFetch(
            String currency,
            LocalDate transactionDate) {
        Optional<RatesOfExchange> cached = findRateForCurrencyAndDate(currency, transactionDate);
        if (cached.isPresent()) {
            return cached;
        }

        Optional<RatesOfExchange> fetched = fetchRateForCurrencyAndDate(currency, transactionDate);
        fetched.ifPresent(repository::save);
        return fetched;
    }

    private Optional<RatesOfExchange> fetchRateForCurrency(String currency) {
        RatesOfExchangeResponse response = fetchPage(1, currency);
        if (response == null || response.data() == null || response.data().isEmpty()) {
            return Optional.empty();
        }

        return toEntities(response.data()).stream().findFirst();
    }

    private Optional<RatesOfExchange> fetchRateForCurrencyAndDate(
            String currency,
            LocalDate transactionDate) {
        RatesOfExchangeResponse response = fetchPage(1, currency, transactionDate);
        if (response == null || response.data() == null || response.data().isEmpty()) {
            return Optional.empty();
        }

        return toEntities(response.data()).stream()
            .filter(rate -> rate.getEffectiveDate() != null
                && !rate.getEffectiveDate().isAfter(transactionDate))
            .max(Comparator.comparing(RatesOfExchange::getEffectiveDate)
                .thenComparing(RatesOfExchange::getRecordDate));
    }

    private RatesOfExchangeResponse fetchPage(int pageNumber, String currencyFilter) {
        return fetchPage(pageNumber, currencyFilter, null);
    }

    @Retryable(
        retryFor = { RestClientException.class, Exception.class },
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2.0)
    )
    private RatesOfExchangeResponse fetchPage(
            int pageNumber,
            String currencyFilter,
            LocalDate transactionDate) {
        StringBuilder filterBuilder = new StringBuilder();
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(BASE_URL)
            .queryParam("fields", fields)
            .queryParam("page[number]", pageNumber)
            .queryParam("page[size]", PAGE_SIZE);

        if (currencyFilter != null && !currencyFilter.isBlank()) {
            filterBuilder.append("currency:eq:").append(currencyFilter);    
            filterBuilder.append(",effective_date:gte:").append(LocalDate.now().minusMonths(monthsToLookBack));
            filterBuilder.append(",effective_date:lte:").append(LocalDate.now());
            builder.queryParam("filter", filterBuilder.toString());
        }

        URI uri = builder.build().toUri();
        return restTemplate.getForObject(uri, RatesOfExchangeResponse.class);
    }

    @Recover
    private RatesOfExchangeResponse recoverFetchPage(
            Exception ex,
            int pageNumber,
            String currencyFilter,
            LocalDate transactionDate) {
        throw new CurrencyConversionUnavailableException(
            "Unable to fetch exchange rates from Treasury API after 3 retry attempts. " +
            "Currency conversion is not available at this moment. Please try again later.",
            ex
        );
    }

    private List<RatesOfExchange> toEntities(List<RatesOfExchangeRecord> records) {
        List<RatesOfExchange> entities = new ArrayList<>();
        for (RatesOfExchangeRecord record : records) {
            RatesOfExchange entity = new RatesOfExchange();
            entity.setRecordDate(LocalDate.parse(record.recordDate()));
            entity.setCountry(record.country());
            entity.setCurrency(record.currency());
            entity.setExchangeRate(new BigDecimal(record.exchangeRate()));
            entity.setEffectiveDate(LocalDate.parse(record.effectiveDate()));
            entities.add(entity);
        }
        return entities;
    }

    public record RatesOfExchangeResponse(
        List<RatesOfExchangeRecord> data,
        Meta meta
    ) {
    }

    public record RatesOfExchangeRecord(
        @JsonProperty("record_date") String recordDate,
        String country,
        String currency,
        @JsonProperty("exchange_rate") String exchangeRate,
        @JsonProperty("effective_date") String effectiveDate
    ) {
    }

    public record Meta(
        @JsonProperty("total-pages") Integer totalPages
    ) {
    }

    public List<RatesOfExchangeDTO> getAllRatesOfExchange() {
        List<RatesOfExchange> rates = repository.findAll();
        List<RatesOfExchangeDTO> dtos = new ArrayList<>();
        for (RatesOfExchange rate : rates) {
            RatesOfExchangeDTO dto = new RatesOfExchangeDTO(
                rate.getCountry(),
                rate.getCurrency(),
                rate.getExchangeRate(),
                rate.getEffectiveDate()
            );
            dtos.add(dto);
        }
        return dtos;
    }
}
