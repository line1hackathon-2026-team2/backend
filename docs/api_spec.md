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
| 소유권 | 토큰의 세션과 `{sessionId}`가 다르면 403. 단, `GET /reports/{reportId}`(4.15)는 누구나 조회할 수 있는 공개 엔드포인트라 이 규칙을 적용하지 않음 |
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
| decision field | `decisionText` `coreValue` `reason` `acceptableCost` `uncheckedCondition` |
| dilemma choice | `KEEP` `RECONSIDER` `UNDECIDED` |
| dilemmaType | `VALUE_CONFLICT` `OPPORTUNITY_COST` `ALT_PATH` `UNCERTAINTY` |
| reportType | `REPORT` `UNDECIDED_REPORT` `INCOMPLETE_REPORT` |
| inputType | `BUTTON` `FREE_TEXT` |
| fieldSource | `USER_STATED`(내가 말했어요) `AI_INFERRED`(AI가 추론했어요) `NEEDS_CHECK`(확인 필요) |

## 2. 상태 전이 표

| 호출 | 허용 현재 상태 | 결과 상태 |
|---|---|---|
| PUT concern | CREATED | CREATED |
| POST persona (최초) | CREATED | EXPLORING |
| POST persona (전환) | EXPLORING | EXPLORING |
| POST messages | EXPLORING | EXPLORING |
| POST decision/draft | EXPLORING | PROVISIONAL_DECISION |
| PATCH decision/{id} (필드 수정) | PROVISIONAL_DECISION | PROVISIONAL_DECISION |
| POST decision/{id}/resolve `APPROVE` (reconsiderCount = 0) | PROVISIONAL_DECISION | DILEMMA |
| POST decision/{id}/resolve `APPROVE` (reconsiderCount ≥ 1) | PROVISIONAL_DECISION | REPORT (REPORT, 딜레마 재생성 없음) |
| POST decision/{id}/resolve `REVISE` | PROVISIONAL_DECISION | EXPLORING |
| POST decision/{id}/resolve `UNDECIDED` | PROVISIONAL_DECISION | REPORT (UNDECIDED_REPORT, 딜레마 없음) |
| POST dilemma/respond `KEEP` | DILEMMA | REFLECTION |
| POST dilemma/respond `RECONSIDER` | DILEMMA | RECONSIDER |
| POST dilemma/respond `RECONSIDER` (마음 변경, reconsiderCount = 0) | REFLECTION | RECONSIDER |
| POST dilemma/respond `UNDECIDED` | DILEMMA | REPORT (UNDECIDED_REPORT) |
| PUT dilemma/followup (KEEP) | REFLECTION | REPORT (REPORT) |
| PUT dilemma/followup (RECONSIDER) | RECONSIDER | EXPLORING |
| POST end `INCOMPLETE_REPORT` | EXPLORING | REPORT (INCOMPLETE_REPORT) |
| POST end `SAVE` (P1) | EXPLORING | PAUSED |
| POST reopen | REPORT | EXPLORING |

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
| 9 | PATCH | `/sessions/{sessionId}/decision/{decisionId}` | S05 | 잠정 결정 필드 단위 수정 | P1 |
| 10 | POST | `/sessions/{sessionId}/decision/{decisionId}/resolve` | S05 | 승인/수정/미결정 | P0 |
| 11 | POST | `/sessions/{sessionId}/dilemma/respond` | S06, S07 | 유지/흔들림/미결정 선택 (S07에서 재흔들림 포함) | P0 |
| 12 | PUT | `/sessions/{sessionId}/dilemma/followup` | S07, S08 | 유지 이유 / 흔들린 가치 입력 | P0 |
| 13 | POST | `/sessions/{sessionId}/end` | O2 | 그만 대화하기 | P0 |
| 14 | GET | `/sessions/{sessionId}/report` | S09 | 리포트 조회 | P0 |
| 15 | GET | `/reports/{reportId}` | S09, S09-U, S09-I, S10 | 리포트 공개 조회(세션 무관) | P1 |
| 16 | POST | `/sessions/{sessionId}/reopen` | S09, S09-U, S09-I | 리포트에서 대화 재개 | P1 |
| 17 | POST | `/sessions/resume` | S01, S10 | 저장 세션 이어하기 | P1 |

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
  "concern": { "freeText": "취업/대학원 고민", "concernType": "CAREER", "options": ["취업", "대학원 진학"], "initialPreference": null, "initialPreferenceReason": null },
  "provisionalDecision": null,
  "dilemma": null,
  "reportId": null
}
```
`dilemma`는 상태에 따라 채워진다. `RECONSIDER` 상태일 때는 4.11이 내려줬던 `options`를 `shakenValueOptions`라는 이름으로 함께 포함해, 새로고침해도 S08 보기를 다시 생성하지 않고 복원할 수 있게 한다.

### 4.3 고민 입력 `PUT /sessions/{sessionId}/concern` (S02, F01)

수정 가능(같은 API로 덮어쓰기). CREATED 상태에서만 호출할 수 있고, 상태는 바뀌지 않는다. 선택지, 초기 선호, 이유는 비워도 진행 가능.

Request
```json
{
  "freeText": "취업과 대학원 진학 사이에서 고민 중이에요.",
  "concernType": "CAREER",
  "options": ["취업", "대학원 진학"],
  "initialPreference": "대학원 진학",
  "initialPreferenceReason": "전문성을 더 키우고 싶어서"
}
```
| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| freeText | string | O | 자유 입력, 1~1000자 |
| concernType | string | O | `CAREER`(취업·대학원 등 진로) / `MAJOR`(전공·학업) / `LEAVE`(휴학·시기) / `OTHER` |
| options | string[] | X | 비교 중인 선택지(S02 2/4). 2~3개, 각 1~15자, 공백 제거 후 중복 불가, 입력한 순서대로 저장. 안 보내거나 빈 배열이면 미입력으로 처리 |
| initialPreference | string | X | 초기 선호(S02 3/4). `options` 중 하나와 정확히 일치해야 함. "아직 모르겠어요"는 `null`. `options` 없이 이 값만 보내면 400 |
| initialPreferenceReason | string | X | 초기 선호를 고른 이유, 0~100자. `initialPreference`가 `null`이어도 보낼 수 있음 |

Response `200`: `{ "state": "CREATED", "concernId": "..." }`

| HTTP | code | 경우 |
|---|---|---|
| 400 | VALIDATION_ERROR | 필수값 누락, 글자 수 초과, `concernType` enum 불일치, 선택지 1개 또는 4개 이상, 선택지 중복, `initialPreference`가 `options`에 없음, `options` 없이 `initialPreference`만 보냄 |
| 409 | INVALID_STATE_TRANSITION | CREATED가 아닌 상태에서 호출 (`currentState` 포함) |

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

응답 대기 중 취소하는 API는 없다(C1-a). 호출이 동기 방식인 한 중도 취소는 지원하지 않으며, SSE 도입(MVP 이후) 시 재검토한다.

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
  "fieldSources": {
    "decisionText": "USER_STATED",
    "coreValue": "AI_INFERRED",
    "reason": "USER_STATED",
    "acceptableCost": "AI_INFERRED",
    "uncheckedCondition": "NEEDS_CHECK"
  },
  "criteria": [
    { "criterionId": "...", "category": "VALUE", "content": "연구 전문성", "source": "USER_STATED" }
  ]
}
```
`fieldSources`는 `fields`와 같은 키를 가지며 S05 화면에서 필드별 배지("내가 말했어요"/"AI가 추론했어요"/"확인 필요")를 그리는 데 쓰인다. `uncheckedCondition`은 정의상 항상 `NEEDS_CHECK`. 값이 `unknown`인 필드는 `fieldSource`가 `null`이며, 프론트는 이 경우 출처 배지 대신 '미입력' 상태로 표시한다.

