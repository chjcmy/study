#!/usr/bin/env kotlinc -script
/**
 * 핵심 개념 1 — JDK / JRE / JVM 계층 구조 & JVM 제품군
 * 실행: kotlinc -script JvmInfo.kts
 */

import java.lang.management.ManagementFactory

fun section(title: String, block: () -> Unit) { println("\n=== $title ==="); block() }
fun prop(key: String) = println("  %-28s: %s".format(key, System.getProperty(key)))

section("Java Version") {
    prop("java.version"); prop("java.specification.version"); prop("java.class.version")
}
section("JVM Implementation") {
    prop("java.vm.name"); prop("java.vm.version"); prop("java.vm.vendor"); prop("java.vm.info")
}
section("JDK / JRE") { prop("java.home"); prop("java.vendor") }
section("Kotlin Runtime") {
    println("  KotlinVersion         : ${KotlinVersion.CURRENT}")
    println("  runs on JVM           : ${System.getProperty("java.vm.name")}")
}
section("Runtime") {
    val runtime = ManagementFactory.getRuntimeMXBean()
    println("  uptime (ms)           : ${runtime.uptime}")
    println("  pid                   : ${ProcessHandle.current().pid()}")
    println("  jvm args              : ${runtime.inputArguments}")
}
section("Memory") {
    val mem = ManagementFactory.getMemoryMXBean()
    println("  heap used             : ${mem.heapMemoryUsage.used / 1024 / 1024} MB")
    println("  heap max              : ${mem.heapMemoryUsage.max / 1024 / 1024} MB")
    println("  non-heap used         : ${mem.nonHeapMemoryUsage.used / 1024 / 1024} MB")
}
section("OS") {
    prop("os.name"); prop("os.arch")
    println("  available processors  : ${Runtime.getRuntime().availableProcessors()}")
}
