package com.jetbrains.jet.engine

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

internal class LazilyMappedRangeTest {
    @Test
    fun `empty range`() {
        assertEquals("[]", LazilyMappedRange.of(2, 1).toString())
    }

    @Test
    fun `single element`() {
        assertEquals("[1]", LazilyMappedRange.of(1, 1).toString())
    }

    @Test
    fun `simple range`() {
        assertEquals("[1, 2, 3]", LazilyMappedRange.of(1, 3).toString())
    }

    @Test
    fun `map to Long`() {
        assertEquals("[2, 3, 4]", LazilyMappedRange.of(1, 3).map { it + 1 }.toString())
    }

    @Test
    fun `map to Double`() {
        assertEquals("[0.25, 0.5, 0.75, 1]", LazilyMappedRange.of(1, 4).map { it / 4.0 }.toString())
    }

    @Test
    fun `map to range`() {
        assertEquals("[[1], [1, 2], [1, 2, 3]]", LazilyMappedRange.of(1, 3).map { LazilyMappedRange.of(1, it) }.toString())
    }

    @Test
    fun `huge range`() {
        val huge = LazilyMappedRange.of(Long.MIN_VALUE, Long.MAX_VALUE)
        assertTrue(huge.toString().endsWith("...]"))
        val mapped = huge.map { 1.0 / it }
        assertTrue(mapped.toString().endsWith("...]"))
    }

    @Test
    fun `composition of mappers`() {
        assertEquals("[4, 9, 16]", LazilyMappedRange.of(1, 3).map { it + 1 }.map { it * it }.toString())
    }

}
