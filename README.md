# money-hunter

캐릭터를 키우고, 돈을 벌어보세요.

## 배포

- GCP Cloud Run 배포 가이드: [docs/deployment-gcp.md](docs/deployment-gcp.md)
- 심사용 서비스는 `review` 프로필, 운영 서비스는 `prod` 프로필로 실행합니다.
- 운영 프로필에서는 테스트 패널과 `/api/player/test/**` API가 비활성화됩니다.
