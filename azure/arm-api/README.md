# Azure ARM API 완전 정리

**Azure Resource Manager** — Azure 리소스를 생성/조회/수정/삭제하는 **중앙 관리 계층**입니다.

---

## ARM이란?

```
사용자/앱의 모든 요청
    │
    ▼
Azure Resource Manager (ARM) ← 중앙 게이트웨이
    │
    ├──▶ Compute (VM, ACA, Functions...)
    ├──▶ Storage (Blob, Queue, Table...)
    ├──▶ Network (VNet, NSG, LB...)
    └──▶ Security (Key Vault, RBAC...)
```

**모든 Azure 관리 작업**은 ARM을 통해 이루어집니다:
- Azure Portal 클릭 → ARM API 호출
- Azure CLI 실행 → ARM API 호출
- Bicep/Terraform 배포 → ARM API 호출
- 코드에서 직접 → ARM REST API 호출

---

## ARM REST API 기본 구조

### Base URL

```
https://management.azure.com
```

### URL 패턴

```
https://management.azure.com
  /subscriptions/{subscriptionId}
  /resourceGroups/{resourceGroupName}
  /providers/{resourceProviderNamespace}/{resourceType}/{resourceName}
  ?api-version={apiVersion}
```

### 예시

| 작업 | HTTP 메서드 | URL |
|------|-----------|-----|
| 구독 목록 조회 | GET | `/subscriptions?api-version=2022-01-01` |
| 리소스 그룹 목록 | GET | `/subscriptions/{id}/resourcegroups?api-version=2021-04-01` |
| 리소스 그룹 생성 | PUT | `/subscriptions/{id}/resourcegroups/{name}?api-version=2021-04-01` |
| VM 목록 | GET | `.../providers/Microsoft.Compute/virtualMachines?api-version=2023-07-01` |
| Key Vault 조회 | GET | `.../providers/Microsoft.KeyVault/vaults/{name}?api-version=2023-07-01` |

---

## 구독 목록 조회 (Log-Doctor에서 사용 ⭐)

### 요청

```http
GET https://management.azure.com/subscriptions?api-version=2022-01-01
Authorization: Bearer {ARM-Access-Token}
```

### 응답

```json
{
  "value": [
    {
      "id": "/subscriptions/abc123-...",
      "subscriptionId": "abc123-def456-...",
      "displayName": "Production-Sub",
      "state": "Enabled",
      "tenantId": "tenant-id-...",
      "subscriptionPolicies": {
        "locationPlacementId": "Public_2014-09-01",
        "quotaId": "PayAsYouGo_2014-09-01",
        "spendingLimit": "Off"
      }
    },
    {
      "id": "/subscriptions/xyz789-...",
      "subscriptionId": "xyz789-...",
      "displayName": "Development-Sub",
      "state": "Enabled",
      "tenantId": "tenant-id-..."
    }
  ]
}
```

### 코드 (Python)

```python
import requests

def get_subscriptions(arm_token: str) -> list:
    """OBO로 교환한 ARM 토큰으로 구독 목록 조회"""
    
    response = requests.get(
        "https://management.azure.com/subscriptions",
        params={"api-version": "2022-01-01"},
        headers={
            "Authorization": f"Bearer {arm_token}",
            "Content-Type": "application/json"
        }
    )
    
    data = response.json()
    return [
        {
            "id": sub["subscriptionId"],
            "name": sub["displayName"],
            "state": sub["state"]
        }
        for sub in data.get("value", [])
    ]
```

### 코드 (Node.js)

```javascript
async function getSubscriptions(armToken) {
  const response = await fetch(
    "https://management.azure.com/subscriptions?api-version=2022-01-01",
    {
      headers: {
        Authorization: `Bearer ${armToken}`,
        "Content-Type": "application/json"
      }
    }
  );
  
  const data = await response.json();
  return data.value.map(sub => ({
    id: sub.subscriptionId,
    name: sub.displayName,
    state: sub.state
  }));
}
```

---

## ARM API 주요 리소스 프로바이더

| 프로바이더 | 리소스 |
|-----------|--------|
| `Microsoft.Compute` | VM, VMSS, Disks |
| `Microsoft.Storage` | Storage Accounts |
| `Microsoft.Network` | VNet, NSG, Public IP, LB |
| `Microsoft.KeyVault` | Key Vault |
| `Microsoft.Web` | App Service, Functions |
| `Microsoft.App` | Container Apps |
| `Microsoft.ContainerRegistry` | ACR |
| `Microsoft.ManagedIdentity` | Managed Identity |
| `Microsoft.OperationalInsights` | Log Analytics |
| `Microsoft.Insights` | Monitor, Diagnostics |
| `Microsoft.Authorization` | RBAC, Policy |

---

## ARM 템플릿 배포 API (커스텀 배포)

### 배포 생성

```http
PUT https://management.azure.com
  /subscriptions/{subscriptionId}
  /resourcegroups/{resourceGroupName}
  /providers/Microsoft.Resources/deployments/{deploymentName}
  ?api-version=2022-09-01

{
  "properties": {
    "mode": "Incremental",
    "templateLink": {
      "uri": "https://raw.githubusercontent.com/.../main.bicep"
    },
    "parameters": {
      "appName": { "value": "log-doctor-agent" },
      "location": { "value": "koreacentral" }
    }
  }
}
```

### 배포 모드

