# HTTPS & TLS/SSL

---

## HTTPS = HTTP + TLS

```
HTTP (평문)                         HTTPS (암호화)
──────────                          ──────────
GET /subscriptions                  GET /subscriptions
Authorization: Bearer abc123        Authorization: Bearer abc123
                                    ↓ TLS로 암호화
네트워크에서 보면:                   네트워크에서 보면:
"GET /subscriptions                 "X#@!$%^&*(!@#$%"
 Authorization: Bearer abc123"      → 도청 불가, 변조 불가
```

### HTTP vs HTTPS 비교

| 구분 | HTTP | HTTPS |
|------|------|-------|
| 포트 | 80 | **443** |
| 암호화 | ❌ | ✅ (TLS) |
| 인증서 | 불필요 | 필요 (CA 발급) |
| 속도 | 약간 빠름 | TLS 핸드셰이크 비용 |
| SEO | 불이익 | **가점** |
| 필수 여부 | - | **사실상 필수** (브라우저 경고) |

---

## TLS Handshake 상세

### TLS 1.2 (Full Handshake — 2-RTT)

```
클라이언트                                    서버
    │                                         │
    │── ClientHello ─────────────────→ │      지원 암호화 목록, 랜덤값
    │                                         │
    │← ServerHello ──────────────────│      선택된 암호화, 랜덤값
    │← Certificate ──────────────────│      서버 인증서 (공개키 포함)
    │← ServerHelloDone ──────────────│
    │                                         │
    │   인증서 검증 (CA 체인 확인)              │
    │                                         │
    │── ClientKeyExchange ───────────→│      Pre-master secret (공개키로 암호화)
    │── ChangeCipherSpec ────────────→│      "이제부터 암호화 통신!"
    │── Finished ────────────────────→│
    │                                         │
    │← ChangeCipherSpec ─────────────│       
    │← Finished ─────────────────────│
    │                                         │
    │═══════ 암호화 통신 시작 ════════════│    대칭키로 데이터 전송
```

### TLS 1.3 (개선된 1-RTT)

```
클라이언트                                    서버
    │                                         │
    │── ClientHello ─────────────────→ │      암호화 목록 + Key Share
    │   (키 교환 데이터도 함께!)                │   
    │                                         │
    │← ServerHello + Key Share ──────│      선택된 암호화
    │← Certificate ──────────────────│      서버 인증서
    │← Finished ─────────────────────│
    │                                         │
    │── Finished ────────────────────→│
    │                                         │
    │═══════ 암호화 통신 시작 ════════════│

TLS 1.2: 2-RTT (왕복 2번)
TLS 1.3: 1-RTT (왕복 1번) → 더 빠름!
         0-RTT도 가능 (이전 연결 캐시 활용)
```

### TLS 1.2 vs TLS 1.3

| 구분 | TLS 1.2 | TLS 1.3 |
|------|---------|---------|
| 핸드셰이크 | 2-RTT | **1-RTT** (0-RTT 가능) |
| 키 교환 | RSA 허용 | **ECDHE만** (Forward Secrecy 필수) |
| 암호화 | 다양한 선택지 | **강력한 것만** (취약 제거) |
| 보안 | 보통 | **강화** |

---

## 대칭키 vs 비대칭키

### 대칭키 암호화 (Symmetric)

```
같은 키로 암호화/복호화

Alice ──[키 A로 암호화]──→ "X#@$%" ──[키 A로 복호화]──→ Bob

장점: 빠름 (AES: 초당 수 GB 처리)
단점: 키를 어떻게 안전하게 공유? (키 교환 문제)
대표: AES-128, AES-256, ChaCha20
```

### 비대칭키 암호화 (Asymmetric)

```
공개키로 암호화, 개인키로 복호화 (또는 반대)

Alice ──[Bob 공개키로 암호화]──→ "X#@$%" ──[Bob 개인키로 복호화]──→ Bob

장점: 키 교환 문제 없음 (공개키는 공개)
단점: 느림 (RSA: 대칭키보다 100~1000배 느림)
대표: RSA-2048, ECDSA (타원곡선), Ed25519
```

### TLS의 하이브리드 방식

