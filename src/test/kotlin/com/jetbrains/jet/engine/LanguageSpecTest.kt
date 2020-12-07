package com.jetbrains.jet.engine

import com.nhaarman.mockitokotlin2.argThat
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import kotlin.test.Test
import java.util.stream.Collectors
import kotlin.Double.Companion.NaN
import kotlin.Double.Companion.POSITIVE_INFINITY
import kotlin.test.assertFailsWith

internal class LanguageSpecTest {
    @Test
    fun `Top level statements`() {
        "out 2+3".isValid()
        "print \"abc\"".isValid()
        "var x = 2 + 3".isValid()

        "".isInvalid()
        "2+3".isInvalid()
        "\"abc\"".isInvalid()
        "x y -> x + y".isInvalid()
    }

    @Test
    fun `Incomplete input`() {
        "ou".isInvalid()
        "out".isInvalid()
        "out 2 +".isInvalid()

        "var".isInvalid()
        "var a".isInvalid()
        "var a = ".isInvalid()
        "var a = 2 +".isInvalid()
    }

    @Test
    fun `Handling of whitespace`() {
        """
        out
          1
            +
              2
        """ gives 3.0
    }

    @Test
    fun `Variable names`() {
        "var aBc=2".isValid()
        "var aBc_1=2".isValid()
        "var _=2".isValid()
        "var _=1".isValid()

        "var 1=1".isInvalid()
        "變量=1".isInvalid()
    }

    @Test
    fun `Variable visibility`() {
        "var a=2 var b=a+1 var c=a+b out c" gives 5.0

        "out a".isInvalid()
        "var a=a".isInvalid()
        "out a var a = 1".isInvalid()
        "var a = 3 a = 4 out a".isInvalid()
    }

    @Test
    fun `The out statement`() {
        "out 1" gives 1.0
        "out 2+3" gives 5.0
        "out 1" gives 1.0
        "var x = 3 out x * x" gives 9.0

        "out".isInvalid()
        "out \"blah\"".isInvalid()
    }

    @Test
    fun `The print statement`() {
        "print \"\"" gives ""
        "print \"Hello world\"" gives "Hello world"
        "print \"你好，世界\"" gives "你好，世界"
        "print \"blah\\n\"" gives "blah\\n"
        "print \"O'Connor\"" gives "O'Connor"


        "print".isInvalid()
        "print 'blah'".isInvalid()
        "print `blah`".isInvalid()
        "print \"blah".isInvalid()
        "print 1".isInvalid()
        "print \"  \\\" \"".isInvalid()
    }

    @Test
    fun `Number formats`() {
        "out 0" gives 0.0
        "out -0" gives -0.0
        "out 123" gives 123.0
        "out +123" gives 123.0
        "out -123" gives -123.0
        "out 0.123" gives 0.123
        "out 1.23" gives 1.23
        "out -1.23" gives -1.23
        "out 1.23E+2" gives 1.23E+2
        "out 1.23E-2" gives 1.23E-2
        "out 1.23E+002" gives 1.23E+2
        "out 1.23E-002" gives 1.23E-2
        "out -1.23E+2" gives -1.23E+2
        "out -1.23E-2" gives -1.23E-2
        "out 1.23e+2" gives 1.23E+2
        "out 1.23e-2" gives 1.23E-2
        "out -1.23e+2" gives -1.23E+2
        "out -1.23e-2" gives -1.23E-2
        "out 1e10000000000" gives POSITIVE_INFINITY

        "out .".isInvalid()
        "out .1".isInvalid()
        "out 1.23e".isInvalid()
        "out 0xff".isInvalid()

    }

    @Test
    fun `Basic math`() {
        "out 3 + 2" gives 5.0
        "out 3 - 2" gives 1.0
        "out 3 * 2" gives 6.0
        "out 3 / 2" gives 1.5
        "out 3 ^ 2" gives 9.0
        "out 2 ^ -3" gives 0.125
        "out 1 / 0" gives POSITIVE_INFINITY
        "out 0 / 0" gives NaN
    }

