package com.wex.challenge.purchasetransaction.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import com.wex.challenge.purchasetransaction.model.RatesOfExchange;
import com.wex.challenge.purchasetransaction.repository.RatesOfExchangeRepository;

@ExtendWith(MockitoExtension.class)
class RatesOfExchangeServiceTest {

    @Mock
    private RatesOfExchangeRepository repository;

    @Mock
    private RestTemplateBuilder restTemplateBuilder;

    @Mock
    private RestTemplate restTemplate;

    @Captor
    private ArgumentCaptor<List<RatesOfExchange>> entityCaptor;

    @Test
    void shouldPersistAllPagesFromApiResponse() {
        when(restTemplateBuilder.build()).thenReturn(restTemplate);
        RatesOfExchangeService service = new RatesOfExchangeService(restTemplateBuilder, repository);
        ReflectionTestUtils.setField(service, "BASE_URL",
            "https://api.fiscaldata.treasury.gov/services/api/fiscal_service/v1/accounting/od/rates_of_exchange");
        ReflectionTestUtils.setField(service, "fields",
            "record_date,country,currency,exchange_rate,effective_date");
        ReflectionTestUtils.setField(service, "PAGE_SIZE", 100);
        ReflectionTestUtils.setField(service, "monthsToLookBack", 6);

        RatesOfExchangeService.RatesOfExchangeResponse firstPage = new RatesOfExchangeService.RatesOfExchangeResponse(
            List.of(
                new RatesOfExchangeService.RatesOfExchangeRecord(
                    "2001-03-31", "Afghanistan", "Afghani", "78400.0", "2001-03-31"
                )
            ),
            new RatesOfExchangeService.Meta(2)
        );
        RatesOfExchangeService.RatesOfExchangeResponse secondPage = new RatesOfExchangeService.RatesOfExchangeResponse(
            List.of(
                new RatesOfExchangeService.RatesOfExchangeRecord(
                    "2001-04-01", "Albania", "Lek", "142.4", "2001-04-01"
                )
            ),
            new RatesOfExchangeService.Meta(2)
        );

        when(restTemplate.getForObject(org.mockito.ArgumentMatchers.any(URI.class),
                org.mockito.ArgumentMatchers.eq(RatesOfExchangeService.RatesOfExchangeResponse.class)))
            .thenReturn(firstPage, secondPage);

        service.fetchAndPersistAll();

        verify(repository, times(2)).saveAll(entityCaptor.capture());
        assertEquals(2, entityCaptor.getAllValues().size());
    }
}
