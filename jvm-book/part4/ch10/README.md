# 10장: 프런트엔드 컴파일과 최적화

> 예상 학습 시간: 4~6시간
> "JVM 밑바닥까지 파헤치기" 4부 — 10장

---

### 핵심 개념 --- javac 컴파일 4단계

`javac`는 단순히 `.java`를 `.class`로 변환하는 도구가 아니다. 내부적으로 4단계의 정교한 파이프라인을 거친다.

```mermaid
graph LR
  src[소스 코드] --> p1[1. 구문 분석]
  p1 --> p2[2. 심벌 테이블 채우기]
  p2 --> p3[3. 애너테이션 처리]
  p3 --> p4["4. 의미 분석 + 바이트코드 생성"]
  p3 -.->|라운드 반복| p1
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

### 핵심 질문 (10장)

1. **javac가 거의 최적화를 하지 않는 이유는 무엇인가?** JVM 언어 생태계와 JIT 컴파일러의 역할 분담 관점에서 설명하라.

2. **자바의 타입 소거 제네릭이 C#의 구체화 제네릭보다 나은 점이 있다면 무엇인가?** 하위 호환성 외에 다른 이점이 있는지 논의하라.

3. Kotlin의 `reified` 키워드가 타입 소거를 피하는 원리는? JVM 수준의 실체화 제네릭과의 차이점은?

4. Lombok이 `AbstractProcessor` 공식 API 대신 내부 API를 사용하는 이유는? 그로 인한 위험성은?

5. `Integer.valueOf(127) == Integer.valueOf(127)`가 `true`이지만 `Integer.valueOf(128) == Integer.valueOf(128)`가 `false`인 이유를 JVM 스펙 수준에서 설명하라.

---

### 학습 완료 체크리스트

- [ ] javac의 4단계 컴파일 과정(구문 분석 → 심벌 테이블 → 애너테이션 처리 → 바이트코드 생성)을 설명할 수 있다
- [ ] AST(추상 구문 트리)가 무엇이고, javac에서 어떤 역할을 하는지 안다
- [ ] 애너테이션 프로세서의 라운드 개념을 이해하고, Lombok의 동작 원리를 설명할 수 있다
- [ ] 자바 타입 소거의 동작 방식과 한계점(메서드 시그니처 충돌, 브릿지 메서드)을 설명할 수 있다
- [ ] 자바 제네릭과 C# 제네릭의 차이를 비교 설명할 수 있다
- [ ] Integer 캐시 범위(-128~127)와 오토박싱 성능 함정을 알고 있다
- [ ] 조건부 컴파일이 자바에서 어떻게 동작하는지 안다
- [ ] JSR 269 API로 간단한 애너테이션 프로세서를 작성할 수 있다
