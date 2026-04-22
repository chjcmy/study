#!/usr/bin/env kotlinc -script
/**
 * 7장 — 파워 툴
 * 실행: kotlinc -script PowerToolsDemo.kts
 */

import kotlin.properties.Delegates
import kotlin.reflect.KProperty

// ── 1. 확장 람다 & DSL ────────────────────────────────────────────
fun buildString(block: StringBuilder.() -> Unit): String {
    val sb = StringBuilder(); sb.block(); return sb.toString()
}

class HtmlBuilder {
    private val content = StringBuilder()
    fun h1(text: String) { content.appendLine("<h1>$text</h1>") }
    fun p(text: String)  { content.appendLine("<p>$text</p>") }
    fun build() = content.toString()
}
fun html(block: HtmlBuilder.() -> Unit) = HtmlBuilder().apply(block).build()

println("=== 확장 람다 & DSL ===")
println(buildString { append("Hello, "); append("Kotlin!") })
println(html { h1("제목"); p("첫 번째 단락"); p("두 번째 단락") })

// ── 2. 영역 함수 ──────────────────────────────────────────────────
data class User(var name: String = "", var age: Int = 0, var email: String = "")

println("=== 영역 함수 ===")

// let — null 체크
val n: String? = "Kotlin"
val len = n?.let { println("let: $it"); it.length }
println("length = $len")

// run — 객체 설정 + 결과 반환
val user = User().run { name = "Alice"; age = 30; email = "alice@a.com"; this }
println("run: $user")

// with — 멤버 직접 호출
val summary = with(user) { "이름: $name, 나이: $age" }
println("with: $summary")

// apply — 빌더 패턴, 수신 객체 반환
val configured = User().apply { name = "Bob"; age = 25; email = "bob@b.com" }
println("apply: $configured")

// also — 부수 효과 (로깅), 수신 객체 반환
val logged = configured.also { println("also: 사용자 생성됨 — ${it.name}") }
println("also 동일 객체: ${logged === configured}")

// ── 3. 제네릭스 (공변 / 반변 / reified) ──────────────────────────
class Producer<out T>(private val value: T) { fun get(): T = value }
class Consumer<in T> { fun consume(v: T) = println("소비: $v") }

inline fun <reified T> List<*>.filterIsType(): List<T> = filterIsInstance<T>()
inline fun <reified T> isType(v: Any) = v is T

println("\n=== 제네릭스 ===")
val strProducer: Producer<String> = Producer("hello")
val anyProducer: Producer<Any> = strProducer    // 공변 OK
println("공변: ${anyProducer.get()}")

val anyConsumer: Consumer<Any> = Consumer()
val strConsumer: Consumer<String> = anyConsumer  // 반변 OK
strConsumer.consume("Kotlin")

val mixed: List<Any> = listOf(1, "hello", 2.0, true, "world", 3)
println("문자열: ${mixed.filterIsType<String>()}")
println("정수: ${mixed.filterIsType<Int>()}")
println("\"hi\" is String: ${isType<String>("hi")}, 42 is String: ${isType<String>(42)}")

// ── 4. 연산자 오버로딩 ────────────────────────────────────────────
data class Vector(val x: Double, val y: Double) {
    operator fun plus(o: Vector)  = Vector(x + o.x, y + o.y)
    operator fun minus(o: Vector) = Vector(x - o.x, y - o.y)
    operator fun times(s: Double) = Vector(x * s, y * s)
    operator fun unaryMinus()     = Vector(-x, -y)
    operator fun contains(v: Double) = x == v || y == v
    val magnitude get() = Math.sqrt(x * x + y * y)
    override fun toString() = "Vector(${"%.1f".format(x)}, ${"%.1f".format(y)})"
}

println("\n=== 연산자 오버로딩 ===")
val v1 = Vector(3.0, 4.0); val v2 = Vector(1.0, 2.0)
println("v1 = $v1, |v1| = ${"%.2f".format(v1.magnitude)}")
println("v1 + v2 = ${v1 + v2}")
println("v1 * 2.0 = ${v1 * 2.0}")
println("-v1 = ${-v1}")
println("3.0 in v1: ${3.0 in v1}")

// ── 5. 프로퍼티 위임 (커스텀) ─────────────────────────────────────
class LoggingDelegate<T>(initialValue: T, private val name: String) {
    private var value = initialValue
    operator fun getValue(t: Any?, p: KProperty<*>): T { println("GET $name = $value"); return value }
    operator fun setValue(t: Any?, p: KProperty<*>, v: T) { println("SET $name: $value → $v"); value = v }
}

class Config { var debug: Boolean by LoggingDelegate(false, "debug") }

println("\n=== 프로퍼티 위임 (커스텀) ===")
val cfg = Config()
println(cfg.debug)
cfg.debug = true
println(cfg.debug)

// ── 6. 위임 도구 (observable / vetoable) ─────────────────────────
println("\n=== 위임 도구 ===")

var observed: String by Delegates.observable("초기값") { p, old, new ->
    println("${p.name}: '$old' → '$new'")
}
observed = "변경1"; observed = "변경2"

var positiveOnly: Int by Delegates.vetoable(0) { _, old, new ->
    val ok = new >= 0; if (!ok) println("거부: $old → $new (음수 불가)"); ok
}
positiveOnly = 10; positiveOnly = -5
println("positiveOnly = $positiveOnly")   // 10

// ── 7. lazy ───────────────────────────────────────────────────────
println("\n=== lazy ===")

val heavy: String by lazy {
    println("lazy 초기화!")
    "무거운 값"
}
println("첫 접근: $heavy")
println("두 번째 접근: $heavy")   // 재계산 없음

// ── 8. lateinit ───────────────────────────────────────────────────
class Container {
    lateinit var service: String
    fun isReady() = ::service.isInitialized
}

println("\n=== lateinit ===")
val container = Container()
println("초기화 전: ${container.isReady()}")

try { println(container.service) }
catch (e: UninitializedPropertyAccessException) { println("미초기화 접근: ${e.message}") }

container.service = "UserService"
println("초기화 후: ${container.isReady()}, service: ${container.service}")

println("""
lazy vs lateinit:
  lazy     — val, 처음 접근 시 초기화, 스레드 안전
  lateinit — var, 수동 초기화, isInitialized 확인 가능
""".trimIndent())
