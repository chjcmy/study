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

// ── 6. 스레드 안전성 5단계 & 락 최적화 4가지 ────────────────────────
println("\n=== 6. 스레드 안전성 5단계 & 락 최적화 ===")

println("""
[Safety] 스레드 안전성 5단계 (Brian Goetz):
  ① 불변(Immutable)        — val + final 필드, 동기화 불필요 (String, Integer)
  ② 절대 안전               — 어떤 상황에서도 안전 (사실상 달성 불가)
  ③ 상대 안전               — 개별 연산은 안전, 복합 연산은 외부 동기화 필요
                               (ConcurrentHashMap, Vector, Collections.synchronizedList)
  ④ 스레드 호환              — 기본 스레드 안전하지 않음, 호출자가 동기화 (ArrayList, HashMap)
  ⑤ 스레드 대립              — 동기화해도 안전하지 않음 (System.setIn/Out 등)

[Safety] ConcurrentHashMap 상대 안전 예시:
  map.put("k", 1)       → 스레드 안전 (개별 연산)
  if (!map.containsKey("k")) map.put("k", 1)  → 안전하지 않음 (복합 연산)
  map.putIfAbsent("k", 1)  → 안전 (원자적 복합 연산)
""".trimIndent())

// 상대 안전 문제 실증: ConcurrentHashMap putIfAbsent vs 복합 연산
val cmap = java.util.concurrent.ConcurrentHashMap<String, Int>()
val putLatch = CountDownLatch(100)
val results = mutableListOf<Boolean>()
val resultsLock = Any()
repeat(100) {
    Thread({
        val inserted = cmap.putIfAbsent("key", it) == null
        synchronized(resultsLock) { results.add(inserted) }
        putLatch.countDown()
    }).start()
}
putLatch.await()
val successCount = results.count { it }
println("[Safety] putIfAbsent 100 스레드: 성공 ${successCount}회 (1이어야 함) → 원자적 보장")

// 락 최적화 4가지 실증
println("\n[LockOpt] 4가지 JVM 락 최적화:")

// ① 스핀 락 (Spin Lock) — CAS 기반
println("\n[LockOpt] ① 스핀 락 — CAS 루프로 OS 블로킹 회피")
val cas = java.util.concurrent.atomic.AtomicReference<String>(null)
val spinLatch = CountDownLatch(4)
val spinStart = System.nanoTime()
repeat(4) { id ->
    Thread({
        // CAS spin — 락 획득까지 반복
        var spins = 0
        while (!cas.compareAndSet(null, "T$id")) { spins++ }
        Thread.sleep(1)  // 임계 구역
        cas.set(null)    // 해제
        if (id == 0) println("[Spin] Thread-$id: ${spins}회 스핀 후 획득")
        spinLatch.countDown()
    }).start()
}
spinLatch.await()
println("[Spin] CAS 스핀: ${(System.nanoTime() - spinStart) / 1_000_000}ms")

// ② 락 제거 (Lock Elision) — JIT 탈출 분석
println("\n[LockOpt] ② 락 제거 — StringBuffer(synchronized)가 지역 변수이면 락 제거")
fun buildBuffer(): String {
    val sb = StringBuffer()  // synchronized 메서드들
    repeat(100) { sb.append("x") }
    return sb.toString()     // sb는 지역 변수 → JIT가 락 제거
}
val elisionStart = System.nanoTime()
repeat(1_000_000) { buildBuffer() }
val elisionMs = (System.nanoTime() - elisionStart) / 1_000_000
println("[Elision] StringBuffer 100만 호출: ${elisionMs}ms (락 제거로 오버헤드 없음)")

// ③ 락 확장 (Lock Coarsening) — 연속된 락을 하나로 합침
println("\n[LockOpt] ③ 락 확장 — 반복문 내 연속 synchronized를 루프 밖으로 합침")
val sb2 = StringBuffer()
val coarseStart = System.nanoTime()
repeat(1_000_000) { sb2.append("a") }  // 1M번 개별 lock/unlock
val coarseMs = (System.nanoTime() - coarseStart) / 1_000_000
println("[Coarse] append 100만회: ${coarseMs}ms (JIT가 반복문 전체를 하나의 락으로 확장)")

