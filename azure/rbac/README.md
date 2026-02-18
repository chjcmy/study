# RBAC (Role-Based Access Control) 완전 정리

Azure 리소스에 대한 **접근 권한을 역할 기반으로 관리**하는 인증 시스템입니다.

---

## RBAC 핵심 개념

```
누가(Who) + 무엇을(What) + 어디에(Where)
   ↓            ↓             ↓
보안 주체    역할 정의      스코프
(Principal) (Role Def)   (Scope)
   ↓            ↓             ↓
   └────────────┴─────────────┘
              역할 할당
         (Role Assignment)
```

### 3가지 구성 요소

| 구성 요소 | 설명 | 예시 |
|-----------|------|------|
| **보안 주체 (Principal)** | 누가 접근하는가 | 사용자, 그룹, 서비스 주체, Managed Identity |
| **역할 정의 (Role Definition)** | 무엇을 할 수 있는가 | Reader, Contributor, Owner |
| **스코프 (Scope)** | 어디에서 적용되는가 | Management Group, Subscription, Resource Group, Resource |

---

## 보안 주체 (Principal) 종류

| 종류 | 설명 |
|------|------|
| User | Azure AD 사용자 (사람) |
| Group | Azure AD 그룹 (여러 사용자 묶음) |
| Service Principal | 앱/서비스 계정 |
| Managed Identity | Azure 관리형 서비스 계정 (비밀번호 없음) |

---

## 스코프 (Scope) 계층

```
Management Group (최상위)
  └─ Subscription (구독)
       └─ Resource Group (리소스 그룹)
            └─ Resource (개별 리소스)
```

**상위 스코프의 권한은 하위에 상속됩니다!**

| 스코프 | 적용 범위 | 예시 |
|--------|----------|------|
| Management Group | 모든 하위 구독 | 조직 전체 정책 |
| Subscription | 구독 내 모든 리소스 그룹/리소스 | 팀 단위 관리 |
| Resource Group | 그룹 내 모든 리소스 | 프로젝트 단위 |
| Resource | 해당 리소스만 | 최소 권한 원칙 적용 |

---

## 기본 제공 역할 (Built-in Roles)

### 일반 역할 (가장 많이 사용)

| 역할 | 권한 | 설명 |
|------|------|------|
| **Owner** | 읽기 + 쓰기 + 삭제 + **역할 할당** | 모든 권한 + RBAC 관리 |
| **Contributor** | 읽기 + 쓰기 + 삭제 | 모든 권한 (RBAC 관리 제외) |
| **Reader** | 읽기만 | 조회만 가능 |
| **User Access Administrator** | **역할 할당만** | RBAC 관리 전문 |

### Key Vault 역할

| 역할 | ID | 권한 |
|------|-----|------|
| Key Vault Administrator | `00482a5a-887f-4fb3-b363-3b7fe8e74483` | 모든 Key Vault 작업 |
| Key Vault Secrets Officer | `b86a8fe4-44ce-4948-aee5-eccb2c155cd7` | 비밀 CRUD |
| **Key Vault Secrets User** | `4633458b-17de-408a-b874-0445c86b69e6` | 비밀 읽기만 ⭐ |
| Key Vault Crypto Officer | `14b46e9e-c2b7-41b4-b07b-48a6ebf60603` | 키 CRUD |
| Key Vault Certificates Officer | `a4417e6f-fecd-4de8-b567-7b0420556985` | 인증서 CRUD |

### Storage 역할

| 역할 | 권한 |
|------|------|
| Storage Blob Data Owner | Blob 모든 권한 + ACL 관리 |
| Storage Blob Data Contributor | Blob 읽기/쓰기/삭제 |
| Storage Blob Data Reader | Blob 읽기만 |
| Storage Queue Data Contributor | Queue 읽기/쓰기/삭제 |
| Storage Table Data Contributor | Table 읽기/쓰기/삭제 |

### Container 역할

| 역할 | 권한 |
|------|------|
| AcrPull | 이미지 Pull만 |
| AcrPush | 이미지 Push + Pull |
| AcrDelete | 이미지 삭제 |

### 기타 자주 쓰는 역할

| 역할 | 권한 |
|------|------|
| Monitoring Reader | 모니터링 데이터 읽기 |
| Monitoring Contributor | 모니터링 설정 관리 |
| Log Analytics Reader | 로그 읽기 |
| Log Analytics Contributor | 로그 설정 관리 |
| Network Contributor | 네트워크 관리 |
| SQL DB Contributor | SQL Database 관리 |

---

## Bicep으로 역할 할당

### 기본 패턴

```bicep
// 역할 정의 ID 변수
var keyVaultSecretsUserRoleId = '4633458b-17de-408a-b874-0445c86b69e6'

// 기존 리소스 참조
resource keyVault 'Microsoft.KeyVault/vaults@2023-07-01' existing = {
  name: keyVaultName
}

// 역할 할당
resource roleAssignment 'Microsoft.Authorization/roleAssignments@2022-04-01' = {
  name: guid(keyVault.id, principalId, keyVaultSecretsUserRoleId)
  scope: keyVault  // ⚠️ 반드시 리소스 참조 (문자열 X)
  properties: {
    roleDefinitionId: subscriptionResourceId(
      'Microsoft.Authorization/roleDefinitions',
      keyVaultSecretsUserRoleId
    )
    principalId: principalId
    principalType: 'ServicePrincipal'  // Managed Identity = ServicePrincipal
  }
}
```

