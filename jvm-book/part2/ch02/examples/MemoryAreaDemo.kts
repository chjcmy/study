#!/usr/bin/env kotlinc -script
/**
 * 2장 — 런타임 데이터 영역 & 메모리 오버플로
 * 실행: kotlinc -script MemoryAreaDemo.kts
 */

// ── 1. StackOverflowError ─────────────────────────────────────────
var depth = 0
fun recurse() { depth++; recurse() }

println("=== 1. StackOverflow ===")
try { recurse() }
catch (e: StackOverflowError) { println("[Stack] StackOverflowError at depth=$depth") }

// ── 2. String Intern (상수 풀) ────────────────────────────────────
println("\n=== 2. String Intern (상수 풀) ===")
val a = "hello"
val b = String("hello".toCharArray())
val c = b.intern()

println("[StringPool] a == b   : ${a === b}")   // false
println("[StringPool] a == c   : ${a === c}")   // true
println("[StringPool] b == c   : ${b === c}")   // false

// ── 3. 객체 헤더 크기 ────────────────────────────────────────────
println("\n=== 3. 객체 헤더 크기 ===")
val runtime = Runtime.getRuntime()
System.gc()
val before  = runtime.totalMemory() - runtime.freeMemory()
val objects = Array(100_000) { Any() }
System.gc()
val after     = runtime.totalMemory() - runtime.freeMemory()
val perObject = (after - before) / objects.size

println("[Header] 빈 객체 1개당 약 ${perObject}B (헤더 포함)")
println("[Header] 이론값: Mark Word 8B + 클래스 포인터 4B(압축) = 최소 12B, 패딩 후 16B")

// ── 4. 스택 vs 힙 — 변수 저장 위치 ──────────────────────────────
println("\n=== 4. 스택 vs 힙 — 변수 저장 위치 ===")

fun stackVsHeap() {
    val primitive = 42          // 원시 타입 → 스택에 값 직접 저장
    val text = "hello"          // 객체 타입 → 스택에 힙 주소(참조), 실제 값은 힙
    val arr = IntArray(3) { it } // 배열 → 힙에 저장, arr 변수는 스택에 주소만

    println("[Stack] primitive=$primitive  (스택에 값 직접)")
    println("[Heap]  text 참조: ${System.identityHashCode(text).toString(16)}  (스택은 이 주소만 보관)")
    println("[Heap]  arr  참조: ${System.identityHashCode(arr).toString(16)}  (배열 본체는 힙)")
}
stackVsHeap()

println("""
[요약]
  원시 타입 지역 변수 (Int, Long, Double ...)
    → 스택 프레임에 값 직접 저장
    → 메서드 종료 시 자동 제거 (GC 불필요)

  객체 타입 지역 변수 (String, Array, 사용자 클래스 ...)
    → 스택 프레임에 힙 주소(참조)만 저장
    → 실제 데이터는 힙에 존재
    → GC가 힙을 정리할 때 수거

  클래스/파일 수준 변수 (var depth = 0 처럼 메서드 밖)
    → 내부적으로 클래스 필드로 컴파일 → 힙에 저장
""".trimIndent())

// ── 5. OOM 줄이기 — 3가지 패턴 ──────────────────────────────────
println("\n=== 5. OOM 줄이기 ===")

// ① 스트리밍: 전부 모으지 말고 하나씩 처리 후 버리기
println("[Stream] 스트리밍 처리 — 10만 건을 메모리에 안 쌓고 합산")
val streamSum = (1..100_000).asSequence()
    .map { it * 2 }
    .filter { it % 3 == 0 }
    .sum()
println("[Stream] 결과=$streamSum (List로 중간 수집 없음)")

// ② 크기 제한 큐: 오래된 것 자동 제거
println("\n[BoundedQueue] 크기 제한 — 최대 5개 유지")
val bounded = ArrayDeque<String>()
val maxSize = 5
repeat(10) { i ->
    if (bounded.size >= maxSize) bounded.removeFirst()
    bounded.addLast("item-$i")
}
println("[BoundedQueue] 10개 추가 후 보관 중인 항목: $bounded")

// ③ SoftReference: 메모리 부족 시 GC가 자동 수거
println("\n[SoftRef] SoftReference — 메모리 부족 시 자동 해제")
val softRefs = (1..5).map { java.lang.ref.SoftReference(ByteArray(1024) { it.toByte() }) }
System.gc()
val alive = softRefs.count { it.get() != null }
println("[SoftRef] GC 후 살아있는 참조: $alive / ${softRefs.size} (메모리 여유 있으면 유지)")

