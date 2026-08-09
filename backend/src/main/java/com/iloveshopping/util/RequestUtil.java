package com.iloveshopping.util;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.util.StringUtils;

public class RequestUtil {

    private RequestUtil() {
    }

    public static String getClientIp(HttpServletRequest request) {
        String remoteAddr = "";

        String header = request.getHeader("X-Forwarded-For");
        if (header != null && !header.isBlank()) {
            remoteAddr = header.split(",")[0].trim();
        }

        if (!StringUtils.hasText(remoteAddr) || "unknown".equalsIgnoreCase(remoteAddr)) {
            remoteAddr = request.getHeader("X-Real-IP");
        }

        if (!StringUtils.hasText(remoteAddr) || "unknown".equalsIgnoreCase(remoteAddr)) {
            remoteAddr = request.getRemoteAddr();
        }

        return remoteAddr != null ? remoteAddr : "";
    }

    public static String getUserAgent(HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");
        return userAgent != null ? userAgent : "";
    }
}