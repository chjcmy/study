/**
 * 5장 — 객체 지향 프로그래밍
 *
 * 실행 방법:
 *   kotlinc OopDemo.kt -include-runtime -d oop.jar
 *   java -jar oop.jar
 *
 * 주제:
 *   - 인터페이스
 *   - 상속 & 추상 클래스
 *   - 다형성 & 타입 캐스팅
 *   - 봉인된 클래스 (sealed)
 *   - 내포된 클래스 & 내부 클래스
 *   - 동반 객체 (companion object)
 *   - 클래스 위임
 *   - 합성 (Composition)
 *   - 객체 선언 (object)
 *   - 타입 검사 (is, as)
 *   - 복잡한 생성자 & 부생성자
 */

// ── 1. 인터페이스 ─────────────────────────────────────────────────
interface Drawable {
    val color: String
        get() = "black"         // 인터페이스에 기본 구현 가능

    fun draw()
    fun describe() = "도형: color=$color"   // 기본 구현
}

interface Resizable {
    fun resize(factor: Double)
}

class Circle(
    val radius: Double,
    override val color: String = "red"
) : Drawable, Resizable {

    override fun draw() = println("원 그리기: radius=$radius, color=$color")
    override fun resize(factor: Double) = println("원 크기 변경: ${radius * factor}")
}

fun interfaceDemo() {
    println("=== 인터페이스 ===")

    val c = Circle(5.0, "blue")
    c.draw()
    c.resize(1.5)
    println(c.describe())

    // 인터페이스 타입으로 다형적 사용
    val drawable: Drawable = Circle(3.0)
    drawable.draw()
    println(drawable.color)     // 기본값 "black" 아님, 생성자 기본값 "red"
}

// ── 2. 상속 & 추상 클래스 ─────────────────────────────────────────
abstract class Animal(val name: String) {

    abstract fun sound(): String

    open fun describe(): String = "$name 이(가) 말합니다: ${sound()}"

    // open 없으면 final — 오버라이드 불가
    fun breathe() = println("$name: 숨을 쉽니다")
}

class Dog(name: String) : Animal(name) {
    override fun sound() = "멍멍"
}

class Cat(name: String, val indoor: Boolean) : Animal(name) {
    override fun sound() = "야옹"
    override fun describe() = super.describe() + if (indoor) " (실내)" else " (실외)"
}

// open 클래스 — 상속 허용 (기본은 final)
open class Vehicle(val brand: String, val speed: Int) {
    open fun info() = "$brand (${speed}km/h)"
}

class ElectricCar(brand: String, speed: Int, val range: Int) : Vehicle(brand, speed) {
    override fun info() = "${super.info()}, 주행거리 ${range}km"
}

fun inheritanceDemo() {
    println("\n=== 상속 & 추상 클래스 ===")

    val animals: List<Animal> = listOf(Dog("Rex"), Cat("Whiskers", true), Dog("Buddy"))
    for (a in animals) println(a.describe())

    println()
    val car = ElectricCar("Tesla", 250, 500)
    println(car.info())
}

// ── 3. 봉인된 클래스 (sealed) ─────────────────────────────────────
sealed class Result<out T> {
    data class Success<T>(val data: T) : Result<T>()
    data class Error(val message: String, val cause: Throwable? = null) : Result<Nothing>()
    object Loading : Result<Nothing>()
}

fun <T> handleResult(result: Result<T>): String = when (result) {
    is Result.Success -> "성공: ${result.data}"
    is Result.Error   -> "실패: ${result.message}"
    Result.Loading    -> "로딩 중..."
    // else 불필요 — sealed로 모든 서브클래스 컴파일러가 파악
}

fun sealedClassDemo() {
    println("\n=== 봉인된 클래스 ===")

    val results: List<Result<String>> = listOf(
        Result.Success("데이터 로드 완료"),
        Result.Error("네트워크 오류"),
        Result.Loading
    )
    results.forEach { println(handleResult(it)) }
}

// ── 4. 내포된 & 내부 클래스 ──────────────────────────────────────
class Outer(val value: Int) {

    // 내포된 클래스 (nested) — Outer 인스턴스 접근 불가 (Java의 static inner class)
    class Nested {
        fun describe() = "중첩 클래스 (Outer 접근 불가)"
    }

    // 내부 클래스 (inner) — Outer 인스턴스 접근 가능
    inner class Inner {
        fun describe() = "내부 클래스, outer.value = ${this@Outer.value}"
    }
}

fun nestedClassDemo() {
    println("\n=== 내포된 & 내부 클래스 ===")

    val nested = Outer.Nested()     // Outer 인스턴스 없이 생성
    println(nested.describe())

    val outer = Outer(42)
    val inner = outer.Inner()       // Outer 인스턴스 필요
    println(inner.describe())
}

// ── 5. 동반 객체 (companion object) ──────────────────────────────
class Logger private constructor(private val tag: String) {

    companion object {
        private var instance: Logger? = null

        fun getInstance(tag: String): Logger =
            instance ?: Logger(tag).also { instance = it }

        const val DEFAULT_TAG = "App"

        fun create(tag: String) = Logger(tag)
    }

    fun log(message: String) = println("[$tag] $message")
}

