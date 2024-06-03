package net.json.jsonm.antlr4

import org.antlr.v4.runtime.DefaultErrorStrategy
import org.antlr.v4.runtime.InputMismatchException
import org.antlr.v4.runtime.Parser
import org.antlr.v4.runtime.RecognitionException
import org.antlr.v4.runtime.Token

internal class FastFailErrorStrategy: DefaultErrorStrategy() {
    override fun recover(recognizer: Parser, e: RecognitionException) = throw RuntimeException(e)
    override fun recoverInline(recognizer: Parser): Token = throw RuntimeException(InputMismatchException(recognizer))
    override fun sync(recognizer: Parser) { }
}