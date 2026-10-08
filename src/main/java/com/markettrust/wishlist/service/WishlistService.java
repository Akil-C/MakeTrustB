package com.markettrust.wishlist.service;

import com.markettrust.exception.ResourceNotFoundException;
import com.markettrust.product.entity.Product;
import com.markettrust.product.entity.ProductImage;
import com.markettrust.product.entity.ProductStatus;
import com.markettrust.product.repository.ProductRepository;
import com.markettrust.product.dto.ProductSummaryDto;
import com.markettrust.wishlist.dto.WishlistItemDto;
import com.markettrust.wishlist.entity.Wishlist;
import com.markettrust.wishlist.entity.WishlistItem;
import com.markettrust.wishlist.repository.WishlistItemRepository;
import com.markettrust.wishlist.repository.WishlistRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Manages the per-user wishlist and provides price-drop detection.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WishlistService {

    private final WishlistRepository     wishlistRepository;
    private final WishlistItemRepository wishlistItemRepository;
    private final ProductRepository      productRepository;

    // =========================================================================
    // Public API
    // =========================================================================

    /**
     * Returns all wishlist items for the user, annotated with price-drop information.
     */
    @Transactional(readOnly = true)
    public List<WishlistItemDto> getWishlist(Long userId) {
        Wishlist wishlist = getOrCreateWishlist(userId);
        List<WishlistItem> items = wishlistItemRepository.findByWishlistId(wishlist.getId());
        return items.stream()
                .map(this::toDto)
                .toList();
    }

    /**
     * Adds a product to the user's wishlist.  Idempotent — duplicate adds are silently ignored.
     */
    @Transactional
    public void addToWishlist(Long userId, Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + productId));

        Wishlist wishlist = getOrCreateWishlist(userId);

        if (wishlistItemRepository.existsByWishlistIdAndProductId(wishlist.getId(), productId)) {
            log.debug("Product {} already in wishlist for userId={}", productId, userId);
            return;
        }

        WishlistItem item = new WishlistItem();
        item.setWishlist(wishlist);
        item.setProductId(productId);
        item.setPriceAtAdd(product.getPriceInCredits() != null ? BigDecimal.valueOf(product.getPriceInCredits()) : BigDecimal.ZERO);

        try {
            wishlistItemRepository.save(item);
            // Increment product wishlist counter
            product.setWishlistCount(product.getWishlistCount() + 1);
            productRepository.save(product);
        } catch (DataIntegrityViolationException ignored) {
            // Race condition: another thread added the same item — safe to ignore
        }
        log.info("Product {} added to wishlist for userId={}", productId, userId);
    }

    /**
     * Removes a product from the user's wishlist.
     */
    @Transactional
    public void removeFromWishlist(Long userId, Long productId) {
        Wishlist wishlist = getOrCreateWishlist(userId);
        if (!wishlistItemRepository.existsByWishlistIdAndProductId(wishlist.getId(), productId)) {
            return; // already not in wishlist
        }
        wishlistItemRepository.deleteByWishlistIdAndProductId(wishlist.getId(), productId);

        // Decrement product wishlist counter
        productRepository.findById(productId).ifPresent(p -> {
            p.setWishlistCount(Math.max(0, p.getWishlistCount() - 1));
            productRepository.save(p);
        });
        log.info("Product {} removed from wishlist for userId={}", productId, userId);
    }

    /**
     * Returns {@code true} if the product is in the user's wishlist.
     */
    @Transactional(readOnly = true)
    public boolean isInWishlist(Long userId, Long productId) {
        return wishlistRepository.findByUserId(userId)
                .map(w -> wishlistItemRepository.existsByWishlistIdAndProductId(w.getId(), productId))
                .orElse(false);
    }

    /**
     * Removes all items from the user's wishlist.
     */
    @Transactional
    public void clearWishlist(Long userId) {
        wishlistRepository.findByUserId(userId).ifPresent(w -> {
            wishlistItemRepository.findByWishlistId(w.getId())
                    .forEach(item -> {
                        productRepository.findById(item.getProductId()).ifPresent(p -> {
                            p.setWishlistCount(Math.max(0, p.getWishlistCount() - 1));
                            productRepository.save(p);
                        });
                        wishlistItemRepository.delete(item);
                    });
        });
        log.info("Wishlist cleared for userId={}", userId);
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private Wishlist getOrCreateWishlist(Long userId) {
        return wishlistRepository.findByUserId(userId).orElseGet(() -> {
            Wishlist w = new Wishlist();
            w.setUserId(userId);
            return wishlistRepository.save(w);
        });
    }

    private WishlistItemDto toDto(WishlistItem item) {
        Optional<Product> productOpt = productRepository.findById(item.getProductId());
        if (productOpt.isEmpty()) {
            // Product was deleted — return a minimal placeholder
            return new WishlistItemDto(item.getId(), null, item.getPriceAtAdd(),
                    null, false, null, item.getCreatedAt());
        }
        Product p = productOpt.get();
        BigDecimal currentPrice  = p.getPriceInCredits() != null ? BigDecimal.valueOf(p.getPriceInCredits()) : BigDecimal.ZERO;
        BigDecimal priceAtAdd    = item.getPriceAtAdd() != null ? item.getPriceAtAdd() : currentPrice;
        BigDecimal dropAmount    = priceAtAdd.subtract(currentPrice);
        boolean    hasDrop       = dropAmount.compareTo(BigDecimal.ZERO) > 0;

        String primaryImage = p.getImages().stream()
                .filter(i -> Boolean.TRUE.equals(i.getIsPrimary()))
                .findFirst()
                .map(ProductImage::getUrl)
                .orElse(null);

        ProductSummaryDto productDto = ProductSummaryDto.builder()
                .id(p.getId())
                .title(p.getTitle())
                .priceInCredits(p.getPriceInCredits())
                .primaryImageUrl(primaryImage)
                .status(p.getStatus())
                .build();

        return new WishlistItemDto(
                item.getId(),
                productDto,
                priceAtAdd,
                currentPrice,
                hasDrop,
                hasDrop ? dropAmount : null,
                item.getCreatedAt()
        );
    }
}
