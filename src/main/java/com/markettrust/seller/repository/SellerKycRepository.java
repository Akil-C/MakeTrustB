package com.markettrust.seller.repository;

import com.markettrust.seller.entity.KycStatus;
import com.markettrust.seller.entity.SellerKyc;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SellerKycRepository extends JpaRepository<SellerKyc, Long> {

    Optional<SellerKyc> findBySellerId(Long sellerId);

    Page<SellerKyc> findByStatus(KycStatus status, Pageable pageable);

    long countByStatus(KycStatus status);
}
