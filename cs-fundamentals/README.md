# CS 기초 (Computer Science Fundamentals)

프로젝트를 만들 때 반복해서 마주치는 **근본 CS 지식**을 정리합니다. 현재는 log-friends, JVM 학습, 클라우드/백엔드 프로젝트를 같이 연결해 본다.

---

## 학습 구조

```
cs-fundamentals/
├── README.md               ← 이 파일 (전체 목차 + 개요)
├── network/                ← 네트워크 (OSI, TCP/IP, HTTP, TLS)
├── os/                     ← 운영체제 (프로세스, 스레드, 메모리)
├── sync-async/             ← 동기/비동기 + 이벤트 루프
├── design-patterns/        ← 디자인 패턴 (Repository, DI, 등)
├── auth-security/          ← 인증/보안 (OAuth, JWT, HTTPS)
├── database/               ← 데이터베이스 (SQL vs NoSQL, 인덱스, 트랜잭션)
└── logging/                ← 로깅 (로그 종류, 보안 로그, 구조화된 로깅)
```

---

## 왜 CS 기초가 중요한가?

백엔드 프로젝트의 모든 기술은 CS 기초 위에 있다:

| 프로젝트 기술 | 근본 CS 지식 |
|-------------|-------------|
| log-friends SDK `POST /ingest` | **네트워크, HTTP, timeout, retry** |
| JVM/ByteBuddy 계측 | **운영체제, 프로세스, 클래스 로딩** |
| TimescaleDB/PostgreSQL | **데이터베이스, 인덱스, 트랜잭션** |
| Spring Boot Console | **HTTP, REST, 동기/비동기, 디자인 패턴** |
| 인증/권한 설계 | **OAuth, JWT, HTTPS, CORS** |
| Docker/DSM 배포 | **네트워크 인터페이스, 포트, 컨테이너** |

---

## 주제별 상세 정리

| # | 주제 | 파일 | 핵심 내용 |
|---|------|------|----------|
| 1 | 네트워크 | [network/](./network/README.md) | OSI 7계층, TCP/IP, HTTP 1~3, TLS, DNS |
| 2 | 운영체제 | [os/](./os/README.md) | 프로세스 vs 스레드, 메모리, 스케줄링, 컨테이너 |
| 3 | 동기/비동기 | [sync-async/](./sync-async/README.md) | 블로킹/논블로킹, 이벤트 루프, async/await |
| 4 | 디자인 패턴 | [design-patterns/](./design-patterns/README.md) | Repository, DI, SOLID, Factory, Observer |
| 5 | 인증/보안 | [auth-security/](./auth-security/README.md) | OAuth 2.0, JWT, HTTPS, CORS, CSRF |
| 6 | 데이터베이스 | [database/](./database/README.md) | SQL vs NoSQL, 인덱스, 트랜잭션, CAP 정리 |
| 7 | 로깅 | [logging/](./logging/README.md) | 로그 종류, 구조화된 로깅, 보안 로그 보존, SIEM |
