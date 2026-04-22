/**
 * 4장 — 함수형 프로그래밍
 *
 * 실행 방법:
 *   kotlinc FunctionalDemo.kt -include-runtime -d functional.jar
 *   java -jar functional.jar
 *
 * 주제:
 *   - 람다 & 익명 함수
 *   - 컬렉션 연산 (map, filter, reduce, ...)
 *   - 멤버 참조
 *   - 고차 함수
 *   - 시퀀스 (지연 평가)
 *   - 지역 함수
 *   - 리스트 접기 (fold, reduce)
 *   - 재귀 & tailrec
 */

// ── 1. 람다 & 익명 함수 ───────────────────────────────────────────
fun lambdaDemo() {
    println("=== 람다 ===")

    // 람다: { 파라미터 -> 본문 }
    val double: (Int) -> Int = { x -> x * 2 }
    val add: (Int, Int) -> Int = { a, b -> a + b }

    println("double(5) = ${double(5)}")
    println("add(3, 4) = ${add(3, 4)}")

    // it — 단일 파라미터 기본 이름
    val isEven: (Int) -> Boolean = { it % 2 == 0 }
    println("isEven(4) = ${isEven(4)}")

    // 클로저 — 외부 변수 캡처
    var count = 0
    val increment = { count++ }
    repeat(3) { increment() }
    println("count = $count")       // 3

    // 익명 함수 (return 동작이 람다와 다름)
    val triple = fun(x: Int): Int { return x * 3 }
    println("triple(4) = ${triple(4)}")

    // 함수 타입 저장 & 호출
    val operations: Map<String, (Int, Int) -> Int> = mapOf(
        "add" to { a, b -> a + b },
        "mul" to { a, b -> a * b },
        "sub" to { a, b -> a - b }
    )
    println("mul(3, 7) = ${operations["mul"]!!(3, 7)}")
}

// ── 2. 컬렉션 연산 ────────────────────────────────────────────────
data class Employee(val name: String, val dept: String, val salary: Double)

fun collectionOpsDemo() {
    println("\n=== 컬렉션 연산 ===")

    val employees = listOf(
        Employee("Alice", "Engineering", 90000.0),
        Employee("Bob",   "Engineering", 75000.0),
        Employee("Carol", "Marketing",   65000.0),
        Employee("Dave",  "Marketing",   70000.0),
        Employee("Eve",   "HR",          60000.0),
    )

    // map — 변환
    val names = employees.map { it.name }
    println("이름: $names")

    // filter — 필터링
    val highPaid = employees.filter { it.salary >= 75000 }
    println("75k+: ${highPaid.map { it.name }}")

    // map + filter 체이닝
    val engSalaries = employees
        .filter { it.dept == "Engineering" }
        .map { it.salary }
    println("엔지니어 급여: $engSalaries")

    // any, all, none
    println("90k+ 있음: ${employees.any { it.salary >= 90000 }}")
    println("전원 50k+: ${employees.all { it.salary >= 50000 }}")
    println("100k+ 없음: ${employees.none { it.salary >= 100000 }}")

    // count, sum, average
    println("엔지니어 수: ${employees.count { it.dept == "Engineering" }}")
    println("평균 급여: ${"%.0f".format(employees.map { it.salary }.average())}")

    // groupBy
    val byDept = employees.groupBy { it.dept }
    byDept.forEach { (dept, emps) ->
        println("$dept: ${emps.map { it.name }}")
    }

    // sortedBy, sortedByDescending
    val sorted = employees.sortedByDescending { it.salary }
    println("급여 순: ${sorted.map { "${it.name}(${it.salary.toInt()})" }}")

    // flatMap
    val departments = listOf(
        listOf("Alice", "Bob"),
        listOf("Carol"),
        listOf("Dave", "Eve")
    )
    println("flatMap: ${departments.flatMap { it }}")

    // zip
    val keys = listOf("a", "b", "c")
    val vals = listOf(1, 2, 3)
    println("zip: ${keys.zip(vals)}")

    // partition — 조건 T/F로 분리
    val (rich, others) = employees.partition { it.salary >= 75000 }
    println("rich: ${rich.map { it.name }}, others: ${others.map { it.name }}")
}

