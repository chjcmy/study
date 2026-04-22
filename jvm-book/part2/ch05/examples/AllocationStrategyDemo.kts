#!/usr/bin/env kotlinc -script
/**
 * 5장 — 고성능 메모리 할당 전략
 * 실행: kotlinc -script AllocationStrategyDemo.kts
 */

import java.nio.ByteBuffer
import java.util.concurrent.ArrayBlockingQueue

// ── 1. 다이렉트 메모리 vs 힙 메모리 ─────────────────────────────
println("=== 1. 다이렉트 메모리 vs 힙 ===")
val SIZE       = 64 * 1024 * 1024   // 64MB (스크립트용 축소)
val ITERATIONS = 100

val heap = ByteBuffer.allocate(SIZE)
val heapStart = System.nanoTime()
repeat(ITERATIONS) { heap.position(0); repeat(SIZE / 8) { heap.putLong(it.toLong()) } }
val heapMs = (System.nanoTime() - heapStart) / 1_000_000
println("[Direct] 힙 메모리 ${ITERATIONS}회: ${heapMs}ms")

val direct = ByteBuffer.allocateDirect(SIZE)
val directStart = System.nanoTime()
repeat(ITERATIONS) { direct.position(0); repeat(SIZE / 8) { direct.putLong(it.toLong()) } }
val directMs = (System.nanoTime() - directStart) / 1_000_000
println("[Direct] 다이렉트 메모리 ${ITERATIONS}회: ${directMs}ms")
println("[Direct] I/O가 많은 환경에선 다이렉트가 빠름 (커널 복사 생략)")

// ── 2. 객체 풀 패턴 ───────────────────────────────────────────────
println("\n=== 2. 객체 풀 패턴 ===")

class ByteBufferPool(private val bufferSize: Int, poolSize: Int) {
    private val pool = ArrayBlockingQueue<ByteBuffer>(poolSize)
    init { repeat(poolSize) { pool.offer(ByteBuffer.allocateDirect(bufferSize)) } }
    fun borrow()  = pool.poll() ?: ByteBuffer.allocateDirect(bufferSize)
    fun release(buf: ByteBuffer) { buf.clear(); pool.offer(buf) }
}

val pool       = ByteBufferPool(64 * 1024, 10)
val iterations = 100_000

val poolStart = System.nanoTime()
repeat(iterations) { val buf = pool.borrow(); buf.putInt(42); pool.release(buf) }
val poolMs = (System.nanoTime() - poolStart) / 1_000_000
println("[Pool] 풀 사용 ${iterations}회: ${poolMs}ms")

val allocStart = System.nanoTime()
repeat(iterations) { ByteBuffer.allocateDirect(64 * 1024).putInt(42) }
val allocMs = (System.nanoTime() - allocStart) / 1_000_000
println("[Pool] 매번 할당 ${iterations}회: ${allocMs}ms")
println("[Pool] 풀이 ${allocMs / poolMs.coerceAtLeast(1)}배 빠름 (대략)")

// ── 3. TLAB 멀티스레드 할당 ──────────────────────────────────────
println("\n=== 3. TLAB 멀티스레드 할당 ===")
println("[TLAB] ${Runtime.getRuntime().availableProcessors()}개 프로세서, 4 스레드 동시 할당")

val threads = (1..4).map { id ->
    Thread({
        val start = System.nanoTime()
        repeat(1_000_000) { Any() }
        val ms = (System.nanoTime() - start) / 1_000_000
        println("[TLAB] Thread-$id: 1M개 할당 완료 ${ms}ms")
    }, "tlab-thread-$id")
}
threads.forEach { it.start() }
threads.forEach { it.join() }
println("[TLAB] 모든 스레드 완료")

// ── 4. GC 전략 가이드 ────────────────────────────────────────────
println("\n=== 4. GC 전략 가이드 ===")
val maxHeap = Runtime.getRuntime().maxMemory() / 1024 / 1024
println("[GC Strategy] 현재 최대 힙: ${maxHeap}MB\n")

when {
    maxHeap < 512   -> println("< 512MB  → Serial GC   : -XX:+UseSerialGC")
    maxHeap < 4096  -> println("< 4GB    → G1 GC       : -XX:+UseG1GC -XX:MaxGCPauseMillis=200")
    maxHeap < 32768 -> println("< 32GB   → ZGC         : -XX:+UseZGC")
    else            -> println("32GB+    → ZGC         : -XX:+UseZGC (Compressed OOP 비활성화 주의)")
}
