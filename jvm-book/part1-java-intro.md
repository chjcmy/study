# 1부: 자바와 친해지기

> 예상 학습 시간: 4~5시간

## 1장 자바 기술 시스템 소개

### 1.1 들어가며

자바는 단순한 프로그래밍 언어가 아니다. **언어 + 가상 머신 + 클래스 라이브러리 + 도구**를 아우르는 기술 플랫폼이다. "Write Once, Run Anywhere"라는 슬로건이 가능한 이유는 자바 코드가 특정 OS의 기계어가 아니라 **바이트코드(bytecode)**로 컴파일되고, JVM이 이를 각 플랫폼에 맞게 실행하기 때문이다.

이 장에서 주목해야 할 핵심 질문은 다음과 같다:

- 자바 기술 시스템을 구성하는 요소들은 어떻게 계층화되어 있는가?
- 수많은 JVM 구현체 중 왜 핫스팟이 표준이 되었는가?
- 자바는 어디로 향하고 있으며, 우리 프로젝트에 어떤 영향을 주는가?

---

### 핵심 개념 1 --- 자바 기술 시스템 구성

#### JDK, JRE, JVM의 관계

자바 기술 시스템은 3개의 동심원 구조로 이해할 수 있다. 안쪽부터 바깥으로 갈수록 범위가 넓어진다.

```
+---------------------------------------------------------------+
|                          JDK                                  |
|  +----------------------------------------------------------+ |
|  |                        JRE                                | |
|  |  +-----------------------------------------------------+ | |
|  |  |                     JVM                              | | |
|  |  |  +----------+  +------------+  +-----------------+   | | |
|  |  |  | 클래스    |  | 실행 엔진  |  | 메모리 관리     |   | | |
|  |  |  | 로더     |  | (JIT 등)   |  | (GC)           |   | | |
|  |  |  +----------+  +------------+  +-----------------+   | | |
|  |  +-----------------------------------------------------+ | |
|  |                                                          | |
|  |  +-----------------------------------------------------+ | |
|  |  |  Java SE API (java.lang, java.util, java.io ...)    | | |
|  |  +-----------------------------------------------------+ | |
|  +----------------------------------------------------------+ |
|                                                               |
|  +----------------------------------------------------------+ |
|  |  개발 도구: javac, javap, jconsole, jvisualvm, jlink ... | |
|  +----------------------------------------------------------+ |
+---------------------------------------------------------------+
```

| 구성 요소 | 포함 범위 | 핵심 역할 |
|-----------|-----------|-----------|
| **JVM** | 실행 엔진 + GC + 클래스 로더 | 바이트코드를 OS별 기계어로 변환하여 실행 |
| **JRE** | JVM + Java SE API | 자바 프로그램을 **실행**하기 위한 최소 환경 |
| **JDK** | JRE + 개발 도구(javac, javap 등) | 자바 프로그램을 **개발 + 실행**하기 위한 전체 환경 |

> **왜 이 구분이 중요한가?**
> JDK 11부터 JRE가 별도로 배포되지 않는다. `jlink`로 필요한 모듈만 포함하는 커스텀 런타임을 만들 수 있게 되었기 때문이다. 하지만 **개념적 계층 구조**는 여전히 유효하며, JVM 내부 동작을 이해하려면 이 경계를 명확히 알아야 한다.

#### Java SE / EE / ME 에디션

```
                Java 기술 시스템 에디션
                        |
        +---------------+---------------+
        |               |               |
    Java SE         Java EE         Java ME
  (Standard)      (Enterprise)      (Micro)
  핵심 API +       SE + 서블릿,      임베디드 /
  JVM 스펙         JPA, EJB 등      모바일 장치
        |               |
        |          Jakarta EE로
        |          이관 (2017~)
        |
   OpenJDK 기반
   구현체들
```

| 에디션 | 대상 | 현재 상태 |
|--------|------|-----------|
| **Java SE** | 데스크톱, 서버 범용 | OpenJDK로 활발히 개발 중 |
| **Java EE** | 엔터프라이즈 (웹서버, 분산 시스템) | Eclipse 재단으로 이관, **Jakarta EE**로 명칭 변경 |
| **Java ME** | 임베디드, 피처폰 | 사실상 퇴장. IoT는 SE 기반으로 전환 |