// ── 3. 멤버 참조 ──────────────────────────────────────────────────
fun isLong(s: String) = s.length > 5
fun toUpperCase(s: String) = s.uppercase()

fun memberReferenceDemo() {
    println("\n=== 멤버 참조 ===")

    val words = listOf("hello", "Kotlin", "world", "programming")

    // 최상위 함수 참조
    println(words.filter(::isLong))
    println(words.map(::toUpperCase))

    // 멤버 함수 참조 (바운드)
    val str = "  kotlin  "
    val trimAndUpper: () -> String = str::trim
    println(trimAndUpper())

    // 생성자 참조
    data class Point(val x: Int, val y: Int)
    val pairs = listOf(1 to 2, 3 to 4, 5 to 6)
    val points = pairs.map { (x, y) -> Point(x, y) }
    println("Points: $points")

    // 프로퍼티 참조
    data class Person(val name: String, val age: Int)
    val people = listOf(Person("Alice", 30), Person("Bob", 25))
    println("나이: ${people.map(Person::age)}")
    println("정렬: ${people.sortedBy(Person::age)}")
}

// ── 4. 고차 함수 ──────────────────────────────────────────────────
// 함수를 인자로 받거나 반환하는 함수
fun <T, R> transform(list: List<T>, transformer: (T) -> R): List<R> =
    list.map(transformer)

fun applyTwice(x: Int, f: (Int) -> Int): Int = f(f(x))

fun makeMultiplier(factor: Int): (Int) -> Int = { it * factor }

inline fun <T> measureTime(label: String, block: () -> T): T {
    val start = System.nanoTime()
    val result = block()
    val ms = (System.nanoTime() - start) / 1_000_000.0
    println("[$label] ${"%.3f".format(ms)}ms")
    return result
}

fun higherOrderFunctionDemo() {
    println("\n=== 고차 함수 ===")

    // 함수를 인자로
    val numbers = listOf(1, 2, 3, 4, 5)
    println(transform(numbers) { it * it })     // 제곱

    println("applyTwice(3, +10) = ${applyTwice(3) { it + 10 }}")   // 23

    // 함수를 반환
    val triple = makeMultiplier(3)
    val quintuple = makeMultiplier(5)
    println("triple(7) = ${triple(7)}")
    println("quintuple(4) = ${quintuple(4)}")

    // inline 함수 — 람다 오버헤드 제거
    val sum = measureTime("sum") {
        (1..1_000_000).sum()
    }
    println("합: $sum")

    // 함수 합성
    val addOne: (Int) -> Int = { it + 1 }
    val double: (Int) -> Int = { it * 2 }
    val addOneThenDouble: (Int) -> Int = { double(addOne(it)) }
    println("addOneThenDouble(3) = ${addOneThenDouble(3)}")     // 8
}

// ── 5. 시퀀스 (지연 평가) ─────────────────────────────────────────
fun sequenceDemo() {
    println("\n=== 시퀀스 ===")

    // 즉시 평가 (List): 단계마다 새 컬렉션 생성
    val listResult = (1..10)
        .filter { it % 2 == 0 }
        .map { it * it }
        .take(3)
    println("List (즉시 평가): $listResult")

    // 지연 평가 (Sequence): 원소 하나씩 파이프라인 통과
    val seqResult = (1..10).asSequence()
        .filter { println("filter $it"); it % 2 == 0 }
        .map { println("map $it"); it * it }
        .take(3)
        .toList()
    println("Sequence (지연 평가): $seqResult")

    // 무한 시퀀스
    val fibonacci = sequence {
        var a = 0; var b = 1
        while (true) {
            yield(a)
            val next = a + b; a = b; b = next
        }
    }
    println("피보나치 10개: ${fibonacci.take(10).toList()}")

    // generateSequence
    val powers = generateSequence(1) { it * 2 }
    println("2의 거듭제곱 (1024 이하): ${powers.takeWhile { it <= 1024 }.toList()}")
}

