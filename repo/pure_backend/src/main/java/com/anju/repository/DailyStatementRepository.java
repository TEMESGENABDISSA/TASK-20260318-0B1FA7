package com.anju.repository;

import com.anju.entity.DailyStatement;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DailyStatementRepository extends JpaRepository<DailyStatement, Long> {
    Optional<DailyStatement> findByStatementDate(LocalDate statementDate);
}
