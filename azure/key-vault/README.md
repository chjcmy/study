# Key Vault 완전 정리

비밀(Secret), 키(Key), 인증서(Certificate)를 **중앙에서 안전하게 관리**하는 Azure 서비스입니다.

---

## Key Vault가 관리하는 3가지

| 종류 | 설명 | 용도 |
|------|------|------|
| **Secrets** | 문자열 값 | DB 비밀번호, API 키, 연결 문자열 |
| **Keys** | 암호화 키 | 데이터 암호화/복호화, 서명/검증 |
| **Certificates** | TLS/SSL 인증서 | HTTPS, 코드 서명 |

---

## 핵심 기능

### 1. 비밀 버전 관리

```
kv-myapp/secrets/db-password
  ├── v1: "pass123"      (2024-01-01, 비활성)
  ├── v2: "s3cure!@#"    (2024-06-01, 비활성)
  └── v3: "N3wP@ss2025"  (2025-01-01, ✅ 현재 활성)
```

- 비밀을 업데이트하면 **새 버전 자동 생성**
- 이전 버전도 보존, 필요시 특정 버전 접근 가능
- 기본적으로 최신 버전 반환

### 2. Soft Delete & Purge Protection

| 기능 | 설명 |
|------|------|
| Soft Delete | 삭제해도 보존 기간 동안 복구 가능 (7~90일) |
| Purge Protection | Soft Delete 기간 내 영구삭제 불가 (추가 보호) |

```bicep
properties: {
  enableSoftDelete: true
  softDeleteRetentionInDays: 90   // 프로덕션 권장
  enablePurgeProtection: true     // 프로덕션 필수!
}
```

### 3. 네트워크 격리

```bicep
properties: {
  networkAcls: {
    defaultAction: 'Deny'         // 기본 차단
    bypass: 'AzureServices'       // Azure 서비스는 허용
    ipRules: [
      { value: '203.0.113.0/24' } // 특정 IP만 허용
    ]
    virtualNetworkRules: [
      { id: subnetId }            // 특정 서브넷만 허용
    ]
  }
}
```

---

## 인증 모델 비교

### RBAC (추천 ⭐)

```bicep
resource keyVault 'Microsoft.KeyVault/vaults@2023-07-01' = {
  properties: {
    enableRbacAuthorization: true  // ← RBAC 활성화
  }
}
```

- Azure IAM으로 통합 관리
- 리소스 수준 세밀한 권한 제어
- 조건부 접근 정책 지원

### Access Policy (레거시)

```bicep
resource keyVault 'Microsoft.KeyVault/vaults@2023-07-01' = {
  properties: {
    enableRbacAuthorization: false
    accessPolicies: [
      {
        tenantId: subscription().tenantId
        objectId: principalId
        permissions: {
          secrets: ['get', 'list']
          keys: ['get', 'list']
          certificates: ['get', 'list']
        }
      }
    ]
  }
}
```

---

## Bicep 코드

### Key Vault + 비밀 생성

```bicep
@description('Azure 리전')
param location string = resourceGroup().location

resource keyVault 'Microsoft.KeyVault/vaults@2023-07-01' = {
  name: 'kv-${uniqueString(resourceGroup().id)}'
  location: location
  properties: {
    sku: {
      family: 'A'
      name: 'standard'
    }
    tenantId: subscription().tenantId
    enableRbacAuthorization: true
    enableSoftDelete: true
    softDeleteRetentionInDays: 7     // 실습용 최소값
  }
}

// 비밀 생성 — parent로 Key Vault 연결
resource secret 'Microsoft.KeyVault/vaults/secrets@2023-07-01' = {
  parent: keyVault
  name: 'db-password'
  properties: {
    value: 'MySecurePassword123!'
    contentType: 'text/plain'        // 선택사항
    attributes: {
      enabled: true
      exp: 1735689600                // 만료 시간 (Unix timestamp)
    }
  }
}
```

### 비밀 속성

| 속성 | 설명 |
|------|------|
| `value` | 비밀 값 |
| `contentType` | MIME 타입 (설명용) |
| `attributes.enabled` | 활성화 여부 |
| `attributes.exp` | 만료 시간 (Unix timestamp) |
| `attributes.nbf` | 활성화 시작 시간 |

### SKU 비교

| SKU | 가격 | HSM 키 | 용도 |
|-----|------|--------|------|
| Standard | 저렴 | ❌ 소프트웨어 키 | 대부분의 시나리오 |
| Premium | 비쌈 | ✅ HSM 보호 키 | 규정 준수 필요 시 |

---

## 코드에서 사용

### Python

