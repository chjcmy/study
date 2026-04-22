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
