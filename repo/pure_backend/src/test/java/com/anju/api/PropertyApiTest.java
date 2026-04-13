package com.anju.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.anju.common.BusinessException;
import com.anju.common.GlobalExceptionHandler;
import com.anju.controller.PropertyController;
import com.anju.dto.PropertyResponse;
import com.anju.service.PropertyService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = PropertyController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class PropertyApiTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PropertyService propertyService;

    @Test
    void createPropertySuccess() throws Exception {
        when(propertyService.create(any())).thenReturn(buildProperty());

        mockMvc.perform(post("/api/v1/properties")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreatePayload(
                                "P-001", "Property A", new BigDecimal("3000.00"), new BigDecimal("3000.00")
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.propertyCode").value("P-001"));
    }

    @Test
    void createPropertyFailureValidation() throws Exception {
        when(propertyService.create(any())).thenThrow(new BusinessException("propertyCode is required"));

        mockMvc.perform(post("/api/v1/properties")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreatePayload(
                                "", "Property A", new BigDecimal("3000.00"), new BigDecimal("3000.00")
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.msg").value("propertyCode is required"));
    }

    @Test
    void getPropertySuccess() throws Exception {
        when(propertyService.getById(eq(1L))).thenReturn(buildProperty());

        mockMvc.perform(get("/api/v1/properties/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.propertyName").value("Property A"));
    }

    @Test
    void getPropertyFailureNotFound() throws Exception {
        when(propertyService.getById(eq(999L))).thenThrow(new BusinessException("property not found"));

        mockMvc.perform(get("/api/v1/properties/999"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.msg").value("property not found"));
    }

    private record CreatePayload(
            String propertyCode,
            String propertyName,
            BigDecimal rent,
            BigDecimal deposit
    ) {
    }

    private PropertyResponse buildProperty() {
        PropertyResponse response = new PropertyResponse();
        response.setId(1L);
        response.setPropertyCode("P-001");
        response.setPropertyName("Property A");
        response.setRent(new BigDecimal("3000.00"));
        response.setDeposit(new BigDecimal("3000.00"));
        response.setStatus("DRAFT");
        return response;
    }
}
