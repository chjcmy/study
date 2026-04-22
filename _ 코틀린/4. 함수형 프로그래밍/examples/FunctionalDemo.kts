#!/usr/bin/env kotlinc -script
/**
 * 4장 — 함수형 프로그래밍
 * 실행: kotlinc -script FunctionalDemo.kts
 */

// ── 1. 람다 ───────────────────────────────────────────────────────
println("=== 람다 ===")

val double: (Int) -> Int = { x -> x * 2 }
val add: (Int, Int) -> Int = { a, b -> a + b }
val isEven: (Int) -> Boolean = { it % 2 == 0 }

println("double(5) = ${double(5)}")
println("add(3, 4) = ${add(3, 4)}")
println("isEven(4) = ${isEven(4)}")

var count = 0
val increment = { count++ }
repeat(3) { increment() }
println("count = $count")

val operations = mapOf<String, (Int, Int) -> Int>(
    "add" to { a, b -> a + b },
    "mul" to { a, b -> a * b }
)
println("mul(3, 7) = ${operations["mul"]!!(3, 7)}")

// ── 2. 컬렉션 연산 ────────────────────────────────────────────────
data class Employee(val name: String, val dept: String, val salary: Double)

val employees = listOf(
    Employee("Alice", "Engineering", 90000.0),
    Employee("Bob",   "Engineering", 75000.0),
    Employee("Carol", "Marketing",   65000.0),
    Employee("Dave",  "Marketing",   70000.0),
    Employee("Eve",   "HR",          60000.0),
)

println("\n=== 컬렉션 연산 ===")
println("이름: ${employees.map { it.name }}")
println("75k+: ${employees.filter { it.salary >= 75000 }.map { it.name }}")
println("90k+ 있음: ${employees.any { it.salary >= 90000 }}")
println("전원 50k+: ${employees.all { it.salary >= 50000 }}")
println("평균 급여: ${"%.0f".format(employees.map { it.salary }.average())}")

employees.groupBy { it.dept }.forEach { (dept, emps) ->
    println("$dept: ${emps.map { it.name }}")
}

println("급여 순: ${employees.sortedByDescending { it.salary }.map { "${it.name}(${it.salary.toInt()})" }}")

val (rich, others) = employees.partition { it.salary >= 75000 }
println("rich: ${rich.map { it.name }}, others: ${others.map { it.name }}")

// ── 3. 멤버 참조 ──────────────────────────────────────────────────
fun isLong(s: String) = s.length > 5
fun toUpper(s: String) = s.uppercase()

println("\n=== 멤버 참조 ===")
val words = listOf("hello", "Kotlin", "world", "programming")
println(words.filter(::isLong))
println(words.map(::toUpper))

data class PersonRef(val name: String, val age: Int)
val people = listOf(PersonRef("Alice", 30), PersonRef("Bob", 25))
println("나이: ${people.map(PersonRef::age)}")
println("정렬: ${people.sortedBy(PersonRef::age)}")

// ── 4. 고차 함수 ──────────────────────────────────────────────────
fun applyTwice(x: Int, f: (Int) -> Int) = f(f(x))
fun makeMultiplier(factor: Int): (Int) -> Int = { it * factor }

inline fun <T> measureTime(label: String, block: () -> T): T {
    val start = System.nanoTime()
    val result = block()
    println("[$label] ${"%.3f".format((System.nanoTime() - start) / 1_000_000.0)}ms")
    return result
}

println("\n=== 고차 함수 ===")
println("applyTwice(3, +10) = ${applyTwice(3) { it + 10 }}")

val triple = makeMultiplier(3)
println("triple(7) = ${triple(7)}")

val sum = measureTime("sum") { (1..1_000_000).sum() }
println("합: $sum")

// ── 5. 시퀀스 ─────────────────────────────────────────────────────
println("\n=== 시퀀스 ===")

val seqResult = (1..10).asSequence()
    .filter { it % 2 == 0 }
    .map { it * it }
    .take(3)
    .toList()
println("짝수 제곱 3개: $seqResult")

val fibonacci = sequence {
    var a = 0; var b = 1
    while (true) { yield(a); val next = a + b; a = b; b = next }
}
println("피보나치 10개: ${fibonacci.take(10).toList()}")

val powers = generateSequence(1) { it * 2 }
println("2의 거듭제곱 (1024 이하): ${powers.takeWhile { it <= 1024 }.toList()}")

// ── 6. 지역 함수 ──────────────────────────────────────────────────
fun processOrder(items: List<String>, discount: Double): Double {
    fun validateItem(item: String) {
        require(item.isNotBlank()) { "빈 아이템 불가" }
    }
    fun calculatePrice(item: String): Double { validateItem(item); return item.length * 1000.0 }
    return items.sumOf { calculatePrice(it) } * (1 - discount)
}

println("\n=== 지역 함수 ===")
println("주문 총액 (10% 할인): ${processOrder(listOf("laptop", "mouse", "keyboard"), 0.1).toInt()}원")

// ── 7. fold & reduce ──────────────────────────────────────────────
println("\n=== fold & reduce ===")

val numbers = listOf(1, 2, 3, 4, 5)
println("reduce 합: ${numbers.reduce { acc, n -> acc + n }}")
println("fold 합: ${numbers.fold(0) { acc, n -> acc + n }}, 곱: ${numbers.fold(1) { acc, n -> acc * n }}")

val sentence = listOf("Hello", "World", "Kotlin").fold("") { acc, w -> if (acc.isEmpty()) w else "$acc $w" }
println("fold 문자열: $sentence")
println("누적 합: ${numbers.runningFold(0) { acc, n -> acc + n }}")

// ── 8. 재귀 & tailrec ────────────────────────────────────────────
tailrec fun factorial(n: Long, acc: Long = 1L): Long = if (n <= 1) acc else factorial(n - 1, acc * n)
tailrec fun fibonacci(n: Int, a: Long = 0, b: Long = 1): Long = if (n == 0) a else fibonacci(n - 1, b, a + b)
tailrec fun optimizedSum(n: Int, acc: Long = 0): Long = if (n == 0) acc else optimizedSum(n - 1, acc + n)

println("\n=== 재귀 & tailrec ===")
println("factorial(20) = ${factorial(20)}")
println("fibonacci(50) = ${fibonacci(50)}")
println("optimizedSum(100000) = ${optimizedSum(100000)}")
