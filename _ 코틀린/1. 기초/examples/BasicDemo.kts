#!/usr/bin/env kotlinc -script
/**
 * 1장 — 기초
 * 실행: kotlinc -script BasicDemo.kts
 */

// ── 1. var vs val ─────────────────────────────────────────────────
println("=== var vs val ===")

val immutable = 42
var mutable = "hello"
mutable = "world"

println("val: $immutable, var: $mutable")

val list = mutableListOf(1, 2, 3)
list.add(4)
println("val list: $list")

// ── 2. 숫자 타입 ──────────────────────────────────────────────────
println("\n=== 숫자 타입 ===")

val i: Int = 2_147_483_647
val l: Long = 9_223_372_036_854_775_807L
val d: Double = 3.14159265358979
val f: Float = 3.14f

println("Int 최대: $i")
println("Long 최대: $l")
println("Double: $d / Float: $f")
println("7 / 2 = ${7 / 2}, 7.0 / 2 = ${7.0 / 2}")
println("0b1010 and 0b1100 = ${0b1010 and 0b1100}")
println("1 shl 4 = ${1 shl 4}")

// ── 3. 문자 & 문자열 ──────────────────────────────────────────────
println("\n=== 문자 & 문자열 ===")

val ch: Char = 'A'
println("Char: $ch, 코드값: ${ch.code}")

val name = "Kotlin"
val version = 2.0
println("$name $version 에 오신 것을 환영합니다!")
println("이름 길이: ${name.length}, 대문자: ${name.uppercase()}")

val json = """
    {
        "language": "$name",
        "version": $version
    }
""".trimIndent()
println(json)

val a = "hello"
val b = "hel" + "lo"
println("a == b: ${a == b}, a === b: ${a === b}")

// ── 4. 불리언 & 특수 타입 ─────────────────────────────────────────
println("\n=== 불리언 & 특수 타입 ===")

val flag = true
println("flag && false = ${flag && false}")
println("flag || false = ${flag || false}")
println("!flag = ${!flag}")

val any: Any = 42
println("Any: $any (${any::class.simpleName})")

fun fail(msg: String): Nothing = throw IllegalStateException(msg)
try { fail("의도적 실패") }
catch (e: IllegalStateException) { println("Nothing 함수: ${e.message}") }

// ── 5. if 식 ──────────────────────────────────────────────────────
println("\n=== if 식 ===")

val x = 10
val result = if (x > 5) "크다" else "작거나 같다"
println("x=$x → $result")

val grade = if (x >= 90) "A" else if (x >= 70) "B" else "C"
println("grade = $grade")

// ── 6. while & in ─────────────────────────────────────────────────
println("\n=== while & in ===")

var idx = 0
while (idx < 3) { print("$idx "); idx++ }
println()

for (n in 1..5) print("$n ")
println()

for (n in 1 until 5) print("$n ")
println()

for (n in 10 downTo 1 step 3) print("$n ")
println()

val score = 85
println("85 in 80..89: ${score in 80..89}")

val fruits = listOf("apple", "banana", "cherry")
for ((index, fruit) in fruits.withIndex()) println("[$index] $fruit")

// ── 7. 문 vs 식 ───────────────────────────────────────────────────
println("\n=== 문 vs 식 ===")

val code = 404
val message = when (code) {
    200  -> "OK"
    404  -> "Not Found"
    500  -> "Internal Server Error"
    else -> "Unknown"
}
println("$code → $message")

val number = try { "123".toInt() } catch (e: NumberFormatException) { -1 }
println("try 식 결과: $number")
println("Kotlin 식 중심: if/when/try 모두 값 반환 가능")
