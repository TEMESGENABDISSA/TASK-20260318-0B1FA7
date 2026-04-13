package com.anju.repository;

import com.anju.entity.FinancialTransaction;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FinancialTransactionRepository extends JpaRepository<FinancialTransaction, Long> {
    Optional<FinancialTransaction> findByTransactionNo(String transactionNo);
    List<FinancialTransaction> findByOccurredAtBetween(LocalDateTime from, LocalDateTime to);
}
