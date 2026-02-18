# REST API 설계 원칙

---

## REST란?

```
REST = Representational State Transfer
Roy Fielding이 2000년 박사 논문에서 제안한 아키텍처 스타일

6가지 제약 조건:
1. Client-Server        클라이언트와 서버 분리
2. Stateless            서버가 클라이언트 상태를 저장하지 않음
3. Cacheable            응답은 캐시 가능해야 함
4. Uniform Interface    일관된 인터페이스
5. Layered System       계층화 (프록시, 로드밸런서 추가 가능)
6. Code on Demand       (선택) 서버가 클라이언트에 코드 전송
```

### RESTful API 설계 원칙

```
핵심 원칙:
1. 리소스 중심 (명사 URL)       /users, /orders
2. HTTP 메서드로 행위 표현       GET (조회), POST (생성), PUT/PATCH (수정), DELETE (삭제)
3. 무상태 (Stateless)           매 요청이 독립적 (토큰 포함)
4. 일관된 인터페이스             JSON 응답, 표준 상태 코드
5. 계층적 리소스 표현            /users/{id}/orders
```

---

## URL 설계 Best Practice

### Good vs Bad

```
❓ Bad (동사 사용, 비일관적)         ✅ Good (명사, 일관적)
GET  /getUser                       GET    /api/v1/users/me
POST /createOrder                   POST   /api/v1/orders
GET  /getOrderList                  GET    /api/v1/orders
POST /deleteProduct/123             DELETE /api/v1/products/123
GET  /user_info                     GET    /api/v1/users/{id}
```

### URL 설계 규칙

```
1. 복수형 사용:     /users (O)      /user (X)
2. 소문자 사용:     /orders         /Orders (X)
3. 하이픈 사용:     /order-items    /order_items (X)
4. 버전 포함:       /api/v1/...     /api/... (X)
5. 계층적 표현:     /users/{id}/orders
6. 필터는 쿼리:     /products?category=electronics&page=1
7. 파일 확장자 X:   /users (O)      /users.json (X)
```

### 페이지네이션, 필터링, 정렬

```
# 페이지네이션
GET /api/v1/products?page=2&limit=20

# 필터링
GET /api/v1/products?category=electronics&in_stock=true

# 정렬
GET /api/v1/products?sort=price&order=asc

# 검색
GET /api/v1/products?search=노트북

# 응답 형식
{
    "data": [...],
    "pagination": {
        "page": 2,
        "limit": 20,
        "total": 158,
        "total_pages": 8
    }
}
```

---

## REST API 설계 예시 (쿼머스)

```
GET    /api/v1/users/me               ← 내 정보 조회
POST   /api/v1/users                  ← 회원가입
PATCH  /api/v1/users/{id}             ← 회원정보 수정

GET    /api/v1/products               ← 상품 목록
GET    /api/v1/products/{id}          ← 상품 상세

POST   /api/v1/orders                 ← 주문 생성
GET    /api/v1/orders/{id}            ← 주문 상세
GET    /api/v1/users/{id}/orders      ← 사용자의 주문 목록
```

### 응답 표준화

```python
# 성공 응답
{
    "data": { "user_id": "123", "name": "홍길동" },
    "message": "OK"
}

# 에러 응답
{
    "detail": "User not found",
    "status_code": 404,
    "error_code": "USER_NOT_FOUND"  # 프론트엔드가 분기 가능
}
```

---

## 웹소켓 vs HTTP vs SSE

| 방식 | 방향 | 연결 | 프로토콜 | 사용 시나리오 |
|------|------|------|---------|-------------|
| **HTTP** | 요청→응답 (단방향) | 매번 / Keep-Alive | HTTP | REST API, 일반 웹 |
| **SSE** | 서버→클라이언트 (단방향) | 유지 | HTTP | 실시간 알림, 주가 |
| **WebSocket** | **양방향** | 유지 | WS/WSS | 채팅, 게임, 실시간 협업 |

### 각 방식 상세

```
HTTP 폴링 (Polling):
클라이언트: GET /api/status  (5초마다 반복)
  → 변경 없으면 200 + 같은 데이터 (낭비)
  → 변경 있으면 200 + 새 데이터

SSE (Server-Sent Events):
서버가 이벤트를 계속 푸시 (text/event-stream)
  data: {"status": "active"}\n\n
  data: {"status": "warning"}\n\n
  → 단방향이라 HTTP/2와 호환 좋음

WebSocket:
최초 HTTP 요청으로 업그레이드
  GET / HTTP/1.1
  Upgrade: websocket
  → 이후 양방향 바이너리/텍스트 프레임 교환
  → 채팅, 실시간 대시보드에 적합
```

### 선택 가이드

```
API 호출 → HTTP (REST)
실시간 알림 (서버→클라) → SSE
실시간 양방향 → WebSocket
변경 빈도 낮음 → HTTP 폴링 (간단)
변경 빈도 높음 → WebSocket 또는 SSE
```

---

## 면접 핵심 포인트

```
Q: REST의 Stateless란?
A: 서버가 클라이언트 상태를 저장하지 않음.
   매 요청에 인증 정보(JWT)를 포함해야 함.
   → 수평 스케일링이 쉬움 (어떤 서버가 받아도 처리 가능)

Q: RESTful API vs GraphQL?
A: REST: 리소스 기반, 다수 엔드포인트, Over/Under-fetching 가능
   GraphQL: 쿼리 기반, 단일 엔드포인트, 정확히 필요한 데이터만

Q: WebSocket vs SSE 차이?
A: WebSocket은 양방향, SSE는 서버→클라 단방향.
   SSE는 HTTP 기반이라 기존 인프라 호환 쉬움.
   WebSocket은 별도 프로토콜(ws://)

Q: API 버전 관리 방법?
A: URL (/api/v1/), 헤더 (Accept: v2), 쿼리 (?version=2)
   → URL 방식이 가장 명확하고 많이 사용
```
