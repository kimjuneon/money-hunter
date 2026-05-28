# money-hunter

캐릭터를 키우고, 돈을 벌어보세요.

## 배포

- GCP Cloud Run 배포 가이드: [docs/deployment-gcp.md](docs/deployment-gcp.md)
- Apps in Toss 출시 계획: [docs/apps-in-toss-release-plan.md](docs/apps-in-toss-release-plan.md)
- 심사용 서비스는 `review` 프로필, 운영 서비스는 `prod` 프로필로 실행합니다.
- 운영 프로필에서는 테스트 패널과 `/api/player/test/**` API가 비활성화됩니다.
- 운영 프로필에서는 더미 광고, 더미 결제, 더미 리워드 지급 API도 비활성화됩니다.

## 로컬 실행

```bash
docker compose up -d

MONEY_HUNTER_ADMIN_USERNAME=admin \
MONEY_HUNTER_ADMIN_ALLOW_PLAIN_PASSWORD=true \
MONEY_HUNTER_ADMIN_PASSWORD='local-admin-password' \
./gradlew bootRun
```

- 앱: `http://localhost:8080`
- 관리자: `http://localhost:8080/admin`
- 로컬 DB 기본값: `jdbc:postgresql://localhost:15432/money_hunter_local`, `money_hunter` / `money_hunter`
- 관리자 계정 환경변수가 없으면 서버가 시작되지 않습니다.
