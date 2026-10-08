package com.markettrust.order.controller;

import com.markettrust.order.dto.*;
import com.markettrust.order.service.OrderService;
import com.markettrust.security.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller exposing order management endpoints for buyers, sellers and admins.
 */
@RestController
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    // -------------------------------------------------------------------------
    // Buyer endpoints
    // -------------------------------------------------------------------------

    /** POST /api/orders — buyer places an order */
    @PostMapping("/api/orders")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<OrderDto> createOrder(
            @Valid @RequestBody CreateOrderRequest request) {
        Long buyerId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(orderService.createOrder(buyerId, request));
    }

    /** GET /api/orders/{id} — buyer or seller views an order */
    @GetMapping("/api/orders/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<OrderDto> getOrder(
            @PathVariable Long id) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(orderService.getOrderById(userId, id));
    }

    /** GET /api/orders/my/buyer — buyer's own orders */
    @GetMapping("/api/orders/my/buyer")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<OrderDto>> myBuyerOrders(
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        Long buyerId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(orderService.getMyOrdersAsBuyer(buyerId, pageable));
    }

    /** GET /api/orders/my/seller — seller's own orders */
    @GetMapping("/api/orders/my/seller")
    @PreAuthorize("hasAnyRole('SELLER','ROLE_SELLER')")
    public ResponseEntity<Page<OrderDto>> mySellerOrders(
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        Long sellerId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(orderService.getMyOrdersAsSeller(sellerId, pageable));
    }

    /** PUT /api/orders/{id}/status — seller updates shipping; buyer confirms */
    @PutMapping("/api/orders/{id}/status")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<OrderDto> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateOrderStatusRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(orderService.updateOrderStatus(userId, id, request));
    }

    /** PUT /api/orders/{id}/cancel */
    @PutMapping("/api/orders/{id}/cancel")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<OrderDto> cancelOrder(
            @PathVariable Long id,
            @RequestParam(required = false) String reason) {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(orderService.cancelOrder(userId, id, reason));
    }

    /** POST /api/orders/{id}/dispute — buyer opens a dispute */
    @PostMapping("/api/orders/{id}/dispute")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<OrderDto> openDispute(
            @PathVariable Long id,
            @Valid @RequestBody DisputeRequest request) {
        Long buyerId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(orderService.openDispute(buyerId, id, request));
    }

    // -------------------------------------------------------------------------
    // Admin endpoints
    // -------------------------------------------------------------------------

    /** GET /api/admin/orders — admin views all orders */
    @GetMapping("/api/admin/orders")
    @PreAuthorize("hasAnyRole('ADMIN','ROLE_ADMIN')")
    public ResponseEntity<Page<OrderDto>> getAllOrders(
            @PageableDefault(size = 20) Pageable pageable) {
        Long adminId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(orderService.getAllOrders(adminId, pageable));
    }

    /** PUT /api/admin/orders/{id}/status — admin forces a status change */
    @PutMapping("/api/admin/orders/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN','ROLE_ADMIN')")
    public ResponseEntity<OrderDto> adminUpdateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateOrderStatusRequest request) {
        Long adminId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(orderService.adminUpdateOrderStatus(adminId, id, request));
    }
}
