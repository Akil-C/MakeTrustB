package com.markettrust.wishlist.controller;

import com.markettrust.security.SecurityUtils;
import com.markettrust.wishlist.dto.WishlistItemDto;
import com.markettrust.wishlist.service.WishlistService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST controller for wishlist management.
 */
@RestController
@RequestMapping("/api/wishlist")
@RequiredArgsConstructor
public class WishlistController {

    private final WishlistService wishlistService;

    /** GET /api/wishlist — returns full wishlist with price-drop info */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<WishlistItemDto>> getWishlist() {
        Long userId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(wishlistService.getWishlist(userId));
    }

    /** POST /api/wishlist/{productId} — add product to wishlist */
    @PostMapping("/{productId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> addToWishlist(@PathVariable Long productId) {
        Long userId = SecurityUtils.getCurrentUserId();
        wishlistService.addToWishlist(userId, productId);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    /** DELETE /api/wishlist/{productId} — remove product from wishlist */
    @DeleteMapping("/{productId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> removeFromWishlist(@PathVariable Long productId) {
        Long userId = SecurityUtils.getCurrentUserId();
        wishlistService.removeFromWishlist(userId, productId);
        return ResponseEntity.noContent().build();
    }

    /** GET /api/wishlist/{productId}/check — check if product is wishlisted */
    @GetMapping("/{productId}/check")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Boolean>> isInWishlist(@PathVariable Long productId) {
        Long userId = SecurityUtils.getCurrentUserId();
        boolean inWishlist = wishlistService.isInWishlist(userId, productId);
        return ResponseEntity.ok(Map.of("inWishlist", inWishlist));
    }
}