println("""
[요약] OOM 줄이는 핵심 원칙
  ① 스트리밍     전부 메모리에 올리지 말고 Sequence/Stream으로 하나씩 처리
  ② 크기 제한    컬렉션에 상한선 설정, 넘으면 오래된 것 제거
  ③ SoftReference  캐시 용도 — 메모리 부족하면 GC가 자동 수거
  ④ WeakReference  키 참조 없으면 자동 제거 (WeakHashMap)
""".trimIndent())

// ── 6. 런타임 데이터 영역 5개 — 역할과 스레드 공유 ─────────────────
println("\n=== 6. 런타임 데이터 영역 5개 ===")

// PC Register: 각 스레드가 독립된 PC를 가짐
val pcThreads = (1..3).map { id ->
    Thread({
        println("[PC] 스레드-$id 독립 실행 중 (각자 다른 바이트코드 위치 추적)")
    }, "pc-thread-$id")
}
pcThreads.forEach { it.start(); it.join() }

// JVM 스택: 메서드 호출마다 프레임 생성
fun level3() = Thread.currentThread().stackTrace.size
fun level2() = level3()
fun level1() = level2()
println("[JVMStack] 메서드 3단계 호출 시 스택 프레임 깊이: ${level1()}")

// 힙: 모든 스레드가 공유
val sharedObj = StringBuilder("공유 객체")
val heapThreads = (1..3).map { id ->
    Thread({ sharedObj.append(" +T$id") }, "heap-thread-$id")
}
heapThreads.forEach { it.start() }
heapThreads.forEach { it.join() }
println("[Heap] 3개 스레드가 같은 객체에 접근: $sharedObj")

// 메서드 영역: 클래스 메타데이터 (로드된 클래스 수로 확인)
val classLoader = ClassLoader.getSystemClassLoader()
println("[MethodArea] 현재 JVM에 로드된 패키지 수: ${Package.getPackages().size}")

println("""
[런타임 영역 요약]
  PC Register      비공유  현재 실행 바이트코드 주소, 스레드 전환 복귀점
  JVM 스택         비공유  메서드 호출마다 프레임 생성 (지역변수/반환주소)
  네이티브 스택    비공유  JNI C/C++ 메서드 (HotSpot은 JVM 스택과 통합)
  힙               공유    모든 객체·배열, GC 대상
  메서드 영역      공유    클래스 메타데이터, 상수 풀, 정적 변수, JIT 코드
""".trimIndent())

// ── 7. 다이렉트 메모리 — 힙 외부 할당 ───────────────────────────
println("\n=== 7. 다이렉트 메모리 ===")

import java.nio.ByteBuffer

val rt = Runtime.getRuntime()
System.gc()
val heapBefore = rt.totalMemory() - rt.freeMemory()
val direct = ByteBuffer.allocateDirect(10 * 1024 * 1024)  // 10MB 네이티브 메모리
val heapAfter = rt.totalMemory() - rt.freeMemory()

direct.putInt(42); direct.flip()
println("[Direct] 힙 변화: ${(heapAfter - heapBefore) / 1024}KB (10MB 할당했지만 힙 거의 안 증가)")
println("[Direct] 읽기: ${direct.getInt()} (네이티브 메모리에서 직접)")
println("""
[Direct 요약]
  힙 외부(네이티브 메모리)에 할당 → GC 대상 아님
  GC STW 없이 대용량 I/O 가능 (NIO, Netty, log-friends BatchTransporter)
  단점: GC가 자동 수거 안 함 → 명시적 해제 또는 Cleaner 필요
  제한: -XX:MaxDirectMemorySize (기본 = -Xmx 값)
""".trimIndent())

// ── 8. TLAB — 스레드별 전용 할당 영역 ───────────────────────────
println("\n=== 8. TLAB (Thread-Local Allocation Buffer) ===")

import java.util.concurrent.CountDownLatch

val threadCount = 4
val allocPerThread = 500_000
val latch = CountDownLatch(threadCount)

val tlabStart = System.nanoTime()
repeat(threadCount) { id ->
    Thread({
        repeat(allocPerThread) { Any() }  // 각 스레드 전용 TLAB에서 락 없이 할당
        latch.countDown()
    }, "tlab-$id").start()
}
latch.await()
val tlabMs = (System.nanoTime() - tlabStart) / 1_000_000.0

println("[TLAB] ${threadCount}스레드 × ${allocPerThread}개 할당: ${"%.2f".format(tlabMs)}ms")
println("""
[TLAB 요약]
  문제: 힙은 공유 → 여러 스레드가 동시에 할당하면 락 경합 발생
  해결: 각 스레드가 Eden 영역 일부를 미리 예약 (전용 구역)
  결과: new Object() = 포인터 bump 1번 → 락 없음, malloc보다 빠름
  TLAB 가득 차면: 새 TLAB 발급 또는 힙에 직접 할당 (느림)
""".trimIndent())