### 4.9 잠정 결정 필드 수정 `PATCH /sessions/{sessionId}/decision/{decisionId}` (S05)

요약 카드의 개별 필드를 대화로 돌아가지 않고 바로 고치고 싶을 때 사용(연필 아이콘). `revisionNo`는 올리지 않고 같은 리비전의 값만 덮어쓴다. `summaryText`는 재생성하지 않는다.

Request
```json
{ "field": "coreValue", "value": "경제적 안정" }
```
| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| field | string | O | `decisionText` `coreValue` `reason` `acceptableCost` `uncheckedCondition` 중 하나 |
| value | string | O | 수정할 값, 1~200자 |

Response `200`
```json
{
  "decisionId": "...",
  "revisionNo": 1,
  "fields": {
    "decisionText": "대학원 진학",
    "coreValue": "경제적 안정",
    "reason": "장기적으로 연구개발 분야에서 일하고 싶음",
    "acceptableCost": "취업 시점의 지연",
    "uncheckedCondition": "실제 대학원 연구 경험"
  },
  "fieldSources": {
    "decisionText": "USER_STATED",
    "coreValue": "USER_STATED",
    "reason": "USER_STATED",
    "acceptableCost": "AI_INFERRED",
    "uncheckedCondition": "NEEDS_CHECK"
  }
}
```
사용자가 직접 수정한 필드의 `fieldSources`는 `USER_STATED`로 바뀐다(위 예시의 `coreValue`). 단 `uncheckedCondition`은 PATCH로 고쳐도 항상 `NEEDS_CHECK`를 유지한다 — 이 배지는 "누가 썼는가"가 아니라 "현실에서 아직 검증되지 않았다"는 상태를 나타내므로 편집 여부와 무관하다.

