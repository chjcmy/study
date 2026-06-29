# MQTT

MQTT는 IoT 환경에서 자주 쓰는 가벼운 pub/sub 메시징 프로토콜이다.

핵심은 장비나 센서가 backend API를 직접 호출하는 구조가 아니라, broker를 중심으로 message를 발행하고 구독하는 구조라는 점이다.

```text
Sensor / Device
-> publish
-> MQTT Broker
-> subscribe
-> Edge gateway / Backend / Monitoring
```

---

## 왜 중요한가

IoT, Edge, 제조 환경에서는 작은 장비가 많고 네트워크가 항상 안정적이지 않다. 모든 장비가 backend endpoint를 직접 알고 HTTP 요청을 보내면 연결 관리, 재시도, fan-out이 복잡해진다.

MQTT는 이런 상황에서 장비와 소비자를 느슨하게 분리한다.

| 관점 | 의미 |
|---|---|
| IoT | 센서가 작은 메시지를 지속적으로 전송 |
| Edge | gateway가 현장 데이터를 모아 broker와 backend 사이를 중계 |
| Backend | 여러 소비자가 같은 topic을 구독해 저장, 알림, 분석 처리 |

---

## 핵심 개념

| 개념 | 설명 |
|---|---|
| Broker | 메시지를 받아 topic 기준으로 구독자에게 전달하는 서버 |
| Client | publish 또는 subscribe를 수행하는 장비/서비스 |
| Topic | 메시지의 주소 역할을 하는 문자열 |
| Publish | 특정 topic으로 메시지를 발행 |
| Subscribe | 특정 topic의 메시지를 구독 |
| QoS | 메시지 전달 보장 수준 |
| Retained Message | topic의 마지막 메시지를 broker가 보관 |
| Last Will | client가 비정상 종료되면 broker가 대신 발행하는 메시지 |

Topic 예시:

```text
factory/line-1/machine-7/temperature
factory/line-1/machine-7/status
factory/line-2/+/status
```

---

## QoS

QoS는 "메시지를 어디까지 보장할 것인가"를 정한다.

| QoS | 의미 | 특징 |
|---|---|---|
| 0 | At most once | 빠르지만 유실 가능 |
| 1 | At least once | 유실은 줄지만 중복 가능 |
| 2 | Exactly once | 가장 강하지만 비용 큼 |

실무에서는 QoS 1을 많이 본다. 단, QoS 1은 중복 가능성이 있으므로 consumer 쪽에서 idempotency나 dedupe를 고려해야 한다.

---

## 언제 쓰는가

MQTT가 잘 맞는 경우:

- 장비나 센서 수가 많다.
- 작은 telemetry 메시지를 자주 보낸다.
- 하나의 데이터가 여러 소비자에게 전달되어야 한다.
- 장비가 backend API 구조를 직접 몰라도 된다.
- 네트워크가 불안정하고 재연결을 전제로 해야 한다.

HTTP가 더 단순한 경우:

- 요청/응답 API만 있으면 충분하다.
- 메시지를 받을 소비자가 하나뿐이다.
- 운영팀이 broker를 추가로 관리하기 어렵다.
- 기존 backend ingestion endpoint가 명확하다.

---

## 실패와 운영 이슈

MQTT를 쓰면 broker가 중심 컴포넌트가 된다. 따라서 메시지 구조뿐 아니라 broker 운영도 같이 봐야 한다.

| 이슈 | 봐야 할 질문 |
|---|---|
| 연결 끊김 | client가 언제 재연결하는가 |
| 중복 메시지 | consumer가 같은 메시지를 여러 번 받아도 안전한가 |
| 메시지 유실 | QoS 0을 써도 되는 데이터인가 |
| broker 장애 | 단일 broker 장애를 어떻게 처리하는가 |
| topic 설계 | topic이 너무 세밀하거나 너무 넓지 않은가 |
| 인증/권한 | client별 publish/subscribe 권한을 나눴는가 |
| backlog | consumer가 느릴 때 메시지가 얼마나 쌓이는가 |

---

## Backend 관점에서 보기

MQTT 자체는 저장소가 아니다. broker는 메시지를 전달하는 역할이고, 장기 저장, 검색, 통계, 알림 정책은 backend나 data platform이 맡는 경우가 많다.

```text
MQTT Broker
-> Consumer
-> Backend API / Stream Processor
-> Database
-> Dashboard / Alert
```

따라서 설계할 때는 "broker에 메시지가 도착했다"와 "backend에 안전하게 저장됐다"를 구분해야 한다.

---

## 비교 질문

```text
Q: MQTT와 HTTP의 가장 큰 차이는?
A: HTTP는 client가 server endpoint로 요청한다. MQTT는 client가 broker의 topic에 publish하고,
   구독자가 topic 기준으로 메시지를 받는다.

Q: MQTT가 WebSocket보다 IoT에 자주 언급되는 이유는?
A: MQTT는 topic, broker, QoS, retained message 같은 메시징 개념이 프로토콜에 포함되어 있다.
   WebSocket은 양방향 연결 통로에 가깝고, 메시징 규칙은 애플리케이션이 직접 설계해야 한다.

Q: QoS 2를 쓰면 항상 좋은가?
A: 아니다. 전달 보장은 강해지지만 handshake와 상태 관리 비용이 커진다.
   데이터 중요도와 처리량을 보고 선택해야 한다.
```

---

## 한 줄 정리

MQTT는 많은 장비가 작은 메시지를 보내고, 여러 소비자가 topic 기준으로 데이터를 받아야 할 때 유용한 IoT/Edge용 pub/sub 프로토콜이다.
