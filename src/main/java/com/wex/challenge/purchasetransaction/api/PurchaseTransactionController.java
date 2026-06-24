package com.wex.challenge.purchasetransaction.api;

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

import com.wex.challenge.purchasetransaction.exception.InvalidTransactionDataException;
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
    public Slice<PurchaseTransactionConvertedDTO> getConverted(
            @RequestParam(required = false) String currency,
            Pageable pageable) {
        return service.getConvertedTransactions(currency, pageable);
    }

    @GetMapping("/converted/{transactionId}")
    public PurchaseTransactionConvertedDTO getConvertedTransaction(
        @PathVariable("transactionId") Long transactionId,
        @RequestParam(required = false) String currency) {
        return service.getConvertedTransaction(transactionId, currency);
    }

    @ExceptionHandler({IllegalArgumentException.class, InvalidTransactionDataException.class})
    public ResponseEntity<Map<String, Object>> handleInvalidRequest(RuntimeException ex) {
        if (ex instanceof InvalidTransactionDataException ite) {
            return ResponseEntity.badRequest().body(Map.of(
                "message", ite.getGeneralMessage(),
                "details", ite.getDetails()
            ));
        }
        return ResponseEntity.badRequest().body(Map.of("message", ex.getMessage()));
    }
}
