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

## 핵심 질문 (6장)

1. **클래스 파일의 상수 풀에는 어떤 종류의 정보가 저장되며, ByteBuddy가 계측할 때 상수 풀을 어떻게 수정하는가?**
   - 힌트: `CONSTANT_Methodref`가 추가되어야 인터셉터 메서드를 호출할 수 있다.

2. **`invokevirtual`, `invokespecial`, `invokestatic`, `invokeinterface`, `invokedynamic`의 차이를 설명하고, log-friends에서 계측하는 메서드 각각이 어떤 호출 명령어로 실행되는지 설명하라.**
   - 힌트: `DispatcherServlet.doService()`는 `invokevirtual`, `PreparedStatement.executeQuery()`는 `invokeinterface`.

3. **Java 9 이후 문자열 연결이 `invokedynamic`을 사용하는 이유와 이것이 `StringBuilder` 방식보다 나은 점은?**

4. **`monitorenter`/`monitorexit` 바이트코드에서 예외 경로의 `monitorexit`이 필요한 이유를 설명하라.**

## 학습 완료 체크리스트

- [ ] 클래스 파일의 각 구성 요소(매직 넘버, 상수 풀, 필드/메서드 테이블)를 설명할 수 있다
- [ ] 상수 풀의 주요 태그 타입과 심볼릭 레퍼런스의 역할을 이해한다
- [ ] 5가지 메서드 호출 명령어의 차이를 정확히 구분할 수 있다
- [ ] `javap -c -v`로 바이트코드를 읽고 실행 흐름을 추적할 수 있다
- [ ] ByteBuddy가 클래스 파일의 어떤 부분을 수정하는지 설명할 수 있다
