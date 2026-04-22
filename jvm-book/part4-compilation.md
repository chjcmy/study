# 4부: 컴파일과 최적화

> 예상 학습 시간: 10~12시간 (2개 장)
>
> "JVM 밑바닥까지 파헤치기" (深入理解Java虚拟机 3판, 저우즈밍) 10장~11장

자바 코드가 `.java` 파일에서 기계어까지 도달하는 여정에는 **두 번의 컴파일**이 존재한다. 프런트엔드 컴파일러(`javac`)가 소스를 바이트코드로 변환하고, 백엔드 컴파일러(JIT/AOT)가 바이트코드를 네이티브 코드로 변환한다. 이 두 단계를 정확히 이해하면 성능 튜닝의 본질이 보인다.

---

## 10장: 프런트엔드 컴파일과 최적화

### 핵심 개념 --- javac 컴파일 4단계

`javac`는 단순히 `.java`를 `.class`로 변환하는 도구가 아니다. 내부적으로 4단계의 정교한 파이프라인을 거친다.

```
소스 코드 → [1. 구문 분석] → [2. 심벌 테이블 채우기] → [3. 애너테이션 처리] → [4. 의미 분석 + 바이트코드 생성]
                                                              ↑                          |
                                                              └──────── 라운드 반복 ───────┘
```

#### 1단계: 구문 분석 (Parse and Enter)

어휘 분석(Lexical Analysis)과 구문 분석(Syntax Analysis)을 수행한다.

- **어휘 분석**: 소스 코드 문자열을 토큰(Token) 스트림으로 변환한다. `int a = 1 + 2;`는 `int`, `a`, `=`, `1`, `+`, `2`, `;` 총 7개 토큰이 된다.
- **구문 분석**: 토큰 스트림을 **추상 구문 트리(AST)**로 조립한다. `javac`는 `com.sun.tools.javac.parser.JavacParser`에서 재귀 하향 파싱(Recursive Descent Parsing)을 사용한다.

AST는 `com.sun.tools.javac.tree.JCTree`의 서브클래스들로 구성된다. 이후 모든 컴파일 단계는 이 트리를 대상으로 동작한다.

#### 2단계: 심벌 테이블 채우기 (Enter)

AST의 각 노드에 대해 **심벌 테이블(Symbol Table)**을 구축한다. 심벌 테이블은 이름과 타입 정보의 매핑이다.

- 클래스 심벌, 메서드 심벌, 변수 심벌을 등록
- 패키지 수준에서 이름 충돌 검사
- `import` 문 처리

이 단계에서 아직 메서드 본문의 의미 분석은 하지 않는다. 클래스와 멤버의 "뼈대"만 확인한다.

#### 3단계: 애너테이션 처리 (Annotation Processing)

JSR 269(Pluggable Annotation Processing API)에 의해 도입된 단계다. 컴파일 타임에 애너테이션을 읽고 **새로운 코드를 생성**할 수 있다.

핵심은 **라운드(Round) 개념**이다:
1. 1라운드: 기존 소스에서 애너테이션을 찾아 프로세서 실행
2. 프로세서가 새로운 소스 파일을 생성하면 → 다시 1단계부터 재시작
3. 더 이상 새로운 파일이 생성되지 않을 때까지 반복

Lombok이 `@Getter`, `@Setter`를 처리하는 것이 대표적 사례다. Lombok은 AST를 직접 수정하는데, 이는 공식 API가 아닌 내부 API(`com.sun.tools.javac.tree`)를 사용하는 것이므로 논란이 있다.

#### 4단계: 의미 분석과 바이트코드 생성

**의미 분석**은 두 하위 단계로 나뉜다:

- **어트리뷰트 분석(Attribute)**: 이름 해석(name resolution), 타입 검사, 상수 폴딩(constant folding). 예를 들어 `int a = 1 + 2;`에서 `1 + 2`를 `3`으로 치환하는 것은 이 단계에서 일어난다.
- **데이터 흐름 분석(Flow)**: 지역 변수 초기화 검사, `final` 변수 재할당 검사, 메서드 반환 경로 검사, checked exception 처리 검사

**바이트코드 생성** 단계에서 수행하는 주요 작업:
- **편의 문법(Syntactic Sugar) 제거**: 제네릭 타입 소거, 오토박싱/언박싱 변환, `for-each` → 반복자 패턴 등
- **`<init>`과 `<clinit>` 생성**: 인스턴스 초기화와 클래스 초기화 메서드를 합성
- 최종적으로 `.class` 파일 작성

```java
// 소스 코드
public class Example {
    private int x = 10;
    private int y;
    
    public Example() {
        y = 20;
    }
}

// javac가 생성하는 실제 <init>
// public Example() {
//     super();        // Object 생성자 호출 (javac가 삽입)
//     x = 10;         // 필드 초기화 코드가 생성자 앞에 삽입됨
//     y = 20;         // 원래 생성자 본문
// }
```

> **깊이 포인트**: `javac`는 성능 최적화를 거의 하지 않는다. 상수 폴딩 정도가 전부다. 최적화는 백엔드(JIT)의 몫이다. 이 설계 덕분에 JRuby, Kotlin, Scala 등 JVM 언어들도 동일한 JIT 최적화 혜택을 받는다.

