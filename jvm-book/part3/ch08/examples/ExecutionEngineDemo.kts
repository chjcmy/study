#!/usr/bin/env kotlinc -script
/**
 * 8장 — 바이트코드 실행 엔진
 * 실행: kotlinc -script ExecutionEngineDemo.kts
 */

import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType

// ── 1. JIT 워밍업 관찰 ────────────────────────────────────────────
println("=== 1. JIT 워밍업 관찰 ===")

fun hotMethod(n: Int): Long {
    var sum = 0L
    for (i in 0..n) sum += i
    return sum
}

val checkpoints = listOf(1_000, 5_000, 10_000, 50_000)
var calls = 0
for (target in checkpoints) {
    val start = System.nanoTime()
    while (calls < target) { hotMethod(1000); calls++ }
    val ns = (System.nanoTime() - start) / calls.coerceAtLeast(1)
    println("[JIT] 누적 ${calls}회 → 평균 ${ns}ns/call")
}
println("[JIT] 후반으로 갈수록 빨라짐 = C1→C2 JIT 컴파일 효과")

// ── 2. 스택 프레임 시뮬레이션 ────────────────────────────────────
println("\n=== 2. 스택 프레임 시뮬레이션 ===")

fun frameC(): Int { println("[Frame] C 실행중"); return 42 }
fun frameB(): Int { println("[Frame] B 실행중"); return frameC() + 1 }
fun frameA(): Int { println("[Frame] A 실행중"); return frameB() * 2 }

println("[Frame] frameA() 호출 → 스택 프레임 3개 생성")
println("[Frame] 결과: ${frameA()}")
println("""
[Frame] 각 메서드 호출마다 스택 프레임 생성:
  - 지역 변수 테이블 (Local Variable Table)
  - 오퍼랜드 스택 (Operand Stack)
  - 프레임 데이터 (상수 풀 참조, 반환 주소)
""".trimIndent())

// ── 3. invokedynamic & 람다 ───────────────────────────────────────
println("=== 3. invokedynamic & 람다 ===")

val lambda: (Int) -> Int = { x -> x * x }
println("[Lambda] lambda(5) = ${lambda(5)}")

println("""
[Lambda] 바이트코드:
  invokedynamic #N, 0  ← bootstrap 메서드 호출 (최초 1회)
  → LambdaMetafactory.metafactory() 호출
  → 런타임에 람다 구현 클래스 동적 생성
  → 이후 호출은 캐시된 CallSite 사용

  invokedynamic 사용처:
    - Kotlin/Java 람다
    - 문자열 연결 (JDK 9+, StringConcatFactory)
    - Kotlin when → 복잡한 분기
""".trimIndent())

// ── 4. MethodHandle ───────────────────────────────────────────────
println("=== 4. MethodHandle ===")

val lookup = MethodHandles.lookup()
val mh = lookup.findVirtual(String::class.java, "length", MethodType.methodType(Int::class.java))
val str = "Kotlin"
val len = mh.invoke(str) as Int
println("[MethodHandle] \"$str\".length() via MethodHandle = $len")

println("""
[MethodHandle] 용도:
  - invokedynamic의 bootstrap 메서드
  - 리플렉션보다 빠름 (JIT 최적화 가능)
  - 타입 안전한 함수 포인터
""".trimIndent())

// ── 5. 인라이닝과 역최적화 ───────────────────────────────────────
println("=== 5. 인라이닝 & 역최적화 (Deoptimization) ===")
println("""
인라이닝 (Inlining):
  JIT가 자주 호출되는 짧은 메서드를 호출 지점에 코드를 직접 삽입
  장점: 메서드 호출 오버헤드 제거, 추가 최적화 기회 확대
  확인: -XX:+PrintInlining -XX:+UnlockDiagnosticVMOptions

역최적화 (Deoptimization):
  JIT가 "A 타입만 올 것"이라 가정하고 최적화했는데
  런타임에 B 타입이 오면 → 기존 컴파일 코드 폐기 → 인터프리터로 복귀
  → 다시 프로파일링 → 재컴파일 (더 일반적인 코드로)

  확인: -XX:+TraceDeoptimization (JDK 진단 옵션)
""".trimIndent())

// ── 6. 정적 디스패치 vs 동적 디스패치 ───────────────────────────
println("\n=== 6. 정적 디스패치 vs 동적 디스패치 ===")

open class Animal { open fun speak() = "..." }
class Dog : Animal() { override fun speak() = "Woof!" }
class Cat : Animal() { override fun speak() = "Meow!" }

// 동적 디스패치: 런타임 타입에 따라 결정 (invokevirtual — vtable 조회)
println("[Dynamic] 동적 디스패치 — 런타임 타입 기준:")
val animals: List<Animal> = listOf(Dog(), Cat(), Dog())
animals.forEach { animal ->
    println("[Dynamic] ${animal.javaClass.simpleName}.speak() = ${animal.speak()}  ← 런타임 vtable 조회")
}

