# Decision Lab Backend

서로 다른 가치관을 가진 AI 자아(성취·안정·탐색)와 대화하고, 가상 딜레마로 자신의 판단을 검증한 뒤, 판단 변화를 의사결정 리포트로 기록하는 서비스의 백엔드입니다.

- ERD: https://dbdiagram.io/d/decision_lab-6ae87bf2c7dad92865eae4c0
- API 명세서: `docs/api-spec.md`

## 기술 스택

| 항목 | 내용 |
|---|---|
| 언어 | Java 17 (Amazon Corretto 17) |
| 프레임워크 | Spring Boot 4.1.2 + Spring AI 2.0.x |
| 빌드 | Gradle (Groovy DSL, Wrapper 사용) |
| DB | MySQL 8.4 LTS |
| 스키마 관리 | Flyway (`src/main/resources/db/migration`) |
| LLM | Anthropic Claude (`claude-sonnet-5-5`) |
| API 문서 | springdoc-openapi 3.0.x (Swagger UI) |
| 인증 | 익명 세션 토큰 (`X-Session-Token`) |

**MySQL 매핑 규칙**
- PK: `sessions.id`만 `UUID`(`CHAR(36)`)로 발급하고(토큰처럼 외부에 노출되어 추측·열거를 막기 위함), 나머지 테이블의 PK는 `BIGINT AUTO_INCREMENT`(Java `Long`)로 한다
- `json` 컬럼(`messages.options`) → MySQL `JSON` 타입
- enum 값은 `VARCHAR`로 저장하고 Java enum으로 매핑
- 시간은 `DATETIME(6)`, 서버 시간대는 `Asia/Seoul`

## 시작하기

### 준비물

- JDK 17 (Amazon Corretto), IntelliJ IDEA Ultimate, Git
- MySQL 8.4 LTS (Windows MSI Installer, Server only)
- Gradle은 설치하지 않고 프로젝트의 `gradlew`를 사용합니다.

### 프로젝트 생성 (B)

B가 start.spring.io에서 아래 설정으로 프로젝트를 생성해 푸시하고, 나머지는 clone합니다.

| 항목 | 값 |
|---|---|
| Project / Language | Gradle - Groovy / Java |
| Spring Boot | 4.1.2 (SNAPSHOT, RC가 붙지 않은 버전) |
| Group / Artifact | `com.decisionlab` / `decisionlab` |
| Package name | `com.decisionlab` |
| Packaging | Jar |
| Configuration | YAML |
| Java | 17 |

의존성: Spring Web, Spring Data JPA, MySQL Driver, Validation, Lombok, Flyway Migration, Anthropic Claude

### 로컬 DB (각자 자기 PC)

root로 접속해 아래를 한 번 실행합니다. (IntelliJ Database 탭 사용)

```sql
CREATE DATABASE decision_lab CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
CREATE USER 'dl_user'@'localhost' IDENTIFIED BY '<비밀번호>';
GRANT ALL PRIVILEGES ON decision_lab.* TO 'dl_user'@'localhost';
```

- DB 이름 `decision_lab`, 사용자 `dl_user`, 문자셋 `utf8mb4`만 맞추고 비밀번호는 각자 정합니다.
- 테이블과 시드 데이터는 서버 실행 시 Flyway(`src/main/resources/db/migration`)가 만듭니다. 이미 merge된 마이그레이션 파일은 수정하지 않고 새 버전 파일로 추가합니다.

### 실행

`application-local.yml`(커밋 금지)에 DB 접속 정보와 `spring.ai.anthropic.api-key: ${ANTHROPIC_API_KEY}`, 모델 `claude-sonnet-5-5`를 설정하고, 키와 비밀번호는 환경변수로 넣습니다.

```bash
./gradlew bootRun --args='--spring.profiles.active=local'
```

