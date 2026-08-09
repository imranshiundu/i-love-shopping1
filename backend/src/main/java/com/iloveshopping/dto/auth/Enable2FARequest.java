package com.iloveshopping.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Enable2FARequest {

    @NotBlank(message = "2FA code is required")
    @Size(min = 6, max = 6, message = "2FA code must be 6 digits")
    private String code;
}