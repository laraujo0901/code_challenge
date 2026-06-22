package com.wex.challenge.purchasetransaction.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.stereotype.Service;

import com.wex.challenge.purchasetransaction.model.PurchaseTransaction;
import com.wex.challenge.purchasetransaction.model.PurchaseTransactionConvertedDTO;
import com.wex.challenge.purchasetransaction.model.PurchaseTransactionDTO;
import com.wex.challenge.purchasetransaction.model.RatesOfExchange;
import com.wex.challenge.purchasetransaction.repository.PurchaseTransactionRepository;

@Service
public class PurchaseTransactionService {

    private final PurchaseTransactionRepository repository;
    private final RatesOfExchangeService ratesOfExchangeService;

    public PurchaseTransactionService(PurchaseTransactionRepository repository,
                                      RatesOfExchangeService ratesOfExchangeService) {
        this.repository = repository;
        this.ratesOfExchangeService = ratesOfExchangeService;
    }

    public PurchaseTransactionDTO create(PurchaseTransactionDTO dto) {
        PurchaseTransaction transaction = new PurchaseTransaction();
        transaction.setDescription(dto.description());
        transaction.setAmount(dto.amount());
        transaction.setTransactionDate(parseTransactionDate(dto.transactionDate()));

        PurchaseTransaction savedTransaction = repository.save(transaction);
        return new PurchaseTransactionDTO(
            savedTransaction.getId(),
            savedTransaction.getDescription(),
            savedTransaction.getAmount(),
            savedTransaction.getTransactionDate() != null
                ? savedTransaction.getTransactionDate().toString()
                : null
        );
    }

    public Slice<PurchaseTransactionConvertedDTO> getConvertedTransactions(
            String currency,
            Pageable pageable) {
        if (currency == null || currency.isBlank()) {
            throw new IllegalArgumentException("Currency is required");
        }

        Slice<PurchaseTransaction> page = repository.findAllBy(pageable);
        List<PurchaseTransactionConvertedDTO> converted = page.getContent().stream()
            .map(transaction -> {
                Optional<RatesOfExchange> rateOpt = ratesOfExchangeService
                    .findRateForCurrencyAndDateOrFetch(
                        currency,
                        transaction.getTransactionDate()
                    );
                if (rateOpt.isEmpty()) {
                    throw new IllegalArgumentException(
                        "Exchange rate for currency " + currency + " not found for transaction " + transaction.getId() + " before or on " + transaction.getTransactionDate()
                    );
                }
                RatesOfExchange rate = rateOpt.get();
                return convertToDTO(transaction, rate);
            })
            .toList();

        return new SliceImpl<>(converted, pageable, page.hasNext());
    }

    private BigDecimal convertAmount(BigDecimal amount, BigDecimal exchangeRate) {
        if (amount == null || exchangeRate == null) {
            return BigDecimal.ZERO;
        }
        return amount.multiply(exchangeRate).setScale(2, RoundingMode.HALF_UP);
    }

    private LocalDate parseTransactionDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        for (DateTimeFormatter formatter : List.of(
                DateTimeFormatter.ofPattern("MM/dd/yyyy"),
                DateTimeFormatter.ISO_LOCAL_DATE)) {
            try {
                return LocalDate.parse(value, formatter);
            } catch (DateTimeParseException ignored) {
                // try next format
            }
        }

        throw new IllegalArgumentException(
            "transactionDate must be in MM/dd/yyyy or yyyy-MM-dd format"
        );
    }

    public PurchaseTransactionConvertedDTO getConvertedTransaction(Long transactionId, String currency) {
        if (transactionId == null) {
            throw new IllegalArgumentException("Transaction ID is required");
        }
        if (currency == null || currency.isBlank()) {
            throw new IllegalArgumentException("Currency is required");
        }

        Optional<PurchaseTransaction> transactionOpt = repository.findById(transactionId);
        if (transactionOpt.isEmpty()) {
            throw new IllegalArgumentException("Transaction not found");
        }

        PurchaseTransaction transaction = transactionOpt.get();
        Optional<RatesOfExchange> rateOpt = ratesOfExchangeService
            .findRateForCurrencyAndDateOrFetch(
                currency,
                transaction.getTransactionDate()
            );
        if (rateOpt.isEmpty()) {
            throw new IllegalArgumentException("Exchange rate for currency " + currency + " not found for transaction " + transactionId + " before or on " + transaction.getTransactionDate());
        }

        RatesOfExchange rate = rateOpt.get();
        return convertToDTO(transaction, rate);
    }

    private PurchaseTransactionConvertedDTO convertToDTO(PurchaseTransaction transaction, RatesOfExchange rate) {
        return new PurchaseTransactionConvertedDTO(
            transaction.getId(),
            transaction.getDescription(),
            transaction.getAmount(),
            transaction.getTransactionDate() != null
                ? transaction.getTransactionDate().toString()
                : null,
            convertAmount(transaction.getAmount(), rate.getExchangeRate()),
            rate.getCurrency(),
            rate.getExchangeRate()
        );
    }

    public Slice<PurchaseTransactionDTO> getTransactions(Pageable pageable) {
        Slice<PurchaseTransaction> page = repository.findAllBy(pageable);
        List<PurchaseTransactionDTO> dtos = page.getContent().stream()
            .map(transaction -> new PurchaseTransactionDTO(
                transaction.getId(),
                transaction.getDescription(),
                transaction.getAmount(),
                transaction.getTransactionDate() != null
                    ? transaction.getTransactionDate().toString()
                    : null
            ))
            .toList();

        return new SliceImpl<>(dtos, pageable, page.hasNext());
    }
}
