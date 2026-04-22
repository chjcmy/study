# 3부: 가상 머신 실행 서브시스템

> **예상 학습 시간:** 15~20시간 (4개 장)
>
> 이 파트는 JVM이 `.class` 파일을 어떻게 읽고, 로딩하고, 실행하는지를 다룬다.
> log-friends SDK가 ByteBuddy로 바이트코드를 계측하는 원리를 이해하는 데 가장 핵심적인 부분이다.

---

## 6장: 클래스 파일 구조

### 핵심 개념 --- 클래스 파일 바이너리 구조

Java/Kotlin 소스 코드는 컴파일러를 거쳐 `.class` 파일이 된다. 이 파일은 **플랫폼 독립적인 바이너리 포맷**으로, JVM 명세에 의해 바이트 단위까지 정밀하게 정의되어 있다.

**클래스 파일 전체 구조:**

```
ClassFile {
    u4             magic;                  // 0xCAFEBABE
    u2             minor_version;
    u2             major_version;          // Java 21 = 65
    u2             constant_pool_count;
    cp_info        constant_pool[];        // 상수 풀 (1 ~ count-1)
    u2             access_flags;           // public, final, abstract, ...
    u2             this_class;             // 상수 풀 인덱스
    u2             super_class;
    u2             interfaces_count;
    u2             interfaces[];
    u2             fields_count;
    field_info     fields[];               // 필드 테이블
    u2             methods_count;
    method_info    methods[];              // 메서드 테이블
    u2             attributes_count;
    attribute_info attributes[];           // 속성 테이블
}
```

**각 구성 요소 상세:**

| 구성 요소 | 크기 | 설명 |
|---|---|---|
| **매직 넘버** | 4바이트 | `0xCAFEBABE` — 클래스 파일임을 식별하는 고정값 |
| **버전** | 4바이트 | minor + major. JVM은 자신의 버전 이하만 실행 가능 |
| **상수 풀** | 가변 | 리터럴, 클래스/메서드/필드의 심볼릭 레퍼런스 저장. 인덱스 1부터 시작 |
| **접근 플래그** | 2바이트 | `ACC_PUBLIC(0x0001)`, `ACC_FINAL(0x0010)`, `ACC_SUPER(0x0020)`, `ACC_ABSTRACT(0x0400)` 등 |
| **클래스 인덱스** | 2+2바이트 | this_class, super_class — 상수 풀의 `CONSTANT_Class_info` 참조 |
| **인터페이스 테이블** | 가변 | 구현하는 인터페이스들의 상수 풀 인덱스 |
| **필드 테이블** | 가변 | 각 필드의 접근 플래그, 이름, 디스크립터, 속성 |
| **메서드 테이블** | 가변 | 각 메서드의 접근 플래그, 이름, 디스크립터, Code 속성(바이트코드) |
| **속성 테이블** | 가변 | Code, LineNumberTable, SourceFile, StackMapTable 등 |

**상수 풀 주요 태그:**

| 태그 | 값 | 설명 |
|---|---|---|
| `CONSTANT_Utf8` | 1 | UTF-8 문자열 |
| `CONSTANT_Integer` | 3 | int 리터럴 |
| `CONSTANT_Long` | 5 | long 리터럴 (2슬롯) |
| `CONSTANT_Class` | 7 | 클래스/인터페이스 심볼릭 레퍼런스 |
| `CONSTANT_String` | 8 | String 리터럴 |
| `CONSTANT_Fieldref` | 9 | 필드 레퍼런스 |
| `CONSTANT_Methodref` | 10 | 메서드 레퍼런스 |
| `CONSTANT_InterfaceMethodref` | 11 | 인터페이스 메서드 레퍼런스 |
| `CONSTANT_NameAndType` | 12 | 이름 + 디스크립터 |
| `CONSTANT_MethodHandle` | 15 | 메서드 핸들 (invokedynamic) |
| `CONSTANT_InvokeDynamic` | 18 | invokedynamic 부트스트랩 정보 |

> **왜 중요한가:** ByteBuddy가 바이트코드를 계측할 때, 이 구조를 정확히 이해하고 상수 풀에 새로운 엔트리를 추가하며 메서드 테이블의 Code 속성을 수정한다. `ElementMatchers.named("org.springframework.web.servlet.DispatcherServlet")`은 상수 풀의 `CONSTANT_Class` 엔트리를 매칭하는 것이다.

### 핵심 개념 --- 바이트코드 명령어 분류

바이트코드는 1바이트 opcode + 피연산자로 구성된다. 총 256개 슬롯 중 약 200개가 정의되어 있다.

**주요 명령어 분류표:**

| 분류 | 주요 명령어 | 설명 |
|---|---|---|
| **로드/스토어** | `iload`, `aload`, `istore`, `astore` | 지역 변수 <-> 피연산자 스택 |
| **산술** | `iadd`, `isub`, `imul`, `idiv`, `lmul` | 정수/실수 연산 |
| **형 변환** | `i2l`, `i2f`, `l2d`, `checkcast` | 타입 캐스팅 |
| **객체 생성** | `new`, `newarray`, `anewarray` | 인스턴스/배열 생성 |
| **필드 접근** | `getfield`, `putfield`, `getstatic`, `putstatic` | 인스턴스/클래스 필드 |
| **배열 접근** | `iaload`, `iastore`, `aaload`, `aastore` | 배열 원소 읽기/쓰기 |
| **스택 조작** | `pop`, `dup`, `swap` | 피연산자 스택 직접 조작 |
| **분기** | `ifeq`, `ifne`, `goto`, `tableswitch` | 조건/무조건 분기 |
| **비교** | `lcmp`, `fcmpl`, `dcmpg` | 비교 후 결과를 스택에 push |
| **메서드 호출** | `invokevirtual`, `invokespecial`, `invokestatic`, `invokeinterface`, `invokedynamic` | 5가지 호출 명령어 |
| **반환** | `ireturn`, `areturn`, `return` | 메서드 반환 |
| **예외** | `athrow` | 예외 던지기 (catch는 예외 테이블로 처리) |
| **동기화** | `monitorenter`, `monitorexit` | synchronized 블록 |

**5가지 메서드 호출 명령어 (가장 중요):**

| 명령어 | 대상 | 예시 |
|---|---|---|
| `invokestatic` | 정적 메서드 | `Math.max()`, `Collections.sort()` |
| `invokespecial` | 생성자, private, super | `<init>`, `super.method()` |
| `invokevirtual` | 인스턴스 메서드 (가상 디스패치) | `obj.toString()`, `list.size()` |
| `invokeinterface` | 인터페이스 메서드 | `iterable.iterator()` |
| `invokedynamic` | 동적 호출 사이트 | 람다, 문자열 연결 (`+`) |

**javap 예시 --- 간단한 메서드 호출:**

