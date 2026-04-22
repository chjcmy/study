/**
 * 7장 — 파워 툴
 *
 * 실행 방법:
 *   kotlinc PowerToolsDemo.kt -include-runtime -d powertools.jar
 *   java -jar powertools.jar
 *
 * 주제:
 *   - 확장 람다 (수신 객체 지정 람다)
 *   - 영역 함수 (let, run, with, apply, also)
 *   - 제네릭스 만들기 (공변, 반변, reified)
 *   - 연산자 오버로딩
 *   - 프로퍼티 위임 (by)
 *   - 프로퍼티 위임 도구 (lazy, observable, vetoable)
 *   - 지연 계산 초기화 (lazy)
 *   - 늦은 초기화 (lateinit)
 */

import kotlin.properties.Delegates
import kotlin.reflect.KProperty

// ── 1. 확장 람다 (수신 객체 지정 람다) ───────────────────────────
// 람다 내에서 this = 수신 객체
fun buildString(block: StringBuilder.() -> Unit): String {
    val sb = StringBuilder()
    sb.block()
    return sb.toString()
}

// DSL 스타일 빌더
class HtmlBuilder {
    private val content = StringBuilder()

    fun h1(text: String) { content.appendLine("<h1>$text</h1>") }
    fun p(text: String)  { content.appendLine("<p>$text</p>") }
    fun build() = content.toString()
}

fun html(block: HtmlBuilder.() -> Unit): String {
    val builder = HtmlBuilder()
    builder.block()
    return builder.build()
}

fun extensionLambdaDemo() {
    println("=== 확장 람다 ===")

    val result = buildString {
        append("Hello, ")       // this = StringBuilder
        append("Kotlin!")
        appendLine()
        append("Power tools.")
    }
    println(result)

    val page = html {
        h1("제목")
        p("첫 번째 단락")
        p("두 번째 단락")
    }
    println(page)
}

// ── 2. 영역 함수 ──────────────────────────────────────────────────
data class User(
    var name: String = "",
    var age: Int = 0,
    var email: String = ""
)

fun scopeFunctionDemo() {
    println("=== 영역 함수 ===")

    // let — 수신 객체를 it으로, 마지막 식 반환 (null 체크에 유용)
    val name: String? = "Kotlin"
    val length = name?.let {
        println("let: $it")
        it.length           // 반환값
    }
    println("length = $length")

    // run — 수신 객체를 this로, 마지막 식 반환
    val user = User().run {
        name  = "Alice"
        age   = 30
        email = "alice@example.com"
        this                // User 반환
    }
    println("run: $user")

    // with — 수신 객체를 this로 (확장 아님), 마지막 식 반환
    val summary = with(user) {
        "이름: $name, 나이: $age"   // User에서 직접 접근
    }
    println("with: $summary")

    // apply — 수신 객체를 this로, 수신 객체 반환 (빌더 패턴)
    val configured = User().apply {
        name  = "Bob"
        age   = 25
        email = "bob@example.com"
    }
    println("apply: $configured")

    // also — 수신 객체를 it으로, 수신 객체 반환 (부수 효과, 로깅)
    val logged = configured.also {
        println("also: 사용자 생성됨 — ${it.name}")
    }
    println("also returned: ${logged === configured}")  // true, 같은 객체

    println("""
영역 함수 선택 가이드:
  let   : null 체크 + 변환 결과 반환
  run   : 객체 설정 + 결과 계산
  with  : 여러 멤버 호출, 결과 반환
  apply : 객체 설정 후 객체 반환 (빌더)
  also  : 로깅/검증 등 부수 효과, 체이닝
    """.trimIndent())
}

// ── 3. 제네릭스 만들기 ────────────────────────────────────────────
// out (공변): 생산자, T를 반환만 함
// in  (반변): 소비자, T를 인자로만 받음
// reified: 런타임에 타입 정보 보존 (inline 함수에서만)

class Producer<out T>(private val value: T) {
    fun get(): T = value
}

