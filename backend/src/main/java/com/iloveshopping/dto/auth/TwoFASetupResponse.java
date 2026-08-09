package com.iloveshopping.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TwoFASetupResponse {

    private String secret;
    private String qrCodeUrl;
    private String manualEntryKey;
}