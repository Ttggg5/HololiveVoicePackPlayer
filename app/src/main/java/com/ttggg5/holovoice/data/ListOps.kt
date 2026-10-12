package com.ttggg5.holovoice.data

/** Returns a copy of this list with the element at [from] moved to index [to]. */
fun <T> List<T>.moved(from: Int, to: Int): List<T> {
    if (from == to || from !in indices || to !in indices) return this
    val result = toMutableList()
    result.add(to, result.removeAt(from))
    return result
}