---

### 핵심 개념 --- 제네릭과 타입 소거

자바 제네릭은 프로그래밍 언어 역사에서 가장 논쟁적인 설계 결정 중 하나다.

#### 자바 제네릭 vs C# 제네릭

| 구분 | 자바 (타입 소거) | C# (구체화/Reification) |
|---|---|---|
| 런타임 타입 정보 | 없음 (`List<String>`과 `List<Integer>`는 동일한 `List`) | 있음 (`List<string>`과 `List<int>`는 다른 타입) |
| 원시 타입 지원 | 불가 (`List<int>` 불가, `List<Integer>` 필요) | 가능 (`List<int>` 가능, 박싱 없음) |
| 런타임 `instanceof` | `obj instanceof List<String>` 불가 | 가능 |
| 성능 | 원시 타입 박싱 오버헤드 | 원시 타입 직접 저장 |
| 하위 호환성 | Java 5 이전 코드와 완벽 호환 | 호환성 보장 불필요 (CLR 2.0에서 새로 도입) |

자바가 타입 소거를 선택한 이유는 **하위 호환성** 때문이다. Java 5에서 제네릭을 추가할 때, 기존 비제네릭 코드와 바이너리 호환성을 유지해야 했다.

#### 타입 소거의 실체

```java
// 컴파일 전
public class Box<T> {
    private T value;
    public T get() { return value; }
    public void set(T value) { this.value = value; }
}

Box<String> box = new Box<>();
box.set("hello");
String s = box.get();

// 타입 소거 후 (실제 바이트코드에 해당하는 자바 코드)
public class Box {
    private Object value;           // T → Object
    public Object get() { return value; }
    public void set(Object value) { this.value = value; }
}

Box box = new Box();
box.set("hello");
String s = (String) box.get();    // 컴파일러가 자동 삽입한 체크캐스트
```

#### 타입 소거가 만드는 함정들

**함정 1: 메서드 시그니처 충돌**

```java
// 컴파일 에러! 타입 소거 후 두 메서드 모두 process(List)가 된다
public void process(List<String> list) { }
public void process(List<Integer> list) { }
```

**함정 2: 브릿지 메서드**

```java
public class StringBox extends Box<String> {
    @Override
    public String get() { return super.get(); }  // 반환 타입이 String
}

// 컴파일러가 브릿지 메서드를 자동 생성한다:
// public Object get() { return this.get(); }  // 바이트코드 수준에서 반환 타입으로 오버로딩
```

바이트코드에서는 반환 타입도 메서드 디스크립터의 일부이므로 `Object get()`과 `String get()`이 공존할 수 있다. 이것은 자바 소스 코드 수준에서는 불가능하지만 바이트코드 수준에서는 합법적이다.

**함정 3: 제네릭과 리플렉션**

타입이 소거되더라도 클래스 파일의 `Signature` 속성에 제네릭 정보가 남아 있다. `Method.getGenericReturnType()` 등으로 조회할 수 있다. 타입 소거는 **코드 실행 경로**에서만 일어나고, **메타데이터**는 보존된다.

#### 실체화 제네릭(Reified Generics) 논의

Project Valhalla에서 자바에 실체화 제네릭을 도입하려는 시도가 진행 중이다. 핵심은 `List<int>` 같은 원시 타입 제네릭을 지원하는 것이다. 이를 위해 **값 타입(Value Type)**과 **특수화(Specialization)** 개념이 필요하다.

> Kotlin의 `reified` 키워드는 인라인 함수와 결합하여 컴파일 타임에 타입 정보를 보존한다. 하지만 이것은 JVM 수준의 실체화가 아니라 **컴파일러 트릭**이다.

---

### 핵심 개념 --- 오토박싱의 함정

오토박싱/언박싱은 Java 5에서 도입된 편의 문법이지만, 무심코 사용하면 성능 함정에 빠진다.

#### Integer 캐시 (-128 ~ 127)

```java
Integer a = 100;
Integer b = 100;
System.out.println(a == b);   // true  (캐시된 동일 객체)

Integer c = 200;
Integer d = 200;
System.out.println(c == d);   // false (새 객체 생성)

Integer e = 100;
int f = 100;
System.out.println(e == f);   // true  (언박싱 후 값 비교)
```

`Integer.valueOf(int)`는 -128~127 범위의 값을 `IntegerCache`에서 반환한다. 이 범위를 벗어나면 매번 새 객체를 생성한다.

#### 성능 함정: 반복문 내 오토박싱

```java
// 나쁜 코드: 약 2억 번 오토박싱 발생
Long sum = 0L;
for (long i = 0; i < Integer.MAX_VALUE; i++) {
    sum += i;   // sum = Long.valueOf(sum.longValue() + i) 로 변환됨
}

// 좋은 코드: 원시 타입 사용
long sum = 0L;
for (long i = 0; i < Integer.MAX_VALUE; i++) {
    sum += i;
}
```

두 코드의 실행 시간 차이는 **5~6배**에 달한다. `Long` 대신 `long`을 쓰는 것만으로 불필요한 객체 생성 약 2^31개를 제거할 수 있다.

#### 조건 표현식의 함정

