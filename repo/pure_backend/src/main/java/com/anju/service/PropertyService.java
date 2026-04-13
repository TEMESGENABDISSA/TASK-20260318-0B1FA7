package com.anju.service;

import com.anju.common.BusinessException;
import com.anju.dto.PropertyCreateRequest;
import com.anju.dto.PropertyMaterialsRequest;
import com.anju.dto.PropertyResponse;
import com.anju.dto.PropertyReviewRequest;
import com.anju.dto.PropertyUpdateRequest;
import com.anju.dto.VacancyPeriodDto;
import com.anju.entity.Property;
import com.anju.entity.PropertyMaterial;
import com.anju.entity.PropertyVacancyPeriod;
import com.anju.repository.PropertyMaterialRepository;
import com.anju.repository.PropertyRepository;
import com.anju.repository.PropertyVacancyPeriodRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PropertyService {

    private final PropertyRepository propertyRepository;
    private final PropertyMaterialRepository propertyMaterialRepository;
    private final PropertyVacancyPeriodRepository propertyVacancyPeriodRepository;
    private final PropertyComplianceService propertyComplianceService;

    public PropertyService(PropertyRepository propertyRepository,
                           PropertyMaterialRepository propertyMaterialRepository,
                           PropertyVacancyPeriodRepository propertyVacancyPeriodRepository,
                           PropertyComplianceService propertyComplianceService) {
        this.propertyRepository = propertyRepository;
        this.propertyMaterialRepository = propertyMaterialRepository;
        this.propertyVacancyPeriodRepository = propertyVacancyPeriodRepository;
        this.propertyComplianceService = propertyComplianceService;
    }

    @Transactional
    public PropertyResponse create(PropertyCreateRequest request) {
        if (request.getPropertyCode() == null || request.getPropertyCode().isBlank()) {
            throw new BusinessException("propertyCode is required");
        }
        if (propertyRepository.findByPropertyCode(request.getPropertyCode()).isPresent()) {
            throw new BusinessException("propertyCode already exists");
        }
        Property p = new Property();
        p.setPropertyCode(request.getPropertyCode());
        p.setPropertyName(request.getPropertyName() == null ? "" : request.getPropertyName());
        p.setRent(request.getRent() == null ? java.math.BigDecimal.ZERO : request.getRent());
        p.setDeposit(request.getDeposit() == null ? java.math.BigDecimal.ZERO : request.getDeposit());
        p.setRentalStartDate(request.getRentalStartDate());
        p.setRentalEndDate(request.getRentalEndDate());
        p.setComplianceResult("UNKNOWN");
        p.setStatus("DRAFT");
        Property saved = propertyRepository.save(p);
        replaceVacancyPeriods(saved.getId(), request.getVacancyPeriods());
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public PropertyResponse getById(Long id) {
        Property property = propertyRepository.findById(id)
                .orElseThrow(() -> new BusinessException("property not found"));
        return toResponse(property);
    }

    @Transactional(readOnly = true)
    public List<PropertyResponse> list() {
        return propertyRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional
    public PropertyResponse update(Long id, PropertyUpdateRequest request) {
        Property current = propertyRepository.findById(id)
                .orElseThrow(() -> new BusinessException("property not found"));
        if (request.getPropertyName() != null && !request.getPropertyName().isBlank()) {
            current.setPropertyName(request.getPropertyName());
        }
        if (request.getRent() != null) {
            current.setRent(request.getRent());
        }
        if (request.getDeposit() != null) {
            current.setDeposit(request.getDeposit());
        }
        if (request.getRentalStartDate() != null) {
            current.setRentalStartDate(request.getRentalStartDate());
        }
        if (request.getRentalEndDate() != null) {
            current.setRentalEndDate(request.getRentalEndDate());
        }
        if (request.getVacancyPeriods() != null) {
            replaceVacancyPeriods(id, request.getVacancyPeriods());
        }
        return toResponse(propertyRepository.save(current));
    }

    @Transactional
    public PropertyResponse attachMaterials(Long id, PropertyMaterialsRequest request) {
        Property current = propertyRepository.findById(id)
                .orElseThrow(() -> new BusinessException("property not found"));
        if (request.getFileIds() == null) {
            throw new BusinessException("fileIds is required");
        }
        propertyMaterialRepository.deleteByPropertyId(id);
        for (Long fileId : request.getFileIds()) {
            PropertyMaterial pm = new PropertyMaterial();
            pm.setPropertyId(id);
            pm.setFileId(fileId);
            propertyMaterialRepository.save(pm);
        }
        return toResponse(current);
    }

    @Transactional
    public PropertyResponse submitReview(Long id) {
        Property current = propertyRepository.findById(id)
                .orElseThrow(() -> new BusinessException("property not found"));
        if (!"DRAFT".equals(current.getStatus()) && !"REJECTED".equals(current.getStatus())) {
            throw new BusinessException("Only draft/rejected property can be submitted for review");
        }
        if (current.getRentalStartDate() == null || current.getRentalEndDate() == null) {
            throw new BusinessException("rental start/end date is required for review submission");
        }
        var compliance = propertyComplianceService.evaluate(current);
        if (!compliance.pass()) {
            current.setComplianceResult("FAIL: " + String.join("; ", compliance.violations()));
            propertyRepository.save(current);
            throw new BusinessException("Compliance check failed: " + String.join("; ", compliance.violations()));
        }
        current.setComplianceResult("PASS");
        current.setStatus("PENDING_REVIEW");
        return toResponse(propertyRepository.save(current));
    }

    @Transactional
    public PropertyResponse review(Long id, PropertyReviewRequest request) {
        Property current = propertyRepository.findById(id)
                .orElseThrow(() -> new BusinessException("property not found"));
        if (!"PENDING_REVIEW".equals(current.getStatus())) {
            throw new BusinessException("Property is not in pending review status");
        }
        if ("APPROVE".equalsIgnoreCase(request.getAction())) {
            current.setStatus("APPROVED");
            return toResponse(propertyRepository.save(current));
        }
        if ("REJECT".equalsIgnoreCase(request.getAction())) {
            current.setStatus("REJECTED");
            return toResponse(propertyRepository.save(current));
        }
        throw new BusinessException("action must be APPROVE or REJECT");
    }

    @Transactional
    public PropertyResponse invalidate(Long id) {
        Property current = propertyRepository.findById(id)
                .orElseThrow(() -> new BusinessException("property not found"));
        current.setStatus("INVALIDATED");
        return toResponse(propertyRepository.save(current));
    }

    private PropertyResponse toResponse(Property property) {
        PropertyResponse response = new PropertyResponse();
        response.setId(property.getId());
        response.setPropertyCode(property.getPropertyCode());
        response.setPropertyName(property.getPropertyName());
        response.setRent(property.getRent());
        response.setDeposit(property.getDeposit());
        response.setStatus(property.getStatus());
        response.setRentalStartDate(property.getRentalStartDate());
        response.setRentalEndDate(property.getRentalEndDate());
        response.setMaterialFileIds(
                propertyMaterialRepository.findByPropertyId(property.getId()).stream()
                        .map(PropertyMaterial::getFileId)
                        .toList()
        );
        response.setVacancyPeriods(
                propertyVacancyPeriodRepository.findByPropertyId(property.getId()).stream().map(v -> {
                    VacancyPeriodDto dto = new VacancyPeriodDto();
                    dto.setStartDate(v.getStartDate());
                    dto.setEndDate(v.getEndDate());
                    return dto;
                }).toList()
        );
        return response;
    }

    private void replaceVacancyPeriods(Long propertyId, List<VacancyPeriodDto> periods) {
        propertyVacancyPeriodRepository.deleteByPropertyId(propertyId);
        if (periods == null) {
            return;
        }
        for (VacancyPeriodDto dto : periods) {
            if (dto.getStartDate() == null || dto.getEndDate() == null || dto.getEndDate().isBefore(dto.getStartDate())) {
                throw new BusinessException("invalid vacancy period range");
            }
            PropertyVacancyPeriod p = new PropertyVacancyPeriod();
            p.setPropertyId(propertyId);
            p.setStartDate(dto.getStartDate());
            p.setEndDate(dto.getEndDate());
            propertyVacancyPeriodRepository.save(p);
        }
    }
}
