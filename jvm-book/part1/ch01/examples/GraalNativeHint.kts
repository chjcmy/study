#!/usr/bin/env kotlinc -script
/**
 * 핵심 개념 3 — GraalVM, Native Image, AOT vs JIT
 * 실행: kotlinc -script GraalNativeHint.kts
 *
 * GraalVM Native 비교:
 *   kotlinc GraalNativeHint.kts -include-runtime -d graal.jar
 *   time java -jar graal.jar           (JVM)
 *   native-image -jar graal.jar && time ./graal  (Native)
 */

fun isGraalVm(): Boolean {
    val vmName = System.getProperty("java.vm.name", "")
    val vendor = System.getProperty("java.vendor", "")
    return "GraalVM" in vmName || "GraalVM" in vendor
}

val processAge = ProcessHandle.current().info().startInstant()
    .map { System.currentTimeMillis() - it.toEpochMilli() }
    .orElse(-1L)

println("vm.name    : ${System.getProperty("java.vm.name")}")
println("vm.vendor  : ${System.getProperty("java.vendor")}")
println("graalvm    : ${isGraalVm()}")
println("kotlin     : ${KotlinVersion.CURRENT}")
println("process age: ${processAge}ms")
