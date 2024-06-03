package net.json.jsonm

import net.json.jsonm.antlr4.FastFailErrorStrategy
import net.json.jsonm.antlr4.FastFailJsonmLexer
import net.json.jsonm.antlr4.JsonmParser
import org.antlr.v4.runtime.CharStream
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import java.io.InputStream
import java.io.Reader
import java.nio.channels.ReadableByteChannel
import java.nio.charset.Charset
import java.nio.charset.CodingErrorAction
import java.nio.charset.StandardCharsets
import java.nio.file.Path

interface JsonmReader {
    companion object {
        private fun fromCharStream(stream: CharStream): JsonmReader =
            JsonmReaderImpl(
                JsonmParser(CommonTokenStream(FastFailJsonmLexer(stream))).apply {
                    errorHandler = FastFailErrorStrategy()
                }
            )

        fun fromPath(path: Path, charset: Charset = StandardCharsets.UTF_8): JsonmReader =
            fromCharStream(CharStreams.fromPath(path, charset))

        fun fromFileName(fileName: String, charset: Charset = StandardCharsets.UTF_8): JsonmReader =
            fromCharStream(CharStreams.fromFileName(fileName, charset))

        fun fromStream(
            inputStream: InputStream, charset: Charset = StandardCharsets.UTF_8,
            inputSize: Long = -1L
        ): JsonmReader =
            fromCharStream(CharStreams.fromStream(inputStream, charset, inputSize))

        fun fromChannel(
            channel: ReadableByteChannel, charset: Charset = StandardCharsets.UTF_8,
            bufferSize: Int = 4096, decodingErrorAction: CodingErrorAction = CodingErrorAction.REPLACE,
            sourceName: String = "<unknown>", inputSize: Long = -1L
        ): JsonmReader =
            fromCharStream(
                CharStreams.fromChannel(
                    channel,
                    charset,
                    bufferSize,
                    decodingErrorAction,
                    sourceName,
                    inputSize
                )
            )

        fun fromReader(r: Reader, sourceName: String = "<unknown>"): JsonmReader =
            fromCharStream(CharStreams.fromReader(r, sourceName))

        fun fromString(s: String, sourceName: String = "<unknown>"): JsonmReader =
            fromCharStream(CharStreams.fromString(s, sourceName))
    }
    fun readAsJsonMatcher(): JsonMatcher
    fun readAsJson(): Json
}

internal class JsonmReaderImpl(private val jsonmParser: JsonmParser) : JsonmReader {
    override fun readAsJsonMatcher(): JsonMatcher = JsonMatcher(jsonmParser.jsonMatch())
    override fun readAsJson(): Json = Json(jsonmParser.json())
}