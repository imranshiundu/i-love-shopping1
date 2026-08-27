package com.iloveshopping.dto.user;

import com.iloveshopping.entity.User;
import com.iloveshopping.entity.Address;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileResponse {

    private String id;
    private String email;
    private String name;
    private String avatar;
    private boolean emailVerified;
    private boolean twoFactorEnabled;
    private Set<User.Role> roles;
    private LocalDateTime createdAt;
    private List<AddressResponse> addresses;

    public static UserProfileResponse from(User user) {
        if (user == null) return null;
        return UserProfileResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .avatar(user.getAvatar())
                .emailVerified(user.getEmailVerified() != null)
                .twoFactorEnabled(user.getTwoFactorEnabled())
                .roles(user.getRoles())
                .createdAt(user.getCreatedAt())
                .addresses(user.getAddresses() != null ? user.getAddresses().stream().map(AddressResponse::from).toList() : List.of())
                .build();
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AddressResponse {
        private String id;
        private String type;
        private String name;
        private String line1;
        private String line2;
        private String city;
        private String state;
        private String postalCode;
        private String country;
        private String phone;
        @JsonProperty("isDefault")
        private boolean isDefault;

        public static AddressResponse from(Address address) {
            if (address == null) return null;
            return AddressResponse.builder()
                    .id(address.getId())
                    .type(address.getType().name())
                    .name(address.getName())
                    .line1(address.getLine1())
                    .line2(address.getLine2())
                    .city(address.getCity())
                    .state(address.getState())
                    .postalCode(address.getPostalCode())
                    .country(address.getCountry())
                    .phone(address.getPhone())
                    .isDefault(address.getIsDefault())
                    .build();
        }
    }
}