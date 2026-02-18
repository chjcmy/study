# Microsoft Entra ID 완전 정리

이전 이름: Azure AD (Azure Active Directory)  
Azure의 **클라우드 기반 ID 및 접근 관리(IAM)** 서비스입니다.

---

## Entra ID란?

```
사용자/앱이 Azure 리소스에 접근하려면:
  1. "나는 누구인가?" → 인증 (Authentication)
  2. "무엇을 할 수 있나?" → 인가 (Authorization)

Entra ID = 이 두 가지를 모두 담당하는 중앙 서비스
```

---

## 핵심 개념

### 테넌트 (Tenant)

```
하나의 조직 = 하나의 Entra ID 테넌트
  └─ 테넌트 ID: a1b2c3d4-e5f6-...
  └─ 도메인: mycompany.onmicrosoft.com
  └─ 사용자, 그룹, 앱 등록 데이터가 여기에 저장
```

| 항목 | 설명 |
|------|------|
| 테넌트 | 조직의 Entra ID 인스턴스 (1개 조직 = 1개 테넌트) |
| 테넌트 ID | GUID 형태의 고유 식별자 |
| 도메인 | `*.onmicrosoft.com` 또는 커스텀 도메인 |

### 멀티 테넌트 (Multi-Tenant) — SaaS의 핵심

```
┌─────────────────────────┐
│  새싹 테넌트 (우리 SaaS) │  ← 앱 등록 (App Registration)
│  tenant: abc-123...      │
└──────────┬──────────────┘
           │ 멀티 테넌트 앱
    ┌──────┼──────┐
    ▼      ▼      ▼
┌──────┐┌──────┐┌──────┐
│고객 A ││고객 B ││고객 C │  ← 각각 Admin Consent으로 앱 허용
│tenant││tenant││tenant│
└──────┘└──────┘└──────┘
```

| 모드 | 설명 | 용도 |
|------|------|------|
| Single-Tenant | 내 테넌트 사용자만 | 사내 앱 |
| Multi-Tenant | 모든 테넌트 사용자 | **SaaS 앱 (Log-Doctor)** |

---

## 앱 등록 (App Registration)

Entra ID에 앱을 **등록**해야 인증/인가 사용 가능합니다.

### 등록 시 생성되는 값

| 값 | 설명 | 용도 |
|----|------|------|
| **Application (client) ID** | 앱 고유 ID | 토큰 요청 시 사용 |
| **Directory (tenant) ID** | 테넌트 ID | 인증 엔드포인트 URL |
| **Client Secret** | 앱 비밀번호 | 백엔드에서 토큰 교환 |
| **Client Certificate** | 앱 인증서 (더 안전) | 프로덕션 권장 |

### API 권한 (Permissions)

| 종류 | 설명 | 예시 |
|------|------|------|
| **Delegated** | 사용자 대신 접근 | "로그인한 사용자의 구독 조회" |
| **Application** | 앱 자체로 접근 (사용자 없이) | "백그라운드 배치 작업" |

**Log-Doctor에서 필요한 권한:**

| 권한 | 타입 | 용도 |
|------|------|------|
| `User.Read` | Delegated | 사용자 프로필 읽기 |
| `https://management.azure.com/user_impersonation` | Delegated ⭐ | ARM API 접근 (OBO) |

---

## 토큰 (Token)

### JWT (JSON Web Token) 구조

```
eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9  ← Header (알고리즘)
.                                          ← 점으로 구분
eyJhdWQiOiJhcGk6Ly9teS1hcHAiLCJpc3Mi   ← Payload (클레임)
.
SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV      ← Signature (서명)
```

### Payload 주요 클레임

