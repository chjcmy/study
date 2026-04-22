#!/usr/bin/env kotlinc -script
/**
 * 3장 — GC & 메모리 할당 전략
 * 실행: kotlinc -script GcDemo.kts
 *
 * GC 로그 보기 (컴파일 후):
 *   kotlinc GcDemo.kts -include-runtime -d gc.jar
 *   java -Xms20m -Xmx20m -Xlog:gc* -jar gc.jar
 */

import java.lang.ref.PhantomReference
import java.lang.ref.ReferenceQueue
import java.lang.ref.SoftReference
import java.lang.ref.WeakReference

// ── 1. 참조 타입 4종 ──────────────────────────────────────────────
println("=== 1. 참조 타입 4종 ===")

var strong: Any? = Object()
val soft    = SoftReference(Object())
val weak    = WeakReference(Object())
val queue   = ReferenceQueue<Any>()
val phantom = PhantomReference(Object(), queue)

println("[Ref] Before GC:")
println("  strong != null   : ${strong != null}")
println("  soft.get != null : ${soft.get() != null}")
println("  weak.get != null : ${weak.get() != null}")

strong = null
System.gc()
Thread.sleep(100)

println("[Ref] After GC:")
println("  soft.get != null : ${soft.get() != null}")
println("  weak.get != null : ${weak.get() != null}")
println("  phantom.get()    : ${phantom.get()}")   // 항상 null

// ── 2. Minor/Major GC 유발 ────────────────────────────────────────
println("\n=== 2. Minor/Major GC 유발 ===")
val MB = 1024 * 1024
println("[GC] Eden에 4MB 4번 할당 → Minor GC 유발")
val a1 = ByteArray(4 * MB); val a2 = ByteArray(4 * MB)
val a3 = ByteArray(4 * MB); val a4 = ByteArray(4 * MB)
println("[GC] 큰 객체(8MB) 직접 Old Gen 할당")
val big = ByteArray(8 * MB)
println("[GC] 할당 완료 a1~a4 각 ${a1.size / MB}MB, big ${big.size / MB}MB")

// ── 3. finalize() 와 객체 부활 ───────────────────────────────────
println("\n=== 3. finalize() 와 객체 부활 ===")

var survivor: Any? = null

class Mortal {
    @Suppress("DEPRECATION")
    override fun finalize() {
        println("[Finalize] finalize() 호출됨 — 객체 부활 시도")
        survivor = this
    }
}

var obj: Mortal? = Mortal()
obj = null
System.gc()
Thread.sleep(500)

if (survivor != null) {
    println("[Finalize] 객체 부활 성공")
    survivor = null
    System.gc()
    Thread.sleep(500)
    println("[Finalize] 두 번째 GC — finalize()는 다시 호출 안 됨")
    println("[Finalize] survivor = $survivor")
}

// ── 4. Old Gen 승격 ───────────────────────────────────────────────
println("\n=== 4. Old Gen 승격 ===")
val allocation1 = ByteArray(MB / 4 * 3)
System.gc()
val allocation2 = ByteArray(MB / 4 * 3)
System.gc()
println("[Tenuring] GC 2회 후 allocation1 유지 중: ${allocation1.size}B")

// ── 5. GC Roots 실증 ──────────────────────────────────────────────
println("\n=== 5. GC Roots 실증 ===")

// GC Root 종류 1: JVM 스택의 지역 변수
fun demonstrateStackRoot() {
    val localRef = ByteArray(1024)   // 스택에 있는 지역 변수 → GC Root
    System.gc()
    println("[GCRoot] 스택 지역 변수: ${localRef.size}B → GC 중에도 생존 (GC Root이므로)")
    // 함수 종료 → localRef 스택 프레임 소멸 → 더 이상 GC Root 아님 → 수거 가능
}
demonstrateStackRoot()
System.gc()
println("[GCRoot] demonstrateStackRoot() 종료 → localRef 참조 해제 → GC 대상")

// GC Root 종류 2: 정적 필드
object StaticHolder {
    @JvmStatic var staticRef: ByteArray? = ByteArray(2048)
}
System.gc()
println("[GCRoot] 정적 필드: StaticHolder.staticRef ${StaticHolder.staticRef?.size}B → static = GC Root")
StaticHolder.staticRef = null    // 정적 필드 null 대입 → GC Root 해제 → 수거 가능
System.gc()
Thread.sleep(100)
println("[GCRoot] staticRef = null 후 GC → 2KB 배열 수거됨")

// GC Root 종류 3: synchronized 모니터
val lockTarget = Any()
synchronized(lockTarget) {
    println("[GCRoot] synchronized 잠금 중인 객체 → 잠금 해제 전까지 GC Root 유지")
}
println("[GCRoot] synchronized 블록 종료 → 모니터 해제 → GC Root 아님")

