package com.markettrust.order.repository;

import com.markettrust.order.entity.Order;
import com.markettrust.order.entity.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    Optional<Order> findByOrderNumber(String orderNumber);

    Optional<Order> findByIdempotencyKey(String key);

    Page<Order> findByBuyerIdOrderByCreatedAtDesc(Long buyerId, Pageable pageable);

    Page<Order> findBySellerIdOrderByCreatedAtDesc(Long sellerId, Pageable pageable);

    Page<Order> findByStatus(OrderStatus status, Pageable pageable);

    List<Order> findByProductId(Long productId);

    @Query("SELECT COUNT(o) FROM Order o WHERE o.status IN " +
           "('PENDING','CONFIRMED','PROCESSING','READY_FOR_SHIPPING','SHIPPED','OUT_FOR_DELIVERY','DELIVERED') " +
           "AND o.buyerId = :buyerId")
    long countActiveOrdersByBuyer(@Param("buyerId") Long buyerId);

    long countByStatus(OrderStatus status);
}
