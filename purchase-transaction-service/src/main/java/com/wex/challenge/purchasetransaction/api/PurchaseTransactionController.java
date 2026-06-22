package com.wex.challenge.purchasetransaction.api;

import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.wex.challenge.purchasetransaction.model.PurchaseTransactionConvertedDTO;
import com.wex.challenge.purchasetransaction.model.PurchaseTransactionDTO;
import com.wex.challenge.purchasetransaction.service.PurchaseTransactionService;

@RestController
@RequestMapping("/api/purchase-transactions")
public class PurchaseTransactionController {

    private final PurchaseTransactionService service;

    public PurchaseTransactionController(PurchaseTransactionService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<PurchaseTransactionDTO> create(@RequestBody PurchaseTransactionDTO dto) {
        PurchaseTransactionDTO created = service.create(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    public Slice<PurchaseTransactionDTO> getTransactions(Pageable pageable) {
        return service.getTransactions(pageable);
    }
    

    @GetMapping("/converted")
    public ResponseEntity<Slice<PurchaseTransactionConvertedDTO>> getConverted(
            @RequestParam String currency,
            Pageable pageable) {
        return ResponseEntity.ok(service.getConvertedTransactions(currency, pageable));
    }

    @GetMapping("/converted/{transactionId}")
    public ResponseEntity<PurchaseTransactionConvertedDTO> getConvertedTransaction(
        @PathVariable("transactionId") Long transactionId,
        @RequestParam String currency) {
        return ResponseEntity.ok(service.getConvertedTransaction(transactionId, currency));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
    }
}
