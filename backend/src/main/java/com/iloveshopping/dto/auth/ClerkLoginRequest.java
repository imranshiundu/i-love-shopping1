package com.iloveshopping.dto.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClerkLoginRequest {

    @NotBlank(message = "Clerk session token is required")
    private String token;
}