- `ddl-auto`는 `validate`로 두고, 스키마는 Flyway로만 변경합니다.
- Anthropic API 키는 개발자 각자 개발용 키를 발급해 쓰고, 배포 서버용 키는 A가 별도로 관리합니다.
- 모델: 개발 중에는 비용이 낮은 `claude-haiku-5-5`, 시연·배포에서는 `claude-sonnet-5-5`를 사용합니다. `application-local.yml`의 `spring.ai.anthropic.chat.options.model` 값만 바꿉니다.

### 배포 계획

- 개발 중에는 각자 로컬 MySQL을 사용합니다. 배포 DB는 필요하지 않습니다.
- 기반 단계가 끝나면 간단한 API 하나(`POST /sessions`)만 **EC2 + RDS for MySQL**에 올려 배포 과정을 한 번 검증합니다. RDS만 먼저 만들지 않고 백엔드와 함께 올립니다.
- 프론트 연동이 시작되면 `develop` 기준으로 재배포하고, 시연 전에 `main` 기준으로 최종 배포 후 전체 시나리오를 확인합니다.
- RDS는 EC2 보안 그룹에서 오는 3306만 허용하고, 사용하지 않는 기간에는 중지합니다. 종료 시 스냅샷 후 삭제합니다.
- DB 비밀번호와 LLM API 키는 서버(EC2) 환경변수로만 관리합니다.

## API 문서 (Swagger)

springdoc-openapi로 Swagger UI를 자동 생성합니다.

| 항목 | 주소 |
|---|---|
| Swagger UI | `http://localhost:8080/swagger-ui/index.html` |
| OpenAPI JSON | `http://localhost:8080/v3/api-docs` |

```groovy
// build.gradle (Spring Boot 4.x용 springdoc 3.x)
implementation 'org.springdoc:springdoc-openapi-starter-webmvc-ui:3.0.3'
```

- 공통 설정(`OpenApiConfig`)은 A가 만듭니다. Swagger UI의 **Authorize** 버튼에 `sessionToken`을 넣으면 `X-Session-Token` 헤더가 자동으로 붙습니다.
- 사용 순서: `POST /sessions` 실행 → 응답의 `sessionToken` 복사 → Authorize에 붙여넣기 → 나머지 API 테스트
- 운영 환경(`application-prod.yml`)에서는 `springdoc.api-docs.enabled`, `springdoc.swagger-ui.enabled`를 `false`로 끕니다.

### 작성 규칙

- 엔드포인트마다 `@Operation(summary = "#6 메시지 전송")`처럼 명세서 번호와 기능명을 쓴다.
- 에러 응답(400, 401, 403, 404, 409, 410, 502)은 `@ApiResponse`로 명시하고, DTO 필드에는 `@Schema(description, example)`를 붙인다.
- Swagger 코드와 API 명세서가 다르면 명세서를 먼저 고치고 코드를 맞춘다.
- 토큰 없이 호출하는 API(`POST /sessions`, `GET /personas`)와 Swagger 경로(`/swagger-ui/**`, `/v3/api-docs/**`)는 인증 필터에서 제외한다.

## 역할 분담

| 담당 | 책임 영역 | 테이블 | API (명세서 번호) | AI 모듈 |
|---|---|---|---|---|
| **A** | 세션·상태 전이·인증·공통 | sessions, concerns, personas, persona_switches | #1 #2 #3 #4 #5 #12 (P1: #14) | 없음 (B의 모듈을 호출) |
| **B** | 1:1 대화, 잠정 결정, LLM 공통 래퍼 | messages, judgment_criteria, provisional_decisions | #6 #7 #8 #9 | Conversation Engine, Decision Extractor |
| **C** | 딜레마, 사후 분기, 리포트 | dilemmas, dilemma_templates, dilemma_responses, reports, report_actions | #10 #11 #13 | Dilemma Generator, Report Generator |

### A: 세션 · 상태 전이 · 공통

