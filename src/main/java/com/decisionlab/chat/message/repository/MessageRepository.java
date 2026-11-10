package com.decisionlab.chat.message.repository;

import com.decisionlab.chat.message.entity.Message;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MessageRepository extends JpaRepository<Message, Long> {

    List<Message> findBySessionIdOrderByTurnNoAsc(UUID sessionId);

    /** #7 대화 이력 조회(최근 turnNo부터 limit개). 응답은 호출부에서 다시 오름차순으로 뒤집는다. */
    List<Message> findBySessionIdOrderByTurnNoDesc(UUID sessionId, Pageable pageable);

    List<Message> findBySessionIdAndTurnNoLessThanOrderByTurnNoDesc(UUID sessionId, Integer turnNo, Pageable pageable);
}
