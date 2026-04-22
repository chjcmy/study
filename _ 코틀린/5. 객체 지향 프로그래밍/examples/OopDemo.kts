#!/usr/bin/env kotlinc -script
/**
 * 5장 — 객체 지향 프로그래밍
 * 실행: kotlinc -script OopDemo.kts
 */

// ── 1. 인터페이스 ─────────────────────────────────────────────────
interface Drawable {
    val color: String get() = "black"
    fun draw()
    fun describe() = "도형: color=$color"
}

interface Resizable { fun resize(factor: Double) }

class Circle(val radius: Double, override val color: String = "red") : Drawable, Resizable {
    override fun draw() = println("원 그리기: radius=$radius, color=$color")
    override fun resize(factor: Double) = println("원 크기 변경: ${radius * factor}")
}

println("=== 인터페이스 ===")
val c = Circle(5.0, "blue")
c.draw(); c.resize(1.5); println(c.describe())

// ── 2. 상속 & 추상 클래스 ─────────────────────────────────────────
abstract class Animal(val name: String) {
    abstract fun sound(): String
    open fun describe() = "$name 이(가) 말합니다: ${sound()}"
}

class Dog(name: String) : Animal(name) { override fun sound() = "멍멍" }
class Cat(name: String, val indoor: Boolean) : Animal(name) {
    override fun sound() = "야옹"
    override fun describe() = super.describe() + if (indoor) " (실내)" else " (실외)"
}

println("\n=== 상속 & 추상 클래스 ===")
listOf(Dog("Rex"), Cat("Whiskers", true), Dog("Buddy")).forEach { println(it.describe()) }

// ── 3. 봉인된 클래스 ──────────────────────────────────────────────
sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val message: String) : Result<Nothing>()
    object Loading : Result<Nothing>()
}

fun <T> handle(r: Result<T>) = when (r) {
    is Result.Success -> "성공: ${r.data}"
    is Result.Error   -> "실패: ${r.message}"
    Result.Loading    -> "로딩 중..."
}

println("\n=== 봉인된 클래스 ===")
listOf(Result.Success("데이터"), Result.Error("네트워크 오류"), Result.Loading)
    .forEach { println(handle(it)) }

// ── 4. 내포된 & 내부 클래스 ──────────────────────────────────────
class Outer(val value: Int) {
    class Nested { fun describe() = "중첩 클래스 (Outer 접근 불가)" }
    inner class Inner { fun describe() = "내부 클래스, outer.value = ${this@Outer.value}" }
}

println("\n=== 내포된 & 내부 클래스 ===")
println(Outer.Nested().describe())
println(Outer(42).Inner().describe())

// ── 5. 동반 객체 ──────────────────────────────────────────────────
class Logger private constructor(private val tag: String) {
    companion object {
        private var instance: Logger? = null
        fun getInstance(tag: String) = instance ?: Logger(tag).also { instance = it }
        const val DEFAULT_TAG = "App"
    }
    fun log(msg: String) = println("[$tag] $msg")
}

println("\n=== 동반 객체 ===")
val log1 = Logger.getInstance("Main")
log1.log("앱 시작")
println("같은 인스턴스: ${log1 === Logger.getInstance("Main")}")
println("DEFAULT_TAG: ${Logger.DEFAULT_TAG}")

// ── 6. 클래스 위임 ────────────────────────────────────────────────
interface Printer { fun print(text: String) }

class ConsolePrinter : Printer {
    override fun print(text: String) = kotlin.io.print(text)
}

class FormattedPrinter(private val printer: Printer = ConsolePrinter()) : Printer by printer {
    override fun print(text: String) { printer.print(">>> $text <<<") }
}

println("\n=== 클래스 위임 ===")
FormattedPrinter().print("Hello")
println()

// ── 7. 합성 ───────────────────────────────────────────────────────
class Engine(val hp: Int) {
    fun start() = println("엔진 시동 (${hp}HP)")
    fun stop()  = println("엔진 정지")
}

class Transmission(val type: String) { fun shift(gear: Int) = println("$type 변속: $gear단") }

class ModernCar(val model: String, private val engine: Engine, private val trans: Transmission) {
    fun start() = engine.start()
    fun drive(gear: Int) = trans.shift(gear)
    fun stop()  = engine.stop()
}

println("\n=== 합성 ===")
ModernCar("Sedan", Engine(200), Transmission("자동")).run { start(); drive(3); stop() }

// ── 8. 타입 검사 & 캐스팅 ────────────────────────────────────────
println("\n=== 타입 검사 & 캐스팅 ===")
listOf(42, "hello", 3.14, listOf(1, 2), true).forEach { obj ->
    when (obj) {
        is Int     -> println("Int: ${obj * 2}")
        is String  -> println("String: ${obj.uppercase()}")
        is Double  -> println("Double: ${"%.1f".format(obj)}")
        is List<*> -> println("List: ${obj.size}개")
        else       -> println("기타: $obj")
    }
}
val num: Any = 42
println("안전한 캐스트: ${num as? String}")    // null

// ── 9. object 선언 ────────────────────────────────────────────────
object Registry {
    private val entries = mutableMapOf<String, Any>()
    fun register(key: String, value: Any) { entries[key] = value }
    fun get(key: String): Any? = entries[key]
    fun size() = entries.size
}

println("\n=== object 선언 ===")
Registry.register("version", "1.0.0")
Registry.register("author", "log-friends")
println("size: ${Registry.size()}, version: ${Registry.get("version")}")

val anon = object { val lang = "Kotlin"; fun hello() = "Hello from $lang!" }
println(anon.hello())
