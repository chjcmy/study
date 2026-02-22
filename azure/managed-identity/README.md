# Azure Managed Identity (관리 ID)

Managed Identity는 Azure 리소스가 자격 증명(비밀번호, 시크릿)을 직접 관리하지 않고도 Azure AD를 통해 다른 서비스에 인증할 수 있게 해주는 기능입니다.

## 🌟 핵심 이점
1. **No Secrets**: 코드나 설정 파일에 패스워드를 적을 필요가 없어 보안이 매우 강력합니다.
2. **Auto Rotation**: Azure가 내부적으로 자격 증명을 자동 갱신하므로 관리 부담이 없습니다.
3. **MFA 우회**: 사용자 계정이 아닌 서비스 계정 기반이므로, 개발/운영 시 번거로운 휴대폰 인증(MFA) 팝업을 피할 수 있습니다.
4. **시각 오차 해결**: `DefaultAzureCredential`을 사용하면 로컬 PC와 서버 간의 미세한 시간 차이로 발생하는 인증 오류(`AADSTS500133`)를 방지할 수 있습니다.

## 🚀 Managed Identity 자동 지원 서비스 목록

아래 서비스들은 SDK의 `DefaultAzureCredential` 클래스를 사용하면 자동으로 관리 ID를 인식하여 인증에 활용합니다.

| 분류 | 서비스 명 | 학습 포인트 |
| :--- | :--- | :--- |
| **대표 서비스** | **Azure Container Apps (ACE)** | 서버리스 컨테이너 환경에서 관리 ID를 통한 API 인증 지원 |
| | **Azure App Service** | 웹 앱에서 시크릿 없이 SQL/Cosmos DB 접근 시 사용 |
| | **Azure Functions** | 이벤트 트리거 시 외부 리소스 인증에 필수적 |
| **데이터/저장** | **Azure Cosmos DB** | RBAC(역할 기반 제어)를 통해 관리 ID에 직접 권한 부여 가능 |
| | **Azure Storage (Blob)** | 시크릿 키 대신 관리 ID로 데이터 읽기/쓰기 권한 제어 |
| **보안/관리** | **Azure Key Vault** | 다른 기밀 정보를 가져올 때 관리 ID를 '열쇠'로 사용 |

## 🛠️ 실무 적용 예시 (Python)

```python
from azure.identity.aio import DefaultAzureCredential
from azure.mgmt.subscription import SubscriptionClient

# DefaultAzureCredential은 아래 순서로 자동 시도합니다:
# 1. Managed Identity (Azure 서버 환경)
# 2. Azure CLI 자격 증명 (로컬 개발 환경: az login)
credential = DefaultAzureCredential()

# 시크릿 없이 바로 구독 리스트 조회 가능
client = SubscriptionClient(credential)
```

---
> [!NOTE]
> 로컬 개발 시에는 터미널에서 `az login`을 한 상태라면, `DefaultAzureCredential`이 이를 알아채고 관리 ID처럼 작동합니다.