### 4.10 잠정 결정 처리 `POST /sessions/{sessionId}/decision/{decisionId}/resolve` (S05)

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
    "title": "학위 없이도 전문성을 쌓을 수 있다면?",
    "scenarioText": "[가상 상황] 희망하던 기업에서 전문 연구를 수행할 수 있는 연구개발 직무를 제안했습니다. 학위 없이도 원하는 역량을 쌓을 수 있다면, 지금의 판단은 달라질까요?",
    "verificationGoal": "전문성을 얻는 방법이 대학원이어야 하는가",
    "isFallback": false
  }
}
```
`title`은 Dilemma Generator가 `scenarioText`와 함께 생성하는 한 줄 제목이다. 템플릿 폴백(`isFallback: true`) 시에도 템플릿에 맞는 `title`을 함께 내려준다.

### 4.11 딜레마 응답 `POST /sessions/{sessionId}/dilemma/respond` (S06, F05~F06, S07)

Request
```json
{ "choice": "KEEP" }
```

| choice | 버튼 | 허용 상태 | Response |
|---|---|---|---|
| KEEP | 선택을 유지할래요 | DILEMMA | `state: REFLECTION`, `prompt`: "새로운 조건이 생겼는데도 같은 선택을 유지하는 이유가 무엇인가요?" + `options` |
| RECONSIDER | 마음이 흔들렸어요 | DILEMMA | `state: RECONSIDER`, `prompt`: 어떤 가치가 흔들렸는지 질문 + `options` + `suggestedPersonaCode` (예: STABILITY) |
| UNDECIDED | 여전히 결정하지 못했어요 | DILEMMA | `state: REPORT`, `reportId` (UNDECIDED_REPORT) |
| RECONSIDER | (S07) 생각이 바뀌었어요 | REFLECTION | `state: RECONSIDER`, DILEMMA에서 호출한 것과 동일한 응답 |

S07(유지 이유 작성 화면)에서 마음이 바뀌면 `keepReason`을 제출하기 전에 같은 엔드포인트를 `choice: RECONSIDER`로 다시 호출할 수 있다. 딜레마는 세션당 1개라 RECONSIDER는 자연히 1회만 가능하다. `reconsiderCount >= 1`인 세션(REFLECTION에서의 재호출 포함)에서 다시 요청하면 `409 RECONSIDER_LIMIT` (방어용). 이때 `options`는 새로 생성하지 않고 DILEMMA 상태에서 처음 생성됐던 값을 그대로 재사용한다(세션당 딜레마가 1개라 맥락이 바뀌지 않고, LLM 재호출도 불필요).

Response 예 (RECONSIDER) `200`
```json
{
  "state": "RECONSIDER",
  "prompt": "어떤 부분이 마음을 흔들었나요?",
  "options": [
    { "optionId": "o1", "label": "좋은 기업에서 일할 기회를 포기하는 게 아까워졌어요" },
    { "optionId": "o2", "label": "대학원이 아니어도 연구 역량을 쌓을 수 있을 것 같아요" }
  ],
  "suggestedPersonaCode": "STABILITY"
}
```
`options`는 Dilemma Generator가 해당 딜레마(`scenarioText`/`verificationGoal`)와 세션 맥락을 바탕으로 생성하는 2~3개의 보기(S08)이며, 최초 생성 이후로는 재생성하지 않고 세션에 고정해서 재사용한다(바로 위 문단). `label`은 1~50자(버튼 문구로 쓰이므로 자유 입력 `shakenValue`의 상한 500자보다 짧게 제한). 생성 실패 시에는 재시도 2회 후에도 실패하면(원래 `dilemma`의 `isFallback` 여부와 무관하게) 502 대신 `dilemmaType`별로 `dilemma_templates`에 미리 준비된 기본 보기 2개로 폴백한다 — 이 시점엔 사용자가 이미 RECONSIDER를 선택하고 다음 질문을 기다리는 중이라, 딜레마 생성과 마찬가지로 흐름을 끊지 않는 쪽을 택한다. S08은 버튼 선택과 자유 입력(4.12의 `shakenValue`)을 동등하게 처리하므로, 버튼을 고르면 그 `label`을 그대로 `shakenValue`로 보낸다.

RECONSIDER 상태에서 새로고침해도 보기를 다시 봐야 하므로, `GET /sessions/{sessionId}`(4.2)의 `dilemma` 객체가 `RECONSIDER` 상태일 때는 이 `options`를 `shakenValueOptions`라는 이름으로 함께 내려준다(저장된 값을 그대로 복원, 재생성 없음).

### 4.12 후속 입력 `PUT /sessions/{sessionId}/dilemma/followup` (S07, S08)

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

### 4.13 그만 대화하기 `POST /sessions/{sessionId}/end` (O2)

Request
```json
{ "mode": "INCOMPLETE_REPORT" }
```
`mode`: `INCOMPLETE_REPORT`(미완료 리포트) / `SAVE`(P1, 저장 후 종료)

Response `200`
- INCOMPLETE_REPORT: `{ "state": "REPORT", "reportId": "..." }`
- SAVE: `{ "state": "PAUSED", "resumeCode": "ABCD-1234" }`

### 4.14 리포트 조회 `GET /sessions/{sessionId}/report` (S09, F07)

리포트 유형별로 값이 다르며, 해당 없는 항목은 `null`, 사용자가 밝히지 않은 항목은 `"unknown"`. `reports.session_id`는 UNIQUE라 세션당 리포트는 하나뿐이다. 재개(4.16) 후 리포트를 다시 만들어도 같은 `reportId`를 유지한 채 내용만 갱신된다.

Response `200` (REPORT)
```json
{
  "reportId": "...",
  "reportType": "REPORT",
  "entryPoint": "S07",
  "concernSummary": "취업과 대학원 진학 사이의 고민",
  "initialView": "대학원에 가고 싶다",
  "initialViewReason": "전문성을 키우고 싶다",
  "revisedView": "대학원 진학을 유지하되, 연구실 인턴으로 먼저 적합성을 확인한다",
  "revisedViewReason": "연구실 인턴으로 적합성부터 확인",
  "guidingValues": "연구 전문성",
  "shakingSituation": "희망 기업의 연구개발 직무 제안",
  "shakingSituationDetail": "대학원 이외의 경로에서도 연구 역량을 쌓을 수 있다는 가능성을 고려하게 되었어요",
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
| initialViewReason / revisedViewReason | O | null | null |
| guidingValues | O | O (가장 중요한 판단 기준) | O (현재까지 확인된 기준) |
| shakingSituation | O | S06 진입 시만, S05 진입이면 null | null |
| shakingSituationDetail | O | S06 진입 시만, S05 진입이면 null | null |
| clarifiedCriteria | O | null | null |
| undecidedReason | null | O | null |
| missingInfo | O | O | O (확인하지 못한 항목) |
| nextActions | O | O | 선택 |

### 4.15 리포트 공개 조회 `GET /reports/{reportId}` (S09, S09-U, S09-I, S10)

"리포트 저장하기" 클릭 시 프론트가 안내하는 영구 링크. 새로 저장하는 API 호출은 없고, 4.14가 돌려준 `reportId`를 그대로 사용해 `X-Session-Token` 없이 조회한다. 존재하지 않는 `reportId`는 `404 NOT_FOUND`.

Response `200`: 4.14와 동일한 리포트 필드 구조.

`reports.session_id`는 UNIQUE라 세션당 리포트가 하나뿐이고, 재개(4.16) 후 리포트가 갱신되면 이 링크로 보이는 내용도 함께 바뀐다 — 저장 시점에 고정되는 스냅샷이 아니라 최신 상태를 가리키는 영구 링크다. 세션이 만료(7일)돼도 `reports` 행은 삭제되지 않으므로 계속 조회된다.

로그인이 없는 서비스 특성상 `reportId`를 아는 사람은 누구나 리포트를 볼 수 있다. 민감한 실명·개인정보를 넣지 말라는 기존 안내(S01)로 위험을 완화한다.

### 4.16 대화 재개 `POST /sessions/{sessionId}/reopen` (S09, S09-U, S09-I)

리포트 화면의 "더 이야기해 볼래요" / "이전 대화 이어가기"에서 호출. 대화 이력과 지금까지의 판단 기준(`criteria`)은 그대로 유지한 채 상태만 되돌린다.

Request: body 없음

Response `200`: `{ "state": "EXPLORING" }`

이후 흐름은 세션에 딜레마가 이미 있었는지에 따라 갈린다.

| 이전 리포트 | 재개 후 동작 |
|---|---|
| REPORT (딜레마 있음) | 이후 `decision/draft`로 새 리비전을 만들 수 있고, `resolve APPROVE`는 재검토 후와 동일하게(reconsiderCount ≥ 1 취급) 딜레마 재생성 없이 바로 REPORT로 간다 |
| UNDECIDED_REPORT / INCOMPLETE_REPORT (딜레마 없음) | 이후 `resolve APPROVE`는 처음과 동일하게 딜레마를 새로 생성한다 |

`reports.session_id`가 UNIQUE라 재개 후 다시 만든 리포트는 새 `reportId`를 생성하지 않고 기존 행(`reportId`, `reportType`, 본문 필드)을 덮어쓴다. 4.14와 4.15는 같은 `reportId`로 갱신된 최신 내용을 반환한다.

### 4.17 이어하기 `POST /sessions/resume` (P1, F08)

`resumeCode` 형식은 `XXXX-9999`(대문자 4자 + 숫자 4자)이며 세션 만료 전까지 유효하다.

Request: `{ "resumeCode": "ABCD-1234" }`

Response `200`: `{ "sessionId": "...", "sessionToken": "...", "state": "PAUSED" }` (이어가면 PAUSED 이전 상태로 복원)

## 5. AI 모듈 ↔ API 매핑

| 모듈 | 호출되는 API | 입력 | 출력 |
|---|---|---|---|
| Conversation Engine | 4.5 persona, 4.6 messages | 고민, 대화 이력, personaCode | 질문 1개, 선택지 2~3개 |
| Decision Extractor | 4.8 decision/draft | 대화 이력 | decision fields, fieldSources, criteria (unknown 허용) |
| Dilemma Generator | 4.10 resolve(APPROVE, reconsiderCount = 0) | 승인된 결정, 핵심 기준 | title, scenarioText, dilemmaType, verificationGoal |
| Dilemma Generator (S08 보기) | 4.11 respond(RECONSIDER) | 딜레마, 세션 맥락 | 흔들린 가치 질문에 대한 options(2~3개) |
| Report Generator | 4.10(UNDECIDED, 재검토 후 APPROVE), 4.11(UNDECIDED), 4.12, 4.13, 4.16(재개 후 재생성) | reportType, 전체 기록 | 유형별 리포트 항목 |

## 6. 결정 사항

| 항목 | 결정 |
|---|---|
| 재검토 후 딜레마 | 딜레마는 세션당 1개. 재검토 후 새 잠정 결정을 APPROVE하면 딜레마 재생성 없이 바로 REPORT |
| 재검토 한도 | 딜레마가 1개뿐이라 RECONSIDER는 1회. `RECONSIDER_LIMIT`은 방어용 |
| `concernType` | `CAREER` / `MAJOR` / `LEAVE` / `OTHER`, `freeText`는 1~1000자 |
| LLM 응답 방식 | 동기, 타임아웃 30초, 재시도 최대 2회. SSE는 MVP 이후, 응답 대기 취소(C1-a)도 SSE 이후로 미룬다 |
| 세션 만료 | 마지막 활동 후 7일 |
| `resumeCode` | `XXXX-9999` 형식 |
| 새 탭/동시 접속 | 같은 토큰이면 같은 세션 허용, 동시 요청은 상태 검증으로 409 |
| 잠정 결정 필드 수정 | S05에서 연필 아이콘으로 필드 단위 수정 가능(4.9, PATCH). `revisionNo`는 유지, `summaryText`는 재생성하지 않음 |
| 딜레마 제목 | `scenarioText`와 별도로 `title`을 Dilemma Generator가 함께 생성(4.10) |
| `reports.session_id` | UNIQUE(기존 V1 제약 유지). 세션당 리포트는 하나만 존재하고, 재개(4.16) 후 다시 생성해도 같은 `reportId`로 덮어쓴다 |
| 리포트 영구 저장 | 별도 저장 API나 스냅샷 테이블 없음. 4.14가 돌려준 `reportId`가 곧 영구 링크(4.15)이고, `X-Session-Token` 없이 조회 가능. 재개로 리포트가 갱신되면 이 링크의 내용도 함께 바뀜(스냅샷 고정 아님). 세션 만료와 무관하게 리포트 행은 유지됨. 비로그인 서비스라 `reportId`를 아는 사람은 누구나 조회 가능 |
| 리포트에서 대화 재개 | S09/S09-U/S09-I에서 `POST reopen`(4.16)으로 EXPLORING 복귀 가능. 기존 딜레마 유무에 따라 이후 흐름이 갈림 |
| 잠정 결정 필드별 출처(S05) | `fields`와 같은 키를 쓰는 `fieldSources` 객체 추가(4.8, 4.9). 값은 `USER_STATED`/`AI_INFERRED`/`NEEDS_CHECK`. 사용자가 PATCH로 직접 고치면 해당 필드는 `USER_STATED`로 갱신되지만, `uncheckedCondition`은 "누가 썼는가"가 아니라 "검증 여부" 플래그라 편집해도 항상 `NEEDS_CHECK` 유지. 값이 `unknown`인 필드는 `fieldSource`가 `null`이며 프론트가 '미입력'으로 표시 |
| 흔들린 가치 선택지(S08) | `dilemma/respond`의 RECONSIDER 응답(4.11)에 `options`(2~3개, label 1~50자) 추가. 버튼 선택 시 그 `label`을 그대로 `followup`의 `shakenValue`로 전송(자유 입력과 동등 처리). 최초 생성 후 재사용(S07 재호출 시 재생성 안 함), LLM 생성 실패 시 `isFallback` 여부와 무관하게 `dilemmaType`별 템플릿 기본 보기로 폴백(502 노출 안 함), `RECONSIDER` 상태에서 `GET /sessions/{id}`(4.2)가 `shakenValueOptions`로 복원 제공 |
| 리포트 단계별 설명(S09) | `initialView`/`revisedView`/`shakingSituation`은 제목 역할, 각각 회색 설명을 담는 `initialViewReason`/`revisedViewReason`/`shakingSituationDetail`을 추가(4.14). null 규칙은 짝이 되는 기존 필드와 동일 |
