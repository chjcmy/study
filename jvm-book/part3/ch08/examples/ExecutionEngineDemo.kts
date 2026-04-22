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
