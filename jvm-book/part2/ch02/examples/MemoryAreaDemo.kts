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

// ── 5. OOM 줄이기 — 3가지 패턴 ──────────────────────────────────
println("\n=== 5. OOM 줄이기 ===")

// ① 스트리밍: 전부 모으지 말고 하나씩 처리 후 버리기
println("[Stream] 스트리밍 처리 — 10만 건을 메모리에 안 쌓고 합산")
val streamSum = (1..100_000).asSequence()
    .map { it * 2 }
    .filter { it % 3 == 0 }
    .sum()
println("[Stream] 결과=$streamSum (List로 중간 수집 없음)")

// ② 크기 제한 큐: 오래된 것 자동 제거
println("\n[BoundedQueue] 크기 제한 — 최대 5개 유지")
val bounded = ArrayDeque<String>()
val maxSize = 5
repeat(10) { i ->
    if (bounded.size >= maxSize) bounded.removeFirst()
    bounded.addLast("item-$i")
}
println("[BoundedQueue] 10개 추가 후 보관 중인 항목: $bounded")

// ③ SoftReference: 메모리 부족 시 GC가 자동 수거
println("\n[SoftRef] SoftReference — 메모리 부족 시 자동 해제")
val softRefs = (1..5).map { java.lang.ref.SoftReference(ByteArray(1024) { it.toByte() }) }
System.gc()
val alive = softRefs.count { it.get() != null }
println("[SoftRef] GC 후 살아있는 참조: $alive / ${softRefs.size} (메모리 여유 있으면 유지)")

println("""
[요약] OOM 줄이는 핵심 원칙
  ① 스트리밍     전부 메모리에 올리지 말고 Sequence/Stream으로 하나씩 처리
  ② 크기 제한    컬렉션에 상한선 설정, 넘으면 오래된 것 제거
  ③ SoftReference  캐시 용도 — 메모리 부족하면 GC가 자동 수거
  ④ WeakReference  키 참조 없으면 자동 제거 (WeakHashMap)
""".trimIndent())

// ── 6. Heap OOM (주석 해제 후 실행, JVM 종료됨) ───────────────────
// println("\n=== 6. Heap OOM ===")
// val list = mutableListOf<ByteArray>()
// var allocated = 0L
// try {
//     while (true) { list.add(ByteArray(1024 * 1024)); allocated++ }
// } catch (e: OutOfMemoryError) { println("[Heap] OOM at ${allocated}MB") }
