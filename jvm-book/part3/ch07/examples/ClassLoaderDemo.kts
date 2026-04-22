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

// ── 6. 능동적 사용 vs 수동적 사용 ────────────────────────────────
println("\n=== 6. 능동적 사용 vs 수동적 사용 ===")
println("""
  능동적 사용(Active Use) → 클래스 초기화 발생:
    - new 로 인스턴스 생성
    - 자기 자신의 정적 필드 접근/할당
    - 정적 메서드 호출
    - 최상위 클래스로 리플렉션 사용
    - JVM 시작 시 main 클래스

  수동적 사용(Passive Use) → 초기화 없음:
    - 자식 클래스를 통한 부모 정적 필드 접근
    - 배열 타입 정의 (원소 클래스 미초기화)
    - 컴파일타임 상수(const val) 참조
""".trimIndent())

// 수동적 사용 1: 자식 클래스를 통해 부모 static 필드 접근 → 부모만 초기화
println("--- 수동적 사용 1: 자식 클래스로 부모 정적 필드 접근 ---")
open class SuperClass {
    companion object {
        val VALUE: Int
        init {
            println("[PassiveUse1] SuperClass 초기화됨")
            VALUE = 100
        }
    }
}
class SubClass : SuperClass() {
    companion object {
        init { println("[PassiveUse1] SubClass 초기화됨") }
    }
}

// SubClass를 통해 접근하지만 SuperClass.VALUE이므로 SuperClass만 초기화
// 스크립트 환경에서는 동반 객체 접근으로 시뮬레이션
println("[PassiveUse1] SuperClass.VALUE 접근 전")
println("[PassiveUse1] SuperClass.VALUE = ${SuperClass.VALUE}")
println("[PassiveUse1] → SubClass는 초기화 안 됨 (실제 바이트코드에서 getstatic은 부모 클래스 대상)")

// 수동적 사용 2: 배열 정의 → 원소 클래스 초기화 안 됨
println("\n--- 수동적 사용 2: 배열 정의 ---")
class ElementClass {
    companion object {
        init { println("[PassiveUse2] ElementClass 초기화됨") }
        val DATA = "element"
    }
}

println("[PassiveUse2] ElementClass 배열 선언 전")
val arr: Array<ElementClass?> = arrayOfNulls(3)  // 배열만 생성, ElementClass 미초기화
println("[PassiveUse2] arrayOfNulls(3) 실행 완료 — ElementClass 초기화 여부 확인:")
println("[PassiveUse2] arr.javaClass.name = ${arr.javaClass.name}")
println("[PassiveUse2] → 배열 객체 생성 완료, ElementClass 자체는 아직 초기화 안 됨")
println("[PassiveUse2] ElementClass 인스턴스 생성 시 초기화 발생:")
arr[0] = ElementClass()  // 여기서 비로소 초기화

// 수동적 사용 3: 컴파일타임 상수(const val) 참조 → 클래스 초기화 안 됨
println("\n--- 수동적 사용 3: 컴파일타임 상수(const val) 참조 ---")
class ConstHolder {
    companion object {
        const val COMPILE_TIME_CONST = "HELLO_CONST"   // 컴파일타임 상수
        val RUNTIME_VAL: String                          // 런타임 필드
        init {
            println("[PassiveUse3] ConstHolder 초기화됨")
            RUNTIME_VAL = "runtime"
        }
    }
}

// const val은 컴파일러가 참조 지점에 직접 값을 인라인 삽입
// → 클래스 로딩/초기화 없이 상수 값 사용 가능
println("[PassiveUse3] ConstHolder.COMPILE_TIME_CONST 참조:")
val c = ConstHolder.COMPILE_TIME_CONST  // 초기화 발생 안 함 (컴파일타임 인라인)
println("[PassiveUse3] 값 = $c")
println("[PassiveUse3] → const val은 바이트코드에 ldc 명령어로 직접 삽입 (getstatic 없음)")
println("[PassiveUse3] RUNTIME_VAL 접근 시 초기화 발생:")
println("[PassiveUse3] ConstHolder.RUNTIME_VAL = ${ConstHolder.RUNTIME_VAL}")

// ── 7. JDK 9 모듈 시스템 영향 ────────────────────────────────────
println("\n=== 7. JDK 9 모듈 시스템 영향 ===")

// 모듈 정보 출력
println("[Module] 주요 클래스들의 모듈 정보:")
listOf(
    "java.lang.String"      to String::class.java,
    "java.util.ArrayList"   to ArrayList::class.java,
    "java.lang.reflect.Field" to java.lang.reflect.Field::class.java,
    "sun.misc.Unsafe (내부)" to try { Class.forName("sun.misc.Unsafe") } catch (e: Exception) { null }
).forEach { (label, clazz) ->
    if (clazz != null) {
        val mod = clazz.module
        println("  $label")
        println("    모듈명: ${mod.name ?: "unnamed"}, 이름있음: ${mod.isNamed}")
    } else {
        println("  $label → 접근 불가")
    }
}

// 현재 스크립트의 모듈 정보
println("\n[Module] 현재 실행 컨텍스트:")
val currentModule = object {}.javaClass.module
println("  현재 모듈: ${currentModule.name ?: "unnamed (클래스패스 기반 실행)"}")
println("  모듈 레이어: ${currentModule.layer}")

println("""
[Module] JDK 9 모듈 시스템과 --add-opens:

  JDK 8까지: 리플렉션으로 모든 클래스의 private 필드/메서드 접근 가능
  JDK 9+   : 모듈 경계 강화 — 모듈 외부에서 비공개 API 접근 시 InaccessibleObjectException

  --add-opens 필요 상황:
    --add-opens java.base/java.lang=ALL-UNNAMED
    → java.lang 패키지의 비공개 API를 unnamed 모듈에서 열어줌
    → Spring Framework 내부, ByteBuddy, 다양한 라이브러리가 필요로 함

  log-friends의 -Djdk.attach.allowAttachSelf=true:
    - JDK 9 이후 모듈 시스템은 attach API 접근도 제한
    - java.lang.ProcessHandle 등 attach 관련 클래스가 java.base 모듈 내부에 있음
    - 같은 JVM 프로세스가 자기 자신에게 attach 시도 시 보안 제한 발동
    - -Djdk.attach.allowAttachSelf=true → JVM 레벨 플래그로 자기 attach 허용
    - log-friends 런타임 계측 (LogFriendsInstaller) 이 이 플래그 없으면 동작 불가

  ByteBuddy + 모듈 시스템:
    - ByteBuddy는 내부적으로 sun.misc.Unsafe 또는 MethodHandles.privateLookupIn() 사용
    - JDK 9+에서 instrumentation 대상 클래스의 모듈을 열어야(open) 재정의 가능
    - Instrumentation.redefineModule() API 로 런타임에 모듈 개방 가능
""".trimIndent())