// 정적 디스패치 (오버로딩): 컴파일 타임 정적 타입에 따라 결정
fun describe(a: Animal) = "Animal(정적 타입)"
fun describe(d: Dog)    = "Dog(정적 타입)"
fun describe(c: Cat)    = "Cat(정적 타입)"

println("\n[Static] 정적 디스패치 — 컴파일 타임 선언 타입 기준 (오버로딩):")
val dog: Dog    = Dog()
val animal: Animal = Dog()  // 선언 타입이 Animal

println("[Static] describe(dog)    → ${describe(dog)}")      // Dog 오버로드 선택
println("[Static] describe(animal) → ${describe(animal)}")   // Animal 오버로드 선택 (런타임은 Dog이지만!)
println("[Static] → 오버로딩은 컴파일 타임 정적 타입으로 결정: invokevirtual이 아닌 컴파일러 선택")

println("""
[Dispatch] vtable(가상 메서드 테이블) 개념:
  - JVM은 클래스마다 vtable을 유지
  - vtable: 클래스의 가상 메서드 → 실제 구현 메서드 주소 매핑 테이블
  - Dog vtable:
      speak() → Dog.speak (오버라이드)
      toString() → Object.toString (상속)
      ...
  - invokevirtual 실행 시:
      1. 수신 객체의 실제 타입 확인 (런타임)
      2. 해당 타입의 vtable에서 메서드 주소 조회
      3. 해당 주소로 점프
  - JIT는 단형 호출(monomorphic call)을 인라인 캐싱으로 최적화
    → 항상 같은 타입만 오면 vtable 조회 건너뜀

[Dispatch] 정리:
  오버로딩(Overloading) → 정적 디스패치 — 컴파일러가 결정 (invokestatic/invokespecial/invokevirtual 중 선택)
  오버라이딩(Overriding) → 동적 디스패치 — 런타임 vtable/itable 조회
""".trimIndent())

// ── 7. 스택 기반 vs 레지스터 기반 아키텍처 ──────────────────────
println("=== 7. 스택 기반 vs 레지스터 기반 아키텍처 ===")

println("""
[Architecture] JVM: 스택 기반 (Stack-Based)
  - 피연산자 스택(Operand Stack)으로 모든 연산 수행
  - 명령어가 단순하고 이식성 높음 (레지스터 개수 무관)
  - 명령어 수가 많아지는 단점

  1 + 2 바이트코드:
    iconst_1   → 스택에 1 push
    iconst_2   → 스택에 2 push
    iadd       → 스택에서 2개 pop → 더함 → 결과 push
    istore_1   → 스택에서 pop → 지역 변수[1]에 저장

[Architecture] Dalvik/ART (Android): 레지스터 기반
  - 가상 레지스터(v0, v1, ...) 직접 지정
  - 명령어 수 적고 연산 빠름 (레지스터 직접 지정)
  - add-int v0, v1, v2  (v0 = v1 + v2 한 명령어)
""".trimIndent())

// JVM 피연산자 스택 동작 시뮬레이션
println("[Stack] JVM 피연산자 스택 시뮬레이션:")
val stack = ArrayDeque<Int>()

// iconst_1 시뮬레이션
stack.addLast(1)
println("[Stack] iconst_1  → 스택: $stack")

// iconst_2 시뮬레이션
stack.addLast(2)
println("[Stack] iconst_2  → 스택: $stack")

// iadd 시뮬레이션
val operand2 = stack.removeLast()
val operand1 = stack.removeLast()
val addResult = operand1 + operand2
stack.addLast(addResult)
println("[Stack] iadd      → 스택: $stack  ($operand1 + $operand2 = $addResult)")

// istore 시뮬레이션
val stored = stack.removeLast()
println("[Stack] istore_1  → 스택: $stack  (지역 변수 = $stored)")
println("[Stack] 최종 결과: 1 + 2 = $stored")

// 현재 스레드 스택 프레임 깊이 측정
println("\n[Stack] 현재 스레드 스택 프레임 깊이 측정:")
fun measureDepth(current: Int): Int =
    try { measureDepth(current + 1) } catch (e: StackOverflowError) { current }

val approxDepth = run {
    var depth = 0
    try {
        fun counter(n: Int): Int = counter(n + 1)
        counter(0)
        0
    } catch (e: StackOverflowError) {
        // StackOverflowError에서 직접 스택 트레이스 길이로 추정
        e.stackTrace.size
    }
}
println("[Stack] StackOverflow 발생 시 스택 트레이스 깊이(추정): $approxDepth 프레임")
println("[Stack] JVM 기본 스레드 스택 크기: 512KB~1MB (OS/JVM 설정에 따라 다름)")
println("[Stack] -Xss 옵션으로 변경 가능 (예: -Xss2m → 2MB)")
println("""
[Stack] 스택 기반 아키텍처가 JVM이식성에 유리한 이유:
  - x86/ARM/RISC-V 등 모든 CPU 아키텍처에서 동일한 바이트코드 실행
  - 레지스터 수, 이름, 규칙이 CPU마다 달라도 무관
  - JIT 컴파일러가 플랫폼별로 레지스터 기반 네이티브 코드로 변환
""".trimIndent())