// ── 9. OOM 4가지 유형 진단 ───────────────────────────────────────
println("\n=== 9. OOM 4가지 유형 ===")
println("""
① 힙 OOM       "Java heap space"
   원인: 메모리 누수, GC Root에서 계속 도달 가능한 객체 누적
   진단: -XX:+HeapDumpOnOutOfMemoryError → MAT/VisualVM 분석
   대응: 누수 코드 수정 or -Xmx 증가

② 스택 OOM     "StackOverflowError" / "unable to create native thread"
   원인: 무한 재귀 or 스레드 과다 생성 (스레드마다 스택 메모리 소비)
   진단: 스택 트레이스 분석 / jstack
   대응: 재귀 → 반복 변환, 스레드 풀 사용, -Xss 조정

③ Metaspace OOM  "Metaspace"
   원인: 동적 프록시/CGLIB로 클래스를 끊임없이 생성
   진단: -XX:+TraceClassLoading
   대응: -XX:MaxMetaspaceSize 조정, 클래스 캐시 재사용

④ 다이렉트 메모리 OOM  "Direct buffer memory"
   원인: ByteBuffer.allocateDirect 과다 할당 후 해제 안 함
   진단: -XX:NativeMemoryTracking=summary
   대응: -XX:MaxDirectMemorySize 조정, Cleaner 명시 호출
""".trimIndent())

// 힙 OOM 유발 (주석 해제 후 실행 — JVM 종료됨)
// val list = mutableListOf<ByteArray>()
// try { while (true) list.add(ByteArray(1024 * 1024)) }
// catch (e: OutOfMemoryError) { println("[OOM] 힙 OOM: ${e.message}") }

// ── 10. 객체 생성 5단계 ──────────────────────────────────────────
println("\n=== 10. 객체 생성 5단계 ===")

// ── 1단계: 클래스 로딩 체크 ──────────────────────────────────────
println("\n[1단계] 클래스 로딩 체크")

// 커스텀 ClassLoader로 로딩 시점 관찰
class TracingLoader(parent: ClassLoader) : ClassLoader(parent) {
    val loaded = mutableListOf<String>()
    override fun loadClass(name: String, resolve: Boolean): Class<*> {
        if (!name.startsWith("java") && !name.startsWith("kotlin") && !name.startsWith("org")) {
            loaded += name
            println("[ClassLoad] 로딩 요청: $name")
        }
        return super.loadClass(name, resolve)
    }
}

val tracer = TracingLoader(ClassLoader.getSystemClassLoader())
// 처음 참조 시점에 로딩 — Class.forName으로 명시적 트리거
try {
    val clazz = Class.forName("java.util.LinkedList", true, tracer)
    println("[ClassLoad] LinkedList 로드 완료: ${clazz.name}")
} catch (_: Exception) {}

println("""
[1단계 요약]
  new 명령 만나면 → Constant Pool에서 클래스 심볼릭 레퍼런스 확인
  Method Area에 클래스 정보 없으면 → ClassLoader 체인으로 .class 로드
  이미 로드됐으면 → 이 단계 스킵 (두 번째 new는 1단계 없음)
""".trimIndent())

// ── 2단계: 메모리 할당 ───────────────────────────────────────────
println("\n[2단계] 메모리 할당 — Bump-the-pointer vs Free-list")

// Bump-the-pointer 속도 (TLAB + 연속 Eden 공간)
val bumpCount = 1_000_000
val bumpStart = System.nanoTime()
repeat(bumpCount) { Any() }
val bumpNs = System.nanoTime() - bumpStart

// 큰 객체 — Old Gen 직접 할당 (bump 불가, 느림)
val largeCount = 1_000
val largeStart = System.nanoTime()
repeat(largeCount) { ByteArray(512 * 1024) }  // 512KB × 1000 = 500MB
val largeNs = System.nanoTime() - largeStart

println("[Alloc] 소형 객체 ${bumpCount}개 TLAB: ${bumpNs / bumpCount}ns/개 (포인터 이동 1번)")
println("[Alloc] 대형 객체 ${largeCount}개 Old Gen: ${largeNs / largeCount}ns/개 (Free-list 탐색)")
println("""
[2단계 요약]
  Bump-the-pointer  Eden 여유 공간 포인터를 크기만큼 앞으로 이동 (락 없음, ns 단위)
  Free-list         단편화 영역에서 알맞은 빈 공간 탐색 (CMS GC 시 사용)
  TLAB              각 스레드가 Eden 일부를 미리 예약 → 스레드 간 충돌 없음
  대형 객체(>임계값) → Eden 건너뛰고 Old Gen에 직접 할당
""".trimIndent())

