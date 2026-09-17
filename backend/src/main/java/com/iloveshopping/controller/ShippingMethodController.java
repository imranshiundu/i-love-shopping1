package com.iloveshopping.controller;

import com.iloveshopping.dto.common.ApiResponse;
import com.iloveshopping.entity.ShippingMethod;
import com.iloveshopping.repository.ShippingMethodRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(name = "Shipping", description = "Delivery options")
public class ShippingMethodController {

    private final ShippingMethodRepository shippingMethodRepository;

    public record ShippingMethodRequest(
            @NotBlank(message = "Name is required") @Size(max = 100) String name,
            @Size(max = 255) String description,
            @NotNull(message = "Cost is required") @PositiveOrZero(message = "Cost must not be negative") BigDecimal cost,
            @Size(max = 50) String estimatedDays,
            Boolean active,
            Integer displayOrder) {
    }

    @GetMapping("/shipping-methods")
    @Operation(summary = "List active delivery options (public)")
    public ResponseEntity<ApiResponse<List<ShippingMethod>>> listActive() {
        return ResponseEntity.ok(ApiResponse.success(shippingMethodRepository.findAllByActiveTrueOrderByDisplayOrderAsc()));
    }

    @GetMapping("/admin/shipping-methods")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "List all delivery options incl. inactive (Admin)")
    public ResponseEntity<ApiResponse<List<ShippingMethod>>> listAll() {
        return ResponseEntity.ok(ApiResponse.success(shippingMethodRepository.findAllByOrderByDisplayOrderAsc()));
    }

    @PostMapping("/admin/shipping-methods")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a delivery option (Admin)")
    public ResponseEntity<ApiResponse<ShippingMethod>> create(@Valid @RequestBody ShippingMethodRequest request) {
        ShippingMethod method = ShippingMethod.builder()
                .name(request.name())
                .description(request.description())
                .cost(request.cost())
                .estimatedDays(request.estimatedDays())
                .active(request.active() != null ? request.active() : true)
                .displayOrder(request.displayOrder() != null ? request.displayOrder() : 0)
                .build();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(shippingMethodRepository.save(method)));
    }

    @PutMapping("/admin/shipping-methods/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update a delivery option (Admin)")
    public ResponseEntity<ApiResponse<ShippingMethod>> update(
            @PathVariable String id,
            @Valid @RequestBody ShippingMethodRequest request) {
        ShippingMethod method = shippingMethodRepository.findById(id)
                .orElseThrow(() -> new com.iloveshopping.exception.ResourceNotFoundException("ShippingMethod", "id", id));
        method.setName(request.name());
        method.setDescription(request.description());
        method.setCost(request.cost());
        method.setEstimatedDays(request.estimatedDays());
        if (request.active() != null) method.setActive(request.active());
        if (request.displayOrder() != null) method.setDisplayOrder(request.displayOrder());
        return ResponseEntity.ok(ApiResponse.success(shippingMethodRepository.save(method)));
    }

    @DeleteMapping("/admin/shipping-methods/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete a delivery option (Admin)")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String id) {
        if (!shippingMethodRepository.existsById(id)) {
            throw new com.iloveshopping.exception.ResourceNotFoundException("ShippingMethod", "id", id);
        }
        shippingMethodRepository.deleteById(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
