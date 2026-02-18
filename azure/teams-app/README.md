# Teams App 완전 정리

Microsoft Teams 내에서 동작하는 **웹 기반 애플리케이션**입니다.

---

## Teams 앱 구성 요소

```
Teams 앱
├── Tab (탭)        ← 웹 페이지를 Teams 내에 임베드 ⭐ (Log-Doctor)
├── Bot (봇)        ← 대화형 인터페이스
├── Message Extension ← 메시지 확장 (검색/액션)
├── Webhook         ← 알림 전송 채널
└── Meeting Extension ← 회의 확장
```

### Log-Doctor = Tab 앱

| 항목 | 설명 |
|------|------|
| 형태 | Personal Tab (개인 탭) |
| 기술 | React SPA (Single Page Application) |
| 호스팅 | 별도 웹 서버에서 호스팅 → Teams에서 iframe으로 로드 |
| 인증 | Teams SDK SSO → 백엔드 OBO Flow |

---

## Teams SDK (teams-js)

Teams 앱에서 Teams 클라이언트와 상호작용하는 공식 라이브러리

### 설치

```bash
npm install @microsoft/teams-js
```

### 초기화

```typescript
import * as microsoftTeams from "@microsoft/teams-js";

// 앱 시작 시 반드시 초기화
async function initializeTeams() {
  await microsoftTeams.app.initialize();
  
  // Teams 컨텍스트 정보 가져오기
  const context = await microsoftTeams.app.getContext();
  
  console.log("User ID:", context.user?.id);
  console.log("Tenant ID:", context.user?.tenant?.id);
  console.log("Theme:", context.app.theme);         // dark, default, contrast
  console.log("Host:", context.app.host.name);       // Teams, Outlook, Office
  console.log("Locale:", context.app.locale);        // ko-KR, en-US
}
```

### SSO 인증

```typescript
// Silent SSO — 사용자 팝업 없이 토큰 획득
async function getToken(): Promise<string> {
  try {
    const token = await microsoftTeams.authentication.getAuthToken();
    // token = SSO JWT (audience = 우리 앱의 Client ID)
    return token;
  } catch (error) {
    console.error("SSO failed:", error);
    // Fallback: 팝업 인증
    return await popupAuth();
  }
}

// 팝업 인증 (SSO 실패 시 또는 Admin Consent 필요 시)
async function popupAuth(): Promise<string> {
  const result = await microsoftTeams.authentication.authenticate({
    url: `${window.location.origin}/auth-start`,
    width: 600,
    height: 535
  });
  return result;
}
```

### 테마 감지

```typescript
// Teams 테마가 변경될 때마다 호출
microsoftTeams.app.registerOnThemeChangeHandler((theme) => {
  // theme: "default" | "dark" | "contrast"
  document.body.setAttribute("data-theme", theme);
});
```

### 딥링크 & 네비게이션

```typescript
// Teams 내에서 새 창으로 URL 열기 (Azure Portal 등)
await microsoftTeams.app.openLink("https://portal.azure.com/#create/...");

// 또는 외부 브라우저에서 열기
window.open(deployUrl, "_blank");
```

---

## 앱 매니페스트 (manifest.json)

Teams 앱의 **설정 파일** — 이름, 탭, 권한 등을 정의

```json
{
  "$schema": "https://developer.microsoft.com/json-schemas/teams/v1.17/MicrosoftTeams.schema.json",
  "manifestVersion": "1.17",
  "version": "1.0.0",
  "id": "app-guid-here",
  
  "developer": {
    "name": "Log-Doctor Inc",
    "websiteUrl": "https://log-doctor.com",
    "privacyUrl": "https://log-doctor.com/privacy",
    "termsOfUseUrl": "https://log-doctor.com/terms"
  },
  
  "name": {
    "short": "Log-Doctor",
    "full": "Log-Doctor - Azure 비용 절감 솔루션"
  },
  
  "description": {
    "short": "Azure 비용을 분석하고 절감합니다",
    "full": "Log-Doctor는 Azure 구독의 리소스 사용량을 분석하여 비용 절감 방안을 제시하는 SaaS 솔루션입니다."
  },
  
  "icons": {
    "color": "color.png",       // 192x192
    "outline": "outline.png"    // 32x32
  },
  
  "accentColor": "#4F6BED",
  
  "staticTabs": [
    {
      "entityId": "dashboard",
      "name": "Dashboard",
      "contentUrl": "https://app.log-doctor.com/tab",
      "websiteUrl": "https://app.log-doctor.com",
      "scopes": ["personal"]
    }
  ],
  
  "permissions": ["identity", "messageTeamMembers"],
  
  "validDomains": [
    "app.log-doctor.com",
    "api.log-doctor.com"
  ],
  
  "webApplicationInfo": {
    "id": "client-id-of-entra-app",                     // Entra ID 앱 Client ID
    "resource": "api://app.log-doctor.com/client-id"     // Application ID URI
  }
}
```

