/**
 * 3장 — 사용성
 *
 * 실행 방법:
 *   kotlinc UsabilityDemo.kt -include-runtime -d usability.jar
 *   java -jar usability.jar
 *
 * 주제:
 *   - 확장 함수 / 확장 프로퍼티
 *   - 이름 붙은 인자 & 디폴트 인자
 *   - 오버로딩
 *   - when 식
 *   - 이넘 (enum)
 *   - 데이터 클래스
 *   - 구조 분해 선언
 *   - 널이 될 수 있는 타입 / 안전한 호출 / 엘비스 / 널 아님 단언
 *   - 제네릭스 소개
 */

// ── 1. 확장 함수 & 확장 프로퍼티 ─────────────────────────────────
fun String.isPalindrome(): Boolean = this == this.reversed()

fun String.wordCount(): Int = if (isBlank()) 0 else trim().split("\\s+".toRegex()).size

val String.lastChar: Char
    get() = this[length - 1]

fun extensionDemo() {
    println("=== 확장 함수 & 프로퍼티 ===")

    println("\"racecar\".isPalindrome() = ${"racecar".isPalindrome()}")
    println("\"hello\".isPalindrome()   = ${"hello".isPalindrome()}")
    println("\"hello world\".wordCount() = ${"hello world".wordCount()}")
    println("\"Kotlin\".lastChar = ${"Kotlin".lastChar}")

    // 확장 함수는 멤버 함수보다 우선순위 낮음
    // 컴파일 시 정적으로 결정됨 (다형성 없음)
    println("확장 함수: 기존 클래스 수정 없이 기능 추가 가능")
}

// ── 2. 이름 붙은 인자 & 디폴트 인자 ─────────────────────────────
fun createUser(
    name: String,
    age: Int = 0,
    email: String = "",
    isAdmin: Boolean = false
): String = "User(name=$name, age=$age, email=$email, admin=$isAdmin)"

fun namedAndDefaultArgsDemo() {
    println("\n=== 이름 붙은 인자 & 디폴트 인자 ===")

    println(createUser("Alice"))                            // 디폴트 사용
    println(createUser("Bob", age = 30))                    // 일부만 지정
    println(createUser("Charlie", isAdmin = true, age = 25)) // 순서 무관
    println(createUser(name = "Dave", email = "d@d.com"))   // 모두 이름 붙임
}

// ── 3. 오버로딩 ───────────────────────────────────────────────────
fun describe(value: Int) = "Int: $value"
fun describe(value: Double) = "Double: $value"
fun describe(value: String) = "String: \"$value\""
fun describe(value: Boolean) = "Boolean: $value"

fun overloadDemo() {
    println("\n=== 오버로딩 ===")
    println(describe(42))
    println(describe(3.14))
    println(describe("hello"))
    println(describe(true))
}

// ── 4. when 식 ────────────────────────────────────────────────────
sealed class Shape
data class Circle(val radius: Double) : Shape()
data class Rectangle(val w: Double, val h: Double) : Shape()
data class Triangle(val base: Double, val height: Double) : Shape()

fun area(shape: Shape): Double = when (shape) {
    is Circle    -> Math.PI * shape.radius * shape.radius
    is Rectangle -> shape.w * shape.h
    is Triangle  -> 0.5 * shape.base * shape.height
}

fun whenDemo() {
    println("\n=== when 식 ===")

    // 값 매칭
    val x = 3
    val name = when (x) {
        1    -> "one"
        2, 3 -> "two or three"
        in 4..10 -> "four to ten"
        else -> "other"
    }
    println("$x → $name")

    // 타입 검사
    val shapes: List<Shape> = listOf(
        Circle(5.0), Rectangle(4.0, 6.0), Triangle(3.0, 8.0)
    )
    for (s in shapes) {
        println("${s::class.simpleName} 넓이: ${"%.2f".format(area(s))}")
    }

    // 인자 없는 when — if-else 체인 대체
    val score = 75
    val grade = when {
        score >= 90 -> "A"
        score >= 80 -> "B"
        score >= 70 -> "C"
        else        -> "F"
    }
    println("$score점 → $grade")
}

// ── 5. 이넘 (enum class) ──────────────────────────────────────────
enum class Direction(val degrees: Int) {
    NORTH(0), EAST(90), SOUTH(180), WEST(270);

    fun opposite(): Direction = when (this) {
        NORTH -> SOUTH
        EAST  -> WEST
        SOUTH -> NORTH
        WEST  -> EAST
    }
}

