# Decision Lab API 명세서 (v0.1)

> 기획서 v1 · IA · ERD 기준. 표시: **P0** MVP 필수 / **P1** 후순위

## 1. 공통 규칙

| 항목 | 내용 |
|---|---|
| Base URL | `/api/v1` |
| 인증 | 로그인 없음. `POST /sessions` 응답의 `sessionToken`을 헤더 `X-Session-Token`에 담아 전송 |
| Content-Type | `application/json` (UTF-8) |
| 시간 | ISO 8601 (`2026-11-02T21:00:00+09:00`) |
| 미확인 값 | 사용자가 밝히지 않은 항목은 추정하지 않고 문자열 `"unknown"` |
| 소유권 | 토큰의 세션과 `{sessionId}`가 다르면 403 |
| ID 형식 | `sessionId`만 UUID 문자열(예: `b7e1c2a0-...`), 그 외 ID(`concernId`, `decisionId`, `criterionId`, `dilemmaId`, `reportId` 등)는 숫자 문자열(Long) |
| LLM 호출 | 동기 응답, 서버 타임아웃 30초, 재시도 최대 2회, 실패 시 502 `LLM_FAILED` (딜레마는 템플릿 폴백) |
| 동시 요청 | 같은 세션의 동시 요청은 상태 검증으로 막고, 허용되지 않는 상태면 409 |

### 에러 응답 형식

```json
{
  "error": {
    "code": "INVALID_STATE_TRANSITION",
    "message": "현재 상태(EXPLORING)에서는 딜레마 응답을 할 수 없습니다.",
    "currentState": "EXPLORING"
  }
}
```

| HTTP | code | 설명 |
|---|---|---|
| 400 | VALIDATION_ERROR | 필수값 누락, enum 불일치 |
| 401 | UNAUTHORIZED | 토큰 없음/불일치 |
| 403 | FORBIDDEN | 다른 세션 접근 |
| 404 | NOT_FOUND | 세션/리소스 없음 |
| 409 | INVALID_STATE_TRANSITION | 허용되지 않은 상태 전이 |
| 409 | RECONSIDER_LIMIT | 재검토 루프 한도(1회) 초과 |
| 410 | SESSION_EXPIRED | 세션 만료 |
| 502 | LLM_FAILED | LLM 호출/스키마 검증 실패 (재시도 후에도 실패 시) |

### Enum

| 이름 | 값 |
|---|---|
| state | `CREATED` `EXPLORING` `PROVISIONAL_DECISION` `DILEMMA` `REFLECTION` `RECONSIDER` `REPORT` `PAUSED` |
| personaCode | `ACHIEVEMENT` `STABILITY` `EXPLORATION` (`DILEMMA_MANAGER`는 선택 불가, 딜레마 전용) |
| decision action | `APPROVE` `REVISE` `UNDECIDED` |
| dilemma choice | `KEEP` `RECONSIDER` `UNDECIDED` |
| dilemmaType | `VALUE_CONFLICT` `OPPORTUNITY_COST` `ALT_PATH` `UNCERTAINTY` |
| reportType | `REPORT` `UNDECIDED_REPORT` `INCOMPLETE_REPORT` |
| inputType | `BUTTON` `FREE_TEXT` |

## 2. 상태 전이 표

| 호출 | 허용 현재 상태 | 결과 상태 |
|---|---|---|
| PUT concern | CREATED | CREATED |
| POST persona (최초) | CREATED | EXPLORING |
| POST persona (전환) | EXPLORING | EXPLORING |
| POST messages | EXPLORING | EXPLORING |
| POST decision/draft | EXPLORING | PROVISIONAL_DECISION |
| POST decision/{id}/resolve `APPROVE` (reconsiderCount = 0) | PROVISIONAL_DECISION | DILEMMA |
| POST decision/{id}/resolve `APPROVE` (reconsiderCount ≥ 1) | PROVISIONAL_DECISION | REPORT (REPORT, 딜레마 재생성 없음) |
| POST decision/{id}/resolve `REVISE` | PROVISIONAL_DECISION | EXPLORING |
| POST decision/{id}/resolve `UNDECIDED` | PROVISIONAL_DECISION | REPORT (UNDECIDED_REPORT, 딜레마 없음) |
| POST dilemma/respond `KEEP` | DILEMMA | REFLECTION |
| POST dilemma/respond `RECONSIDER` | DILEMMA | RECONSIDER |
| POST dilemma/respond `UNDECIDED` | DILEMMA | REPORT (UNDECIDED_REPORT) |
| PUT dilemma/followup (KEEP) | REFLECTION | REPORT (REPORT) |
| PUT dilemma/followup (RECONSIDER) | RECONSIDER | EXPLORING |
| POST end `INCOMPLETE_REPORT` | EXPLORING | REPORT (INCOMPLETE_REPORT) |
| POST end `SAVE` (P1) | EXPLORING | PAUSED |

