#!/usr/bin/env kotlinc -script
/**
 * 2장 — 런타임 데이터 영역 & 메모리 오버플로
 * 실행: kotlinc -script MemoryAreaDemo.kts
 */

// ── 1. StackOverflowError ─────────────────────────────────────────
var depth = 0
fun recurse() { depth++; recurse() }

println("=== 1. StackOverflow ===")
try { recurse() }
catch (e: StackOverflowError) { println("[Stack] StackOverflowError at depth=$depth") }

// ── 2. String Intern (상수 풀) ────────────────────────────────────
println("\n=== 2. String Intern (상수 풀) ===")
val a = "hello"
val b = String("hello".toCharArray())
val c = b.intern()

println("[StringPool] a == b   : ${a === b}")   // false
println("[StringPool] a == c   : ${a === c}")   // true
println("[StringPool] b == c   : ${b === c}")   // false

// ── 3. 객체 헤더 크기 ────────────────────────────────────────────
println("\n=== 3. 객체 헤더 크기 ===")
val runtime = Runtime.getRuntime()
System.gc()
val before  = runtime.totalMemory() - runtime.freeMemory()
val objects = Array(100_000) { Any() }
System.gc()
val after     = runtime.totalMemory() - runtime.freeMemory()
val perObject = (after - before) / objects.size

println("[Header] 빈 객체 1개당 약 ${perObject}B (헤더 포함)")
println("[Header] 이론값: Mark Word 8B + 클래스 포인터 4B(압축) = 최소 12B, 패딩 후 16B")

// ── 4. 스택 vs 힙 — 변수 저장 위치 ──────────────────────────────
println("\n=== 4. 스택 vs 힙 — 변수 저장 위치 ===")

fun stackVsHeap() {
    val primitive = 42          // 원시 타입 → 스택에 값 직접 저장
    val text = "hello"          // 객체 타입 → 스택에 힙 주소(참조), 실제 값은 힙
    val arr = IntArray(3) { it } // 배열 → 힙에 저장, arr 변수는 스택에 주소만

    println("[Stack] primitive=$primitive  (스택에 값 직접)")
    println("[Heap]  text 참조: ${System.identityHashCode(text).toString(16)}  (스택은 이 주소만 보관)")
    println("[Heap]  arr  참조: ${System.identityHashCode(arr).toString(16)}  (배열 본체는 힙)")
}
stackVsHeap()

println("""
[요약]
  원시 타입 지역 변수 (Int, Long, Double ...)
    → 스택 프레임에 값 직접 저장
    → 메서드 종료 시 자동 제거 (GC 불필요)

  객체 타입 지역 변수 (String, Array, 사용자 클래스 ...)
    → 스택 프레임에 힙 주소(참조)만 저장
    → 실제 데이터는 힙에 존재
    → GC가 힙을 정리할 때 수거

  클래스/파일 수준 변수 (var depth = 0 처럼 메서드 밖)
    → 내부적으로 클래스 필드로 컴파일 → 힙에 저장
""".trimIndent())

// ── 5. Heap OOM (주석 해제 후 실행, JVM 종료됨) ───────────────────
// println("\n=== 4. Heap OOM ===")
// val list = mutableListOf<ByteArray>()
// var allocated = 0L
// try {
//     while (true) { list.add(ByteArray(1024 * 1024)); allocated++ }
// } catch (e: OutOfMemoryError) { println("[Heap] OOM at ${allocated}MB") }
