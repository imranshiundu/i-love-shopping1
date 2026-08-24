package com.iloveshopping.controller;

import com.iloveshopping.dto.admin.UpdateOrderStatusRequest;
import com.iloveshopping.dto.common.ApiResponse;
import com.iloveshopping.dto.order.OrderResponse;
import com.iloveshopping.dto.user.UserProfileResponse;
import com.iloveshopping.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin", description = "Admin-only endpoints for order and user management")
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/orders")
    @Operation(summary = "List all orders (Admin)")
    public ResponseEntity<ApiResponse<Page<OrderResponse>>> getAllOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Page<OrderResponse> orders = adminService.getAllOrders(page, size);
        return ResponseEntity.ok(ApiResponse.success(orders));
    }

    @PutMapping("/orders/{orderNumber}/status")
    @Operation(summary = "Update order status (Admin)")
    public ResponseEntity<ApiResponse<OrderResponse>> updateOrderStatus(
            @PathVariable String orderNumber,
            @Valid @RequestBody UpdateOrderStatusRequest request) {

        OrderResponse order = adminService.updateOrderStatus(orderNumber, request);
        return ResponseEntity.ok(ApiResponse.success(order));
    }

    @GetMapping("/users")
    @Operation(summary = "List all users (Admin)")
    public ResponseEntity<ApiResponse<List<UserProfileResponse>>> getAllUsers() {

        List<UserProfileResponse> users = adminService.getAllUsers();
        return ResponseEntity.ok(ApiResponse.success(users));
    }
}
