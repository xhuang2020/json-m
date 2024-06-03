package net.json.jsonm

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isSuccess
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import kotlin.Result.Companion.failure
import kotlin.Result.Companion.success

@TestInstance(PER_CLASS)
class JsonMatcherTest {
    private fun paramProvider() = listOf(
        Arguments.of(
            """
                *
            """,
            """
                {
                    "firstName": "John",
                    "lastName": "Smith"
                }
            """,
            success(true)
        ),
        Arguments.of(
            """
                {
                    "firstName"?: "John",
                    "lastName": "Smith"
                }
            """,
            """
                [
                    "John",
                    "Smith"
                ]
            """,
            failure<Boolean>(MismatchException("object mismatched with array at $"))
        ),
        Arguments.of(
            """
                [
                    "John",
                    "Smith"
                ]
            """,
            """
                {
                    "firstName": "John",
                    "lastName": "Smith"
                }
            """,
            failure<Boolean>(MismatchException("array mismatched with object at $"))
        ),
        Arguments.of(
            """
                {
                    "xyz": /[0-9]/,
                    *: *,
                    "firstName"?: "John",
                    "lastName": "Smith"
                }
            """,
            """
                {
                    "firstName": "John",
                    "lastName": "Smith"
                }
            """,
            success(true)
        )
    )

    @ParameterizedTest
    @MethodSource("paramProvider")
    fun `test JsonMatcher match`(jsonMatchStr: String, jsonStr: String, rtn: Any) {
        val jsonMatcher = JsonmReader.fromString(jsonMatchStr).readAsJsonMatcher()
        val json = JsonmReader.fromString(jsonStr).readAsJson()
        val result = jsonMatcher.match(json)
        val expected = rtn as Result<*>
        if (expected.isSuccess) {
            assertThat(result).isSuccess()
        } else {
            val expectedException: Throwable? = expected.exceptionOrNull()
            val resultException: Throwable? = result.exceptionOrNull()
            assertThat(resultException).isNotNull().given { rtnException ->
                assertThat(rtnException.javaClass).isEqualTo(expectedException?.javaClass)
                assertThat(rtnException.message).isEqualTo(expectedException?.message)
            }
        }
    }
}