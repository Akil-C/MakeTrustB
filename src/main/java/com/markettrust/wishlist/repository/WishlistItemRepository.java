package com.markettrust.wishlist.repository;

import com.markettrust.wishlist.entity.WishlistItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface WishlistItemRepository extends JpaRepository<WishlistItem, Long> {

    Optional<WishlistItem> findByWishlistIdAndProductId(Long wishlistId, Long productId);

    boolean existsByWishlistIdAndProductId(Long wishlistId, Long productId);

    List<WishlistItem> findByWishlistId(Long wishlistId);

    @Transactional
    void deleteByWishlistIdAndProductId(Long wishlistId, Long productId);
}
