#!/usr/bin/env kotlinc -script
/**
 * 12장 — 자바 메모리 모델(JMM)과 스레드
 * 실행: kotlinc -script MemoryModelDemo.kts
 */

import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

// ── 1. 가시성 — volatile ──────────────────────────────────────────
println("=== 1. 가시성 — volatile ===")

@Volatile var volatileFlag = false

val t1 = Thread({
    println("[Visibility] Writer: volatileFlag = true 설정 (50ms 후)")
    Thread.sleep(50)
    volatileFlag = true
}, "volatile-writer")

val t2 = Thread({
    var count = 0
    while (!volatileFlag) count++
    println("[Visibility] Reader: volatileFlag 감지 (spin ${count}회)")
}, "volatile-reader")

t2.start(); t1.start()
t1.join(); t2.join()
println("[Visibility] volatile 없으면 Reader가 영원히 루프할 수 있음")

// ── 2. 원자성 — i++ 경쟁 조건 ────────────────────────────────────
println("\n=== 2. 원자성 — i++ 경쟁 조건 ===")

var counter = 0
val atomicCounter = AtomicInteger(0)
val threads = 10; val increments = 100_000
val latch = CountDownLatch(threads)

repeat(threads) {
    Thread({
        repeat(increments) { counter++; atomicCounter.incrementAndGet() }
        latch.countDown()
    }).start()
}
latch.await()
val expected = threads * increments
println("[Atomic] 예상값:       $expected")
println("[Atomic] 일반 counter: $counter (손실 = ${expected - counter})")
println("[Atomic] AtomicInt:    ${atomicCounter.get()} (손실 없음)")

// ── 3. happens-before ────────────────────────────────────────────
println("\n=== 3. happens-before ===")

@Volatile var sharedValue = 0
var nonVolatile = 0

val writer = Thread({
    nonVolatile = 42          // 1. 일반 쓰기
    sharedValue = 1           // 2. volatile 쓰기 → 1도 함께 플러시
}, "hb-writer")

val reader = Thread({
    while (sharedValue == 0) {}
    println("[HB] sharedValue=$sharedValue, nonVolatile=$nonVolatile")
    println("[HB] volatile 쓰기 HB volatile 읽기 → nonVolatile도 보임")
}, "hb-reader")

reader.start(); writer.start()
writer.join(); reader.join()

println("""
[HB] 8가지 happens-before 규칙:
  1. 프로그램 순서   4. Thread.start()
  2. Monitor unlock  5. Thread.join()
  3. volatile 쓰기   8. 전이성 (A HB B, B HB C → A HB C)
""".trimIndent())

// ── 4. 이중 확인 잠금 (DCL) ──────────────────────────────────────
println("=== 4. 이중 확인 잠금 ===")

class SafeSingleton private constructor() {
    companion object {
        @Volatile private var instance: SafeSingleton? = null
        fun getInstance() = instance ?: synchronized(this) {
            instance ?: SafeSingleton().also { instance = it }
        }
    }
}

val latch2 = CountDownLatch(100)
val instances = mutableSetOf<SafeSingleton>()
val lock = Any()
repeat(100) {
    Thread({
        synchronized(lock) { instances.add(SafeSingleton.getInstance()) }
        latch2.countDown()
    }).start()
}
latch2.await()
println("[DCL] SafeSingleton 인스턴스 수 (1이어야 함): ${instances.size}")

// ── 5. long/double 원자성 ────────────────────────────────────────
println("\n=== 5. long/double 원자성 ===")
println("""
[Long] 32비트 JVM에서 long(64비트) = 32비트 쓰기 2번
  → Word Tearing: 상위/하위 32비트가 다른 스레드에 혼합될 수 있음
  해결: volatile long, AtomicLong, synchronized

  64비트 JVM에서는 대부분 안전하지만 JMM 스펙상 보장 안 됨
  → log-friends BatchTransporter의 sentCount, dropCount가 AtomicLong인 이유
""".trimIndent())

val atomicLong = AtomicLong(100L)
println("[Long] CAS(100→200): ${atomicLong.compareAndSet(100L, 200L)}, 현재: ${atomicLong.get()}")
println("[Long] CAS(100→300): ${atomicLong.compareAndSet(100L, 300L)}, 현재: ${atomicLong.get()}")
