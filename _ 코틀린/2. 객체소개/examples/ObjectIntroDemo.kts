#!/usr/bin/env kotlinc -script
/**
 * 2장 — 객체 소개
 * 실행: kotlinc -script ObjectIntroDemo.kts
 */

// ── 1. 클래스 & 프로퍼티 ──────────────────────────────────────────
class Person(val name: String, var age: Int) {
    val isAdult: Boolean get() = age >= 18
    override fun toString() = "Person(name=$name, age=$age)"
}

println("=== 클래스 & 프로퍼티 ===")
val p = Person("Alice", 25)
println(p)
println("성인: ${p.isAdult}")
p.age = 26
println("나이 변경 후: $p")

// ── 2. 생성자 ─────────────────────────────────────────────────────
class Car(val model: String, val year: Int = 2024) {
    var mileage: Int = 0
        private set
    init { println("Car 생성: $model ($year)") }
    fun drive(km: Int) { mileage += km }
    override fun toString() = "$model (${year}년형, ${mileage}km)"
}

println("\n=== 생성자 ===")
val car1 = Car("Tesla Model 3")
val car2 = Car("BMW 5", 2023)
car1.drive(100)
println(car1)
println(car2)

// ── 3. 가시성 제한자 ──────────────────────────────────────────────
class BankAccount(initialBalance: Double) {
    private var balance: Double = initialBalance
    fun deposit(amount: Double) { require(amount > 0); balance += amount }
    fun withdraw(amount: Double): Boolean {
        if (amount > balance) return false
        balance -= amount; return true
    }
    fun getBalance() = balance
}

println("\n=== 가시성 제한자 ===")
val account = BankAccount(1000.0)
account.deposit(500.0)
println("잔액: ${account.getBalance()}")
println("2000 출금 성공: ${account.withdraw(2000.0)}")
account.withdraw(500.0)
println("500 출금 후 잔액: ${account.getBalance()}")

// ── 4. 프로퍼티 접근자 ────────────────────────────────────────────
class Temperature(celsius: Double) {
    var celsius: Double = celsius
        set(value) { require(value >= -273.15) { "절대 영도 이하 불가" }; field = value }
    val fahrenheit: Double get() = celsius * 9.0 / 5.0 + 32
    val kelvin: Double get() = celsius + 273.15
}

println("\n=== 프로퍼티 접근자 ===")
val temp = Temperature(100.0)
println("섭씨: ${temp.celsius}°C → 화씨: ${temp.fahrenheit}°F → 켈빈: ${temp.kelvin}K")
temp.celsius = 0.0
println("0°C → ${temp.fahrenheit}°F")
try { temp.celsius = -300.0 }
catch (e: IllegalArgumentException) { println("검증 실패: ${e.message}") }

// ── 5. 컬렉션 ─────────────────────────────────────────────────────
println("\n=== 컬렉션 ===")

val mutableList = mutableListOf(1, 2, 3)
mutableList.add(4); mutableList[0] = 10
println("List: $mutableList")

val set = mutableSetOf(1, 2, 3, 2, 1)
println("Set: $set")

val map = mutableMapOf("one" to 1, "two" to 2)
map["three"] = 3
println("Map: $map, map['two']: ${map["two"]}")

// ── 6. 가변 인자 ──────────────────────────────────────────────────
fun sum(vararg numbers: Int) = numbers.sum()
fun printAll(sep: String, vararg items: String) = println(items.joinToString(sep))

println("\n=== 가변 인자 ===")
println("합: ${sum(1, 2, 3, 4, 5)}")
val words = arrayOf("Hello", "World", "Kotlin")
printAll(" | ", *words)
printAll(", ", "a", "b", "c")

// ── 7. 예외 ───────────────────────────────────────────────────────
println("\n=== 예외 ===")

fun divide(a: Int, b: Int) = try {
    a / b
} catch (e: ArithmeticException) {
    println("나눗셈 오류: ${e.message}"); -1
} finally {
    println("finally 실행")
}

println("10 / 2 = ${divide(10, 2)}")
println("10 / 0 = ${divide(10, 0)}")
println("Kotlin: checked exception 없음, 모두 unchecked")
