#!/usr/bin/env kotlinc -script
/**
 * 11장 부록 — JVM 최적화 철학
 * "포인터를 숨기는 대신 JVM이 최적화를 대신 해준다"
 *
 * 실행: kotlinc -script JvmOptPhilosophy.kts
 */

import java.nio.ByteBuffer
import java.util.concurrent.CountDownLatch

// ── 0. 핵심 트레이드오프 ──────────────────────────────────────────
println("=== JVM 최적화 철학 ===")
println("""
  C/C++   포인터 직접 조작 가능
            int* p = &x;
            p++;          // 주소 직접 이동
            *p = 42;      // 임의 메모리 쓰기
            → 최적화 전적으로 개발자 책임

  JVM     포인터 존재하지만 완전히 숨김
            val a = "hello"  // 내부적으로 주소지만 개발자는 접근 불가
            a === b          // "같은 주소냐" 만 확인 가능 (전부)
            → 대신 JVM이 런타임에 훨씬 더 잘 최적화해줌

  트레이드오프:
    자유도 포기  →  안전성 + JVM 자동 최적화 획득
""".trimIndent())

// ── 1. 참조는 숨겨진 포인터 ──────────────────────────────────────
println("=== 1. 참조 = 숨겨진 포인터 ===")

val a = "hello"              // a는 내부적으로 주소(포인터)를 저장
val b = String("hello".toCharArray())  // 힙에 새 객체 → 다른 주소
val c = a                    // 같은 주소 복사 (얕은 복사)

println("a === b : ${a === b}   // 주소가 다름 (다른 객체)")
println("a === c : ${a === c}   // 주소가 같음 (같은 객체)")
println("a == b  : ${a == b}   // 값은 같음  (equals)")
println("""
  C로 치면:
    char* a = "hello";           // 상수 풀 주소
    char* b = strdup("hello");   // 힙 주소
    a == b       → false  (포인터 비교)  ← Kotlin ===
    strcmp(a, b) → 0      (값 비교)      ← Kotlin ==
""".trimIndent())

// ── 2. 포인터를 숨긴 덕분에 GC가 객체를 자유롭게 이동 ─────────
println("=== 2. GC가 객체를 이동할 수 있는 이유 ===")
println("""
  개발자가 주소를 직접 들고 있다면:
    val addr = addressOf(obj)   // 0x1A2B
    // GC가 obj를 0x3C4D로 이동
    *addr = 42                  // 💥 잘못된 주소 — 버그

  JVM은 참조만 노출하므로:
    val ref = obj               // 내부적으로 0x1A2B
    // GC가 obj를 0x3C4D로 이동 → ref도 0x3C4D로 자동 업데이트
    ref.field = 42              // ✓ 항상 안전

  G1 GC가 Region 간 객체를 이동할 수 있는 것도 이 덕분
  포인터를 숨기지 않았다면 GC는 객체를 절대 이동시킬 수 없음
""".trimIndent())

// ── 3. 탈출 분석 — 개발자가 할 수 없는 최적화 ───────────────────
println("=== 3. 탈출 분석 (개발자 불가능, JVM만 가능) ===")

data class Point(val x: Int, val y: Int)

fun noEscape(): Int {
    val p = Point(3, 4)   // 메서드 밖으로 안 나감
    return p.x + p.y      // JIT: 힙 할당 생략 → 레지스터에서 바로 계산
}

fun escape(): Point {
    return Point(3, 4)    // 밖으로 나감 → 힙 할당 필수
}

val iterations = 10_000_000
val s1 = System.nanoTime()
var sum1 = 0; repeat(iterations) { sum1 += noEscape() }
val ms1 = (System.nanoTime() - s1) / 1_000_000.0

val s2 = System.nanoTime()
var sum2 = 0; repeat(iterations) { sum2 += escape().x + escape().y }
val ms2 = (System.nanoTime() - s2) / 1_000_000.0