```java
public class Hello {
    public String greet(String name) {
        return "Hello, " + name;
    }
}
```

```
$ javap -c Hello.class

public java.lang.String greet(java.lang.String);
  Code:
    0: aload_1                          // name을 스택에 로드
    1: invokedynamic #7, 0              // StringConcatFactory.makeConcatWithConstants
                                        // "Hello, \u0001" 템플릿으로 문자열 연결
    6: areturn                          // 결과 반환
```

> Java 9 이후 문자열 연결은 `StringBuilder`가 아닌 `invokedynamic`을 사용한다. 이는 런타임에 JVM이 최적 전략을 선택할 수 있게 해준다.

**javap 예시 --- synchronized 블록:**

```java
public void sync() {
    synchronized (this) {
        System.out.println("locked");
    }
}
```

```
$ javap -c

public void sync();
  Code:
    0: aload_0
    1: dup
    2: astore_1
    3: monitorenter              // 락 획득
    4: getstatic     #2          // System.out
    7: ldc           #3          // "locked"
    9: invokevirtual #4          // println
   12: aload_1
   13: monitorexit               // 정상 경로 락 해제
   14: goto          22
   17: astore_2
   18: aload_1
   19: monitorexit               // 예외 경로 락 해제 (finally 역할)
   20: aload_2
   21: athrow
   22: return
  Exception table:
    from  to  target  type
      4   14    17    any
     17   20    17    any
```

> `monitorenter`/`monitorexit`는 반드시 쌍으로 존재해야 하며, 컴파일러가 예외 경로에도 `monitorexit`을 삽입한다.

### 핵심 개념 --- 클래스 파일 구조의 진화

| 버전 | 주요 변화 |
|---|---|
| Java 1.1 | 내부 클래스, `InnerClasses` 속성 추가 |
| Java 5 | 제네릭(`Signature`), 어노테이션(`RuntimeVisibleAnnotations`), enum |
| Java 7 | `invokedynamic`, `BootstrapMethods` 속성 |
| Java 8 | 람다(invokedynamic 활용), `MethodParameters` 속성 |
| Java 9 | 모듈(`Module`, `ModulePackages` 속성) |
| Java 11 | Nest(`NestHost`, `NestMembers`) — private 접근 최적화 |
| Java 16 | `Record` 속성 |
| Java 17 | Sealed 클래스(`PermittedSubclasses`) |
| Java 21 | 가상 스레드 관련 최적화 (continuation) |

> 클래스 파일 포맷은 하위 호환성을 유지하면서 **속성(attribute)** 을 추가하는 방식으로 확장된다. 이것이 ByteBuddy 같은 도구가 여러 Java 버전에 걸쳐 작동할 수 있는 이유다.

---

## 7장: 클래스 로딩 메커니즘

### 핵심 개념 --- 클래스 로딩 시점

JVM 명세는 클래스를 **"처음 능동적으로 사용(active use)할 때"** 초기화하도록 규정한다.

**6가지 능동적 사용 (초기화 트리거):**

1. `new`, `getstatic`, `putstatic`, `invokestatic` 명령어를 만났을 때
2. `java.lang.reflect` 패키지로 리플렉션 호출할 때
3. 부모 클래스가 아직 초기화되지 않았을 때 (부모 먼저)
4. JVM 시작 시 main 메서드를 포함한 클래스
5. `MethodHandle` 최종 해석 결과가 `REF_getStatic`, `REF_putStatic`, `REF_invokeStatic`, `REF_newInvokeSpecial`일 때
6. `default` 메서드를 가진 인터페이스의 구현 클래스가 초기화될 때

**수동적 사용 (초기화되지 않는 경우):**

```java
// 부모 클래스의 정적 필드를 자식 클래스로 접근 → 부모만 초기화
class Parent { static int value = 42; }
class Child extends Parent {}
System.out.println(Child.value);  // Parent만 초기화됨

// 배열 정의 → 원소 클래스는 초기화되지 않음
Parent[] arr = new Parent[10];    // Parent 초기화 안 됨

// 컴파일 타임 상수 → 상수 풀로 전파되어 참조 클래스 초기화 안 됨
class Const { static final String NAME = "log-friends"; }
System.out.println(Const.NAME);   // Const 초기화 안 됨 (상수 풀에 직접 저장)
```

> **log-friends 연결:** `LogFriendsInstaller`는 `EnvironmentPostProcessor` 시점에 ByteBuddy를 설치한다. 이 시점은 아직 대부분의 애플리케이션 클래스가 로딩되기 전이므로, `RETRANSFORMATION` 전략으로 이미 로딩된 클래스와 앞으로 로딩될 클래스 모두를 계측할 수 있다.

### 핵심 개념 --- 클래스 로딩 5단계

```
   Loading ──→ Linking ──────────────────────→ Initialization ──→ Using ──→ Unloading
                 │
                 ├── Verification (검증)
                 ├── Preparation (준비)
                 └── Resolution (해석)
```

#### 1단계: 로딩 (Loading)

바이트 스트림을 얻어 메서드 영역에 런타임 데이터 구조로 저장하고, `java.lang.Class` 객체를 힙에 생성한다.

- 바이트 스트림의 출처: `.class` 파일, JAR/WAR, 네트워크, 런타임 생성(Proxy, ByteBuddy), 데이터베이스 등
- **비배열 클래스**: 클래스 로더의 `defineClass()`가 담당. 개발자가 커스터마이징 가능
- **배열 클래스**: JVM이 직접 생성. 원소 타입이 참조형이면 해당 클래스를 재귀 로딩

> **log-friends 연결:** ByteBuddy의 `AgentBuilder`는 `ClassFileTransformer`를 `Instrumentation`에 등록한다. 이후 클래스가 로딩될 때마다 JVM이 이 transformer를 호출하여, `type()` 매처에 부합하면 바이트코드를 변환한 후 `defineClass()`에 전달한다.

#### 2단계: 검증 (Verification)

로딩된 바이트 스트림이 JVM 명세에 부합하는지 검증한다. 4단계로 구성:

| 검증 단계 | 검증 내용 |
|---|---|
| **파일 형식 검증** | 매직 넘버, 버전, 상수 풀 태그, UTF-8 인코딩 |
| **메타데이터 검증** | 부모 클래스 존재 여부, final 상속 금지, 추상 메서드 구현 |
| **바이트코드 검증** | 타입 안전성, 스택 오버플로, 분기 대상 유효성 (StackMapTable 활용) |
| **심볼릭 레퍼런스 검증** | 접근 권한, 클래스/필드/메서드 존재 여부 |

> `-Xverify:none` (Java 13부터 deprecated)으로 검증을 생략할 수 있지만 권장하지 않는다.

#### 3단계: 준비 (Preparation)

**클래스 변수**(static 필드)에 대해 메모리를 할당하고 **제로값**으로 초기화한다.

