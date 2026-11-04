package com.decisionlab.chat.message.repository;

import com.decisionlab.chat.message.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MessageRepository extends JpaRepository<Message, Long> {

    List<Message> findBySessionIdOrderByTurnNoAsc(UUID sessionId);
}
