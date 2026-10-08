package com.markettrust.chat.repository;

import com.markettrust.chat.entity.ChatMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for {@link ChatMessage} entity.
 */
@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    List<ChatMessage> findByRoomIdOrderByCreatedAtAsc(Long roomId);

    List<ChatMessage> findByRoomIdAndCreatedAtAfterOrderByCreatedAtAsc(Long roomId, LocalDateTime after);

    long countByRoomIdAndSenderIdNotAndIsReadFalse(Long roomId, Long senderId);

    List<ChatMessage> findByIsFlaggedTrue(Pageable pageable);
}
