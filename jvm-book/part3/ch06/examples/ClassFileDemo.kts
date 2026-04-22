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
