package com.iloveshopping.dto.auth;

import com.iloveshopping.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    private String accessToken;
    private String refreshToken;
    private long expiresIn;
    private UserDto user;
    private boolean twoFactorRequired;
    private String sessionId;
    private String message;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserDto {
        private String id;
        private String email;
        private String name;
        private String avatar;
        private boolean emailVerified;
        private boolean twoFactorEnabled;
        private Set<User.Role> roles;

        public static UserDto from(User user) {
            return UserDto.builder()
                    .id(user.getId())
                    .email(user.getEmail())
                    .name(user.getName())
                    .avatar(user.getAvatar())
                    .emailVerified(user.getEmailVerified() != null)
                    .twoFactorEnabled(user.getTwoFactorEnabled())
                    .roles(user.getRoles())
                    .build();
        }
    }
}