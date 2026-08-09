package com.iloveshopping.util;

import com.iloveshopping.config.RecaptchaProperties;
import com.iloveshopping.dto.auth.CaptchaResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class CaptchaUtil {

    private final RecaptchaProperties recaptchaProperties;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public boolean verify(String captchaToken) {
        if (captchaToken == null || captchaToken.isBlank()) {
            log.warn("CAPTCHA token is empty");
            return false;
        }

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("secret", recaptchaProperties.getSecretKey());
        formData.add("response", captchaToken);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(formData, headers);

        try {
            String url = recaptchaProperties.getVerifyUrl();
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    url, request, Map.class
            );

            Map<String, Object> body = response.getBody();
            if (body == null) {
                log.warn("CAPTCHA verification failed - null response");
                return false;
            }

            Boolean success = (Boolean) body.get("success");
            if (success == null || !success) {
                log.warn("CAPTCHA verification failed - success=false");
                return false;
            }

            Double score = (Double) body.get("score");
            if (score != null && score < 0.5) {
                log.warn("CAPTCHA score too low: {}", score);
                return false;
            }

            log.debug("CAPTCHA verification successful");
            return true;
        } catch (Exception e) {
            log.error("CAPTCHA verification failed: {}", e.getMessage(), e);
            return false;
        }
    }

    public String getSiteKey() {
        return recaptchaProperties.getSiteKey();
    }
}