```java
Object flag = true ? new Integer(1) : new Double(2.0);
System.out.println(flag);   // 출력: 1.0 (Integer가 아닌 Double!)
```

삼항 연산자에서 두 피연산자의 타입이 다르면, 자바 언어 명세에 따라 **수치 승격(Numeric Promotion)**이 적용된다. `Integer`가 `Double`로 언박싱-확장 변환되어 `1.0`이 된다.

#### 조건부 컴파일

```java
// javac가 컴파일 시점에 조건부 제거
if (true) {
    System.out.println("A");
} else {
    System.out.println("B");   // 이 코드는 바이트코드에 포함되지 않음
}
```

`javac`는 `if` 문의 조건이 상수 표현식일 때 도달 불가능한 분기를 제거한다. 이것이 자바에서 유일하게 가능한 "조건부 컴파일"이다. C/C++의 `#ifdef`와 달리 메서드 본문 내 `if` 문에서만 동작한다.

---

### 실전 --- 플러그인 애너테이션 처리기 제작

JSR 269 API를 활용하면 컴파일 타임에 코드를 검사하거나 생성할 수 있다.

```java
// 간단한 네이밍 규칙 검사 프로세서 예시
@SupportedAnnotationTypes("*")
@SupportedSourceVersion(SourceVersion.RELEASE_21)
public class NamingConventionProcessor extends AbstractProcessor {

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        for (Element element : roundEnv.getRootElements()) {
            checkNaming(element);
        }
        return false;  // 다른 프로세서도 처리할 수 있게 false 반환
    }

    private void checkNaming(Element element) {
        String name = element.getSimpleName().toString();
        if (element.getKind() == ElementKind.CLASS) {
            // 클래스명이 대문자로 시작하는지 검사
            if (Character.isLowerCase(name.charAt(0))) {
                processingEnv.getMessager().printMessage(
                    Diagnostic.Kind.WARNING,
                    "클래스명은 대문자로 시작해야 합니다: " + name,
                    element
                );
            }
        }
        // 자식 요소 재귀 검사
        for (Element child : element.getEnclosedElements()) {
            checkNaming(child);
        }
    }
}
```

등록은 `META-INF/services/javax.annotation.processing.Processor` 파일에 FQCN을 기록하거나, Google의 `@AutoService` 애너테이션을 사용한다.

> Lombok, MapStruct, Dagger, QueryDSL의 `Q클래스` 생성이 모두 이 메커니즘을 사용한다.

---

## 11장: 백엔드 컴파일과 최적화

### 핵심 개념 --- JIT 컴파일러

JVM은 처음에 바이트코드를 **인터프리터**로 실행한다. 특정 코드가 반복 실행되어 "뜨겁다(hot)"고 판단되면 **JIT(Just-In-Time) 컴파일러**가 네이티브 코드로 변환한다.

#### 인터프리터와 컴파일러의 공존

```
             ┌─────────────────────────────────────────┐
 바이트코드 ──┤  인터프리터 (즉시 실행, 프로파일링 수집)   │
             │                                         │
             │  핫 코드 감지 ──→ JIT 컴파일러            │
             │                  ├─ C1 (클라이언트)       │
             │                  └─ C2 (서버)            │
             │                                         │
             │  네이티브 코드 캐시 (Code Cache)          │
             └─────────────────────────────────────────┘
```

- **인터프리터**: 시작이 빠르다. 최적화 정보(프로파일링 데이터)를 수집한다.
- **C1 (클라이언트 컴파일러)**: 빠르게 컴파일, 간단한 최적화. 시작 속도 중시.
- **C2 (서버 컴파일러)**: 느리게 컴파일, 공격적 최적화. 피크 성능 중시.

#### 계층형 컴파일 (Tiered Compilation)

Java 7부터 기본 활성화된 계층형 컴파일은 5단계로 나뉜다:

| 레벨 | 설명 | 특징 |
|---|---|---|
| 0 | 인터프리터 | 프로파일링 수집 |
| 1 | C1 컴파일, 프로파일링 없음 | 단순 메서드, getter/setter |
| 2 | C1 컴파일, 제한적 프로파일링 | C2 큐가 찬 경우 |
| 3 | C1 컴파일, 전체 프로파일링 | 일반적 C1 컴파일 |
| 4 | C2 컴파일 | 최고 수준 최적화 |

일반적인 경로: `0 → 3 → 4` (인터프리터 → C1 전체 프로파일링 → C2 최적화)

#### 핫 코드 판별: 두 가지 카운터

JIT 컴파일 대상이 되는 "핫 코드"는 두 종류다:

**1. 메서드 호출 카운터 (Invocation Counter)**
- 메서드가 호출될 때마다 증가
- C2 기준 기본 임계값: **10,000회** (`-XX:CompileThreshold=10000`)
- 임계값 초과 시 해당 메서드 전체를 컴파일

**2. 백엣지 카운터 (Back Edge Counter)**
- 루프의 역방향 분기(back edge)를 만날 때마다 증가
- 루프 본문이 충분히 "뜨거우면" **OSR(On-Stack Replacement)** 컴파일 수행
- OSR: 메서드 실행 도중에 인터프리터에서 컴파일된 코드로 전환

