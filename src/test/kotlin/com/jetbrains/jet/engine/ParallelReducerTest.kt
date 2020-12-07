package com.jetbrains.jet.engine

import java.util.stream.IntStream
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals

internal class ParallelReducerTest {
    private val reducer = ParallelReducer()
    @Test
    fun `reduce associative`() {
        assertEquals(
            1000000,
            reducer.reduce(IntStream.range(0, 1000000).mapToObj { 1 }, 0) { x, y -> x + y })
    }

    @Ignore("ParallelReducer can only handle associative functions")
    @Test
    fun `reduce non-associative`() {
        assertEquals(
            -1000000,
            reducer.reduce(IntStream.range(0, 1000000).mapToObj { 1 }, 0) { x, y -> x - y })
    }
}