```json
{
  "aud": "api://my-app",                    // 토큰 대상 (audience)
  "iss": "https://login.microsoftonline.com/tenant-id/v2.0",  // 발급자
  "iat": 1700000000,                        // 발급 시간
  "exp": 1700003600,                        // 만료 시간 (1시간)
  "sub": "user-object-id",                  // 사용자 ID
  "name": "홍길동",                          // 사용자 이름
  "preferred_username": "hong@company.com",  // 이메일
  "tid": "tenant-id",                       // 테넌트 ID
  "oid": "object-id",                       // Azure AD 객체 ID
  "scp": "User.Read user_impersonation",     // 권한 범위 (scope)
  "roles": ["Admin"]                         // 앱 역할
}
```

### 토큰 종류

| 토큰 | 수명 | 용도 |
|------|------|------|
| **Access Token** | 1시간 | API 호출 시 Authorization 헤더에 사용 |
| **Refresh Token** | 24시간~90일 | Access Token 갱신 |
| **ID Token** | 1시간 | 사용자 정보 확인 (프론트엔드) |

---

## SSO (Single Sign-On) — Teams 앱에서의 인증

### Silent SSO 흐름

```
Teams 앱 (탭)
    │
    │ ① Teams SDK - getAuthToken()
    ▼
Teams 클라이언트
    │
    │ ② 이미 Teams에 로그인되어 있으므로 Silent하게 처리
    ▼
Entra ID
    │
    │ ③ SSO 토큰 (JWT) 발급
    ▼
Teams 앱
    │
    │ ④ 백엔드에 토큰 전달
    ▼
SaaS 백엔드
```

### Teams SDK 코드

```typescript
import * as microsoftTeams from "@microsoft/teams-js";

// Teams SDK 초기화
await microsoftTeams.app.initialize();

// Silent SSO 토큰 요청 (사용자 팝업 없음!)
const token = await microsoftTeams.authentication.getAuthToken();
// → 이 토큰은 Teams 앱 전용 (audience = 앱의 client ID)
// → ARM API 접근 불가! → OBO Flow 필요

// 백엔드에 토큰 전달
const response = await fetch("https://api.log-doctor.com/auth/token", {
  headers: {
    Authorization: `Bearer ${token}`
  }
});
```

### Silent SSO가 가능한 이유

1. 사용자가 이미 **Teams에 로그인**되어 있음
2. Teams 클라이언트가 Entra ID와 **세션을 공유**
3. 앱이 같은 테넌트(또는 Admin Consent된 멀티 테넌트)에 등록
4. → 추가 로그인 팝업 없이 토큰 획득!

---

## OBO Flow (On-Behalf-Of) — 핵심 중의 핵심 ⭐

### 왜 필요한가?

```
문제:
  Teams SSO 토큰의 audience = "api://my-app" (우리 앱용)
  ARM API의 audience = "https://management.azure.com" (Azure용)
  
  → 같은 사용자인데 토큰의 대상(audience)이 다름!
  → SSO 토큰으로 직접 ARM API 호출 불가!

해결: OBO Flow
  SSO 토큰(우리 앱용) → Entra ID에 교환 요청 → ARM 토큰(Azure용) 발급
```

### OBO Flow 상세

```
        Teams 앱 (프론트)                  SaaS 백엔드                  Entra ID                 ARM API
             │                              │                           │                         │
             │── SSO 토큰 전달 ──────────▶│                           │                         │
             │                              │                           │                         │
             │                              │── OBO 토큰 교환 요청 ──▶│                         │
             │                              │   grant_type=             │                         │
             │                              │   urn:ietf:params:oauth:  │                         │
             │                              │   grant-type:jwt-bearer   │                         │
             │                              │                           │                         │
             │                              │◀── ARM Access Token ────│                         │
             │                              │   (audience=              │                         │
             │                              │    management.azure.com)  │                         │
             │                              │                           │                         │
             │                              │── GET /subscriptions ────────────────────────────▶│
             │                              │   Authorization: Bearer   │                         │
             │                              │   <ARM Token>             │                         │
             │                              │                           │                         │
             │                              │◀─────────────────────────────── 구독 목록 JSON ──│
             │                              │                           │                         │
             │◀── 구독 목록 반환 ──────────│                           │                         │
```