> **log-friends와의 연결:** log-friends-sdk는 Java SE 21 위에서 동작한다. Spring Boot는 과거 Java EE의 서블릿 스펙에 기반하되 Jakarta EE 네임스페이스(`jakarta.servlet`)로 전환되었다. SDK의 `SpringInterceptor`가 `DispatcherServlet`을 계측하는 것도 이 서블릿 스펙 위에서 이루어진다.

---

### 핵심 개념 2 --- JDK 역사와 주요 변곡점

자바의 역사는 단순한 버전 나열이 아니라, **기술적 결정이 왜 그 시점에 이루어졌는가**를 이해하는 것이 중요하다.

#### JDK 버전별 핵심 이정표

| 버전 | 연도 | 대표 기술 | 의미 |
|------|------|-----------|------|
| **JDK 1.0** | 1996 | JVM, Applet, AWT | 자바 탄생. "Write Once, Run Anywhere" |
| **JDK 1.1** | 1997 | JDBC, Inner Class, Reflection, RMI | 엔터프라이즈 기반 마련 |
| **JDK 1.2** | 1998 | JIT 컴파일러, Collections, Swing | **Java 2 플랫폼** (SE/EE/ME 분리). 성능 도약 |
| **JDK 1.3** | 2000 | **HotSpot VM 기본 채택**, JNDI | 핫스팟이 표준 VM이 되는 전환점 |
| **JDK 1.4** | 2002 | NIO, 정규표현식, assert, 로깅 API | 첫 JCP(Java Community Process) 주도 릴리즈 |
| **JDK 5** | 2004 | 제네릭, 어노테이션, 오토박싱, enum, `java.util.concurrent` | **언어 문법의 대변혁**. 현대 자바의 시작 |
| **JDK 6** | 2006 | 스크립트 엔진, JDBC 4.0, **OpenJDK 오픈소스화** | 썬의 오픈소스 전환 결정 |
| **JDK 7** | 2011 | try-with-resources, diamond, Fork/Join, invokedynamic | 오라클 인수 후 첫 릴리즈. `invokedynamic`은 Kotlin/Scala에 핵심 |
| **JDK 8** | 2014 | **Lambda, Stream API**, Optional, 인터페이스 default 메서드 | 함수형 프로그래밍 도입. 역대 가장 오래 쓰인 LTS |
| **JDK 9** | 2017 | **모듈 시스템(Jigsaw)**, JShell, G1 GC 기본 | 6개월 릴리즈 주기 시작 |
| **JDK 11** | 2018 | HTTP Client, var 지역변수, Flight Recorder 오픈소스 | 첫 LTS (새 릴리즈 정책). JavaFX 분리 |
| **JDK 14** | 2020 | Records(프리뷰), Switch 표현식, ZGC 개선 | 데이터 중심 클래스의 시작 |
| **JDK 17** | 2021 | Sealed Classes, Pattern Matching, macOS AArch64 | LTS. 많은 기업이 JDK 8 → 17로 마이그레이션 |
| **JDK 21** | 2023 | **가상 스레드(Loom)**, Sequenced Collections, Pattern Matching 확장, Generational ZGC | LTS. **동시성 모델의 패러다임 전환** |

#### 주요 변곡점 해설

**1. 오라클의 썬 인수 (2009~2010)**

썬 마이크로시스템즈가 오라클에 인수되면서 자바 생태계가 크게 요동쳤다. 오라클은 JRockit VM의 기술을 핫스팟에 통합하기 시작했고(JDK 7~8), 유료 라이선스 정책을 도입했다. 이에 대한 반작용으로 OpenJDK 기반의 무료 배포판(AdoptOpenJDK, Amazon Corretto, Azul Zulu 등)이 활성화되었다.

**2. 6개월 릴리즈 주기 (JDK 9~)**

JDK 9부터 6개월마다 새 버전이 나오는 "시간 기반 릴리즈" 모델로 전환했다. 기능이 준비되면 넣고, 안 되면 다음 버전으로 미루는 방식이다. 이로 인해:
- 프리뷰/인큐베이터 기능으로 실험적 기술을 미리 공개
- 2년마다 LTS(Long-Term Support) 버전 지정 (11, 17, 21, ...)
- 기업들은 LTS만 채택하고, 개발자들은 최신 기능을 빠르게 테스트