// ── 6. 지역 함수 ──────────────────────────────────────────────────
fun processOrder(items: List<String>, discount: Double): Double {
    // 지역 함수 — 외부 변수 접근 가능, 재사용 없는 내부 로직 캡슐화
    fun validateItem(item: String) {
        require(item.isNotBlank()) { "빈 아이템 불가" }
        require(item.length <= 50) { "아이템 이름 너무 김: $item" }
    }

    fun calculatePrice(item: String): Double {
        validateItem(item)
        return item.length * 1000.0     // 길이당 가격 (예시)
    }

    val total = items.sumOf { calculatePrice(it) }
    return total * (1 - discount)
}

fun localFunctionDemo() {
    println("\n=== 지역 함수 ===")

    val order = listOf("laptop", "mouse", "keyboard")
    val total = processOrder(order, 0.1)
    println("주문 총액 (10% 할인): ${total.toInt()}원")
}

// ── 7. 리스트 접기 (fold & reduce) ───────────────────────────────
fun foldReduceDemo() {
    println("\n=== fold & reduce ===")

    val numbers = listOf(1, 2, 3, 4, 5)

    // reduce — 초기값 없음, 첫 원소가 시작값
    val sum = numbers.reduce { acc, n -> acc + n }
    println("reduce 합: $sum")

    // fold — 초기값 제공
    val sumWithFold = numbers.fold(0) { acc, n -> acc + n }
    val product = numbers.fold(1) { acc, n -> acc * n }
    println("fold 합: $sumWithFold, 곱: $product")

    // 문자열 누적
    val words = listOf("Hello", "World", "Kotlin")
    val sentence = words.fold("") { acc, word ->
        if (acc.isEmpty()) word else "$acc $word"
    }
    println("fold 문자열: $sentence")

    // foldRight — 오른쪽에서 접기
    val reversed = numbers.foldRight("") { n, acc -> "$acc$n" }
    println("foldRight: $reversed")

    // runningFold — 중간 결과 포함
    val cumulative = numbers.runningFold(0) { acc, n -> acc + n }
    println("누적 합: $cumulative")    // [0, 1, 3, 6, 10, 15]
}

// ── 8. 재귀 & tailrec ────────────────────────────────────────────
// tailrec — 꼬리 재귀 최적화 (스택 오버플로 방지)
tailrec fun factorial(n: Long, acc: Long = 1L): Long =
    if (n <= 1) acc else factorial(n - 1, acc * n)

tailrec fun fibonacci(n: Int, a: Long = 0, b: Long = 1): Long =
    if (n == 0) a else fibonacci(n - 1, b, a + b)

fun recursionDemo() {
    println("\n=== 재귀 & tailrec ===")

    println("factorial(10) = ${factorial(10)}")
    println("factorial(20) = ${factorial(20)}")

    println("fibonacci(10) = ${fibonacci(10)}")
    println("fibonacci(50) = ${fibonacci(50)}")

    // tailrec 없는 재귀 — 깊으면 StackOverflowError
    fun naiveSum(n: Int): Long = if (n == 0) 0L else n + naiveSum(n - 1)
    println("naiveSum(1000) = ${naiveSum(1000)}")

    // tailrec 최적화 — 루프로 변환됨
    tailrec fun optimizedSum(n: Int, acc: Long = 0): Long =
        if (n == 0) acc else optimizedSum(n - 1, acc + n)
    println("optimizedSum(100000) = ${optimizedSum(100000)}")
}

fun main() {
    lambdaDemo()
    collectionOpsDemo()
    memberReferenceDemo()
    higherOrderFunctionDemo()
    sequenceDemo()
    localFunctionDemo()
    foldReduceDemo()
    recursionDemo()
}