### OBO 토큰 교환 요청 (백엔드 코드)

```python
import requests

def exchange_token_obo(sso_token: str) -> str:
    """SSO 토큰을 ARM 접근용 토큰으로 교환"""
    
    token_url = f"https://login.microsoftonline.com/{TENANT_ID}/oauth2/v2.0/token"
    
    response = requests.post(token_url, data={
        "grant_type": "urn:ietf:params:oauth:grant-type:jwt-bearer",
        "client_id": CLIENT_ID,          # 앱 등록 Client ID
        "client_secret": CLIENT_SECRET,  # 앱 등록 Client Secret
        "assertion": sso_token,           # Teams에서 받은 SSO 토큰
        "scope": "https://management.azure.com/.default",  # ARM API 스코프
        "requested_token_use": "on_behalf_of"
    })
    
    return response.json()["access_token"]
```

### OBO 요청 파라미터

| 파라미터 | 값 | 설명 |
|---------|-----|------|
| `grant_type` | `urn:ietf:params:oauth:grant-type:jwt-bearer` | OBO 플로우 식별자 |
| `client_id` | 앱 Client ID | 우리 앱 ID |
| `client_secret` | 앱 Secret | 앱 인증 (백엔드만 알고 있음) |
| `assertion` | SSO JWT 토큰 | Teams에서 받은 원본 토큰 |
| `scope` | `https://management.azure.com/.default` | 교환 대상 API 스코프 |
| `requested_token_use` | `on_behalf_of` | OBO 사용 명시 |

### OBO의 핵심 포인트

1. **사용자의 권한을 그대로 위임** — 백엔드가 사용자 대신(on behalf of) 접근
2. **백엔드만 가능** — `client_secret`이 필요하므로 프론트에서 불가
3. **scope 변경만 가능** — 원본 토큰의 사용자 정보는 동일
4. **Admin Consent 필수** — 멀티 테넌트 OBO는 관리자 동의 필요

---

## Admin Consent (관리자 동의)

### 왜 필요한가?

```
고객사의 사용자가 우리 SaaS 앱을 사용하려면:
  1. 앱이 "이 사용자의 구독을 읽겠습니다" 권한 요청
  2. 고객사 관리자가 "허용합니다" 동의
  3. → 그 이후부터 해당 테넌트의 모든 사용자가 앱 사용 가능
```

### Consent 종류

| 종류 | 누가 | 적용 범위 | 사용 사례 |
|------|------|----------|----------|
| **User Consent** | 개별 사용자 | 본인만 | 낮은 권한 (User.Read 등) |
| **Admin Consent** | 테넌트 관리자 | 테넌트 전체 | 높은 권한 (ARM 접근 등) ⭐ |

### Admin Consent URL

```
https://login.microsoftonline.com/{tenant-id}/adminconsent
  ?client_id={앱-client-id}
  &redirect_uri={앱-리다이렉트-URI}
  &scope=https://management.azure.com/.default
```

### Teams 앱에서 Admin Consent 팝업

```typescript
// Teams SDK로 인증 팝업 띄우기
const result = await microsoftTeams.authentication.authenticate({
  url: `https://login.microsoftonline.com/common/adminconsent?client_id=${CLIENT_ID}&redirect_uri=${REDIRECT_URI}`,
  width: 600,
  height: 535
});
```

### Consent 후 Entra ID에 기록되는 것

```
고객사 테넌트 (Entra ID)
  └─ Enterprise Applications (엔터프라이즈 애플리케이션)
       └─ "Log-Doctor" 앱
            ├─ 허용된 권한: User.Read, user_impersonation
            ├─ Consent 타입: Admin
            └─ 동의 일시: 2025-01-15
