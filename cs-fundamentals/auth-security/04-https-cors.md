# HTTPS & CORS 보안

---

## HTTPS 인증서 체인

```
인증서 검증 과정:

Root CA (DigiCert, Let's Encrypt Root)
  │  서명
  ├── Intermediate CA (Let's Encrypt R3)
  │     │  서명
  │     └── Server Certificate
  │           Domain: www.example.com
  │           Public Key: RSA 2048-bit
  │           Valid: 2024-01-01 ~ 2025-01-01
  │
  └── 브라우저/OS에 Root CA 내장 → 신뢰 체인 완성

검증 순서:
  1. 서버 인증서의 서명을 Intermediate CA 공개키로 검증
  2. Intermediate 인증서의 서명을 Root CA 공개키로 검증
  3. Root CA가 신뢰 저장소에 있는지 확인
  → 전체 체인이 유효하면 신뢰!
```

### 인증서 유형

| 유형 | 검증 수준 | 비용 | 특징 |
|------|----------|------|------|
| DV (Domain Validation) | 도메인 소유만 | 무료~저가 | Let's Encrypt |
| OV (Organization Validation) | 조직 실재 확인 | 중간 | 기업 웹사이트 |
| EV (Extended Validation) | 법적 실체 확인 | 고가 | 은행, 결제 |

---

## TLS Handshake (복습)

```
TLS 1.3 Handshake (1-RTT):

클라이언트                         서버
    │── ClientHello ────────→│
    │   지원 cipher 목록          │
    │   키 교환 파라미터          │
    │                            │
    │← ServerHello ─────────│
    │   선택된 cipher             │
    │   인증서                    │
    │   서명                      │
    │                            │
    │── Finished ───────────→│
    │   (이후 암호화 통신)         │

TLS 1.2: 2-RTT (한 번 더 왕복)
TLS 1.3: 1-RTT (더 빠름, 더 안전)
```

---

## CORS (Cross-Origin Resource Sharing) 심화

### 왜 CORS가 보안 메커니즘인가?

```
SOP (Same-Origin Policy) = 브라우저 보안의 근본

Origin = Protocol + Domain + Port
https://shop.com:443  ← 하나의 Origin

Same Origin:
  https://shop.com/page1 → https://shop.com/api  ✅ (같은 Origin)

Cross Origin:
  https://shop.com → https://api.shop.com  ❌ (다른 도메인)
  https://shop.com → http://shop.com       ❌ (다른 프로토콜)
  http://shop.com:80 → http://shop.com:8080 ❌ (다른 포트)

XSS 공격자가 CORS 없이 할 수 있는 것:
  악성 스크립트: fetch("https://bank.com/api/balance")
  → CORS가 없다면 → 잔액 정보를 공격자 서버로 전송!
  → CORS가 있으면 → bank.com이 악성 Origin을 허용하지 않으면 차단!

주의: CORS는 요청 자체를 막지 않음!
      서버는 요청을 처리할 수 있음
      브라우저가 응답을 JS에 전달하는 것을 차단
```

### Preflight 요청 (OPTIONS)

```
Simple Request가 아닌 경우 Preflight 발생:

Simple Request 조건 (Preflight 없음):
  - GET, HEAD, POST 중 하나
  - Content-Type이 text/plain, multipart/form-data, application/x-www-form-urlencoded 중 하나
  - 커스텀 헤더 없음

Preflight 필요 (대부분의 API 요청):
  - PUT, DELETE, PATCH
  - Content-Type: application/json  ← 이것만으로도 Preflight!
  - Authorization 헤더 포함

Preflight 과정:
  브라우저: OPTIONS /api/users (자동 전송)
           Origin: https://shop.com
           Access-Control-Request-Method: POST
           Access-Control-Request-Headers: Authorization, Content-Type
  
  서버:    200 OK
           Access-Control-Allow-Origin: https://shop.com
           Access-Control-Allow-Methods: GET, POST, PUT
           Access-Control-Allow-Headers: Authorization, Content-Type
           Access-Control-Max-Age: 3600  ← 1시간 캐시!
  
  브라우저: 허용 확인 → 실제 POST /api/users 전송
```

### CORS 설정 실수와 대응

```python
# ❌ 실수 1: 전체 허용
allow_origins=["*"]                 # 모든 출처 허용 → 위험!
allow_credentials=True              # + 인증 포함 → 에러!
# → *와 credentials 동시 사용 불가!

# ❌ 실수 2: Origin 반사 (Reflect)
# 요청의 Origin을 그대로 Allow-Origin에 넣음
# → 사실상 전체 허용과 동일!

# ✅ 올바른 설정
from fastapi.middleware.cors import CORSMiddleware

app.add_middleware(
    CORSMiddleware,
    allow_origins=[
        "https://shop.com",
        "https://admin.shop.com",
    ],
    allow_credentials=True,
    allow_methods=["GET", "POST", "PUT", "DELETE"],
    allow_headers=["Authorization", "Content-Type"],
    max_age=3600,  # Preflight 캐시 1시간
)
```

---

## 보안 헤더

```
주요 보안 응답 헤더:

X-Content-Type-Options: nosniff
  → MIME 타입 스니핑 방지

X-Frame-Options: DENY
  → iframe 삽입 방지 (Clickjacking 차단)

Content-Security-Policy: default-src 'self'; script-src 'self'
  → 인라인 스크립트, 외부 스크립트 실행 차단 (XSS 방지)

Strict-Transport-Security: max-age=31536000; includeSubDomains
  → HSTS: 브라우저가 항상 HTTPS로만 접근하도록 강제

Referrer-Policy: strict-origin-when-cross-origin
  → 다른 사이트로 이동 시 URL 정보 제한
```

---

## 면접 핵심 포인트

```
Q: CORS는 서버를 보호하는가?
A: CORS는 서버가 아닌 "브라우저 사용자"를 보호.
   서버는 요청을 처리할 수 있으나, 브라우저가
   응답을 JS에 전달하는 것을 차단.
   서버 보호는 인증/인가로 해야 함.

Q: Preflight 요청이 발생하는 조건?
A: Content-Type이 application/json이거나,
   커스텀 헤더(Authorization)가 있거나,
   GET/HEAD/POST 외의 메서드 사용 시.
   Max-Age로 캐시하여 빈도 줄일 수 있음.

Q: HSTS란?
A: HTTP Strict Transport Security.
   브라우저에게 "이 사이트는 항상 HTTPS로만 접근하라"고 지시.
   HTTP 요청을 브라우저가 자동으로 HTTPS로 변환.
   첫 방문 시 MITM 가능 → Preload List로 해결.
```
