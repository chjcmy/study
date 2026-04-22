#!/usr/bin/env kotlinc -script
/**
 * 7장 — 클래스 로딩 메커니즘
 * 실행: kotlinc -script ClassLoaderDemo.kts
 */

// ── 1. 클래스 로더 계층 ───────────────────────────────────────────
println("=== 1. 클래스 로더 계층 ===")

fun printLoaderChain(label: String, clazz: Class<*>) {
    val cl = clazz.classLoader
    println("\n[$label]")
    println("  로더: ${cl?.javaClass?.name ?: "null (Bootstrap)"}")
    var parent = cl?.parent
    while (parent != null) { println("  부모: ${parent.javaClass.name}"); parent = parent.parent }
    if (cl != null) println("  최상위: null → Bootstrap (native)")
}

printLoaderChain("java.lang.String", String::class.java)
printLoaderChain("java.util.ArrayList", java.util.ArrayList::class.java)

// ── 2. 부모 위임 모델 ─────────────────────────────────────────────
println("\n=== 2. 부모 위임 모델 ===")
println("""
  loadClass(name) 호출 시:
    1. findLoadedClass(name)    → 이미 로드됐으면 반환
    2. parent.loadClass(name)   → 부모에게 위임
    3. findClass(name)          → 부모가 못 찾으면 자신이 로드
    4. ClassNotFoundException   → 전부 실패

  목적:
    - java.lang.String 을 사용자가 재정의하지 못하게 방지
    - 클래스 중복 로딩 방지 (같은 로더가 로드한 클래스만 동일)
""".trimIndent())

// ── 3. 클래스 초기화 순서 ─────────────────────────────────────────
println("=== 3. 클래스 초기화 순서 ===")

class InitOrder {
    companion object {
        val CONST: String
        init {
            println("[InitOrder] companion object init 실행")
            CONST = "initialized"
        }
    }
    val instanceField: String
    init {
        println("[InitOrder] instance init 실행")
        instanceField = "instance"
    }
}

println("InitOrder.CONST 접근 전")
println("CONST = ${InitOrder.CONST}")
println("인스턴스 생성 중...")
val obj = InitOrder()
println("instanceField = ${obj.instanceField}")

// ── 4. 커스텀 ClassLoader ─────────────────────────────────────────
println("\n=== 4. 커스텀 ClassLoader ===")

class LoggingClassLoader(parent: ClassLoader) : ClassLoader(parent) {
    override fun loadClass(name: String, resolve: Boolean): Class<*> {
        if (!name.startsWith("java.") && !name.startsWith("kotlin.") && !name.startsWith("sun.")) {
            println("[CustomLoader] 로드 요청: $name")
        }
        return super.loadClass(name, resolve)
    }
}

val loader = LoggingClassLoader(Thread.currentThread().contextClassLoader)
val clazz  = loader.loadClass("java.util.HashMap")
println("로드된 클래스: ${clazz.name}")
println("클래스 로더: ${clazz.classLoader?.javaClass?.name ?: "Bootstrap"}")

// ── 5. 스레드 컨텍스트 ClassLoader ───────────────────────────────
println("\n=== 5. 스레드 컨텍스트 ClassLoader ===")
println("""
  Thread.currentThread().contextClassLoader
    → 기본값: 애플리케이션 ClassLoader
    → JDBC, JNDI 등 SPI가 구현체를 찾을 때 사용
    → 부모 위임을 역전시키는 패턴 (Bootstrap → App ClassLoader 방향)

  사용 예:
    val cl = Thread.currentThread().contextClassLoader
    val svc = ServiceLoader.load(MyInterface::class.java, cl)
""".trimIndent())
println("현재 스레드 컨텍스트 로더: ${Thread.currentThread().contextClassLoader?.javaClass?.name}")