```java
// 준비 단계: value = 0 (int의 제로값)
// 초기화 단계: value = 123 (<clinit>에서 대입)
public static int value = 123;

// 예외: ConstantValue 속성이 있으면 준비 단계에서 바로 대입
public static final int CONST = 123;  // 준비 단계에서 CONST = 123
```

#### 4단계: 해석 (Resolution)

상수 풀의 **심볼릭 레퍼런스**를 **다이렉트 레퍼런스**(메모리 포인터)로 변환한다.

- **심볼릭 레퍼런스**: `CONSTANT_Class_info`, `CONSTANT_Fieldref_info` 등 문자열 기반
- **다이렉트 레퍼런스**: 런타임 메모리 주소, vtable 오프셋 등

해석 대상: 클래스/인터페이스, 필드, 메서드, 인터페이스 메서드, 메서드 타입, 메서드 핸들, 호출 사이트 한정자

> 해석 시점은 JVM 구현에 따라 다르다. "lazy resolution"이면 실제 사용 시점까지 지연, "eager resolution"이면 로딩 직후 수행.

#### 5단계: 초기화 (Initialization)

`<clinit>()` 메서드를 실행한다. 이 메서드는 컴파일러가 자동 생성하며, static 변수 대입문과 static 블록을 순서대로 합친 것이다.

```java
class Example {
    static int a = 1;           // (1)
    static { a = 2; }          // (2)
    static int b = a;          // (3) → b = 2
}
```

**`<clinit>()` 특성:**
- JVM이 멀티스레드 환경에서 하나의 스레드만 `<clinit>()`을 실행하도록 보장 (락)
- 부모의 `<clinit>()`이 먼저 실행됨
- `<clinit>()`이 없으면 (static 변수/블록이 없으면) 생성되지 않음
- 인터페이스의 `<clinit>()`은 부모 인터페이스의 것을 먼저 호출하지 않음

> **주의:** `<clinit>()` 내에서 무한 루프에 빠지면 다른 스레드가 영원히 블로킹된다.

### 핵심 개념 --- 부모 위임 모델 (Parent Delegation Model)

```
                  ┌──────────────────────┐
                  │  Bootstrap ClassLoader │  ← C++ 구현 (null 반환)
                  │  (rt.jar, java.base)  │
                  └──────────┬───────────┘
                             │ 위임
                  ┌──────────┴───────────┐
                  │  Platform ClassLoader  │  ← Java 9+: ExtClassLoader 대체
                  │  (java.sql, java.xml) │
                  └──────────┬───────────┘
                             │ 위임
                  ┌──────────┴───────────┐
                  │ Application ClassLoader│  ← classpath의 클래스
                  │   (사용자 코드)        │
                  └──────────┬───────────┘
                             │ 위임
                  ┌──────────┴───────────┐
                  │  Custom ClassLoader    │  ← 사용자 정의
                  │  (특수 로딩 로직)      │
                  └──────────────────────┘
```

**동작 원리:**

1. 클래스 로딩 요청이 들어오면 **먼저 부모에게 위임**
2. 부모가 로딩에 실패하면 (ClassNotFoundException) 자신이 시도
3. 최상위 Bootstrap까지 올라갔다가 내려오는 구조

**`ClassLoader.loadClass()` 핵심 코드:**

```java
protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
    synchronized (getClassLoadingLock(name)) {
        // 1. 이미 로딩된 클래스 확인
        Class<?> c = findLoadedClass(name);
        if (c == null) {
            try {
                // 2. 부모에게 위임
                if (parent != null) {
                    c = parent.loadClass(name, false);
                } else {
                    c = findBootstrapClassOrNull(name);
                }
            } catch (ClassNotFoundException e) {
                // 부모가 못 찾음
            }
            if (c == null) {
                // 3. 자신이 로딩 시도
                c = findClass(name);
            }
        }
        return c;
    }
}
```

**부모 위임 모델의 장점:**
- **안전성**: 사용자가 `java.lang.Object`를 만들어도 Bootstrap이 진짜를 로딩
- **유일성**: 같은 클래스가 서로 다른 로더에 의해 중복 로딩되는 것을 방지
- **계층 구조**: 기본 라이브러리 → 확장 라이브러리 → 애플리케이션 코드의 명확한 구분

### 핵심 개념 --- 부모 위임 모델의 도전

부모 위임 모델은 세 차례 큰 "파괴"를 겪었다:

#### 1차 파괴: JDK 1.2 이전의 호환성

- `ClassLoader`에 `loadClass()` 밖에 없었으므로 사용자가 직접 오버라이드
- JDK 1.2에서 `findClass()`를 추가하여 오버라이드 대상을 분리
- 하지만 `loadClass()` 오버라이드를 막을 수 없으므로 완전한 강제는 불가

#### 2차 파괴: SPI (Service Provider Interface)

**문제:** Bootstrap ClassLoader가 로딩하는 `java.sql.DriverManager`가 classpath의 JDBC 드라이버를 찾아야 함

```
Bootstrap CL (java.sql.Driver 인터페이스) → Application CL (MySQL Driver 구현)
                          ↑ 부모가 자식의 클래스를 볼 수 없다!
```

**해결: Thread Context ClassLoader**

```java
// DriverManager 내부 (Bootstrap 영역)
ClassLoader cl = Thread.currentThread().getContextClassLoader();
ServiceLoader<Driver> sl = ServiceLoader.load(Driver.class, cl);
// → Application ClassLoader를 사용하여 드라이버 구현체 로딩
```

이것은 부모가 자식 로더에게 로딩을 요청하는 것이므로, 부모 위임의 역전이다.

> **log-friends 연결:** log-friends SDK는 `compileOnly`로 Spring Boot에 의존한다. 실행 시에는 Spring Boot의 `LaunchedURLClassLoader`가 fat JAR 내부의 `BOOT-INF/lib/`에서 SDK를 로딩한다. 이 로더 자체가 부모 위임을 일부 변형한 구현이다.

#### 3차 파괴: OSGi와 모듈화

OSGi는 번들마다 독립적인 클래스 로더를 가지며, 패키지 단위로 **네트워크형** 위임 구조를 만든다:

1. `java.*` → 부모 위임 (Bootstrap)
2. 위임 목록에 있는 패키지 → 부모 위임
3. Import-Package → 해당 패키지를 Export하는 번들의 로더
4. 자기 자신의 번들 ClassPath
5. Fragment Bundle
6. Dynamic Import

### 핵심 개념 --- 자바 모듈 시스템 (JDK 9+)

JDK 9에서 도입된 모듈 시스템(JPMS)은 클래스 로딩에 근본적인 변화를 가져왔다.

**모듈의 핵심 개념:**

```java
// module-info.java
module java.sql {
    requires java.base;         // 의존 모듈
    exports java.sql;           // 외부 공개 패키지
    exports javax.sql;
    uses java.sql.Driver;       // SPI 소비자 선언
}
```

