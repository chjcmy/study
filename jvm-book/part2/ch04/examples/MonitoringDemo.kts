#!/usr/bin/env kotlinc -script
/**
 * 4장 — 성능 모니터링 & 진단 도구
 * 실행: kotlinc -script MonitoringDemo.kts
 *
 * 실행 중 별도 터미널에서:
 *   jps -lv
 *   jstat -gcutil <pid> 1000
 *   jstack -l <pid>
 *   jmap -histo:live <pid> | head -20
 */

import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock

val pid = ProcessHandle.current().pid()

println("========================================")
println(" JVM 모니터링 대상 앱 실행 중")
println(" PID: $pid")
println("========================================\n")
println("모니터링 명령:")
println("  jstat -gcutil $pid 1000")
println("  jstack -l $pid")
println("  jmap -histo:live $pid | head -20\n")

// ── 1. 메모리 누수 시뮬레이터 ────────────────────────────────────
println("=== 1. 메모리 누수 시뮬레이터 시작 ===")
val leakedObjects = mutableListOf<ByteArray>()
Executors.newSingleThreadScheduledExecutor { r ->
    Thread(r, "memory-leak-simulator").apply { isDaemon = true }
}.scheduleAtFixedRate({
    leakedObjects.add(ByteArray(1024 * 1024))
    if (leakedObjects.size % 10 == 0) println("[Memory] Leaked ${leakedObjects.size}MB accumulated")
}, 0, 2, TimeUnit.SECONDS)

// ── 2. 데드락 생성 ────────────────────────────────────────────────
println("\n=== 2. 데드락 생성 ===")
val lockA = ReentrantLock(); val lockB = ReentrantLock()

Thread({
    lockA.lock()
    println("[Deadlock] Thread-A: lockA 획득, lockB 대기 중...")
    Thread.sleep(100)
    lockB.lock(); lockB.unlock(); lockA.unlock()
}, "deadlock-thread-A").start()

Thread({
    lockB.lock()
    println("[Deadlock] Thread-B: lockB 획득, lockA 대기 중...")
    Thread.sleep(100)
    lockA.lock(); lockA.unlock(); lockB.unlock()
}, "deadlock-thread-B").start()

Thread.sleep(500)
println("[Deadlock] 데드락 발생! jstack -l $pid 로 확인")

// ── 3. 스레드 상태 ────────────────────────────────────────────────
println("\n=== 3. 스레드 상태 생성 ===")
Thread({ var i = 0L; while (true) { i++ } }, "cpu-bound-thread").apply { isDaemon = true; start() }
Thread({ while (true) { Thread.sleep(Long.MAX_VALUE) } }, "sleeping-thread").apply { isDaemon = true; start() }
val monitor = Object()
Thread({ synchronized(monitor) { monitor.wait() } }, "waiting-thread").apply { isDaemon = true; start() }
println("[Thread] RUNNABLE / TIMED_WAITING / WAITING 스레드 생성 완료")

// ── 4. 힙 히스토그램 대상 ────────────────────────────────────────
println("\n=== 4. 힙 히스토그램 대상 생성 ===")
val strings = (1..100_000).map { "log-friends-$it" }
val arrays  = (1..10_000).map  { IntArray(100) }
println("[Histogram] String ${strings.size}개, IntArray ${arrays.size}개 생성")
println("[Histogram] jmap -histo:live $pid | grep 'String\\|int\\[' 로 확인")

println("\n앱 실행 중 — Ctrl+C 로 종료")
Thread.currentThread().join()
