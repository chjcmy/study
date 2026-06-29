# JVM 학습 로드맵: Log Friends 기준

목표는 JVM 전체를 한 번에 공부하는 것이 아니다.

Log Friends SDK를 안전하게 만들기 위해 필요한 JVM 지식을 먼저 본다.

```text
기준:
대상 앱 JVM 안에서 SDK가 같이 돈다.
따라서 SDK queue, payload, interceptor, flush thread도 대상 앱 heap과 thread 자원을 쓴다.
```

## 1. JVM 메모리 구조

먼저 봐야 할 것:

- heap
- stack
- stack frame
- method call
- object reference
- GC 기본 흐름

Log Friends와 연결되는 질문:

- `args: Array<Any?>`는 어디에 존재하는가?
- `AgentEvent`, payload map, JSON 문자열은 heap을 얼마나 쓰는가?
- Console down 상태에서 queue가 커지면 왜 사용자 앱 heap에 영향을 주는가?
- bounded queue가 왜 필요한가?

관련 코드:

- `LogEventInterceptor.kt`
- `BatchTransporter.kt`
- `AgentEventFactory.kt`
- `EventJsonWriter.kt`

## 2. JVM thread와 executor

다음으로 봐야 할 것:

- Java thread
- daemon thread
- `ScheduledExecutorService`
- executor 내부 task queue
- blocking call과 timeout

Log Friends와 연결되는 질문:

- flush thread는 사용자 요청 thread와 분리되어 있는가?
- HTTP timeout 동안 flush thread가 막히면 어떤 일이 생기는가?
- event queue는 bounded인데 flush task queue가 쌓이면 왜 위험한가?
- shutdown hook에서 flush를 시도할 때 앱 종료가 얼마나 지연될 수 있는가?

관련 코드:

- `BatchTransporter.kt`
- `IngestHttpClient.kt`
- `LogFriendsAutoConfiguration.kt`

## 3. Annotation은 실행기가 아니라 metadata

봐야 할 것:

- Kotlin/Java annotation
- `@Target`
- `@Retention`
- runtime annotation
- reflection으로 annotation 읽기
- method parameter annotation

Log Friends와 연결되는 질문:

- `@LogEvent`는 혼자 실행되는가?
- `@LogField`는 어디에 남아 있는가?
- `method.getAnnotation(LogEvent::class.java)`는 언제 가능한가?
- runtime retention이 아니면 scanner가 읽을 수 있는가?

관련 코드:

- `LogEvent.kt`
- `LogField.kt`
- `LogMasked.kt`
- `LogEventInterceptor.kt`
- `DiscoveredLogEventScanner.kt`

## 4. ByteBuddy와 Java Instrumentation

봐야 할 것:

- `java.lang.instrument.Instrumentation`
- dynamic attach
- class transform
- method delegation
- matcher
- `@Origin`
- `@AllArguments`
- `@SuperCall`

Log Friends와 연결되는 질문:

- ByteBuddy는 annotation을 실행하는가, method call 사이에 들어가는가?
- `callable.call()`은 원래 메서드를 어떻게 계속 실행시키는가?
- 원래 메서드에서 예외가 나면 interceptor는 어디까지 실행되는가?
- 이미 로드된 class와 아직 로드되지 않은 class는 어떻게 다르게 다뤄지는가?

관련 코드:

- `LogFriendsInstaller.kt`
- `InstrumentationRegistry.kt`
- `SpringInterceptor.kt`
- `LogEventInterceptor.kt`
- `JdbcInterceptor.kt`
- `MethodTraceInterceptor.kt`

## 5. Class loading

봐야 할 것:

- class loader
- application class loader
- class loading timing
- `instrumentation.allLoadedClasses`
- startup 이후 새 class load

Log Friends와 연결되는 질문:

- Discovered LogEvent scanner가 모든 `@LogEvent`를 항상 찾을 수 있는가?
- 왜 startup report를 전체 후보 report로 잡았는가?
- class loading 지연 때문에 삭제 자동화가 위험한 이유는 무엇인가?

관련 코드:

- `DiscoveredLogEventScanner.kt`
- `LogFriendsAutoConfiguration.kt`

## 6. Serialization과 payload size

봐야 할 것:

- object to JSON
- primitive / DTO / collection
- circular reference
- payload size
- masking

Log Friends와 연결되는 질문:

- return 값을 `_outcome.result`에 넣을 때 어디까지 직렬화할 것인가?
- DTO 전체를 저장하면 왜 위험한가?
- 개인정보가 섞인 값은 어떻게 mask할 것인가?
- payload가 커지면 queue와 heap에 어떤 영향을 주는가?

관련 코드:

- `EventJsonWriter.kt`
- `AgentEventFactory.kt`
- `LogMasker.kt`
- `log-outcome.md`

## 지금 당장 볼 순서

1차 구현 기준으로는 아래 순서가 가장 효율적이다.

```text
1. JVM heap / stack / object reference
2. Thread / executor / timeout
3. Annotation runtime metadata
4. ByteBuddy method delegation
5. Class loading
6. Serialization / payload size
```

지금 가장 직접적인 학습 주제:

```text
Console down 상황에서
SDK queue와 flush task가
대상 앱 JVM heap/thread에 어떤 영향을 주는가
```

이걸 이해하면 Log Friends SDK의 1차 runtime policy를 설명할 수 있다.

## 면접/지원서 연결 문장

컨포트랩용으로는 이렇게 말할 수 있다.

> Log Friends SDK를 만들며 단순히 Spring Boot 기능을 사용하는 것을 넘어, SDK가 대상 애플리케이션 JVM 안에서 같은 heap과 thread 자원을 사용한다는 점을 기준으로 설계했습니다. 그래서 bounded queue, batch flush, timeout, drop policy를 두어 Console 장애가 사용자 애플리케이션 안정성으로 번지지 않게 하는 방향을 선택했습니다.

## 관련 문서

- [[sdk/runtime-policy]]
- [[sdk/instrumentation]]
- [[sdk/log-event]]
- [[sdk/log-outcome]]
- [[velog/03-sdk-runtime-policy]]
- [[velog/05-jvm-stack-heap-interceptor-learning]]
- [[velog/15-annotation-bytebuddy-jvm-sdk-boundary]]
