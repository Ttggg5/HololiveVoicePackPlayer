package com.ttggg5.holovoice.data

import org.junit.Assert.assertEquals
import org.junit.Test

class ListOpsTest {
    private val list = listOf("a", "b", "c", "d")

    @Test
    fun moveDown() = assertEquals(listOf("b", "c", "a", "d"), list.moved(0, 2))

    @Test
    fun moveUp() = assertEquals(listOf("d", "a", "b", "c"), list.moved(3, 0))

    @Test
    fun sameIndexOrOutOfRangeIsNoOp() {
        assertEquals(list, list.moved(1, 1))
        assertEquals(list, list.moved(-1, 2))
        assertEquals(list, list.moved(0, 4))
    }
}
