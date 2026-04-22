/**
 * 6장 — 실패 방지하기
 *
 * 실행 방법:
 *   kotlinc ErrorHandlingDemo.kt -include-runtime -d error.jar
 *   java -jar error.jar
 *
 * 주제:
 *   - 예외 처리 전략
 *   - 검사 명령 (require, check, assert)
 *   - Nothing 타입
 *   - 자원 해제 (use, AutoCloseable)
 *   - 로깅
 *   - 단위 테스트 원칙
 */

import java.io.Closeable
import java.io.File

// ── 1. 예외 처리 전략 ─────────────────────────────────────────────
// Kotlin 철학: 예외는 복구 불가 상황에만, 로직은 Result/sealed class로
sealed class ApiResult<out T> {
    data class Ok<T>(val value: T) : ApiResult<T>()
    data class Err(val message: String, val code: Int = 0) : ApiResult<Nothing>()
}

fun fetchUser(id: Int): ApiResult<String> = when {
    id <= 0  -> ApiResult.Err("잘못된 ID", 400)
    id > 100 -> ApiResult.Err("사용자 없음", 404)
    else     -> ApiResult.Ok("User#$id")
}

fun exceptionStrategyDemo() {
    println("=== 예외 처리 전략 ===")

    // 1. try-catch — 복구 가능한 외부 실패
    val result = try {
        Integer.parseInt("not-a-number")
    } catch (e: NumberFormatException) {
        println("파싱 실패: ${e.message}")
        -1
    }
    println("result = $result")

    // 2. sealed class — 예상 가능한 성공/실패 (예외보다 명시적)
    listOf(5, -1, 200).forEach { id ->
        when (val r = fetchUser(id)) {
            is ApiResult.Ok  -> println("성공: ${r.value}")
            is ApiResult.Err -> println("오류 [${r.code}]: ${r.message}")
        }
    }

    // 3. 다중 catch
    fun parse(input: String): Int {
        return try {
            input.trim().toInt()
        } catch (e: NumberFormatException) {
            println("형식 오류: $input")
            throw IllegalArgumentException("숫자 아님: $input", e)
        } catch (e: Exception) {
            println("알 수 없는 오류: ${e.message}")
            throw e
        }
    }

    try { parse("abc") }
    catch (e: IllegalArgumentException) { println("상위 처리: ${e.message}") }
}

// ── 2. 검사 명령 ──────────────────────────────────────────────────
// require — 인자 검증 (IllegalArgumentException)
// check   — 상태 검증 (IllegalStateException)
// assert  — 내부 불변식 (AssertionError, -ea 플래그 필요)
class BankAccount(initialBalance: Double) {

    private var balance = initialBalance
    private var isOpen = true

    init {
        require(initialBalance >= 0) { "초기 잔액은 0 이상이어야 합니다: $initialBalance" }
    }

    fun deposit(amount: Double) {
        check(isOpen) { "닫힌 계좌에는 입금할 수 없습니다" }
        require(amount > 0) { "입금액은 양수여야 합니다: $amount" }
        balance += amount
    }

    fun withdraw(amount: Double): Double {
        check(isOpen) { "닫힌 계좌에서는 출금할 수 없습니다" }
        require(amount > 0) { "출금액은 양수여야 합니다" }
        require(amount <= balance) { "잔액 부족: 잔액=${balance}, 요청=${amount}" }
        balance -= amount
        return amount
    }

    fun close() { isOpen = false }
    fun getBalance() = balance
}

fun checkCommandsDemo() {
    println("\n=== 검사 명령 ===")

    val account = BankAccount(1000.0)
    account.deposit(500.0)
    println("잔액: ${account.getBalance()}")

    // require 실패
    try { account.deposit(-100.0) }
    catch (e: IllegalArgumentException) { println("require 실패: ${e.message}") }

    // check 실패
    account.close()
    try { account.deposit(100.0) }
    catch (e: IllegalStateException) { println("check 실패: ${e.message}") }

    // 잘못된 초기화
    try { BankAccount(-500.0) }
    catch (e: IllegalArgumentException) { println("초기화 실패: ${e.message}") }
}

// ── 3. Nothing 타입 ───────────────────────────────────────────────
// Nothing — 정상 반환하지 않는 함수의 반환 타입
// throw 식, 무한 루프, 항상 예외 던지는 함수

fun fail(message: String): Nothing =
    throw IllegalStateException(message)

fun assertNotEmpty(list: List<*>): List<*> =
    if (list.isEmpty()) fail("리스트가 비어있습니다") else list

fun nothingTypeDemo() {
    println("\n=== Nothing 타입 ===")

    val list = listOf(1, 2, 3)
    val nonEmpty = assertNotEmpty(list)
    println("리스트: $nonEmpty")

    try { assertNotEmpty(emptyList<Int>()) }
    catch (e: IllegalStateException) { println("실패: ${e.message}") }

    // Nothing은 모든 타입의 서브타입 → 타입 추론에서 유용
    val value: String = if (true) "hello" else fail("불가능한 분기")
    println("value: $value")

    // 엘비스와 결합
    val nullable: String? = null
    val result: String = nullable ?: fail("null 불가")    // 위 코드는 실행 안 됨
    // val result2: String = null ?: fail("null 불가")    // 이건 실패
}

// ── 4. 자원 해제 (use) ────────────────────────────────────────────
class DatabaseConnection(val url: String) : Closeable {
    init { println("DB 연결: $url") }

