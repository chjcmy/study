# Timeout, Retry, Backoff

네트워크 요청은 실패한다는 전제로 설계해야 한다. 서버가 죽지 않아도 패킷 유실, DNS 지연, TCP 연결 지연, TLS 핸드셰이크 지연, 서버 과부하 때문에 요청은 늦거나 끊길 수 있다.

---

## Timeout

Timeout은 "언제까지 기다릴 것인가"를 정하는 값이다. timeout이 없으면 호출 스레드가 무한정 묶일 수 있다.

| 종류 | 의미 | 예시 |
|---|---|---|
| Connect Timeout | TCP 연결 수립까지 기다리는 시간 | Console 포트가 막혔거나 서버가 내려간 경우 |
| Read/Response Timeout | 요청 후 응답을 기다리는 시간 | Console이 느리거나 DB 작업이 밀린 경우 |
| Overall Timeout | 전체 요청에 허용하는 최대 시간 | 연결, 전송, 응답 전체 제한 |

log-friends SDK의 `BatchTransporter`는 Console `/ingest`로 batch를 보낸다. 이 요청은 대상 앱 내부에서 발생하므로, timeout은 대상 앱 보호 장치다.

---

## Retry

Retry는 실패한 요청을 다시 시도하는 것이다. 하지만 모든 요청이 재시도에 안전한 것은 아니다.

| 요청 성격 | 재시도 위험 |
|---|---|
| `GET` 조회 | 보통 낮음 |
| `PUT` 같은 idempotent 수정 | 설계에 따라 낮음 |
| `POST` 생성 | 중복 생성 위험 |
| batch ingest | 서버 저장 성공 후 응답만 유실되면 중복 저장 가능 |

log-friends의 `POST /ingest`는 Raw Event 저장 요청이다. 서버가 저장까지 성공했는데 네트워크가 끊겨 SDK가 실패로 판단하면, 같은 batch를 다시 보내 중복 Raw Event가 생길 수 있다.

1차 구현에서는 단순성이 중요하므로 완벽한 exactly-once 전송을 목표로 하지 않는다. 대신 중복 가능성을 문서화하고, 나중에 `eventId` 또는 batch item dedupe 키를 추가할 수 있게 남겨 둔다.

---

## Backoff

Backoff는 재시도 간격을 점점 늘리는 전략이다. 서버가 장애 상태인데 즉시 재시도를 반복하면 장애를 더 키운다.

```text
1회 실패 -> 100ms 대기
2회 실패 -> 200ms 대기
3회 실패 -> 400ms 대기
4회 실패 -> 800ms 대기
```

실무에서는 모든 클라이언트가 동시에 재시도하지 않도록 jitter를 섞는다.

```text
대기 시간 = 기본 backoff + 랜덤 jitter
```

---

## log-friends 기준 정리

| 상황 | 1차 판단 |
|---|---|
| Console이 잠깐 느림 | 짧은 timeout으로 대상 앱 보호 |
| Console이 내려감 | queue에 재삽입하되 capacity 초과 시 drop |
| 응답 유실 | 중복 Raw Event 가능성 인정 |
| 장기 장애 | 무한 재시도보다 dropCount 관찰 |
| 대규모 개선 | eventId/dedupe 키, backoff, circuit breaker 검토 |

핵심은 "수집 안정성"과 "대상 앱 보호"의 균형이다. 수집 SDK가 대상 앱을 더 위험하게 만들면 안 된다.
