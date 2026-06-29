# OPC UA

OPC UA는 산업 자동화 환경에서 장비 데이터를 표준화해서 다루기 위한 프로토콜이자 데이터 모델이다.

Modbus가 register 주소를 읽는 느낌이라면, OPC UA는 장비, 변수, 상태, 관계를 의미 있는 `Node` 구조로 표현하는 데 가깝다.

```text
PLC / 설비 / 센서
-> OPC UA Server
-> Edge gateway / SCADA / Data platform
-> Node 탐색, 값 읽기, subscription
```

## 기본 구조

OPC UA는 client/server 구조로 많이 사용된다.

| 역할 | 의미 |
|---|---|
| OPC UA Server | 장비 데이터와 Node 모델을 제공 |
| OPC UA Client | Node를 탐색하고 값을 읽거나 구독 |
| Node | 변수, 객체, 메서드, 타입 같은 데이터 단위 |
| Namespace | Node 이름 충돌을 피하고 모델을 구분하는 영역 |
| NodeId | 특정 Node를 가리키는 식별자 |

## 데이터 모델

OPC UA의 강점은 값에 의미와 구조를 붙일 수 있다는 점이다.

```text
Factory
-> Line A
-> Machine 1
-> Temperature
-> Pressure
-> RunningStatus
```

단순히 `40001 = 1234`처럼 보는 것이 아니라, `Machine1.Temperature = 12.34 C`처럼 해석할 수 있다.

| 개념 | 의미 |
|---|---|
| Object | 설비, 라인, 장치 같은 대상 |
| Variable | 온도, 압력, 상태 같은 값 |
| Method | 장비가 제공하는 동작 |
| Reference | Node 사이의 관계 |
| DataType | 값의 타입 |

## Read와 Subscription

OPC UA에서는 값을 직접 읽을 수도 있고, 변경을 구독할 수도 있다.

### Read

client가 필요할 때 server에 값을 요청한다.

```text
현재 온도 읽기
현재 모터 상태 읽기
현재 알람 값 읽기
```

단순하고 이해하기 쉽지만, 자주 읽으면 polling 부하가 생길 수 있다.

### Subscription

client가 관심 있는 Node를 등록하고, server가 변경 사항을 전달한다.

```text
Temperature 값이 바뀌면 알림
RunningStatus가 변경되면 알림
Alarm Node가 갱신되면 알림
```

실시간 모니터링이나 상태 변화 감지에는 subscription이 더 자연스럽다.

## 신뢰성과 운영 이슈

OPC UA는 구조화와 표준화에 강하지만, 실제 운영에서는 모델과 연결 정책이 중요하다.

- Node 모델이 장비나 벤더마다 다를 수 있다.
- 어떤 Node를 수집할지 명확히 정해야 한다.
- sampling interval과 publishing interval을 적절히 잡아야 한다.
- 연결 끊김, session 만료, subscription 복구를 처리해야 한다.
- 인증서, 보안 정책, 사용자 인증 설정을 맞춰야 한다.
- 고빈도 데이터를 너무 많이 구독하면 server와 network에 부담이 된다.

## Modbus와 비교

| 기준 | Modbus | OPC UA |
|---|---|---|
| 데이터 관점 | register 주소와 값 | 의미 있는 Node 모델 |
| 구조화 | 약함 | 강함 |
| 수집 방식 | 주로 polling | read와 subscription |
| 장점 | 단순하고 널리 쓰임 | 표준화, 보안, 모델링에 강함 |
| 주의점 | register 해석 필요 | Node 모델 설계와 운영 정책 필요 |

## 구현할 때 물어볼 것

```text
OPC UA Server는 어디에 있는가?
수집할 NodeId 목록이 있는가?
Node 모델을 탐색해서 정할 것인가, 고정 목록으로 관리할 것인가?
read 방식인가, subscription 방식인가?
sampling interval과 publishing interval은 얼마가 적절한가?
연결이 끊겼을 때 session과 subscription을 어떻게 복구할 것인가?
인증서와 보안 정책은 무엇을 사용할 것인가?
수집한 Node를 backend 데이터 모델에 어떻게 매핑할 것인가?
```

## 한 줄 정리

OPC UA는 산업 데이터를 단순 값이 아니라 의미 있는 Node 모델로 다루게 해주는 표준이며, 핵심은 어떤 Node를 어떤 주기로 어떻게 안정적으로 수집하고 복구할지 정하는 것이다.
