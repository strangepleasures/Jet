package com.jetbrains.jet.engine

import com.jetbrains.jet.engine.JetParser.*
import org.antlr.v4.runtime.ParserRuleContext
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor
import java.util.concurrent.ForkJoinPool.commonPool
import kotlin.math.pow

typealias VariableIndex = MutableMap<String, Int>
typealias Variables = Array<Any?>
typealias Params = Array<out Any?>

data class SyntaxError(val line: Int, val column: Int, val message: String)

class SyntaxException(vararg val errors: SyntaxError) : Exception()

class ExecutionException(val line: Int, val column: Int, message: String) : RuntimeException(message) {
    override val message: String
        get() = super.message!!
}

/**
 * Run-time environment
 */
interface RT {
    /**
     * Used for returning/printing results by `out` or `print`
     */
    fun out(o: Any)

    /**
     * A [Reducer] used for executing `reduce` function
     */
    val reducer: Reducer
}

/**
 * A top-level class representing a "compiled" program
 */
class Program @Throws(SyntaxException::class) constructor(ast: ProgramContext) {
    private val statements: Array<Statement>
    private val nvars: Int

    init {
        val variableIndex: VariableIndex = mutableMapOf()
        statements = ast.statement()
            .map {
                when (it) {
                    is VarDeclarationStatementContext -> VariableDeclarationStatement(it, variableIndex)
                    is OutStatementContext -> OutStatement(it, variableIndex)
                    is PrintStatementContext -> PrintStatement(it)
                    else -> throw AssertionError("Unrecognised statement: ${it::class.simpleName}")
                }
            }
            .toTypedArray()
        nvars = variableIndex.size
    }

    /**
     * Runs the program in background on the provided executor.
     *
     * @param rt The run-time environment to use
     * @param executor The executor used for running the main job. Child reduce jobs might be executed elsewhere.
     *
     * @return A [CompletableFuture] allowing to control program's execution. Please note that a program doesn't
     * produce any result, e.g. a number, except to values submitted to [RT.out].
     *
     * @see [checkForCancellation]
     */
    fun run(rt: RT, executor: Executor = commonPool()): CompletableFuture<Unit> {
        val future = CompletableFuture<Unit>()
        executor.execute {
            val vars: Variables = arrayOfNulls(nvars)
            try {
                statements.forEach { it.execute(vars, rt, future) }
                future.complete(Unit)
            } catch (e: Throwable) {
                future.completeExceptionally(e)
            }
        }
        return future
    }

    /**
     * Runs the program in a blocking way.
     * Main job will be executed on the current thread. Child reduce jobs might be executed elsewhere.
     */
    fun runAndWait(rt: RT) {
        try {
            run(rt) { it.run() }.get()
        } catch (e: java.util.concurrent.ExecutionException) {
            throw e.cause!!
        }
    }
}

sealed class Statement(ast: StatementContext) {
    val line = ast.start.line
    val column = ast.start.charPositionInLine + 1
    fun execute(variables: Variables, rt: RT, future: CompletableFuture<Unit>) {
        try {
            exec(variables, rt, future)
        } catch (e: Exception) {
            throw when (e) {
                is ExecutionException -> e
                else -> ExecutionException(line, column, "Run-time exception ${e.message ?: e}")
            }
        }
    }

    protected abstract fun exec(variables: Variables, rt: RT, future: CompletableFuture<Unit>)
}

class VariableDeclarationStatement(ast: VarDeclarationStatementContext, variableIndex: VariableIndex) : Statement(ast) {
    private val address = variableIndex.size
    private val rhs = expression(ast.expression(), variableIndex)

    init {
        if (variableIndex.put(ast.ID().text, address) != null) {
            throw SyntaxException(SyntaxError(line, column, "Duplicate declaration: ${ast.ID().text}"))
        }
    }

    override fun exec(variables: Variables, rt: RT, future: CompletableFuture<Unit>) {
        variables[address] = rhs.evaluate(variables, rt, future)
    }

}