**3. JDK 21과 가상 스레드 (Project Loom)**

가상 스레드는 OS 스레드 1개 위에 수천~수만 개의 경량 스레드를 올릴 수 있게 해준다. 이는 I/O 바운드 애플리케이션의 동시성을 극적으로 향상시킨다.

```java
// 전통적 방식: OS 스레드 풀 (수백 개 한계)
ExecutorService executor = Executors.newFixedThreadPool(200);

// JDK 21 가상 스레드: 수만 개도 가능
ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
```

> **log-friends와의 연결:** log-friends-sdk가 JDK 21을 타겟으로 하는 것은 가상 스레드의 혜택을 직접 받을 수 있다는 뜻이다. `BatchTransporter`의 비동기 큐 처리나 Kafka 전송 로직에서 가상 스레드를 활용하면 적은 리소스로 높은 처리량을 달성할 수 있다.

---

### 핵심 개념 3 --- 자바 가상 머신 제품군

JVM은 **스펙(specification)**이지 단일 제품이 아니다. JVM 스펙을 충족하는 한 누구나 자체 JVM을 만들 수 있다.

#### 주요 JVM 비교

| JVM | 개발사 | 핵심 특징 | 현재 상태 |
|-----|--------|-----------|-----------|
| **HotSpot VM** | 썬 → 오라클 | C1(클라이언트) + C2(서버) 2단계 JIT, 적응형 최적화 | **사실상 표준**. OpenJDK 기본 VM |
| **Exact VM** | 썬 | 정확한 메모리 관리(Exact Memory Management) 시도 | JDK 1.2에서 잠시 등장 후 핫스팟에 흡수 |
| **JRockit** | BEA → 오라클 | 서버 사이드 최적화에 특화, Mission Control 도구 | 핫스팟에 기술 통합 후 단종. Flight Recorder가 핫스팟으로 이식 |
| **IBM J9 / OpenJ9** | IBM → Eclipse | 엔터프라이즈 환경 최적화, AOT 컴파일, 공유 클래스 캐시 | Eclipse OpenJ9로 오픈소스 전환. 여전히 활발 |
| **Azul Zing / Zulu Prime** | Azul Systems | C4 GC(무정지 GC), ReadyNow(워밍업 제거) | 상용. 초저지연 요구사항 환경에서 사용 |
| **GraalVM** | 오라클 Labs | 다중 언어(Polyglot), 네이티브 이미지(AOT), Graal JIT | 오픈소스 CE + 상용 EE. 차세대 JVM의 핵심 |
| **Android Runtime (ART)** | 구글 | DEX 바이트코드, AOT + JIT 혼합 | 안드로이드 전용. JVM 스펙 비준수 |

#### 핫스팟이 표준이 된 이유

```
성능 비결: 적응형 최적화 (Adaptive Optimization)
                                                    
  바이트코드 실행     프로파일링       JIT 컴파일
  ┌──────────┐     ┌──────────┐    ┌──────────────┐
  │ 인터프리터 │ ──→ │  핫스팟   │ ──→│ C1 (빠른     │
  │ 로 시작   │     │  탐지    │    │  컴파일)     │
  └──────────┘     └──────────┘    └──────┬───────┘
                                          │
                                          ▼
                                   ┌──────────────┐
                                   │ C2 (최적화    │
                                   │  컴파일)     │
                                   └──────────────┘
                                          │
                                          ▼
                                   네이티브 코드
```

핫스팟 VM은 "모든 코드를 미리 컴파일하지 않고, **자주 실행되는 핫 코드만 선별적으로 최적화**"한다는 철학을 가지고 있다. 이 접근법이 성공한 이유는:

1. **인터프리터로 빠른 시작**: 애플리케이션 시작 시 컴파일 대기 없이 즉시 실행
2. **프로파일링 데이터 축적**: 실행 중 어떤 메서드가 자주 호출되는지, 어떤 분기를 타는지 정보 수집
3. **계층형 JIT 컴파일**: C1(빠르지만 덜 최적화) → C2(느리지만 고도 최적화)로 단계적 컴파일
4. **역최적화(Deoptimization)**: 최적화 가정이 깨지면 인터프리터로 돌아가서 다시 시작

