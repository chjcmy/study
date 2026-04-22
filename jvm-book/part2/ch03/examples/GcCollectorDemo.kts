#!/usr/bin/env kotlinc -script
/**
 * 3장 — GC 컬렉터 비교 & 튜닝
 * 실행: kotlinc -script GcCollectorDemo.kts
 *
 * 컬렉터별 비교 (컴파일 후):
 *   kotlinc GcCollectorDemo.kts -include-runtime -d gc.jar
 *   java -XX:+UseG1GC  -Xlog:gc* -jar gc.jar
 *   java -XX:+UseZGC   -Xlog:gc* -jar gc.jar
 *   java -XX:+UseSerialGC -Xlog:gc* -jar gc.jar
 */

import java.lang.management.ManagementFactory

// ── 1. STW 측정 ───────────────────────────────────────────────────
println("=== 1. STW 측정 ===")

@Volatile var running = true
val gaps = mutableListOf<Long>()

val measureThread = Thread({
    var prev = System.currentTimeMillis()
    while (running) {
        Thread.sleep(1)
        val now = System.currentTimeMillis()
        val gap = now - prev
        if (gap > 5) gaps.add(gap)
        prev = now
    }
}, "stw-watcher").apply { isDaemon = true; start() }

val list = mutableListOf<ByteArray>()
repeat(500) { i ->
    list.add(ByteArray(1024 * 512))
    if (i % 50 == 0) { list.clear(); System.gc() }
}

running = false
measureThread.join(500)
println("[STW] 감지된 횟수: ${gaps.size}")
if (gaps.isNotEmpty()) {
    println("[STW] 최대: ${gaps.max()}ms, 평균: ${"%.1f".format(gaps.average())}ms")
}

// ── 2. GC MXBean ──────────────────────────────────────────────────
println("\n=== 2. GC MXBean 컬렉터 정보 ===")
val gcBeans = ManagementFactory.getGarbageCollectorMXBeans()
gcBeans.forEach { gc ->
    println("  이름: ${gc.name}")
    println("  대상 풀: ${gc.memoryPoolNames.joinToString()}")
    println("  GC 횟수: ${gc.collectionCount}, 총 시간: ${gc.collectionTime}ms\n")
}

val names  = gcBeans.map { it.name }
val gcType = when {
    names.any { "ZGC"       in it } -> "ZGC"
    names.any { "Shenandoah" in it } -> "Shenandoah"
    names.any { "G1"        in it } -> "G1 GC"
    names.any { "Parallel"  in it } -> "Parallel GC"
    names.any { "MarkSweep" in it } -> "Serial/CMS GC"
    else                             -> "알 수 없음"
}
println("[MXBean] 현재 GC: $gcType")

// ── 3. G1 Mixed GC 유발 ───────────────────────────────────────────
println("\n=== 3. G1 Mixed GC 유발 ===")
val MB = 1024 * 1024
val longLived = mutableListOf<ByteArray>()
repeat(100) {
    longLived.add(ByteArray(MB))
    if (it % 10 == 0) { System.gc(); Thread.sleep(10) }
}
println("[G1] Old Gen에 약 ${longLived.size}MB 적재")
repeat(50) { ByteArray(MB * 4) }
println("[G1] Mixed GC 완료 — longLived 살아남음: ${longLived.size}개")

// ── 4. GC 로그 분석 가이드 ────────────────────────────────────────
println("\n=== 4. GC 로그 분석 가이드 ===")
println("""
G1 GC 로그 예시 (-Xlog:gc*):
  [0.523s] GC(3) Pause Young (Normal) 45M->12M(256M) 8.234ms
  [1.234s] GC(8) Pause Young (Mixed)  89M->34M(256M) 12.567ms
  [2.345s] GC(12) Concurrent Mark Cycle

ZGC 로그 예시:
  [0.456s] GC(1) Pause Mark Start   0.456ms  ← STW (매우 짧음)
  [0.467s] GC(1) Concurrent Mark    11.234ms ← 동시 실행
  [0.489s] GC(1) Concurrent Relocate 21.567ms

핵심 지표: YGC/YGCT FGC/FGCT E S0/S1 O M
  jstat -gcutil <pid> 1000
""".trimIndent())

// ── 5. 카드 테이블 & Remember Set ────────────────────────────────
println("=== 5. 카드 테이블 & Remember Set ===")
println("""
카드 테이블 동작:
  Old Gen 객체가 Young Gen 객체를 참조할 때
  Write Barrier가 해당 512B 카드를 dirty로 표시
  Minor GC 시 dirty 카드만 스캔 → GC Roots 열거 비용 절감

G1 Remember Set:
  각 Region이 "나를 참조하는 다른 Region 목록"을 유지
  Mixed GC 시 특정 Region만 수집해도 참조 누락 없음
""".trimIndent())