```java
// 이 메서드가 1번만 호출되더라도 내부 루프가 핫 코드가 될 수 있다
public void processLargeArray(int[] data) {
    long sum = 0;
    for (int i = 0; i < data.length; i++) {   // 백엣지 카운터 증가
        sum += data[i] * 2 + 1;
    }
    // 루프의 백엣지 카운터가 임계값을 넘으면 OSR 컴파일 발동
    // → 루프 실행 도중 네이티브 코드로 전환
}
```

> **열 감쇠(Counter Decay)**: 메서드 호출 카운터는 시간이 지나면 반감된다. 이를 통해 "과거에 많이 호출됐지만 지금은 안 쓰는 메서드"가 계속 핫 코드로 남는 것을 방지한다. `-XX:-UseCounterDecay`로 비활성화 가능.

#### JIT 컴파일 과정

```
바이트코드 → HIR (High-level IR) → LIR (Low-level IR) → 레지스터 할당 → 네이티브 코드
            ├─ 인라인                ├─ 피프홀 최적화
            ├─ 상수 전파             └─ 명령어 스케줄링
            ├─ 탈출 분석
            └─ 루프 최적화
```

#### 실전: JIT 컴파일 결과 확인

```bash
# 컴파일 이벤트 출력
java -XX:+PrintCompilation -jar app.jar

# 출력 예시:
#   132   1       3       java.lang.String::hashCode (55 bytes)
#   시간  ID  레벨  클래스::메서드             (바이트코드 크기)
# 레벨 3 = C1 전체 프로파일링, 레벨 4 = C2

# 인라인 결정 확인
java -XX:+UnlockDiagnosticVMOptions -XX:+PrintInlining -jar app.jar

# 어셈블리 출력 (hsdis 플러그인 필요)
java -XX:+UnlockDiagnosticVMOptions -XX:+PrintAssembly -jar app.jar
```

---

### 핵심 개념 --- 핵심 최적화 기법

JIT 컴파일러의 최적화는 수십 가지에 달하지만, 실무에서 영향이 큰 핵심 기법들을 깊이 살펴본다.

| 기법 | 설명 | 효과 |
|---|---|---|
| 메서드 인라인 | 메서드 호출을 본문으로 교체 | 호출 오버헤드 제거, 후속 최적화 기반 |
| 탈출 분석 | 객체가 메서드 밖으로 나가는지 분석 | 스택 할당, 동기화 제거, 스칼라 치환 |
| 스칼라 치환 | 객체를 필드별 개별 변수로 분해 | 힙 할당 완전 제거 |
| 공통 하위 표현식 제거 | 동일 계산의 중복 제거 | CPU 사이클 절약 |
| 배열 경계 검사 제거 | 범위 증명 시 검사 생략 | 루프 성능 대폭 향상 |

#### 메서드 인라인 (Method Inlining)

**가장 중요한 최적화**다. 인라인 자체의 이점(호출 오버헤드 제거)도 있지만, 진짜 가치는 **후속 최적화의 문을 여는 것**이다.

```java
// === 인라인 전 ===
public int calculate(int x) {
    return add(x, 1) * multiply(x, 2);
}

private int add(int a, int b) {
    return a + b;
}

private int multiply(int a, int b) {
    return a * b;
}

// === 인라인 후 (JIT가 수행) ===
public int calculate(int x) {
    return (x + 1) * (x * 2);   // 메서드 호출이 본문으로 교체됨
    // 이제 상수 전파, 강도 절감 등 추가 최적화가 가능해진다
}
```

**인라인 조건**:
- 메서드 바이트코드 크기가 35바이트 이하 (`-XX:MaxInlineSize=35`) → 핫 여부 무관하게 인라인
- 핫 메서드는 325바이트 이하 (`-XX:FreqInlineSize=325`) → 호출 빈도가 높으면 더 큰 메서드도 인라인
- `final`, `private`, `static` 메서드 → 가상 호출이 아니므로 쉽게 인라인
- 가상 메서드는 **타입 프로파일링** 기반으로 판단. 단일 구현체만 관찰되면 **추측적 인라인(Speculative Inlining)** 수행 후 가드(guard) 코드 삽입

```java
// 가상 메서드의 추측적 인라인
interface Shape { int area(); }
class Circle implements Shape { int area() { return (int)(Math.PI * r * r); } }
class Square implements Shape { int area() { return side * side; } }

// 런타임에 Circle만 관찰되면:
// if (shape.getClass() == Circle.class) {
//     return (int)(Math.PI * r * r);   // 인라인된 코드
// } else {
//     return shape.area();              // 가드 실패 시 가상 호출 (탈최적화)
// }
```

#### 탈출 분석 (Escape Analysis)

객체가 현재 메서드나 스레드의 범위를 벗어나는지(탈출하는지) 분석한다. 탈출하지 않는 객체에 대해 세 가지 최적화가 가능하다.

```java
// === 탈출 분석 대상 코드 ===
public int sumOfPoints() {
    int total = 0;
    for (int i = 0; i < 1000; i++) {
        Point p = new Point(i, i * 2);   // p는 메서드 밖으로 나가지 않음
        total += p.getX() + p.getY();
    }
    return total;
}

class Point {
    private final int x, y;
    Point(int x, int y) { this.x = x; this.y = y; }
    int getX() { return x; }
    int getY() { return y; }
}
```

