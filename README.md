# money-hunter

> 캐릭터 성장, 광고 보상, 이벤트, 결제, 관리자 운영 기능을 포함한 Spring Boot 기반 웹 게임 서비스입니다.

## 1. 프로젝트 소개

money-hunter는 사용자가 캐릭터를 성장시키며 골드와 보상을 획득하는 웹 게임 서비스입니다. 단순 정적 게임 화면을 넘어 사용자 상태 저장, 광고 보상 세션, 이벤트 보상, 인앱 결제, 관리자 운영 도구까지 백엔드 중심으로 설계했습니다.

Apps in Toss 배포 환경과 One Store 심사용 Android WebView 빌드를 함께 고려했으며, 운영 환경에서는 심사용 도구와 테스트 API가 비활성화되도록 프로필을 분리했습니다.

## 2. 주요 기능

- 플레이어 상태 조회 및 직업 선택
- 전투, 자동 사냥, 던전, 보스 레이드, 미니게임, 펀치킹 기능
- 광고 시청 완료 기반 보상 지급
- 스킬 강화, 펫/스킨 구매 및 장착
- 루키 이벤트, 일일 미션, VIP, 친구 초대 보상
- Toss 로그인, Toss 광고, Toss 결제, Toss 포인트 리워드 연동을 위한 클라이언트 구조
- 관리자 로그인, 플레이어 제어, 이상 징후 모니터링, 매출/광고 분석, 감사 로그 조회
- Flyway 기반 DB 마이그레이션과 PostgreSQL 저장소 구성

## 3. 기술 스택

| 분류 | 기술 |
| --- | --- |
| Language | Java 21, JavaScript, TypeScript |
| Backend | Spring Boot 4, Spring Web MVC, Spring Security, Spring Data JPA, Validation |
| Database | PostgreSQL, Flyway |
| Infra | Docker, Docker Compose, GCP Cloud Run, Cloud SQL |
| Test | JUnit 5, Spring Boot Test, Testcontainers |
| Frontend | HTML, CSS, JavaScript, Vite, Apps in Toss Web Framework |
| Mobile | Android WebView for One Store review build |

## 4. 프로젝트 구조

```text
.
├── src/main/java/com/money_hunter
│   ├── domain              # 플레이어, 보상, 결제, 이벤트 도메인
│   ├── application         # 게임/운영/외부 연동 비즈니스 로직
│   ├── infrastructure      # 설정, persistence, Toss API client
│   └── presentation        # Player/Admin/Auth REST API
├── src/main/resources
│   ├── db/migration        # Flyway migration
│   └── static              # 웹 게임 화면 및 관리자 화면
├── apps-in-toss            # Apps in Toss 빌드용 프론트 리소스
├── android-onestore        # One Store 심사용 Android WebView shell
├── infra/gcp               # GCP 초기 설정 스크립트
└── .github/workflows       # Cloud Run 배포 워크플로우
```

## 5. 실행 방법

### 사전 준비

- Java 21
- Docker
- PostgreSQL 로컬 실행용 Docker Compose

### 로컬 DB 실행

```bash
docker compose up -d
```

기본 로컬 DB 설정은 다음과 같습니다.

```text
url      jdbc:postgresql://localhost:15432/money_hunter_local
user     money_hunter
password money_hunter
```

### Spring Boot 실행

```bash
MONEY_HUNTER_ADMIN_USERNAME=admin \
MONEY_HUNTER_ADMIN_ALLOW_PLAIN_PASSWORD=true \
MONEY_HUNTER_ADMIN_PASSWORD='local-admin-password' \
./gradlew bootRun
```

접속 주소는 다음과 같습니다.

```text
앱        http://localhost:8080
관리자    http://localhost:8080/admin
```

관리자 계정 환경변수가 없으면 서버가 시작되지 않습니다.

### Apps in Toss 프론트 빌드

```bash
cd apps-in-toss
npm install
npm run dev
npm run build
```

### One Store Android 빌드

```bash
./gradlew -p android-onestore :app:assembleRelease
./gradlew -p android-onestore :app:bundleRelease
```

## 6. API 명세

대표 API는 다음과 같습니다.

| Method | URI | 설명 |
| --- | --- | --- |
| GET | `/api/player` | 플레이어 상태 조회 |
| POST | `/api/player/job` | 직업 선택 |
| POST | `/api/player/combat/hit` | 전투 보상 처리 |
| POST | `/api/player/ads/sessions` | 광고 보상 세션 시작 |
| POST | `/api/player/reward/claim-after-ad` | 광고 시청 후 보상 수령 |
| POST | `/api/player/dungeon/run` | 던전 진행 |
| POST | `/api/player/boss/raid` | 보스 레이드 진행 |
| POST | `/api/auth/toss/login` | Toss 로그인 |
| POST | `/api/admin/auth/login` | 관리자 로그인 |
| GET | `/api/admin/overview` | 관리자 대시보드 요약 |
| GET | `/api/admin/players` | 플레이어 목록 조회 |

운영 프로필에서는 `/api/player/test/**` 및 테스트 패널 관련 기능이 비활성화됩니다.

## 7. 테스트 및 배포

### 테스트

```bash
./gradlew test
```

일부 테스트는 Testcontainers 기반 PostgreSQL을 사용하므로 Docker 실행이 필요할 수 있습니다.

### 배포

`.github/workflows/deploy-gcp.yml`은 `main` 브랜치 push 또는 수동 실행 시 다음 흐름으로 동작합니다.

1. Gradle 테스트 실행
2. Docker 이미지 빌드
3. Artifact Registry push
4. Cloud Run review/prod 서비스 배포

주요 배포 환경은 GCP Cloud Run, Cloud SQL, Artifact Registry, Secret Manager를 기준으로 구성되어 있습니다.

## 8. 학습 포인트

- 단순 CRUD를 넘어 실제 서비스 운영에 필요한 관리자 기능을 설계했습니다.
- 광고 보상, 결제, 포인트 리워드처럼 중복 지급과 검증이 중요한 흐름을 별도 세션/주문/보상 도메인으로 분리했습니다.
- local, review, prod, onestore 프로필을 나누어 심사/운영 환경에서 기능 노출 범위를 다르게 가져갔습니다.
- Flyway 마이그레이션으로 데이터베이스 변경 이력을 관리했습니다.

## 9. 개선할 점

- API 문서를 Swagger/OpenAPI로 자동화하기
- 운영 알림과 장애 대응 로그를 더 체계화하기
- 관리자 기능에 대한 E2E 테스트 보강하기
- 외부 Toss 연동 실패 시 재시도/보상 정책을 더 명확히 문서화하기
