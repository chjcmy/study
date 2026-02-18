# Managed Identity 완전 정리

Azure 서비스가 **비밀번호 없이** 다른 Azure 리소스에 접근할 수 있게 해주는 인증 메커니즘입니다.

---

## 왜 Managed Identity가 필요한가?

### ❌ 전통적 방식 (위험)

```
앱 → 연결 문자열/비밀번호를 코드에 저장 → DB/Key Vault 접근
```

**문제점:**
- 비밀번호가 코드/설정에 노출
- 비밀번호 회전(교체) 시 앱 재배포 필요
- 유출 시 보안 사고

### ✅ Managed Identity 방식 (안전)

```
앱 → Azure AD에 자동 인증 → 토큰 발급 → DB/Key Vault 접근
```

**장점:**
- 코드에 비밀번호 없음
- Azure AD가 자동으로 토큰 관리
- 토큰 자동 갱신 (개발자 신경 X)

---

## 두 가지 종류

### 1. System Assigned (시스템 할당)

| 항목 | 설명 |
|------|------|
| 생성 방식 | 리소스와 함께 자동 생성 |
| 수명 | 리소스 삭제 시 함께 삭제 |
| 공유 | 해당 리소스만 사용 가능 |
| 사용 사례 | 단일 리소스가 다른 서비스에 접근할 때 |

```bicep
resource app 'Microsoft.Web/sites@2022-09-01' = {
  name: 'my-app'
  location: location
  identity: {
    type: 'SystemAssigned'  // 자동 생성
  }
}

// Principal ID 접근
output principalId string = app.identity.principalId
```

### 2. User Assigned (사용자 할당) ⭐

| 항목 | 설명 |
|------|------|
| 생성 방식 | 독립적으로 별도 생성 |
| 수명 | 리소스와 독립적, 직접 삭제해야 함 |
| 공유 | 여러 리소스가 공유 가능 |
| 사용 사례 | 여러 리소스가 같은 권한으로 접근할 때 |

```bicep
// 1. Identity 생성
resource identity 'Microsoft.ManagedIdentity/userAssignedIdentities@2023-01-31' = {
  name: 'mi-myapp'
  location: location
}

// 2. 앱에 할당
resource app 'Microsoft.Web/sites@2022-09-01' = {
  name: 'my-app'
  location: location
  identity: {
    type: 'UserAssigned'
    userAssignedIdentities: {
      '${identity.id}': {}  // ID를 키로 전달
    }
  }
}
```

### 비교

| 항목 | System Assigned | User Assigned |
|------|----------------|---------------|
| 생성 | 리소스와 함께 | 별도 생성 |
| 삭제 | 리소스와 함께 | 독립적 |
| 공유 | 불가능 | 여러 리소스 가능 |
| 관리 용이성 | ⭐ 간편 | ⭐⭐ 유연 |
| 권한 관리 | 리소스별 개별 설정 | 한 번에 여러 리소스 |
| 추천 시나리오 | 단순 1:1 매핑 | 복잡한 환경, 재사용 |

---

## 인증 흐름 (어떻게 동작하는가?)

```
┌─────────────────────┐
│  1. Container App    │
│  (코드에서 토큰 요청) │
└──────────┬──────────┘
           │ HTTP GET
           │ http://169.254.169.254/metadata/identity/oauth2/token
           ▼
┌─────────────────────┐
│  2. Azure AD         │
│  (토큰 발급)         │
└──────────┬──────────┘
           │ Access Token
           ▼
┌─────────────────────┐
│  3. Key Vault        │
│  (RBAC 권한 확인)    │
│  (비밀값 반환)       │
└─────────────────────┘
```

### 코드에서 사용 (Python)

```python
from azure.identity import DefaultAzureCredential
from azure.keyvault.secrets import SecretClient

# Managed Identity 자동 인식 (비밀번호 불필요!)
credential = DefaultAzureCredential()

# Key Vault 접근
client = SecretClient(
    vault_url="https://my-keyvault.vault.azure.net/",
    credential=credential
)

secret = client.get_secret("db-password")
print(secret.value)
```

### 코드에서 사용 (C#)

```csharp
using Azure.Identity;
using Azure.Security.KeyVault.Secrets;

var credential = new DefaultAzureCredential();
var client = new SecretClient(
    new Uri("https://my-keyvault.vault.azure.net/"),
    credential
);

KeyVaultSecret secret = await client.GetSecretAsync("db-password");
Console.WriteLine(secret.Value);
```

