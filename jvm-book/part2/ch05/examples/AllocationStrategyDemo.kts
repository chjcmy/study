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

// ── 5. 안전 지점(Safepoint) 지연 — counted vs uncounted loop ──────
println("\n=== 5. 안전 지점(Safepoint) 지연 ===")

println("""
[Safepoint] 안전 지점이란?
  JVM이 GC를 시작하려면 모든 스레드가 안전 지점에서 멈춰야 한다
  안전 지점 = OopMap이 기록된 지점 (메서드 호출, 루프 백엣지, 예외 발생 등)
  스레드는 "폴링 플래그"를 확인하고 자발적으로 멈춤

[Safepoint] Counted Loop 문제:
  int 카운터 루프는 JIT가 "counted loop"로 최적화 → 백엣지에 안전 지점 미삽입
  → 루프가 끝날 때까지 GC STW가 지연됨!

  해결: long 카운터 사용 (uncounted loop → 백엣지에 안전 지점 삽입)
        또는 -XX:+UseCountedLoopSafepoints (JDK 14+ 기본)
""".trimIndent())

// counted loop (int) vs uncounted loop (long) 처리 시간 비교
val LOOP_COUNT = 500_000_000

val cs = System.nanoTime()
var sumInt = 0
for (i in 0 until LOOP_COUNT.toInt()) { sumInt += i and 1 }  // int 카운터 (counted loop)
val cm = (System.nanoTime() - cs) / 1_000_000.0

val us = System.nanoTime()
var sumLong = 0L
for (i in 0L until LOOP_COUNT.toLong()) { sumLong += i and 1L }  // long 카운터 (uncounted loop)
val um = (System.nanoTime() - us) / 1_000_000.0

println("[Safepoint] int  루프 (counted):   ${"%.0f".format(cm)}ms — GC가 이 루프 동안 대기할 수 있음")
println("[Safepoint] long 루프 (uncounted): ${"%.0f".format(um)}ms — 백엣지마다 안전 지점 존재")
println("[Safepoint] log-friends drainTo(100)은 최대 100건 → 안전 지점 문제 낮음")

// ── 6. Runtime.exec() 메모리 함정 ────────────────────────────────
println("\n=== 6. Runtime.exec() 메모리 함정 ===")
println("""
[exec] Runtime.exec("curl ...") 호출 시:
  1. JVM 프로세스 fork() → 부모 메모리를 Copy-on-Write(CoW)로 복제
  2. 힙 크기가 크면 (예: -Xmx4g) fork 직후 4GB 메모리 예약
  3. 실제 exec() 전까지 두 프로세스가 동일 메모리 공유
  4. exec() 완료 후 자식 프로세스는 독립 — 그러나 짧은 순간 메모리 2배 소비

  문제: OS 메모리 부족 → fork() 실패 → OutOfMemoryError (힙은 여유 있는데!)

  해결:
    - HttpClient/OkHttp로 대체 (외부 프로세스 fork 없음)
    - ProcessBuilder 사용 시 힙을 최소화
    - 컨테이너 환경: -Xmx는 컨테이너 메모리의 50% 이하 (fork 여유 확보)

  log-friends:
    → Kafka 클라이언트가 Java 소켓으로 직접 통신 → exec() 없음 → 안전
    → ByteBuddy attach도 JVM 내부 API → exec() 없음
""".trimIndent())

// ── 7. Eden → Survivor → Old 승격 흐름 요약 ─────────────────────
println("\n=== 7. Eden → Survivor → Old 승격 흐름 ===")
println("""
[Promotion] 5가지 할당/승격 전략:
  ① Eden 우선 할당
     - 모든 새 객체는 Eden에 먼저 할당 (TLAB 사용)
     - Eden이 가득 차면 Minor GC 발생

  ② 큰 객체 직접 Old Gen 할당 (-XX:PretenureSizeThreshold)
     - 긴 배열, 큰 문자열
     - Eden에서 복사되는 오버헤드 없음 대신 Old Gen 압박

  ③ 장기 생존 객체 Old 승격 (-XX:MaxTenuringThreshold=15)
     - Minor GC 생존 횟수 = 객체 나이
     - 기본 15번 생존 시 Old Gen으로 이동

  ④ 동적 나이 판정
     - 같은 나이 객체 합 > Survivor 50% → 그 나이 이상 즉시 승격
     - 임계값보다 빠르게 Old로 보낼 수 있음

  ⑤ 공간 할당 보장 (HandlePromotionFailure)
     - Minor GC 전: Old 여유 공간 ≥ Young 전체 or 이전 평균 승격량 확인
     - 여유 없으면 Full GC 먼저 → Minor GC

[Promotion] log-friends 적용:
  AgentEvent 객체: 단명 → Eden에서 Minor GC로 빠르게 수거 (정상)
  KafkaProducer 버퍼(32MB): 큰 객체 → Old Gen 직접 할당 가능
  CompanionObject.instance: static 필드 = GC Root → 영구 생존 (의도된 설계)
""".trimIndent())
