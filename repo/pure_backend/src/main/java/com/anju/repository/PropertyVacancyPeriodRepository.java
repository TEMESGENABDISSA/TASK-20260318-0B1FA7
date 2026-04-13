package com.anju.repository;

import com.anju.entity.PropertyVacancyPeriod;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PropertyVacancyPeriodRepository extends JpaRepository<PropertyVacancyPeriod, Long> {
    List<PropertyVacancyPeriod> findByPropertyId(Long propertyId);
    void deleteByPropertyId(Long propertyId);
}
