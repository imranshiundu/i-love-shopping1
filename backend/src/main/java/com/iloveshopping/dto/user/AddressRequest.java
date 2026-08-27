package com.iloveshopping.dto.user;

import com.iloveshopping.entity.Address;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddressRequest {

    @NotNull(message = "Address type is required")
    private Address.AddressType type;

    @NotBlank(message = "Name is required")
    @Size(max = 100, message = "Name must not exceed 100 characters")
    private String name;

    @NotBlank(message = "Address line 1 is required")
    @Size(max = 200, message = "Address line 1 must not exceed 200 characters")
    private String line1;

    @Size(max = 200, message = "Address line 2 must not exceed 200 characters")
    private String line2;

    @NotBlank(message = "City is required")
    @Size(max = 100, message = "City must not exceed 100 characters")
    private String city;

    @NotBlank(message = "State/County is required")
    @Size(max = 100, message = "State must not exceed 100 characters")
    private String state;

    @NotBlank(message = "Postal code is required")
    @Size(max = 20, message = "Postal code must not exceed 20 characters")
    private String postalCode;

    @NotBlank(message = "Country is required")
    @Size(min = 2, max = 2, message = "Country must be a 2-letter code")
    private String country;

    @Size(max = 20, message = "Phone must not exceed 20 characters")
    private String phone;

    @JsonProperty("isDefault")
    private boolean isDefault;
}