### 주요 필드

| 필드 | 설명 |
|------|------|
| `staticTabs` | Personal Tab 정의 (Log-Doctor 대시보드) |
| `contentUrl` | Tab이 로드할 웹 페이지 URL |
| `validDomains` | 허용된 도메인 (보안) |
| `webApplicationInfo` | SSO 인증 설정 ⭐ |
| `webApplicationInfo.id` | Entra ID 앱 Client ID |
| `webApplicationInfo.resource` | Application ID URI |

---

## React 앱 구조 (Log-Doctor)

```
src/
├── App.tsx                  # 라우팅
├── index.tsx                # 진입점
├── components/
│   ├── Tab.tsx              # 메인 탭 컴포넌트 ⭐
│   ├── SubscriptionList.tsx # 구독 선택 드롭다운
│   ├── DeployButton.tsx     # 에이전트 설치 버튼
│   ├── Dashboard.tsx        # 대시보드 (설치 후)
│   └── ConsentPopup.tsx     # Admin Consent 팝업
├── services/
│   ├── authService.ts       # SSO + OBO 토큰 관리
│   ├── apiService.ts        # 백엔드 API 호출
│   └── teamsService.ts      # Teams SDK 래퍼
├── hooks/
│   ├── useTeams.ts          # Teams 초기화 훅
│   └── useSubscriptions.ts  # 구독 조회 훅
└── types/
    └── index.ts             # 타입 정의
```

### 메인 Tab 컴포넌트

```typescript
// components/Tab.tsx
import { useState, useEffect } from "react";
import { useTeams } from "../hooks/useTeams";
import { SubscriptionList } from "./SubscriptionList";
import { DeployButton } from "./DeployButton";
import { Dashboard } from "./Dashboard";

export function Tab() {
  const { token, isAuthenticated, needsConsent } = useTeams();
  const [subscriptions, setSubscriptions] = useState([]);
  const [selectedSub, setSelectedSub] = useState(null);
  const [agentStatus, setAgentStatus] = useState("none"); // none | deploying | active

  useEffect(() => {
    if (isAuthenticated && token) {
      fetchSubscriptions(token).then(setSubscriptions);
    }
  }, [isAuthenticated, token]);

  // Admin Consent 필요 시
  if (needsConsent) {
    return <ConsentPopup />;
  }

  // 에이전트 활성화 완료 시 → 대시보드
  if (agentStatus === "active") {
    return <Dashboard subscriptionId={selectedSub} />;
  }

  // 구독 선택 + 설치 화면
  return (
    <div>
      <h1>Log-Doctor 에이전트 설치</h1>
      <SubscriptionList
        subscriptions={subscriptions}
        onSelect={setSelectedSub}
      />
      {selectedSub && (
        <DeployButton
          subscriptionId={selectedSub}
          onDeployStarted={() => setAgentStatus("deploying")}
        />
      )}
    </div>
  );
}
```

### 구독 선택 컴포넌트

```typescript
// components/SubscriptionList.tsx
interface Subscription {
  id: string;
  name: string;
  state: string;
}

export function SubscriptionList({ subscriptions, onSelect }) {
  return (
    <div>
      <label>구독 선택</label>
      <select onChange={(e) => onSelect(e.target.value)}>
        <option value="">-- 구독을 선택하세요 --</option>
        {subscriptions.map((sub: Subscription) => (
          <option key={sub.id} value={sub.id}>
            {sub.name} ({sub.id.slice(0, 8)}...)
          </option>
        ))}
      </select>
    </div>
  );
}
```

### Deploy 버튼 (Portal Handoff)