```
1단계: 비대칭키로 "대칭키"를 안전하게 교환
  Client ──[서버 공개키로 Pre-master secret 암호화]──→ Server
  양쪽이 동일한 대칭키(Session Key) 생성

2단계: 대칭키로 실제 데이터 암/복호화
  Client ←──[대칭키로 암호화된 데이터]──→ Server

→ 비대칭키의 "안전한 교환" + 대칭키의 "빠른 속도" 결합!
```

### Forward Secrecy (전방 비밀성)

```
과거의 통신을 나중에 해독할 수 없도록 보장

❌ RSA 키 교환 (TLS 1.2)
  서버 개인키가 유출되면 → 과거 모든 통신을 해독 가능!

✅ ECDHE (Ephemeral) 키 교환 (TLS 1.3 필수)
  매 세션마다 새로운 임시 키 생성
  서버 키가 유출되어도 과거 세션은 안전!
```

---

## 인증서 (Certificate)

### 인증서 체인

```
Root CA (최상위 인증기관)
  │  발급
  ├── Intermediate CA (중간 인증기관)
  │     │  발급
  │     └── www.example.com (서버 인증서)
  │
  └── 브라우저/OS에 Root CA 목록이 내장되어 있음
      → Root CA 서명 검증으로 신뢰 확인

인증서에 포함된 정보:
  - 도메인명 (CN/SAN)
  - 공개키
  - 유효 기간
  - 발급자 (CA)
  - 서명 (CA의 개인키로 서명)
```

### 인증서 유형

| 유형 | 검증 수준 | 비용 | 예시 |
|------|----------|------|------|
| **DV** (Domain Validation) | 도메인 소유 | 무료~저가 | Let's Encrypt |
| **OV** (Organization) | 조직 검증 | 중간 | 기업 웹사이트 |
| **EV** (Extended) | 확장 검증 | 높음 | 은행, 정부 기관 |

> 대부분의 웹사이트는 **DV 인증서** (Let's Encrypt 등)로 충분

---

## DNS (Domain Name System)

### DNS 조회 과정

```
사용자: "www.example.com" 접속하고 싶어

1. 브라우저 캐시 확인         → 있으면 바로 사용
2. OS 캐시 확인 (/etc/hosts) → 로컬 매핑 확인
3. 로컬 DNS 서버 질문         → ISP의 DNS 서버
4. 재귀 조회:
   Root DNS (.com 어디있어?)
     → TLD DNS (.com → example.com은 어디?)
       → 권한 DNS (example.com → IP 반환)
5. IP 주소 반환: 93.184.216.34
6. 해당 IP로 TCP 연결 시작
```

### DNS 레코드 유형

| 레코드 | 용도 | 예시 |
|--------|------|------|
| **A** | 도메인 → IPv4 | `api.example.com → 93.184.216.34` |
| **AAAA** | 도메인 → IPv6 | `api.example.com → 2001:db8::1` |
| **CNAME** | 도메인 → 다른 도메인 | `www.example.com → example.com` |
| **MX** | 메일 서버 | `example.com → mail.example.com` |
| **TXT** | 텍스트 정보 | SPF, DKIM 인증 |
| **NS** | 네임서버 | `example.com → ns1.dns-provider.com` |

### DNS 캐시와 TTL

```
TTL (Time To Live) = DNS 레코드의 캐시 유효 시간

TTL 300 (5분): 5분마다 DNS 재질의
  → 빠른 변경 가능, DNS 부하 높음

TTL 86400 (24시간): 하루동안 캐시
  → DNS 부하 낮음, 변경 반영 느림

일반적: 300~3600초
```

---

## 면접 핵심 포인트

```
Q: HTTPS의 동작 방식을 설명하세요
A: TLS 핸드셰이크 → 비대칭키로 대칭키 교환 → 대칭키로 암호화 통신
   서버 인증서로 신뢰성 검증, CA 체인으로 인증서 검증

Q: TLS 1.3과 1.2의 차이?
A: 1-RTT 핸드셰이크(속도↑), 취약 암호화 제거(보안↑),
   ECDHE 필수(Forward Secrecy 보장)

Q: Forward Secrecy란?
A: 서버 키가 유출되어도 과거 통신을 해독 불가.
   매 세션마다 임시 키(Ephemeral Key) 사용.

Q: DNS는 TCP? UDP?
A: 일반 쿼리는 UDP (빠름, 패킷 1개).
   응답이 512바이트 초과 시 TCP로 재시도.
   Zone Transfer(서버 간 동기화)는 TCP.
```
