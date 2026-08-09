package com.iloveshopping.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CaptchaResponse {

    private boolean success;
    private String challengeTs;
    private String hostname;
    private String[] errorCodes;
}