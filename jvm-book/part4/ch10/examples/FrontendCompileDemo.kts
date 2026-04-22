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

// ── 7. javac 4단계 파이프라인 ────────────────────────────────────
println("\n=== 7. javac 4단계 파이프라인 ===")

// 1단계: 토큰화 (Lexical Analysis)
val source = "int a = 1 + 2;"
val tokens = source.split(Regex("(?<=[=+;])|(?=[=+;])| +"))
    .map { it.trim() }.filter { it.isNotEmpty() }
println("[Pipeline] 소스: \"$source\"")
println("[Pipeline] 1단계 토큰화 (${tokens.size}개): $tokens")
// → [int, a, =, 1, +, 2, ;]

// 2단계: AST 구성 (구문 분석)
println("""
[Pipeline] 2단계 AST 구성:
  LocalVariableDecl
    type  = int
    name  = a
    init  = BinaryExpr(op=+, left=Literal(1), right=Literal(2))
""".trimIndent())

// 3단계: 의미 분석 (타입 체크, 어노테이션 처리)
println("[Pipeline] 3단계 의미 분석: 타입 호환성 검사, 심볼 테이블 구성, APT 실행")

// 4단계: 상수 폴딩 후 바이트코드 생성
val foldedX = 2 + 3           // 컴파일 시 5로 고정
println("[Pipeline] 4단계 상수 폴딩: val x = 2 + 3 → 바이트코드에 BIPUSH 5 (x=${foldedX})")
println("[Pipeline] 흐름: 소스 → 토큰 → AST → 바이트코드 (.class)")

// ── 8. 타입 소거 함정 — 브릿지 메서드와 시그니처 충돌 ────────────
println("\n=== 8. 타입 소거 함정 — 브릿지 메서드 & 시그니처 충돌 ===")

// 함정 1: 시그니처 충돌 (컴파일 에러 — 설명용)
// fun process(list: List<String>) {}  // 타입 소거 후: process(List)
// fun process(list: List<Int>) {}     // 타입 소거 후: process(List) → 중복!
println("[Bridge] List<String>과 List<Int> 오버로딩 → 타입 소거 후 같은 시그니처 → 컴파일 에러")

// 함정 2: 브릿지 메서드 확인 (공변 반환 타입)
open class StringBox {
    open fun get(): String = "hello"
}
val methods = StringBox::class.java.declaredMethods
methods.forEach { m ->
    println("[Bridge] ${m.returnType.simpleName} ${m.name}() synthetic=${m.isSynthetic}")
}
println("[Bridge] 부모 클래스 공변 오버라이드 시 컴파일러가 브릿지 메서드 자동 생성")

// ── 9. 조건부 컴파일 ──────────────────────────────────────────────
println("\n=== 9. 조건부 컴파일 ===")

const val DEBUG = false

fun conditionalCompile(): String {
    if (DEBUG) {
        println("[ConditionalCompile] 이 코드는 DEBUG=false이면 바이트코드에서 제거될 수 있음")
    }
    return "result"
}

val condResult = conditionalCompile()
println("[ConditionalCompile] DEBUG=$DEBUG → if 블록 바이트코드 포함 여부 설명")
println("[ConditionalCompile] 결과: $condResult")
println("""
[ConditionalCompile] javac 처리 방식:
  if(true)  {} → 블록 내부 코드 유지, if 구조 제거
  if(false) {} → 블록 전체 제거 (dead code elimination)
  const val/static final 사용 시 컴파일러가 값 인라인 → 동일 효과
  C# #if DEBUG 와 달리 Java/Kotlin은 소스레벨 if 문으로만 가능
""".trimIndent())

// ── 10. Java vs C# 제네릭 비교 ───────────────────────────────────
println("\n=== 10. Java vs C# 제네릭 비교 ===")

// Java: 타입 소거 → List<String>과 List<Int>는 런타임에 동일
// Kotlin reified로 Java 한계 극복 (컴파일러 트릭)
inline fun <reified T> printType() = println("[Generic] 런타임 타입: ${T::class.simpleName}")
printType<String>()
printType<Int>()
// 실제로는 컴파일 시 구체 타입으로 치환 → JVM 수준 실체화 아님

println("""
[Generic 비교]
  Java 타입 소거: List<String> == List<Integer> at runtime
  C# 구체화:      List<string> != List<int> at runtime
  Kotlin reified: inline fun으로 컴파일러가 타입 구체화 (JVM 레벨 아님)
  Project Valhalla: 미래에 List<int> 직접 지원 예정

[Java 타입 소거 장단점]
  장점: 하위 호환성 (Java 5 이전 코드와 바이너리 호환)
  단점: instanceof 불가, 원시타입 박싱 필요, 런타임 타입 정보 손실
""".trimIndent())
