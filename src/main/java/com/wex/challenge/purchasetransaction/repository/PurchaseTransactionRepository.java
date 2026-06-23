package com.wex.challenge.purchasetransaction.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;

import com.wex.challenge.purchasetransaction.model.PurchaseTransaction;

public interface PurchaseTransactionRepository extends JpaRepository<PurchaseTransaction, Long> {

    Slice<PurchaseTransaction> findAllBy(Pageable pageable);

}
