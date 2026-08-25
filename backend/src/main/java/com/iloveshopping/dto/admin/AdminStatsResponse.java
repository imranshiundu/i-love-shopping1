package com.iloveshopping.dto.admin;

import com.iloveshopping.dto.order.OrderResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminStatsResponse {

    private BigDecimal totalRevenue;
    private BigDecimal cancelledValue;
    private BigDecimal refundedValue;
    private BigDecimal averageOrderValue;

    private long totalOrders;
    private long totalCustomers;
    private long totalProducts;
    private long activeProducts;
    private long pendingOrders;
    private BigDecimal pendingValue;

    private Map<String, Long> ordersByStatus;
    private List<DayPoint> revenueByDay;
    private List<TopProduct> topProducts;
    private List<LowStockProduct> lowStock;
    private List<OrderResponse> recentOrders;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DayPoint {
        private String date;
        private BigDecimal revenue;
        private long orders;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopProduct {
        private String name;
        private String slug;
        private long unitsSold;
        private BigDecimal revenue;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LowStockProduct {
        private String id;
        private String name;
        private int stock;
        private BigDecimal price;
    }
}
