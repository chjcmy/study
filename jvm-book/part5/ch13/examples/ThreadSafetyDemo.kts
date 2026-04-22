#!/usr/bin/env kotlinc -script
/**
 * 13장 — 스레드 안전성과 락 최적화
 * 실행: kotlinc -script ThreadSafetyDemo.kts   (JDK 21 필수 — 가상 스레드)
 */

import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.locks.ReentrantLock

// ── 1. synchronized vs ReentrantLock vs AtomicLong ────────────────
println("=== 1. synchronized vs ReentrantLock vs AtomicLong ===")

var syncCounter   = 0L
val rl            = ReentrantLock()
var lockCounter   = 0L
val atomicCounter = AtomicLong(0L)
val threads = 8; val increments = 500_000

// synchronized
syncCounter = 0
var latch = CountDownLatch(threads)
val t1 = System.nanoTime()
val syncObj = Any()
repeat(threads) { Thread({ repeat(increments) { synchronized(syncObj) { syncCounter++ } }; latch.countDown() }).start() }
latch.await(); val ms1 = (System.nanoTime() - t1) / 1_000_000.0

// ReentrantLock
lockCounter = 0
latch = CountDownLatch(threads)
val t2 = System.nanoTime()
repeat(threads) { Thread({ repeat(increments) { rl.lock(); try { lockCounter++ } finally { rl.unlock() } }; latch.countDown() }).start() }
latch.await(); val ms2 = (System.nanoTime() - t2) / 1_000_000.0

// AtomicLong
atomicCounter.set(0)
latch = CountDownLatch(threads)
val t3 = System.nanoTime()
repeat(threads) { Thread({ repeat(increments) { atomicCounter.incrementAndGet() }; latch.countDown() }).start() }
latch.await(); val ms3 = (System.nanoTime() - t3) / 1_000_000.0

val expected = (threads * increments).toLong()
println("[Lock] synchronized  : ${"%.2f".format(ms1)}ms, 결과=${syncCounter}/$expected")
println("[Lock] ReentrantLock : ${"%.2f".format(ms2)}ms, 결과=${lockCounter}/$expected")
println("[Lock] AtomicLong    : ${"%.2f".format(ms3)}ms, 결과=${atomicCounter.get()}/$expected")

// ── 2. 락 업그레이드 3단계 ───────────────────────────────────────
println("\n=== 2. 락 업그레이드 3단계 ===")
println("""
  편향 락 (Biased):     단일 스레드 독점, CAS 없이 스레드 ID 비교
  경량 락 (Lightweight): 교대 사용, Mark Word CAS + 스핀 대기
  중량 락 (Heavyweight): 실제 경합, OS Mutex → 컨텍스트 스위칭

  JDK 15: 편향 락 deprecated (성능 이점 줄어듦)
""".trimIndent())

class Counter { private var count = 0; @Synchronized fun increment() { count++ }; fun get() = count }

// 단일 스레드 → 편향 락
val c1 = Counter()
val s1 = System.nanoTime(); repeat(1_000_000) { c1.increment() }
val m1 = (System.nanoTime() - s1) / 1_000_000.0
println("[LockUpgrade] 단일 스레드 100만회: ${"%.2f".format(m1)}ms (편향 락)")

// 4스레드 경합 → 중량 락
val c2 = Counter(); val l2 = CountDownLatch(4)
val s2 = System.nanoTime()
repeat(4) { Thread({ repeat(250_000) { c2.increment() }; l2.countDown() }).start() }
l2.await(); val m2 = (System.nanoTime() - s2) / 1_000_000.0
println("[LockUpgrade] 4스레드 경합 100만회: ${"%.2f".format(m2)}ms (중량 락)")

// ── 3. ReentrantLock 고급 기능 ───────────────────────────────────
println("\n=== 3. ReentrantLock — Condition ===")

val fairLock  = ReentrantLock(true)
val condition = fairLock.newCondition()
var ready     = false

val consumer = Thread({
    fairLock.lock()
    try { while (!ready) { println("[Consumer] 대기 중..."); condition.await() }
          println("[Consumer] 데이터 수신!") }
    finally { fairLock.unlock() }
}, "consumer")

val producer = Thread({
    Thread.sleep(200); fairLock.lock()
    try { ready = true; condition.signal(); println("[Producer] 데이터 생성 + 통지") }
    finally { fairLock.unlock() }
}, "producer")

consumer.start(); producer.start()
consumer.join(); producer.join()

val got = fairLock.tryLock(100, TimeUnit.MILLISECONDS)
println("[ReentrantLock] tryLock(100ms) 성공: $got")
if (got) fairLock.unlock()

// ── 4. 가상 스레드 vs 플랫폼 스레드 ─────────────────────────────
println("\n=== 4. 가상 스레드 vs 플랫폼 스레드 ===")

val tasks = 10_000; val taskDuration = 10L

val pl = CountDownLatch(tasks)
val platformPool = Executors.newFixedThreadPool(200)
val pt1 = System.nanoTime()
repeat(tasks) { platformPool.submit { Thread.sleep(taskDuration); pl.countDown() } }
pl.await(); platformPool.shutdown()
val pm = (System.nanoTime() - pt1) / 1_000_000.0

val vl = CountDownLatch(tasks)
val virtualPool = Executors.newVirtualThreadPerTaskExecutor()
val vt1 = System.nanoTime()
repeat(tasks) { virtualPool.submit { Thread.sleep(taskDuration); vl.countDown() } }
vl.await(); virtualPool.shutdown()
val vm = (System.nanoTime() - vt1) / 1_000_000.0

println("[Virtual] 플랫폼 스레드(200개): ${"%.0f".format(pm)}ms")
println("[Virtual] 가상 스레드:          ${"%.0f".format(vm)}ms")
println("[Virtual] 가상 스레드 빠른 이유: I/O 대기 중 OS 스레드 반환")

// ── 5. LongAdder vs AtomicLong ───────────────────────────────────
println("\n=== 5. LongAdder vs AtomicLong ===")

val atomic = AtomicLong(0); val adder = java.util.concurrent.atomic.LongAdder()
val t16 = 16; val inc = 1_000_000

var la = CountDownLatch(t16)
val at1 = System.nanoTime()
repeat(t16) { Thread({ repeat(inc) { atomic.incrementAndGet() }; la.countDown() }).start() }
la.await(); val am = (System.nanoTime() - at1) / 1_000_000.0

la = CountDownLatch(t16)
val at2 = System.nanoTime()
repeat(t16) { Thread({ repeat(inc) { adder.increment() }; la.countDown() }).start() }
la.await(); val adm = (System.nanoTime() - at2) / 1_000_000.0

println("[Spin] AtomicLong (${t16}스레드): ${"%.2f".format(am)}ms, 값=${atomic.get()}")
println("[Spin] LongAdder  (${t16}스레드): ${"%.2f".format(adm)}ms, 값=${adder.sum()}")
println("[Spin] LongAdder 원리: 스레드마다 별도 셀에 누적 → sum() 시 합산")
