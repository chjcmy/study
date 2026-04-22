#!/usr/bin/env kotlinc -script
/**
 * 핵심 개념 3 — HotSpot JIT, 계층형 컴파일
 * 실행: kotlinc -script JitWarmupDemo.kts
 *
 * JIT 비교:
 *   kotlinc JitWarmupDemo.kts -include-runtime -d jit.jar
 *   java -jar jit.jar                        (JIT ON)
 *   java -Xint -jar jit.jar                  (인터프리터만)
 *   java -XX:+PrintCompilation -jar jit.jar  (JIT 컴파일 로그)
 */

fun compute(n: Long): Long {
    var result = 0L
    for (i in 1..n) result += i * i - i / 2
    return result
}

val rounds = 20
val work   = 1_000_000L

println("%-8s %14s".format("round", "ns/op"))
println("-".repeat(24))

repeat(rounds) { i ->
    val start   = System.nanoTime()
    val result  = compute(work)
    val elapsed = System.nanoTime() - start
    println("%-8d %14d   (checksum=${result % 1000})".format(i + 1, elapsed))
}
