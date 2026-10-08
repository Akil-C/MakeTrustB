package com.markettrust.seller.repository;

import com.markettrust.seller.entity.SellerTrustScore;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SellerTrustScoreRepository extends JpaRepository<SellerTrustScore, Long> {

    @Query("SELECT s FROM SellerTrustScore s WHERE s.sellerProfileId = :sellerId")
    Optional<SellerTrustScore> findBySellerId(@Param("sellerId") Long sellerId);

    Optional<SellerTrustScore> findBySellerProfileId(Long sellerProfileId);
}