> **log-friends와의 연결:** `MethodTraceInterceptor`가 `@Service` 메서드 중 10ms 이상 걸린 것만 기록하는 임계값 필터는, 핫스팟의 "핫 코드 선별" 철학과 유사하다. 모든 것을 기록하면 오버헤드가 크므로, 의미 있는 것만 선택적으로 관측한다.

#### GraalVM 상세

GraalVM은 단순한 JVM이 아니라 **범용 가상 머신 플랫폼**이다.

```
+-------------------------------------------------------+
|                      GraalVM                          |
|  +----------------+  +----------------------------+   |
|  | Graal JIT      |  | Truffle Framework          |   |
|  | (Java로 작성된 |  | (언어 인터프리터 프레임워크)|   |
|  |  JIT 컴파일러) |  |                            |   |
|  +----------------+  +----------------------------+   |
|          |                       |                    |
|     JVM 위에서              다중 언어 지원            |
|     C2 대체 가능         JS, Python, Ruby,            |
|                          R, LLVM 기반 언어            |
|  +--------------------------------------------------+ |
|  | Native Image (AOT 컴파일)                         | |
|  | → JVM 없이 단독 실행 가능한 바이너리 생성         | |
|  +--------------------------------------------------+ |
+-------------------------------------------------------+
```

GraalVM의 두 가지 핵심 가치:

1. **Graal JIT 컴파일러**: Java로 작성된 JIT 컴파일러. C2를 대체하여 더 공격적인 최적화 수행
2. **Native Image**: AOT(Ahead-Of-Time) 컴파일로 JVM 없이 실행되는 네이티브 바이너리 생성. 시작 시간 밀리초 단위, 메모리 사용량 대폭 감소

---

### 핵심 개념 4 --- 자바 기술의 미래

저우즈밍이 제시한 자바의 미래 방향을 현재(2026년) 시점에서 재평가한다.

#### 1. 언어 독립 --- JVM 위의 다중 언어

JVM 스펙은 "자바 언어"가 아니라 "바이트코드"를 정의한다. 따라서 바이트코드를 생성할 수 있는 **모든 언어**가 JVM 위에서 동작한다.

```
   Kotlin     Scala     Groovy    Clojure    JRuby
     |          |         |          |         |
     +----+-----+----+----+----+-----+----+---+
          |          |         |          |
          ▼          ▼         ▼          ▼
       .class     .class    .class     .class
       파일들      파일들     파일들      파일들
          |          |         |          |
          +----+-----+----+---+----+-----+
               |          |         |
               ▼          ▼         ▼
         +-------------------------------------+
         |            JVM (HotSpot)            |
         |  클래스 로더 → 바이트코드 검증 →     |
         |  인터프리터/JIT → 네이티브 실행      |
         +-------------------------------------+
```

> **log-friends와의 연결:** log-friends-sdk가 **Kotlin**으로 작성되었지만 Spring Boot(Java) 애플리케이션을 계측할 수 있는 이유가 바로 이것이다. Kotlin 코드도 결국 `.class` 바이트코드가 되며, ByteBuddy는 바이트코드 수준에서 동작하므로 원래 소스 언어와 무관하게 계측이 가능하다.

#### 2. 차세대 JIT 컴파일러 --- Graal

핫스팟의 C2 컴파일러는 C++로 작성되어 유지보수가 어렵다. Graal 컴파일러는 Java(정확히는 자기 자신이 JIT할 수 있는 Java)로 작성되어:

- JIT 컴파일러 자체가 JIT 최적화를 받을 수 있음
- 새로운 최적화 기법 추가가 용이
- JVMCI(JVM Compiler Interface)를 통해 핫스팟에 플러그인으로 장착

```bash
# Graal JIT 컴파일러를 핫스팟에서 사용하기
java -XX:+UnlockExperimentalVMOptions -XX:+UseJVMCICompiler -jar app.jar
```

#### 3. 네이티브 시대 --- GraalVM Native Image

