package com.jetbrains.jet.engine

import org.antlr.v4.runtime.*


/**
 * Parses the code and return an executable program
 *
 * @param code Source code
 * @return An instance of [com.jetbrains.jet.engine.Program]
 *
 * @throws SyntaxException in case of syntax errors
 */
fun parse(code: String): Program = parse(CharStreams.fromString(code))

private fun parse(code: CharStream): Program {
    val errors = mutableListOf<SyntaxError>()
    val errorListener = object : BaseErrorListener() {
        override fun syntaxError(
            recognizer: Recognizer<*, *>?,
            offendingSymbol: Any?,
            line: Int,
            charPositionInLine: Int,
            msg: String?,
            e: RecognitionException?
        ) {
            errors.add(SyntaxError(line, charPositionInLine + 1, msg ?: "Syntax error"))
        }
    }
    val lexer = JetLexer(code).withErrorListener(errorListener)
    val parser = JetParser(CommonTokenStream(lexer)).withErrorListener(errorListener)

    val ast = parser.program()

    return if (errors.isEmpty()) Program(ast)
    else throw SyntaxException(*errors.toTypedArray())
}

private fun <T : Recognizer<*, *>> T.withErrorListener(errorListener: ANTLRErrorListener): T {
    removeErrorListeners() // Disable console output
    addErrorListener(errorListener)
    return this
}
