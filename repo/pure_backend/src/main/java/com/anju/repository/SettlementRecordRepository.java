package com.anju.repository;

import com.anju.entity.SettlementRecord;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SettlementRecordRepository extends JpaRepository<SettlementRecord, Long> {
}
