package com.iloveshopping.security.oauth2;

import com.iloveshopping.config.JwtProperties;
import com.iloveshopping.entity.Session;
import com.iloveshopping.repository.SessionRepository;
import com.iloveshopping.security.JwtService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtService jwtService;
    private final SessionRepository sessionRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProperties jwtProperties;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        CustomOAuth2User oauth2User = (CustomOAuth2User) authentication.getPrincipal();
        var user = oauth2User.getUser();

        String sessionId = UUID.randomUUID().toString();
        String refreshToken = jwtService.generateRefreshToken(user, sessionId);
        String refreshTokenHash = passwordEncoder.encode(refreshToken);

        sessionRepository.findByUserId(user.getId())
                .forEach(session -> sessionRepository.deleteById(session.getId()));

        Session session = Session.builder()
                .id(sessionId)
                .user(user)
                .refreshTokenHash(refreshTokenHash)
                .userAgent(request.getHeader("User-Agent"))
                .ip(request.getRemoteAddr())
                .expiresAt(LocalDateTime.now().plusDays(jwtProperties.getRefreshExpiryDays()))
                .build();

        sessionRepository.save(session);

        String accessToken = jwtService.generateAccessToken(user, sessionId);

        String targetUrl = UriComponentsBuilder.fromUriString(frontendUrl + "/oauth2/redirect")
                .queryParam("accessToken", accessToken)
                .queryParam("refreshToken", refreshToken)
                .build().toUriString();

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}