## 3. 엔드포인트 요약

| # | Method | Path | 화면 | 기능 | 우선순위 |
|---|---|---|---|---|---|
| 1 | POST | `/sessions` | S01 | 세션 생성 | P0 |
| 2 | GET | `/sessions/{sessionId}` | 전 화면 | 세션 상태 조회(새로고침 복원) | P0 |
| 3 | PUT | `/sessions/{sessionId}/concern` | S02 | 고민 입력 | P0 |
| 4 | GET | `/personas` | S03 | 자아 카드 목록 | P0 |
| 5 | POST | `/sessions/{sessionId}/persona` | S03, O1, S08 | 자아 선택/전환 | P0 |
| 6 | POST | `/sessions/{sessionId}/messages` | S04 | 사용자 발화 전송 + AI 응답 | P0 |
| 7 | GET | `/sessions/{sessionId}/messages` | S04 | 대화 이력 조회 | P0 |
| 8 | POST | `/sessions/{sessionId}/decision/draft` | S04→S05 | 잠정 결정 요약 생성 | P0 |
| 9 | POST | `/sessions/{sessionId}/decision/{decisionId}/resolve` | S05 | 승인/수정/미결정 | P0 |
| 10 | POST | `/sessions/{sessionId}/dilemma/respond` | S06 | 유지/흔들림/미결정 선택 | P0 |
| 11 | PUT | `/sessions/{sessionId}/dilemma/followup` | S07, S08 | 유지 이유 / 흔들린 가치 입력 | P0 |
| 12 | POST | `/sessions/{sessionId}/end` | O2 | 그만 대화하기 | P0 |
| 13 | GET | `/sessions/{sessionId}/report` | S09 | 리포트 조회 | P0 |
| 14 | POST | `/sessions/resume` | S01, S10 | 저장 세션 이어하기 | P1 |

## 4. 상세 명세

### 4.1 세션 생성 `POST /sessions` (S01)

Request: body 없음

Response `201`
```json
{
  "sessionId": "b7e1c2a0-...",
  "sessionToken": "eyJhbGciOi...",
  "state": "CREATED",
  "expiresAt": "2026-11-09T21:00:00+09:00"
}
```

만료: 마지막 활동 후 **7일** (요청이 올 때마다 `expiresAt` 갱신). 같은 토큰이면 새 탭에서도 같은 세션으로 접근할 수 있다(프론트는 localStorage에 토큰 저장).

### 4.2 세션 조회 `GET /sessions/{sessionId}`

Response `200`
```json
{
  "sessionId": "b7e1c2a0-...",
  "state": "EXPLORING",
  "currentPersonaCode": "ACHIEVEMENT",
  "reconsiderCount": 0,
  "concern": { "freeText": "취업/대학원 고민", "concernType": "CAREER", "initialPreference": null },
  "provisionalDecision": null,
  "dilemma": null,
  "reportId": null
}
```

### 4.3 고민 입력 `PUT /sessions/{sessionId}/concern` (S02, F01)

수정 가능(같은 API로 덮어쓰기). 초기 선호는 비워도 진행 가능.

Request
```json
{
  "freeText": "취업과 대학원 진학 사이에서 고민 중이에요.",
  "concernType": "CAREER",
  "initialPreference": "GRAD_SCHOOL"
}
```
| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| freeText | string | O | 자유 입력, 1~1000자 |
| concernType | string | O | `CAREER`(취업·대학원 등 진로) / `MAJOR`(전공·학업) / `LEAVE`(휴학·시기) / `OTHER` |
| initialPreference | string | X | 초기 가설. AI는 확정 선택이 아닌 가설로 취급 |

