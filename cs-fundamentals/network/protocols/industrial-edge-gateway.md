# Industrial Edge Gateway

Industrial Edge Gateway는 설비, 센서, PLC와 backend 사이에서 데이터를 수집, 정리, 보호, 전달하는 중간 계층이다.

핵심은 단순 relay가 아니다. 현장 장비의 통신 방식과 backend의 데이터 처리 방식을 맞춰주는 변환 지점이다.

```text
설비 / 센서 / PLC
-> Industrial Edge Gateway
-> Backend API / Message broker / Data platform
-> Monitoring / 분석 / AI
```

## 왜 필요한가

현장 장비는 backend가 기대하는 방식으로 데이터를 주지 않는 경우가 많다.

- 장비마다 프로토콜이 다르다.
- 데이터 단위가 register, tag, event, log처럼 제각각이다.
- 네트워크가 항상 안정적이지 않다.
- 현장망과 backend망을 직접 연결하기 어렵다.
- 보안, 인증, 감사 로그가 필요하다.

Gateway는 이 차이를 흡수해서 backend가 더 일관된 방식으로 데이터를 받을 수 있게 만든다.

## 주요 역할

| 역할 | 설명 |
|---|---|
| 프로토콜 어댑터 | Modbus, OPC UA, MQTT, HTTP 등 장비별 통신 방식을 연결한다 |
| 버퍼링 | backend 장애나 네트워크 단절 시 데이터를 임시 저장한다 |
| 정규화 | register 값, tag, event를 공통 schema로 바꾼다 |
| 보안 경계 | 인증, 암호화, 접근 제어, 현장망 분리를 담당한다 |
| 모니터링 | 장비 연결 상태, 수집 지연, 전송 실패, queue 적체를 관찰한다 |

## 프로토콜 어댑터

Gateway는 여러 프로토콜을 하나의 backend 전달 방식으로 바꾸는 adapter 역할을 한다.

예시:

```text
Modbus register read
-> temperature tag로 해석
-> JSON event로 변환
-> HTTP POST 또는 MQTT publish
```

중요한 질문:

```text
어떤 장비에서 어떤 값을 읽는가?
값의 단위, scale, 의미는 어디서 관리하는가?
읽기 실패와 timeout은 어떻게 처리하는가?
```

## 버퍼링

현장 데이터는 backend가 잠깐 죽었다고 바로 버리면 안 되는 경우가 많다.

Gateway는 보통 아래 정책을 가진다.

- 메모리 또는 디스크 queue에 임시 저장
- 재시도와 backoff 적용
- 오래된 데이터 삭제 기준 설정
- 중복 전송을 감안한 idempotency key 사용

버퍼링을 볼 때 핵심은 `얼마나 오래 보관할 것인가`와 `중복 전송을 어떻게 처리할 것인가`다.

## 정규화

장비 데이터는 원래 backend 친화적인 데이터가 아니다.

```text
register 40001 = 253
scale = 0.1
unit = celsius
meaning = chamber_temperature
```

정규화 후:

```json
{
  "equipmentId": "EQ-01",
  "metric": "chamber_temperature",
  "value": 25.3,
  "unit": "celsius",
  "timestamp": "2026-06-24T10:00:00Z"
}
```

정규화는 단순 format 변환이 아니라, 값에 의미를 붙이는 작업이다.

## 보안

Gateway는 현장망과 backend망 사이의 보안 경계가 된다.

- backend로 나가는 방향만 허용
- 인증서, token, API key 관리
- TLS 적용
- 장비 접근 권한 제한
- 설정 변경 감사 로그 기록

장비를 backend에 직접 노출하지 않는 것이 기본 방향이다.

## 모니터링

Gateway 자체도 운영 대상이다.

봐야 할 지표:

| 지표 | 의미 |
|---|---|
| 장비 연결 상태 | PLC, 센서, OPC UA server와 연결되어 있는가 |
| 수집 지연 | 현장 값이 backend에 도착하기까지 얼마나 걸리는가 |
| 전송 실패율 | backend 전송이 얼마나 실패하는가 |
| queue 크기 | 버퍼가 밀리고 있는가 |
| adapter 오류 | 특정 프로토콜 변환에서 실패가 반복되는가 |

## 핵심 질문

```text
Gateway가 없으면 backend가 현장 프로토콜을 직접 알아야 하는가?
네트워크 단절 시 데이터는 어디에 남는가?
장비별 값의 의미와 단위는 어디서 관리하는가?
현장망과 backend망의 보안 경계는 어디인가?
```

