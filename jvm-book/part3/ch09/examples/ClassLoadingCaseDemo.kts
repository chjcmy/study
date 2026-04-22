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

// ── 6. JDK Proxy vs CGLIB vs ByteBuddy 비교 ──────────────────────
println("\n=== 6. JDK Proxy vs CGLIB vs ByteBuddy ===")

// JDK 동적 프록시: 인터페이스만 가능
import java.lang.reflect.Proxy

interface Greeter { fun greet(name: String): String }

val jdkProxy = Proxy.newProxyInstance(
    Greeter::class.java.classLoader,
    arrayOf(Greeter::class.java)
) { _, method, args -> "JDK Proxy: Hello, ${args?.get(0)}" } as Greeter

println("[JDKProxy] ${jdkProxy.greet("World")}")
println("[JDKProxy] 프록시 클래스명: ${jdkProxy.javaClass.name}")   // $Proxy0 형태
println("[JDKProxy] 인터페이스 구현 여부: ${jdkProxy is Greeter}")

println("""
[Proxy 비교]
  JDK Proxy:
    - Proxy.newProxyInstance() — 인터페이스만 가능
    - 런타임에 $Proxy0, $Proxy1 ... 클래스 생성
    - InvocationHandler.invoke()로 모든 호출 위임
    - Spring AOP가 인터페이스 있을 때 기본 사용

  CGLIB (ASM 기반):
    - Enhancer.create() — 클래스도 프록시 가능 (서브클래싱)
    - final 클래스/메서드는 불가 (상속 기반이므로)
    - Spring AOP가 인터페이스 없을 때 기본 사용
    - MethodInterceptor.intercept()로 호출 위임

  ByteBuddy Agent (log-friends 방식):
    - Instrumentation.retransformClasses() — 기존 클래스 자체를 수정
    - 새 클래스 생성 없음 → 코드 수정 없이 투명 계측 가능
    - final 클래스/메서드도 계측 가능
    - JDK Proxy/CGLIB: 새 클래스 → 호출자가 프록시 참조 필요
    - ByteBuddy Agent: 원본 클래스 수정 → 기존 모든 참조에 적용

  log-friends가 ByteBuddy Agent를 선택한 이유:
    "코드 수정 없이" = 기존 Spring 코드의 참조 변경 없이 계측
    → JDK Proxy/CGLIB는 참조를 바꿔야 하므로 "코드 수정 없이" 불가
""".trimIndent())

// ── 7. RETRANSFORMATION vs 기본 전략 ─────────────────────────────
println("\n=== 7. RETRANSFORMATION vs 기본 전략 ===")

// 현재 JVM에 로딩된 클래스 중 Spring 관련 확인
val loadedPackages = Package.getPackages()
    .map { it.name }
    .filter { it.startsWith("org.springframework") || it.startsWith("javax.servlet") }
    .take(5)

println("[RETRANSFORM] 현재 로딩된 Spring/Servlet 패키지 (상위 5개):")
if (loadedPackages.isEmpty()) {
    println("  (스크립트 환경 — Spring 미로드)")
} else {
    loadedPackages.forEach { println("  $it") }
}

println("""
[RETRANSFORM] 세 가지 Agent 전략 비교:
  기본 전략 (없음):
    - ClassFileTransformer를 등록하면 이후 로딩되는 클래스만 변환
    - 이미 로딩된 클래스에는 적용 안 됨
    - log-friends에서 불충분한 이유:
      DispatcherServlet은 Spring Boot 초기화 시 이미 로딩됨

  REDEFINITION:
    - 이미 로딩된 클래스를 완전히 새로운 바이트코드로 교체
    - 메서드 본문만 교체 가능 (필드/메서드 추가/삭제 불가)
    - ClassFileTransformer 없이 직접 Instrumentation.redefineClasses() 호출

  RETRANSFORMATION (log-friends 선택):
    - 이미 로딩된 클래스를 ClassFileTransformer를 통해 재변환
    - Instrumentation.retransformClasses(clazz) → transformer 재호출
    - 왜 더 좋은가: ByteBuddy의 기존 transform 파이프라인 재사용 가능
    - EnvironmentPostProcessor 시점에 transformer 등록 후
      → retransformClasses로 이미 로딩된 DispatcherServlet 즉시 계측

  실행 시점 타이밍:
    EnvironmentPostProcessor (log-friends 실행)
      ↓ 아직 대부분 미로딩
    SpringApplication.run() → 빈 초기화
      ↓ DispatcherServlet 로딩됨 (이미 transformer 등록 → 자동 계측)
    ApplicationReadyEvent
      ↓ 모든 계측 완료
    HTTP 요청 처리 시작
""".trimIndent())