**최적화 1: 스칼라 치환 (Scalar Replacement)**

객체를 필드별 개별 변수로 분해한다. 가장 효과적인 최적화다.

```java
// 스칼라 치환 후
public int sumOfPoints() {
    int total = 0;
    for (int i = 0; i < 1000; i++) {
        int px = i;         // Point.x → 지역 변수
        int py = i * 2;     // Point.y → 지역 변수
        total += px + py;   // 힙 할당 완전 제거!
    }
    return total;
}
// 1,000개의 Point 객체 힙 할당이 0개로 줄어든다
```

**최적화 2: 스택 할당 (Stack Allocation)**

> 주의: HotSpot JVM은 실제로 스택 할당을 구현하지 **않았다**. 책에서 언급하지만, 실제로는 스칼라 치환이 더 강력하기 때문에 스택 할당은 불필요하다고 판단한 것이다. 스칼라 치환이 불가능한 경우(객체가 배열로 전달되는 등)에는 여전히 힙에 할당된다.

**최적화 3: 동기화 제거 (Lock Elision)**

```java
// 탈출하지 않는 객체의 동기화는 무의미하다
public String concatThreadLocal() {
    StringBuffer sb = new StringBuffer();   // sb는 이 메서드에서만 사용
    sb.append("hello");                      // StringBuffer.append는 synchronized
    sb.append("world");
    return sb.toString();
}

// 동기화 제거 후 (JIT)
public String concatThreadLocal() {
    // synchronized 블록이 완전히 제거됨
    // StringBuilder와 동일한 성능
    ...
}
```

탈출 분석 확인:
```bash
# 탈출 분석 활성화 (Java 8u40+에서 기본 활성화)
java -XX:+DoEscapeAnalysis -XX:+EliminateAllocations -XX:+EliminateLocks -jar app.jar

# 비활성화하여 성능 차이 비교
java -XX:-DoEscapeAnalysis -jar app.jar
```

#### 공통 하위 표현식 제거 (Common Subexpression Elimination)

```java
// 최적화 전
int a = b * c + g;
int d = b * c * e;

// 공통 하위 표현식 제거 후
int tmp = b * c;    // 공통 부분 한 번만 계산
int a = tmp + g;
int d = tmp * e;
```

**로컬 공통 하위 표현식 제거**: 기본 블록(Basic Block) 내에서 적용. C1, C2 모두 수행.
**글로벌 공통 하위 표현식 제거**: 기본 블록 경계를 넘어 적용. C2에서 수행.

#### 배열 경계 검사 제거 (Array Bounds Check Elimination)

자바는 배열 접근 시마다 인덱스가 유효한지 검사한다. 매번 검사하면 성능 손실이 크다.

```java
// 일반적인 루프 — JIT가 검사를 제거할 수 있다
for (int i = 0; i < array.length; i++) {
    sum += array[i];   // i는 항상 0 ~ array.length-1 이므로 검사 불필요
}

// JIT는 루프 진입 전에 한 번만 검사하고, 루프 내부 검사를 제거한다
// if (array != null && array.length > 0) {
//     for (int i = 0; i < array.length; i++) {
//         sum += array[i];  // 경계 검사 생략
//     }
// }
```

---

### 핵심 개념 --- AOT vs JIT

#### AOT (Ahead-Of-Time) 컴파일

코드를 **실행 전에 미리** 네이티브 코드로 컴파일하는 방식이다.

| 비교 항목 | JIT | AOT |
|---|---|---|
| 컴파일 시점 | 런타임 (실행 중) | 빌드 시점 (실행 전) |
| 시작 시간 | 느림 (워밍업 필요) | 빠름 (즉시 네이티브 실행) |
| 피크 성능 | 높음 (프로파일 기반 최적화) | 보통 (정적 분석 한계) |
| 메모리 사용량 | 높음 (프로파일러 + Code Cache) | 낮음 |
| 적합한 환경 | 장시간 실행 서버 | 서버리스, CLI, 마이크로서비스 |

#### AOT의 근본적 한계: 프로파일 기반 최적화 불가

JIT 컴파일러의 최강 무기는 **런타임 프로파일 정보**다.

- 가상 메서드의 실제 구현체 분포 → 추측적 인라인
- 분기문의 실제 실행 빈도 → 분기 예측 최적화
- 타입 정보 → 불필요한 타입 체크 제거

AOT는 이런 정보 없이 정적 분석에만 의존하므로, 장시간 실행되는 서버 애플리케이션에서는 JIT의 피크 성능을 따라잡기 어렵다.

#### GraalVM native-image

GraalVM의 `native-image`는 가장 성숙한 AOT 솔루션이다.

```bash
# Spring Boot 앱을 네이티브 바이너리로 빌드
./gradlew nativeCompile

# 결과: 시작 시간 수십 ms, 메모리 사용량 수십 MB
# 단, 빌드 시간이 수 분~수십 분 소요
```

**native-image의 제약**:
- **리플렉션**: `reflect-config.json`에 명시적 등록 필요
- **동적 프록시**: `proxy-config.json`에 등록 필요
- **클래스 로딩**: 런타임 클래스 로딩 불가 (Closed-World Assumption)
- **바이트코드 조작**: ByteBuddy 등 런타임 코드 생성 불가

