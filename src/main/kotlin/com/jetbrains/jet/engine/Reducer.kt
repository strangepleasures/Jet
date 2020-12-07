package com.jetbrains.jet.engine


import java.util.stream.Stream

/**
 * Performs a reduction on the elements of the provided stream,
 * using the provided identity value and accumulation function,
 * and returns the reduced value.
 */
interface Reducer {
    fun <T> reduce(stream: Stream<T>, identity: T, accumulator: (T, T) -> T): T
}

/**
 * Performs reduction sequentially on the caller thread.
 * This implementation can also work with non-associative accumulators.
 */
class SequentialReducer : Reducer {
    override fun <T> reduce(stream: Stream<T>, identity: T, accumulator: (T, T) -> T): T =
        stream.reduce(identity, accumulator)
}

/**
 * Performs parallel reduction on [java.util.concurrent.ForkJoinPool.commonPool].
 * The accumulator function MUST be associative.
 */
class ParallelReducer : Reducer {
    override fun <T> reduce(stream: Stream<T>, identity: T, accumulator: (T, T) -> T): T =
        stream.parallel()
            .reduce(identity, accumulator, accumulator)
}


// Some ideas for the future


/**
 * Transforms the provided job into GPU instructions and executes them.
 * We expect all arguments to be Jet-backed, so this might be not a too complex task.
 */
class GpuReducer : Reducer {
    override fun <T> reduce(stream: Stream<T>, identity: T, accumulator: (T, T) -> T): T {
        TODO("Not yet implemented")
    }
}

/**
 * Uses the AWS Lambda serverless compute service
 */
class AWSLambdaReducer : Reducer {
    override fun <T> reduce(stream: Stream<T>, identity: T, accumulator: (T, T) -> T): T {
        TODO("Not yet implemented")
    }
}

// Not to be taken seriously

/**
 * Uses another great service from Amazon.
 *
 * Explains the computational job in [Basic English](https://simple.wikipedia.org/wiki/Basic_English)
 * and posts it on [mturk.com](https://www.mturk.com) offering ¢ 0.000001 per floating point operation.
 *
 * @throws OutOfMoneyError
 */
class AmazonMechanicalTurkReducer : Reducer {
    override fun <T> reduce(stream: Stream<T>, identity: T, accumulator: (T, T) -> T): T {
        TODO("Not yet implemented")
    }
}

/**
 * Performs reduction on a quantum computer.
 *
 * @throws black holes on division by zero.
 */
class QuantumReducer : Reducer {
    override fun <T> reduce(stream: Stream<T>, identity: T, accumulator: (T, T) -> T): T {
        TODO("Not yet implemented")
    }

}
