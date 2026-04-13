package com.anju.repository;

import com.anju.entity.InvoiceRecord;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InvoiceRecordRepository extends JpaRepository<InvoiceRecord, Long> {
}
