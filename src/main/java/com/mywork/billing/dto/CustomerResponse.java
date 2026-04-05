package com.mywork.billing.dto;

import com.mywork.billing.domain.Customer;

import java.time.LocalDateTime;

// Response DTO - controls exactly what we expose in the API (never expose entities directly)
public record CustomerResponse(
        Long id,
        String name,
        String email,
        String phone,
        LocalDateTime createdAt
) {
    // Static factory method to map from entity to DTO
    public static CustomerResponse from(Customer customer) {
        return new CustomerResponse(
                customer.getId(),
                customer.getName(),
                customer.getEmail(),
                customer.getPhone(),
                customer.getCreatedAt()
        );
    }
}