fun companionObjectDemo() {
    println("\n=== 동반 객체 ===")

    val logger = Logger.getInstance("Main")
    logger.log("앱 시작")

    val logger2 = Logger.getInstance("Main")
    println("같은 인스턴스: ${logger === logger2}")   // true

    println("DEFAULT_TAG: ${Logger.DEFAULT_TAG}")

    val customLogger = Logger.create("Debug")
    customLogger.log("디버그 메시지")
}

// ── 6. 클래스 위임 (by) ───────────────────────────────────────────
interface Printer {
    fun print(text: String)
    fun printLine(text: String) = print("$text\n")
}

class ConsolePrinter : Printer {
    override fun print(text: String) = kotlin.io.print(text)
}

class FormattedPrinter(
    private val printer: Printer = ConsolePrinter()
) : Printer by printer {             // Printer 구현을 printer에 위임

    // 필요한 메서드만 오버라이드
    override fun print(text: String) {
        printer.print(">>> $text <<<")
    }
}

fun delegationDemo() {
    println("\n=== 클래스 위임 ===")

    val printer = FormattedPrinter()
    printer.print("Hello")
    println()
    printer.printLine("World")  // 위임된 기본 구현 사용
}

// ── 7. 합성 vs 상속 ───────────────────────────────────────────────
// 합성: has-a 관계, 더 유연함
class Engine(val horsepower: Int) {
    fun start() = println("엔진 시동 (${horsepower}HP)")
    fun stop() = println("엔진 정지")
}

class Transmission(val type: String) {
    fun shift(gear: Int) = println("$type 변속: $gear단")
}

class ModernCar(
    val model: String,
    private val engine: Engine,         // 합성 — 상속 아님
    private val transmission: Transmission
) {
    fun start() = engine.start()
    fun drive(gear: Int) { transmission.shift(gear) }
    fun stop() = engine.stop()
}

fun compositionDemo() {
    println("\n=== 합성 ===")

    val car = ModernCar(
        "Sedan",
        Engine(200),
        Transmission("자동")
    )
    car.start()
    car.drive(3)
    car.stop()
}

// ── 8. 타입 검사 & 캐스팅 ────────────────────────────────────────
fun typeCheckDemo() {
    println("\n=== 타입 검사 & 캐스팅 ===")

    val objects: List<Any> = listOf(42, "hello", 3.14, listOf(1, 2), true)

    for (obj in objects) {
        when (obj) {
            is Int     -> println("Int: ${obj * 2}")       // 스마트 캐스트
            is String  -> println("String: ${obj.uppercase()}")
            is Double  -> println("Double: ${"%.1f".format(obj)}")
            is List<*> -> println("List: ${obj.size}개")
            else       -> println("기타: $obj")
        }
    }

    // as — 강제 캐스팅 (실패 시 ClassCastException)
    val any: Any = "Kotlin"
    val str: String = any as String
    println("강제 캐스트: $str")

    // as? — 안전한 캐스팅 (실패 시 null)
    val num: Any = 42
    val strOrNull: String? = num as? String
    println("안전한 캐스트: $strOrNull")    // null
}

// ── 9. 복잡한 생성자 & 부생성자 ──────────────────────────────────
class MultiConstructorClass(val primary: String) {

    var secondary: Int = 0
    var tertiary: Double = 0.0

    init {
        println("init 블록 1: primary=$primary")
    }

    // 부생성자 — constructor 키워드 + this() 위임
    constructor(primary: String, secondary: Int) : this(primary) {
        this.secondary = secondary
        println("부생성자 1: secondary=$secondary")
    }

    constructor(primary: String, secondary: Int, tertiary: Double) : this(primary, secondary) {
        this.tertiary = tertiary
        println("부생성자 2: tertiary=$tertiary")
    }

    override fun toString() = "Multi(primary=$primary, secondary=$secondary, tertiary=$tertiary)"
}

fun constructorDemo() {
    println("\n=== 복잡한 생성자 ===")

    println("--- 주 생성자 ---")
    val c1 = MultiConstructorClass("only")
    println(c1)

    println("--- 부생성자 1 ---")
    val c2 = MultiConstructorClass("A", 10)
    println(c2)

    println("--- 부생성자 2 ---")
    val c3 = MultiConstructorClass("B", 20, 3.14)
    println(c3)
}

// ── 10. object 선언 ──────────────────────────────────────────────
object Registry {
    private val entries = mutableMapOf<String, Any>()

    fun register(key: String, value: Any) { entries[key] = value }
    fun get(key: String): Any? = entries[key]
    fun size() = entries.size
}

fun objectDeclarationDemo() {
    println("\n=== object 선언 ===")

    Registry.register("version", "1.0.0")
    Registry.register("author", "log-friends")
    println("Registry size: ${Registry.size()}")
    println("version: ${Registry.get("version")}")

    // 익명 객체
    val greet = object {
        val lang = "Kotlin"
        fun hello() = "Hello from $lang!"
    }
    println(greet.hello())
}

fun main() {
    interfaceDemo()
    inheritanceDemo()
    sealedClassDemo()
    nestedClassDemo()
    companionObjectDemo()
    delegationDemo()
    compositionDemo()
    typeCheckDemo()
    constructorDemo()
    objectDeclarationDemo()
}
