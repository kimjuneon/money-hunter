# Money Hunter GCP 배포 가이드

## 구조

- `money-hunter-review`: 심사용 Cloud Run 서비스입니다. `SPRING_PROFILES_ACTIVE=review`로 실행되며 테스트 패널과 `/api/player/test/**` API가 활성화됩니다.
- `money-hunter-prod`: 운영 Cloud Run 서비스입니다. `SPRING_PROFILES_ACTIVE=prod`로 실행되며 테스트 패널과 `/api/player/test/**` API가 비활성화됩니다.
- PostgreSQL은 Cloud SQL을 사용하고, 운영 DB와 심사용 DB를 분리합니다.
- GitHub Actions는 Workload Identity Federation으로 GCP에 접속합니다. 장기 서비스 계정 키를 GitHub에 저장하지 않습니다.

## 최초 1회 GCP 준비

1. GCP CLI와 GitHub CLI에 로그인합니다.

```bash
gcloud auth login
gcloud billing accounts list
gh auth status
```

2. GCP 프로젝트 ID와 결제 계정 ID를 정한 뒤 부트스트랩을 실행합니다.

```bash
PROJECT_ID=money-hunter-497411 \
BILLING_ACCOUNT_ID=000000-000000-000000 \
GITHUB_REPOSITORY=kimjuneon/money-hunter \
SET_GITHUB_VARIABLES=true \
./infra/gcp/bootstrap.sh
```

프로젝트를 스크립트로 새로 만들 때만 `CREATE_PROJECT=true`를 추가합니다. 결제 연결도 스크립트로 처리할 때만 `LINK_BILLING=true`를 추가합니다. 이미 콘솔에서 프로젝트 생성과 결제 연결을 끝냈다면 두 플래그는 넣지 않아도 됩니다.

3. GitHub Actions의 `Deploy to GCP Cloud Run` 워크플로우를 실행합니다.

- `main` 브랜치에 push하면 심사용 서비스만 자동 배포됩니다.
- 운영 배포는 Actions 수동 실행에서 `target=prod` 또는 `target=all`을 선택해야 합니다.

## 심사용 URL 확인

```bash
gcloud run services describe money-hunter-review \
  --project money-hunter-497411 \
  --region asia-northeast3 \
  --format='value(status.url)'
```

심사관에게는 위 URL을 전달하면 됩니다. 심사용 서비스에서는 오른쪽 테스트 도구가 노출되고, 운영 서비스에서는 노출되지 않습니다.

## 운영 테스트 API 차단 확인

```bash
curl -i -X POST https://PROD_URL/api/player/test/reset
curl https://PROD_URL/api/app/config
```

기대 결과:

- `/api/player/test/reset`: `404`
- `/api/app/config`: `{"reviewToolsEnabled":false,"guestUserEnabled":false,"environment":"prod"}`

## 비용 주의

Cloud Run, Cloud SQL, Artifact Registry, Cloud Build는 실제 GCP 비용이 발생할 수 있습니다. 심사 완료 후 사용하지 않는 심사용 서비스를 중지하거나 최소 인스턴스를 0으로 유지하세요.