println("""
[GCRoot] 7가지 GC Root 종류:
  ① JVM 스택의 지역 변수 (현재 실행 중인 메서드)
  ② 메서드 영역의 정적 필드 (static 참조)
  ③ 메서드 영역의 상수 (상수 풀 참조)
  ④ JNI 참조 (네이티브 메서드의 글로벌/로컬 참조)
  ⑤ synchronized 잠금 객체
  ⑥ JVM 내부 참조 (Class, ClassLoader, Thread, 기본 예외 클래스)
  ⑦ JVMTI 에이전트 등록 참조

  핵심: GC는 이 Root들에서 출발하여 참조를 따라 탐색
       도달 불가능한 객체 = 수거 대상
""".trimIndent())

// ── 6. 삼색 마킹 (Tri-Color Marking) 시뮬레이션 ──────────────────
println("=== 6. 삼색 마킹 시뮬레이션 ===")

println("""
[TriColor] 삼색 표시법:
  ■ 흰색: 아직 방문 안 됨 (GC 후에도 흰색 = 회수 대상)
  ■ 회색: 방문했지만 자식을 아직 다 처리 안 함
  ■ 검정: 방문 완료, 모든 자식 처리 완료

[TriColor] 탐색 과정:
  초기: GC Root는 회색, 나머지 모두 흰색
  처리: 회색 객체를 꺼내 → 자식을 회색으로 → 자신은 검정으로
  종료: 회색 없음 → 흰색 = 수거 대상, 검정 = 생존

[TriColor] 동시 마킹 중 "객체 소실" 위험:
  조건 1: 검정 객체가 흰색 객체의 참조를 추가 (A → C)
  조건 2: 회색 객체가 그 흰색 객체를 버림 (B X→ C)
  → C는 흰색인데 검정 A에서만 도달 → 이미 검정은 재방문 안 함 → C 잘못 수거!

  해결:
    증분 갱신 (CMS): 검정→흰색 참조 추가 시 검정을 회색으로 되돌림
    SATB (G1):       회색→흰색 참조 삭제 시 삭제 전 참조를 버퍼에 기록
""".trimIndent())

// 도달 가능성 분석 코드 시뮬레이션
class GraphNode(val id: String, val children: MutableList<GraphNode> = mutableListOf())

val root  = GraphNode("Root")
val nodeA = GraphNode("A")
val nodeB = GraphNode("B")
val nodeC = GraphNode("C")
val nodeD = GraphNode("D (고아)")
root.children.add(nodeA)
root.children.add(nodeB)
nodeA.children.add(nodeC)

// 도달 가능성 탐색 (BFS 방식 — GC와 동일한 원리)
val visited = mutableSetOf<GraphNode>()
val queue2  = ArrayDeque<GraphNode>()
queue2.addLast(root)
while (queue2.isNotEmpty()) {
    val node = queue2.removeFirst()
    if (visited.add(node)) {
        node.children.forEach { queue2.addLast(it) }
    }
}
val unreachable = listOf(root, nodeA, nodeB, nodeC, nodeD).filter { it !in visited }
println("[TriColor] 도달 가능: ${visited.map { it.id }}")
println("[TriColor] 도달 불가 (수거 대상): ${unreachable.map { it.id }}")
println("[TriColor] nodeD는 참조 체인에서 누락 → GC가 수거")

// ── 7. 세대별 할당 & 동적 나이 판정 ─────────────────────────────
println("\n=== 7. 세대별 할당 & 동적 나이 판정 ===")

println("""
[Generational] 세대 단위 컬렉션 3가지 가설:
  ① 약한 세대 가설: 대부분의 객체는 일찍 죽는다 (Infant Mortality)
  ② 강한 세대 가설: 오래 살아남은 객체는 앞으로도 오래 산다
  ③ 세대 간 참조 가설: Old → Young 참조는 전체 참조의 극소수

[Generational] 할당 전략:
  ① 객체는 먼저 Eden에 할당
  ② 큰 객체(-XX:PretenureSizeThreshold)는 바로 Old Gen
  ③ Survivor에서 나이 임계값(-XX:MaxTenuringThreshold=15) 초과 시 Old 승격
  ④ 동적 나이 판정: 같은 나이 객체 합이 Survivor 50% 초과 → 그 나이 이상 즉시 Old 승격
""".trimIndent())

// 메모리 풀 정보로 실제 세대 구조 확인
import java.lang.management.ManagementFactory
val pools = ManagementFactory.getMemoryPoolMXBeans()
println("[Generational] 현재 JVM 메모리 풀:")
pools.forEach { pool ->
    val usage = pool.usage
    val used  = usage.used / 1024
    val max   = if (usage.max > 0) "${usage.max / 1024}KB" else "무제한"
    println("  ${pool.name.padEnd(20)} 사용: ${used}KB / 최대: $max")
}
