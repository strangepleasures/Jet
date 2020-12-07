package com.jetbrains.jet.engine

import java.util.stream.IntStream
import kotlin.test.Test
import kotlin.test.assertEquals


internal class SequentialReducerTest {
    private val reducer = SequentialReducer()
    @Test
    fun `reduce associative`() {
        assertEquals(1000000, reducer.reduce(IntStream.range(0, 1000000).mapToObj{ 1 }, 0) {x, y -> x + y})
    }

    @Test
    fun `reduce non-associative`() {
        assertEquals(-1000000, reducer.reduce(IntStream.range(0, 1000000).mapToObj{ 1 }, 0) {x, y -> x - y})
    }
}