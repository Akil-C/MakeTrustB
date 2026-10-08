package com.markettrust.seller.repository;

import com.markettrust.seller.entity.SellerProfile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SellerProfileRepository extends JpaRepository<SellerProfile, Long> {

    Optional<SellerProfile> findByUserId(Long userId);

    Page<SellerProfile> findByIsVerifiedTrueOrderByTrustScoreDesc(Pageable pageable);
}
