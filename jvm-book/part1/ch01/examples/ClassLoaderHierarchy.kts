#!/usr/bin/env kotlinc -script
/**
 * 핵심 개념 3 — JVM 클래스 로더 계층
 * 실행: kotlinc -script ClassLoaderHierarchy.kts
 */

fun section(title: String, block: () -> Unit) { println("\n=== $title ==="); block() }
fun printLoader(label: String, clazz: Class<*>) {
    val cl = clazz.classLoader
    println("  %-40s %s".format(label, cl?.javaClass?.name ?: "null (Bootstrap)"))
}

section("ClassLoader per class") {
    printLoader("String (java.lang)", String::class.java)
    printLoader("ArrayList (java.util)", java.util.ArrayList::class.java)
    printLoader("kotlin.collections.ArrayDeque", ArrayDeque::class.java)
}

section("Parent chain of App ClassLoader") {
    var loader: ClassLoader? = Thread.currentThread().contextClassLoader
    while (loader != null) {
        println("  ${loader.javaClass.name}")
        loader = loader.parent
    }
    println("  null → Bootstrap ClassLoader (native)")
}

section("ClassPath") {
    println("  " + (System.getProperty("java.class.path") ?: "(없음)"))
}

section("Module Path (JDK 9+)") {
    println("  " + (System.getProperty("jdk.module.path") ?: "(없음)"))
}
