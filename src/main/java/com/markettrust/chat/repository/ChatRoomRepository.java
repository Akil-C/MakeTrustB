package com.markettrust.chat.repository;

import com.markettrust.chat.entity.ChatRoom;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for {@link ChatRoom} entity.
 */
@Repository
public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    Optional<ChatRoom> findByBuyerIdAndSellerIdAndProductId(Long buyerId, Long sellerId, Long productId);

    List<ChatRoom> findByBuyerIdOrSellerIdOrderByLastMessageAtDesc(Long buyerId, Long sellerId);

    Page<ChatRoom> findByBuyerIdOrSellerId(Long userId, Long userId2, Pageable pageable);
}
