package com.wex.challenge.purchasetransaction.api;

import com.wex.challenge.purchasetransaction.service.RatesOfExchangeService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.wex.challenge.purchasetransaction.model.RatesOfExchangeDTO;

@RestController
@RequestMapping("/api/rates-of-exchange")
public class RatesOfExchangeController {
    
    private final RatesOfExchangeService ratesOfExchangeService;

    RatesOfExchangeController(RatesOfExchangeService ratesOfExchangeService) {
        this.ratesOfExchangeService = ratesOfExchangeService;
    }

    @GetMapping
    public Slice<RatesOfExchangeDTO> getRatesOfExchange(Pageable pageable) {
        return ratesOfExchangeService.getAllRatesOfExchange(pageable);
    }
}
