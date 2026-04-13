package com.anju.repository;

import com.anju.entity.Property;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PropertyRepository extends JpaRepository<Property, Long> {
    Optional<Property> findByPropertyCode(String propertyCode);
}
