package com.decisionlab.session.repository;

import com.decisionlab.session.entity.Session;
import com.decisionlab.session.entity.SessionState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface SessionRepository extends JpaRepository<Session, UUID> {

    Optional<Session> findBySessionToken(String sessionToken);

    /** #17 이어하기 코드로 세션 찾기 */
    Optional<Session> findByResumeCode(String resumeCode);

    boolean existsByResumeCode(String resumeCode);

    /**
     * 동시 요청 처리
     * 상태가 expected일 때만 next로 바꾼다. 바뀐 행 수(0 또는 1)를 반환한다.
     * 동시 요청 중 먼저 커밋한 쪽만 1을 받고, 나머지는 0을 받는다.
     */
    @Modifying(flushAutomatically = true)
    @Query("update Session s set s.state = :next where s.id = :id and s.state = :expected")
    int updateStateIfMatches(@Param("id") UUID id,
                             @Param("expected") SessionState expected,
                             @Param("next") SessionState next);
}