| 모드 | 설명 | 위험도 |
|------|------|--------|
| **Incremental** | 기존 리소스 유지 + 새 리소스 추가/업데이트 | 안전 ⭐ |
| **Complete** | 템플릿에 없는 리소스는 삭제 | ⚠️ 위험 |

---

## Azure Portal 커스텀 배포 (Portal Handoff) ⭐

### Log-Doctor에서 사용하는 패턴

```
Teams 앱에서 → Azure Portal 커스텀 배포 화면으로 리다이렉트
```

### 커스텀 배포 URL 형식

```
https://portal.azure.com/#create/Microsoft.Template/uri/{encodedTemplateUri}
```

### 실제 URL 생성

```javascript
function generateDeploymentUrl(subscriptionId, templateUrl) {
  const encodedTemplate = encodeURIComponent(templateUrl);
  
  // 기본 커스텀 배포
  return `https://portal.azure.com/#create/Microsoft.Template/uri/${encodedTemplate}`;
}

// 또는 구독 ID와 리소스 그룹을 미리 지정
function generateDeploymentUrlWithParams(subscriptionId, templateUrl, params) {
  const encodedTemplate = encodeURIComponent(templateUrl);
  const encodedParams = encodeURIComponent(JSON.stringify(params));
  
  return `https://portal.azure.com/#create/Microsoft.Template`
    + `/uri/${encodedTemplate}`
    + `/createUIDefinitionUri/${encodedCreateUi}`;  // 커스텀 UI 정의
}
```

### Deploy to Azure 버튼 (GitHub에서 자주 보는 것)

```markdown
[![Deploy to Azure](https://aka.ms/deploytoazurebutton)](
  https://portal.azure.com/#create/Microsoft.Template/uri/
  https%3A%2F%2Fraw.githubusercontent.com%2Fmyorg%2Fmyrepo%2Fmain%2Fazuredeploy.json
)
```

### 파라미터를 미리 채워주는 URL

```
https://portal.azure.com/#create/Microsoft.Template
  /uri/{encodedTemplateUri}
  /deploymentParameters/{encodedParametersJson}
```

```javascript
// Log-Doctor에서 실제 사용하는 패턴
const templateUrl = "https://raw.githubusercontent.com/log-doctor/agent/main/deploy/azuredeploy.json";
const params = {
  "$schema": "https://schema.management.azure.com/schemas/2019-04-01/deploymentParameters.json#",
  "contentVersion": "1.0.0.0",
  "parameters": {
    "subscriptionId": { "value": selectedSubscriptionId },
    "saasEndpoint": { "value": "https://api.log-doctor.com" },
    "agentVersion": { "value": "1.0.0" }
  }
};

const deployUrl = `https://portal.azure.com/#create/Microsoft.Template`
  + `/uri/${encodeURIComponent(templateUrl)}`
  + `/deploymentParameters/${encodeURIComponent(JSON.stringify(params))}`;

// Teams 앱에서 새 창으로 열기
window.open(deployUrl, "_blank");
```

---

## 인증 방법

### 1. Bearer Token (사용자 대신 — OBO)

```http
GET https://management.azure.com/subscriptions
Authorization: Bearer eyJ0eXAiOiJKV1Qi...
```

### 2. Client Credentials (앱 자체 — 서비스 주체)

```python
from azure.identity import ClientSecretCredential
from azure.mgmt.resource import SubscriptionClient

credential = ClientSecretCredential(
    tenant_id=TENANT_ID,
    client_id=CLIENT_ID,
    client_secret=CLIENT_SECRET
)

sub_client = SubscriptionClient(credential)
for sub in sub_client.subscriptions.list():
    print(sub.display_name)
```

### 3. Managed Identity (Azure 서비스에서)

```python
from azure.identity import DefaultAzureCredential
from azure.mgmt.resource import ResourceManagementClient

credential = DefaultAzureCredential()
client = ResourceManagementClient(credential, subscription_id)
```

---

## Azure SDK vs REST API

| 항목 | REST API 직접 호출 | Azure SDK |
|------|-------------------|-----------|
| 유연성 | ⭐⭐⭐ 최대 | ⭐⭐ 보통 |
| 개발 편의성 | ⭐ 수동 | ⭐⭐⭐ 편리 |
| 인증 처리 | 수동 (Bearer Token) | 자동 (Credential) |
| 페이징 | 수동 (nextLink) | 자동 |
| 재시도 | 수동 | 자동 |
| 타입 안전성 | ❌ | ✅ |

**Log-Doctor에서는 OBO 토큰으로 REST API 직접 호출이 적합** (고객의 토큰을 사용하므로)

---

## 에러 처리

| HTTP 코드 | 의미 | 대응 |
|-----------|------|------|
| 400 | Bad Request | 요청 형식 확인 |
| 401 | Unauthorized | 토큰 만료 → 재발급 |
| 403 | Forbidden | 권한 부족 → RBAC 확인 |
| 404 | Not Found | 리소스/구독 존재 확인 |
| 409 | Conflict | 리소스 이미 존재 |
| 429 | Too Many Requests | Rate Limit → 재시도 (Retry-After 헤더) |
| 500 | Internal Server Error | Azure 측 이슈 → 재시도 |

### Rate Limit (요청 제한)

```
ARM API 제한:
  - 구독당: 12,000 읽기/시간, 1,200 쓰기/시간
  - 테넌트당: 12,000 읽기/시간
```

```python
# 429 응답 시 Retry-After 헤더 확인
if response.status_code == 429:
    retry_after = int(response.headers.get("Retry-After", 60))
    time.sleep(retry_after)
    # 재시도
```