### ⚠️ 주의: name에 guid() 사용

```bicep
// ✅ 올바른 방법 - guid()로 고유 이름 생성
name: guid(keyVault.id, principalId, roleId)

// ❌ 잘못된 방법 - 직접 이름 지정
name: 'my-role-assignment'  // 중복 배포 시 충돌!
```

> `guid()` 함수는 **같은 입력이면 항상 같은 GUID 생성** → 멱등성(idempotent) 보장

### ⚠️ 주의: scope 타입

```bicep
// ✅ 올바른 방법 - 리소스 참조
scope: keyVault

// ❌ 잘못된 방법 - 문자열 전달
scope: keyVault.id        // 에러! string ≠ resource
scope: '/subscriptions...' // 에러! string ≠ resource
```

### principalType 종류

| 값 | 대상 |
|----|------|
| `User` | Azure AD 사용자 |
| `Group` | Azure AD 그룹 |
| `ServicePrincipal` | Service Principal, Managed Identity |
| `ForeignGroup` | 외부 테넌트 그룹 |

---

## 여러 역할을 한번에 할당

```bicep
// 여러 역할 정의
var roles = [
  {
    name: 'Key Vault Secrets User'
    id: '4633458b-17de-408a-b874-0445c86b69e6'
  }
  {
    name: 'Storage Blob Data Reader'
    id: '2a2b9908-6ea1-4ae2-8e65-a410df84e7d1'
  }
]

// for 루프로 한번에 할당
resource roleAssignments 'Microsoft.Authorization/roleAssignments@2022-04-01' = [for role in roles: {
  name: guid(resourceGroup().id, principalId, role.id)
  properties: {
    roleDefinitionId: subscriptionResourceId(
      'Microsoft.Authorization/roleDefinitions', role.id
    )
    principalId: principalId
    principalType: 'ServicePrincipal'
  }
}]
```

---

## CLI 명령어

### 역할 할당

```bash
# 역할 할당 생성
az role assignment create \
  --assignee <principalId or email> \
  --role "Key Vault Secrets User" \
  --scope /subscriptions/{subId}/resourceGroups/{rg}/providers/Microsoft.KeyVault/vaults/{kvName}

# 리소스 그룹 범위로 할당
az role assignment create \
  --assignee <principalId> \
  --role "Reader" \
  --resource-group my-rg

# 구독 범위로 할당
az role assignment create \
  --assignee <principalId> \
  --role "Contributor" \
  --scope /subscriptions/{subId}
```

### 역할 조회

```bash
# 특정 리소스 그룹의 역할 할당 조회
az role assignment list --resource-group my-rg -o table

# 특정 사용자의 역할 할당 조회
az role assignment list --assignee user@example.com -o table

# 모든 기본 제공 역할 조회
az role definition list --output table

# 특정 역할 검색
az role definition list --name "Key Vault" -o table
```

### 역할 삭제

```bash
az role assignment delete \
  --assignee <principalId> \
  --role "Reader" \
  --resource-group my-rg
```

---

## RBAC vs Access Policy (Key Vault)

Key Vault는 두 가지 인증 모델을 지원합니다:

| 항목 | RBAC (추천 ⭐) | Access Policy (레거시) |
|------|---------------|---------------------|
| 관리 위치 | Azure IAM | Key Vault 설정 |
| 세분화 | 역할 기반 세밀한 제어 | Vault 단위 |
| 스코프 | 리소스/그룹/구독 | Vault만 |
| 상속 | 상위 스코프 상속 | 불가 |
| 조건부 접근 | ✅ 지원 | ❌ 미지원 |
| 감사 | Azure Activity Log | 별도 설정 필요 |
| 활성화 | `enableRbacAuthorization: true` | `enableRbacAuthorization: false` |

---

## 커스텀 역할 (Custom Role)

기본 역할이 맞지 않을 때 직접 만들 수 있습니다.

```json
{
  "Name": "Key Vault Secret Reader Only",
  "Description": "Can only read secrets, nothing else",
  "Actions": [],
  "NotActions": [],
  "DataActions": [
    "Microsoft.KeyVault/vaults/secrets/getSecret/action"
  ],
  "NotDataActions": [],
  "AssignableScopes": [
    "/subscriptions/{subId}"
  ]
}
```

```bash
# 커스텀 역할 생성
az role definition create --role-definition custom-role.json

# 커스텀 역할 조회
az role definition list --custom-role-only true -o table
```

---

## Best Practice

1. **최소 권한 원칙** — 필요한 권한만 부여 (Owner/Contributor 남발 금지)
2. **그룹에 할당** — 개별 사용자 대신 Azure AD 그룹에 역할 할당
3. **리소스 수준 할당** — 구독/그룹보다 개별 리소스에 할당 추천
4. **RBAC 모드 사용** — Key Vault는 Access Policy 대신 RBAC 사용
5. **정기 감사** — `az role assignment list`로 불필요한 할당 확인/제거
6. **Deny Assignment 활용** — 특정 작업을 명시적으로 차단
