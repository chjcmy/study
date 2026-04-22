#!/usr/bin/env kotlinc -script
/**
 * 10장 — 프런트엔드 컴파일과 최적화
 * 실행: kotlinc -script FrontendCompileDemo.kts
 */

// ── 1. 제네릭 타입 소거 ───────────────────────────────────────────
println("=== 1. 제네릭 타입 소거 ===")

val strings: List<String> = listOf("a", "b", "c")
val ints:    List<Int>    = listOf(1, 2, 3)

println("[TypeErasure] strings 런타임 타입: ${strings.javaClass.name}")
println("[TypeErasure] ints 런타임 타입:    ${ints.javaClass.name}")
println("[TypeErasure] 동일한 클래스: ${strings.javaClass == ints.javaClass}")

// reified로 런타임 타입 유지 (inline 함수)
inline fun <reified T> List<*>.filterIsType() = filterIsInstance<T>()
val mixed: List<Any> = listOf(1, "hello", 2.0, true, "world")
println("[TypeErasure] filterIsType<String>: ${mixed.filterIsType<String>()}")

// ── 2. 오토박싱/언박싱 함정 ──────────────────────────────────────
println("\n=== 2. 오토박싱/언박싱 함정 ===")

val a: Int? = 127; val b: Int? = 127
println("[Boxing] 127 == 127 (참조): ${a === b}")   // true (캐시 -128~127)

val c: Int? = 128; val d: Int? = 128
println("[Boxing] 128 == 128 (참조): ${c === d}")   // false (캐시 초과)
println("[Boxing] 128 == 128 (값):   ${c == d}")    // true

val start1 = System.nanoTime()
var sum1: Long = 0L
for (i in 0..1_000_000L) sum1 += i
val ms1 = (System.nanoTime() - start1) / 1_000_000.0

val start2 = System.nanoTime()
var sum2: Long? = 0L
for (i in 0..1_000_000L) sum2 = sum2!! + i
val ms2 = (System.nanoTime() - start2) / 1_000_000.0

println("[Boxing] 기본형 Long: ${"%.2f".format(ms1)}ms")
println("[Boxing] 박싱 Long?:  ${"%.2f".format(ms2)}ms (느림)")

// ── 3. for-each 내부 변환 ────────────────────────────────────────
println("\n=== 3. for-each 내부 변환 ===")
val list = listOf(1, 2, 3, 4, 5)
for (n in list) print("$n ")
println()
println("[ForEach] List for-each → Iterator 패턴 (javap -c 확인)")
println("[ForEach] Array for-each → 인덱스 기반 (Iterator 오버헤드 없음)")

// ── 4. String switch 변환 ─────────────────────────────────────────
println("\n=== 4. String switch 변환 ===")

fun classify(cmd: String) = when (cmd) {
    "start"  -> "시작 명령"
    "stop"   -> "중지 명령"
    "status" -> "상태 조회"
    else     -> "알 수 없음"
}

listOf("start", "stop", "status", "unknown").forEach { println("[Switch] '$it' → ${classify(it)}") }
println("[Switch] 내부적으로 hashCode() + equals() 2단계 비교")
listOf("start", "stop", "status").forEach { println("  '$it'.hashCode() = ${it.hashCode()}") }

// ── 5. 어노테이션 처리 (APT) ─────────────────────────────────────
println("\n=== 5. 어노테이션 처리 ===")

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
annotation class Timed(val name: String = "")

class Service { @Timed("fetchUser") fun fetchUser(id: Long) = "User($id)" }

val method = Service::class.java.getMethod("fetchUser", Long::class.java)
val timed  = method.getAnnotation(Timed::class.java)
println("[APT] @Timed 이름: ${timed?.name}")
println("[APT] 어노테이션: ${method.annotations.map { it.annotationClass.simpleName }}")
println("[APT] Lombok @Data, MapStruct @Mapper, Dagger @Component → 컴파일 시 코드 생성")

// ── 6. var 타입 추론 ──────────────────────────────────────────────
println("\n=== 6. var 타입 추론 ===")
val x    = 42
val lst  = mutableListOf(1, 2, 3)
val map  = HashMap<String, Int>()
println("[Var] x 타입: ${(x as Any).javaClass.simpleName}")
println("[Var] list 타입: ${lst.javaClass.simpleName}")
println("[Var] map 타입: ${map.javaClass.simpleName}")
println("[Var] 런타임에 타입 완전 결정됨, Java var은 로컬 변수만 가능")
