#!/usr/bin/env kotlinc -script
/**
 * 9장 — 클래스 로딩 & 실행 서브시스템 사례
 * 실행: kotlinc -script ClassLoadingCaseDemo.kts
 */

import java.lang.management.ManagementFactory

// ── 1. 핫 리로드 원리 ─────────────────────────────────────────────
println("=== 1. 핫 리로드 원리 ===")

open class ServiceV1 { open fun greet(name: String) = "Hello, $name! (v1)" }
class ServiceV2 : ServiceV1() { override fun greet(name: String) = "Hi, $name! (v2 - reloaded)" }

val service: ServiceV1 = ServiceV1()
println("[HotReload] 초기: ${service.greet("World")}")

println("""
[HotReload] 실제 구현:
  1. Instrumentation.redefineClasses(ClassDefinition(clazz, newBytes))
     → 실행 중 메서드 바디를 새 바이트코드로 교체 (구조 변경 불가)
  2. Spring DevTools: src/main/classes를 별도 ClassLoader로 로드
     → 파일 변경 감지 → 해당 ClassLoader 폐기 → 새 ClassLoader로 재로드
  3. log-friends: LogFriendsInstaller가 addTransformer()로 바이트코드 가로채기
""".trimIndent())

// ── 2. 플러그인 격리 ──────────────────────────────────────────────
println("=== 2. 플러그인 격리 ===")

class PluginClassLoader(private val pluginName: String, parent: ClassLoader) : ClassLoader(parent) {
    private val loadedClasses = mutableMapOf<String, Class<*>>()
    override fun loadClass(name: String, resolve: Boolean): Class<*> {
        if (name.startsWith("plugin.$pluginName.")) {
            return loadedClasses.getOrPut(name) {
                println("[Plugin] [$pluginName] 클래스 로드: $name")
                super.loadClass(name, resolve)
            }
        }
        return super.loadClass(name, resolve)
    }
}

val parent  = ClassLoader.getSystemClassLoader()
val pluginA = PluginClassLoader("A", parent)
val pluginB = PluginClassLoader("B", parent)
println("[Plugin] 부모 공유: ${pluginA.parent == pluginB.parent}")
println("[Plugin] 독립 로더: pluginA ≠ pluginB → 같은 라이브러리 다른 버전 공존 가능")

// ── 3. Tomcat 클래스 로더 계층 ────────────────────────────────────
println("\n=== 3. Tomcat 클래스 로더 계층 ===")
println("""
  Bootstrap ClassLoader
    └── System ClassLoader (Tomcat 기동)
          └── Common ClassLoader (Tomcat 공용 라이브러리)
                ├── Catalina ClassLoader (Tomcat 내부)
                └── Shared ClassLoader (웹 앱 공유)
                      └── WebApp ClassLoader ← 핵심!
                            WEB-INF/classes → WEB-INF/lib → 부모 위임

  WebApp ClassLoader 특징:
    - 부모 위임 역전: WEB-INF를 먼저 탐색
    - 서블릿 API (javax.servlet) 는 예외 — 항상 부모에서 로드
    - 각 웹 앱이 독립적인 Spring 버전 사용 가능

  log-friends: java-agent는 Bootstrap ClassLoader 레벨에서 등록해야 함
    → -javaagent 옵션 사용 이유
""".trimIndent())

// ── 4. 바이트코드 조작 원리 ───────────────────────────────────────
println("=== 4. 바이트코드 조작 원리 (ByteBuddy 스타일) ===")

class Calculator { fun add(a: Int, b: Int) = a + b }

class InstrumentedCalculator {
    private val original = Calculator()
    fun add(a: Int, b: Int): Int {
        val start = System.nanoTime()
        return try {
            original.add(a, b).also {
                println("[Instrument] add($a, $b) = $it, ${System.nanoTime() - start}ns")
            }
        } catch (e: Exception) { println("[Instrument] 예외: ${e.message}"); throw e }
    }
}

println("[Bytecode] 원본: ${Calculator().add(3, 4)}")
println("[Bytecode] 계측된 호출:")
InstrumentedCalculator().run { add(3, 4); add(10, 20) }

// ── 5. Instrumentation API 정보 ──────────────────────────────────
println("\n=== 5. Instrumentation API 정보 ===")
val memBean   = ManagementFactory.getMemoryMXBean()
val classBean = ManagementFactory.getClassLoadingMXBean()
println("  로드된 클래스 수: ${classBean.loadedClassCount}")
println("  총 로드 시도:     ${classBean.totalLoadedClassCount}")
println("  힙 사용량:        ${memBean.heapMemoryUsage.used / 1024 / 1024}MB")
println("  Metaspace:        ${memBean.nonHeapMemoryUsage.used / 1024 / 1024}MB")
println("""
Instrumentation 접근 방법:
  방법 1: -javaagent:agent.jar (premain)
  방법 2: 런타임 attach (agentmain) — log-friends 방식
    VirtualMachine.attach(pid).loadAgent("agent.jar")
    → -Djdk.attach.allowAttachSelf=true 필요
""".trimIndent())
