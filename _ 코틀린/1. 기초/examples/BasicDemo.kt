/**
 * 1장 — 기초
 *
 * 실행 방법:
 *   kotlinc BasicDemo.kt -include-runtime -d basic.jar
 *   java -jar basic.jar
 *
 * 주제:
 *   - var vs val
 *   - 숫자/문자/문자열/불리언/특수 타입
 *   - if식, while, in 키워드
 *   - 문(Statement) vs 식(Expression)
 */

// ── 1. var vs val ─────────────────────────────────────────────────
fun varValDemo() {
    println("=== var vs val ===")

    val immutable = 42          // 재할당 불가
    var mutable = "hello"       // 재할당 가능
    mutable = "world"

    // immutable = 100          // 컴파일 에러
    println("val: $immutable, var: $mutable")

    // val은 참조가 불변, 객체 내부는 변경 가능
    val list = mutableListOf(1, 2, 3)
    list.add(4)                 // OK — 참조는 그대로
    println("val list: $list")
}

// ── 2. 숫자 타입 ──────────────────────────────────────────────────
fun numberTypesDemo() {
    println("\n=== 숫자 타입 ===")

    val i: Int = 2_147_483_647          // 32비트 정수 최대값
    val l: Long = 9_223_372_036_854_775_807L
    val d: Double = 3.14159265358979    // 64비트 부동소수
    val f: Float = 3.14f                // 32비트 부동소수

    println("Int 최대: $i")
    println("Long 최대: $l")
    println("Double: $d")
    println("Float: $f")

    // 정수 나눗셈 — 소수점 버림
    println("7 / 2 = ${7 / 2}")        // 3
    println("7.0 / 2 = ${7.0 / 2}")    // 3.5

    // 비트 연산
    println("0b1010 and 0b1100 = ${0b1010 and 0b1100}")   // 8
    println("1 shl 4 = ${1 shl 4}")    // 16
}

// ── 3. 문자 & 문자열 타입 ─────────────────────────────────────────
fun stringDemo() {
    println("\n=== 문자 & 문자열 ===")

    val ch: Char = 'A'
    println("Char: $ch, 코드값: ${ch.code}")    // 65

    val name = "Kotlin"
    val version = 2.0

    // 문자열 템플릿
    println("$name $version 에 오신 것을 환영합니다!")
    println("이름 길이: ${name.length}")
    println("대문자: ${name.uppercase()}")

    // 로우 문자열 (이스케이프 불필요)
    val json = """
        {
            "language": "$name",
            "version": $version
        }
    """.trimIndent()
    println(json)

    // 문자열 비교: == 는 값 비교 (Java equals와 동일)
    val a = "hello"
    val b = "hel" + "lo"
    println("a == b: ${a == b}")        // true
    println("a === b: ${a === b}")      // 참조 비교 (false일 수도 있음)
}

// ── 4. 불리언 & 특수 타입 ─────────────────────────────────────────
fun specialTypesDemo() {
    println("\n=== 불리언 & 특수 타입 ===")

    val flag: Boolean = true
    println("flag && false = ${flag && false}")
    println("flag || false = ${flag || false}")
    println("!flag = ${!flag}")

    // Unit — 반환값 없음 (Java의 void)
    fun printHello(): Unit { println("Hello") }

    // Any — 모든 타입의 최상위 (Java Object)
    val any: Any = 42
    println("Any: $any (${any::class.simpleName})")

    // Nothing — 절대 반환하지 않음 (예외 던지기, 무한 루프)
    fun fail(msg: String): Nothing = throw IllegalStateException(msg)

    try { fail("의도적 실패") }
    catch (e: IllegalStateException) { println("Nothing 함수: ${e.message}") }
}

// ── 5. if 식 ──────────────────────────────────────────────────────
fun ifExpressionDemo() {
    println("\n=== if 식 ===")

    val x = 10

    // if는 문이 아니라 식 — 값을 반환
    val result = if (x > 5) "크다" else "작거나 같다"
    println("x=$x → $result")

    // 삼항 연산자 없음 — if 식이 대신
    val max = if (x > 7) x else 7
    println("max(x, 7) = $max")

    // 블록도 마지막 식이 값
    val grade = if (x >= 90) {
        println("우수")
        "A"
    } else if (x >= 70) {
        println("보통")
        "B"
    } else {
        println("미흡")
        "C"
    }
    println("grade = $grade")
}

// ── 6. while & in ─────────────────────────────────────────────────
fun loopAndInDemo() {
    println("\n=== while & in ===")

    // while
    var i = 0
    while (i < 3) { print("$i "); i++ }
    println()

    // do-while
    var j = 0
    do { print("$j "); j++ } while (j < 3)
    println()

    // for + range
    for (n in 1..5) print("$n ")
    println()

    // until — 끝 미포함
    for (n in 1 until 5) print("$n ")
    println()

    // downTo, step
    for (n in 10 downTo 1 step 3) print("$n ")
    println()

    // in — 범위 검사
    val score = 85
    println("85 in 80..89: ${score in 80..89}")     // true
    println("85 !in 90..100: ${score !in 90..100}") // true

    // 컬렉션 순회
    val fruits = listOf("apple", "banana", "cherry")
    for ((index, fruit) in fruits.withIndex()) {
        println("[$index] $fruit")
    }
}

// ── 7. 문(Statement) vs 식(Expression) ───────────────────────────
fun statementVsExpressionDemo() {
    println("\n=== 문 vs 식 ===")

    // Kotlin은 식(Expression) 중심 언어
    // if, when, try 모두 값을 반환하는 식

    // when 식
    val code = 404
    val message = when (code) {
        200  -> "OK"
        404  -> "Not Found"
        500  -> "Internal Server Error"
        else -> "Unknown"
    }
    println("$code → $message")

    // try 식
    val number = try {
        "123".toInt()
    } catch (e: NumberFormatException) {
        -1
    }
    println("try 식 결과: $number")

    // 문은 값을 반환하지 않음
    // val x = println("hi")  // println은 Unit 반환
    println("Kotlin 식 중심: if/when/try 모두 값 반환 가능")
}

fun main() {
    varValDemo()
    numberTypesDemo()
    stringDemo()
    specialTypesDemo()
    ifExpressionDemo()
    loopAndInDemo()
    statementVsExpressionDemo()
}
