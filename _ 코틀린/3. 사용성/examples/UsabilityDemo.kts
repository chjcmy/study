#!/usr/bin/env kotlinc -script
/**
 * 3장 — 사용성
 * 실행: kotlinc -script UsabilityDemo.kts
 */

// ── 1. 확장 함수 & 프로퍼티 ──────────────────────────────────────
fun String.isPalindrome() = this == this.reversed()
fun String.wordCount() = if (isBlank()) 0 else trim().split("\\s+".toRegex()).size
val String.lastChar: Char get() = this[length - 1]

println("=== 확장 함수 & 프로퍼티 ===")
println("\"racecar\".isPalindrome() = ${"racecar".isPalindrome()}")
println("\"hello\".isPalindrome()   = ${"hello".isPalindrome()}")
println("\"hello world\".wordCount() = ${"hello world".wordCount()}")
println("\"Kotlin\".lastChar = ${"Kotlin".lastChar}")

// ── 2. 이름 붙은 인자 & 디폴트 인자 ─────────────────────────────
fun createUser(name: String, age: Int = 0, email: String = "", isAdmin: Boolean = false) =
    "User(name=$name, age=$age, email=$email, admin=$isAdmin)"

println("\n=== 이름 붙은 인자 & 디폴트 인자 ===")
println(createUser("Alice"))
println(createUser("Bob", age = 30))
println(createUser("Charlie", isAdmin = true, age = 25))

// ── 3. 오버로딩 ───────────────────────────────────────────────────
fun describe(value: Int)     = "Int: $value"
fun describe(value: Double)  = "Double: $value"
fun describe(value: String)  = "String: \"$value\""
fun describe(value: Boolean) = "Boolean: $value"

println("\n=== 오버로딩 ===")
println(describe(42)); println(describe(3.14))
println(describe("hello")); println(describe(true))

// ── 4. when 식 ────────────────────────────────────────────────────
sealed class Shape
data class Circle(val radius: Double) : Shape()
data class Rectangle(val w: Double, val h: Double) : Shape()
data class Triangle(val base: Double, val height: Double) : Shape()

fun area(shape: Shape) = when (shape) {
    is Circle    -> Math.PI * shape.radius * shape.radius
    is Rectangle -> shape.w * shape.h
    is Triangle  -> 0.5 * shape.base * shape.height
}

println("\n=== when 식 ===")
val x = 3
println("$x → ${when (x) { 1 -> "one"; 2, 3 -> "two or three"; in 4..10 -> "four~ten"; else -> "other" }}")

listOf(Circle(5.0), Rectangle(4.0, 6.0), Triangle(3.0, 8.0)).forEach { s ->
    println("${s::class.simpleName} 넓이: ${"%.2f".format(area(s))}")
}

val score = 75
println("$score점 → ${when { score >= 90 -> "A"; score >= 80 -> "B"; score >= 70 -> "C"; else -> "F" }}")

// ── 5. 이넘 ───────────────────────────────────────────────────────
enum class Direction(val degrees: Int) {
    NORTH(0), EAST(90), SOUTH(180), WEST(270);
    fun opposite() = when (this) { NORTH -> SOUTH; EAST -> WEST; SOUTH -> NORTH; WEST -> EAST }
}

println("\n=== 이넘 ===")
Direction.entries.forEach { println("${it.name}(${it.degrees}°) ↔ ${it.opposite().name}") }

// ── 6. 데이터 클래스 ──────────────────────────────────────────────
data class Point(val x: Int, val y: Int)

println("\n=== 데이터 클래스 ===")
val p1 = Point(3, 4); val p2 = Point(3, 4); val p3 = p1.copy(y = 10)
println("p1 == p2: ${p1 == p2}, p1 === p2: ${p1 === p2}")
println("p3: $p3")
println(mapOf(p1 to "origin")[Point(3, 4)])

// ── 7. 구조 분해 선언 ─────────────────────────────────────────────
println("\n=== 구조 분해 선언 ===")
val (px, py) = Point(10, 20)
println("x=$px, y=$py")

val scores = mapOf("Alice" to 95, "Bob" to 87)
for ((person, sc) in scores) println("$person: $sc")

fun minMax(list: List<Int>) = list.min() to list.max()
val (min, max) = minMax(listOf(3, 1, 4, 1, 5, 9))
println("min=$min, max=$max")

// ── 8. 널 안전성 ──────────────────────────────────────────────────
println("\n=== 널 안전성 ===")

var nullable: String? = "world"
println(nullable?.length)           // 5
nullable = null
println(nullable?.length)           // null
println("len = ${nullable?.length ?: -1}")

data class City(val name: String)
data class Address(val city: City?)
data class User(val address: Address?)

println(User(Address(City("Seoul"))).address?.city?.name)   // Seoul
println(User(Address(null)).address?.city?.name)            // null

val str: String? = "Kotlin"
println(str!!.length)

// ── 9. 제네릭스 소개 ──────────────────────────────────────────────
class Box<T>(val value: T) { fun describe() = "Box<${value!!::class.simpleName}>: $value" }
fun <T : Comparable<T>> clamp(v: T, min: T, max: T): T = if (v < min) min else if (v > max) max else v

println("\n=== 제네릭스 ===")
println(Box(42).describe()); println(Box("hello").describe())
println(clamp(15, 0, 10)); println(clamp(5, 0, 10)); println(clamp(-3, 0, 10))