fun enumDemo() {
    println("\n=== 이넘 ===")

    for (dir in Direction.entries) {
        println("${dir.name}(${dir.degrees}°) ↔ ${dir.opposite().name}")
    }

    val d = Direction.valueOf("NORTH")
    println("valueOf: $d, ordinal: ${d.ordinal}")
}

// ── 6. 데이터 클래스 ──────────────────────────────────────────────
data class Point(val x: Int, val y: Int)

data class Employee(
    val id: Int,
    val name: String,
    val department: String,
    val salary: Double
)

fun dataClassDemo() {
    println("\n=== 데이터 클래스 ===")

    val p1 = Point(3, 4)
    val p2 = Point(3, 4)
    val p3 = p1.copy(y = 10)

    println("p1 == p2: ${p1 == p2}")    // true (구조 비교)
    println("p1 === p2: ${p1 === p2}")  // false (참조 비교)
    println("p3: $p3")                  // copy

    // 자동 생성: equals, hashCode, toString, copy, componentN
    val emp = Employee(1, "Alice", "Engineering", 80000.0)
    println(emp)
    println("이름: ${emp.component2()}")    // componentN 직접 호출

    // HashMap 키로 사용 가능 (hashCode 구현됨)
    val map = mapOf(p1 to "origin area", p3 to "shifted")
    println(map[Point(3, 4)])   // origin area
}

// ── 7. 구조 분해 선언 ─────────────────────────────────────────────
fun destructuringDemo() {
    println("\n=== 구조 분해 선언 ===")

    val p = Point(10, 20)
    val (x, y) = p
    println("x=$x, y=$y")

    // 불필요한 컴포넌트는 _ 로 무시
    val emp = Employee(1, "Bob", "HR", 60000.0)
    val (id, name, _, salary) = emp
    println("id=$id, name=$name, salary=$salary")

    // Map 순회
    val scores = mapOf("Alice" to 95, "Bob" to 87, "Charlie" to 92)
    for ((person, score) in scores) {
        println("$person: $score")
    }

    // 함수 반환값 구조 분해
    fun minMax(list: List<Int>) = list.min() to list.max()
    val numbers = listOf(3, 1, 4, 1, 5, 9, 2, 6)
    val (min, max) = minMax(numbers)
    println("min=$min, max=$max")
}

// ── 8. 널 안전성 ──────────────────────────────────────────────────
fun nullSafetyDemo() {
    println("\n=== 널 안전성 ===")

    var nonNull: String = "hello"
    var nullable: String? = null

    // nonNull = null           // 컴파일 에러
    nullable = "world"

    // 안전한 호출 ?.
    println(nullable?.length)   // 5
    nullable = null
    println(nullable?.length)   // null (예외 없음)

    // 엘비스 연산자 ?:
    val len = nullable?.length ?: -1
    println("len = $len")       // -1

    // 안전한 호출 체인
    data class City(val name: String)
    data class Address(val city: City?)
    data class User(val address: Address?)

    val user = User(Address(City("Seoul")))
    val noCity = User(Address(null))

    println(user.address?.city?.name)     // Seoul
    println(noCity.address?.city?.name)   // null

    // 널 아님 단언 !! — NPE 가능, 피하기 권장
    val str: String? = "Kotlin"
    println(str!!.length)       // 6, str이 null이면 NullPointerException

    // let으로 null 아닐 때만 실행
    nullable?.let { println("널이 아님: $it") }
    val nonNullStr: String? = "non-null"
    nonNullStr?.let { println("값: $it") }
}

// ── 9. 제네릭스 소개 ──────────────────────────────────────────────
class Box<T>(val value: T) {
    fun describe() = "Box<${value!!::class.simpleName}>: $value"
}

fun <T : Comparable<T>> clamp(value: T, min: T, max: T): T =
    if (value < min) min else if (value > max) max else value

fun genericsDemo() {
    println("\n=== 제네릭스 ===")

    val intBox = Box(42)
    val strBox = Box("hello")
    println(intBox.describe())
    println(strBox.describe())

    // 타입 상한 (upper bound)
    println(clamp(15, 0, 10))   // 10
    println(clamp(5, 0, 10))    // 5
    println(clamp(-3, 0, 10))   // 0

    // 타입 추론
    val list: List<Int> = listOf(1, 2, 3)
    val first: Int = list.first()
    println("first: $first")
}

fun main() {
    extensionDemo()
    namedAndDefaultArgsDemo()
    overloadDemo()
    whenDemo()
    enumDemo()
    dataClassDemo()
    destructuringDemo()
    nullSafetyDemo()
    genericsDemo()
}