```python
from azure.identity import DefaultAzureCredential
from azure.keyvault.secrets import SecretClient

credential = DefaultAzureCredential()
client = SecretClient(
    vault_url="https://kv-myapp.vault.azure.net/",
    credential=credential
)

# 비밀 읽기
secret = client.get_secret("db-password")
print(f"값: {secret.value}")
print(f"버전: {secret.properties.version}")
print(f"생성일: {secret.properties.created_on}")

# 비밀 생성/업데이트
client.set_secret("new-secret", "my-value")

# 비밀 삭제
poller = client.begin_delete_secret("old-secret")
poller.result()  # 삭제 완료 대기

# 모든 비밀 목록
for secret_props in client.list_properties_of_secrets():
    print(f"  {secret_props.name}: enabled={secret_props.enabled}")

# 특정 버전 읽기
secret = client.get_secret("db-password", version="abc123")
```

### C#

```csharp
using Azure.Identity;
using Azure.Security.KeyVault.Secrets;

var client = new SecretClient(
    new Uri("https://kv-myapp.vault.azure.net/"),
    new DefaultAzureCredential()
);

// 비밀 읽기
KeyVaultSecret secret = await client.GetSecretAsync("db-password");
Console.WriteLine(secret.Value);

// 비밀 생성
await client.SetSecretAsync("api-key", "sk-1234567890");
```

### Container App 환경변수로 주입

```bicep
// 방법 1: Key Vault 참조 (권장)
env: [
  {
    name: 'DB_PASSWORD'
    secretRef: 'db-password-secret'
  }
]
secrets: [
  {
    name: 'db-password-secret'
    keyVaultUrl: '${keyVaultUri}secrets/db-password'
    identity: managedIdentityId
  }
]

// 방법 2: 코드에서 직접 읽기
env: [
  {
    name: 'KEY_VAULT_URL'
    value: keyVaultUri
  }
  {
    name: 'AZURE_CLIENT_ID'
    value: managedIdentityClientId
  }
]
```

---

## CLI 명령어

### Key Vault 관리

```bash
# Key Vault 생성
az keyvault create \
  --name kv-myapp \
  --resource-group my-rg \
  --location koreacentral \
  --enable-rbac-authorization true

# Key Vault 목록
az keyvault list -o table

# Key Vault 삭제
az keyvault delete --name kv-myapp --resource-group my-rg

# 삭제된 Key Vault 복구
az keyvault recover --name kv-myapp

# 삭제된 Key Vault 영구 삭제
az keyvault purge --name kv-myapp
```

### 비밀 관리

```bash
# 비밀 생성
az keyvault secret set \
  --vault-name kv-myapp \
  --name db-password \
  --value "MySecurePassword123!"

# 비밀 조회
az keyvault secret show \
  --vault-name kv-myapp \
  --name db-password

# 비밀 값만 조회
az keyvault secret show \
  --vault-name kv-myapp \
  --name db-password \
  --query value -o tsv

# 비밀 목록
az keyvault secret list --vault-name kv-myapp -o table

# 비밀 삭제
az keyvault secret delete --vault-name kv-myapp --name db-password

# 비밀 버전 목록
az keyvault secret list-versions --vault-name kv-myapp --name db-password -o table
```

---

## Key Vault 참조 패턴

### App Service 환경변수로 Key Vault 참조

```bicep
resource appSettings 'Microsoft.Web/sites/config@2022-09-01' = {
  name: 'appsettings'
  parent: app
  properties: {
    DB_PASSWORD: '@Microsoft.KeyVault(SecretUri=${keyVaultUri}secrets/db-password/)'
    API_KEY: '@Microsoft.KeyVault(SecretUri=${keyVaultUri}secrets/api-key/)'
  }
}
```

> `@Microsoft.KeyVault()` 구문으로 App Service가 자동으로 Key Vault에서 값을 읽어옴

---

## 비밀 회전 (Rotation)

### 자동 회전 설정

```bicep
resource rotationPolicy 'Microsoft.KeyVault/vaults/secrets@2023-07-01' = {
  properties: {
    attributes: {
      exp: dateTimeToEpoch(dateTimeAdd(utcNow(), 'P90D'))  // 90일 후 만료
    }
  }
}
```

### Event Grid로 만료 알림

```bash
# Key Vault 이벤트 구독 (비밀 만료 30일 전 알림)
az eventgrid event-subscription create \
  --name secret-expiry-alert \
  --source-resource-id /subscriptions/{subId}/resourceGroups/{rg}/providers/Microsoft.KeyVault/vaults/{kvName} \
  --endpoint https://my-webhook.example.com/api/alert \
  --included-event-types Microsoft.KeyVault.SecretNearExpiry
```

---

## Best Practice

1. **RBAC 사용** — Access Policy 대신 `enableRbacAuthorization: true`
2. **Soft Delete + Purge Protection** — 프로덕션에서 반드시 활성화
3. **네트워크 격리** — Private Endpoint 또는 서비스 엔드포인트 사용
4. **비밀 회전** — 90일마다 자동 교체 설정
5. **진단 로그** — Key Vault 작업 로그를 Log Analytics에 전송
6. **태그 관리** — 환경, 소유자, 프로젝트 태그 필수
7. **코드에 비밀 금지** — Key Vault + Managed Identity 조합 사용
