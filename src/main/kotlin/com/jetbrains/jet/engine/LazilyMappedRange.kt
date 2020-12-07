package com.jetbrains.jet.engine

import java.util.stream.LongStream
import java.util.stream.Stream

class LazilyMappedRange<T>(
    private val startInclusive: Long,
    private val endInclusive: Long,
    private val mapper: (Long) -> T
) {
    companion object {
        fun of(startInclusive: Long, endInclusive: Long) = LazilyMappedRange(startInclusive, endInclusive) { it }
    }

    fun <R> map(f: (T) -> R) = LazilyMappedRange(startInclusive, endInclusive) { f(mapper(it)) }

    fun stream(): Stream<T> = LongStream.rangeClosed(startInclusive, endInclusive).mapToObj(mapper)

    fun dump(builder: StringBuilder) {
        builder.append('[')
        stream().takeWhile {
            if (builder.length > 1000) {
                builder.append("...")
                return@takeWhile false
            }

            if (builder[builder.length - 1] != '[') {
                builder.append(", ")
            }

            if (it is LazilyMappedRange<*>) {
                it.dump(builder)
            } else if (it is Double && it.compareTo(it.toLong()) == 0) {
                builder.append((it as Double).toLong())
            } else {
                builder.append(it)
            }

            return@takeWhile true
        }.forEach { /* consume */ }

        builder.append("]")
    }

    override fun toString(): String {
        val builder = StringBuilder()
        dump(builder)
        return builder.toString()
    }
}