// ── 3단계: 메모리 0 초기화 ───────────────────────────────────────
println("\n[3단계] 메모리 0 초기화 — 생성자 실행 전 기본값 보장")

class ZeroInitDemo {
    var intField: Int = 42          // 명시 초기화
    var nullableField: String? = null
    var boolField: Boolean = false

    init {
        // 이 블록 실행 전에 이미 intField=42, nullableField=null, boolField=false 확정
        println("[ZeroInit] init 블록 진입 시점 — intField=$intField, nullableField=$nullableField")
    }
}

val zd = ZeroInitDemo()
println("[ZeroInit] 생성 후 — intField=${zd.intField}, boolField=${zd.boolField}")
println("""
[3단계 요약]
  할당된 메모리 전체를 0으로 덮어씀 (JVM 보장)
  덕분에 int=0, boolean=false, Object=null 기본값 보장
  명시 초기화(intField=42)는 4단계 <init>에서 덮어씀
  이 단계가 있기에 Java/Kotlin은 미초기화 필드 접근이 안전
""".trimIndent())

// ── 4단계: 객체 헤더 설정 ────────────────────────────────────────
println("\n[4단계] 객체 헤더 설정 — Mark Word + 클래스 포인터")

val obj1 = Any()
val obj2 = Any()

// identityHashCode 호출 → Mark Word에 해시값 기록 트리거
val hash1 = System.identityHashCode(obj1)
val hash2 = System.identityHashCode(obj2)

println("[Header] obj1 identity hash: 0x${hash1.toString(16).padStart(8, '0')}")
println("[Header] obj2 identity hash: 0x${hash2.toString(16).padStart(8, '0')} (다른 객체 → 다른 값)")
println("[Header] 클래스 포인터 확인 — obj1 클래스: ${obj1.javaClass.name}")

// synchronized 블록 → Mark Word의 락 상태 변화 관찰
val lockTarget = Any()
print("[Header] synchronized 진입 전 hash: 0x${System.identityHashCode(lockTarget).toString(16)}")
synchronized(lockTarget) {
    // Mark Word: Biased Lock(편향 락) → Lightweight Lock(박형 락) 상태로 전환
    print("  →  Lock 보유 중")
}
println("  →  Lock 해제 완료")

println("""
[4단계 요약]  객체 헤더 구조 (64비트 JVM, 압축 포인터 기준)
  Mark Word  (8B)  ┌ 해시코드 31비트
                   ├ GC 나이   4비트  (Young GC 생존 횟수, 15 넘으면 Old 이동)
                   ├ 락 상태   2비트  (01=무락, 00=경량락, 10=중량락, 11=GC 마킹)
                   └ 편향 스레드 ID (편향 락 활성화 시)
  클래스 포인터 (4B)  Method Area의 Klass 포인터 (압축 시 4B, 비압축 8B)
  총 12B → 8B 패딩 정렬 → 실제 최소 객체 크기 16B
""".trimIndent())

// ── 5단계: <init> 실행 ───────────────────────────────────────────
println("\n[5단계] <init> 실행 — 생성자 실행 순서")

open class Base(val name: String) {
    val baseField = "Base-init".also { println("[<init>] Base 필드 초기화: $it") }
    init { println("[<init>] Base init 블록: name=$name") }
}

class Child(name: String, val age: Int) : Base(name) {
    val childField = "Child-init".also { println("[<init>] Child 필드 초기화: $it") }
    init { println("[<init>] Child init 블록: age=$age") }
    constructor(name: String) : this(name, 0) {
        println("[<init>] 보조 생성자 실행")
    }
}

println("[<init>] --- 주 생성자 new Child(\"Alice\", 30) ---")
val c1 = Child("Alice", 30)
println("[<init>] --- 보조 생성자 new Child(\"Bob\") ---")
val c2 = Child("Bob")

println("""
[5단계 요약]  <init> 실행 순서 (바이트코드 기준)
  ① 부모 클래스 <init> 먼저 (super() 호출)
  ② 현재 클래스 필드 초기화 (선언 순서대로)
  ③ init { } 블록 실행 (선언 순서대로)
  ④ 생성자 본문 실행

[전체 5단계 요약]
  1. 클래스 로딩  Method Area에 클래스 메타데이터 없으면 ClassLoader 가동
  2. 메모리 할당  Eden(TLAB Bump-the-pointer) 또는 Old Gen(Free-list)
  3. 0 초기화     모든 필드를 타입 기본값으로 (null, 0, false)
  4. 헤더 설정    Mark Word(해시·GC나이·락) + 클래스 포인터 기록
  5. <init>       부모 → 필드 → init블록 → 생성자 본문 순서로 초기화
""".trimIndent())
