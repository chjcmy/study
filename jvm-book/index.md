# JVM 밑바닥까지 파헤치기 — 학습 로드맵

> **책:** 深入理解Java虚拟机 3판 (저우즈밍)
> **목표:** JVM 내부 동작 원리를 이해하고, log-friends SDK 코드와 연결하여 체화
> **총 예상 학습 시간:** 45~55시간 (5파트, 13장)

---

## 학습 순서

```
Part 1 (기초)
  ↓
Part 2 (메모리) ← 가장 중요, 시간 투자 필요
  ↓
Part 3 (실행) ← log-friends SDK와 연결 가장 밀접
  ↓
Part 4 (컴파일) ← JIT 최적화 이해
  ↓
Part 5 (동시성) ← BatchTransporter, KafkaProducer 설계 이해
```

---

## 파트별 개요

### [1부: 자바와 친해지기](part1-java-intro.md)

| 항목 | 내용 |
|---|---|
| **범위** | 1장 — 자바 기술 시스템 소개 |
| **학습 시간** | 4~5시간 |
| **핵심** | JDK/JRE/JVM 계층, JDK 역사 (1.0~21), VM 제품군 비교, 자바의 미래 |
| **프로젝트 연결** | JDK 21 가상 스레드, ByteBuddy와 Instrumentation API, Kotlin 언어 독립성 |

### [2부: 자동 메모리 관리](part2-memory.md)

| 항목 | 내용 |
|---|---|
| **범위** | 2장 메모리 영역 / 3장 GC / 4장 모니터링 도구 / 5장 튜닝 사례 |
| **학습 시간** | 15~18시간 |
| **핵심** | 런타임 데이터 영역, 객체 생성과 레이아웃, GC 알고리즘 4종, GC 수집기 (Serial~ZGC), OOM 진단 |
| **프로젝트 연결** | BatchTransporter 큐의 GC 압력, ByteBuddy Metaspace 영향, KafkaProducer 다이렉트 메모리, ZGC 권장 설정 |

### [3부: 가상 머신 실행 서브시스템](part3-execution.md)

| 항목 | 내용 |
|---|---|
| **범위** | 6장 클래스 파일 / 7장 클래스 로딩 / 8장 바이트코드 실행 / 9장 실전 |
| **학습 시간** | 15~20시간 |
| **핵심** | 클래스 파일 구조, 부모 위임 모델, 스택 프레임과 디스패치, invokedynamic, ByteBuddy 바이트코드 |
| **프로젝트 연결** | LogFriendsInstaller RETRANSFORMATION, InstrumentationRegistry 계측, MethodDelegation 내부 동작 |

### [4부: 컴파일과 최적화](part4-compilation.md)

| 항목 | 내용 |
|---|---|
| **범위** | 10장 프런트엔드 컴파일 / 11장 백엔드 컴파일 |
| **학습 시간** | 10~12시간 |
| **핵심** | javac 4단계, 타입 소거 제네릭, JIT (C1/C2 계층형 컴파일), 인라인, 탈출 분석, AOT vs JIT |
| **프로젝트 연결** | ByteBuddy 프록시 인라인, Kotlin inline과 JVM 인라인 관계, 10ms 임계값과 워밍업, native-image 호환성 |

### [5부: 효율적인 동시성](part5-concurrency.md)

| 항목 | 내용 |
|---|---|
| **범위** | 12장 자바 메모리 모델 / 13장 스레드 안전과 락 최적화 |
| **학습 시간** | 10~12시간 |
| **핵심** | JMM, volatile/happens-before, 가상 스레드, CAS, 락 팽창 (편향→경량→중량), 스레드 안전성 5단계 |
| **프로젝트 연결** | BatchTransporter DCL+@Volatile, AtomicBoolean/AtomicLong CAS, LinkedBlockingQueue dual-lock, 가상 스레드 pinning |

---

## log-friends SDK와의 연결 지도

```
SDK 컴포넌트              →  JVM 챕터

LogFriendsInstaller       →  7장 (클래스 로딩, RETRANSFORMATION)
                             3장 (Metaspace GC)

InstrumentationRegistry   →  6장 (바이트코드 구조)
                             8장 (MethodDelegation, invokedynamic)
                             9장 (ByteBuddy vs CGLIB)

SpringInterceptor         →  8장 (동적 디스패치, vtable)
MethodTraceInterceptor    →  11장 (JIT 워밍업, 10ms 임계값)

BatchTransporter          →  2장 (힙 메모리, GC 압력)
                             12장 (volatile, DCL)
                             13장 (CAS, BlockingQueue)

KafkaProducer (lazy init) →  2장 (다이렉트 메모리)
                             12장 (happens-before)
                             13장 (스레드 안전성)
```

---

## 추천 학습 방법

1. **각 파트를 순서대로** 읽되, Part 2와 Part 3에 가장 많은 시간 투자
2. **실습 섹션**은 반드시 직접 실행 (jps, jstat, javap, JITWatch 등)
3. **핵심 질문**을 먼저 읽고 답변을 가린 채 자기 언어로 설명해보기
4. **체크리스트** 항목을 모두 체크할 수 있을 때 다음 파트로 이동
5. **프로젝트 연결** 섹션에서 실제 SDK 코드를 열어 대조하며 읽기