Response `200`: `{ "state": "CREATED", "concernId": "..." }`

### 4.4 자아 목록 `GET /personas` (S03)

Response `200`
```json
{
  "personas": [
    { "code": "ACHIEVEMENT", "name": "성취의 나", "coreValue": "성장, 목표, 역량", "sampleQuestion": "그 선택이 네가 원하는 목표에 실제로 가까워지는 길일까?", "themeColor": "#..." },
    { "code": "STABILITY", "name": "안정의 나", "coreValue": "경제, 시간, 위험", "sampleQuestion": "...", "themeColor": "#..." },
    { "code": "EXPLORATION", "name": "탐색의 나", "coreValue": "가능성, 경험, 자율성", "sampleQuestion": "...", "themeColor": "#..." }
  ]
}
```

### 4.5 자아 선택/전환 `POST /sessions/{sessionId}/persona` (S03, O1, S08)

전환 시 기존 대화와 판단 기준은 유지되고, 서버가 handoff 요약을 만들어 새 자아에 전달한다.

Request
```json
{ "personaCode": "STABILITY", "reason": "SWITCH" }
```
`reason`: `USER_SELECT`(최초) / `SWITCH`(대화 중 전환) / `RECONSIDER`(딜레마 후 이동)

Response `200`
```json
{
  "state": "EXPLORING",
  "personaCode": "STABILITY",
  "openingMessage": {
    "messageId": "...",
    "content": "성취의 나와 이야기하며 연구 전문성을 중요하게 생각한다고 했지. 이번 제안 때문에 현실적인 부담이 신경 쓰이기 시작한 거야?",
    "options": [ { "optionId": "o1", "label": "맞아" }, { "optionId": "o2", "label": "조금 달라" }, { "optionId": "o3", "label": "아직 모르겠어" } ]
  }
}
```

### 4.6 메시지 전송 `POST /sessions/{sessionId}/messages` (S04, F03)

한 번에 질문 1개, 선택지 2~3개 + 자유 입력. 버튼과 자유 입력은 동등하게 처리.

Request
```json
{
  "inputType": "BUTTON",
  "selectedOptionId": "o1",
  "content": null
}
```
자유 입력이면 `inputType: "FREE_TEXT"`, `content`에 텍스트, `selectedOptionId`는 null.

Response `200`
```json
{
  "userMessageId": "...",
  "aiMessage": {
    "messageId": "...",
    "personaCode": "ACHIEVEMENT",
    "content": "연구 역량이 왜 중요한지 조금 더 말해줄래?",
    "options": [ { "optionId": "o1", "label": "장기적으로 연구개발을 하고 싶어서" }, { "optionId": "o2", "label": "학위가 필요해서" } ]
  },
  "turnNo": 3,
  "canProceedToDecision": true
}
```
`canProceedToDecision`: 약 3~5턴 이후 true를 권장하지만 "이 정도면 충분해요" 버튼은 턴 수와 무관하게 항상 노출 (프론트에서 항상 활성화).

### 4.7 대화 이력 `GET /sessions/{sessionId}/messages`

Query: `?limit=50&beforeTurn=...` (선택)

Response `200`
```json
{
  "messages": [
    { "messageId": "...", "role": "AI", "personaCode": "ACHIEVEMENT", "content": "...", "options": [], "inputType": null, "turnNo": 1, "createdAt": "..." }
  ]
}
```

### 4.8 잠정 결정 요약 생성 `POST /sessions/{sessionId}/decision/draft` (S05, F04)

"이 정도면 충분해요" 클릭 시 Decision Extractor 호출. 사용자 발언 근거 없는 값은 `unknown`.

Request: body 없음

Response `200`
```json
{
  "state": "PROVISIONAL_DECISION",
  "decisionId": "...",
  "revisionNo": 1,
  "summaryText": "현재는 대학원 진학에 조금 더 마음이 기울어 있고, 가장 중요한 이유는 연구 역량을 쌓고 싶기 때문으로 이해했어요.",
  "fields": {
    "decisionText": "대학원 진학",
    "coreValue": "연구 전문성",
    "reason": "장기적으로 연구개발 분야에서 일하고 싶음",
    "acceptableCost": "취업 시점의 지연",
    "uncheckedCondition": "실제 대학원 연구 경험"
  },
  "criteria": [
    { "criterionId": "...", "category": "VALUE", "content": "연구 전문성", "source": "USER_STATED" }
  ]
}
```

