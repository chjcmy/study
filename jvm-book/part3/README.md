# 3부: 가상 머신 실행 서브시스템

> **예상 학습 시간:** 15~20시간 (4개 장)
>
> 이 파트는 JVM이 `.class` 파일을 어떻게 읽고, 로딩하고, 실행하는지를 다룬다.
> log-friends SDK가 ByteBuddy로 바이트코드를 계측하는 원리를 이해하는 데 가장 핵심적인 부분이다.

---

## 챕터 목록

| 장 | 제목 | 핵심 주제 |
|---|---|---|
| [6장](./ch06/README.md) | 클래스 파일 구조 | 바이너리 포맷, 상수 풀, 바이트코드 명령어 |
| [7장](./ch07/README.md) | 가상 머신 클래스 로딩 메커니즘 | 로딩 5단계, 부모 위임 모델, 모듈 시스템 |
| [8장](./ch08/README.md) | 가상 머신 바이트코드 실행 엔진 | 스택 프레임, 메서드 디스패치, invokedynamic |
| [9장](./ch09/README.md) | 클래스 로딩 및 실행 서브시스템 사례와 실전 | 톰캣/Spring Boot 클래스 로더, 동적 프락시 비교 |

---

## 각 장 요약

### [6장: 클래스 파일 구조](./ch06/README.md)

`.class` 파일의 바이너리 구조 전체를 다룬다. 매직 넘버(`0xCAFEBABE`), 상수 풀, 필드/메서드 테이블, 속성 테이블의 구성을 학습한다. 5가지 메서드 호출 명령어(`invokevirtual`, `invokespecial`, `invokestatic`, `invokeinterface`, `invokedynamic`)의 차이가 핵심이다. ByteBuddy가 상수 풀에 인터셉터 레퍼런스를 추가하고 Code 속성을 재작성하는 원리와 직결된다.

### [7장: 가상 머신 클래스 로딩 메커니즘](./ch07/README.md)

클래스 로딩의 5단계(로딩 → 검증 → 준비 → 해석 → 초기화)와 능동적/수동적 사용의 차이를 다룬다. 부모 위임 모델(Bootstrap → Platform → Application → Custom)의 동작 원리와 세 차례 "파괴" 사례(SPI, OSGi), JDK 9 모듈 시스템까지 학습한다. `LogFriendsInstaller`가 `EnvironmentPostProcessor` 시점에 ByteBuddy를 설치하는 이유와 연결된다.

### [8장: 가상 머신 바이트코드 실행 엔진](./ch08/README.md)

메서드 호출마다 생성되는 스택 프레임(지역 변수 테이블, 피연산자 스택, 동적 링크, 반환 주소)의 구조를 다룬다. 정적 디스패치(오버로딩)와 동적 디스패치(오버라이딩)의 바이트코드 수준 차이, vtable을 통한 가상 메서드 탐색, `invokedynamic`의 Bootstrap Method → CallSite → MethodHandle 흐름을 학습한다. `@SuperCall Callable`의 동작 원리와 직결된다.

### [9장: 클래스 로딩 및 실행 서브시스템 사례와 실전](./ch09/README.md)

톰캣의 WebAppClassLoader(부모 위임 역전)와 Spring Boot의 LaunchedURLClassLoader(Nested JAR 처리)를 실제 사례로 분석한다. JDK 동적 프락시, CGLIB, ByteBuddy Agent 세 가지 접근법을 클래스 파일 생성 관점에서 비교하고, log-friends가 ByteBuddy Agent를 선택한 이유(코드 수정 없는 투명 계측)를 정리한다. `RETRANSFORMATION` 전략의 필요성도 다룬다.

---

## log-friends와의 연결 포인트

| 장 | 연결 포인트 | SDK 코드 |
|---|---|---|
| **6장** | ByteBuddy가 상수 풀에 인터셉터 레퍼런스를 추가하고 Code 속성을 재작성 | `InstrumentationRegistry` 전체 |
| **7장** | `EnvironmentPostProcessor` 시점에 agent 설치 → 클래스 로딩 전에 transformer 등록 | `LogFriendsInstaller.postProcessEnvironment()` |
| **7장** | Spring Boot `LaunchedURLClassLoader`가 `BOOT-INF/lib/`에서 SDK 로딩 | `spring.factories` / `AutoConfiguration` |
| **8장** | `invokevirtual`로 `doService()` 호출 시 계측된 버전이 실행됨 (vtable 갱신) | `SpringInterceptor.intercept()` |
| **8장** | `@SuperCall Callable`이 MethodHandle 기반으로 원본 메서드 호출 | `callable.call()` in `SpringInterceptor` |
| **9장** | JDK Proxy/CGLIB와 달리 기존 클래스를 직접 수정 → 코드 수정 불필요 | `AgentBuilder.RETRANSFORMATION` |
