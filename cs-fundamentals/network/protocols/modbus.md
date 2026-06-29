# Modbus

Modbus는 산업 설비, 센서, PLC에서 오래 사용된 통신 프로토콜이다.

핵심은 객체나 JSON을 주고받는 것이 아니라, 장비가 가진 `coil`, `input`, `register` 주소를 읽고 쓰는 방식이라는 점이다.

```text
SCADA / HMI / Edge gateway
-> Modbus master/client
-> PLC / 설비 / 계측기
-> coil / register 값
```

## 기본 구조

Modbus는 주로 요청/응답 방식으로 동작한다.

| 역할 | 의미 |
|---|---|
| Client / Master | 값을 읽거나 쓰는 쪽 |
| Server / Slave | register 값을 제공하는 장비 |
| Function code | read/write 같은 동작 종류 |
| Address | 읽거나 쓸 데이터 위치 |
| Register value | 실제 설비 상태나 측정값 |

자주 보는 형태:

- Modbus RTU: 시리얼 통신 기반
- Modbus TCP: TCP/IP 기반

## 데이터 모델

Modbus 데이터는 의미 있는 객체보다 메모리 맵에 가깝다.

| 영역 | 용도 |
|---|---|
| Coil | 1비트 출력 값, on/off 제어 |
| Discrete Input | 1비트 입력 상태 |
| Input Register | 읽기 전용 측정값 |
| Holding Register | 읽기/쓰기 가능한 설정값 또는 상태값 |

중요한 점은 register 값만 봐서는 의미를 알 수 없다는 것이다.

```text
40001 = 온도?
40002 = 압력?
40003 = 모터 속도?
값 1234 = 12.34도? 1234rpm? 알 수 없음
```

따라서 장비 매뉴얼이나 register map이 반드시 필요하다.

## Polling

Modbus는 보통 client가 주기적으로 값을 읽는다.

```text
1초마다 40001~40010 읽기
5초마다 상태 register 읽기
이상 상태일 때 특정 coil 쓰기
```

polling 주기를 정할 때는 아래를 봐야 한다.

| 기준 | 질문 |
|---|---|
| 변화 속도 | 값이 얼마나 자주 변하는가 |
| 장비 부하 | 너무 자주 읽으면 PLC나 장비에 부담이 되는가 |
| 네트워크 | 지연, 끊김, 패킷 손실이 있는가 |
| 업무 중요도 | 늦게 알아도 되는 값인가, 즉시 알아야 하는 값인가 |

## 신뢰성과 운영 이슈

Modbus는 단순하고 널리 쓰이지만, 운영할 때는 주의할 점이 많다.

- register 주소 체계가 장비마다 다를 수 있다.
- 값의 단위, scale, byte order를 잘못 해석하기 쉽다.
- timeout, retry, reconnect 정책이 필요하다.
- polling 주기가 너무 짧으면 장비와 네트워크에 부담이 된다.
- 쓰기 작업은 설비 동작에 영향을 줄 수 있으므로 권한과 검증이 필요하다.
- 보안 기능이 약하므로 네트워크 분리, gateway, 방화벽이 중요하다.

## 구현할 때 물어볼 것

```text
Modbus RTU인가, Modbus TCP인가?
어떤 장비의 어떤 register를 읽는가?
register map 문서가 있는가?
값의 타입, 단위, scale, byte order는 무엇인가?
읽기만 하는가, 쓰기도 하는가?
polling 주기는 얼마가 적절한가?
timeout, retry, reconnect는 어떻게 할 것인가?
장비 장애와 네트워크 장애를 어떻게 구분할 것인가?
```

## 한 줄 정리

Modbus는 산업 장비의 register 값을 직접 읽고 쓰는 단순한 프로토콜이며, 핵심은 통신 자체보다 register map을 정확히 해석하고 안정적으로 polling하는 것이다.