### 4.9 잠정 결정 처리 `POST /sessions/{sessionId}/decision/{decisionId}/resolve` (S05)

Request
```json
{ "action": "APPROVE" }
```

| action | 버튼 | Response |
|---|---|---|
| APPROVE (reconsiderCount = 0) | 맞아요, 이 선택으로 시험할래요 | `state: DILEMMA` + `dilemma` 객체 (아래 예시) |
| APPROVE (reconsiderCount ≥ 1) | 맞아요, 이 선택으로 시험할래요 | `state: REPORT`, `reportId` (딜레마를 다시 만들지 않고 바로 REPORT 생성) |
| REVISE | 다시 수정할래요 | `state: EXPLORING` (대화 이력 유지, 해당 결정 요약만 폐기) |
| UNDECIDED | 아직 결정하지 못했어요 | `state: REPORT`, `reportId` (딜레마 생성 안 함) |

재검토 후 APPROVE(reconsiderCount ≥ 1)의 리포트는 유지 이유 입력(S07)을 거치지 않으며, `initialView` = 첫 잠정 결정(revision 1), `revisedView` = 재승인된 잠정 결정, `shakingSituation` = 기존 딜레마로 채운다. 재검토 후 `UNDECIDED`는 UNDECIDED_REPORT.

APPROVE(reconsiderCount = 0) 시 딜레마 생성(LLM)이 걸리므로 응답이 느릴 수 있음 → 프론트 로딩 화면(C1). 생성 실패/품질 조건 미충족 시 서버가 템플릿으로 폴백 (`isFallback: true`).

Response 예 (APPROVE) `200`
```json
{
  "state": "DILEMMA",
  "dilemma": {
    "dilemmaId": "...",
    "dilemmaType": "ALT_PATH",
    "scenarioText": "[가상 상황] 희망하던 기업에서 전문 연구를 수행할 수 있는 연구개발 직무를 제안했습니다. 학위 없이도 원하는 역량을 쌓을 수 있다면, 지금의 판단은 달라질까요?",
    "verificationGoal": "전문성을 얻는 방법이 대학원이어야 하는가",
    "isFallback": false
  }
}
```

### 4.10 딜레마 응답 `POST /sessions/{sessionId}/dilemma/respond` (S06, F05~F06)

Request
```json
{ "choice": "KEEP" }
```

| choice | 버튼 | Response |
|---|---|---|
| KEEP | 선택을 유지할래요 | `state: REFLECTION`, `prompt`: "새로운 조건이 생겼는데도 같은 선택을 유지하는 이유가 무엇인가요?" + `options` |
| RECONSIDER | 마음이 흔들렸어요 | `state: RECONSIDER`, `prompt`: 어떤 가치가 흔들렸는지 질문 + `suggestedPersonaCode` (예: STABILITY) |
| UNDECIDED | 여전히 결정하지 못했어요 | `state: REPORT`, `reportId` (UNDECIDED_REPORT) |

딜레마는 세션당 1개라 RECONSIDER는 자연히 1회만 가능하다. `reconsiderCount >= 1`인 세션에서 다시 요청하면 `409 RECONSIDER_LIMIT` (방어용).

### 4.11 후속 입력 `PUT /sessions/{sessionId}/dilemma/followup` (S07, S08)

Request (S07, 상태 REFLECTION)
```json
{ "keepReason": "체계적으로 연구 방법을 배우는 과정이 더 중요하다고 생각해요." }
```
Response `200`: `{ "state": "REPORT", "reportId": "..." }`

Request (S08, 상태 RECONSIDER)
```json
{ "shakenValue": "좋은 기업에서 일할 기회를 포기하는 게 아까워졌어요." }
```
Response `200`: `{ "state": "EXPLORING", "reconsiderCount": 1, "suggestedPersonaCode": "STABILITY" }`
→ 이후 프론트는 4.5(`reason: RECONSIDER`)로 자아를 선택/전환한다.

### 4.12 그만 대화하기 `POST /sessions/{sessionId}/end` (O2)

Request
```json
{ "mode": "INCOMPLETE_REPORT" }
```
`mode`: `INCOMPLETE_REPORT`(미완료 리포트) / `SAVE`(P1, 저장 후 종료)