### 코드에서 사용 (Node.js)

```javascript
const { DefaultAzureCredential } = require("@azure/identity");
const { SecretClient } = require("@azure/keyvault-secrets");

const credential = new DefaultAzureCredential();
const client = new SecretClient(
  "https://my-keyvault.vault.azure.net/",
  credential
);

const secret = await client.getSecret("db-password");
console.log(secret.value);
```

---

## DefaultAzureCredential 인증 순서

`DefaultAzureCredential`은 아래 순서대로 인증을 시도합니다:

| 순서 | 방법 | 환경 |
|------|------|------|
| 1 | Environment Variables | CI/CD |
| 2 | Workload Identity | Kubernetes |
| 3 | Managed Identity | Azure VM, ACA, App Service |
| 4 | Azure CLI (`az login`) | 로컬 개발 |
| 5 | Azure PowerShell | 로컬 개발 |
| 6 | Azure Developer CLI | 로컬 개발 |
| 7 | Interactive Browser | 로컬 개발 (최후) |

> **핵심:** 코드 변경 없이 **로컬에서는 az login**, **Azure에서는 Managed Identity**로 자동 전환

---

## Managed Identity를 사용할 수 있는 Azure 서비스

| 서비스 | System | User |
|--------|--------|------|
| Virtual Machines | ✅ | ✅ |
| App Service / Functions | ✅ | ✅ |
| Container Apps | ✅ | ✅ |
| Container Instances | ✅ | ✅ |
| AKS (Kubernetes) | ✅ | ✅ |
| Logic Apps | ✅ | ✅ |
| Data Factory | ✅ | ✅ |
| API Management | ✅ | ✅ |
| Cognitive Services | ✅ | ✅ |

---

## Managed Identity로 접근 가능한 리소스

| 대상 리소스 | 필요 역할 |
|------------|-----------|
| Key Vault | Key Vault Secrets User |
| Storage Blob | Storage Blob Data Reader |
| Storage Queue | Storage Queue Data Reader |
| SQL Database | db_datareader 등 |
| Cosmos DB | Cosmos DB Account Reader |
| Service Bus | Service Bus Data Receiver |
| Event Hub | Event Hubs Data Receiver |
| Container Registry | AcrPull |

---

## CLI 명령어

```bash
# User Assigned Identity 생성
az identity create \
  --name mi-myapp \
  --resource-group my-rg

# 정보 확인
az identity show \
  --name mi-myapp \
  --resource-group my-rg

# App Service에 System Assigned 활성화
az webapp identity assign \
  --name my-app \
  --resource-group my-rg

# App Service에 User Assigned 할당
az webapp identity assign \
  --name my-app \
  --resource-group my-rg \
  --identities /subscriptions/{subId}/resourceGroups/my-rg/providers/Microsoft.ManagedIdentity/userAssignedIdentities/mi-myapp

# 목록 조회
az identity list --resource-group my-rg -o table
```

---

## Bicep 코드

```bicep
// User Assigned Managed Identity 생성
resource managedIdentity 'Microsoft.ManagedIdentity/userAssignedIdentities@2023-01-31' = {
  name: 'mi-${appName}'
  location: location
}

// 출력값 (다른 모듈에서 사용)
output managedIdentityId string = managedIdentity.id
output managedIdentityPrincipalId string = managedIdentity.properties.principalId
output managedIdentityClientId string = managedIdentity.properties.clientId
```

### 출력값 설명

| 출력 | 용도 |
|------|------|
| `id` | 리소스 ID → Container App에 Identity 할당 시 사용 |
| `principalId` | Azure AD 객체 ID → RBAC 역할 할당 시 사용 |
| `clientId` | 앱 ID → 코드에서 `AZURE_CLIENT_ID` 환경변수로 사용 |

---

## 보안 Best Practice

1. **User Assigned 선호** — 여러 리소스 공유, 라이프사이클 분리
2. **최소 권한 원칙** — 필요한 역할만 부여 (Reader/User, 절대 Contributor/Owner X)
3. **코드에 비밀번호 금지** — `DefaultAzureCredential` 사용
4. **네트워크 격리** — Private Endpoint와 함께 사용
5. **정기 감사** — 역할 할당 주기적 검토