class OutStatement(ast: OutStatementContext, variableIndex: VariableIndex) : Statement(ast) {
    private val expr = expression(ast.expression(), variableIndex)

    override fun exec(variables: Variables, rt: RT, future: CompletableFuture<Unit>) {
        rt.out(expr.evaluate(variables, rt, future))
    }

}

class PrintStatement(ast: PrintStatementContext) : Statement(ast) {
    private val message = ast.STRING().text.drop(1).dropLast(1)

    override fun exec(variables: Variables, rt: RT, future: CompletableFuture<Unit>) {
        rt.out(message)
    }
}

fun expression(ast: ExpressionContext, variableIndex: VariableIndex): Expression =
    when (ast) {
        is ParenExpressionContext -> expression(ast.expression(), variableIndex)
        is NumberLiteralContext -> Constant(ast, ast.text.toDouble())
        is VarReferenceContext -> VarReference(ast, variableIndex)
        is BinaryOperationContext -> when (ast.operator.type) {
            PLUS -> Plus(ast, variableIndex)
            MINUS -> Minus(ast, variableIndex)
            MULTIPLICATION -> Multiplication(ast, variableIndex)
            DIVISION -> Division(ast, variableIndex)
            POWER -> Power(ast, variableIndex)
            COMMA -> Range(ast, variableIndex)
            else -> throw AssertionError("Unsupported operation: ${ast.operator.text}")
        }
        is MapExpressionContext -> MapExpression(ast, variableIndex)
        is ReduceExpressionContext -> ReduceExpression(ast, variableIndex)
        else -> throw AssertionError("Unrecognised expression: ${ast::class.simpleName}")
    }


sealed class Expression(ast: ParserRuleContext) {
    val line = ast.start.line
    val column = ast.start.charPositionInLine + 1

    fun evaluate(params: Params, rt: RT, future: CompletableFuture<Unit>): Any {
        try {
            return eval(params, rt, future)
        } catch (e: Exception) {
            throw when (e) {
                is ExecutionException -> e
                else -> ExecutionException(line, column, "Run-time exception ${e.message ?: e}")
            }
        }
    }

    protected abstract fun eval(params: Params, rt: RT, future: CompletableFuture<Unit>): Any

    inline fun <reified T> evaluateAs(params: Params, rt: RT, future: CompletableFuture<Unit>): T =
        evaluate(params, rt, future) as? T ?: throw ExecutionException(line, column, "Expected a ${T::class.simpleName}.")
}

class Constant(ast: ParserRuleContext, private val value: Any) : Expression(ast) {
    override fun eval(params: Params, rt: RT, future: CompletableFuture<Unit>): Any = value
}

class VarReference(ast: VarReferenceContext, variableIndex: VariableIndex) : Expression(ast) {
    private val address = variableIndex[ast.text] ?: throw SyntaxException(
        SyntaxError(line, column, "Undefined variable: ${ast.text}")
    )

    override fun eval(params: Params, rt: RT, future: CompletableFuture<Unit>): Any = params[address]!!
}

sealed class BinaryExpression(ast: BinaryOperationContext, variableIndex: VariableIndex) : Expression(ast) {
    private val left = expression(ast.left, variableIndex)
    private val right = expression(ast.right, variableIndex)

    override fun eval(params: Params, rt: RT, future: CompletableFuture<Unit>): Any =
        op(left.evaluateAs(params, rt, future), right.evaluateAs(params, rt, future))

    protected abstract fun op(lhs: Double, rhs: Double): Any
}

class Plus(ast: BinaryOperationContext, variableIndex: VariableIndex) : BinaryExpression(ast, variableIndex) {
    override fun op(lhs: Double, rhs: Double) = lhs + rhs
}

class Minus(ast: BinaryOperationContext, variableIndex: VariableIndex) : BinaryExpression(ast, variableIndex) {
    override fun op(lhs: Double, rhs: Double) = lhs - rhs
}