| # | Method | Path | 화면 | 상태 전이 | 비고 |
|---|---|---|---|---|---|
| 1 | POST | `/sessions` | S01 | → CREATED | 토큰 발급 |
| 2 | GET | `/sessions/{id}` | 전 화면 | 변화 없음 | 새로고침 복원용, 다른 담당 데이터(결정/딜레마/리포트 id)를 읽어서 조합 |
| 3 | PUT | `/sessions/{id}/concern` | S02 | CREATED → CREATED | 덮어쓰기 가능 |
| 4 | GET | `/personas` | S03 | - | 시드 데이터 조회 |
| 5 | POST | `/sessions/{id}/persona` | S03, O1, S08 | CREATED → EXPLORING / EXPLORING → EXPLORING | B의 Conversation Engine 호출 (첫 질문 + handoff 요약) |
| 12 | POST | `/sessions/{id}/end` | O2 | EXPLORING → REPORT / PAUSED | INCOMPLETE는 C의 Report Generator 호출 |
| 14 | POST | `/sessions/resume` | S01, S10 | PAUSED → 이전 상태 | P1 |

공통 산출물 (B, C가 사용)
- `SessionState` enum, `ErrorCode` enum, `ApiException`, `GlobalExceptionHandler`
- 토큰 인증 필터: 토큰 검증, `{sessionId}` 소유권 확인(403), 만료 확인(410)
- `SessionStateService`: 허용 상태 검증 + 상태 변경
- personas 시드 데이터 (성취/안정/탐색/딜레마 관리자)
- ERD 전체 엔티티 + DDL: 12개 테이블 전부 한 번에 (FK 충돌 방지)
- Swagger 공통 설정 (`OpenApiConfig`, 세션 토큰 Authorize, 인증 필터 제외 경로)

### B: 1:1 대화 · 잠정 결정 · LLM 래퍼

| # | Method | Path | 화면 | 상태 전이 | 호출하는 AI 모듈 |
|---|---|---|---|---|---|
| 6 | POST | `/sessions/{id}/messages` | S04 | EXPLORING → EXPLORING | Conversation Engine |
| 7 | GET | `/sessions/{id}/messages` | S04 | - | - |
| 8 | POST | `/sessions/{id}/decision/draft` | S04→S05 | EXPLORING → PROVISIONAL_DECISION | Decision Extractor |
| 9 | POST | `/sessions/{id}/decision/{decisionId}/resolve` | S05 | APPROVE → DILEMMA / REVISE → EXPLORING / UNDECIDED → REPORT | APPROVE 시 C의 Dilemma Generator, UNDECIDED 시 C의 Report Generator |