클라우드 네이티브 환경에서 JVM의 워밍업 시간과 메모리 사용량은 약점이다. Native Image는 이를 해결한다.

```
                  JVM 모드              Native Image 모드
시작 시간        수 초 (워밍업 포함)      수십 밀리초
최대 처리량      높음 (JIT 최적화 후)     중간 (AOT 한계)
메모리 사용      높음 (JVM 오버헤드)      낮음 (필요한 것만 포함)
빌드 시간        빠름                    느림 (정적 분석 필요)
리플렉션         자유롭게 사용            설정 파일로 명시 필요
동적 프록시      자유롭게 사용            설정 파일로 명시 필요
```

> **주의:** Native Image의 "리플렉션/동적 프록시 제한"은 log-friends-sdk 같은 ByteBuddy 기반 도구에 직접적 영향을 미친다. ByteBuddy는 런타임 바이트코드 생성에 의존하므로, Native Image 환경에서는 다른 접근(빌드 타임 계측)이 필요하다.

#### 4. 유연한 뚱뚱이 --- Project Valhalla (값 타입)

JVM의 모든 객체는 힙에 할당되고, 객체 헤더(16바이트)를 가진다. 작은 데이터(좌표, 금액 등)도 예외가 아니다. Valhalla는 **Value Type**을 도입해 객체 헤더 없이 스택/인라인에 저장 가능하게 한다.

```java
// 기존: int 래핑 → 힙 할당 + 객체 헤더 16바이트
record Point(int x, int y) {}  // 실제 데이터 8바이트 + 헤더 16바이트

// Valhalla Value Type (프리뷰): 인라인 저장
value record Point(int x, int y) {}  // 실제 데이터 8바이트만
```

#### 5. 동시성의 혁명 --- Project Loom (가상 스레드)

JDK 21에서 정식 도입된 가상 스레드는 자바 동시성 모델의 근본적 변화다.

```
전통적 스레드 모델:
  Java Thread 1:1 OS Thread (커널 스레드)
  → OS 스레드 수천 개 = 컨텍스트 스위칭 비용 폭증

가상 스레드 모델:
  Virtual Thread M:N Platform Thread(Carrier)
  → 가상 스레드 100만 개도 플랫폼 스레드 소수로 처리

  +--Virtual Thread 1--+  +--VT 2--+  +--VT 3--+  ...  +--VT 100만--+
          |                   |           |                    |
          +-------+-----------+-----------+----+---------------+
                  |                            |
          +--Carrier Thread 1--+    +--Carrier Thread 2--+
                  |                            |
          +--OS Thread 1-------+    +--OS Thread 2-------+
```

```kotlin
// log-friends BatchTransporter에서 가상 스레드 활용 가능성
val executor = Executors.newVirtualThreadPerTaskExecutor()
executor.submit {
    kafkaProducer.send(record)  // I/O 대기 시 자동으로 Carrier에서 내려옴
}
```

#### 6. 문법 개선 --- Project Amber

| 기능 | JDK 버전 | 설명 |
|------|---------|------|
| Local Variable Type Inference (`var`) | 10 | `var list = new ArrayList<String>()` |
| Switch Expressions | 14 | switch가 값을 반환 |
| Text Blocks | 15 | `"""멀티라인 문자열"""` |
| Records | 16 | 불변 데이터 클래스 자동 생성 |
| Sealed Classes | 17 | 상속 계층 제한 |
| Pattern Matching for instanceof | 16 | 타입 검사 + 캐스팅 한 번에 |
| Record Patterns | 21 | 레코드 구조 분해 |
| String Templates | 21 (프리뷰) | `STR."Hello \{name}"` |

#### 7. 네이티브 접근 --- Project Panama

JNI(Java Native Interface)를 대체하는 Foreign Function & Memory API로, C/C++ 라이브러리를 안전하고 효율적으로 호출할 수 있다.

---

### 이 프로젝트(log-friends)와의 연결

#### 1. JDK 21 타겟과 가상 스레드 활용 가능성

log-friends-sdk는 JDK 21을 타겟으로 빌드된다. 이는 프로젝트 차원에서 다음을 의미한다:

