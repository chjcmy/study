# WebSocket

WebSocket은 client와 server가 하나의 연결을 유지하면서 양방향으로 메시지를 주고받는 프로토콜이다.

HTTP처럼 요청할 때마다 응답을 받는 구조가 아니라, 연결을 열어 둔 뒤 server도 필요할 때 client로 데이터를 보낼 수 있다.

```text
Client
-> HTTP Upgrade
-> WebSocket Connection
<-> Server
```

---

## 왜 중요한가

Backend 시스템에서 모든 통신이 요청/응답으로 끝나지는 않는다. 상태 변화, 알림, 실시간 대시보드, 작업 진행률처럼 server가 client에게 먼저 알려야 하는 경우가 있다.

WebSocket은 이런 "연결 유지 기반 실시간 통신"을 이해하는 핵심 프로토콜이다.

| 관점 | 의미 |
|---|---|
| IoT | gateway나 dashboard가 장비 상태를 실시간으로 표시 |
| Edge | 현장 상태 변화나 제어 결과를 빠르게 전달 |
| Backend | 알림, 진행률, 협업 화면, live monitoring 제공 |

---

## 핵심 개념

| 개념 | 설명 |
|---|---|
| HTTP Upgrade | 처음에는 HTTP로 시작하고 WebSocket 연결로 전환 |
| Persistent Connection | 연결을 계속 유지 |
| Full-duplex | client와 server가 동시에 양방향 전송 가능 |
| Frame | WebSocket에서 데이터를 나누어 보내는 단위 |
| Ping/Pong | 연결 생존 여부를 확인하는 heartbeat |
| Close Handshake | 연결 종료를 서로 확인하는 절차 |

HTTP 상태 코드에서 `101 Switching Protocols`는 WebSocket 전환과 관련이 있다.

```text
HTTP request
-> 101 Switching Protocols
-> WebSocket frames
```

---

## 언제 쓰는가

WebSocket이 잘 맞는 경우:

- server가 client에게 먼저 이벤트를 보내야 한다.
- 화면 상태를 낮은 지연으로 갱신해야 한다.
- polling을 자주 하느라 비용이 커진다.
- 연결된 사용자별 session 상태를 유지해야 한다.

HTTP polling이 더 단순한 경우:

- 몇 초 또는 몇 분 단위 갱신이면 충분하다.
- client가 필요할 때만 조회하면 된다.
- 연결 유지 비용을 줄이고 싶다.
- 서버 인프라가 stateless HTTP에 최적화되어 있다.

---

## WebSocket vs Polling

| 기준 | Polling | WebSocket |
|---|---|---|
| 연결 | 요청마다 사용 | 연결 유지 |
| server push | 불가능 | 가능 |
| 지연 | polling 주기에 따라 증가 | 낮음 |
| 구현 | 단순 | 연결 상태 관리 필요 |
| 운영 | HTTP 인프라와 잘 맞음 | connection 수, heartbeat 관리 필요 |

Polling은 단순하지만 불필요한 요청이 많아질 수 있다. WebSocket은 실시간성은 좋지만, 연결을 오래 유지하는 운영 부담이 생긴다.

---

## 실패와 운영 이슈

WebSocket은 "연결이 살아 있다"는 전제가 중요하다. 그래서 메시지 포맷보다 연결 수명 관리가 더 큰 문제가 되는 경우가 많다.

| 이슈 | 봐야 할 질문 |
|---|---|
| 연결 끊김 | client가 언제, 어떤 backoff로 재연결하는가 |
| heartbeat | ping/pong 주기를 어떻게 정하는가 |
| 메시지 유실 | 끊긴 동안 발생한 이벤트를 복구할 수 있는가 |
| 순서 보장 | 재연결 후 이전 메시지와 새 메시지 순서가 깨지지 않는가 |
| 확장성 | 서버 한 대가 유지할 수 있는 connection 수는 얼마인가 |
| 로드밸런싱 | sticky session이 필요한가 |
| 인증 만료 | 연결 중 token이 만료되면 어떻게 처리하는가 |

---

## Backend 관점에서 보기

WebSocket은 실시간 통로이지, 메시지 저장소나 작업 큐가 아니다. 연결 중인 client에게 빠르게 전달하는 데는 좋지만, client가 오프라인인 동안의 이벤트 보관은 별도 설계가 필요하다.

```text
Domain Event
-> Backend
-> WebSocket Session
-> Browser / Dashboard
```

중요 이벤트라면 WebSocket으로 보내는 것과 별개로 database나 message queue에 기록해야 한다.

---

## 비교 질문

```text
Q: WebSocket과 HTTP의 가장 큰 차이는?
A: HTTP는 기본적으로 요청/응답 모델이다. WebSocket은 연결을 유지하고 server도 client에게
   먼저 메시지를 보낼 수 있다.

Q: WebSocket과 MQTT는 어떻게 다른가?
A: WebSocket은 양방향 연결 통로에 가깝다. MQTT는 broker, topic, QoS를 가진 pub/sub 메시징
   프로토콜이다. WebSocket 위에 직접 메시징 규칙을 만들 수도 있지만, MQTT는 그 규칙이
   프로토콜 수준에 포함되어 있다.

Q: 실시간이면 무조건 WebSocket을 써야 하는가?
A: 아니다. 갱신 주기가 길거나 단순 조회면 polling이 더 낫다.
   server push가 필요하고 지연을 줄여야 할 때 WebSocket을 검토한다.
```

---

## 한 줄 정리

WebSocket은 server push와 낮은 지연이 필요한 화면, 알림, 모니터링에 적합한 연결 유지형 양방향 통신 프로토콜이다.
