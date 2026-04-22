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

// ── 6. JMM 8가지 연산 & MESI 시뮬레이션 ────────────────────────────
println("\n=== 6. JMM 8가지 연산 & MESI 프로토콜 ===")

println("""
[JMM] 메인 메모리 ↔ 작업 메모리 8가지 연산:
  lock    : 메인 메모리 변수를 특정 스레드 전용으로 잠금
  unlock  : lock 해제 (타 스레드 접근 가능)
  read    : 메인 메모리 → 전송 시작 (load와 쌍)
  load    : 작업 메모리에 값 적재 (read의 완료)
  use     : 작업 메모리 값 → 실행 엔진에 전달 (읽기 연산마다)
  assign  : 실행 엔진 결과 → 작업 메모리에 저장 (쓰기 연산마다)
  store   : 작업 메모리 → 전송 시작 (write와 쌍)
  write   : 메인 메모리에 값 반영 (store의 완료)

[JMM] volatile 쓰기 순서:
  assign → store → write  (즉시 메인 메모리 반영 강제)
  + 다른 스레드의 해당 변수 작업 메모리 무효화 (MESI Invalid)

[MESI] 캐시 라인 상태:
  Modified  : 이 캐시에서만 수정됨, 메인 메모리 불일치
  Exclusive : 이 캐시만 보유, 메인 메모리 일치
  Shared    : 여러 캐시가 보유, 일치
  Invalid   : 무효화됨 → 다음 읽기 시 메인 메모리에서 로드
""".trimIndent())

// MESI "Shared → Modified → Invalid" 시퀀스 시뮬레이션
class MesiCache(val name: String) {
    enum class State { MODIFIED, EXCLUSIVE, SHARED, INVALID }
    var state = State.SHARED; var value = 0
    fun write(v: Int) { value = v; state = State.MODIFIED }
    fun invalidate() { state = State.INVALID }
    fun loadFromMain(v: Int) { value = v; state = State.SHARED }
    override fun toString() = "$name: state=${state.name}, value=$value"
}

val cacheA = MesiCache("Core0-L1"); cacheA.loadFromMain(10)
val cacheB = MesiCache("Core1-L1"); cacheB.loadFromMain(10)
println("[MESI] 초기 (둘 다 Shared, value=10):")
println("  $cacheA"); println("  $cacheB")

// Core0이 value를 20으로 쓰기 → Core1 캐시 무효화
cacheA.write(20); cacheB.invalidate()
println("[MESI] Core0 쓰기 후 (Core1 Invalid):")
println("  $cacheA"); println("  $cacheB")

// Core1이 읽기 → 메인 메모리(=20)에서 재로드
cacheB.loadFromMain(20)
println("[MESI] Core1 재로드 후 (가시성 보장):")
println("  $cacheA"); println("  $cacheB")

// volatile이 하는 일: write → 즉시 스토어 버퍼 플러시 + 타 코어 캐시 Invalid
println("[MESI] volatile 쓰기 = lock 접두사 → 스토어 버퍼 flush + 캐시 Invalid 전파")

// ── 7. 명령어 재배열 방지 & 스레드 상태 전이 ────────────────────────
println("\n=== 7. 명령어 재배열 방지 & 스레드 상태 전이 ===")

println("""
[Reorder] volatile 없는 DCL의 위험:
  instance = new Singleton() 는 3단계:
    ① 메모리 할당 (alloc)
    ② 생성자 실행 (init)
    ③ instance 참조 할당 (assign)
  JIT/CPU가 ②③ 재배열 가능 → 다른 스레드가 null 아닌 미초기화 객체를 볼 수 있음
  @Volatile → StoreStore + LoadLoad 배리어 삽입 → ②가 ③보다 반드시 먼저
""".trimIndent())

// 스레드 상태 6가지 실증
println("[ThreadState] JVM 스레드 6가지 상태:")

val stateResults = mutableMapOf<Thread.State, String>()

// NEW
val newThread = Thread { }
stateResults[newThread.state] = "newThread (start() 호출 전)"

// RUNNABLE
val runnable = Thread { var x = 0L; while (x < Long.MAX_VALUE / 10) x++ }.apply { isDaemon = true; start() }
Thread.sleep(10)
stateResults[runnable.state] = "runnable (CPU 연산 중)"

// TIMED_WAITING
val timedWaiting = Thread { Thread.sleep(10_000) }.apply { isDaemon = true; start() }
Thread.sleep(20)
stateResults[timedWaiting.state] = "timedWaiting (sleep 중)"

// WAITING
val mon = Object()
val waiting = Thread { synchronized(mon) { mon.wait() } }.apply { isDaemon = true; start() }
Thread.sleep(20)
stateResults[waiting.state] = "waiting (monitor.wait() 중)"

// BLOCKED
val blockLock = Any()
synchronized(blockLock) {
    val blocked = Thread { synchronized(blockLock) {} }.apply { start() }
    Thread.sleep(20)
    stateResults[blocked.state] = "blocked (synchronized 진입 대기)"
}

stateResults.forEach { (state, desc) ->
    println("  ${state.name.padEnd(14)} ← $desc")
}

println("""

[ThreadState] 가상 스레드(JDK 21) 상태 전이:
  RUNNING → I/O 블로킹 → unmount (스택을 힙에 저장) → carrier thread 해제
  I/O 완료 → ForkJoinPool이 carrier 재배치 → mount → RUNNING
  결과: OS 스레드 수백 개로 수만 개의 가상 스레드 처리 가능

[ThreadState] log-friends:
  "log-friends-batch-flush": TIMED_WAITING 정상 (scheduleAtFixedRate 대기)
  BLOCKED 지속 → flush() synchronized 블록에서 경쟁 → 배치 크기/간격 조정 필요
""".trimIndent())