Response `200`
- INCOMPLETE_REPORT: `{ "state": "REPORT", "reportId": "..." }`
- SAVE: `{ "state": "PAUSED", "resumeCode": "ABCD-1234" }`

### 4.13 리포트 조회 `GET /sessions/{sessionId}/report` (S09, F07)

리포트 유형별로 값이 다르며, 해당 없는 항목은 `null`, 사용자가 밝히지 않은 항목은 `"unknown"`.

Response `200` (REPORT)
```json
{
  "reportId": "...",
  "reportType": "REPORT",
  "entryPoint": "S07",
  "concernSummary": "취업과 대학원 진학 사이의 고민",
  "initialView": "대학원에 가고 싶다",
  "revisedView": "대학원 진학을 유지하되, 연구실 인턴으로 먼저 적합성을 확인한다",
  "guidingValues": "연구 전문성",
  "shakingSituation": "희망 기업의 연구개발 직무 제안",
  "clarifiedCriteria": "학위보다 체계적인 연구 방법 습득이 중요함",
  "undecidedReason": null,
  "missingInfo": "실제 연구실 생활 적합성",
  "nextActions": [ { "seq": 1, "content": "연구실 인턴 지원하기" } ],
  "disclaimer": "이 리포트는 선택의 정답을 평가하지 않습니다.",
  "generatedAt": "2026-11-02T21:30:00+09:00"
}
```

유형별 필드 규칙

| 필드 | REPORT | UNDECIDED_REPORT | INCOMPLETE_REPORT |
|---|---|---|---|
| concernSummary | O | O (현재 비교하는 선택) | O |
| initialView / revisedView | O | null | null |
| guidingValues | O | O (가장 중요한 판단 기준) | O (현재까지 확인된 기준) |
| shakingSituation | O | S06 진입 시만, S05 진입이면 null | null |
| clarifiedCriteria | O | null | null |
| undecidedReason | null | O | null |
| missingInfo | O | O | O (확인하지 못한 항목) |
| nextActions | O | O | 선택 |

### 4.14 이어하기 `POST /sessions/resume` (P1, F08)

`resumeCode` 형식은 `XXXX-9999`(대문자 4자 + 숫자 4자)이며 세션 만료 전까지 유효하다.

Request: `{ "resumeCode": "ABCD-1234" }`

Response `200`: `{ "sessionId": "...", "sessionToken": "...", "state": "PAUSED" }` (이어가면 PAUSED 이전 상태로 복원)

## 5. AI 모듈 ↔ API 매핑

| 모듈 | 호출되는 API | 입력 | 출력 |
|---|---|---|---|
| Conversation Engine | 4.5 persona, 4.6 messages | 고민, 대화 이력, personaCode | 질문 1개, 선택지 2~3개 |
| Decision Extractor | 4.8 decision/draft | 대화 이력 | decision fields, criteria (unknown 허용) |
| Dilemma Generator | 4.9 resolve(APPROVE, reconsiderCount = 0) | 승인된 결정, 핵심 기준 | scenarioText, dilemmaType, verificationGoal |
| Report Generator | 4.9(UNDECIDED, 재검토 후 APPROVE), 4.10(UNDECIDED), 4.11, 4.12 | reportType, 전체 기록 | 유형별 리포트 항목 |

## 6. 결정 사항

| 항목 | 결정 |
|---|---|
| 재검토 후 딜레마 | 딜레마는 세션당 1개. 재검토 후 새 잠정 결정을 APPROVE하면 딜레마 재생성 없이 바로 REPORT |
| 재검토 한도 | 딜레마가 1개뿐이라 RECONSIDER는 1회. `RECONSIDER_LIMIT`은 방어용 |
| `concernType` | `CAREER` / `MAJOR` / `LEAVE` / `OTHER`, `freeText`는 1~1000자 |
| LLM 응답 방식 | 동기, 타임아웃 30초, 재시도 최대 2회. SSE는 MVP 이후 |
| 세션 만료 | 마지막 활동 후 7일 |
| `resumeCode` | `XXXX-9999` 형식 |
| 새 탭/동시 접속 | 같은 토큰이면 같은 세션 허용, 동시 요청은 상태 검증으로 409 |