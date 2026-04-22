#!/usr/bin/env kotlinc -script
/**
 * 6장 — 클래스 파일 구조
 * 실행: kotlinc -script ClassFileDemo.kts
 */

import java.io.DataInputStream

fun hex(b: Int) = "0x%02X".format(b)

// ── 1. Magic Number 파싱 ──────────────────────────────────────────
println("=== 1. Magic Number 파싱 ===")

fun readMagic(classBytes: ByteArray): String {
    val magic = (0..3).map { classBytes[it].toInt() and 0xFF }
    return if (magic == listOf(0xCA, 0xFE, 0xBA, 0xBE)) "✓ CAFEBABE (유효한 클래스 파일)"
    else "✗ 유효하지 않은 클래스 파일"
}

val classStream = String::class.java.classLoader?.getResourceAsStream("java/lang/String.class")
if (classStream != null) {
    val bytes = classStream.readBytes()
    println("[Magic] java/lang/String.class: ${readMagic(bytes)}")
    val dis = DataInputStream(bytes.inputStream())
    println("[Magic] magic=0x%08X".format(dis.readInt()))
    println("[Magic] minor_version=${dis.readShort()}")
    println("[Magic] major_version=${dis.readShort()} (52=Java8, 55=Java11, 61=Java17, 65=Java21)")
} else {
    println("[Magic] String.class 로드 불가 (스크립트 환경 제한)")
}

// ── 2. 접근 플래그 해석 ───────────────────────────────────────────
println("\n=== 2. 접근 플래그 ===")

val ACCESS_FLAGS = mapOf(
    0x0001 to "ACC_PUBLIC", 0x0010 to "ACC_FINAL",
    0x0020 to "ACC_SUPER",  0x0200 to "ACC_INTERFACE",
    0x0400 to "ACC_ABSTRACT", 0x1000 to "ACC_SYNTHETIC",
    0x2000 to "ACC_ANNOTATION", 0x4000 to "ACC_ENUM"
)

fun decodeFlags(flags: Int): String =
    ACCESS_FLAGS.entries.filter { (k, _) -> flags and k != 0 }.joinToString(" | ") { it.value }

// 예시 클래스들의 플래그
mapOf(
    "public class"            to 0x0021,
    "public final class"      to 0x0031,
    "public interface"        to 0x0601,
    "public abstract class"   to 0x0421,
    "public enum"             to 0x4031
).forEach { (desc, flags) ->
    println("  $desc (0x%04X): ${decodeFlags(flags)}".format(flags))
}

// ── 3. 상수 풀 태그 ───────────────────────────────────────────────
println("\n=== 3. 상수 풀 태그 ===")
println("""
  태그  의미
  ────  ──────────────────────────────────
   1    Utf8             — 문자열 데이터
   3    Integer          — int 리터럴
   4    Float            — float 리터럴
   5    Long             — long 리터럴
   7    Class            — 클래스/인터페이스 참조
   8    String           — java.lang.String 리터럴
   9    Fieldref         — 필드 참조
  10    Methodref        — 메서드 참조
  11    InterfaceMethodref
  12    NameAndType      — 이름 + 타입 기술자
  15    MethodHandle     — invokedynamic 핵심
  18    InvokeDynamic    — 람다/메서드 참조
""".trimIndent())

// ── 4. 타입 기술자 ────────────────────────────────────────────────
println("=== 4. 타입 기술자 (Descriptor) ===")
println("""
  기술자  Java 타입
  ──────  ──────────────────────────────
  B       byte
  C       char
  D       double
  F       float
  I       int
  J       long
  S       short
  Z       boolean
  V       void
  [I      int[]
  [[B     byte[][]
  Ljava/lang/String;   String

  메서드 기술자:
    (Ljava/lang/String;I)Z  →  boolean method(String, int)
    ()V                      →  void method()
    ([BII)V                  →  void method(byte[], int, int)
""".trimIndent())

// ── 5. Kotlin → 바이트코드 매핑 ──────────────────────────────────
println("=== 5. Kotlin → 바이트코드 매핑 ===")
println("""
  Kotlin 소스               바이트코드 패턴
  ─────────────────         ─────────────────────────────────
  data class Item(...)      equals/hashCode/toString/copy/componentN 자동 생성
  companion object          내부 static Companion 클래스
  sealed class              abstract class + 서브클래스 패키지 제한
  when (sealed)             tableswitch/lookupswitch
  null 체크 (x!!)           Intrinsics.checkNotNullParameter()
  람다                       invokedynamic (JDK 8+)
  inline fun                호출 지점에 코드 인라인 삽입
  object                    싱글톤 — INSTANCE static 필드
""".trimIndent())

// ── 6. 5가지 메서드 호출 명령어 ──────────────────────────────────
println("\n=== 6. 5가지 메서드 호출 명령어 ===")

// invokestatic: 정적 메서드 호출 — 수신 객체 없음
println("\n[invokestatic]")
val maxVal = Math.max(10, 42)
println("[invokestatic] Math.max(10, 42) = $maxVal  ← invokestatic 명령어 사용")
println("[invokestatic] javap 출력 예시:")
println("  invokestatic  #N // Method java/lang/Math.max:(II)I")

