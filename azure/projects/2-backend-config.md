# 2단계: 백엔드 구현 및 인증 연동

백엔드 코드에서 관리 ID를 인식하고, 상황에 맞는 자격 증명을 자동으로 선택하게 만드는 과정입니다.

## 1. 환경 변수 설정 (.env)
Managed Identity 모드로 전환하기 위해 필요한 설정입니다.

```env
# AUTH_METHOD: "managed_identity"로 설정
AUTH_METHOD=managed_identity

# 만든 관리 ID의 클라이언트 ID 입력
CLIENT_ID=408b6a5b-923f-4131-8c25-d869d38aa854

# (선택) 테넌트 ID가 명확할 경우 입력 권장
TENANT_ID=ccdcba04-0a62-4e96-9964-dc1fc61279f8
```

## 2. Python 코드 구현 (`azure-identity`)
`DefaultAzureCredential`은 로컬 개발 환경과 Azure 운영 환경을 모두 지원하는 가장 강력한 인증 방식입니다.

### 핵심 라이브러리 설치
```bash
pip install azure-identity
```

### 코드 예시 (`auth_provider.py`)
```python
from azure.identity.aio import DefaultAzureCredential
from .config import settings

async def get_service_token(self) -> str:
    """
    DefaultAzureCredential은 아래 우선순위로 인증을 시도합니다:
    1. 운영 환경: 에이전트 내 Managed Identity
    2. 개발 환경 (터미널): 'az login' 세션
    3. 개발 환경 (환경변수): AZURE_CLIENT_ID 등
    """
    # Managed Identity 사용 시 특정 Client ID를 명시
    credential = DefaultAzureCredential(
        managed_identity_client_id=settings.CLIENT_ID if settings.AUTH_METHOD == "managed_identity" else None
    )

    try:
        # Azure Resource Management API 스코프
        token_info = await credential.get_token("https://management.azure.com/.default")
        return token_info.token
    finally:
        await credential.close()
```

---
> [!IMPORTANT]
> 로컬에서 개발할 때는 터미널에서 반드시 **`az login`**을 수행해야 합니다. 그러면 `DefaultAzureCredential`이 별도의 시크릿 없이도 내 로컬 세션을 가져와 로그인을 시도합니다.
