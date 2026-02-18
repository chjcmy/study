# JWT (JSON Web Token) 심층 분석

---

## 구조

```
eyJhbGciOiJSUzI1NiJ9.eyJzdWIiOiJ1c2VyMTIzIn0.signature
│                      │                         │
│   Header (헤더)       │   Payload (페이로드)     │   Signature (서명)

각 부분은 Base64URL 인코딩!
(암호화가 아님 → 누구나 디코딩 가능 → 민감 정보 넣지 말 것!)
```

### Header

```json
{
  "alg": "RS256",       // 서명 알고리즘
  "typ": "JWT",
  "kid": "key-id-123"   // 서명에 사용된 키 식별자
}

alg 종류:
  HS256: HMAC + SHA256 (대칭키 — 같은 키로 서명/검증)
  RS256: RSA + SHA256 (비대칭키 — 개인키로 서명, 공개키로 검증) ⭐
  ES256: ECDSA + SHA256 (타원곡선 — 짧은 키, 빠름)

단일 서버: HS256 (간단, 빠름)
MSA/분산 시스템: RS256 (공개키 공유만으로 검증 가능) ⭐
```

### Payload (클레임)

```json
{
  // ── 등록된 클레임 (RFC 7519 표준) ──
  "sub": "user123",                // 주체 (사용자 식별자)
  "iss": "auth.example.com",      // 발급자 (누가 만들었나)
  "aud": "api.example.com",       // 대상 (누구를 위한 토큰인가)
  "iat": 1706000000,              // 발급 시간 (issued at)
  "exp": 1706003600,              // 만료 시간 (1시간)
  "nbf": 1706000000,              // 이전 사용 불가 (not before)
  "jti": "unique-token-id",       // 토큰 고유 ID (재사용 방지)

  // ── 커스텀 클레임 (서비스별 정의) ──
  "roles": ["admin"],             // 역할
  "name": "홍길동",                // 사용자 이름
  "email": "user@example.com"     // 이메일
}
```

### Signature

```
서명 = 알고리즘(Base64(Header) + "." + Base64(Payload), Key)

HS256 (대칭키):
  서명 = HMAC_SHA256(header.payload, SecretKey)
  검증 = HMAC_SHA256(header.payload, SecretKey) == Signature?
  → 같은 키로 서명/검증 → 키 유출 시 위조 가능

RS256 (비대칭키):
  서명 = RSA_SHA256(header.payload, PrivateKey)   ← 비밀!
  검증 = RSA_VERIFY(header.payload, Signature, PublicKey) ← 공개
  → 공개키로 검증만 가능, 위조 불가

→ Header나 Payload를 1비트라도 변경하면 서명 불일치!
→ 위변조 불가능!
```

---

## JWT 검증 순서

```
JWT 수신 시 서버가 수행하는 검증 (순서 중요!):

1. 형식 확인
   header.payload.signature 3파트인지

2. 알고리즘 확인
   Header의 alg이 허용 목록에 있는지 (none 공격 방지!)

3. 서명 검증 ⭐
   비밀키/공개키로 서명 검증 → 위변조 여부 확인

4. 만료 시간 (exp) 확인
   exp > 현재 시간? → 만료된 토큰 거부

5. nbf 확인
   현재 시간 > nbf? → 아직 유효하지 않은 토큰 거부

6. 발급자 (iss) 확인
   신뢰할 수 있는 발급자인가?

7. 대상 (aud) 확인
   이 토큰이 내 서비스를 위한 것인가?
```

```python
# Python 구현 예시 (PyJWT)
import jwt

SECRET_KEY = "your-secret-key"

# 토큰 생성
def create_token(user_id: str, roles: list[str]) -> str:
    payload = {
        "sub": user_id,
        "roles": roles,
        "iat": datetime.utcnow(),
        "exp": datetime.utcnow() + timedelta(hours=1),
        "iss": "auth.example.com",
    }
    return jwt.encode(payload, SECRET_KEY, algorithm="HS256")

# 토큰 검증
def verify_token(token: str) -> dict:
    try:
        payload = jwt.decode(
            token,
            SECRET_KEY,
            algorithms=["HS256"],         # 허용 알고리즘 제한
            issuer="auth.example.com",    # iss 확인
        )
        return payload
    except jwt.ExpiredSignatureError:
        raise AuthError("토큰 만료")
    except jwt.InvalidTokenError:
        raise AuthError("유효하지 않은 토큰")
```

---

## Access Token vs Refresh Token

```
Access Token:  짧은 수명 (15분~1시간), API 요청에 사용
Refresh Token: 긴 수명 (7~90일), Access Token 갱신에 사용

왜 분리하는가?
  Access Token 유출 시 → 짧은 수명이라 피해 제한
  Refresh Token 유출 시 → 서버에서 즉시 무효화 가능

사용자 ────→ 인증 서버: 로그인
            ←── Access Token (1시간) + Refresh Token (30일)
            
사용자 ────→ API 서버: GET /api/orders (Access Token)
            ←── 200 OK + 데이터

... 1시간 후 ...

사용자 ────→ API 서버: GET /api/orders (만료된 Access Token)
            ←── 401 Unauthorized

사용자 ────→ 인증 서버: 토큰 갱신 (Refresh Token)
            ←── 새 Access Token + 새 Refresh Token
```

### Refresh Token Rotation

```
보안 강화: Refresh Token 사용 시 새 것을 발급하고 기존 것을 무효화

RT-1 사용 → 새 AT + RT-2 발급 → RT-1 무효화
RT-2 사용 → 새 AT + RT-3 발급 → RT-2 무효화

만약 RT-1이 유출되어 공격자가 사용하면?
→ RT-1은 이미 무효 → 거부됨!
→ 더 안전하게: 기존 RT 재사용 감지 시 전체 로그아웃 강제
```

---

## JWT 보안 주의사항

```
1. ❌ Payload에 민감 정보 넣지 말 것 (Base64 = 디코딩 가능)
2. ❌ alg: "none" 허용하지 말 것 (서명 우회 공격)
3. ✅ 항상 서명 검증
4. ✅ 만료 시간 짧게 (1시간 이내)
5. ✅ aud, iss 검증 필수
6. ✅ HTTPS 필수 (토큰 평문 전송 방지)
7. ✅ 적절한 저장 위치 선택 (아래 참조)
```

---

## 면접 핵심 포인트

```
Q: JWT의 장단점?
A: 장점: 무상태(스케일링), 서버 저장 불필요, 정보 자체 포함
   단점: 즉시 무효화 어려움, 크기가 큼, Payload 노출
   → 즉시 무효화 필요시 블랙리스트(Redis) 사용

Q: HS256 vs RS256?
A: HS256: 대칭키 (같은 키로 서명+검증, 빠름, 단일 서비스)
   RS256: 비대칭키 (개인키 서명, 공개키 검증, MSA 적합)
   → MSA에서 RS256: 각 서버가 공개키만으로 검증 가능

Q: JWT를 어디에 저장해야 하나?
A: 웹: httpOnly + Secure + SameSite 쿠키 (XSS 방지)
   SPA: 메모리 (가장 안전) + Refresh Token Rotation
   모바일: 안전한 저장소 (Keychain, Keystore)
   → localStorage는 XSS에 취약 → 비권장
```
