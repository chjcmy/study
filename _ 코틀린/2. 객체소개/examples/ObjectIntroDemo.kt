/**
 * 2장 — 객체 소개
 *
 * 실행 방법:
 *   kotlinc ObjectIntroDemo.kt -include-runtime -d object.jar
 *   java -jar object.jar
 *
 * 주제:
 *   - 클래스 만들기, 프로퍼티, 생성자
 *   - 가시성 제한자, 패키지
 *   - 리스트, 가변 인자, 집합, 맵
 *   - 프로퍼티 접근자
 *   - 예외 기초
 */

// ── 1. 클래스 & 프로퍼티 ──────────────────────────────────────────
class Person(
    val name: String,           // val → getter만 생성
    var age: Int                // var → getter + setter 생성
) {
    // 커스텀 프로퍼티
    val isAdult: Boolean
        get() = age >= 18

    override fun toString() = "Person(name=$name, age=$age)"
}

fun classDemo() {
    println("=== 클래스 & 프로퍼티 ===")

    val p = Person("Alice", 25)
    println(p)
    println("성인: ${p.isAdult}")

    p.age = 26                  // var이므로 수정 가능
    // p.name = "Bob"           // val이므로 컴파일 에러
    println("나이 변경 후: $p")
}

// ── 2. 생성자 ─────────────────────────────────────────────────────
class Car(val model: String, val year: Int = 2024) {

    var mileage: Int = 0
        private set                 // 외부에서 직접 수정 불가

    // init 블록 — 주 생성자 실행 직후 호출
    init {
        println("Car 생성: $model ($year)")
    }

    fun drive(km: Int) {
        mileage += km
    }

    override fun toString() = "$model (${year}년형, ${mileage}km)"
}

fun constructorDemo() {
    println("\n=== 생성자 ===")

    val car1 = Car("Tesla Model 3")        // 기본값 사용
    val car2 = Car("BMW 5", 2023)
    car1.drive(100)
    println(car1)
    println(car2)
    // car1.mileage = 999                  // private set — 컴파일 에러
}

// ── 3. 가시성 제한자 ──────────────────────────────────────────────
class BankAccount(initialBalance: Double) {

    private var balance: Double = initialBalance    // 클래스 내부만
    internal var accountNumber: String = "001"      // 같은 모듈
    // protected: 상속 클래스까지
    // public (기본값): 어디서나

    fun deposit(amount: Double) {
        require(amount > 0) { "입금액은 양수여야 합니다" }
        balance += amount
    }

    fun withdraw(amount: Double): Boolean {
        if (amount > balance) return false
        balance -= amount
        return true
    }

    fun getBalance() = balance
}

fun visibilityDemo() {
    println("\n=== 가시성 제한자 ===")

    val account = BankAccount(1000.0)
    account.deposit(500.0)
    println("잔액: ${account.getBalance()}")     // 1500.0

    val success = account.withdraw(2000.0)
    println("2000 출금 성공: $success")          // false

    account.withdraw(500.0)
    println("500 출금 후 잔액: ${account.getBalance()}")  // 1000.0
}

// ── 4. 프로퍼티 접근자 ────────────────────────────────────────────
class Temperature(celsius: Double) {

    var celsius: Double = celsius
        set(value) {
            require(value >= -273.15) { "절대 영도 이하 불가" }
            field = value           // field — backing field 참조
        }

    val fahrenheit: Double
        get() = celsius * 9.0 / 5.0 + 32

    val kelvin: Double
        get() = celsius + 273.15
}

fun propertyAccessorDemo() {
    println("\n=== 프로퍼티 접근자 ===")

    val temp = Temperature(100.0)
    println("섭씨: ${temp.celsius}°C")
    println("화씨: ${temp.fahrenheit}°F")
    println("켈빈: ${temp.kelvin}K")

    temp.celsius = 0.0
    println("0°C → ${temp.fahrenheit}°F")

    try {
        temp.celsius = -300.0       // setter에서 예외
    } catch (e: IllegalArgumentException) {
        println("검증 실패: ${e.message}")
    }
}

// ── 5. 리스트, 집합, 맵 ───────────────────────────────────────────
fun collectionsDemo() {
    println("\n=== 컬렉션 ===")

    // List — 순서 있음, 중복 허용
    val immutableList = listOf("a", "b", "c")
    val mutableList = mutableListOf(1, 2, 3)
    mutableList.add(4)
    mutableList[0] = 10
    println("List: $mutableList")

    // Set — 순서 없음(LinkedHashSet은 순서 유지), 중복 불허
    val set = mutableSetOf(1, 2, 3, 2, 1)
    println("Set: $set")                    // [1, 2, 3]
    println("3 in set: ${3 in set}")

    // Map — 키-값 쌍
    val map = mutableMapOf("one" to 1, "two" to 2, "three" to 3)
    map["four"] = 4
    println("Map: $map")
    println("Map['two']: ${map["two"]}")
    println("Map['five']: ${map["five"]}")  // null (없는 키)

    // 불변 뷰
    val readOnly: List<Int> = mutableList
    println("readOnly: $readOnly")
}

// ── 6. 가변 인자 (vararg) ─────────────────────────────────────────
fun sum(vararg numbers: Int): Int = numbers.sum()

fun printAll(separator: String, vararg items: String) {
    println(items.joinToString(separator))
}

fun varargDemo() {
    println("\n=== 가변 인자 ===")

    println("합: ${sum(1, 2, 3, 4, 5)}")

    val words = arrayOf("Hello", "World", "Kotlin")
    printAll(" | ", *words)     // * — 스프레드 연산자

    printAll(", ", "a", "b", "c")
}

// ── 7. 예외 기초 ──────────────────────────────────────────────────
fun exceptionDemo() {
    println("\n=== 예외 ===")

    // try-catch-finally
    fun divide(a: Int, b: Int): Int {
        return try {
            a / b
        } catch (e: ArithmeticException) {
            println("나눗셈 오류: ${e.message}")
            -1
        } finally {
            println("finally 항상 실행")
        }
    }

    println("10 / 2 = ${divide(10, 2)}")
    println("10 / 0 = ${divide(10, 0)}")

    // 커스텀 예외
    class InsufficientFundsException(amount: Double)
        : Exception("잔액 부족: ${amount}원 필요")

    fun checkBalance(balance: Double, amount: Double) {
        if (balance < amount) throw InsufficientFundsException(amount)
    }

    try {
        checkBalance(100.0, 500.0)
    } catch (e: Exception) {
        println("예외: ${e.message}")
    }

    // Kotlin은 checked exception 없음 — throws 선언 불필요
    println("Kotlin: checked exception 없음, 모두 unchecked")
}

fun main() {
    classDemo()
    constructorDemo()
    visibilityDemo()
    propertyAccessorDemo()
    collectionsDemo()
    varargDemo()
    exceptionDemo()
}