    @Test
    fun `Operator precedence `() {
        "out 10 - 2 + 3" gives 11.0
        "out 2 * 3 - 4" gives 2.0
        "out 2 + 3 * 4" gives 14.0
        "out (2 + 3) * 4" gives 20.0
        "out 2 * (3 + 4)" gives 14.0
        "out 3 * 4^-1" gives 0.75
        "out 3 / 4^-1" gives 12.0
        "out -2^2" gives 4.0  // weird but the grammar treats -2 as an atom.
        "out -1 * 2^2" gives -4.0
    }

    @Test
    fun `Infix negation is not supported`() {
        //  Not to be mistaken with negative number literals.
        //  Fix the grammar if this is undesired.
        "var x = 1 out -x".isInvalid()
        "out -(1)".isInvalid()
    }

    @Test
    fun `Range expressions`() {
        "out {1,3}".produces(1.0, 2.0, 3.0)
        "out {1,1}".produces(1.0)
        "out {2,1}".produces()
        "out {-2.5,2.5}".produces(-2.0, -1.0, 0.0, 1.0, 2.0)
        "var n = 3 out {n, n+2}".produces(3.0, 4.0, 5.0)

        "var range={1, 1E18} print \"Ranges are light-weight\"" gives "Ranges are light-weight"

        "{}".isInvalid()
        "{1}".isInvalid()
        "{1,2,3}".isInvalid()
    }

    @Test
    fun `The map function`() {
        "out map({1, 3}, x -> x * x)".produces(1.0, 4.0, 9.0)
        "out map(map({1, 3}, x -> x * x), y -> y + 1)".produces(2.0, 5.0, 10.0)
        "var seq = map({1, 1E18}, x -> x^x) print \"Map is lazy\"" gives "Map is lazy"

        "out map({1, 10}, n -> {1, n})".isValid() // A sequence of sequences

        "out map".isInvalid()
        "var map = 1".isInvalid()
        "var out map({1,3})".isInvalid()
        "out map({1,3}, x->x*x, 1)".isInvalid()
        "var n = 3 out map({1,3}, x->x^n)".isInvalid()
    }

    @Test
    fun `The reduce function`() {
        "out reduce({1,100}, 0, acc x -> acc + x)" gives 5050.0
        "out reduce(map({1,1000000}, x-> 1), 0, acc x -> acc + x)" gives 1000000.0

        "out reduce".isInvalid()
        "var reduce = 1".isInvalid()
        "out reduce({1,3}, acc x -> acc + x)".isInvalid()
        "out reduce({1,3}, 0, acc x -> acc + x, 100)".isInvalid()
        "out reduce({1,3}, 0, x -> x + x)".isInvalid()
        "out reduce({1,3}, 0, x x -> x + x)".isInvalid()
        "out reduce({1,3}, 0, x y z -> x + y + z)".isInvalid()
        "var n = 3 out reduce({1,3}, 0, acc x -> n)".isInvalid()
    }

    @Test
    fun `Type cast errors`() {
        "out 2 + {3 ,4}".errors()
        "out {2, {3, 4}}".errors()
        "out {2, 3} * {3, 4}".errors()
    }


    private val testReducer = SequentialReducer()

    fun String.isValid() {
        parse(this)
    }

    fun String.isInvalid() {
        assertFailsWith<SyntaxException> { parse(this) }
    }

    infix fun String.gives(expectedResult: Any) {
        val rt = mock<RT> {
            on { reducer } doReturn testReducer
        }
        parse(this).runAndWait(rt)

        verify(rt).out(expectedResult)
    }

    fun String.produces(vararg elements: Any) {
        val rt = mock<RT> {
            on { reducer } doReturn testReducer
        }
        parse(this).runAndWait(rt)

        verify(rt).out(argThat { x ->
            (x as LazilyMappedRange<*>).stream().collect(Collectors.toList()) == listOf(*elements)
        })
    }

    fun String.errors() {
        val rt = mock<RT> {
            on { reducer } doReturn testReducer
        }
        assertFailsWith<ExecutionException> { parse(this).runAndWait(rt) }
    }
}
