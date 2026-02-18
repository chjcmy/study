# CS 기초 (Computer Science Fundamentals)

Azure 프로젝트(Log-Doctor)의 **근본이 되는 CS 지식**을 정리합니다.

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
└── database/               ← 데이터베이스 (SQL vs NoSQL, 인덱스, 트랜잭션)
```

---

## 왜 CS 기초가 중요한가?

Log-Doctor 프로젝트의 모든 기술이 CS 기초 위에 있습니다:

| 프로젝트 기술 | 근본 CS 지식 |
|-------------|-------------|
| FastAPI (async/await) | **동기/비동기, 이벤트 루프, 코루틴** |
| OBO Flow / JWT | **인증/보안, 토큰 기반 인증, OAuth 2.0** |
| ARM API / REST | **네트워크, HTTP, TLS/SSL** |
| Cosmos DB (파티션 키) | **데이터베이스, NoSQL, 분산 시스템** |
| Repository 패턴 / DI | **디자인 패턴, SOLID 원칙** |
| Container App (스케일링) | **운영체제, 프로세스/스레드, 컨테이너** |

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
