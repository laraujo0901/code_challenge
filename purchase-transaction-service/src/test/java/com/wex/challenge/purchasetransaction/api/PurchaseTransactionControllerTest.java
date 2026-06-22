package com.wex.challenge.purchasetransaction.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.wex.challenge.purchasetransaction.model.PurchaseTransaction;
import com.wex.challenge.purchasetransaction.model.RatesOfExchange;
import com.wex.challenge.purchasetransaction.repository.PurchaseTransactionRepository;
import com.wex.challenge.purchasetransaction.repository.RatesOfExchangeRepository;

@SpringBootTest(properties = "rates.of.exchange.enabled=false")
@AutoConfigureMockMvc
class PurchaseTransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PurchaseTransactionRepository repository;

    @Autowired
    private RatesOfExchangeRepository ratesOfExchangeRepository;

    @AfterEach
    void cleanup() {
        repository.deleteAll();
        ratesOfExchangeRepository.deleteAll();
    }

    @Test
    void shouldCreatePurchaseTransactionFromDto() throws Exception {
        String requestBody = """
                {
                  "description": "Lunch",
                  "amount": 25.50,
                  "transactionDate": "2026-06-17",
                  "convertedAmount": 26.00,
                  "currency": "Real",
                  "exchangeRate": 1.02
                }
                """;

        mockMvc.perform(post("/api/purchase-transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.description").value("Lunch"))
                .andExpect(jsonPath("$.amount").value(25.5));

        assertEquals(1, repository.count());
    }

    @Test
    void shouldRejectInvalidTransactionDateFormat() throws Exception {
        String requestBody = """
                {
                  "description": "Lunch",
                  "amount": 25.50,
                  "transactionDate": "17-06-2026",
                  "convertedAmount": 26.00,
                  "currency": "Real",
                  "exchangeRate": 1.02
                }
                """;

        mockMvc.perform(post("/api/purchase-transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                    .value("transactionDate must be in MM/dd/yyyy or yyyy-MM-dd format"));
    }

    @Test
    void shouldReturnConvertedTransactionsPaginated() throws Exception {
        PurchaseTransaction transaction = new PurchaseTransaction();
        transaction.setDescription("Lunch");
        transaction.setAmount(BigDecimal.valueOf(25.5));
        transaction.setTransactionDate(LocalDate.of(2026, 6, 17));
        repository.save(transaction);

        RatesOfExchange rate = new RatesOfExchange();
        rate.setRecordDate(LocalDate.of(2026, 6, 17));
        rate.setCountry("United States");
        rate.setCurrency("USD");
        rate.setExchangeRate(BigDecimal.valueOf(1.5));
        rate.setEffectiveDate(LocalDate.of(2026, 6, 17));
        ratesOfExchangeRepository.save(rate);

        mockMvc.perform(get("/api/purchase-transactions/converted")
                .param("currency", "USD")
                .param("page", "0")
                .param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].description").value("Lunch"))
                .andExpect(jsonPath("$.content[0].currency").value("USD"))
                .andExpect(jsonPath("$.content[0].convertedAmount").value(38.25));
    }

    @Test
    void shouldUseExchangeRateEffectiveOnOrBeforeTransactionDate() throws Exception {
        PurchaseTransaction transaction = new PurchaseTransaction();
        transaction.setDescription("Dinner");
        transaction.setAmount(BigDecimal.valueOf(10.00));
        transaction.setTransactionDate(LocalDate.of(2026, 6, 18));
        repository.save(transaction);

        RatesOfExchange earlierRate = new RatesOfExchange();
        earlierRate.setRecordDate(LocalDate.of(2026, 6, 10));
        earlierRate.setCountry("United States");
        earlierRate.setCurrency("USD");
        earlierRate.setExchangeRate(BigDecimal.valueOf(1.10));
        earlierRate.setEffectiveDate(LocalDate.of(2026, 6, 10));
        ratesOfExchangeRepository.save(earlierRate);

        RatesOfExchange laterRate = new RatesOfExchange();
        laterRate.setRecordDate(LocalDate.of(2026, 6, 20));
        laterRate.setCountry("United States");
        laterRate.setCurrency("USD");
        laterRate.setExchangeRate(BigDecimal.valueOf(1.50));
        laterRate.setEffectiveDate(LocalDate.of(2026, 6, 20));
        ratesOfExchangeRepository.save(laterRate);

        mockMvc.perform(get("/api/purchase-transactions/converted")
                .param("currency", "USD")
                .param("page", "0")
                .param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].description").value("Dinner"))
                .andExpect(jsonPath("$.content[0].exchangeRate").value(1.10))
                .andExpect(jsonPath("$.content[0].convertedAmount").value(11.00));
    }
}