#### jaotc (Java AOT Compiler) - 구 방식

Java 9에서 실험적으로 도입된 `jaotc`는 Java 17에서 제거되었다. GraalVM native-image가 사실상 표준이 되었기 때문이다.

```bash
# (Java 9~16에서만 가능, 현재는 deprecated)
jaotc --output libHelloWorld.so HelloWorld.class
java -XX:AOTLibrary=./libHelloWorld.so HelloWorld
```

---

### 실전 --- Graal 컴파일러 깊이 이해

Graal은 Java로 작성된 JIT 컴파일러다. C2를 대체하는 것이 목표이며, JVMCI(JVM Compiler Interface)를 통해 HotSpot에 플러그인된다.

#### JVMCI (JVM Compiler Interface)

JDK 9에서 도입된 표준 인터페이스로, JIT 컴파일러를 **교체**할 수 있게 해준다.

```bash
# Graal을 JIT 컴파일러로 사용
java -XX:+UnlockExperimentalVMOptions -XX:+UseJVMCICompiler -jar app.jar
```

JVMCI가 제공하는 핵심 서비스:
- 바이트코드와 메타데이터 조회
- 프로파일링 데이터 조회
- 컴파일된 네이티브 코드를 Code Cache에 설치

#### Graal의 중간 표현 (IR: Intermediate Representation)

Graal은 **Sea-of-Nodes IR**을 사용한다. 전통적인 CFG(Control Flow Graph)와 달리, 데이터 의존성과 제어 의존성을 단일 그래프에 표현한다.

```
전통적 IR (C2):
  Block 1: a = load x
  Block 1: b = load y
  Block 1: c = a + b
  Block 1: if c > 0 goto Block 2

Sea-of-Nodes (Graal):
  [LoadNode x] ──데이터──→ [AddNode] ──데이터──→ [IfNode]
  [LoadNode y] ──데이터──↗              │
                                      제어
                                    ↙    ↘
                              [True]    [False]
```

Sea-of-Nodes의 장점:
- 노드 재배치가 자유로움 → 명령어 스케줄링에 유리
- 공통 하위 표현식 제거가 자연스러움
- 부분 탈출 분석(Partial Escape Analysis) 구현에 적합

#### Graal의 핵심 최적화: 부분 탈출 분석

C2의 탈출 분석은 "탈출하거나 / 안 하거나" 이분법이다. Graal은 **경로별로** 분석한다.

```java
public Object partialEscape(boolean condition) {
    Object obj = new Object();    // 객체 생성
    if (condition) {
        return obj;               // 이 경로에서는 탈출
    } else {
        return obj.hashCode();    // 이 경로에서는 탈출하지 않음
    }
}

// C2: obj가 탈출할 수 있으므로 항상 힙에 할당
// Graal (부분 탈출 분석): 
//   condition이 false인 경로에서는 할당을 지연/제거
//   condition이 true인 경로에서만 실제 할당(materialization)
```

---

### 이 프로젝트(log-friends)와의 연결

#### ByteBuddy 프록시와 JIT 인라인

ByteBuddy가 생성한 프록시 메서드는 JIT 인라인 대상이 **될 수 있다**. ByteBuddy는 런타임에 새로운 클래스를 생성하지만, 생성된 바이트코드는 일반 자바 클래스와 동일하게 취급된다.

```
원본 메서드 호출 → ByteBuddy 프록시 메서드 → 인터셉터 로직 → 원본 메서드
```

그러나 인터셉터 체인의 길이와 가상 호출의 존재로 인해, 전체 호출 경로가 인라인되기는 어렵다. 특히 `MethodDelegation`을 사용하는 경우 가상 디스패치가 포함되어 C2의 추측적 인라인에 의존하게 된다.

log-friends의 `InstrumentationRegistry`에서 설치하는 5개 인터셉터는 모두 정적 메서드(`@RuntimeType static` 패턴)를 사용하므로 가상 호출 오버헤드는 없지만, `Callable.call()`을 통한 원본 메서드 호출은 가상 디스패치를 포함한다.

#### Kotlin inline 함수와 JVM 인라인

Kotlin의 `inline` 키워드는 **컴파일러(kotlinc) 수준**의 인라인이다. JVM JIT의 인라인과는 별개로, 컴파일 시점에 호출부에 함수 본문을 복사한다.

```kotlin
// Kotlin inline
inline fun <T> measure(block: () -> T): T {
    val start = System.nanoTime()
    val result = block()
    println("${System.nanoTime() - start}ns")
    return result
}

// 컴파일 후: 호출부에 코드가 복사됨 (람다 객체 생성 없음)
// JIT가 추가로 이 코드를 더 최적화할 수 있음
```

Kotlin `inline`은 **람다 객체 할당 제거**가 핵심 목적이다. JIT 인라인은 **호출 오버헤드 제거와 후속 최적화 활성화**가 목적이다. 두 인라인은 서로 보완적이다.

#### @Service 메서드 10ms 임계값과 JIT 워밍업

log-friends는 `@Service` 메서드 실행 시간이 10ms 이상일 때만 `METHOD_TRACE` 이벤트를 전송한다. 여기서 JIT와 관련된 중요한 고려사항이 있다:

