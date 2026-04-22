#!/usr/bin/env kotlinc -script
/**
 * 11장 — 백엔드 컴파일과 최적화
 * 실행: kotlinc -script BackendCompileDemo.kts
 *
 * JIT 로그 보기 (컴파일 후):
 *   kotlinc BackendCompileDemo.kts -include-runtime -d backend.jar
 *   java -XX:+PrintCompilation -jar backend.jar
 *   java -Xint -jar backend.jar   (인터프리터 전용, 비교용)
 */

// ── 1. 탈출 분석 (Escape Analysis) ───────────────────────────────
println("=== 1. 탈출 분석 ===")

data class Point(val x: Int, val y: Int)

fun noEscape(): Int { val p = Point(3, 4); return p.x + p.y }   // JIT → 스택/레지스터
fun escape():   Point { return Point(3, 4) }                      // 힙 할당 필수

val iterations = 50_000_000

val start1 = System.nanoTime()
var sum1 = 0; repeat(iterations) { sum1 += noEscape() }
val ms1 = (System.nanoTime() - start1) / 1_000_000.0

val start2 = System.nanoTime()
var sum2 = 0; repeat(iterations) { sum2 += escape().x + escape().y }
val ms2 = (System.nanoTime() - start2) / 1_000_000.0

println("[Escape] 비탈출 (스택/스칼라 치환): ${"%.2f".format(ms1)}ms")
println("[Escape] 탈출 (힙 할당):             ${"%.2f".format(ms2)}ms")
println("[Escape] 비탈출이 빠른 이유: GC STW 없음 + 힙 할당 비용 없음")

// ── 2. 루프 언롤링 ────────────────────────────────────────────────
println("\n=== 2. 루프 언롤링 ===")

fun sumNormal(arr: IntArray): Long { var sum = 0L; for (x in arr) sum += x; return sum }

fun sumUnrolled(arr: IntArray): Long {
    var sum = 0L; val len = arr.size; var i = 0
    while (i + 3 < len) { sum += arr[i] + arr[i+1] + arr[i+2] + arr[i+3]; i += 4 }
    while (i < len) { sum += arr[i++] }
    return sum
}

val arr    = IntArray(10_000_000) { it % 100 }
val rounds = 10

val s1 = System.nanoTime(); var r1 = 0L; repeat(rounds) { r1 = sumNormal(arr) }
val m1 = (System.nanoTime() - s1) / 1_000_000.0

val s2 = System.nanoTime(); var r2 = 0L; repeat(rounds) { r2 = sumUnrolled(arr) }
val m2 = (System.nanoTime() - s2) / 1_000_000.0

println("[Loop] 일반 루프 ${rounds}회:  ${"%.2f".format(m1)}ms (합=$r1)")
println("[Loop] 언롤 루프 ${rounds}회:  ${"%.2f".format(m2)}ms (합=$r2)")
println("[Loop] JIT 워밍업 후 자동 최적화로 두 값 수렴")

// ── 3. 공통 부분식 제거 & 상수 폴딩 ─────────────────────────────
println("\n=== 3. CSE & 상수 폴딩 ===")

fun inefficient(a: Int, b: Int) = (a * b) + (a * b) * 2 + (a * b) * 3

val xv = 2 + 3           // javac → 5로 폴딩
val yv = 10 * 20 / 4     // javac → 50으로 폴딩
println("[CSE] x = $xv (컴파일 시 2+3 → 5)")
println("[CSE] y = $yv (컴파일 시 10*20/4 → 50)")
println("[CSE] inefficient(3, 7) = ${inefficient(3, 7)}")
println("[CSE] 내부적으로 a*b=21 한 번만 계산 후 재사용")

// ── 4. 죽은 코드 제거 (DCE) ──────────────────────────────────────
println("\n=== 4. 죽은 코드 제거 ===")

fun withDeadCode(debug: Boolean): String {
    val result = "computed"
    if (debug) { println("debug: result=$result") }  // debug=false면 JIT가 제거
    return result
}

val startDCE = System.nanoTime()
var r = ""
repeat(100_000) { r = withDeadCode(false) }
val msDCE = (System.nanoTime() - startDCE) / 1_000_000.0
println("[DCE] 100,000회 호출: ${"%.2f".format(msDCE)}ms (if 블록 JIT 제거)")

// ── 5. JIT 계층형 컴파일 5단계 ───────────────────────────────────
println("\n=== 5. JIT 계층형 컴파일 5단계 ===")
println("""
  Level 0: 인터프리터 (프로파일링 시작)
  Level 1: C1 컴파일 (프로파일링 없음)
  Level 2: C1 컴파일 (호출 카운터)
  Level 3: C1 컴파일 (전체 프로파일링)
  Level 4: C2 컴파일 (최대 최적화)

  전환 경로:
    0 → 3 → 4: 일반 (핫 메서드)
    0 → 2 → 3 → 4: C2 큐 바쁠 때
    0 → 3 → 1: deopt 이력 있을 때
""".trimIndent())

fun hotLoop(n: Int): Long { var s = 0L; for (i in 0..n) s += i; return s }
val checkpoints = listOf(100, 2_000, 10_000, 50_000)
var totalCalls = 0
for (target in checkpoints) {
    val start = System.nanoTime()
    while (totalCalls < target) { hotLoop(1000); totalCalls++ }
    val ns = (System.nanoTime() - start) / totalCalls.coerceAtLeast(1)
    println("[Tier] 누적 ${totalCalls}회 → 평균 ${ns}ns/call")
}
