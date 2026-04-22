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

// ── 5. jstat 열 의미 실증 (MemoryMXBean) ─────────────────────────
println("\n=== 5. jstat 열 의미 — MemoryMXBean으로 실증 ===")

import java.lang.management.ManagementFactory
import java.lang.management.MemoryType

val memBeans = ManagementFactory.getMemoryPoolMXBeans()
println("[jstat] 현재 JVM 메모리 풀 상태:")
println("  ${"풀 이름".padEnd(30)} ${"타입".padEnd(8)} ${"사용".padEnd(10)} ${"최대"}")
println("  " + "─".repeat(65))
memBeans.forEach { pool ->
    val used = pool.usage.used / 1024
    val max  = if (pool.usage.max > 0) "${pool.usage.max / 1024}KB" else "-"
    val type = if (pool.type == MemoryType.HEAP) "HEAP" else "NON-HEAP"
    println("  ${pool.name.padEnd(30)} ${type.padEnd(8)} ${used}KB".padEnd(50) + max)
}

println("""
[jstat] jstat -gcutil <pid> 1000 열 의미:
  S0   : Survivor 0 사용률(%) — 현재 From/To 중 사용 중인 쪽
  S1   : Survivor 1 사용률(%)
  E    : Eden 사용률(%) — 빠르게 오르면 새 객체 할당 많음
  O    : Old Gen 사용률(%) — 천천히 오르다가 Full GC에서 급감
  M    : Metaspace 사용률(%) — ByteBuddy 클래스 생성 시 증가
  CCS  : Compressed Class Space 사용률(%)
  YGC  : Young GC 누적 횟수
  YGCT : Young GC 누적 시간(초) — YGC/YGCT = 평균 STW
  FGC  : Full GC 누적 횟수 — 높으면 위험 신호
  FGCT : Full GC 누적 시간(초)
  GCT  : 전체 GC 시간 합계

[jstat] log-friends 모니터링 포인트:
  E(Eden) 증가 속도: AgentEvent 생성 빈도와 비례
  M(Metaspace) 증가: ByteBuddy 변환 클래스 수 증가
  FGC > 0 지속 증가: 메모리 누수 또는 힙 부족 신호
""".trimIndent())

// ── 6. 스레드 덤프 분석 — ThreadMXBean ───────────────────────────
println("\n=== 6. 스레드 덤프 분석 ===")

val threadBean = ManagementFactory.getThreadMXBean()

// 데드락 감지
val deadlocked = threadBean.findDeadlockedThreads()
if (deadlocked != null && deadlocked.isNotEmpty()) {
    println("[jstack] 데드락 감지! 스레드 IDs: ${deadlocked.toList()}")
    threadBean.getThreadInfo(deadlocked).forEach { info ->
        println("[jstack] ${info.threadName} — ${info.threadState}")
        println("[jstack]   대기 중인 락: ${info.lockName}")
        println("[jstack]   락 보유자: ${info.lockOwnerName}")
    }
} else {
    println("[jstack] 데드락 없음 (데드락 스레드는 위에서 이미 생성됨)")
}

// 스레드 상태별 분류
println("\n[jstack] 스레드 상태별 분류:")
val threadInfos = threadBean.getThreadInfo(threadBean.allThreadIds, 0)
val byState = threadInfos.groupBy { it?.threadState }
byState.entries.sortedBy { it.key?.ordinal ?: 99 }.forEach { (state, threads) ->
    if (state != null) {
        val names = threads.mapNotNull { it?.threadName }.take(3)
        println("  $state (${threads.size}개): ${names.joinToString()} ${if (threads.size > 3) "..." else ""}")
    }
}

println("""
[jstack] jstack 사용법:
  jstack <pid>          기본 스레드 덤프
  jstack -l <pid>       락 정보 포함 (데드락 분석용)
  jstack -F <pid>       응답 없는 프로세스 강제 덤프

  스레드 상태 의미:
    RUNNABLE     : CPU 실행 중 (I/O 대기도 포함)
    BLOCKED      : synchronized 락 대기 (데드락 가능성)
    WAITING      : Object.wait(), join(), LockSupport.park()
    TIMED_WAITING: sleep(), wait(timeout), parkNanos()
    TERMINATED   : 종료됨

  log-friends 확인 포인트:
    "log-friends-batch-flush" 스레드 — TIMED_WAITING이 정상
    (ScheduledExecutorService.scheduleAtFixedRate 대기 중)
    BLOCKED 상태이면 flush()의 synchronized 블록에서 경쟁 발생 신호
""".trimIndent())

println("\n앱 실행 중 — Ctrl+C 로 종료")
Thread.currentThread().join()