```typescript
// components/DeployButton.tsx
const TEMPLATE_URL = "https://raw.githubusercontent.com/log-doctor/agent/main/deploy/azuredeploy.json";

export function DeployButton({ subscriptionId, onDeployStarted }) {
  
  const handleDeploy = () => {
    const params = {
      "$schema": "https://schema.management.azure.com/schemas/2019-04-01/deploymentParameters.json#",
      "contentVersion": "1.0.0.0",
      "parameters": {
        "subscriptionId": { "value": subscriptionId },
        "saasEndpoint": { "value": "https://api.log-doctor.com" }
      }
    };
    
    const deployUrl = `https://portal.azure.com/#create/Microsoft.Template`
      + `/uri/${encodeURIComponent(TEMPLATE_URL)}`
      + `/deploymentParameters/${encodeURIComponent(JSON.stringify(params))}`;
    
    // Azure Portal을 새 창으로 열기
    window.open(deployUrl, "_blank");
    onDeployStarted();
  };

  return (
    <button onClick={handleDeploy}>
      🚀 1-Click 에이전트 설치
    </button>
  );
}
```

### 상태 폴링

```typescript
// hooks/useAgentStatus.ts
export function useAgentStatus(subscriptionId: string) {
  const [status, setStatus] = useState<"none" | "deploying" | "active">("none");

  useEffect(() => {
    if (!subscriptionId) return;

    const interval = setInterval(async () => {
      const res = await fetch(
        `https://api.log-doctor.com/agents/${subscriptionId}/status`
      );
      const data = await res.json();
      
      if (data.active) {
        setStatus("active");
        clearInterval(interval);  // 활성화되면 폴링 중단
      }
    }, 5000);  // 5초마다 확인

    return () => clearInterval(interval);
  }, [subscriptionId]);

  return status;
}
```

---

## 앱 배포 및 테스트

### 로컬 개발

```bash
# Teams Toolkit 사용 (VS Code 확장)
npx @microsoft/teamsfx-cli new --interactive false --app-type tab

# 개발 서버 실행
npm run dev

# ngrok으로 외부 접근 가능하게 (Teams에서 로드)
ngrok http 3000
```

### Teams 앱 사이드로드 (테스트용)

```
1. manifest.json + 아이콘 2개를 zip으로 압축
2. Teams → 앱 → 앱 업로드 → 사용자 지정 앱 업로드
3. zip 파일 선택 → 설치
```

### Teams 앱 스토어 배포 (프로덕션)

```
1. Partner Center에 앱 등록
2. 심사 통과 후 Teams 앱 스토어에 공개
3. 또는 조직 내 앱 카탈로그에 배포
```

---

## SaaS 백엔드 API (Log-Doctor API)

### API 구조

```
POST /auth/token         → SSO 토큰 검증 + OBO 교환
GET  /subscriptions      → 고객의 구독 목록 (OBO 토큰으로 ARM 조회)
GET  /agents/{subId}/status → 에이전트 상태 확인
POST /agents             → 에이전트 설치 완료 Webhook (에이전트 → SaaS)
GET  /dashboard/{subId}  → 대시보드 데이터
```

### 인증 플로우 (백엔드)

```python
# POST /auth/token
@app.post("/auth/token")
async def exchange_token(request):
    # 1. Teams SSO 토큰 추출
    sso_token = request.headers["Authorization"].replace("Bearer ", "")
    
    # 2. 토큰 검증 (audience, issuer, 서명)
    claims = validate_jwt(sso_token)
    tenant_id = claims["tid"]
    user_id = claims["oid"]
    
    # 3. OBO로 ARM 토큰 교환
    arm_token = exchange_obo(sso_token)
    
    # 4. DB에 사용자/테넌트 정보 저장
    upsert_user(tenant_id, user_id, claims["name"])
    
    return {"armToken": arm_token, "tenantId": tenant_id}
```

---

## Webhook — 에이전트 Handshake ⭐

### 에이전트 → SaaS 설치 완료 알림

```
Azure 리소스 배포 완료
    │
    ▼
Function App (에이전트) 최초 기동
    │
    │ POST https://api.log-doctor.com/agents
    │ {
    │   "subscriptionId": "abc-123",
    │   "tenantId": "xyz-789",
    │   "agentVersion": "1.0.0",
    │   "functionAppName": "func-log-doctor-abc"
    │ }
    ▼
SaaS 백엔드
    │
    │ DB 업데이트: status = "Active"
    ▼
Teams 앱 (폴링 → 상태 감지)
    │
    │ 대시보드 화면으로 전환
    ▼
고객에게 대시보드 표시 🎉
```

### 에이전트 코드 (Function App)

```python
# 에이전트가 최초 기동 시 실행하는 함수
import os
import requests