class Consumer<in T> {
    fun consume(value: T) = println("소비: $value")
}

inline fun <reified T> List<*>.filterIsType(): List<T> =
    filterIsInstance<T>()

inline fun <reified T> isExactType(value: Any): Boolean = value is T

fun genericsDemo() {
    println("\n=== 제네릭스 만들기 ===")

    // 공변 (out) — Producer<Dog>를 Producer<Animal>로 사용 가능
    val strProducer: Producer<String> = Producer("hello")
    val anyProducer: Producer<Any> = strProducer    // OK (out 공변)
    println("공변: ${anyProducer.get()}")

    // 반변 (in) — Consumer<Any>를 Consumer<String>으로 사용 가능
    val anyConsumer: Consumer<Any> = Consumer()
    val strConsumer: Consumer<String> = anyConsumer  // OK (in 반변)
    strConsumer.consume("Kotlin")

    // reified — 런타임 타입 보존
    val mixed: List<Any> = listOf(1, "hello", 2.0, true, "world", 3)
    val strings = mixed.filterIsType<String>()
    val ints = mixed.filterIsType<Int>()
    println("문자열: $strings")
    println("정수: $ints")

    println("\"hi\" is String: ${isExactType<String>("hi")}")
    println("42 is String: ${isExactType<String>(42)}")
}

// ── 4. 연산자 오버로딩 ────────────────────────────────────────────
data class Vector(val x: Double, val y: Double) {

    operator fun plus(other: Vector)  = Vector(x + other.x, y + other.y)
    operator fun minus(other: Vector) = Vector(x - other.x, y - other.y)
    operator fun times(scalar: Double) = Vector(x * scalar, y * scalar)
    operator fun unaryMinus() = Vector(-x, -y)

    val magnitude: Double get() = Math.sqrt(x * x + y * y)

    // in 연산자 (contains)
    operator fun contains(value: Double) = x == value || y == value

    override fun toString() = "Vector(${"%.1f".format(x)}, ${"%.1f".format(y)})"
}

// compareTo — <, >, <=, >= 오버로딩
data class Money(val amount: Double, val currency: String) : Comparable<Money> {
    override fun compareTo(other: Money): Int {
        require(currency == other.currency) { "통화 불일치" }
        return amount.compareTo(other.amount)
    }

    operator fun plus(other: Money): Money {
        require(currency == other.currency)
        return Money(amount + other.amount, currency)
    }
}

fun operatorOverloadDemo() {
    println("\n=== 연산자 오버로딩 ===")

    val v1 = Vector(3.0, 4.0)
    val v2 = Vector(1.0, 2.0)

    println("v1 = $v1, |v1| = ${"%.2f".format(v1.magnitude)}")
    println("v1 + v2 = ${v1 + v2}")
    println("v1 - v2 = ${v1 - v2}")
    println("v1 * 2.0 = ${v1 * 2.0}")
    println("-v1 = ${-v1}")
    println("3.0 in v1: ${3.0 in v1}")   // contains

    val m1 = Money(100.0, "USD")
    val m2 = Money(50.0, "USD")
    println("$m1 + $m2 = ${m1 + m2}")
    println("m1 > m2: ${m1 > m2}")       // compareTo
}

// ── 5. 프로퍼티 위임 ──────────────────────────────────────────────
// 커스텀 위임 구현
class LoggingDelegate<T>(initialValue: T, private val name: String) {
    private var value = initialValue

    operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
        println("GET $name = $value")
        return value
    }

    operator fun setValue(thisRef: Any?, property: KProperty<*>, newValue: T) {
        println("SET $name: $value → $newValue")
        value = newValue
    }
}

class Config {
    var debug: Boolean by LoggingDelegate(false, "debug")
    var timeout: Int by LoggingDelegate(30, "timeout")
}

fun propertyDelegationDemo() {
    println("\n=== 프로퍼티 위임 ===")

    val config = Config()
    println("초기 debug: ${config.debug}")
    config.debug = true
    config.timeout = 60
    println("현재 timeout: ${config.timeout}")
}

