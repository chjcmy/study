# Protocol Selection

프로토콜 선택은 “무엇이 더 좋은가”보다 “이 데이터 흐름에 무엇이 맞는가”의 문제다.

먼저 reliability, latency, topology, data model, operation cost를 본다.

## 먼저 잡을 기준

| 기준 | 질문 |
|---|---|
| 신뢰성 | 유실되면 안 되는가, 재전송이 필요한가 |
| 지연 시간 | ms 단위 반응이 필요한가, 초 단위 수집이면 충분한가 |
| 연결 구조 | 1:1 요청/응답인가, pub/sub인가, 양방향 연결인가 |
| 데이터 모델 | 단순 byte인가, JSON 객체인가, schema가 강한가, register/tag인가 |
| 운영 비용 | broker, schema, 인증서, 장애 대응을 감당할 수 있는가 |

## 빠른 선택표

| 상황 | 우선 후보 |
|---|---|
| 일반 backend API | HTTP / REST |
| 실시간 화면 업데이트 | WebSocket |
| IoT 센서 publish/subscribe | MQTT |
| 내부 서비스 간 고성능 RPC | gRPC |
| 산업 장비 register 읽기 | Modbus |
| 산업 데이터 모델 표준화 | OPC UA |
| 단순하고 빠른 전송, 일부 유실 허용 | UDP |
| 순서와 재전송이 필요한 기본 연결 | TCP |

## TCP / UDP

TCP와 UDP는 상위 프로토콜의 기반이다.

- TCP: 연결 기반, 순서 보장, 재전송, 흐름 제어
- UDP: 연결 부담이 작고 빠르지만 유실과 순서 문제를 직접 다뤄야 함

선택 기준:

```text
데이터 유실을 애플리케이션이 감당할 수 있는가?
순서 보장이 필요한가?
재전송 비용이 latency보다 중요한가?
```

대부분의 업무 시스템은 TCP 기반 프로토콜을 먼저 검토한다. UDP는 실시간성이나 단순 전송이 더 중요한 경우에 제한적으로 본다.

## HTTP / REST

HTTP는 요청/응답 기반 backend API에 적합하다.

좋은 경우:

- gateway가 backend로 batch를 올린다.
- client가 명확한 API endpoint를 호출한다.
- status code, timeout, retry 정책을 단순하게 가져가고 싶다.

주의할 점:

- 지속적인 실시간 stream에는 비효율적일 수 있다.
- polling을 많이 하면 서버와 네트워크 비용이 커진다.

## WebSocket

WebSocket은 연결을 유지하면서 양방향 메시지를 주고받을 때 쓴다.

좋은 경우:

- dashboard 상태를 실시간으로 갱신한다.
- 서버가 client에 즉시 push해야 한다.
- 요청/응답보다 연결 유지가 자연스럽다.

주의할 점:

- 연결 수 관리가 필요하다.
- 재연결, heartbeat, 세션 정리 정책이 필요하다.

## MQTT

MQTT는 broker를 중심으로 topic에 publish/subscribe하는 구조다.

좋은 경우:

- 센서나 gateway가 데이터를 계속 publish한다.
- 소비자가 여러 개일 수 있다.
- 장비가 backend endpoint를 직접 몰라도 된다.
- QoS 정책이 필요하다.

주의할 점:

- broker 운영이 필요하다.
- topic 설계가 흐트러지면 데이터 흐름을 추적하기 어렵다.

## gRPC

gRPC는 HTTP/2와 proto schema 기반 RPC다.

좋은 경우:

- 내부 서비스 간 contract를 강하게 가져가고 싶다.
- binary payload와 streaming이 필요하다.
- latency와 처리량이 중요하다.

주의할 점:

- browser나 외부 공개 API에는 REST보다 불편할 수 있다.
- proto 관리와 client code generation이 필요하다.

## Modbus

Modbus는 산업 장비에서 register를 읽고 쓰는 데 자주 쓰인다.

좋은 경우:

- PLC나 장비의 register 값을 직접 읽는다.
- 데이터 모델이 단순 주소와 값 중심이다.
- 현장 장비가 Modbus만 지원한다.

주의할 점:

- 값의 의미, 단위, scale은 별도로 관리해야 한다.
- 보안 기능이 약하므로 망 분리나 gateway 보호가 중요하다.

## OPC UA

OPC UA는 산업 데이터를 tag, node, type, 관계로 구조화해 다루기 좋다.

좋은 경우:

- 장비 데이터를 의미 있는 모델로 보고 싶다.
- 여러 장비와 시스템을 표준 방식으로 연결하고 싶다.
- 보안, 인증, 구독 기반 수집이 필요하다.

주의할 점:

- Modbus보다 복잡하다.
- server 정보 모델 설계와 인증서 운영이 필요하다.

## 판단 순서

프로토콜을 고를 때는 아래 순서로 좁히면 된다.

```text
1. 장비나 상대 시스템이 이미 지원하는 프로토콜이 있는가?
2. 데이터 유실을 허용할 수 있는가?
3. 요청/응답, stream, pub/sub 중 어떤 topology인가?
4. 데이터가 단순 값인가, schema가 필요한 객체인가, 산업 tag 모델인가?
5. broker, 인증서, schema, client 생성 비용을 운영할 수 있는가?
```

핵심은 기능보다 운영 가능성이다. 좋은 프로토콜도 팀이 안정적으로 운영하지 못하면 시스템 전체의 장애 지점이 된다.