def agent_startup():
    """에이전트 설치 완료를 SaaS에 알림"""
    
    saas_endpoint = os.environ["SAAS_ENDPOINT"]
    subscription_id = os.environ["SUBSCRIPTION_ID"]
    
    response = requests.post(
        f"{saas_endpoint}/agents",
        json={
            "subscriptionId": subscription_id,
            "tenantId": os.environ.get("TENANT_ID"),
            "agentVersion": "1.0.0",
            "functionAppName": os.environ["WEBSITE_SITE_NAME"],
            "region": os.environ.get("REGION_NAME", "unknown")
        },
        headers={
            "X-Agent-Key": os.environ["AGENT_SECRET_KEY"]
        }
    )
    
    if response.status_code == 200:
        print("✅ Agent registered successfully")
    else:
        print(f"❌ Registration failed: {response.status_code}")
```

### SaaS 백엔드 (Webhook 수신)

```python
# POST /agents — 에이전트 등록 Webhook
@app.post("/agents")
async def register_agent(request):
    body = await request.json()
    
    # DB 업데이트
    await db.agents.upsert({
        "subscriptionId": body["subscriptionId"],
        "tenantId": body["tenantId"],
        "agentVersion": body["agentVersion"],
        "functionAppName": body["functionAppName"],
        "status": "Active",
        "registeredAt": datetime.utcnow()
    })
    
    return {"status": "ok"}
```

---

## 에이전트 배포 Bicep 템플릿

```bicep
param location string = resourceGroup().location
param subscriptionId string
param saasEndpoint string = 'https://api.log-doctor.com'

// Storage (Functions 필수)
resource storage 'Microsoft.Storage/storageAccounts@2023-01-01' = {
  name: 'stlogdoctor${uniqueString(resourceGroup().id)}'
  location: location
  kind: 'StorageV2'
  sku: { name: 'Standard_LRS' }
}

// Managed Identity
resource identity 'Microsoft.ManagedIdentity/userAssignedIdentities@2023-01-31' = {
  name: 'mi-log-doctor-agent'
  location: location
}

// RBAC — 구독 수준 Reader 권한
resource readerRole 'Microsoft.Authorization/roleAssignments@2022-04-01' = {
  name: guid(subscription().id, identity.id, 'reader')
  properties: {
    roleDefinitionId: subscriptionResourceId(
      'Microsoft.Authorization/roleDefinitions',
      'acdd72a7-3385-48ef-bd42-f606fba81ae7'  // Reader
    )
    principalId: identity.properties.principalId
    principalType: 'ServicePrincipal'
  }
}

// Function App (에이전트)
resource functionApp 'Microsoft.Web/sites@2022-09-01' = {
  name: 'func-log-doctor-${uniqueString(resourceGroup().id)}'
  location: location
  kind: 'functionapp,linux'
  identity: {
    type: 'UserAssigned'
    userAssignedIdentities: {
      '${identity.id}': {}
    }
  }
  properties: {
    serverFarmId: plan.id
    siteConfig: {
      linuxFxVersion: 'PYTHON|3.11'
      appSettings: [
        { name: 'FUNCTIONS_WORKER_RUNTIME', value: 'python' }
        { name: 'FUNCTIONS_EXTENSION_VERSION', value: '~4' }
        { name: 'SAAS_ENDPOINT', value: saasEndpoint }
        { name: 'SUBSCRIPTION_ID', value: subscriptionId }
        { name: 'AZURE_CLIENT_ID', value: identity.properties.clientId }
        {
          name: 'AzureWebJobsStorage'
          value: 'DefaultEndpointsProtocol=https;AccountName=${storage.name};AccountKey=${storage.listKeys().keys[0].value}'
        }
      ]
    }
  }
}

// Consumption Plan
resource plan 'Microsoft.Web/serverfarms@2022-09-01' = {
  name: 'plan-log-doctor'
  location: location
  sku: { name: 'Y1', tier: 'Dynamic' }
  properties: { reserved: true }
}
```

---

## 전체 시퀀스 요약

```
[1단계: 접속 + 인증]
Teams 앱 실행 → Teams SDK SSO → Entra ID 토큰 → 백엔드 Consent 확인

[2단계: 구독 조회]
백엔드 OBO 교환 → ARM API GET /subscriptions → 구독 목록 반환

[3단계: 에이전트 배포]
구독 선택 → Deploy 버튼 → Azure Portal 리다이렉트 → Bicep 배포 실행

[4단계: Handshake]
Function App 기동 → POST /agents Webhook → DB Active → 대시보드 전환
```
