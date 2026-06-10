package com.roadmap.securevault.tenant.dto.tenant;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record TenantRegistrationRequest(
        @NotBlank(message = "Company name cannot be blank")
        String companyName,

        @NotBlank(message = "Company code cannot be blank")
        @Pattern(regexp = "^[a-zA-Z0-9_-]{3,32}$",
                message = "Company code must be between 3 and 32 characters and contain only letters, numbers, hyphens, and underscores")
        String companyCode,

        @NotBlank(message = "Owner email cannot be blank")
        @Email(message = "Invalid email format")
        String ownerEmail
) {}
