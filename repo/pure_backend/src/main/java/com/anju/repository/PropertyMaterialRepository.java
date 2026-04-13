package com.anju.repository;

import com.anju.entity.PropertyMaterial;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PropertyMaterialRepository extends JpaRepository<PropertyMaterial, Long> {
    List<PropertyMaterial> findByPropertyId(Long propertyId);
    void deleteByPropertyId(Long propertyId);
}
