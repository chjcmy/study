# TCP vs UDP

---

## TCP (Transmission Control Protocol)

### 3-Way Handshake (연결 수립)

```
클라이언트              서버
    │── SYN ──────→ │        1. "연결할래!" (SYN=1, seq=100)
    │← SYN+ACK ────│        2. "OK, 나도!" (SYN=1, ACK=1, seq=300, ack=101)
    │── ACK ──────→ │        3. "확인!" (ACK=1, seq=101, ack=301)
    │                │
    │   ═══ 연결 완료 ═══    이제 데이터 전송 가능
```

### 4-Way Handshake (연결 종료)

```
클라이언트              서버
    │── FIN ──────→ │        1. "끊을래!" 
    │← ACK ────────│        2. "알겠어, 잠깐만" (아직 보낼 게 남았을 수 있음)
    │               │
    │← FIN ────────│        3. "나도 끊을 준비 됐어!"
    │── ACK ──────→ │        4. "OK, bye!"
    │               │
    │  TIME_WAIT    │        클라이언트 2MSL 대기 후 완전 종료
```

### 왜 3-Way/4-Way인가?

```
연결: 3-Way 
  → SYN과 ACK를 하나의 패킷(SYN+ACK)으로 합칠 수 있어서 3번

종료: 4-Way
  → 서버가 FIN 받아도 아직 보낼 데이터가 있을 수 있음
  → ACK와 FIN을 따로 보내야 해서 4번
```

### TCP 주요 특성

| 특성 | 설명 | 동작 |
|------|------|------|
| **순서 보장** | Sequence Number로 순서 추적 | 순서 뒤바뀌면 재조립 |
| **재전송** | ACK 못 받으면 재전송 | Timeout 또는 3중복 ACK |
| **흐름 제어** | 수신자가 처리 가능한 만큼만 | Window Size 조절 |
| **혼잡 제어** | 네트워크 과부하 방지 | Slow Start → Congestion Avoidance |

### TCP 흐름 제어 (Flow Control)

```
수신 측이 "나 이만큼만 받을 수 있어" 알려줌

송신 →  [Seg 1][Seg 2][Seg 3]   Window Size = 3
수신 ←  ACK + Window Size = 2    "좀 바쁘니까 2개씩만"
송신 →  [Seg 4][Seg 5]           Window 줄임
수신 ←  ACK + Window Size = 0    "잠깐 멈춰!"
         ... 수신 버퍼 비움 ...
수신 ←  Window Update = 4        "다시 4개 보내도 돼"
```

### TCP 혼잡 제어 (Congestion Control)

```
Slow Start:
  Window 1 → 2 → 4 → 8 → 16 (지수 증가)
  ssthresh 도달 시 → Congestion Avoidance (선형 증가)

네트워크 혼잡 감지 시 (패킷 유실):
  1. TCP Tahoe: Window를 1로 리셋 (보수적)
  2. TCP Reno:  Window를 절반으로 (빠른 회복)

Window
  │      /\
  │     /  \    /\  /\
  │    /    \  /  \/  \
  │   /      \/        \
  │  /                   \
  │_/________________________→ 시간
   ↑         ↑
 Slow Start  패킷 유실 감지
```

---

## UDP (User Datagram Protocol)

```
[빠르지만 신뢰성 없음] 연결 없이 바로 전송

클라이언트              서버
    │── 데이터 ────→ │        바로 전송 (핸드셰이크 없음)
    │── 데이터 ────→ │        순서 보장 없음
    │── 데이터 ────→ │        손실 가능, 확인 없음
```

### UDP 헤더 구조

```
TCP 헤더: 20~60 바이트 (옵션 포함)
UDP 헤더: 8 바이트만!

┌──────────────┬──────────────┐
│  Source Port  │  Dest Port   │  4 바이트
├──────────────┼──────────────┤
│    Length     │   Checksum   │  4 바이트
└──────────────┴──────────────┘
│         Payload              │

→ 헤더 오버헤드가 적어서 빠름
```

---

## 비교 정리

| 특성 | TCP | UDP |
|------|-----|-----|
| 연결 | 연결형 (3-way handshake) | 비연결형 |
| 신뢰성 | 보장 (재전송, 순서) | 미보장 |
| 순서 | 보장 (Sequence Number) | 미보장 |
| 속도 | 느림 (오버헤드) | **빠름** |
| 헤더 크기 | 20~60 바이트 | **8 바이트** |
| 흐름 제어 | 있음 (Window) | 없음 |
| 혼잡 제어 | 있음 | 없음 |
| 사용 | HTTP, API, 파일 전송, 이메일 | DNS, 스트리밍, 게임, VoIP |
| **Log-Doctor** | **REST API 통신** | DNS 조회 시 |

### 언제 뭘 쓸까?

```
TCP를 써야 하는 경우:
  ✅ 데이터 정확성이 중요 (API, 파일 전송, 이메일)
  ✅ 순서가 중요 (HTML 페이지 로딩)
  ✅ Log-Doctor의 REST API 통신

UDP를 써야 하는 경우:
  ✅ 속도가 중요 (실시간 스트리밍, 게임)
  ✅ 약간의 손실이 허용됨 (음성 통화 — 약간 끊겨도 OK)
  ✅ 간단한 요청/응답 (DNS 쿼리 — 1개 패킷)
```

---

## 면접 핵심 포인트

```
Q: TCP와 UDP의 가장 큰 차이?
A: TCP는 "신뢰성", UDP는 "속도" 
   → TCP는 3-way handshake + 재전송 + 순서 보장
   → UDP는 연결 없이 바로 전송, 8바이트 헤더
   
Q: HTTP/3가 UDP를 쓰는 이유?
A: TCP의 Head-of-Line Blocking 문제 해결
   → QUIC 프로토콜이 UDP 위에 신뢰성을 구현
   → TCP 수준의 신뢰성 + UDP 수준의 속도

Q: TCP의 TIME_WAIT 상태는 왜 필요한가?
A: 1. 지연된 패킷이 새 연결에 혼입되는 것 방지
   2. 마지막 ACK 유실 시 재전송 가능하도록 대기
   → 보통 2 * MSL(Maximum Segment Lifetime) = 60초~120초
```
