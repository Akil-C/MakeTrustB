package com.markettrust.product.repository;

import com.markettrust.product.entity.ProductView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface ProductViewRepository extends JpaRepository<ProductView, Long> {

    boolean existsByProductIdAndIpAddressAndViewedAtAfter(Long productId, String ipAddress, LocalDateTime after);

    boolean existsByProductIdAndViewerIdAndViewedAtAfter(Long productId, Long viewerId, LocalDateTime after);

    long countByProductIdAndViewedAtAfter(Long productId, LocalDateTime after);
}
