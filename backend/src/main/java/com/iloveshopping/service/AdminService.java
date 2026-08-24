package com.iloveshopping.service;

import com.iloveshopping.dto.admin.UpdateOrderStatusRequest;
import com.iloveshopping.dto.order.OrderResponse;
import com.iloveshopping.dto.user.UserProfileResponse;
import com.iloveshopping.entity.Order;
import com.iloveshopping.entity.User;
import com.iloveshopping.exception.ResourceNotFoundException;
import com.iloveshopping.repository.OrderRepository;
import com.iloveshopping.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;

    // ===== Orders =====

    public Page<OrderResponse> getAllOrders(int page, int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Order> orders = orderRepository.findAll(pageable);
        return orders.map(OrderResponse::from);
    }

    @Transactional
    public OrderResponse updateOrderStatus(String orderNumber, UpdateOrderStatusRequest request) {
        Order order = orderRepository.findByNumber(orderNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "number", orderNumber));

        order.setStatus(request.getStatus());
        order = orderRepository.save(order);

        return OrderResponse.from(order);
    }

    // ===== Users =====

    public List<UserProfileResponse> getAllUsers() {
        List<User> users = userRepository.findAll();
        return users.stream()
                .map(UserProfileResponse::from)
                .collect(Collectors.toList());
    }
}