println("비탈출 (스택/레지스터): ${"%.2f".format(ms1)}ms")
println("탈출   (힙 할당):       ${"%.2f".format(ms2)}ms")
println("""
  C/C++에서는 개발자가 스택/힙을 직접 선택:
    Point p = {3, 4};       // 스택 (개발자 선택)
    Point* p = new Point(); // 힙  (개발자 선택)

  JVM에서는 개발자가 선택 불가, 대신 JIT가 런타임에 자동 판단:
    val p = Point(3, 4)     // 코드는 동일
    → 탈출 여부에 따라 JIT가 스택/힙 자동 선택
    → 심지어 C 개발자보다 더 정확하게 판단 가능 (런타임 정보 활용)
""".trimIndent())

// ── 4. TLAB — 포인터 없이도 빠른 할당 ───────────────────────────
println("=== 4. TLAB (Thread-Local Allocation Buffer) ===")
println("""
  C의 malloc:
    malloc(size) → 전역 힙 락 → 빈 공간 탐색 → 포인터 반환
    멀티스레드 → 락 경합 발생

  JVM TLAB:
    각 스레드가 Eden 영역 일부를 미리 예약 (자기만의 구역)
    new Object() → 그냥 포인터 bump (덧셈 1번)
    멀티스레드 → 서로 다른 구역 → 락 없음

  결과: new Object()가 malloc보다 빠른 경우도 있음
""".trimIndent())

val threadCount = 4
val objectsPerThread = 1_000_000
val latch = CountDownLatch(threadCount)
val tlabStart = System.nanoTime()

repeat(threadCount) { id ->
    Thread({
        repeat(objectsPerThread) { Any() }  // TLAB로 락 없이 할당
        latch.countDown()
    }, "tlab-$id").start()
}
latch.await()
val tlabMs = (System.nanoTime() - tlabStart) / 1_000_000.0
println("${threadCount}스레드 × ${objectsPerThread}개 할당: ${"%.2f".format(tlabMs)}ms (락 없음)")

// ── 5. JIT C2 — 런타임 정보로 C 수준 최적화 ─────────────────────
println("\n=== 5. JIT C2 — 런타임 정보 활용 최적화 ===")
println("""
  C 컴파일러 (정적):
    컴파일 시점 정보만 사용
    "이 가상 함수가 실제로 어떤 구현체로 올지 모름"
    → 보수적 최적화

  JIT C2 (동적):
    런타임에 "항상 Dog 타입만 온다" 는 걸 프로파일로 확인
    → 가상 함수 호출을 직접 호출로 교체 (인라이닝)
    → 나중에 Cat이 오면 역최적화 후 재컴파일

  즉, 포인터 제어권을 포기한 대신
  런타임 프로파일 정보를 JVM이 활용해
  정적 컴파일러가 할 수 없는 최적화를 수행
""".trimIndent())

// ── 6. 그나마 개발자가 만질 수 있는 것 ──────────────────────────
println("=== 6. 개발자에게 허용된 저수준 제어 ===")
println("""
  ===                    주소 비교 (같은 객체냐)
  intern()               문자열 상수 풀에 직접 올리기
  System.gc()            GC 요청 (강제는 아님)
  WeakReference          GC 수거 시점에 개입
  ByteBuffer.allocateDirect()  GC 밖 네이티브 메모리 직접 사용
  sun.misc.Unsafe        (비공개 API) 실제 포인터 수준 접근 가능
                         → HikariCP, Netty 등 고성능 라이브러리만 사용

  결론:
    일반 개발자 → 포인터 신경 쓸 필요 없음, JVM이 더 잘함
    라이브러리 개발자 → Unsafe로 포인터 수준 접근 가능 (책임 직접)
""".trimIndent())

val direct = ByteBuffer.allocateDirect(1024)  // GC 밖 네이티브 메모리
direct.putInt(42)
direct.flip()
println("allocateDirect — GC 밖 네이티브 메모리: ${direct.getInt()}읽기 성공")
println("(NIO, Netty, log-friends BatchTransporter가 이 방식 활용)")