```

---

## OAuth 2.0 인증 플로우 비교

| 플로우 | 사용 환경 | 특징 |
|--------|----------|------|
| **Authorization Code** | 웹 앱 (서버 사이드) | 가장 일반적, code → token |
| **Authorization Code + PKCE** | SPA, 모바일 | secret 없이 안전하게 |
| **Client Credentials** | 서버 to 서버 | 사용자 없이 앱 자체 인증 |
| **On-Behalf-Of (OBO)** | 백엔드 API | 토큰 교환 ⭐ |
| **Device Code** | CLI, IoT | 브라우저 없는 환경 |
| **Implicit** | (레거시 SPA) | ❌ 더 이상 권장 안함 |

---

## MSAL (Microsoft Authentication Library)

Entra ID 인증을 구현하는 **공식 라이브러리**입니다.

### Python (MSAL)

```python
from msal import ConfidentialClientApplication

# 앱 초기화
app = ConfidentialClientApplication(
    client_id=CLIENT_ID,
    authority=f"https://login.microsoftonline.com/{TENANT_ID}",
    client_credential=CLIENT_SECRET
)

# OBO 토큰 교환
result = app.acquire_token_on_behalf_of(
    user_assertion=sso_token,
    scopes=["https://management.azure.com/.default"]
)

arm_token = result["access_token"]
```

### Node.js (MSAL)

```javascript
const { ConfidentialClientApplication } = require("@azure/msal-node");

const msalClient = new ConfidentialClientApplication({
  auth: {
    clientId: CLIENT_ID,
    authority: `https://login.microsoftonline.com/${TENANT_ID}`,
    clientSecret: CLIENT_SECRET
  }
});

// OBO 토큰 교환
const result = await msalClient.acquireTokenOnBehalfOf({
  oboAssertion: ssoToken,
  scopes: ["https://management.azure.com/.default"]
});

const armToken = result.accessToken;
```

---

## Entra ID 엔드포인트

| 엔드포인트 | URL | 용도 |
|-----------|-----|------|
| 인증 | `https://login.microsoftonline.com/{tenant}/oauth2/v2.0/authorize` | 사용자 로그인 |
| 토큰 | `https://login.microsoftonline.com/{tenant}/oauth2/v2.0/token` | 토큰 발급/교환 |
| Admin Consent | `https://login.microsoftonline.com/{tenant}/adminconsent` | 관리자 동의 |
| JWKS | `https://login.microsoftonline.com/{tenant}/discovery/v2.0/keys` | 토큰 서명 검증 키 |
| OpenID Config | `https://login.microsoftonline.com/{tenant}/v2.0/.well-known/openid-configuration` | 전체 설정 |

> `{tenant}` 자리에 `common` (멀티 테넌트), `organizations` (조직만), 또는 특정 tenant ID

---

## CLI 명령어

```bash
# 앱 등록 생성
az ad app create \
  --display-name "Log-Doctor" \
  --sign-in-audience AzureADMultipleOrgs  # 멀티 테넌트

# 앱 등록 조회
az ad app list --display-name "Log-Doctor" -o table

# Client Secret 추가
az ad app credential reset \
  --id <app-object-id> \
  --append

# API 권한 추가
az ad app permission add \
  --id <app-id> \
  --api 797f4846-ba00-4fd7-ba43-dac1f8f63013 \  # ARM API
  --api-permissions 41094075-9dad-400e-a0bd-54e686782033=Scope  # user_impersonation

# Admin Consent 부여
az ad app permission admin-consent --id <app-id>

# 서비스 주체(Enterprise App) 생성
az ad sp create --id <app-id>
```

---

## 보안 Best Practice

1. **Client Secret은 Key Vault에 저장** — 코드/설정에 하드코딩 금지
2. **토큰 캐싱** — MSAL 라이브러리의 내장 캐시 활용 (불필요한 OBO 호출 방지)
3. **최소 scope 원칙** — 필요한 API 권한만 요청
4. **토큰 검증** — 백엔드에서 반드시 JWT 서명, audience, 만료 검증
5. **HTTPS Only** — 토큰은 반드시 HTTPS로만 전송
6. **Refresh Token 보호** — 서버 사이드에서만 저장, 클라이언트 노출 금지
