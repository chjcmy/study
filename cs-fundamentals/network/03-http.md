# HTTP 완전 정리

---

## HTTP 버전 비교

| 버전 | 연결 | 특징 | 성능 |
|------|------|------|------|
| **HTTP/1.0** | 매 요청마다 TCP 연결 | 단순 | 느림 |
| **HTTP/1.1** | Keep-Alive (재사용) | 파이프라이닝 | 보통 |
| **HTTP/2** | 멀티플렉싱 (1 연결 N 요청) | 바이너리, 서버 푸시 | 빠름 |
| **HTTP/3** | QUIC (UDP 기반) | 0-RTT, 연결 마이그레이션 | 가장 빠름 |

### HTTP/1.0 → HTTP/1.1 진화

```
HTTP/1.0: 요청마다 TCP 연결 새로 맺음 (매번 3-way handshake)
──────────
[TCP 연결]──GET /index──[응답]──[TCP 종료]
[TCP 연결]──GET /style──[응답]──[TCP 종료]    ← 비효율!
[TCP 연결]──GET /image──[응답]──[TCP 종료]

HTTP/1.1: Keep-Alive로 연결 재사용
──────────
[TCP 연결]
  ├── GET /index ──→ 응답
  ├── GET /style ──→ 응답     ← 같은 연결 재사용!
  └── GET /image ──→ 응답
[TCP 종료]

추가된 기능:
  - Host 헤더 필수 (가상 호스팅)
  - Chunked Transfer Encoding
  - Cache-Control 헤더
```

### HTTP/1.1 vs HTTP/2

```
HTTP/1.1 (Head-of-Line Blocking 문제)
──────────────────────────────
요청1 ──→ │응답1│ 요청2 ──→ │응답2│ 요청3 ──→ │응답3│
           ↑ 이 응답이 올 때까지 다음 요청 불가!

해결 시도: 브라우저가 도메인당 6~8개 TCP 연결 → 비효율

HTTP/2 (Multiplexing으로 해결)
──────────────────────────────
요청1 ──→ │
요청2 ──→ │──→ 응답1, 응답3, 응답2  (순서 상관없이 동시 처리)
요청3 ──→ │

한 TCP 연결 안에서 여러 "스트림"이 동시에 흐름
```

### HTTP/2 핵심 특징

```
1. 바이너리 프레이밍
   HTTP/1.1: 텍스트 프로토콜   "GET /index.html HTTP/1.1\r\n"
   HTTP/2:   바이너리 프레임   [Type][Length][Flags][Stream ID][Payload]
   → 파싱 더 빠르고 오류 적음

2. 멀티플렉싱
   하나의 TCP 연결에서 여러 요청/응답 동시 처리
   각 스트림에 고유 ID 부여

3. 헤더 압축 (HPACK)
   HTTP/1.1: 매 요청마다 헤더 전체 전송 (쿠키 포함 수 KB)
   HTTP/2:   허프만 코딩 + 정적/동적 테이블로 압축
   → 반복 헤더 크기 85~88% 감소

4. 서버 푸시
   클라이언트가 요청하기 전에 필요한 리소스 미리 전송
   HTML 요청 → 서버가 CSS, JS도 함께 전송
```

### HTTP/3 (QUIC)

```
HTTP/2의 문제: TCP 레벨 Head-of-Line Blocking
  → TCP 패킷 하나 유실 시 모든 스트림이 블로킹!

HTTP/3 해결: UDP 기반 QUIC 프로토콜
  → 스트림별 독립적 전송, 하나 유실되어도 나머지 정상 진행

추가 장점:
  - 0-RTT: 이전에 연결했던 서버라면 즉시 데이터 전송
  - 연결 마이그레이션: Wi-Fi → LTE 전환해도 연결 유지
    (Connection ID 기반, IP 변경 무관)
```

---

## HTTP 메서드

| 메서드 | 용도 | 멱등성 | 안전 | 본문 | 예시 |
|--------|------|--------|-----|------|------|
| **GET** | 조회 | ✅ | ✅ | ❌ | `GET /api/v1/users/me` |
| **POST** | 생성 | ❌ | ❌ | ✅ | `POST /api/v1/users` |
| **PUT** | 전체 수정 | ✅ | ❌ | ✅ | `PUT /api/v1/users/123` |
| **PATCH** | 부분 수정 | ❌ | ❌ | ✅ | `PATCH /api/v1/users/123` |
| **DELETE** | 삭제 | ✅ | ❌ | ❌ | `DELETE /api/v1/users/123` |
| **OPTIONS** | 허용 메서드 확인 | ✅ | ✅ | ❌ | CORS Preflight |
| **HEAD** | 헤더만 응답 | ✅ | ✅ | ❌ | 리소스 존재 확인 |

