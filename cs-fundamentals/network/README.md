# 네트워크

데이터가 컴퓨터 간에 **어떻게 전달되는지** 이해하는 CS의 핵심 영역입니다.

log-friends 기준으로는 SDK가 대상 앱 안에서 eventType을 수집하고 Console의 `POST /ingest`로 보내는 경로를 이해하기 위한 기반이다.

---

## 학습 순서

1. 계층 구조: [OSI와 TCP/IP](./01-osi-tcp-ip.md)
2. 전송 방식: [TCP vs UDP](./02-tcp-vs-udp.md)
3. 웹 프로토콜: [HTTP](./03-http.md), [HTTPS와 TLS](./04-https-tls.md)
4. API 설계: [REST API](./05-rest-api.md), [CORS](./06-cors.md), [gRPC vs REST](./07-grpc.md)
5. 실행 환경: [localhost vs 0.0.0.0](./08-localhost.md)
6. 장애 대응: [Timeout, Retry, Backoff](./09-timeout-retry-backoff.md)
7. 성능 기본기: [Connection Reuse와 Pool](./10-connection-reuse-pool.md)

---

## 파일 목록

| # | 파일 | 핵심 내용 |
|---|------|----------|
| 1 | [OSI와 TCP/IP](./01-osi-tcp-ip.md) | OSI 7계층, TCP/IP 4계층, 백엔드 요청 흐름 매핑 |
| 2 | [TCP vs UDP](./02-tcp-vs-udp.md) | 3-way handshake, 신뢰성 비교 |
| 3 | [HTTP](./03-http.md) | 버전 비교, 메서드, 상태 코드 |
| 4 | [HTTPS와 TLS](./04-https-tls.md) | TLS Handshake, 대칭키/비대칭키, DNS |
| 5 | [REST API](./05-rest-api.md) | 설계 원칙, URL 설계, WebSocket/SSE 비교 |
| 6 | [CORS](./06-cors.md) | Cross-Origin, Preflight, Spring 설정 |
| 7 | [gRPC vs REST](./07-grpc.md) | 프로토콜 비교, 사용처 |
| 8 | [localhost vs 0.0.0.0](./08-localhost.md) | 루프백 주소, Docker 배포 |
| 9 | [Timeout, Retry, Backoff](./09-timeout-retry-backoff.md) | 네트워크 실패, 재시도, 중복 저장 위험 |
| 10 | [Connection Reuse와 Pool](./10-connection-reuse-pool.md) | Keep-Alive, 커넥션 재사용, `HttpClient` |

---

## log-friends 연결

| 네트워크 개념 | log-friends에서 보이는 곳 | 봐야 할 질문 |
|---|---|---|
| HTTP 요청/응답 | SDK `BatchTransporter` -> Console `POST /ingest` | 실패 시 SDK가 어떻게 재시도하거나 버리는가? |
| TCP 연결 | SDK의 `HttpClient` 전송 | flush마다 새 연결을 만들지 않는가? |
| Timeout | connect timeout, request timeout | Console 장애 때 대상 앱 스레드를 오래 붙잡지 않는가? |
| Retry | 전송 실패 후 queue 재삽입 | 서버 저장 성공 후 응답 유실이면 중복 Raw Event가 생기지 않는가? |
| 0.0.0.0/localhost | DSM Docker, Spring Boot 서버 바인딩 | 같은 LAN의 앱에서 Console에 접근 가능한가? |
| TLS | 운영 환경 HTTPS | workerId와 payload가 평문으로 노출되지 않는가? |