1. **워밍업 효과**: 애플리케이션 시작 직후에는 JIT가 아직 활성화되지 않아 인터프리터로 실행된다. 같은 메서드라도 인터프리터 실행 시 10ms가 걸리던 것이 JIT 컴파일 후 1ms로 줄어들 수 있다.
2. **OSR에 의한 시간 변동**: 루프가 있는 메서드가 실행 도중 OSR 컴파일되면, 한 번의 호출 내에서도 실행 속도가 변한다.
3. **탈최적화(Deoptimization)**: JIT가 추측적 최적화를 했는데 가정이 깨지면, 네이티브 코드에서 인터프리터로 되돌아간다. 이때 일시적으로 실행 시간이 급증할 수 있다.

```
시간 →
실행시간
  ↑
  │  ████                        ██
  │  ████  ███                   ██
  │  ████  ███  ██               ██  ██
  │──████──███──██──██──██──██──────────── 10ms 임계값
  │              ██  ██  ██  ██      ██
  └──────────────────────────────────────→
     인터프리터    C1     C2        탈최적화
     (느림)      (보통)  (빠름)     (일시적 느림)
```

#### KafkaProducer의 hot path

`BatchTransporter`의 Kafka 전송 로직은 이벤트가 지속적으로 발생하는 환경에서 hot path가 된다:

- `KafkaProducer.send()` 내부의 직렬화, 파티셔닝, 버퍼링 코드가 C2 컴파일 대상
- Protobuf 직렬화 코드(`AgentMessage.toByteArray()`)도 반복 호출 시 JIT 최적화
- 배치 큐(`LinkedBlockingQueue`)의 `offer()`/`poll()` 연산은 락 관련 최적화(락 거칠화, 어댑티브 스피닝)가 적용될 수 있음

#### GraalVM native-image와 SDK 호환성

log-friends SDK를 GraalVM native-image로 AOT 컴파일하는 것은 **현재로서는 불가능**하다.

핵심 장벽:
1. **ByteBuddy 런타임 코드 생성**: native-image의 Closed-World Assumption과 직접 충돌
2. **Byte Buddy Agent `attach`**: JVM attach API는 native-image에서 지원되지 않음
3. **리플렉션 기반 스캔**: `SpecScanner`의 클래스 스캔이 동적 클래스 로딩에 의존

가능한 대안:
- ByteBuddy의 빌드 타임 플러그인(Gradle/Maven)으로 전환하여 컴파일 시점에 바이트코드 변환
- Spring AOT 처리와 통합하여 `reflect-config.json` 자동 생성
- Quarkus처럼 빌드 타임 계측 아키텍처로 전환 (대규모 리팩터링 필요)

---

### 실습

#### 실습 1: `-XX:+PrintCompilation`으로 JIT 확인

```bash
# log-friends examples 앱에서 JIT 컴파일 관찰
java -XX:+PrintCompilation \
     -Djdk.attach.allowAttachSelf=true \
     -jar examples/build/libs/examples.jar 2>&1 | head -50

# 출력에서 확인할 것:
# - BatchTransporter 관련 메서드가 컴파일되는 시점
# - 컴파일 레벨 (1=C1, 4=C2)
# - "made not entrant" (탈최적화) 발생 여부
```

#### 실습 2: JITWatch를 사용한 시각적 분석

```bash
# JITWatch 설치 및 실행
git clone https://github.com/AdoptOpenJDK/jitwatch.git
cd jitwatch
mvn clean compile exec:java

# 핫 로그 수집을 위한 JVM 옵션
java -XX:+UnlockDiagnosticVMOptions \
     -XX:+TraceClassLoading \
     -XX:+LogCompilation \
     -XX:LogFile=jit.log \
     -jar app.jar

# jit.log 파일을 JITWatch에서 열면:
# - 메서드별 인라인 트리
# - 바이트코드 → 네이티브 코드 매핑
# - 최적화 적용/실패 이유
```

#### 실습 3: 탈출 분석 효과 확인

```java
// EscapeAnalysisBenchmark.java
public class EscapeAnalysisBenchmark {
    
    static class Point {
        int x, y;
        Point(int x, int y) { this.x = x; this.y = y; }
    }
    
    public static long testWithEscape() {
        long sum = 0;
        for (int i = 0; i < 100_000_000; i++) {
            Point p = new Point(i, i + 1);
            sum += p.x + p.y;
        }
        return sum;
    }
    
    public static void main(String[] args) {
        // 워밍업
        for (int i = 0; i < 5; i++) testWithEscape();
        
        // 측정
        long start = System.nanoTime();
        long result = testWithEscape();
        long elapsed = System.nanoTime() - start;
        System.out.printf("결과: %d, 시간: %d ms%n", result, elapsed / 1_000_000);
    }
}
```

```bash
# 탈출 분석 ON (기본값)
java -XX:+DoEscapeAnalysis EscapeAnalysisBenchmark
# 예상: ~50ms, GC 거의 없음

# 탈출 분석 OFF
java -XX:-DoEscapeAnalysis EscapeAnalysisBenchmark
# 예상: ~300ms, GC 빈번 발생 (1억 개 객체 할당)

# GC 로그로 확인
java -XX:+DoEscapeAnalysis -Xlog:gc EscapeAnalysisBenchmark
java -XX:-DoEscapeAnalysis -Xlog:gc EscapeAnalysisBenchmark
```

