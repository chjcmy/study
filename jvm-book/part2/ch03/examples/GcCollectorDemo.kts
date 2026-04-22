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

// ── 6. CMS 4단계 & 단점 실증 ─────────────────────────────────────
println("\n=== 6. CMS 4단계 & 단점 ===")
println("""
[CMS] Concurrent Mark Sweep — 4단계:
  1. 초기 마킹 (Initial Mark) — STW, 매우 짧음
     GC Root에서 직접 참조하는 객체만 마킹
     (깊이 1만 탐색 → 빠름)

  2. 동시 마킹 (Concurrent Mark) — 애플리케이션과 동시 실행
     GC Root부터 전체 객체 그래프를 탐색
     동시 실행이므로 CPU 점유 (처리량 감소)
     참조 변경을 증분 갱신(Incremental Update)으로 추적

  3. 재마킹 (Remark) — STW, 초기 마킹보다 약간 김
     동시 마킹 중 변경된 참조를 재처리
     SATB 버퍼 또는 카드 테이블의 dirty 카드 재스캔

  4. 동시 스윕 (Concurrent Sweep) — 애플리케이션과 동시 실행
     쓰레기 객체 회수 (객체 이동 없음 → 단편화 발생!)

[CMS] 3가지 단점:
  ① CPU 민감: 동시 단계에서 GC 스레드가 CPU 코어 점유 (기본 1/4)
  ② 부유 쓰레기(Floating Garbage): 동시 스윕 중 새로 죽은 객체 → 다음 GC에서 회수
  ③ 메모리 단편화: 마크-스윕 방식 → 큰 객체 할당 실패 → Concurrent Mode Failure
     → Serial Old로 풀백 (Full STW!)
     → -XX:CMSInitiatingOccupancyFraction 으로 미리 GC 시작

JDK 9 deprecated → JDK 14 제거
""".trimIndent())

// CMS 관련: Old Gen 점유율 모니터링 (CMSInitiatingOccupancyFraction 기본 92%)
val memMXBean = ManagementFactory.getMemoryMXBean()
val heapUsage = memMXBean.heapMemoryUsage
val occupancy = if (heapUsage.max > 0) heapUsage.used * 100 / heapUsage.max else 0L
println("[CMS] 현재 힙 점유율: $occupancy% (CMS 기본 시작: 92%)")
println("[CMS] 92% 미만이면 동시 GC가 완료되기 전에 힙이 가득 찰 위험 없음")

// ── 7. ZGC 컬러드 포인터 & 로드 배리어 ───────────────────────────
println("\n=== 7. ZGC 컬러드 포인터 & 로드 배리어 ===")
println("""
[ZGC] 컬러드 포인터 (Colored Pointers):
  64-bit 참조의 상위 비트에 GC 메타데이터를 직접 인코딩

  ┌──16bit(unused)──┬─M0─┬─M1─┬─Remap─┬─Fin─┬───44bit(주소)───┐
  │                 │    │    │       │     │   (최대 16TB)   │
  └─────────────────┴────┴────┴───────┴─────┴────────────────┘
  M0/M1: 마킹 비트 (삼색 표시, GC 사이클마다 교대)
  Remap: 재배치 완료 표시
  Fin:   finalize 대상

  장점: 포인터 하나에 GC 상태가 포함 → 별도 자료구조 불필요
  단점: 64-bit 전용 (32-bit 불가), 16TB 힙 제한

[ZGC] 로드 배리어 (Load Barrier):
  참조를 "읽을 때" 배리어 실행 (대부분의 GC는 쓰기 장벽 사용)
  포인터 컬러가 잘못된 경우 → slow path로 포인터를 자가 치유

  if (ptr.color != good_color) {
      ptr = slow_path(ptr);  // 재배치된 새 주소로 갱신
      *field = ptr;           // 필드에 새 주소 기록 → 다음엔 배리어 불필요 (Self-Healing)
  }

[ZGC] STW 3회, 각각 ~1ms (힙 크기와 무관):
  ① Pause Mark Start  — GC Roots만 마킹
  ② Pause Mark End    — SATB 버퍼 처리
  ③ Pause Relocate Start — GC Roots가 가리키는 객체 이동

[ZGC] Generational ZGC (JDK 21+):
  기존 ZGC는 세대 구분 없음 → 단명 객체에도 전체 힙 탐색 비용
  Generational ZGC: Young/Old 독립 수집 → 단명 객체 더 빠르게 수거
  log-friends AgentEvent(단명) + Protobuf 객체 → Generational ZGC 최적화

[컬렉터 선택 가이드]
  힙 < 200MB   → Serial GC   (단순, 오버헤드 최소)
  배치/처리량  → Parallel GC  (JDK 8 기본)
  힙 1~8GB     → G1 GC       (JDK 9+ 기본, 예측 가능 STW)
  초저지연     → ZGC          (JDK 21 기본, STW ~1ms)
  Red Hat JDK  → Shenandoah  (브룩스 포인터, STW <10ms)
""".trimIndent())