### 멱등성(Idempotency) 이해

```
멱등성 = 같은 요청을 여러 번 보내도 결과가 같은가?

GET  /users/me       → 항상 같은 결과 → 멱등 ✅
POST /users          → 매번 새 사용자 생성 → 멱등 ❌
PUT  /users/123      → 항상 같은 상태로 덮어씀 → 멱등 ✅
DELETE /users/123    → 이미 삭제되었으면 404지만 상태 동일 → 멱등 ✅

실무 중요성:
  네트워크 오류로 재시도할 때,
  멱등한 메서드는 안전하게 재시도 가능!
```

---

## HTTP 상태 코드

### 카테고리별 정리

```
1xx 정보 (Informational)
├── 100 Continue          ← 큰 바디 전송 전 확인
└── 101 Switching         ← WebSocket 업그레이드

2xx 성공 (Success)
├── 200 OK               ← 일반 성공
├── 201 Created          ← 리소스 생성 성공 (POST)
├── 202 Accepted         ← 비동기 처리 접수됨
└── 204 No Content       ← 성공했지만 응답 본문 없음 (DELETE)

3xx 리다이렉트 (Redirection)
├── 301 Moved Permanently    ← URL 영구 변경 (SEO 반영)
├── 302 Found               ← 임시 리다이렉트
├── 304 Not Modified        ← 캐시 사용 (ETag/Last-Modified)
└── 307 Temporary Redirect  ← 메서드 유지 리다이렉트

4xx 클라이언트 오류 (Client Error)
├── 400 Bad Request         ← 잘못된 요청 형식
├── 401 Unauthorized        ← 인증 필요 (토큰 없음/만료)
├── 403 Forbidden           ← 권한 없음 (RBAC 부족)
├── 404 Not Found           ← 리소스 없음
├── 405 Method Not Allowed  ← 허용 안 된 메서드
├── 409 Conflict            ← 중복 (이미 존재하는 리소스)
├── 422 Unprocessable       ← 유효성 검증 실패
└── 429 Too Many Requests   ← 속도 제한 (Rate Limiting)

5xx 서버 오류 (Server Error)
├── 500 Internal Error     ← 서버 코드 오류
├── 502 Bad Gateway        ← 리버스 프록시/게이트웨이 오류
├── 503 Service Unavailable ← 서비스 점검/과부하
└── 504 Gateway Timeout    ← 업스트림 서버 응답 지연
```

### 실무에서 자주 보는 상태 코드

```python
# 401: 토큰 만료 → 재인증 필요
raise HTTPException(status_code=401, detail="Token expired")

# 404: 리소스 없음
raise HTTPException(status_code=404, detail="User not found")

# 409: 중복 리소스
raise HTTPException(status_code=409, detail="Email already exists")

# 429: Rate Limit 초과 → Retry-After 헤더 확인 후 재시도
# 422: 입력값 검증 실패 → 필드별 에러 메시지 반환
```

---

## HTTP 헤더 심화

### 주요 요청 헤더

```
Authorization: Bearer eyJhbGci...     ← JWT 토큰
Content-Type: application/json         ← 요청 바디 형식
Accept: application/json               ← 원하는 응답 형식
Origin: https://www.example.com        ← CORS 요청 출처
User-Agent: Chrome/120.0               ← 클라이언트 정보
Cache-Control: no-cache                ← 캐시 정책
```

### 주요 응답 헤더

```
Content-Type: application/json                         ← 응답 형식
Access-Control-Allow-Origin: https://www.example.com   ← CORS 허용
X-Request-ID: abc-123-def                               ← 요청 추적 ID
Retry-After: 5                                          ← 재시도 대기 (초)
Cache-Control: max-age=3600                             ← 1시간 캐시
ETag: "v1-abc123"                                       ← 리소스 버전
```

---

## 면접 핵심 포인트

```
Q: HTTP/1.1과 HTTP/2의 가장 큰 차이?
A: 멀티플렉싱. HTTP/1.1은 요청당 순차 처리(HOL Blocking),
   HTTP/2는 하나의 TCP 연결에서 여러 스트림 동시 처리.

Q: HTTP/3가 UDP를 쓰면 신뢰성은 어떻게?
A: QUIC 프로토콜이 UDP 위에 재전송, 순서 보장을 자체 구현.
   TCP 수준의 신뢰성 + 스트림별 독립성 확보.

Q: PUT vs PATCH 차이?
A: PUT은 리소스 전체를 교체(멱등), PATCH는 일부 필드만 수정.
   PUT은 미전달 필드가 초기화될 수 있음.

Q: 쿠키 vs Authorization 헤더?
A: 쿠키: 브라우저 자동 포함 → CSRF 취약
   Authorization: 직접 설정 → CSRF 안전 (API 서버 권장)
```