**모듈 시스템에서의 클래스 로더 변화:**

| JDK 8 이전 | JDK 9 이후 |
|---|---|
| Bootstrap + Extension + Application | Bootstrap + **Platform** + Application |
| Extension ClassLoader (`ext/`) | Platform ClassLoader (모듈 기반) |
| `URLClassLoader` 상속 | `BuiltinClassLoader` 내부 클래스 |

**모듈 로딩 규칙:**
- 각 모듈은 하나의 클래스 로더에 소속
- 하나의 클래스 로더는 여러 모듈을 로딩 가능
- `exports` 되지 않은 패키지는 다른 모듈에서 접근 불가 (리플렉션 포함)
- `--add-opens`, `--add-exports`로 런타임에 열 수 있음

> **log-friends 연결:** ByteBuddy가 내부 API(`sun.misc.Unsafe` 등)에 접근할 때 모듈 시스템의 제약을 받는다. 이것이 `--add-opens java.base/java.lang=ALL-UNNAMED` 같은 JVM 옵션이 필요한 이유이며, `-Djdk.attach.allowAttachSelf=true`도 같은 맥락이다.

---

## 8장: 바이트코드 실행 엔진

### 핵심 개념 --- 스택 프레임 구조

메서드가 호출될 때마다 JVM은 가상 머신 스택에 **스택 프레임(Stack Frame)** 을 push한다.

```
  ┌─────────────────────────────────────┐
  │          JVM 가상 머신 스택           │
  │  ┌─────────────────────────────────┐ │
  │  │  현재 스택 프레임 (Current Frame) │ │ ← 실행 중인 메서드
  │  │  ┌───────────────────────────┐  │ │
  │  │  │   지역 변수 테이블          │  │ │
  │  │  │   (Local Variable Table)  │  │ │
  │  │  │   [0] this                │  │ │
  │  │  │   [1] arg1                │  │ │
  │  │  │   [2] arg2                │  │ │
  │  │  │   [3] localVar            │  │ │
  │  │  ├───────────────────────────┤  │ │
  │  │  │   피연산자 스택             │  │ │
  │  │  │   (Operand Stack)         │  │ │
  │  │  │   ┌─────┐                 │  │ │
  │  │  │   │ val │ ← top           │  │ │
  │  │  │   ├─────┤                 │  │ │
  │  │  │   │ val │                 │  │ │
  │  │  │   └─────┘                 │  │ │
  │  │  ├───────────────────────────┤  │ │
  │  │  │   동적 링크                 │  │ │
  │  │  │   (Dynamic Link)          │  │ │
  │  │  │   → 런타임 상수 풀 참조     │  │ │
  │  │  ├───────────────────────────┤  │ │
  │  │  │   반환 주소                 │  │ │
  │  │  │   (Return Address)        │  │ │
  │  │  └───────────────────────────┘  │ │
  │  └─────────────────────────────────┘ │
  │  ┌─────────────────────────────────┐ │
  │  │  호출자 스택 프레임              │ │
  │  └─────────────────────────────────┘ │
  │  ┌─────────────────────────────────┐ │
  │  │  ...                            │ │
  │  └─────────────────────────────────┘ │
  └─────────────────────────────────────┘
```

#### 지역 변수 테이블 (Local Variable Table)

- **슬롯(Slot)** 단위로 구성. 32비트 이하 타입은 1슬롯, `long`/`double`은 2슬롯
- 인스턴스 메서드의 슬롯 0은 항상 `this`
- 메서드 파라미터 → 선언 순서대로 슬롯 할당 → 지역 변수
- 슬롯 재사용(reuse): 스코프가 끝난 변수의 슬롯을 다른 변수가 재사용

```java
public void example(int a, long b) {
    // 슬롯 0: this
    // 슬롯 1: a (int, 1슬롯)
    // 슬롯 2-3: b (long, 2슬롯)
    String s = "hello";
    // 슬롯 4: s (reference, 1슬롯)
}
```

> **GC 관련 주의:** 슬롯이 재사용되지 않으면, 스코프를 벗어난 객체라도 지역 변수 테이블이 참조를 유지하여 GC되지 않을 수 있다. 하지만 JIT 컴파일러가 이를 최적화하므로 실무에서 `var = null` 패턴은 불필요하다.

#### 피연산자 스택 (Operand Stack)

- LIFO 구조, 최대 깊이는 컴파일 타임에 결정 (`max_stack`)
- 바이트코드 명령어가 값을 push/pop하며 연산 수행
- 산술 예: `iadd`는 스택에서 두 int를 pop → 더한 결과를 push

```
// 1 + 2 계산
iconst_1    // 스택: [1]
iconst_2    // 스택: [1, 2]
iadd        // 스택: [3]
```

#### 동적 링크 (Dynamic Link)

상수 풀의 심볼릭 레퍼런스 중 일부는 클래스 로딩 또는 첫 사용 시 다이렉트 레퍼런스로 변환된다(정적 해석). 나머지는 **매 호출 시마다** 해석된다(동적 링크).

- 정적 해석: `invokestatic`, `invokespecial` 대상 메서드
- 동적 링크: `invokevirtual`, `invokeinterface` 대상 메서드 (런타임 타입에 따라 결정)

#### 반환 주소 (Return Address)

메서드가 반환될 때 실행을 이어갈 PC 카운터 값. 두 가지 종료 방식:

1. **정상 종료**: `return`/`ireturn`/`areturn` 등 → 호출자의 PC 카운터를 복원
2. **예외 종료**: 미처리 예외 → 예외 테이블에서 핸들러를 찾아 PC 설정. 반환 주소 없음

### 핵심 개념 --- 메서드 디스패치

#### 정적 디스패치 (Static Dispatch) = 오버로딩 해석

**컴파일 타임**에 매개변수의 **정적 타입(Static Type)** 을 기준으로 호출할 메서드를 결정한다.

```java
class Overload {
    void say(Object arg)  { System.out.println("Object"); }
    void say(String arg)  { System.out.println("String"); }
    void say(int arg)     { System.out.println("int"); }

    public static void main(String[] args) {
        Overload o = new Overload();
        Object str = "hello";
        o.say(str);       // "Object" 출력 — 정적 타입이 Object
        o.say("hello");   // "String" 출력 — 정적 타입이 String
    }
}
```

```
$ javap -c Overload.class

// o.say(str)
invokevirtual #7  // Overload.say:(Ljava/lang/Object;)V  ← 컴파일 타임에 Object 버전 선택

// o.say("hello")
invokevirtual #8  // Overload.say:(Ljava/lang/String;)V  ← 컴파일 타임에 String 버전 선택
```

> 오버로딩 해석은 컴파일러가 수행하며, 바이트코드에는 이미 구체적인 디스크립터가 고정되어 있다.

