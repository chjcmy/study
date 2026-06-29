# 다양한 통신 프로토콜

목표는 모든 프로토콜을 깊게 외우는 것이 아니다.

`다양한 통신 프로토콜`은 웹 백엔드 통신만 뜻하지 않는다. 제조, IoT, Edge 시스템에서는 설비, 센서, PLC, Edge gateway, cloud backend가 서로 데이터를 주고받는 전체 통신 흐름을 이해해야 한다.

```text
설비 / 센서 / PLC
-> Edge gateway
-> Middleware
-> Backend API
-> Data platform
-> Monitoring / AI Agent
```

## 먼저 잡을 기준

통신 프로토콜을 볼 때는 아래 4가지를 먼저 본다.

| 기준 | 질문 |
|---|---|
| 연결 방식 | 계속 연결되는가, 요청할 때만 연결되는가 |
| 데이터 단위 | 메시지인가, 스트림인가, 레지스터 값인가 |
| 신뢰성 | 유실되면 안 되는가, 일부 유실을 감수할 수 있는가 |
| 사용 위치 | 설비 쪽인가, backend 쪽인가, cloud 쪽인가 |

## 1차로 볼 프로토콜

### TCP / UDP

기본 전송 계층이다.

- TCP: 연결 기반, 순서 보장, 재전송
- UDP: 빠르지만 순서/전송 보장 약함

봐야 할 이유:

- Modbus TCP, HTTP, MQTT, gRPC는 결국 TCP 위에서 동작한다.
- 센서 데이터는 빠른 전송과 유실 허용 여부가 중요하다.

관련 파일:

- `../02-tcp-vs-udp.md`

### HTTP / REST

Backend API 기본 통신이다.

봐야 할 이유:

- Edge gateway가 backend로 데이터를 올릴 때 가장 이해하기 쉬운 방식이다.
- Log Friends의 `/ingest`도 HTTP POST 기반이다.
- 요청/응답, timeout, retry, status code를 이해해야 한다.

관련 파일:

- `../03-http.md`
- `../05-rest-api.md`
- `../09-timeout-retry-backoff.md`

### WebSocket

서버와 클라이언트가 연결을 유지하며 양방향 통신하는 방식이다.

봐야 할 이유:

- 설비 상태나 대시보드 실시간 업데이트에 쓰일 수 있다.
- polling과 streaming의 차이를 이해하는 데 좋다.

핵심 질문:

```text
실시간 화면 갱신이 필요한가?
요청/응답만으로 충분한가?
연결이 끊겼을 때 복구 정책은 무엇인가?
```

### MQTT

IoT에서 자주 쓰는 pub/sub 메시징 프로토콜이다.

봐야 할 이유:

- 센서/Edge 환경에서 가벼운 메시지 전송에 자주 등장한다.
- broker, topic, publish, subscribe 구조를 이해해야 한다.

핵심 질문:

```text
센서가 backend endpoint를 직접 알아야 하는가?
여러 소비자가 같은 데이터를 받을 수 있어야 하는가?
QoS를 어디까지 보장할 것인가?
```

### gRPC

HTTP/2 기반 RPC 통신이다.

봐야 할 이유:

- 내부 서비스 통신이나 고성능 API에서 쓰일 수 있다.
- proto contract, streaming, binary payload 개념을 익히기 좋다.

관련 파일:

- `../07-grpc.md`

### Modbus

산업 설비/PLC 쪽에서 자주 언급되는 프로토콜이다.

봐야 할 이유:

- 제조 현장 데이터 수집을 이해하려면 register 기반 통신 감각이 필요하다.
- 웹 API처럼 JSON 객체를 주고받는 것이 아니라, 주소와 레지스터 값을 읽고 쓰는 방식에 가깝다.

핵심 질문:

```text
어떤 장비의 어떤 register를 읽는가?
읽은 값의 단위와 의미는 무엇인가?
polling 주기는 어떻게 정하는가?
```

### OPC UA

산업 자동화에서 장비 데이터를 표준화해 다루기 위한 프로토콜/모델이다.

봐야 할 이유:

- 단순 값 읽기를 넘어 장비, 태그, 변수, 관계를 구조화해서 볼 수 있다.
- 제조 데이터 플랫폼과 잘 연결되는 개념이다.

핵심 질문:

```text
장비 데이터가 단순 숫자인가, 의미 있는 노드인가?
데이터 모델을 어떻게 표준화할 것인가?
```

## 세부 문서

- [WebSocket](./websocket.md)
- [MQTT](./mqtt.md)
- [Modbus](./modbus.md)
- [OPC UA](./opc-ua.md)
- [Industrial Edge Gateway](./industrial-edge-gateway.md)
- [Protocol Selection](./protocol-selection.md)
