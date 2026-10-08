package com.markettrust.product.repository;

import com.markettrust.product.entity.Product;
import com.markettrust.product.entity.ProductCondition;
import com.markettrust.product.entity.ProductStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    @Query("SELECT p FROM Product p WHERE p.status = 'ACTIVE' "
            + "AND (:keyword IS NULL OR LOWER(p.title) LIKE LOWER(CONCAT('%',:keyword,'%')) "
            +      "OR LOWER(p.description) LIKE LOWER(CONCAT('%',:keyword,'%'))) "
            + "AND (:categoryId IS NULL OR p.categoryId = :categoryId) "
            + "AND (:minPrice IS NULL OR p.priceInCredits >= :minPrice) "
            + "AND (:maxPrice IS NULL OR p.priceInCredits <= :maxPrice) "
            + "AND (:condition IS NULL OR p.condition = :condition)")
    Page<Product> searchProducts(
            @Param("keyword")    String keyword,
            @Param("categoryId") Long categoryId,
            @Param("minPrice")   Long minPrice,
            @Param("maxPrice")   Long maxPrice,
            @Param("condition")  ProductCondition condition,
            Pageable pageable);

    @Query(value = "SELECT p.*, "
            + "(6371 * acos(cos(radians(:lat)) * cos(radians(p.latitude)) "
            + " * cos(radians(p.longitude) - radians(:lng)) "
            + " + sin(radians(:lat)) * sin(radians(p.latitude)))) AS distance "
            + "FROM products p "
            + "WHERE p.status = 'ACTIVE' AND p.latitude IS NOT NULL AND p.longitude IS NOT NULL "
            + "HAVING distance < :radiusKm ORDER BY distance ASC LIMIT :limit",
            nativeQuery = true)
    List<Object[]> findNearbyProducts(
            @Param("lat")      double lat,
            @Param("lng")      double lng,
            @Param("radiusKm") double radiusKm,
            @Param("limit")    int    limit);

    Page<Product> findBySellerId(Long sellerId, Pageable pageable);

    Page<Product> findBySellerIdAndStatus(Long sellerId, ProductStatus status, Pageable pageable);

    Page<Product> findByCategoryIdAndStatus(Long categoryId, ProductStatus status, Pageable pageable);

    List<Product> findByCategoryIdAndStatusAndIdNot(Long categoryId, ProductStatus status, Long excludeId, Pageable pageable);

    Page<Product> findByStatusOrderByCreatedAtDesc(ProductStatus status, Pageable pageable);

    Page<Product> findByStatusOrderByViewsDesc(ProductStatus status, Pageable pageable);

    Page<Product> findByStatusAndIsFeaturedTrue(ProductStatus status, Pageable pageable);

    long countByStatus(ProductStatus status);

    @Modifying
    @Query("UPDATE Product p SET p.views = p.views + 1 WHERE p.id = :id")
    void incrementViews(@Param("id") Long id);
}