#### 동적 디스패치 (Dynamic Dispatch) = 오버라이딩

**런타임**에 객체의 **실제 타입(Actual Type)** 을 기준으로 호출할 메서드를 결정한다.

```java
class Animal {
    void speak() { System.out.println("..."); }
}
class Dog extends Animal {
    @Override void speak() { System.out.println("Woof!"); }
}
class Cat extends Animal {
    @Override void speak() { System.out.println("Meow!"); }
}

Animal a = new Dog();
a.speak();  // "Woof!" — 런타임에 Dog.speak() 선택
```

**`invokevirtual` 실행 과정:**

1. 피연산자 스택 top에서 객체 참조를 꺼냄
2. 객체의 **실제 클래스**에서 디스크립터와 이름이 일치하는 메서드를 검색
3. 접근 권한 검증 → 통과하면 다이렉트 레퍼런스 반환
4. 없으면 상위 클래스로 올라가며 반복 검색
5. 최종적으로 못 찾으면 `AbstractMethodError`

#### 가상 메서드 테이블 (vtable)

매번 클래스 계층을 탐색하면 성능이 나쁘므로, JVM은 **vtable(Virtual Method Table)** 을 사용한다.

```
Animal vtable:
  [0] Object.hashCode() → Object 구현
  [1] Object.equals()   → Object 구현
  [2] Object.toString() → Object 구현
  [3] Animal.speak()    → Animal 구현

Dog vtable:
  [0] Object.hashCode() → Object 구현   (상속)
  [1] Object.equals()   → Object 구현   (상속)
  [2] Object.toString() → Object 구현   (상속)
  [3] Animal.speak()    → Dog 구현       (오버라이드!)
```

- vtable은 **준비(Preparation)** 단계에서 초기화
- 오버라이드된 메서드는 같은 인덱스에 자식의 구현으로 교체
- `invokeinterface`는 **itable(Interface Method Table)** 을 사용

> **log-friends 연결:** `InstrumentationRegistry.installSpring()`이 `DispatcherServlet.doService()`를 계측할 때, ByteBuddy는 `MethodDelegation`으로 원래 메서드 호출을 `@SuperCall callable`로 감싼다. 이때 원본 메서드 호출은 내부적으로 `invokevirtual` 또는 `invokespecial`로 이루어지며, vtable을 통한 동적 디스패치가 작동한다.

### 핵심 개념 --- invokedynamic과 동적 타입 언어 지원

#### invokedynamic의 탄생 배경

Java 7 이전의 4가지 호출 명령어(`invokestatic`, `invokespecial`, `invokevirtual`, `invokeinterface`)는 모두 **컴파일 타임에 호출 대상의 심볼릭 레퍼런스가 결정**된다. 동적 타입 언어(Groovy, JRuby 등)를 JVM 위에서 효율적으로 실행하려면 런타임에 호출 대상을 결정할 수 있는 메커니즘이 필요했다.

#### java.lang.invoke 패키지

```java
// MethodHandle — 리플렉션보다 가볍고, JIT 최적화 가능
MethodHandles.Lookup lookup = MethodHandles.lookup();
MethodType mt = MethodType.methodType(void.class, String.class);
MethodHandle mh = lookup.findVirtual(PrintStream.class, "println", mt);
mh.invoke(System.out, "Hello via MethodHandle");
```

**MethodHandle vs Reflection:**

| 비교 항목 | Reflection | MethodHandle |
|---|---|---|
| 추상 수준 | Java API 수준 | 바이트코드 명령어 수준 |
| 타입 검사 | 런타임 (비용 높음) | 링크 타임 (MethodType) |
| JIT 최적화 | 제한적 | 인라이닝 가능 |
| 접근 제어 | 호출 시점 검사 | lookup 생성 시점 검사 |

#### invokedynamic 동작 메커니즘

```
invokedynamic #X  (Bootstrap Method 호출)
       │
       ▼
  Bootstrap Method 실행
  (최초 1회만)
       │
       ▼
  CallSite 반환
  (MethodHandle을 담고 있음)
       │
       ▼
  이후 호출: CallSite의 MethodHandle을 직접 실행
  (Bootstrap 재실행 없음)
```

**람다에서의 활용 (Java 8+):**

```java
Runnable r = () -> System.out.println("lambda");
```

```
$ javap -c -p LambdaExample.class

// 람다 생성 부분
invokedynamic #2, 0
  // InvokeDynamic #0:run:()Ljava/lang/Runnable;
  // Bootstrap: LambdaMetafactory.metafactory(...)

// 컴파일러가 생성한 private static 메서드
private static void lambda$main$0();
  Code:
    0: getstatic     #3    // System.out
    3: ldc           #4    // "lambda"
    5: invokevirtual #5    // println
    8: return

// BootstrapMethods 속성:
// 0: #28 LambdaMetafactory.metafactory
//   Method arguments:
//     #29 ()V                                    // 함수형 인터페이스 메서드 시그니처
//     #30 invokestatic LambdaExample.lambda$main$0  // 실제 구현
//     #31 ()V                                    // 인스턴스화된 메서드 타입
```

**실행 흐름:**

1. `invokedynamic` 최초 실행 시 `LambdaMetafactory.metafactory()` 호출
2. `metafactory`가 런타임에 `Runnable` 구현 클래스를 동적 생성
3. `CallSite`에 해당 클래스의 생성 `MethodHandle`을 설정
4. 이후 호출은 `CallSite`를 통해 직접 실행 (Bootstrap 재실행 없음)

> **log-friends 연결:** ByteBuddy는 내부적으로 `invokedynamic`과 유사한 메커니즘을 활용한다. `MethodDelegation.to(SpringInterceptor::class.java)`는 대상 메서드 호출을 인터셉터로 위임하는 바이트코드를 생성하며, `@SuperCall Callable<*>`은 원래 메서드를 호출하는 `MethodHandle` 기반의 callable을 런타임에 생성한다.

### 핵심 개념 --- 스택 기반 바이트코드 해석 및 실행 엔진

JVM의 명령어 집합은 **스택 기반(Stack-based)** 아키텍처다. 대부분의 하드웨어 CPU는 **레지스터 기반(Register-based)** 이다.

**스택 기반 vs 레지스터 기반:**

```
// 1 + 2 계산

// 스택 기반 (JVM)
iconst_1          // 스택: [1]
iconst_2          // 스택: [1, 2]
iadd              // 스택: [3]
istore_0          // 결과를 지역변수 0에 저장

// 레지스터 기반 (x86 유사)
mov  eax, 1       // eax = 1
add  eax, 2       // eax = 3
```

