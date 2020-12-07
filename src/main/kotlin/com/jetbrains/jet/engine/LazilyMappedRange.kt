package com.jetbrains.jet.engine

import java.util.stream.LongStream

class LazilyMappedRange<T>(private val startInclusive: Long, private val endInclusive: Long, private val mapper: (Long) -> T) {
    companion object {
        fun of(startInclusive: Long, endInclusive: Long) = LazilyMappedRange(startInclusive, endInclusive) { it }
    }

    fun <R> map(f: (T) -> R) = LazilyMappedRange(startInclusive, endInclusive) { f(mapper(it)) }

    fun stream() = LongStream.rangeClosed(startInclusive, endInclusive).mapToObj(mapper)

    override fun toString(): String  {
        val builder = StringBuilder("[")
        stream().takeWhile {
            if (builder.length > 1) {
                builder.append(", ")
            }
            if (it is Double && it.compareTo(it.toLong()) == 0) {
                builder.append(it.toLong()) // compact if possible
            } else {
                builder.append(it)
            }
            if (builder.length > 1000) {
                builder.append("...")
                false
            } else {
                true
            }
        }.forEach { /* consume */ }

        return builder.append("]").toString()
    }
}