```
log-friends-sdk가 JDK 21에서 얻는 것들:
                                                    
  +-- 가상 스레드 (Project Loom) ──────────────────+
  |   BatchTransporter의 Kafka 전송이 I/O 바운드    |
  |   → 가상 스레드로 처리량 대폭 향상 가능         |
  +------------------------------------------------+
                                                    
  +-- Generational ZGC ────────────────────────────+
  |   SDK가 대상 앱에 주는 GC 영향 최소화           |
  |   → 에이전트로 인한 Stop-the-World 감소         |
  +------------------------------------------------+
                                                    
  +-- Pattern Matching 확장 ───────────────────────+
  |   이벤트 타입별 분기 처리 코드 간결화            |
  +------------------------------------------------+
```

#### 2. ByteBuddy와 핫스팟 VM의 Instrumentation API

ByteBuddy는 핫스팟 VM이 제공하는 `java.lang.instrument` 패키지에 의존한다. 이 API의 핵심은:

```
JVM 시작
    │
    ▼
┌─ Instrumentation API ─────────────────────────────┐
│                                                    │
│  premain() 또는 agentmain()                        │
│       │                                            │
│       ▼                                            │
│  ClassFileTransformer 등록                          │
│       │                                            │
│       ▼                                            │
│  클래스 로딩 시 바이트코드 변환                      │
│  (= ByteBuddy가 하는 일)                           │
│       │                                            │
│       ▼                                            │
│  변환된 바이트코드를 JIT 컴파일                      │
│  (핫스팟의 C1/C2가 최적화)                          │
│                                                    │
└────────────────────────────────────────────────────┘
```

log-friends-sdk의 `LogFriendsInstaller`가 `EnvironmentPostProcessor`를 통해 ByteBuddy를 설치하는 과정은 정확히 이 Instrumentation API 위에서 동작한다. JVM 옵션 `-Djdk.attach.allowAttachSelf=true`가 필요한 이유도 핫스팟의 Attach API를 통해 런타임에 에이전트를 로드하기 때문이다.

#### 3. Kotlin이 JVM 위에서 동작하는 원리

```
Kotlin 소스 (.kt)
       │
       ▼
  kotlinc (Kotlin 컴파일러)
       │
       ▼
  .class 바이트코드 ──── Java의 .class와 동일한 포맷
       │
       ▼
  JVM 클래스 로더가 로드
       │
       ▼
  핫스팟이 JIT 컴파일
       │
       ▼
  네이티브 코드로 실행
```

Kotlin 컴파일러가 생성하는 바이트코드는 Java 컴파일러가 생성하는 것과 동일한 `.class` 포맷이다. 따라서:

- ByteBuddy가 Kotlin 코드를 계측할 때 Java 코드와 **동일한 메커니즘** 사용
- Kotlin의 `suspend` 함수도 바이트코드 수준에서는 상태 머신으로 변환되므로 계측 가능
- `@Service` 어노테이션이 Kotlin 클래스에 붙어 있어도 `MethodTraceInterceptor`가 동일하게 동작

---

### 실습

#### 실습 1: JVM 정보 확인

```bash
# 현재 사용 중인 JVM 확인
java -version

# 출력 예시:
# openjdk version "21.0.2" 2024-01-16
# OpenJDK Runtime Environment (build 21.0.2+13-58)
# OpenJDK 64-Bit Server VM (build 21.0.2+13-58, mixed mode, sharing)
#                           ^^^^^^^^^^^^^^^^^^^^^^
#                           HotSpot VM임을 확인

# JVM 상세 정보
java -XshowSettings:all -version 2>&1 | head -50
```

#### 실습 2: 바이트코드 확인 --- Java와 Kotlin 비교

```bash
# Java 파일 컴파일 후 바이트코드 확인
echo 'public class Hello { public static void main(String[] args) { System.out.println("Hello"); } }' > Hello.java
javac Hello.java
javap -c Hello.class

# Kotlin 파일 컴파일 후 바이트코드 확인
echo 'fun main() { println("Hello") }' > Hello.kt
kotlinc Hello.kt -include-runtime -d hello.jar
javap -c -classpath hello.jar HelloKt
```