| 비교 | 스택 기반 | 레지스터 기반 |
|---|---|---|
| **이식성** | 높음 (하드웨어 독립) | 낮음 (레지스터 수/종류 의존) |
| **코드 크기** | 작음 (오퍼랜드 없음) | 큼 (레지스터 번호 필요) |
| **실행 속도** | 느림 (메모리 접근 많음) | 빠름 (레지스터 접근) |
| **구현 난이도** | 쉬움 | 어려움 |
| **최적화** | JIT 컴파일러가 레지스터로 변환 | 하드웨어 직접 실행 |

> JVM이 스택 기반을 선택한 핵심 이유는 **플랫폼 독립성**이다. 레지스터 수와 종류에 의존하지 않으므로 "Write Once, Run Anywhere"가 가능하다. 실행 성능은 JIT 컴파일러가 네이티브 코드로 변환할 때 레지스터 할당을 수행하여 보완한다.

**실행 예시 --- 전체 흐름:**

```java
public int calculate() {
    int a = 100;
    int b = 200;
    int c = 300;
    return (a + b) * c;
}
```

```
$ javap -c

public int calculate();
  Code:
    0: bipush   100     // 100을 스택에 push       스택: [100]
    2: istore_1         // 슬롯1(a)에 저장          스택: []
    3: sipush   200     // 200을 스택에 push       스택: [200]
    6: istore_2         // 슬롯2(b)에 저장          스택: []
    7: sipush   300     // 300을 스택에 push       스택: [300]
   10: istore_3         // 슬롯3(c)에 저장          스택: []
   11: iload_1          // 슬롯1(a)을 push         스택: [100]
   12: iload_2          // 슬롯2(b)을 push         스택: [100, 200]
   13: iadd             // 100+200=300 push       스택: [300]
   14: iload_3          // 슬롯3(c)을 push         스택: [300, 300]
   15: imul             // 300*300=90000 push     스택: [90000]
   16: ireturn          // 90000 반환
```

---

## 9장: 클래스 로딩과 실행 서브시스템 사례

### 사례 분석

#### 톰캣의 클래스 로더 아키텍처

톰캣은 부모 위임 모델을 변형하여 웹 애플리케이션 간 격리를 구현한다:

```
             Bootstrap
                │
           System (JRE)
                │
           Common ClassLoader     ← server/lib + shared/lib
           ╱              ╲
  Catalina CL          Shared ClassLoader
  (서버 내부)           ╱              ╲
               WebApp CL #1     WebApp CL #2    ← 앱 간 격리
                    │                │
               JSP CL #1        JSP CL #2       ← JSP 핫 리로드
```

- **WebAppClassLoader**: 부모 위임을 **역전** — 자기 자신이 먼저 로딩 시도 (`/WEB-INF/classes`, `/WEB-INF/lib`)
- **JSP ClassLoader**: JSP 변경 시 기존 CL을 버리고 새로 생성 → 핫 리로드
- 각 웹 앱이 서로 다른 버전의 같은 라이브러리를 사용할 수 있음

#### Spring Boot Fat JAR와 LaunchedURLClassLoader

```
application.jar
├── META-INF/
│   └── MANIFEST.MF          → Main-Class: JarLauncher
├── org/springframework/boot/loader/   → 런처 코드
├── BOOT-INF/
│   ├── classes/              → 애플리케이션 코드
│   └── lib/                  → 의존 라이브러리 JAR들
```

```java
// Spring Boot의 LaunchedURLClassLoader
public class LaunchedURLClassLoader extends URLClassLoader {
    // BOOT-INF/classes/ 와 BOOT-INF/lib/*.jar를 URL로 등록
    // Nested JAR를 처리하기 위한 커스텀 프로토콜 핸들러

    @Override
    protected Class<?> loadClass(String name, boolean resolve) {
        // 1. 이미 로딩된 클래스 확인
        // 2. 부모 위임 (java.*, javax.* 등)
        // 3. 자기 자신에서 로딩 (BOOT-INF/)
    }
}
```

> **log-friends 연결:** `log-friends-sdk`는 `compileOnly`로 선언되어 별도의 JAR로 배포되지 않고, 사용하는 프로젝트의 `BOOT-INF/lib/`에 포함된다. `LogFriendsInstaller`가 `spring.factories`를 통해 발견되는 것도 `LaunchedURLClassLoader`가 `BOOT-INF/classes/META-INF/spring.factories`를 스캔하기 때문이다.

#### 바이트코드 생성 기술과 동적 프락시

**JDK 동적 프락시:**

```java
// InvocationHandler를 구현하면 JVM이 프락시 클래스를 런타임 생성
Object proxy = Proxy.newProxyInstance(
    target.getClass().getClassLoader(),
    target.getClass().getInterfaces(),
    (proxyObj, method, args) -> {
        System.out.println("Before: " + method.getName());
        Object result = method.invoke(target, args);
        System.out.println("After: " + method.getName());
        return result;
    }
);
```

- `sun.misc.ProxyGenerator`가 바이트코드를 동적 생성
- 인터페이스만 프락시 가능 (클래스 프락시 불가)
- 생성된 프락시 클래스는 `$Proxy0`, `$Proxy1` 등의 이름

**CGLIB:**

```java
Enhancer enhancer = new Enhancer();
enhancer.setSuperclass(TargetClass.class);  // 클래스도 프락시 가능!
enhancer.setCallback((MethodInterceptor) (obj, method, args, proxy) -> {
    System.out.println("Before");
    Object result = proxy.invokeSuper(obj, args);
    System.out.println("After");
    return result;
});
TargetClass proxy = (TargetClass) enhancer.create();
```

- ASM 라이브러리로 서브클래스를 바이트코드 수준에서 생성
- `final` 클래스/메서드는 프락시 불가 (상속 기반이므로)

**ByteBuddy (log-friends가 사용하는 방식):**

```kotlin
// log-friends의 InstrumentationRegistry에서 사용하는 패턴
AgentBuilder.Default()
    .with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)
    .type(ElementMatchers.named("org.springframework.web.servlet.DispatcherServlet"))
    .transform { builder, _, _, _, _ ->
        builder.method(ElementMatchers.named("doService"))
            .intercept(MethodDelegation.to(SpringInterceptor::class.java))
    }.installOn(inst)
```

**세 가지 접근 방식 비교:**

| 비교 항목 | JDK Proxy | CGLIB | ByteBuddy Agent |
|---|---|---|---|
| 대상 | 인터페이스만 | 클래스 (서브클래싱) | 모든 클래스 (기존 클래스 수정) |
| 원리 | `Proxy.newProxyInstance` | ASM으로 서브클래스 생성 | `Instrumentation.retransformClasses` |
| 성능 | 보통 | 빠름 | 최적 (원본 클래스 자체 수정) |
| 코드 수정 | 불필요 (인터페이스 필요) | 불필요 (상속 가능해야 함) | **완전히 불필요** (투명 계측) |
| 사용 사례 | Spring AOP (인터페이스) | Spring AOP (클래스) | Java Agent, APM 도구 |

