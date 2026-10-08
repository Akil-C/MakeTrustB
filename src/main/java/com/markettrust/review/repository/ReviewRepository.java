package com.markettrust.review.repository;

import com.markettrust.review.entity.Review;
import com.markettrust.review.entity.ReviewStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    Optional<Review> findByOrderId(Long orderId);

    boolean existsByOrderIdAndBuyerId(Long orderId, Long buyerId);

    Page<Review> findBySellerIdAndStatusOrderByCreatedAtDesc(
            Long sellerId, ReviewStatus status, Pageable pageable);

    Page<Review> findByProductIdAndStatusOrderByCreatedAtDesc(
            Long productId, ReviewStatus status, Pageable pageable);

    Page<Review> findByBuyerIdOrderByCreatedAtDesc(Long buyerId, Pageable pageable);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.sellerId = :sellerId AND r.status = 'ACTIVE'")
    Double getAverageRatingBySellerId(@Param("sellerId") Long sellerId);

    long countBySellerIdAndStatus(Long sellerId, ReviewStatus status);
}
