# gRPC vs REST

---

## gRPC란?

```
Google이 개발한 고성능 RPC(Remote Procedure Call) 프레임워크

RPC = 원격 서버의 함수를 마치 로컬 함수처럼 호출

# REST 방식
response = httpx.get("https://api.example.com/users/123")
user = response.json()

# gRPC 방식 (마치 로컬 함수 호출처럼)
user = stub.GetUser(UserRequest(id=123))
```

---

## 핵심 비교

| 특성 | REST | gRPC |
|------|------|------|
| 프로토콜 | HTTP/1.1 (주로) | **HTTP/2** |
| 데이터 형식 | JSON (텍스트) | **Protocol Buffers (바이너리)** |
| 속도 | 보통 | **빠름** (직렬화 2~10배 빠름) |
| 페이로드 크기 | 큼 (JSON 키 포함) | **작음** (바이너리 + 필드 번호) |
| 스트리밍 | SSE만 (단방향) | **4가지** (양방향 포함) |
| 브라우저 | 직접 지원 | gRPC-Web 필요 |
| API 문서 | OpenAPI/Swagger | .proto 파일이 문서 |
| 사용처 | 웹 API, 외부 공개 | **내부 서비스 간 통신** |
| 학습 곡선 | 낮음 | 높음 |

### Protocol Buffers 예시

```protobuf
// user.proto — 스키마 정의
syntax = "proto3";

service UserService {
    rpc GetUser (UserRequest) returns (UserResponse);
    rpc ListUsers (Empty) returns (stream UserResponse);  // 서버 스트리밍
}

message UserRequest {
    int32 id = 1;        // 필드 번호로 식별 (키 이름 전송 X → 작음)
}

message UserResponse {
    int32 id = 1;
    string name = 2;
    string email = 3;
}
```

```
JSON:  {"id": 123, "name": "John", "email": "john@a.com"}  → 52 바이트
Proto: [바이너리]                                           → 23 바이트
                                                           → 56% 절감!
```

---

## gRPC 4가지 통신 패턴

```
1. Unary (1:1) — REST와 유사
   Client ──Request──→ Server
   Client ←─Response──  Server

2. Server Streaming (1:N) — SSE와 유사
   Client ──Request──→ Server
   Client ←─Response─  Server
   Client ←─Response─  Server
   Client ←─Response─  Server

3. Client Streaming (N:1)
   Client ──Request──→ Server
   Client ──Request──→ Server
   Client ──Request──→ Server
   Client ←─Response──  Server

4. Bidirectional Streaming (N:N) — WebSocket과 유사
   Client ←─→ Server (양방향 동시 스트리밍)
```

---

## 언제 뭘 쓸까?

```
REST를 쓸 때:
  ✅ 웹 브라우저 직접 호출 (Teams 앱 → FastAPI)
  ✅ 외부 공개 API (third-party 개발자)
  ✅ 간단한 CRUD
  ✅ 팀이 REST에 익숙할 때

gRPC를 쓸 때:
  ✅ 마이크로서비스 간 내부 통신
  ✅ 낮은 지연 시간이 중요할 때
  ✅ 양방향 스트리밍이 필요할 때
  ✅ 대용량 데이터 전송 (바이너리 효율)
  ✅ polyglot 환경 (여러 언어 서비스 → .proto로 코드 자동 생성)
```

> Log-Doctor: 외부 API(ARM, Teams)는 **REST**, 만약 내부 서비스가 추가되면 **gRPC**가 효율적

---

## 면접 핵심 포인트

```
Q: gRPC가 REST보다 빠른 이유?
A: 1. HTTP/2 (멀티플렉싱, 헤더 압축)
   2. Protocol Buffers (바이너리 직렬화, 작은 크기)
   3. 양방향 스트리밍 + 연결 재사용

Q: gRPC를 브라우저에서 못 쓰는 이유?
A: 브라우저가 HTTP/2 프레이밍을 직접 제어 불가.
   gRPC-Web (프록시) 또는 Envoy를 통해 변환 필요.

Q: REST와 gRPC를 함께 쓸 수 있나?
A: 네. 외부 API는 REST, 내부 MSA 통신은 gRPC.
   API Gateway에서 REST ↔ gRPC 변환도 가능.
```