> **핵심 차이:** JDK Proxy와 CGLIB는 **새로운 클래스**를 만들지만, ByteBuddy Agent는 `Instrumentation` API로 **기존 클래스의 바이트코드를 직접 수정**한다. 이것이 log-friends가 "코드 수정 없이" 계측할 수 있는 근본적인 이유다.

#### 바이트코드 수준에서 본 ByteBuddy의 계측

`MethodDelegation.to(SpringInterceptor::class.java)`가 `DispatcherServlet.doService()`에 적용되면, ByteBuddy는 대략 다음과 같은 변환을 수행한다:

```
// 변환 전 (원본 doService)
public void doService(HttpServletRequest req, HttpServletResponse res) {
    // 원래 로직
}

// 변환 후 (ByteBuddy가 수정한 doService) — 개념적 표현
public void doService(HttpServletRequest req, HttpServletResponse res) {
    // ByteBuddy가 삽입한 코드:
    // 1. SpringInterceptor.intercept()에 전달할 인자 준비
    //    - @Origin Method → 현재 메서드 리플렉션 객체
    //    - @Argument(0) → req
    //    - @Argument(1) → res
    //    - @SuperCall Callable → 원본 로직을 감싼 callable
    // 2. SpringInterceptor.intercept() 호출
    // 3. 반환값 처리

    return SpringInterceptor.intercept(method, req, res, originalCallable);
}
```

실제 바이트코드 수준에서는:

```
// 개념적 바이트코드 (실제는 더 복잡)
aload_0                          // this (DispatcherServlet)
aload_1                          // req
aload_2                          // res
// ... Callable 생성 (원본 코드를 감싸는 내부 클래스)
invokestatic SpringInterceptor.intercept(
    Ljava/lang/reflect/Method;
    Ljava/lang/Object;
    Ljava/lang/Object;
    Ljava/util/concurrent/Callable;
)Ljava/lang/Object;
areturn
```

### 이 프로젝트(log-friends)와의 연결 --- 종합

| 장 | 연결 포인트 | SDK 코드 |
|---|---|---|
| **6장** (클래스 파일 구조) | ByteBuddy가 상수 풀에 인터셉터 레퍼런스를 추가하고 Code 속성을 재작성 | `InstrumentationRegistry` 전체 |
| **7장** (클래스 로딩) | `EnvironmentPostProcessor` 시점에 agent 설치 → 클래스 로딩 전에 transformer 등록 | `LogFriendsInstaller.postProcessEnvironment()` |
| **7장** (부모 위임 도전) | Spring Boot `LaunchedURLClassLoader`가 `BOOT-INF/lib/`에서 SDK 로딩 | `spring.factories` / `AutoConfiguration` |
| **7장** (클래스 로딩 시점) | `@Service` 어노테이션이 붙은 클래스가 Spring에 의해 로딩될 때 계측 발생 | `installMethodTrace()` |
| **8장** (동적 디스패치) | `invokevirtual`로 `doService()` 호출 시 계측된 버전이 실행됨 (vtable 갱신) | `SpringInterceptor.intercept()` |
| **8장** (invokedynamic) | `@SuperCall Callable`이 MethodHandle 기반으로 원본 메서드 호출 | `callable.call()` in `SpringInterceptor` |
| **9장** (동적 프락시 비교) | JDK Proxy/CGLIB와 달리 기존 클래스를 직접 수정 → 코드 수정 불필요 | `AgentBuilder.RETRANSFORMATION` |
| **9장** (바이트코드 생성) | `MethodDelegation`이 인터셉터 호출 바이트코드를 동적 생성 | `MethodDelegation.to(...)` |

**`RETRANSFORMATION` 전략이 중요한 이유:**

```kotlin
// InstrumentationRegistry.kt
AgentBuilder.Default()
    .with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION)
```

- `RETRANSFORMATION`: 이미 로딩된 클래스도 다시 변환 가능. `Instrumentation.retransformClasses()` 사용
- `REDEFINITION`: 이미 로딩된 클래스를 완전히 재정의. 일부 JVM에서 제약
- 없음 (기본): 아직 로딩되지 않은 클래스만 변환

`LogFriendsInstaller`가 `EnvironmentPostProcessor` 시점에 실행되므로, `DispatcherServlet` 같은 Spring 클래스는 이미 로딩되어 있을 수 있다. `RETRANSFORMATION`이 이를 처리한다.

### 실습

#### 실습 1: javap로 바이트코드 분석

```bash
# Kotlin 클래스의 바이트코드 확인
javap -c -p build/classes/kotlin/main/com/logfriends/agent/SpringInterceptor.class

# 상수 풀까지 보기
javap -v -p build/classes/kotlin/main/com/logfriends/agent/SpringInterceptor.class

# 특정 메서드만 확인
javap -c -p -s build/classes/kotlin/main/com/logfriends/agent/BatchTransporter.class
```

**확인할 포인트:**
- `@RuntimeType`이 바이트코드에 어떻게 반영되는지
- `@SuperCall Callable` 파라미터의 디스크립터
- `invokevirtual` vs `invokestatic` vs `invokeinterface` 사용 패턴

#### 실습 2: ClassLoader 계층 확인

```java
// Spring Boot 애플리케이션 내에서 실행
public class ClassLoaderInspector {
    public static void inspect() {
        ClassLoader cl = ClassLoaderInspector.class.getClassLoader();
        System.out.println("=== ClassLoader Hierarchy ===");
        while (cl != null) {
            System.out.println(cl.getClass().getName() + ": " + cl);
            cl = cl.getParent();
        }
        System.out.println("Bootstrap ClassLoader (null)");

        // log-friends SDK의 클래스 로더 확인
        System.out.println("\n=== Log Friends Classes ===");
        System.out.println("LogFriendsInstaller: " +
            LogFriendsInstaller.class.getClassLoader());
        System.out.println("SpringInterceptor: " +
            SpringInterceptor.class.getClassLoader());
    }
}
```

**예상 출력 (Spring Boot Fat JAR 환경):**

```
=== ClassLoader Hierarchy ===
org.springframework.boot.loader.LaunchedURLClassLoader: ...
jdk.internal.loader.ClassLoaders$AppClassLoader: ...
jdk.internal.loader.ClassLoaders$PlatformClassLoader: ...
Bootstrap ClassLoader (null)
```

#### 실습 3: ByteBuddy 계측 전후 바이트코드 비교

```bash
# ByteBuddy 디버깅 모드로 변환된 클래스 파일 저장
# JVM 옵션에 추가:
# -Dnet.bytebuddy.dump=/tmp/bytebuddy-dump

# 변환 후 클래스 확인
javap -c -p /tmp/bytebuddy-dump/org/springframework/web/servlet/DispatcherServlet.class

# 원본과 비교하여 ByteBuddy가 삽입한 코드 확인
```

#### 실습 4: Instrumentation API 직접 사용

