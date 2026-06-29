# Connection Reuse와 Pool

HTTP 요청은 단순히 JSON을 보내는 것처럼 보여도 아래 비용이 숨어 있다.

```text
DNS 조회 -> TCP 3-way handshake -> TLS handshake -> HTTP 요청/응답
```

매번 새 연결을 만들면 요청 하나마다 이 비용을 반복한다. 그래서 HTTP 클라이언트는 가능한 한 연결을 재사용한다.

---

## Keep-Alive

HTTP/1.1은 기본적으로 연결 재사용을 전제로 한다.

```text
나쁜 흐름:
요청 1 -> TCP 연결 -> 응답 -> 연결 종료
요청 2 -> TCP 연결 -> 응답 -> 연결 종료
요청 3 -> TCP 연결 -> 응답 -> 연결 종료

좋은 흐름:
TCP 연결 1개
  -> 요청 1 / 응답
  -> 요청 2 / 응답
  -> 요청 3 / 응답
```

연결 재사용은 TCP handshake와 TLS handshake 비용을 줄인다.

---

## Connection Pool

Connection Pool은 여러 연결을 미리 만들거나 유지하면서 재사용하는 방식이다.

| 장점 | 주의점 |
|---|---|
| 연결 수립 비용 감소 | idle timeout 이후 끊긴 연결 처리 필요 |
| 처리량 증가 | max connection 수를 너무 키우면 서버 부담 |
| TLS 비용 감소 | 오래 열린 연결이 네트워크 장비에서 끊길 수 있음 |

HTTP/2는 하나의 TCP 연결 위에서 여러 stream을 동시에 처리할 수 있으므로 connection pool의 의미가 HTTP/1.1과 조금 다르다.

---

## log-friends 기준

SDK의 `BatchTransporter`는 singleton으로 존재하고, 내부에서 `HttpClient`를 lazy 생성한다. 이 구조는 매 flush마다 새 클라이언트를 만들지 않기 위한 방향이다.

```kotlin
private val httpClient: HttpClient by lazy {
    HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(5))
        .build()
}
```

이 설계가 중요한 이유:

- flush마다 TCP/TLS 연결 비용을 반복하지 않는다.
- 대상 앱 내부에서 불필요한 객체와 스레드 생성을 줄인다.
- Console 장애 시 timeout으로 빠져나올 수 있다.

---

## 관찰 포인트

| 확인할 것 | 이유 |
|---|---|
| flush 주기 | 너무 짧으면 HTTP 요청 수가 많아진다 |
| batch size | 너무 작으면 연결 재사용 이점보다 요청 오버헤드가 커진다 |
| timeout | 너무 길면 대상 앱 리소스를 붙잡는다 |
| Console 응답 시간 | DB 저장 지연이 SDK 전송 지연으로 이어진다 |
| 서버 keep-alive 설정 | 서버가 너무 빨리 연결을 끊으면 재사용 효과가 줄어든다 |

log-friends의 네트워크 튜닝은 "얼마나 빨리 보내는가"보다 "대상 앱에 부담을 얼마나 적게 주는가"가 먼저다.
