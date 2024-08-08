package net.json.jsonm

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isSuccess
import kotlin.Result.Companion.failure
import kotlin.Result.Companion.success
import net.json.jsonm.JsonMatcher.Companion.matchJson
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

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
                    "xyz": /[0-9]+/,
                    *: *,
                    "firstName"?: "John",
                    "lastName": "Smith"
                }
            """,
            """
                {
                    "lastName": "Smith",
                    "xyz": "123",
                    "son": { }
                }
            """,
            success(true)
        ),
        Arguments.of(
            """
                {
                    *: *,
                    "firstName"?: "John",
                    "age": int,
                    "employed": boolean,
                    "son": {
                        "firstName"?: string,
                        "lastName": /J[a-z]+/
                    }
                }
            """,
            """
                {
                    "firstName": "John",
                    "age": 50, 
                    "employed": true,
                    "son": {
                        "firstName": "Smith",
                        "lastName": "John"
                    }
                }
            """,
            success(true)
        ),
        Arguments.of(
            """
                {
                    *: *
                }
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
                    *: *,
                    "firstName"?: "John",
                    "lastName": "Smith"
                }
            """,
            """
                {
                    "firstName": "John"
                }
            """,
            failure<Boolean>(MismatchException("required field \"lastName\" missing from $"))
        ),
        Arguments.of(
            """
                {
                    "firstName"?: "John",
                    "lastName": "Smith"
                }
            """,
            """
                {
                    "lastName": "Smith",
                    "second_lastName": "John"
                }
            """,
            failure<Boolean>(MismatchException("unexpected field \"second_lastName\" in object $"))
        ),
        Arguments.of(
            """
                {
                    "name": *,
                    "weight": float
                }
            """,
            """
                {
                    "name": "Smith",
                    "weight": 123
                }
            """,
            failure<Boolean>(MismatchException("expect float at $.\"weight\""))
        ),
        Arguments.of(
            """
                {
                    "name": *,
                    "age": int
                }
            """,
            """
                {
                    "name": "Smith",
                    "age": 40.1
                }
            """,
            failure<Boolean>(MismatchException("expect integer at $.\"age\""))
        ),
        Arguments.of(
            """
                {
                    "firstName"?: "John",
                    "son": {
                        "firstName"?: *,
                        "lastName": *
                    }
                }
            """,
            """
                {
                    "firstName": "John",
                    "son": {
                        "firstName": "Smith"
                    }
                }
            """,
            failure<Boolean>(MismatchException("required field \"lastName\" missing from $.\"son\""))
        ),
        Arguments.of(
            """
               [
                   /[a-zA-Z]+/,
                   int,
                   (boolean)+,
                   (null)*,
                   (number)+,
                   (boolean|null)?,
                   *
               ]
            """,
            """
                [
                   "firstEntry",
                   123,
                   true,
                   123.3E+10,
                   234,
                   false
                ]
            """,
            success(true)
        ),
        Arguments.of(
            """
               [
                   /[a-zA-Z]+/,
                   int,
                   (boolean)+,
                   (null)*,
                   (number)+,
                   (boolean|null)?,
                   *
               ]
            """,
            """
                [
                   "firstEntry",
                   true,
                   123.3E+10,
                   234,
                   false
                ]
            """,
            failure<Boolean>(MismatchException("unexpected value in array $[1]"))
        ),
        Arguments.of(
            """
               [
                   /[a-zA-Z]+/,
                   int,
                   (boolean)+,
                   (null)*,
                   (number)+,
                   (boolean|null)?,
                   *
               ]
            """,
            """
                [
                   "firstEntry",
                   123,
                   123.3E+10,
                   234,
                   false
                ]
            """,
            failure<Boolean>(MismatchException("unexpected value in array $[2]"))
        ),
        Arguments.of(
            """
               [
                   /[a-zA-Z]+/,
                   (int)+
               ]
            """,
            """
                [
                   "firstEntry"
                ]
            """,
            failure<Boolean>(MismatchException("required value missed in array $[1]"))
        ),
        Arguments.of(
            """
               [
                   /[a-zA-Z]+/,
                   (int)+
               ]
            """,
            """
                [
                   "firstEntry",
                   123
                ]
            """,
            success(true)
        ),
        Arguments.of(
            """
               [
                   /[a-zA-Z]+/,
                   (int)*
               ](1, 2)
            """,
            """
                [
                   "firstEntry"
                ]
            """,
            success(true)
        ),
        Arguments.of(
            """
               [
                   /[a-zA-Z]+/,
                   (int)*
               ](1, 2)
            """,
            """
                [
                   "firstEntry",
                   "secondEntry",
                   "thirdEntry"
                ]
            """,
            failure<Boolean>(MismatchException("required maximum array size=2, but the actual array size=3 in array $"))
        )
    )

    @ParameterizedTest
    @MethodSource("paramProvider")
    fun `test JsonMatcher match`(jsonMatchStr: String, jsonStr: String, rtn: Any) {
        val result = jsonMatchStr matchJson jsonStr
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