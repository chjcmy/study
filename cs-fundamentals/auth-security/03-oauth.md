# OAuth 2.0

---

## 핵심 개념

```
OAuth 2.0 = 제3자 애플리케이션에 사용자 리소스 접근 권한을 안전하게 위임하는 프로토콜

"비밀번호를 주지 않고도, 내 데이터에 접근할 수 있게 해줄게"

4가지 역할:
1. Resource Owner (리소스 소유자) = 사용자 (사람)
2. Client (클라이언트)            = 제3자 앱 (모바일앱, 웹앱)
3. Authorization Server (인증 서버) = Google, GitHub, Kakao 등
4. Resource Server (리소스 서버)    = Google API, GitHub API 등

실생활 비유:
  발렛 파킹 키: 자동차의 모든 기능이 아니라 "주차만" 할 수 있는 제한된 키
  OAuth 토큰: 모든 권한이 아니라 "허가된 범위(scope)만" 접근 가능
```

---

## Grant Types (인증 흐름)

### 1. Authorization Code Flow (가장 안전, 가장 보편적) ⭐

```
서버 사이드 앱에서 사용 (Spring, Django, Express 등)

사용자              클라이언트 앱            인증 서버 (Google)
  │──"구글 로그인"──→│                    │
  │                │──/authorize?         │
  │                │  client_id=xxx       │
  │                │  redirect_uri=...    │
  │                │  scope=email profile │
  │                │  state=csrf_token ──→│
  │                │                      │
  │←── 구글 로그인 페이지 ────────────────│
  │──ID/PW 입력 + 동의─────────────→│
  │                │                      │
  │                │← code=abc123 ───────│ (redirect로 전달)
  │                │                      │
  │                │──POST /token         │
  │                │  code=abc123         │
  │                │  client_secret=xxx   │ (서버에서만!)
  │                │────────────────→│
  │                │← access_token ──────│
  │←── 로그인 완료 ──│                   │

핵심:
  - Authorization Code는 1회용!
  - Client Secret은 서버 사이드에서만 사용
  - 브라우저에는 Secret이 노출되지 않음 = 안전
  - state 파라미터로 CSRF 방지
```

### 2. Authorization Code + PKCE (SPA/모바일용) ⭐

```
Client Secret을 안전하게 저장할 수 없는 환경 (SPA, 네이티브 앱)

1. Code Verifier 생성: 랜덤 문자열 (43~128자)
2. Code Challenge 생성: SHA256(Code Verifier) → Base64URL

앱 → 인증 서버: /authorize + code_challenge + method=S256
인증 서버 → 앱: code=abc123

앱 → 인증 서버: /token + code=abc123 + code_verifier
                 ↑ 인증 서버가 SHA256(verifier) == challenge 확인

→ code를 가로채도 verifier 없으면 토큰 교환 불가!
→ Client Secret 없이도 안전!
```

### 3. Client Credentials Flow (서비스 간 통신)

```
사용자 없이 서비스 자체가 인증 (M2M: Machine to Machine)

서비스 A ──POST /token + client_id + client_secret──→ 인증 서버
서비스 A ←── access_token ──────────────────────────── 인증 서버

사용 예시:
  - 백엔드 서비스 간 API 호출
  - 배치 작업, 데몬 프로세스
  - CI/CD 파이프라인에서 API 접근
```

### 4. Implicit Flow (더 이상 권장하지 않음)

```
과거 SPA에서 사용 — 토큰이 URL에 직접 노출 (보안 취약)
→ PKCE가 대체하여 현재는 비권장
```

---

## OAuth 2.0 핵심 개념

### Scope (권한 범위)

```
scope = 토큰이 접근할 수 있는 범위

Google 예시:
  scope=email           → 이메일만
  scope=email profile   → 이메일 + 프로필
  scope=drive.readonly  → 구글 드라이브 읽기만

GitHub 예시:
  scope=repo            → 저장소 접근
  scope=user:email      → 이메일 읽기

→ 최소 권한 원칙: 필요한 scope만 요청
```

### State (CSRF 방지)

```
state 파라미터 = 랜덤값 전송 → 콜백에서 동일한 값인지 확인

1. 앱 → 인증 서버: state=random123
2. 인증 서버 → 앱: code=abc&state=random123
3. 앱: state==random123? ✅ 정상

→ 공격자가 위조한 콜백은 state가 다름 → 거부
```

---

## OpenID Connect (OIDC)

```
OIDC = OAuth 2.0 + 인증 (Authentication) 계층

OAuth 2.0: 인가(Authorization)만 — "이 앱이 내 데이터에 접근해도 돼"
OIDC:      인증도 추가 — "이 사람이 누구인지 확인" + ID Token

ID Token (JWT 형식):
{
  "sub": "user123",
  "name": "홍길동",
  "email": "user@example.com",
  "iss": "accounts.google.com",
  "aud": "my-app-id",
  "exp": 1706003600
}

→ SSO(Single Sign-On)의 기반
→ "구글 로그인", "카카오 로그인"이 OIDC
```

---

## 면접 핵심 포인트

```
Q: OAuth 2.0의 Authorization Code Flow를 설명하세요
A: 1. 클라이언트가 인증 서버에 인증 요청 (redirect)
   2. 사용자가 로그인 + 동의하면 Authorization Code 반환
   3. 클라이언트가 Code + Secret으로 Access Token 교환
   → Code는 1회용, Secret은 서버에서만 → 보안 강화

Q: OAuth 2.0과 OIDC의 차이?
A: OAuth 2.0: 인가(Authorization) 전용 — 리소스 접근 권한 위임
   OIDC: OAuth 2.0 위에 인증(Authentication) 추가 — 사용자 신원 확인
   OIDC는 ID Token(JWT)으로 사용자 정보 제공

Q: PKCE란?
A: Proof Key for Code Exchange. SPA/모바일처럼 Client Secret을
   안전하게 저장할 수 없는 환경에서 Code 탈취 공격 방지.
   Code Verifier/Challenge 쌍으로 Code 교환을 검증.
```
