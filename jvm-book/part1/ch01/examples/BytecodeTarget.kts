#!/usr/bin/env kotlinc -script
/**
 * 핵심 개념 1 — 바이트코드, WORA
 * 실행: kotlinc -script BytecodeTarget.kts
 *
 * 바이트코드 확인 (컴파일 후):
 *   kotlinc BytecodeTarget.kts -include-runtime -d bt.jar
 *   jar xf bt.jar && javap -c Item
 */

data class Item(val id: Int, val name: String, val price: Double)

sealed class Status {
    object Active   : Status()
    object Inactive : Status()
    data class Error(val message: String) : Status()
}

class Cart(private val ownerId: String) {
    private val items = mutableListOf<Item>()
    fun add(item: Item) = items.add(item)
    fun total() = items.sumOf { it.price }
    fun describe(status: Status) = when (status) {
        is Status.Active   -> "owner=$ownerId active, total=${total()}"
        is Status.Inactive -> "owner=$ownerId inactive"
        is Status.Error    -> "error: ${status.message}"
    }
    fun cheaperThan(threshold: Double) = items.filter { it.price < threshold }
    companion object { fun empty(ownerId: String) = Cart(ownerId) }
}

val cart = Cart.empty("user-1")
cart.add(Item(1, "Coffee", 4500.0))
cart.add(Item(2, "Bread", 3200.0))
cart.add(Item(3, "Water", 1000.0))

println(cart.describe(Status.Active))
println("under 4000: ${cart.cheaperThan(4000.0)}")

val original   = Item(1, "Coffee", 4500.0)
val discounted = original.copy(price = 3000.0)
println("original=$original discounted=$discounted same=${original == discounted}")

val nullableName: String? = null
println("nullable length: ${nullableName?.length ?: -1}")
