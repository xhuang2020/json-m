package net.json.jsonm.antlr4

import org.antlr.v4.runtime.CharStream
import org.antlr.v4.runtime.LexerNoViableAltException

internal class FastFailJsonmLexer(input: CharStream): JsonmLexer(input) {
    override fun recover(e: LexerNoViableAltException) = throw RuntimeException(e)
}