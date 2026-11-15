package com.decisionlab.global.time;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

/**
 * API 응답 시간 변환. 명세서 1장: ISO 8601 + KST 오프셋 (예: 2026-11-02T21:00:00+09:00)
 * 엔티티는 LocalDateTime(서버 기본 시간대)으로 저장하고, 응답 DTO에서만 이걸로 바꿔 내보낸다.
 */
public final class ApiTime {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private ApiTime() {
    }

    public static OffsetDateTime toKst(LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        return dateTime.atZone(ZoneId.systemDefault())
                .withZoneSameInstant(KST)
                .toOffsetDateTime()
                .truncatedTo(ChronoUnit.SECONDS);
    }
}
