package com.iloveshopping.service;

import com.iloveshopping.dto.admin.AdminStatsResponse;
import com.iloveshopping.dto.admin.UpdateOrderStatusRequest;
import com.iloveshopping.dto.order.OrderResponse;
import com.iloveshopping.dto.user.UserProfileResponse;
import com.iloveshopping.entity.Order;
import com.iloveshopping.entity.Product;
import com.iloveshopping.entity.User;
import com.iloveshopping.exception.ResourceNotFoundException;
import com.iloveshopping.repository.OrderRepository;
import com.iloveshopping.repository.ProductRepository;
import com.iloveshopping.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;

    private static final Set<Order.OrderStatus> PAID_STATUSES = Set.of(
            Order.OrderStatus.CONFIRMED, Order.OrderStatus.PROCESSING,
            Order.OrderStatus.SHIPPED, Order.OrderStatus.DELIVERED);

    // ===== Stats =====

    public AdminStatsResponse getStats() {
        List<Order> allOrders = orderRepository.findAll();
        List<Product> allProducts = productRepository.findAll();
        List<User> allUsers = userRepository.findAll();

        BigDecimal totalRevenue = BigDecimal.ZERO;
        BigDecimal cancelledValue = BigDecimal.ZERO;
        BigDecimal refundedValue = BigDecimal.ZERO;
        long pendingOrders = 0;

        Map<String, Long> ordersByStatus = new LinkedHashMap<>();
        for (Order.OrderStatus status : Order.OrderStatus.values()) {
            ordersByStatus.put(status.name(), 0L);
        }

        for (Order order : allOrders) {
            ordersByStatus.merge(order.getStatus().name(), 1L, Long::sum);
            switch (order.getStatus()) {
                case CANCELLED -> cancelledValue = cancelledValue.add(order.getTotal());
                case REFUNDED -> refundedValue = refundedValue.add(order.getTotal());
                case PENDING -> pendingOrders++;
                default -> totalRevenue = totalRevenue.add(order.getTotal());
            }
        }
        BigDecimal pendingValue = pendingValue(allOrders);

        long paidCount = allOrders.stream().filter(o -> PAID_STATUSES.contains(o.getStatus())).count();
        BigDecimal averageOrderValue = paidCount == 0 ? BigDecimal.ZERO :
                totalRevenue.divide(BigDecimal.valueOf(paidCount), 2, RoundingMode.HALF_UP);

        LocalDateTime sevenDaysAgo = LocalDate.now().minusDays(6).atStartOfDay();
        Map<LocalDate, List<Order>> byDay = allOrders.stream()
                .filter(o -> o.getCreatedAt() != null && o.getCreatedAt().isAfter(sevenDaysAgo))
                .collect(Collectors.groupingBy(o -> o.getCreatedAt().toLocalDate()));

        List<AdminStatsResponse.DayPoint> revenueByDay = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate day = LocalDate.now().minusDays(i);
            List<Order> dayOrders = byDay.getOrDefault(day, List.of());
            revenueByDay.add(AdminStatsResponse.DayPoint.builder()
                    .date(day.toString())
                    .revenue(dayOrders.stream()
                            .filter(o -> PAID_STATUSES.contains(o.getStatus()))
                            .map(Order::getTotal)
                            .reduce(BigDecimal.ZERO, BigDecimal::add))
                    .orders(dayOrders.size())
                    .build());
        }

        Map<String, AdminStatsResponse.TopProduct> productAgg = new HashMap<>();
        allOrders.stream()
                .filter(o -> PAID_STATUSES.contains(o.getStatus()))
                .flatMap(o -> o.getItems().stream())
                .forEach(item -> {
                    AdminStatsResponse.TopProduct agg = productAgg.computeIfAbsent(item.getName(),
                            name -> AdminStatsResponse.TopProduct.builder()
                                    .name(name)
                                    .slug(item.getProduct() != null ? item.getProduct().getSlug() : null)
                                    .unitsSold(0)
                                    .revenue(BigDecimal.ZERO)
                                    .build());
                    agg.setUnitsSold(agg.getUnitsSold() + item.getQuantity());
                    agg.setRevenue(agg.getRevenue().add(
                            item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()))));
                });

        List<AdminStatsResponse.TopProduct> topProducts = productAgg.values().stream()
                .sorted(Comparator.comparingLong(AdminStatsResponse.TopProduct::getUnitsSold).reversed())
                .limit(5)
                .toList();

        List<AdminStatsResponse.LowStockProduct> lowStock = allProducts.stream()
                .filter(Product::getIsActive)
                .filter(p -> p.getStock() <= 5)
                .sorted(Comparator.comparingInt(Product::getStock))
                .limit(8)
                .map(p -> AdminStatsResponse.LowStockProduct.builder()
                        .id(p.getId()).name(p.getName()).stock(p.getStock()).price(p.getPrice())
                        .build())
                .toList();

        List<OrderResponse> recentOrders = allOrders.stream()
                .sorted(Comparator.comparing(Order::getCreatedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(8)
                .map(OrderResponse::from)
                .toList();

        return AdminStatsResponse.builder()
                .totalRevenue(totalRevenue.max(BigDecimal.ZERO))
                .cancelledValue(cancelledValue)
                .refundedValue(refundedValue)
                .averageOrderValue(averageOrderValue)
                .totalOrders(allOrders.size())
                .totalCustomers(allUsers.stream().filter(u -> u.getRoles() != null && u.getRoles().stream()
                        .noneMatch(r -> r == User.Role.ADMIN)).count())
                .totalProducts(allProducts.size())
                .activeProducts(allProducts.stream().filter(Product::getIsActive).count())
                .pendingOrders(pendingOrders)
                .pendingValue(pendingValue)
                .ordersByStatus(ordersByStatus)
                .revenueByDay(revenueByDay)
                .topProducts(topProducts)
                .lowStock(lowStock)
                .recentOrders(recentOrders)
                .build();
    }

    private BigDecimal pendingValue(List<Order> orders) {
        return orders.stream()
                .filter(o -> o.getStatus() == Order.OrderStatus.PENDING)
                .map(Order::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // ===== Orders =====

    public Page<OrderResponse> getAllOrders(int page, int size, String status) {
        Sort sort = Sort.by(Sort.Direction.DESC, "createdAt");
        if (status != null && !status.isBlank()) {
            Order.OrderStatus parsed = Order.OrderStatus.valueOf(status.toUpperCase());
            List<Order> matching = orderRepository.findByStatus(parsed);
            List<OrderResponse> responses = matching.stream().map(OrderResponse::from).toList();
            int start = (int) Math.min((long) page * size, responses.size());
            int end = Math.min(start + size, responses.size());
            return new PageImpl<>(responses.subList(start, end), PageRequest.of(page, size), responses.size());
        }
        PageRequest pageable = PageRequest.of(page, size, sort);
        return orderRepository.findAll(pageable).map(OrderResponse::from);
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
