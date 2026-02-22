# Log Doctor 환경 변수(ENV) 가이드

이 문서는 Log Doctor 프로젝트의 로컬 개발 및 배포를 위해 필요한 핵심 환경 변수 설정값들을 정리합니다.

---

## 🖥️ Backend (.env)
백엔드 루트 디렉토리에 위치하며, 인증 방식 및 데이터베이스 연결에 필수적입니다.

| 변수명 | 현재 설정값 (Sample) | 설명 |
| :--- | :--- | :--- |
| **AUTH_METHOD** | `managed_identity` | `managed_identity`(권장) 또는 `secret` |
| **CLIENT_ID** | `6880f4e3-6c6f-4865-a16e-f2cd081a3f9d` | Microsoft Entra ID 앱 등록 ID |
| **TENANT_ID** | `ccdcba04-0a62-4e96-9964-dc1fc61279f8` | Azure Active Directory 테넌트 ID |
| **CLIENT_SECRET** | `bDq8Q~Mkt3FP...` | `AUTH_METHOD=secret`일 때 필요 |
| **APP_ID_URI** | `api://localhost:53000/...` | 토큰 검증 시 Audience로 사용 |
| **COSMOS_ENDPOINT** | `http://localhost:8081/` | Cosmos DB 엔드포인트 (로컬 에뮬레이터) |
| **COSMOS_DATABASE** | `log-doctor-db` | 사용할 데이터베이스 이름 |
| **COSMOS_KEY** | `C2y6yDjf5/R+...` | Cosmos DB 마스터 키 |

---

## 🎨 Frontend (env/.env.dev)
Teams Toolkit 및 프론트엔드 동작에 필요한 변수들입니다.

| 변수명 | 현재 설정값 (Sample) | 설명 |
| :--- | :--- | :--- |
| **TEAMS_APP_TENANT_ID** | `ccdcba04-0a62-4e96-9964-dc1fc61279f8` | 테넌트 고유 ID |
| **TEAMS_APP_ID** | `9e94de61-9b2e...` | Teams 앱 고유 ID |
| **TAB_ENDPOINT** | `https://tabf1d67b...` | 배포된 탭 서비스의 URL |
| **AZURE_SUBSCRIPTION_ID** | `b5a82513-0077...` | 배포 대상 Azure 구독 ID |

---

## 💡 주요 운영 팁
1. **Managed Identity 활용**: 
   - 로컬에서 `AUTH_METHOD=managed_identity` 설정 후 `az login`을 수행하면, 별도의 시크릿 관리 없이 안전하게 인증할 수 있습니다.
2. **Cosmos DB Emulator**: 
   - 로컬 개발 시 `AZURE_COSMOS_DISABLE_SSL=True` 설정을 통해 에뮬레이터와의 연결 문제를 예방할 수 있습니다.
3. **SSO 도메인**: 
   - `TAB_DOMAIN`이 `manifest.json`의 `validDomains` 및 `webApplicationInfo`와 일치해야 정상적인 SSO가 작동합니다.
