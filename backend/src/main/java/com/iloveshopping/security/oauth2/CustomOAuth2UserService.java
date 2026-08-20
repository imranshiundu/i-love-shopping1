package com.iloveshopping.security.oauth2;

import com.iloveshopping.entity.User;
import com.iloveshopping.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        String email = oAuth2User.getAttribute("email");
        if (email == null) {
            throw new OAuth2AuthenticationException("Email not found from OAuth2 provider");
        }

        Optional<User> userOptional = userRepository.findByEmailIgnoreCase(email);
        User user;
        if (userOptional.isPresent()) {
            user = userOptional.get();
        } else {
            user = User.builder()
                    .email(email.toLowerCase())
                    .name(oAuth2User.getAttribute("name"))
                    .avatar(oAuth2User.getAttribute("picture") != null ? oAuth2User.getAttribute("picture") : oAuth2User.getAttribute("avatar_url"))
                    .emailVerified(LocalDateTime.now()) // Since it's from OAuth2, we consider it verified
                    .roles(Set.of(User.Role.USER))
                    .twoFactorEnabled(false)
                    .build();
            userRepository.save(user);
        }

        return new CustomOAuth2User(oAuth2User, user);
    }
}
