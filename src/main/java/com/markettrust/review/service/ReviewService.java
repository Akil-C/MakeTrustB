package com.markettrust.review.service;

import com.markettrust.exception.ResourceNotFoundException;
import com.markettrust.order.entity.Order;
import com.markettrust.order.entity.OrderStatus;
import com.markettrust.order.repository.OrderRepository;
import com.markettrust.product.entity.Product;
import com.markettrust.product.repository.ProductRepository;
import com.markettrust.review.dto.CreateReviewRequest;
import com.markettrust.review.dto.ReviewDto;
import com.markettrust.review.entity.Review;
import com.markettrust.review.entity.ReviewStatus;
import com.markettrust.review.exception.ReviewAlreadyExistsException;
import com.markettrust.review.exception.ReviewNotAllowedException;
import com.markettrust.review.repository.ReviewRepository;
import com.markettrust.seller.entity.SellerLevel;
import com.markettrust.seller.repository.SellerProfileRepository;
import com.markettrust.user.entity.RoleName;
import com.markettrust.user.entity.User;
import com.markettrust.user.repository.UserRepository;
import com.markettrust.order.dto.UserSummaryDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.EnumSet;

/**
 * Review lifecycle service.
 *
 * <p>Reviews are locked to a single verified-purchase order: one review per order per buyer.
 * On creation, the seller's average rating and trust score are recalculated immediately.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository       reviewRepository;
    private final OrderRepository        orderRepository;
    private final ProductRepository      productRepository;
    private final UserRepository         userRepository;
    private final SellerProfileRepository sellerProfileRepository;

    private static final EnumSet<OrderStatus> REVIEWABLE_STATUSES =
            EnumSet.of(OrderStatus.BUYER_CONFIRMED, OrderStatus.COMPLETED);

    // =========================================================================
    // Eligibility check
    // =========================================================================

    /**
     * Returns {@code true} when the buyer is allowed to review the order.
     */
    @Transactional(readOnly = true)
    public boolean canReview(Long buyerId, Long orderId) {
        return orderRepository.findById(orderId)
                .filter(o -> o.getBuyerId().equals(buyerId))
                .filter(o -> REVIEWABLE_STATUSES.contains(o.getStatus()))
                .filter(o -> !reviewRepository.existsByOrderIdAndBuyerId(orderId, buyerId))
                .isPresent();
    }

    // =========================================================================
    // Create review
    // =========================================================================

    @Transactional
    public ReviewDto createReview(Long buyerId, CreateReviewRequest req) {
        // 1-4. Validate
        if (!canReview(buyerId, req.orderId())) {
            throw new ReviewNotAllowedException(
                    "You are not allowed to review order " + req.orderId());
        }
        if (reviewRepository.existsByOrderIdAndBuyerId(req.orderId(), buyerId)) {
            throw new ReviewAlreadyExistsException(
                    "A review already exists for order " + req.orderId());
        }

        Order order = orderRepository.findById(req.orderId())
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + req.orderId()));

        // 5. Build review
        Review review = new Review();
        review.setOrderId(req.orderId());
        review.setProductId(order.getProductId());
        review.setBuyerId(buyerId);
        review.setSellerId(order.getSellerId());
        review.setRating(req.rating());
        review.setTitle(req.title());
        review.setComment(req.comment());
        review.setProductConditionRating(req.productConditionRating());
        review.setSellerCommunicationRating(req.sellerCommunicationRating());
        review.setAccuracyRating(req.accuracyRating());
        review.setIsVerifiedPurchase(true);
        review.setStatus(ReviewStatus.ACTIVE);
        Review saved = reviewRepository.save(review);

        // 6-8. Recalculate seller rating & trust
        recalculateSellerMetrics(order.getSellerId());

        log.info("Review {} created by buyerId={} for orderId={}", saved.getId(), buyerId, req.orderId());
        return toDto(saved);
    }

    // =========================================================================
    // Queries
    // =========================================================================

    @Transactional(readOnly = true)
    public Page<ReviewDto> getSellerReviews(Long sellerId, Pageable pageable) {
        return reviewRepository.findBySellerIdAndStatusOrderByCreatedAtDesc(
                        sellerId, ReviewStatus.ACTIVE, pageable)
                .map(this::toDto);
    }

    @Transactional(readOnly = true)
    public Page<ReviewDto> getProductReviews(Long productId, Pageable pageable) {
        return reviewRepository.findByProductIdAndStatusOrderByCreatedAtDesc(
                        productId, ReviewStatus.ACTIVE, pageable)
                .map(this::toDto);
    }

    @Transactional(readOnly = true)
    public Page<ReviewDto> getMyReviews(Long buyerId, Pageable pageable) {
        return reviewRepository.findByBuyerIdOrderByCreatedAtDesc(buyerId, pageable)
                .map(this::toDto);
    }

    // =========================================================================
    // Seller reply
    // =========================================================================

    @Transactional
    public ReviewDto addSellerReply(Long sellerId, Long reviewId, String reply) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found: " + reviewId));

        if (!review.getSellerId().equals(sellerId)) {
            throw new AccessDeniedException("Only the seller of this review can reply");
        }
        if (review.getSellerReply() != null) {
            throw new ReviewAlreadyExistsException("Seller has already replied to this review");
        }
        review.setSellerReply(reply);
        review.setSellerRepliedAt(LocalDateTime.now());
        return toDto(reviewRepository.save(review));
    }

    // =========================================================================
    // Admin
    // =========================================================================

    @Transactional
    public ReviewDto adminHideReview(Long adminId, Long reviewId, String reason) {
        requireAdmin(adminId);
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found: " + reviewId));
        review.setStatus(ReviewStatus.HIDDEN);
        Review saved = reviewRepository.save(review);

        // Re-calculate seller metrics after hide
        recalculateSellerMetrics(review.getSellerId());

        log.info("Admin {} hid review {}. Reason: {}", adminId, reviewId, reason);
        return toDto(saved);
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private void recalculateSellerMetrics(Long sellerId) {
        sellerProfileRepository.findByUserId(sellerId).ifPresent(sp -> {
            Double avg = reviewRepository.getAverageRatingBySellerId(sellerId);
            long count = reviewRepository.countBySellerIdAndStatus(sellerId, ReviewStatus.ACTIVE);
            sp.setAverageRating(avg != null ? avg : 0.0);
            sp.setReviewCount((int) count);

            // Update trust score: +2 per review, bounded by rating quality
            double reviewPoints = (avg != null ? avg : 0.0) * 2;
            sp.setTrustScore(Math.min(100.0, sp.getTrustScore() + reviewPoints));

            // Promote seller level
            sp.setSellerLevel(computeLevel(sp.getCompletedOrders(), avg));
            sellerProfileRepository.save(sp);
        });
    }

    private SellerLevel computeLevel(int completedOrders, Double avgRating) {
        double rating = avgRating != null ? avgRating : 0.0;
        if (completedOrders >= 200 && rating >= 4.8) return SellerLevel.ELITE;
        if (completedOrders >= 100 && rating >= 4.5) return SellerLevel.PLATINUM;
        if (completedOrders >= 50  && rating >= 4.0) return SellerLevel.GOLD;
        if (completedOrders >= 20  && rating >= 3.5) return SellerLevel.SILVER;
        if (completedOrders >= 5   && rating >= 3.0) return SellerLevel.BRONZE;
        return SellerLevel.NEW;
    }

    private void requireAdmin(Long adminId) {
        User admin = userRepository.findById(adminId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + adminId));
        boolean isAdmin = admin.getRoles().stream()
                .anyMatch(r -> r.getName() == RoleName.ADMIN || r.getName() == RoleName.ROLE_ADMIN);
        if (!isAdmin) {
            throw new AccessDeniedException("Admin access required");
        }
    }

    private ReviewDto toDto(Review r) {
        User buyer = userRepository.findById(r.getBuyerId()).orElse(null);
        Product product = productRepository.findById(r.getProductId()).orElse(null);
        return new ReviewDto(
                r.getId(),
                r.getOrderId(),
                r.getProductId(),
                product != null ? product.getTitle() : null,
                buyer != null
                        ? new UserSummaryDto(buyer.getId(), buyer.getName(), null, buyer.getProfileImageUrl())
                        : null,
                r.getRating(),
                r.getTitle(),
                r.getComment(),
                r.getProductConditionRating(),
                r.getSellerCommunicationRating(),
                r.getAccuracyRating(),
                r.getIsVerifiedPurchase(),
                r.getStatus(),
                r.getSellerReply(),
                r.getSellerRepliedAt(),
                r.getCreatedAt()
        );
    }
}
