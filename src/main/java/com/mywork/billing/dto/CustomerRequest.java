package com.mywork.billing.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

// Java record - immutable DTO, compiler generates constructor, getters, equals, hashCode, toString
public record CustomerRequest(

        @NotBlank(message = "Name is required")
        String name,

        @Email(message = "Invalid email format")
        @NotBlank(message = "Email is required")
        String email,

        String phone
) {}