#### 실습 4: 인라인 효과 확인

```bash
# 인라인 결정 출력
java -XX:+UnlockDiagnosticVMOptions \
     -XX:+PrintInlining \
     -XX:MaxInlineSize=0 \
     -jar app.jar 2>&1 | grep "inline"

# MaxInlineSize=0으로 인라인 비활성화 후 성능 비교
java -XX:MaxInlineSize=0 -XX:FreqInlineSize=0 -jar app.jar
```

---

### 핵심 질문

1. **javac가 거의 최적화를 하지 않는 이유는 무엇인가?** JVM 언어 생태계와 JIT 컴파일러의 역할 분담 관점에서 설명하라.

2. **자바의 타입 소거 제네릭이 C#의 구체화 제네릭보다 나은 점이 있다면 무엇인가?** 하위 호환성 외에 다른 이점이 있는지 논의하라.

3. **메서드 인라인이 "모든 최적화의 어머니"라고 불리는 이유는?** 인라인 후에만 가능해지는 최적화를 3가지 이상 나열하라.

4. **탈출 분석에서 "스택 할당"이 HotSpot에 구현되지 않은 이유는?** 스칼라 치환이 스택 할당보다 우월한 이유를 설명하라.

5. **log-friends의 `@Service` 메서드 임계값(10ms)을 JIT 워밍업 관점에서 평가하라.** 애플리케이션 시작 직후에 불필요한 METHOD_TRACE 이벤트가 다수 발생할 수 있는가? 이를 해결하기 위한 전략은?

6. **ByteBuddy가 생성한 인터셉터 코드가 JIT 최적화에 미치는 영향을 분석하라.** 프록시 체인이 인라인을 방해하는가? `@RuntimeType` 정적 메서드 패턴이 이를 완화하는가?

7. **계층형 컴파일에서 C1으로 컴파일된 코드가 왜 다시 C2로 재컴파일되는가?** C1이 수집한 프로파일링 데이터가 C2 최적화에 어떻게 기여하는지 설명하라.

8. **AOT 컴파일이 JIT 컴파일의 피크 성능에 도달하지 못하는 근본적 이유는?** 프로파일 기반 최적화(PGO)를 AOT에 적용하면 이 격차를 줄일 수 있는가?

9. **Graal 컴파일러의 부분 탈출 분석(Partial Escape Analysis)은 어떤 시나리오에서 C2보다 우수한가?** 실제 코드 패턴으로 예시를 들라.

10. **KafkaProducer의 hot path가 JIT 컴파일되면 어떤 최적화가 적용되겠는가?** Protobuf 직렬화, 파티셔닝, 버퍼링 각각에 대해 예상되는 최적화를 논의하라.

---

### 학습 완료 체크리스트

#### 10장: 프런트엔드 컴파일과 최적화

- [ ] javac의 4단계 컴파일 과정(구문 분석 → 심벌 테이블 → 애너테이션 처리 → 바이트코드 생성)을 설명할 수 있다
- [ ] AST(추상 구문 트리)가 무엇이고, javac에서 어떤 역할을 하는지 안다
- [ ] 애너테이션 프로세서의 라운드 개념을 이해하고, Lombok의 동작 원리를 설명할 수 있다
- [ ] 자바 타입 소거의 동작 방식과 한계점(메서드 시그니처 충돌, 브릿지 메서드)을 설명할 수 있다
- [ ] 자바 제네릭과 C# 제네릭의 차이를 비교 설명할 수 있다
- [ ] Integer 캐시 범위(-128~127)와 오토박싱 성능 함정을 알고 있다
- [ ] 조건부 컴파일이 자바에서 어떻게 동작하는지 안다
- [ ] JSR 269 API로 간단한 애너테이션 프로세서를 작성할 수 있다

#### 11장: 백엔드 컴파일과 최적화

- [ ] 인터프리터, C1, C2의 역할과 계층형 컴파일 5단계를 설명할 수 있다
- [ ] 메서드 호출 카운터와 백엣지 카운터의 차이를 알고, OSR을 설명할 수 있다
- [ ] 메서드 인라인의 조건(크기 제한, 가상 메서드 처리)을 설명할 수 있다
- [ ] 탈출 분석의 3가지 최적화(스칼라 치환, 동기화 제거, 스택 할당)를 설명할 수 있다
- [ ] 공통 하위 표현식 제거와 배열 경계 검사 제거를 예시로 설명할 수 있다
- [ ] JIT vs AOT의 장단점을 비교하고, 각각이 적합한 시나리오를 제시할 수 있다
- [ ] GraalVM native-image의 제약사항(리플렉션, 동적 프록시, 클래스 로딩)을 안다
- [ ] Graal 컴파일러의 JVMCI, Sea-of-Nodes IR, 부분 탈출 분석을 개념적으로 설명할 수 있다
- [ ] `-XX:+PrintCompilation`, JITWatch를 사용해 JIT 동작을 관찰할 수 있다
- [ ] log-friends 프로젝트에서 JIT가 미치는 영향(워밍업, 인터셉터 인라인, AOT 호환성)을 설명할 수 있다