// ④ 편향 락 → 경량 락 → 중량 락 팽창 (이미 섹션 2에서 측정)
println("""
[LockOpt] ④ 락 팽창 — Mark Word 구조 변화:
  편향 락: [thread_id(54)|epoch(2)|01] — 스레드 ID만 비교, CAS 없음
  경량 락: [ptr_to_stack(62)|00]       — 스택 LR에 Mark Word 복사, CAS 1회
  중량 락: [ptr_to_monitor(62)|10]     — OS 뮤텍스, 컨텍스트 스위치

  단방향 팽창: 편향 → 경량 → 중량 (역방향 없음)
  JDK 15+: 편향 락 기본 비활성화 (revocation 비용 > CAS 비용)
""".trimIndent())

// ── 7. log-friends BatchTransporter 동시성 분석 & jstack ────────────
println("\n=== 7. BatchTransporter 동시성 설계 & jstack 진단 ===")

println("""
[BatchTransporter] 동시성 구성 요소 분석:

  ① @Volatile instance — DCL + 명령어 재배열 방지
     생성자 실행(②)과 참조 할당(③) 재배열 방지
     다른 스레드가 반쯤 초기화된 인스턴스를 볼 수 없음

  ② synchronized(this) getInstance() — 초기화 뮤텍스
     한 번만 실행, 이후 volatile 읽기만으로 인스턴스 반환

  ③ AtomicBoolean running — CAS 기반 상태 플래그
     shutdown() 중복 호출 방지 (compareAndSet(true, false))

  ④ AtomicLong sentCount / dropCount — CAS 카운터
     락 없이 멀티스레드 안전한 증분 (incrementAndGet)
     LongAdder보다 AtomicLong: 실시간 읽기(get)가 필요한 경우 적합

  ⑤ LinkedBlockingQueue(10000) — put/takeLock 분리 ReentrantLock
     enqueue(producer)와 flush(consumer)가 동시 진행 가능
     ArrayBlockingQueue(단일 락)보다 처리량 우수

  ⑥ @Synchronized flush() — 복합 연산 원자성 보장
     drainTo + Kafka 전송을 하나의 락으로 묶음
     스케줄러 flush + 즉시 flush 동시 호출 방지

[BatchTransporter] 락 경쟁 진단:
  정상: "log-friends-batch-flush" → TIMED_WAITING
  이상: → BLOCKED → flush() 내 synchronized에서 경쟁
  → 해결: batchSize 증가 또는 interval 증가로 flush 빈도 감소
""".trimIndent())

// jstack 스레드 상태 진단 (ThreadMXBean으로 현재 스레드 분석)
import java.lang.management.ManagementFactory

val threadBean = ManagementFactory.getThreadMXBean()
val allInfos = threadBean.getThreadInfo(threadBean.allThreadIds, 3)
val byState = allInfos.filterNotNull().groupBy { it.threadState }

println("[jstack] 현재 JVM 스레드 상태 분포:")
byState.entries.sortedBy { it.key.ordinal }.forEach { (state, list) ->
    val names = list.map { it.threadName }.take(3)
    println("  ${state.name.padEnd(14)} ${list.size}개: ${names.joinToString()} ${if (list.size > 3) "..." else ""}")
}

println("""

[jstack] 진단 명령:
  jstack <pid>           기본 스레드 덤프
  jstack -l <pid>        락 소유/대기 정보 포함 (데드락 분석)
  jstack <pid> | grep -A 10 "log-friends-batch-flush"

  진단 포인트:
  BLOCKED  on <0x...> (a com.logfriends.agent.BatchTransporter)
    → flush() synchronized 진입 대기 — 배치 간격 조정 필요
  WAITING  on java.util.concurrent.locks.AbstractQueuedSynchronizer
    → LinkedBlockingQueue.take() 정상 대기 (이벤트 없음)
  RUNNABLE + lock: ... locked <0x...>
    → Kafka I/O 처리 중 (정상)
""".trimIndent())