> 두 바이트코드를 비교하면 구조가 매우 유사함을 확인할 수 있다. 이것이 "JVM 언어 독립성"의 실체다.

#### 실습 3: OpenJDK 소스 빌드 (선택)

```bash
# macOS 기준 (Homebrew 필요)
# 1. 의존성 설치
brew install autoconf freetype

# 2. OpenJDK 소스 클론
git clone https://github.com/openjdk/jdk.git
cd jdk

# 3. 빌드 설정
bash configure \
  --with-target-bits=64 \
  --with-debug-level=slowdebug \
  --with-jvm-variants=server

# 4. 빌드 실행 (시간 소요: 30분~1시간)
make images

# 5. 빌드된 JDK 확인
./build/macosx-aarch64-server-slowdebug/jdk/bin/java -version
```

> **slowdebug** 빌드를 사용하면 JVM 내부 동작을 디버거로 추적할 수 있다. 이후 장에서 GC나 JIT 동작을 분석할 때 유용하다.

#### 실습 4: log-friends-sdk에서 VM 정보 로깅

```kotlin
// 실제 프로젝트에서 JVM 정보를 확인하는 코드
fun printJvmInfo() {
    val runtime = Runtime.getRuntime()
    println("JVM Name: ${System.getProperty("java.vm.name")}")
    println("JVM Version: ${System.getProperty("java.vm.version")}")
    println("JVM Vendor: ${System.getProperty("java.vm.vendor")}")
    println("Available Processors: ${runtime.availableProcessors()}")
    println("Max Memory: ${runtime.maxMemory() / 1024 / 1024}MB")
    println("Attach Self Allowed: ${System.getProperty("jdk.attach.allowAttachSelf")}")
}
```

---

### 핵심 질문

#### Q1. 핫스팟 VM이 JDK 1.3부터 기본이 된 이유는?

**A:** 핫스팟 VM은 "적응형 최적화(Adaptive Optimization)" 전략을 채택했다. 프로그램 실행 중 **프로파일링 데이터를 수집**하여 자주 실행되는 핫 코드만 선별적으로 JIT 컴파일한다. 이 방식이 Classic VM의 인터프리터 전용 방식이나 Exact VM의 전체 컴파일 방식보다 **시작 속도와 최대 처리량 모두**에서 균형잡힌 성능을 보였기 때문이다. 특히 인터프리터 → C1 → C2로 이어지는 계층형 컴파일은 워밍업 시간과 최대 성능 사이의 트레이드오프를 잘 해결했다.

#### Q2. JVM 스펙은 자바 언어를 전제하지 않는데, 왜 "자바" 가상 머신이라 부르는가?

**A:** 역사적 이유다. JVM은 자바 언어와 함께 태어났으므로 이름에 "자바"가 들어갔지만, JVM 스펙(JVMS)은 **바이트코드 포맷과 실행 규칙**만 정의한다. 소스 언어에 대한 가정이 없다. 실제로 JDK 7에서 추가된 `invokedynamic` 명령어는 자바보다 **동적 언어(JRuby, Groovy)**를 위해 도입되었으며, Kotlin, Scala, Clojure 등이 JVM 위에서 성공적으로 동작하는 것이 이를 증명한다.

#### Q3. GraalVM Native Image가 모든 자바 앱에 적합하지 않은 이유는?

**A:** Native Image는 **클로즈드 월드 가정(Closed-World Assumption)**에 기반한다. AOT 컴파일 시점에 도달 가능한 모든 코드를 정적 분석으로 파악해야 한다. 따라서:
- **리플렉션**: 런타임에 동적으로 클래스를 로드하는 코드는 빌드 시 설정 파일로 명시해야 함
- **동적 프록시**: `java.lang.reflect.Proxy`로 런타임 생성하는 프록시도 사전 선언 필요
- **바이트코드 생성**: ByteBuddy, CGLIB 같은 런타임 바이트코드 생성 라이브러리는 원칙적으로 동작 불가
- **클래스 로더**: 커스텀 클래스 로더 제한

log-friends-sdk처럼 ByteBuddy로 런타임 계측하는 도구는 Native Image와 근본적으로 충돌한다.

#### Q4. 오라클의 썬 인수가 자바 생태계에 미친 영향은?