공통 산출물 (A, C가 사용)
- `LlmClient` 래퍼: Spring AI `ChatClient` 감싸기, 타임아웃 30초, 재시도 최대 2회, JSON 스키마 파싱 실패 처리 → 실패 시 `LLM_FAILED`
- 프롬프트 파일 로딩 구조 (`resources/prompts/*.txt`)
- `ConversationEngine.open(...)` (A의 #5가 호출), `createHandoffSummary(...)`

AI 규칙
- Conversation Engine: 현재 자아 관점만 유지, 질문은 한 번에 1개, 선택 지시/미래 예언 금지
- Decision Extractor: 사용자가 직접 말한 값(`USER_STATED`)과 AI 추론(`AI_INFERRED`) 구분, 근거 없으면 `unknown`

### C: 딜레마 · 사후 분기 · 리포트

| # | Method | Path | 화면 | 상태 전이 | 호출하는 AI 모듈 |
|---|---|---|---|---|---|
| 10 | POST | `/sessions/{id}/dilemma/respond` | S06 | KEEP → REFLECTION / RECONSIDER → RECONSIDER / UNDECIDED → REPORT | UNDECIDED 시 Report Generator |
| 11 | PUT | `/sessions/{id}/dilemma/followup` | S07, S08 | REFLECTION → REPORT / RECONSIDER → EXPLORING | KEEP 시 Report Generator |
| 13 | GET | `/sessions/{id}/report` | S09 | - | - |

공통 산출물 (A, B가 사용)
- `DilemmaGenerator.generate(sessionId, decisionId)`: B의 #9(APPROVE)가 호출
- `ReportGenerator.generate(sessionId, reportType, entryPoint)`: A의 #12, B의 #9(UNDECIDED)가 호출
- `dilemma_templates` 시드 데이터 (4유형, 폴백용)

AI 규칙
- Dilemma Generator: 검증 기준 정확히 1개, 실제 미래 예측 금지, 정답/오답 평가 금지, 승인된 결정 없으면 호출하지 않음. 품질 조건 미충족 시 템플릿 폴백
- Report Generator: 입력 `reportType`(REPORT / UNDECIDED_REPORT / INCOMPLETE_REPORT)에 따라 구성 항목이 다름, 밝히지 않은 항목은 `unknown`

## 담당 간 인터페이스

`sessionId`는 `UUID`, 그 외 ID(`decisionId` 등 `sessions` 외 테이블 PK)는 `Long`입니다.

```java
// A 제공
interface SessionStateService {
    void requireState(UUID sessionId, SessionState... allowed); // 불가 시 INVALID_STATE_TRANSITION
    void transition(UUID sessionId, SessionState to);
}

// B 제공
interface ConversationEngine {
    AiMessage open(UUID sessionId, PersonaCode code);          // A의 #5가 호출
    String createHandoffSummary(UUID sessionId);               // 자아 전환 시 요약
}

// C 제공
interface DilemmaGenerator {
    DilemmaResult generate(UUID sessionId, Long decisionId);   // B의 #9가 호출
}
interface ReportGenerator {
    ReportResult generate(UUID sessionId, ReportType type, EntryPoint entry); // A, B, C가 호출
}
```

원칙
- 다른 담당의 테이블은 **읽기만** 한다. 쓰기는 담당자의 서비스를 통해서만 한다.
- 인터페이스와 DTO는 구현보다 먼저 PR로 올리고 3명이 리뷰한다.
- 구현 전에는 가짜 구현체(고정값)를 두고 연동한다.

### API 호출 흐름

| API | 담당 | 내부에서 호출하는 타 담당 기능 |
|---|---|---|
| #5 persona | A | B `ConversationEngine.open`, `createHandoffSummary` |
| #9 resolve (APPROVE) | B | C `DilemmaGenerator.generate` |
| #9 resolve (UNDECIDED) | B | C `ReportGenerator.generate(UNDECIDED_REPORT)` |
| #12 end (INCOMPLETE) | A | C `ReportGenerator.generate(INCOMPLETE_REPORT)` |
| 전체 | A, B, C | A `SessionStateService` (상태 검증/전이) |
| #6, #8 등 LLM 호출 | A 제외 | B `LlmClient` |
| #2 세션 조회 | A | B/C 테이블 읽기 (결정, 딜레마, 리포트 id) |

## 진행 순서

| 단계 | 기간(예시) | A | B | C |
|---|---|---|---|---|
| 0. 합의 | 1일 | 인터페이스/DTO/enum 합의 | ← | ← |
| 1. 기반 | 1~2일 | 엔티티 + DDL 전체, 공통 에러/인증/상태 서비스, personas 시드 | 프로젝트 생성·푸시(가장 먼저), `LlmClient` 래퍼, 프롬프트 로딩 | 인터페이스 + 가짜 구현체, 템플릿 시드 |
| 2. 기능 | 3~5일 | #1~#5 | #6~#9 | #10, #11, #13 |
| 3. 연동 | 마지막 | #12, 가짜 구현체를 실제 구현으로 교체 | ← | ← |
| 4. 통합 테스트 | 마지막 | 세션 생성 → 리포트까지 전체 플로우, 리포트 3유형 모두 | ← | ← |

> C의 병목: 딜레마 생성에 필요한 B의 결정/기준 데이터는 가짜 데이터를 DB에 직접 넣어서 먼저 개발한다.

## 패키지 구조

도메인(기능) 기준으로 폴더를 나누고, 도메인 안에서는 `controller / service / repository / entity / dto` 폴더로 층을 나눕니다. 담당별로 패키지가 분리되어 같은 파일을 동시에 고칠 일이 줄어듭니다.

```
com.decisionlab
├─ DecisionlabApplication.java
│
├─ global/                          # A: 공통
│   ├─ config/                      OpenApiConfig, WebConfig
│   ├─ error/                       ErrorCode, ApiException, GlobalExceptionHandler, ErrorResponse
│   ├─ auth/                        SessionTokenFilter, SessionContext
│   └─ entity/                      BaseTimeEntity
│
├─ session/                         # A   (각 도메인 폴더 안: controller/ service/ repository/ entity/ dto/)
├─ concern/                         # A
├─ persona/                         # A
│
├─ chat/                            # B
│   ├─ message/                     (controller/ service/ repository/ entity/ dto/)
│   ├─ criterion/                   judgment_criteria
│   └─ decision/                    provisional_decisions
│
├─ dilemma/                         # C   dilemma, dilemma_response, dilemma_template
├─ report/                          # C   report, report_action
│
└─ ai/                              # AI 연동 (모듈별 담당이 다름)
    ├─ llm/                         B: LlmClient, 재시도/타임아웃, 스키마 파싱
    ├─ prompt/                      B: 프롬프트 파일 로더
    ├─ conversation/                B: ConversationEngine
    ├─ decision/                    B: DecisionExtractor
    ├─ dilemma/                     C: DilemmaGenerator
    └─ report/                      C: ReportGenerator
```

```
src/main/resources
├─ application.yml
├─ application-local.yml            (깃에 올리지 않음)
├─ application-prod.yml
├─ db/migration/                    V1__init_schema.sql, V2__seed_...sql
└─ prompts/
    ├─ conversation/                common.txt, achievement.txt, stability.txt, exploration.txt
    ├─ decision-extractor.txt
    ├─ dilemma-generator.txt
    └─ report-generator.txt
```

테스트(`src/test/java`)도 같은 패키지 구조로 맞춥니다.

### 코드 규칙

- 클래스 하나당 파일 하나. Controller, Service, Repository, Entity는 도메인당(또는 테이블당) 1개씩 만든다.
- 이름은 `XxxController`, `XxxService`, `XxxRepository`로 통일하고, DTO는 `XxxRequest`, `XxxResponse`(record)로 파일을 따로 둔다.
- Service는 인터페이스와 `Impl`로 나누지 않고 클래스 하나로 쓴다. 아래만 인터페이스로 둔다.
    - 담당 간에 호출하는 것: `SessionStateService`, `ConversationEngine`, `DilemmaGenerator`, `ReportGenerator`
    - 가짜 구현체와 실제 구현체를 바꿔 끼워야 하는 것: `LlmClient`
- 다른 도메인의 Repository는 읽기만 직접 사용하고, 쓰기는 담당자의 Service를 통해서만 한다.
- 엔티티를 API 응답으로 바로 반환하지 않고 항상 DTO로 변환한다.
- 공통 enum은 소유한 도메인의 `entity/`에 둔다. (`SessionState`는 `session/entity`, `PersonaCode`는 `persona/entity`, 에러 코드는 `global/error`)

## 브랜치 · PR 규칙

### 브랜치 전략

| 브랜치 | 용도 | 규칙 |
|---|---|---|
| `main` | 시연/제출용, 항상 실행 가능한 상태 | 직접 push 금지, `develop`에서만 merge |
| `develop` | 통합 브랜치 | 직접 push 금지, PR로만 merge |
| `feature/{기능}` | 기능 개발 | `develop`에서 분기, 끝나면 삭제 |
| `fix/{내용}` | 버그 수정 | `develop`에서 분기 |

예: `feature/session-api`, `feature/llm-client`, `feature/report-generator`, `fix/message-null`

### 커밋 메시지

`타입: 내용` 형식

| 타입 | 용도 |
|---|---|
| feat | 기능 추가 |
| fix | 버그 수정 |
| refactor | 동작 변경 없는 구조 개선 |
| docs | 문서 수정 |
| test | 테스트 추가/수정 |
| chore | 설정, 의존성, 빌드 |

예: `feat: 세션 생성 API 구현`, `fix: 상태 전이 검증 누락 수정`

### PR 규칙

- PR 제목은 커밋 형식과 동일하게 쓴다. 예: `feat: 잠정 결정 요약 API (#8)`
- **하나의 기능 단위**로 작게 올린다. 하루 이상 걸리면 쪼갠다.
- 리뷰어 **최소 1명** 승인 후 merge한다. 본인이 직접 merge하지 않는다.
- merge 방식은 **Squash and merge**로 통일한다.
- 올리기 전에 로컬에서 빌드와 테스트가 통과해야 한다.
- 리뷰 요청 후 **하루 안에** 확인한다. 막히면 팀 채팅에 알린다.
- `develop`에 merge하기 전에 최신 `develop`을 rebase하거나 merge해서 충돌을 먼저 해결한다.

### 3명 전원 리뷰가 필요한 변경

- ERD / DDL / 엔티티 변경
- 담당 간 공통 인터페이스(`SessionStateService`, `ConversationEngine`, `DilemmaGenerator`, `ReportGenerator`) 시그니처와 DTO 변경
- 공통 enum, 에러 코드 변경
- API 명세서 변경 (명세 먼저 고치고 구현한다)

### 지켜야 할 것

- **API 키, DB 비밀번호는 커밋 금지.** 환경변수를 사용한다.
- 다른 담당의 테이블/코드는 읽기만 한다. 수정이 필요하면 담당자에게 요청하거나 PR 리뷰로 합의한다.
- 프롬프트 파일(`resources/prompts/*.txt`) 수정도 PR로 올리고, 변경 이유를 PR 설명에 쓴다.
- 가짜 구현체(Mock)를 실제 구현으로 교체할 때는 PR 설명에 적는다.

PR 템플릿은 `.github/pull_request_template.md`에 있다.

## 결정 사항

| 항목 | 결정 | 담당 |
|---|---|---|
| 재검토(RECONSIDER) 후 딜레마 | 딜레마는 **세션당 1개**. 재검토 후 대화로 돌아가 새 잠정 결정을 APPROVE하면 딜레마를 다시 만들지 않고 **바로 REPORT 생성**. 유지 이유 입력(S07)은 생략하고, `initialView`=첫 잠정 결정, `revisedView`=재승인 결정, `shakingSituation`=기존 딜레마로 채운다. 재검토 후 UNDECIDED는 UNDECIDED_REPORT. | B, C |
| 재검토 상태 전이 | `reconsiderCount >= 1`인 세션에서 `PROVISIONAL_DECISION --APPROVE--> REPORT` (API 명세 4.9, 상태 전이 표에 반영) | B |
| 재검토 한도 | RECONSIDER는 딜레마 응답(S06)에서만 가능하고 딜레마가 1회뿐이라 자연히 1회로 제한. `RECONSIDER_LIMIT`(409)은 방어용으로 유지 | C |
| `concernType` | `CAREER`(취업·대학원 등 진로), `MAJOR`(전공·학업), `LEAVE`(휴학·시기), `OTHER` | A |
| `freeText` 길이 | 1~1000자 | A |
| LLM 응답 방식 | 동기 응답. `LlmClient` 타임아웃 30초, 재시도 최대 2회, 실패 시 502 `LLM_FAILED` (딜레마는 템플릿 폴백). SSE는 MVP 이후 | B |
| 세션 만료 | 마지막 활동 후 7일 (`expiresAt` 갱신) | A |
| `resumeCode` (P1) | `XXXX-9999` 형식(대문자 4자 + 숫자 4자), 세션 만료 전까지 유효 | A |
| 새 탭/동시 접속 | 같은 토큰이면 같은 세션으로 접근 허용(프론트는 localStorage에 토큰 저장). 동시 요청은 상태 검증으로 막고 위반 시 409 | A |
| LLM | Anthropic Claude `claude-sonnet-5-5`, Spring AI Anthropic 스타터 | B |
| API 키 | 개발자 각자 개발용 키(환경변수), 배포 서버 키는 A가 별도 발급해 서버 환경변수로 관리 | A |
| 스키마 변경 절차 | Flyway 새 버전 파일 추가 + 3명 전원 리뷰 | A |