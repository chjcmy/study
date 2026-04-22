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

// ── 4. Heap OOM (주석 해제 후 실행, JVM 종료됨) ───────────────────
// println("\n=== 4. Heap OOM ===")
// val list = mutableListOf<ByteArray>()
// var allocated = 0L
// try {
//     while (true) { list.add(ByteArray(1024 * 1024)); allocated++ }
// } catch (e: OutOfMemoryError) { println("[Heap] OOM at ${allocated}MB") }
