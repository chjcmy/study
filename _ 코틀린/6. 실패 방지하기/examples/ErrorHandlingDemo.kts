#!/usr/bin/env kotlinc -script
/**
 * 6장 — 실패 방지하기
 * 실행: kotlinc -script ErrorHandlingDemo.kts
 */

import java.io.Closeable

// ── 1. 예외 처리 전략 ─────────────────────────────────────────────
sealed class ApiResult<out T> {
    data class Ok<T>(val value: T) : ApiResult<T>()
    data class Err(val message: String, val code: Int = 0) : ApiResult<Nothing>()
}

fun fetchUser(id: Int): ApiResult<String> = when {
    id <= 0  -> ApiResult.Err("잘못된 ID", 400)
    id > 100 -> ApiResult.Err("사용자 없음", 404)
    else     -> ApiResult.Ok("User#$id")
}

println("=== 예외 처리 전략 ===")
val parsed = try { Integer.parseInt("not-a-number") }
catch (e: NumberFormatException) { println("파싱 실패: ${e.message}"); -1 }
println("result = $parsed")

listOf(5, -1, 200).forEach { id ->
    when (val r = fetchUser(id)) {
        is ApiResult.Ok  -> println("성공: ${r.value}")
        is ApiResult.Err -> println("오류 [${r.code}]: ${r.message}")
    }
}

// ── 2. 검사 명령 (require / check) ───────────────────────────────
class BankAccount(initialBalance: Double) {
    private var balance = initialBalance
    private var isOpen = true
    init { require(initialBalance >= 0) { "초기 잔액은 0 이상: $initialBalance" } }

    fun deposit(amount: Double) {
        check(isOpen) { "닫힌 계좌" }
        require(amount > 0) { "입금액은 양수: $amount" }
        balance += amount
    }
    fun close() { isOpen = false }
    fun getBalance() = balance
}

println("\n=== 검사 명령 ===")
val acct = BankAccount(1000.0)
acct.deposit(500.0)
println("잔액: ${acct.getBalance()}")

try { acct.deposit(-100.0) }
catch (e: IllegalArgumentException) { println("require 실패: ${e.message}") }

acct.close()
try { acct.deposit(100.0) }
catch (e: IllegalStateException) { println("check 실패: ${e.message}") }

try { BankAccount(-500.0) }
catch (e: IllegalArgumentException) { println("초기화 실패: ${e.message}") }

// ── 3. Nothing 타입 ───────────────────────────────────────────────
fun fail(msg: String): Nothing = throw IllegalStateException(msg)
fun assertNotEmpty(list: List<*>) = if (list.isEmpty()) fail("리스트가 비어있음") else list

println("\n=== Nothing 타입 ===")
println("비어있지 않음: ${assertNotEmpty(listOf(1, 2, 3))}")
try { assertNotEmpty(emptyList<Int>()) }
catch (e: IllegalStateException) { println("실패: ${e.message}") }

val value: String = if (true) "hello" else fail("불가능한 분기")
println("value: $value")

// ── 4. 자원 해제 (use) ────────────────────────────────────────────
class DatabaseConnection(val url: String) : Closeable {
    init { println("DB 연결: $url") }
    fun query(sql: String): List<String> { println("실행: $sql"); return listOf("row1", "row2") }
    override fun close() = println("DB 연결 해제: $url")
}

println("\n=== 자원 해제 (use) ===")
val rows = DatabaseConnection("jdbc:h2:mem:test").use { it.query("SELECT * FROM users") }
println("결과: $rows")

try {
    DatabaseConnection("jdbc:test").use { conn ->
        conn.query("SELECT 1")
        throw RuntimeException("처리 중 오류")
    }
} catch (e: RuntimeException) { println("오류 처리: ${e.message} (close 완료)") }

// ── 5. 로깅 전략 ──────────────────────────────────────────────────
enum class LogLevel { DEBUG, INFO, WARN, ERROR }

class SimpleLogger(private val name: String, private val min: LogLevel = LogLevel.INFO) {
    private fun log(level: LogLevel, msg: () -> String) {
        if (level >= min) println("[${level.name}] [$name] ${msg()}")
    }
    fun debug(msg: () -> String) = log(LogLevel.DEBUG, msg)
    fun info(msg: () -> String)  = log(LogLevel.INFO, msg)
    fun warn(msg: () -> String)  = log(LogLevel.WARN, msg)
    fun error(msg: () -> String) = log(LogLevel.ERROR, msg)
}

println("\n=== 로깅 ===")
val logger = SimpleLogger("App")
logger.debug { "이건 안 보임" }
logger.info  { "서버 시작" }
logger.warn  { "메모리 사용량 높음" }
logger.error { "DB 연결 실패" }

// ── 6. 단위 테스트 원칙 ───────────────────────────────────────────
class Calculator {
    fun add(a: Int, b: Int) = a + b
    fun divide(a: Double, b: Double): Double {
        require(b != 0.0) { "0으로 나눌 수 없음" }
        return a / b
    }
}

fun assertEquals(expected: Any?, actual: Any?, label: String = "") {
    val ok = expected == actual
    println("[${if (ok) "PASS" else "FAIL"}] $label")
    if (!ok) throw AssertionError("$label: expected $expected but was $actual")
}

println("\n=== 단위 테스트 원칙 ===")
val calc = Calculator()
assertEquals(5, calc.add(2, 3), "add(2,3)")
assertEquals(0, calc.add(0, 0), "add(0,0)")
assertEquals(-1, calc.add(2, -3), "add(2,-3)")
assertEquals(2.5, calc.divide(5.0, 2.0), "divide(5,2)")

var threw = false
try { calc.divide(1.0, 0.0) } catch (e: IllegalArgumentException) { threw = true }
assertEquals(true, threw, "divide by zero throws")
println("모든 테스트 통과!")