```java
// 간단한 Java Agent 구현 (log-friends의 원리 이해용)
public class SimpleAgent {
    public static void premain(String args, Instrumentation inst) {
        inst.addTransformer((loader, className, classBeingRedefined,
                             protectionDomain, classfileBuffer) -> {
            if ("com/example/Target".equals(className)) {
                System.out.println("Transforming: " + className);
                System.out.println("ClassLoader: " + loader);
                System.out.println("Buffer size: " + classfileBuffer.length);
                // 바이트코드 수정 로직 (ASM/ByteBuddy 사용)
            }
            return null; // null = 변환하지 않음
        });
    }
}
```

---

## 핵심 질문

### 6장 관련

1. **클래스 파일의 상수 풀에는 어떤 종류의 정보가 저장되며, ByteBuddy가 계측할 때 상수 풀을 어떻게 수정하는가?**
   - 힌트: `CONSTANT_Methodref`가 추가되어야 인터셉터 메서드를 호출할 수 있다.

2. **`invokevirtual`, `invokespecial`, `invokestatic`, `invokeinterface`, `invokedynamic`의 차이를 설명하고, log-friends에서 계측하는 메서드 각각이 어떤 호출 명령어로 실행되는지 설명하라.**
   - 힌트: `DispatcherServlet.doService()`는 `invokevirtual`, `PreparedStatement.executeQuery()`는 `invokeinterface`.

3. **Java 9 이후 문자열 연결이 `invokedynamic`을 사용하는 이유와 이것이 `StringBuilder` 방식보다 나은 점은?**

4. **`monitorenter`/`monitorexit` 바이트코드에서 예외 경로의 `monitorexit`이 필요한 이유를 설명하라.**

### 7장 관련

5. **`LogFriendsInstaller`가 `EnvironmentPostProcessor` 시점에 실행되는 것과 `ApplicationReadyEvent` 시점에 실행되는 것의 차이는? 왜 전자를 선택했는가?**
   - 힌트: 클래스 로딩 시점과 `ClassFileTransformer` 등록 타이밍의 관계.

6. **`Thread.currentThread().getContextClassLoader()`가 부모 위임 모델을 어떻게 우회하며, JDBC DriverManager가 이를 활용하는 구체적인 흐름은?**

7. **Spring Boot의 `LaunchedURLClassLoader`가 Nested JAR를 처리하는 방식과, 이것이 `log-friends-sdk`의 클래스 로딩에 미치는 영향은?**

8. **JDK 9 모듈 시스템에서 `--add-opens`가 필요한 이유를 설명하고, ByteBuddy가 모듈 경계를 넘어 계측할 때의 제약은?**

### 8장 관련

9. **스택 프레임의 지역 변수 테이블에서 슬롯 재사용이 GC에 미칠 수 있는 영향을 설명하라. JIT 컴파일러가 이를 어떻게 해결하는가?**

10. **정적 디스패치(오버로딩)와 동적 디스패치(오버라이딩)의 차이를 바이트코드 수준에서 설명하라. `invokevirtual`이 vtable을 탐색하는 과정은?**

11. **`@SuperCall Callable<*>`이 내부적으로 어떻게 동작하는가? ByteBuddy가 이를 위해 생성하는 바이트코드의 구조를 추론하라.**
    - 힌트: `Callable.call()` 내부에서 원본 메서드를 `invokespecial` 또는 별도의 메서드로 호출.

12. **JVM이 스택 기반 아키텍처를 선택한 이유와, JIT 컴파일러가 이를 레지스터 기반 네이티브 코드로 변환할 때의 주요 최적화는?**

### 9장 관련

13. **JDK 동적 프락시, CGLIB, ByteBuddy Agent의 차이를 클래스 파일 생성 관점에서 비교하라. log-friends가 ByteBuddy Agent 방식을 선택한 이유는?**
    - 힌트: "코드 수정 없이" 계측하려면 기존 클래스를 수정해야 하며, 새 클래스 생성으로는 불가능.

14. **`AgentBuilder.RedefinitionStrategy.RETRANSFORMATION`과 `REDEFINITION`의 차이를 설명하고, `LogFriendsInstaller`의 실행 시점을 고려할 때 왜 `RETRANSFORMATION`이 필요한가?**

15. **톰캣의 WebAppClassLoader와 Spring Boot의 LaunchedURLClassLoader가 각각 부모 위임을 변형하는 방식의 차이는?**

---

## 학습 완료 체크리스트

### 6장: 클래스 파일 구조
- [ ] 클래스 파일의 각 구성 요소(매직 넘버, 상수 풀, 필드/메서드 테이블)를 설명할 수 있다
- [ ] 상수 풀의 주요 태그 타입과 심볼릭 레퍼런스의 역할을 이해한다
- [ ] 5가지 메서드 호출 명령어의 차이를 정확히 구분할 수 있다
- [ ] `javap -c -v`로 바이트코드를 읽고 실행 흐름을 추적할 수 있다
- [ ] ByteBuddy가 클래스 파일의 어떤 부분을 수정하는지 설명할 수 있다

### 7장: 클래스 로딩 메커니즘
- [ ] 클래스 로딩 5단계의 순서와 각 단계의 역할을 설명할 수 있다
- [ ] 능동적 사용과 수동적 사용의 차이를 예시로 설명할 수 있다
- [ ] 부모 위임 모델의 동작 원리와 장단점을 설명할 수 있다
- [ ] Thread Context ClassLoader를 통한 SPI 메커니즘을 이해한다
- [ ] `LogFriendsInstaller`의 실행 시점이 클래스 로딩과 어떻게 맞물리는지 설명할 수 있다
- [ ] JDK 9 모듈 시스템이 클래스 로딩에 미치는 영향을 이해한다

### 8장: 바이트코드 실행 엔진
- [ ] 스택 프레임의 4가지 구성 요소를 설명할 수 있다
- [ ] 지역 변수 테이블의 슬롯 할당 규칙을 이해한다
- [ ] 정적 디스패치와 동적 디스패치의 차이를 바이트코드 수준에서 설명할 수 있다
- [ ] vtable의 구조와 역할을 설명할 수 있다
- [ ] `invokedynamic`의 Bootstrap Method → CallSite → MethodHandle 흐름을 이해한다
- [ ] 스택 기반 vs 레지스터 기반 아키텍처의 트레이드오프를 설명할 수 있다

### 9장: 사례와 실전
- [ ] 톰캣/Spring Boot의 클래스 로더 아키텍처를 도식화할 수 있다
- [ ] JDK Proxy, CGLIB, ByteBuddy의 차이를 명확히 설명할 수 있다
- [ ] `RETRANSFORMATION` 전략의 필요성을 log-friends의 실행 시점과 연결하여 설명할 수 있다
- [ ] `Instrumentation` API의 기본 사용법을 이해한다
