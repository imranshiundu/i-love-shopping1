package com.iloveshopping.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        String jsonResponse = """
                {
                  "success": false,
                  "error": {
                    "statusCode": 401,
                    "error": "Unauthorized",
                    "message": "%s",
                    "timestamp": "%s",
                    "path": "%s"
                  },
                  "timestamp": "%s"
                }
                """.formatted(
                authException.getMessage(),
                java.time.Instant.now().toString(),
                request.getRequestURI(),
                java.time.Instant.now().toString()
        );

        response.getWriter().write(jsonResponse);
    }
}