class Multiplication(ast: BinaryOperationContext, variableIndex: VariableIndex) : BinaryExpression(ast, variableIndex) {
    override fun op(lhs: Double, rhs: Double) = lhs * rhs
}

class Division(ast: BinaryOperationContext, variableIndex: VariableIndex) : BinaryExpression(ast, variableIndex) {
    override fun op(lhs: Double, rhs: Double) = lhs / rhs
}

class Power(ast: BinaryOperationContext, variableIndex: VariableIndex) : BinaryExpression(ast, variableIndex) {
    override fun op(lhs: Double, rhs: Double) = lhs.pow(rhs)
}

class Range(ast: BinaryOperationContext, variableIndex: VariableIndex) : BinaryExpression(ast, variableIndex) {
    override fun op(lhs: Double, rhs: Double) = LazilyMappedRange(lhs.toLong(), rhs.toLong()) { it.toDouble() }
}

class MapExpression(ast: MapExpressionContext, variableIndex: VariableIndex) : Expression(ast) {
    private val sequenceExpr = expression(ast.sequence, variableIndex)
    private val lambdaExpression = LambdaExpression(ast.lambda()).expectedNumberOfArguments(1)

    override fun eval(params: Params, rt: RT, future: CompletableFuture<Unit>): Any {
        val lambda: Lambda = lambdaExpression.evaluate(params, rt, future) as Lambda
        return (sequenceExpr.evaluate(params, rt, future) as LazilyMappedRange<*>).map { lambda(it!!) }
    }
}

class ReduceExpression(ast: ReduceExpressionContext, variableIndex: VariableIndex) : Expression(ast) {
    private val sequenceExpr = expression(ast.sequence, variableIndex)
    private val identityExpression = expression(ast.unit, variableIndex)
    private val lambdaExpression = LambdaExpression(ast.lambda()).expectedNumberOfArguments(2)

    override fun eval(params: Params, rt: RT, future: CompletableFuture<Unit>): Any {
        val stream = sequenceExpr.evaluateAs<LazilyMappedRange<Any>>(params, rt, future).stream()
        val identity = identityExpression.evaluate(params, rt, future)
        val accumulator = lambdaExpression.evaluateAs<Lambda>(params, rt, future)

        val cancellableAccumulator = { acc: Any, x: Any ->
            // This is currently the only place where we need to check for cancellation
            checkForCancellation(future)

            accumulator(acc, x)
        }

        return rt.reducer.reduce(stream, identity, cancellableAccumulator)
    }
}

class LambdaExpression(ast: LambdaContext) : Expression(ast) {
    val nargs: Int
    private val body: Expression

    init {
        val variableIndex = mutableMapOf<String, Int>()
        ast.argument().forEachIndexed { i, a ->
            if (variableIndex.put(a.text, i) != null) {
                throw SyntaxException(SyntaxError(line, column, "Duplicate argument name: ${a.text}"))
            }
        }

        nargs = variableIndex.size
        body = expression(ast.expression(), variableIndex)
    }

    override fun eval(params: Params, rt: RT, future: CompletableFuture<Unit>) = Lambda(body, rt, future)

    fun expectedNumberOfArguments(n: Int): LambdaExpression {
        return if (nargs == n) this
        else   throw SyntaxException(SyntaxError(line, column, "Invalid number of arguments. Expected $n, got ${nargs}."))
    }
}

class Lambda(private val body: Expression, private val rt: RT, private val future: CompletableFuture<Unit>) {
    operator fun invoke(vararg params: Any): Any = body.evaluate(params, rt, future)
}

fun checkForCancellation(future: CompletableFuture<Unit>) {
    if (future.isCancelled || future.isCompletedExceptionally) {
        throw RuntimeException("I'm an orphan without future. Gonna kill myself. Nobody cares anyway.")
    }
}

