package com.markettrust.wallet.repository;

import com.markettrust.wallet.entity.CreditHold;
import com.markettrust.wallet.entity.HoldStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CreditHoldRepository extends JpaRepository<CreditHold, Long> {

    Optional<CreditHold> findByOrderIdAndStatus(String orderId, HoldStatus status);

    Optional<CreditHold> findByOrderId(String orderId);
}
