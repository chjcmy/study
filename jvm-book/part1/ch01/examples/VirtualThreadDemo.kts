#!/usr/bin/env kotlinc -script
/**
 * 핵심 개념 2 — JDK 21, 가상 스레드 (Project Loom)
 * 실행: kotlinc -script VirtualThreadDemo.kts   (JDK 21 필수)
 */

import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

val TASK_COUNT = 10_000
val SLEEP_MS   = 10L

fun runWith(label: String, executorFactory: () -> java.util.concurrent.ExecutorService) {
    val completed = AtomicInteger(0)
    val executor  = executorFactory()
    val start     = System.currentTimeMillis()

    repeat(TASK_COUNT) {
        executor.submit {
            try {
                Thread.sleep(SLEEP_MS)
                completed.incrementAndGet()
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
            }
        }
    }

    executor.shutdown()
    executor.awaitTermination(5, TimeUnit.MINUTES)
    val elapsed = System.currentTimeMillis() - start
    println("%-47s completed=%d  elapsed=%dms\n".format(label, completed.get(), elapsed))
}

println("tasks=$TASK_COUNT, sleep per task=${SLEEP_MS}ms\n")
runWith("Platform Thread (fixed pool 200)") { Executors.newFixedThreadPool(200) }
runWith("Virtual Thread (newVirtualThreadPerTaskExecutor)") { Executors.newVirtualThreadPerTaskExecutor() }