// ── 6. 프로퍼티 위임 도구 ─────────────────────────────────────────
fun delegationToolsDemo() {
    println("\n=== 프로퍼티 위임 도구 ===")

    // observable — 변경 감지
    var observedName: String by Delegates.observable("초기값") { prop, old, new ->
        println("${prop.name}: '$old' → '$new'")
    }
    observedName = "변경1"
    observedName = "변경2"

    // vetoable — 변경 거부 가능
    var positiveOnly: Int by Delegates.vetoable(0) { _, old, new ->
        val accept = new >= 0
        if (!accept) println("거부: $old → $new (음수 불가)")
        accept
    }
    positiveOnly = 10       // 허용
    positiveOnly = -5       // 거부
    println("positiveOnly = $positiveOnly")   // 10

    // notNull — 초기화 전 접근 방지 (lateinit과 유사, null 가능 타입에서도 사용)
    var notNullValue: String by Delegates.notNull()
    notNullValue = "initialized"
    println("notNull = $notNullValue")
}

// ── 7. 지연 계산 초기화 (lazy) ────────────────────────────────────
class ExpensiveResource {
    init { println("ExpensiveResource 생성 (비용 큼)") }
    fun compute() = println("연산 수행")
}

object AppConfig {
    // 처음 접근할 때만 초기화, 이후 캐시
    val heavyResource: ExpensiveResource by lazy {
        println("lazy 초기화 시작")
        ExpensiveResource()
    }

    // 스레드 안전 모드 선택
    val threadSafe: String by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        "스레드 안전 lazy"
    }

    val none: String by lazy(LazyThreadSafetyMode.NONE) {
        "단일 스레드 전용 (빠름)"
    }
}

fun lazyInitDemo() {
    println("\n=== 지연 계산 초기화 ===")

    println("AppConfig 생성 후 (아직 heavyResource 미접근)")
    println("첫 번째 접근:")
    AppConfig.heavyResource.compute()
    println("두 번째 접근 (캐시된 값):")
    AppConfig.heavyResource.compute()

    // isInitialized 확인
    val lazyVal: String by lazy { "초기화됨" }
    // lazyVal.isInitialized    // 확장 프로퍼티로 확인 가능
    println(lazyVal)
}

// ── 8. 늦은 초기화 (lateinit) ────────────────────────────────────
class ServiceContainer {
    // var이고 non-null, 기본 타입 불가 (Int → Int? 사용)
    lateinit var service: String
    lateinit var config: Map<String, Any>

    fun initialize() {
        service = "UserService"
        config = mapOf("timeout" to 30, "retry" to 3)
    }

    fun isReady() = ::service.isInitialized && ::config.isInitialized
}

fun lateinitDemo() {
    println("\n=== 늦은 초기화 (lateinit) ===")

    val container = ServiceContainer()
    println("초기화 전 isReady: ${container.isReady()}")

    // 접근하면 UninitializedPropertyAccessException
    try {
        println(container.service)
    } catch (e: UninitializedPropertyAccessException) {
        println("미초기화 접근: ${e.message}")
    }

    container.initialize()
    println("초기화 후 isReady: ${container.isReady()}")
    println("service: ${container.service}")
    println("config: ${container.config}")

    println("""
lazy vs lateinit:
  lazy    — val, 처음 접근 시 초기화, 스레드 안전 옵션
  lateinit — var, 나중에 수동 초기화, isInitialized 확인 가능
  사용처:
    lazy    → 무거운 계산, 싱글톤 컴포넌트
    lateinit → DI 프레임워크, 테스트 setUp
    """.trimIndent())
}

fun main() {
    extensionLambdaDemo()
    scopeFunctionDemo()
    genericsDemo()
    operatorOverloadDemo()
    propertyDelegationDemo()
    delegationToolsDemo()
    lazyInitDemo()
    lateinitDemo()
}
