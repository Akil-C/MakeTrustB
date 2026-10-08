package com.markettrust.review.controller;

import com.markettrust.review.dto.CreateReviewRequest;
import com.markettrust.review.dto.ReviewDto;
import com.markettrust.review.service.ReviewService;
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

import java.util.Map;

/**
 * REST controller for review management.
 */
@RestController
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    // -------------------------------------------------------------------------
    // Buyer endpoints
    // -------------------------------------------------------------------------

    /** POST /api/reviews — buyer submits a review */
    @PostMapping("/api/reviews")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ReviewDto> createReview(
            @Valid @RequestBody CreateReviewRequest request) {
        Long buyerId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(reviewService.createReview(buyerId, request));
    }

    /** GET /api/reviews/my — buyer views their own reviews */
    @GetMapping("/api/reviews/my")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<ReviewDto>> getMyReviews(
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        Long buyerId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(reviewService.getMyReviews(buyerId, pageable));
    }

    /** GET /api/reviews/can-review/{orderId} — checks if buyer can review */
    @GetMapping("/api/reviews/can-review/{orderId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Boolean>> canReview(
            @PathVariable Long orderId) {
        Long buyerId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(Map.of("canReview", reviewService.canReview(buyerId, orderId)));
    }

    // -------------------------------------------------------------------------
    // Public endpoints
    // -------------------------------------------------------------------------

    /** GET /api/reviews/seller/{sellerId} — public seller reviews */
    @GetMapping("/api/reviews/seller/{sellerId}")
    public ResponseEntity<Page<ReviewDto>> getSellerReviews(
            @PathVariable Long sellerId,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return ResponseEntity.ok(reviewService.getSellerReviews(sellerId, pageable));
    }

    /** GET /api/reviews/product/{productId} — public product reviews */
    @GetMapping("/api/reviews/product/{productId}")
    public ResponseEntity<Page<ReviewDto>> getProductReviews(
            @PathVariable Long productId,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return ResponseEntity.ok(reviewService.getProductReviews(productId, pageable));
    }

    // -------------------------------------------------------------------------
    // Seller endpoint
    // -------------------------------------------------------------------------

    /** PUT /api/reviews/{id}/reply — seller adds reply to a review */
    @PutMapping("/api/reviews/{id}/reply")
    @PreAuthorize("hasAnyRole('SELLER','ROLE_SELLER')")
    public ResponseEntity<ReviewDto> addReply(
            @PathVariable Long id,
            @RequestParam String reply) {
        Long sellerId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(reviewService.addSellerReply(sellerId, id, reply));
    }

    // -------------------------------------------------------------------------
    // Admin endpoint
    // -------------------------------------------------------------------------

    /** PUT /api/admin/reviews/{id}/hide — admin hides a review */
    @PutMapping("/api/admin/reviews/{id}/hide")
    @PreAuthorize("hasAnyRole('ADMIN','ROLE_ADMIN')")
    public ResponseEntity<ReviewDto> hideReview(
            @PathVariable Long id,
            @RequestParam(required = false, defaultValue = "Violates community guidelines") String reason) {
        Long adminId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(reviewService.adminHideReview(adminId, id, reason));
    }
}