    fun query(sql: String): List<String> {
        println("실행: $sql")
        return listOf("row1", "row2", "row3")
    }

    override fun close() {
        println("DB 연결 해제: $url")
    }
}

class FileProcessor(val path: String) : AutoCloseable {
    private val file = File(path)

    fun readLines() = if (file.exists()) file.readLines() else emptyList()
    override fun close() = println("FileProcessor 닫힘: $path")
}

fun resourceManagementDemo() {
    println("\n=== 자원 해제 (use) ===")

    // use — Closeable/AutoCloseable에 대한 try-with-resources
    val results = DatabaseConnection("jdbc:h2:mem:test").use { conn ->
        conn.query("SELECT * FROM users")
    }
    println("쿼리 결과: $results")

    // 예외 시에도 close() 호출 보장
    try {
        DatabaseConnection("jdbc:test").use { conn ->
            conn.query("SELECT 1")
            throw RuntimeException("처리 중 오류")
        }
    } catch (e: RuntimeException) {
        println("오류 처리: ${e.message} (close는 이미 호출됨)")
    }

    // 여러 자원 순서대로 해제 (역순)
    println("\n다중 자원:")
    DatabaseConnection("db1").use { db ->
        FileProcessor("/tmp/test.txt").use { file ->
            println("내부 작업 수행")
        }   // file.close() 먼저
    }   // db.close() 나중
}

// ── 5. 로깅 전략 ──────────────────────────────────────────────────
// 실무에서는 SLF4J + Logback/Log4j2 사용
// 여기서는 원칙 설명
enum class LogLevel { DEBUG, INFO, WARN, ERROR }

class SimpleLogger(private val name: String, private val minLevel: LogLevel = LogLevel.INFO) {

    private fun log(level: LogLevel, msg: () -> String) {
        if (level >= minLevel) {
            println("[${level.name}] [$name] ${msg()}")
        }
    }

    fun debug(msg: () -> String) = log(LogLevel.DEBUG, msg)
    fun info(msg: () -> String)  = log(LogLevel.INFO, msg)
    fun warn(msg: () -> String)  = log(LogLevel.WARN, msg)
    fun error(msg: () -> String) = log(LogLevel.ERROR, msg)
}

fun loggingDemo() {
    println("\n=== 로깅 전략 ===")

    val logger = SimpleLogger("App")

    logger.debug { "연산 결과: ${1 + 1}" }       // DEBUG < INFO → 출력 안 됨
    logger.info  { "서버 시작" }
    logger.warn  { "메모리 사용량 높음" }
    logger.error { "데이터베이스 연결 실패" }

    // 람다로 전달 — 출력 안 할 때 불필요한 문자열 연산 방지
    val debugLogger = SimpleLogger("Debug", LogLevel.DEBUG)
    debugLogger.debug { "비용이 큰 연산: ${(1..100).sum()}" }   // DEBUG 레벨에서만 계산

    println("""
로깅 원칙:
  - DEBUG: 개발 중 상세 정보 (운영에선 OFF)
  - INFO:  주요 이벤트 (서버 시작, 처리 완료)
  - WARN:  잠재적 문제 (재시도, 느린 쿼리)
  - ERROR: 복구 불가 오류 (예외 스택 트레이스 포함)
  - 람다 전달로 레벨 미충족 시 문자열 연산 스킵
    """.trimIndent())
}

// ── 6. 단위 테스트 원칙 ───────────────────────────────────────────
// 실제 테스트는 JUnit5 + Kotest 사용
// 여기서는 직접 검증으로 원칙 설명

class Calculator {
    fun add(a: Int, b: Int) = a + b
    fun divide(a: Double, b: Double): Double {
        require(b != 0.0) { "0으로 나눌 수 없습니다" }
        return a / b
    }
}

fun unitTestPrinciplesDemo() {
    println("\n=== 단위 테스트 원칙 ===")

    val calc = Calculator()

    // AAA 패턴: Arrange - Act - Assert
    fun assertEquals(expected: Any?, actual: Any?, label: String = "") {
        val ok = expected == actual
        println("[${if (ok) "PASS" else "FAIL"}] $label: expected=$expected, actual=$actual")
        if (!ok) throw AssertionError("$label: expected $expected but was $actual")
    }

    // 정상 경로 (happy path)
    assertEquals(5, calc.add(2, 3), "add(2,3)")
    assertEquals(0, calc.add(0, 0), "add(0,0)")
    assertEquals(-1, calc.add(2, -3), "add(2,-3)")
    assertEquals(2.5, calc.divide(5.0, 2.0), "divide(5,2)")

    // 경계값
    assertEquals(Int.MAX_VALUE + 1L, calc.add(Int.MAX_VALUE, 1).toLong(), "overflow")

    // 예외 케이스
    var threwException = false
    try { calc.divide(1.0, 0.0) }
    catch (e: IllegalArgumentException) { threwException = true }
    assertEquals(true, threwException, "divide by zero throws")

    println("""
단위 테스트 원칙:
  - 빠름: 외부 시스템 없이 실행
  - 독립적: 테스트 간 의존 없음
  - 반복 가능: 같은 결과 보장
  - AAA 패턴: Arrange / Act / Assert
  - 경계값 테스트: 0, MAX, MIN, null
  - 실패 경로도 테스트
    """.trimIndent())
}

fun main() {
    exceptionStrategyDemo()
    checkCommandsDemo()
    nothingTypeDemo()
    resourceManagementDemo()
    loggingDemo()
    unitTestPrinciplesDemo()
}
