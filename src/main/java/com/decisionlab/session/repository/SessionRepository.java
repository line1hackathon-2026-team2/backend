package com.decisionlab.session.repository;

import com.decisionlab.session.entity.Session;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SessionRepository extends JpaRepository<Session, UUID> {

    Optional<Session> findBySessionToken(String sessionToken);
}