**A:** 양면적이다. **긍정적으로는** JRockit의 기술(Flight Recorder, Mission Control)이 핫스팟에 통합되어 모니터링 역량이 강화되었고, 6개월 릴리즈 주기로 혁신 속도가 빨라졌다. **부정적으로는** Oracle JDK의 유료 라이선스 정책(2019~)이 혼란을 야기했고, Java EE의 발전이 정체되어 결국 Eclipse 재단으로 이관(Jakarta EE)되었다. 하지만 이 위기가 OpenJDK 생태계(Amazon Corretto, Azul Zulu, Adoptium 등)의 다양화를 촉진한 역설적 결과를 낳았다.

#### Q5. `invokedynamic`은 왜 중요하며 log-friends와 어떤 관련이 있는가?

**A:** `invokedynamic`(indy)은 JDK 7에서 도입된 바이트코드 명령어로, **호출 대상을 런타임에 결정**할 수 있게 한다. 자바의 람다 표현식(JDK 8)이 내부적으로 indy를 사용하며, Kotlin의 람다도 마찬가지다. ByteBuddy 역시 `@Advice` 어노테이션 기반의 계측 코드를 위빙할 때 indy를 활용할 수 있다. log-friends-sdk의 인터셉터들이 바이트코드에 삽입하는 어드바이스 코드도 이 메커니즘의 혜택을 받는다.

#### Q6. JDK 21의 가상 스레드가 기존 스레드 풀 방식보다 유리한 시나리오는?

**A:** **I/O 바운드 작업이 많은 서버 애플리케이션**에서 압도적으로 유리하다. 전통적 스레드 풀(200~500개)은 모든 스레드가 I/O 대기 상태이면 더 이상 요청을 처리할 수 없다. 가상 스레드는 I/O 대기 시 캐리어 스레드에서 자동으로 언마운트되어 다른 가상 스레드가 사용할 수 있게 한다. log-friends의 `BatchTransporter`가 Kafka에 메시지를 전송할 때 네트워크 I/O가 발생하는데, 가상 스레드를 사용하면 소수의 OS 스레드로도 높은 동시 전송량을 달성할 수 있다. 단, **CPU 바운드 작업**에서는 가상 스레드의 이점이 없다.

#### Q7. Java SE, EE, ME 에디션 구분이 현재도 의미 있는가?

**A:** **기술적으로는 여전히 유효하지만 경계가 흐려지고 있다.** Java ME는 사실상 퇴장했고, Java EE는 Jakarta EE로 이관되어 Spring Boot 같은 프레임워크가 이를 대체하고 있다. 하지만 서블릿 스펙(`jakarta.servlet`), JPA(`jakarta.persistence`), CDI(`jakarta.inject`) 등 Jakarta EE의 핵심 스펙은 Spring 생태계에서 여전히 기반 기술로 사용된다. log-friends-sdk가 `DispatcherServlet`을 계측하는 것도 이 서블릿 스펙이 살아있기 때문이다.

---

### 학습 완료 체크리스트

- [ ] JDK, JRE, JVM의 포함 관계와 각각의 역할을 설명할 수 있다
- [ ] Java SE / EE / ME 에디션의 차이와 현재 상태를 설명할 수 있다
- [ ] JDK 역사에서 5개 이상의 주요 변곡점과 그 이유를 설명할 수 있다
- [ ] 핫스팟 VM의 적응형 최적화 원리를 설명할 수 있다
- [ ] GraalVM의 두 가지 핵심 가치(Graal JIT, Native Image)를 구분하여 설명할 수 있다
- [ ] 가상 스레드(Project Loom)의 동작 원리와 적합한 사용 시나리오를 설명할 수 있다
- [ ] ByteBuddy가 핫스팟의 Instrumentation API에 의존하는 이유를 설명할 수 있다
- [ ] Kotlin이 JVM 위에서 동작하는 원리와 언어 독립성의 관계를 설명할 수 있다
- [ ] `java -version` 출력 결과에서 VM 종류, 빌드 모드를 식별할 수 있다
- [ ] Native Image 환경에서 ByteBuddy 같은 런타임 바이트코드 생성이 제한되는 이유를 설명할 수 있다