// invokespecial: 생성자, private 메서드, super 호출
println("\n[invokespecial]")
class SpecialDemo {
    private fun secret() = "private 메서드 결과"
    fun callSecret() = secret()
    init { println("[invokespecial] 생성자 호출 — <init> 메서드 실행 (invokespecial)") }
}
val sd = SpecialDemo()
println("[invokespecial] private 메서드 간접 호출: ${sd.callSecret()}")
println("[invokespecial] javap 출력 예시:")
println("  invokespecial #N // Method SpecialDemo.secret:()Ljava/lang/String;")

// invokevirtual: 인스턴스 메서드 (다형성 — vtable 기반)
println("\n[invokevirtual]")
open class Shape { open fun area() = 0.0 }
class Circle(val r: Double) : Shape() { override fun area() = Math.PI * r * r }
class Rect(val w: Double, val h: Double) : Shape() { override fun area() = w * h }

val shapes: List<Shape> = listOf(Circle(3.0), Rect(4.0, 5.0), Circle(1.0))
shapes.forEach { s ->
    println("[invokevirtual] ${s.javaClass.simpleName}.area() = ${"%.2f".format(s.area())}  ← 런타임 타입에 따라 결정")
}
println("[invokevirtual] javap 출력 예시:")
println("  invokevirtual #N // Method Shape.area:()D  (런타임에 vtable 조회)")

// invokeinterface: 인터페이스 메서드 호출
println("\n[invokeinterface]")
interface Drawable { fun draw(): String }
class Canvas : Drawable { override fun draw() = "Canvas에 그리기" }
class SVG : Drawable { override fun draw() = "SVG로 렌더링" }

val drawables: List<Drawable> = listOf(Canvas(), SVG())
drawables.forEach { d ->
    println("[invokeinterface] ${d.javaClass.simpleName}: ${d.draw()}  ← invokeinterface (itable 조회)")
}
println("[invokeinterface] javap 출력 예시:")
println("  invokeinterface #N, 1 // InterfaceMethod Drawable.draw:()Ljava/lang/String;")

// invokedynamic: 람다 — 런타임에 CallSite 생성
println("\n[invokedynamic]")
val multiplier: (Int, Int) -> Int = { a, b -> a * b }
println("[invokedynamic] 람다 호출 결과: ${multiplier(6, 7)}")
println("[invokedynamic] 람다 클래스명: ${multiplier.javaClass.name}")
println("[invokedynamic] javap 출력 예시:")
println("  invokedynamic #N, 0  // bootstrap: LambdaMetafactory.metafactory")
println("""
[invokedynamic] 핵심 차이점 요약:
  invokestatic      — 수신 객체 없음, 컴파일 타임에 메서드 고정
  invokespecial     — 수신 객체 있음, 컴파일 타임에 메서드 고정 (오버라이드 불가)
  invokevirtual     — 수신 객체 있음, 런타임 vtable 조회 (클래스 상속)
  invokeinterface   — 수신 객체 있음, 런타임 itable 조회 (인터페이스)
  invokedynamic     — CallSite를 통해 완전 동적 — 람다/문자열 연결/Kotlin DSL
""".trimIndent())

// ── 7. ByteBuddy 계측과 클래스 파일 연결 ─────────────────────────
println("=== 7. ByteBuddy 계측과 클래스 파일 연결 ===")
println("""
[ByteBuddy] 계측 시 클래스 파일 변경 과정:
  1. 상수 풀(Constant Pool) 확장
     - 새로운 Methodref 항목 추가 (인터셉터 클래스의 메서드 참조)
     - Utf8 항목 추가 (클래스명, 메서드명, 디스크립터)
     - 예: #NN Methodref com/logfriends/agent/MethodTraceInterceptor.enter:(...)V

  2. Code 속성(Code Attribute) 재작성
     - 기존 메서드의 Code 바이트코드 앞뒤에 invokestatic 명령어 삽입
     - 진입 시: invokestatic → MethodTraceInterceptor.enter()
     - 반환 시: invokestatic → MethodTraceInterceptor.exit()
     - 예외 발생 시: astore + invokestatic → exit(exception)

  3. RETRANSFORMATION (log-friends 방식)
     - 이미 로딩된 클래스도 재변환 가능
     - Instrumentation.retransformClasses(clazz) 호출
     - ClassFileTransformer.transform()에서 수정된 바이트 배열 반환

  참고: ByteBuddy는 ASM 라이브러리 위에서 작동 (저수준 바이트코드 조작)
""".trimIndent())

// 수정 전후 클래스 객체 동일성 확인 (동일 클래스 로더 → 같은 객체)
class BeforeInstrumentation { fun hello() = "original" }
val instance1 = BeforeInstrumentation()
val instance2 = BeforeInstrumentation()
println("[ByteBuddy] 같은 클래스 다른 인스턴스 — 클래스 객체 동일성:")
println("  instance1.javaClass === instance2.javaClass : ${instance1.javaClass === instance2.javaClass}")
println("  System.identityHashCode(instance1.javaClass) = ${System.identityHashCode(instance1.javaClass)}")
println("  System.identityHashCode(instance2.javaClass) = ${System.identityHashCode(instance2.javaClass)}")
println("  → 같은 ClassLoader가 로드한 클래스는 JVM 내 단 하나의 Class 객체")
println("  → ByteBuddy가 클래스를 retransform하면 Class 객체 참조는 유지, 내부 Code만 교체")
