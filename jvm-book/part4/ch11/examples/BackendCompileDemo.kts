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

// ── 6. OSR (On-Stack Replacement) 실증 ───────────────────────────
println("\n=== 6. OSR (On-Stack Replacement) ===")

fun processLargeArray(size: Int): Long {
    var sum = 0L
    for (i in 0 until size) {   // 백엣지 카운터 증가
        sum += i * 2L + 1L
    }
    return sum
}

// 메서드 1번만 호출해도 루프가 크면 OSR 발동
val osrStart = System.nanoTime()
val osrResult = processLargeArray(5_000_000)
val osrMs = (System.nanoTime() - osrStart) / 1_000_000.0
println("[OSR] 5M 루프, 메서드 1회 호출: ${"%.2f".format(osrMs)}ms, 결과=$osrResult")
println("[OSR] 루프 도중 JIT 전환 가능 (-XX:+PrintCompilation으로 확인)")
println("""
[OSR 원리]
  백엣지 카운터 임계값: ~14000 (C1) → 100000 (C2, 대략)
  루프 실행 중 JIT 컴파일 완료 → 루프 다음 이터레이션부터 네이티브 코드
  일반 JIT와 달리 메서드 재호출 필요 없음
""".trimIndent())

// ── 7. 동기화 제거 (Lock Elision) ────────────────────────────────
println("\n=== 7. 동기화 제거 (Lock Elision) ===")

// StringBuffer는 모든 메서드가 synchronized
// 하지만 메서드 내부에서만 사용하면 JIT가 동기화 제거
fun buildStringBuffer(): String {
    val sb = StringBuffer()   // synchronized, but escapes = false
    sb.append("Hello, ")
    sb.append("World")
    sb.append("!")
    return sb.toString()
}

// StringBuilder (비동기)와 비교
fun buildStringBuilder(): String {
    val sb = StringBuilder()
    sb.append("Hello, ")
    sb.append("World")
    sb.append("!")
    return sb.toString()
}

val lockRounds = 5_000_000
val sl1 = System.nanoTime(); var lr1 = ""; repeat(lockRounds) { lr1 = buildStringBuffer() }
val lm1 = (System.nanoTime() - sl1) / 1_000_000.0

val sl2 = System.nanoTime(); var lr2 = ""; repeat(lockRounds) { lr2 = buildStringBuilder() }
val lm2 = (System.nanoTime() - sl2) / 1_000_000.0

println("[LockElision] StringBuffer  (synchronized): ${"%.2f".format(lm1)}ms")
println("[LockElision] StringBuilder (unsynchronized): ${"%.2f".format(lm2)}ms")
println("[LockElision] JIT 워밍업 후 두 값 수렴 → 탈출 분석으로 동기화 제거됨")

// ── 8. 배열 경계 검사 제거 ────────────────────────────────────────
println("\n=== 8. 배열 경계 검사 제거 ===")

val boundsArr = IntArray(10_000_000) { it % 100 }

// 패턴 1: for(i in 0 until arr.size) → JIT가 검사 제거 가능
fun sumWithBoundsCheck(arr: IntArray): Long {
    var sum = 0L
    for (i in 0 until arr.size) sum += arr[i]   // 범위 증명 가능 → 검사 제거
    return sum
}

// 패턴 2: random access → 검사 제거 불가
fun sumRandomAccess(arr: IntArray, indices: IntArray): Long {
    var sum = 0L
    for (idx in indices) sum += arr[idx]   // 임의 인덱스 → 검사 필요
    return sum
}

val bRounds = 5
val bs1 = System.nanoTime(); var br1 = 0L; repeat(bRounds) { br1 = sumWithBoundsCheck(boundsArr) }
val bm1 = (System.nanoTime() - bs1) / 1_000_000.0 / bRounds

val randomIdx = IntArray(10_000_000) { (it * 7 + 3) % boundsArr.size }
val bs2 = System.nanoTime(); var br2 = 0L; repeat(bRounds) { br2 = sumRandomAccess(boundsArr, randomIdx) }
val bm2 = (System.nanoTime() - bs2) / 1_000_000.0 / bRounds

println("[BoundsCheck] 순차 루프 (검사 제거):  ${"%.2f".format(bm1)}ms/round")
println("[BoundsCheck] 랜덤 접근 (검사 유지):  ${"%.2f".format(bm2)}ms/round")

// ── 9. JIT vs AOT 비교 요약 ──────────────────────────────────────
println("\n=== 9. JIT vs AOT ===")
println("""
[JIT] 런타임 컴파일 — 장점:
  ① 프로파일 기반 최적화 (런타임에 타입/분기 패턴 관찰)
  ② 추측적 인라인 (단일 구현체일 때 가상 메서드 인라인)
  ③ 역최적화 후 재컴파일 (상황 변화 대응)
  단점: 워밍업 시간, 메모리 사용량 (Code Cache)

[AOT] 빌드시 컴파일 — 장점:
  ① 즉시 최대 성능 (워밍업 없음)
  ② 메모리 사용량 낮음
  단점: 런타임 프로파일 없음 → 피크 성능 낮음
  GraalVM native-image 제약: 리플렉션/동적 프록시/클래스 로딩 불가

[log-friends 관련]
  ByteBuddy 런타임 계측 = JIT 환경에서만 동작
  native-image 불가 이유:
    1. ByteBuddy가 런타임에 바이트코드 생성
    2. Instrumentation attach API native-image 미지원
    3. SpecScanner의 동적 클래스 로딩 의존
""".trimIndent())

val rt = Runtime.getRuntime()
println("[JIT] JVM 시작 후 Code Cache 사용량: ${(rt.totalMemory() - rt.freeMemory()) / 1024 / 1024}MB 힙")
