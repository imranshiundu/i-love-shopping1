package com.iloveshopping.controller;

import com.iloveshopping.dto.common.ApiResponse;
import com.iloveshopping.dto.user.AddressRequest;
import com.iloveshopping.dto.user.ChangePasswordRequest;
import com.iloveshopping.dto.user.UpdateProfileRequest;
import com.iloveshopping.dto.user.UserProfileResponse;
import com.iloveshopping.dto.user.ReviewRequest;
import com.iloveshopping.dto.user.ReviewResponse;
import com.iloveshopping.service.AddressService;
import com.iloveshopping.service.ReviewService;
import com.iloveshopping.service.UserManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(name = "User Profile", description = "User profile and address management")
public class UserController {

    private final UserManagementService userService;
    private final AddressService addressService;

    @GetMapping("/user/profile")
    @Operation(summary = "Get current user profile")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getProfile() {
        UserProfileResponse profile = userService.getCurrentUserProfile();
        return ResponseEntity.ok(ApiResponse.success(profile));
    }

    @PutMapping("/user/profile")
    @Operation(summary = "Update current user profile")
    public ResponseEntity<ApiResponse<UserProfileResponse>> updateProfile(
            @Valid @RequestBody UpdateProfileRequest request) {

        UserProfileResponse profile = userService.updateProfile(request);
        return ResponseEntity.ok(ApiResponse.success(profile));
    }

    @PostMapping("/user/password")
    @Operation(summary = "Change password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @Valid @RequestBody ChangePasswordRequest request) {

        userService.changePassword(request);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    // ===== Addresses =====

    @GetMapping("/user/addresses")
    @Operation(summary = "Get all addresses for current user")
    public ResponseEntity<ApiResponse<List<UserProfileResponse.AddressResponse>>> getAddresses() {
        List<UserProfileResponse.AddressResponse> addresses = addressService.getUserAddresses();
        return ResponseEntity.ok(ApiResponse.success(addresses));
    }

    @PostMapping("/user/addresses")
    @Operation(summary = "Add a new address")
    public ResponseEntity<ApiResponse<UserProfileResponse.AddressResponse>> addAddress(
            @Valid @RequestBody AddressRequest request) {

        UserProfileResponse.AddressResponse address = addressService.addAddress(request);
        return ResponseEntity.ok(ApiResponse.success(address));
    }

    @PutMapping("/user/addresses/{id}")
    @Operation(summary = "Update an address")
    public ResponseEntity<ApiResponse<UserProfileResponse.AddressResponse>> updateAddress(
            @PathVariable String id,
            @Valid @RequestBody AddressRequest request) {

        UserProfileResponse.AddressResponse address = addressService.updateAddress(id, request);
        return ResponseEntity.ok(ApiResponse.success(address));
    }

    @DeleteMapping("/user/addresses/{id}")
    @Operation(summary = "Delete an address")
    public ResponseEntity<ApiResponse<Void>> deleteAddress(
            @PathVariable String id) {

        addressService.deleteAddress(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PostMapping("/user/addresses/{id}/default")
    @Operation(summary = "Set address as default for its type")
    public ResponseEntity<ApiResponse<UserProfileResponse.AddressResponse>> setDefaultAddress(
            @PathVariable String id) {

        UserProfileResponse.AddressResponse address = addressService.setDefaultAddress(id);
        return ResponseEntity.ok(ApiResponse.success(address));
    }
}