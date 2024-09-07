package net.json.jsonm

import assertk.assertThat
import assertk.assertions.isEqualTo
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
            MatchResult(true)
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
            MatchResult(true)
        ),
        Arguments.of(
            """
                {
                    *: *,
                    "firstName"?: "John",
                    "age": integer,
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
            MatchResult(true)
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
            MatchResult(true)
        ),
        Arguments.of(
            """
                [(float(-2.0, 4E10))+]  // Singe line comment will be ignored
            """,
            """
               [0.0, -1.1, 100.2] 
            """,
            MatchResult(true)
        ),
        Arguments.of(
            """
                {
                    !"firstName": *,
                    "lastName": "Smith",
                    *: *
                }
            """,
            """
                {
                    "lastName": "Smith",
                    "sex": "Male"
                }
            """,
            MatchResult(true)
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
            MatchResult(false, "object mismatched with array at $")
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
            MatchResult(false, "array mismatched with object at $")
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
            MatchResult(false, "required field \"lastName\" missing from $")
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
            MatchResult(false, "unexpected field \"second_lastName\" in object $")
        ),
        Arguments.of(
            """
                {
                    !"firstName": *,
                    "lastName": "Smith",
                    *: *
                }
            """,
            """
                {
                    "firstName": "John",
                    "lastName": "Smith",
                    "sex": "Male"
                }
            """,
            MatchResult(false, "The field \"firstName\" cannot appear in $")
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
            MatchResult(false, "expect float at $.\"weight\"")
        ),
        Arguments.of(
            """
                {
                    "name": *,
                    "age": integer
                }
            """,
            """
                {
                    "name": "Smith",
                    "age": 40.1
                }
            """,
            MatchResult(false, "expect integer at $.\"age\"")
        ),
        Arguments.of(
            """
                {
                    "name": *,
                    "age": integer[1, 100)
                }
            """,
            """
                {
                    "name": "Smith",
                    "age": 100
                }
            """,
            MatchResult(false, "100 is beyond the range [1,100) at $.\"age\"")
        ),
        Arguments.of(
            """
                {
                    "name": *,
                    "age": integer[1,100)
                }
            """,
            """
                {
                    "name": "Smith",
                    "age": 0
                }
            """,
            MatchResult(false, "0 is beyond the range [1,100) at $.\"age\"")
        ),
        Arguments.of(
            """
                [(float(-2.0, 4E10))+]
            """,
            """
               [0.0, -2.0, 100.2] 
            """,
            MatchResult(false, "unexpected value in array $[1]")
        ),
        Arguments.of(
            """
              {
                 "weight": float(-2.0, 4E10)
              }
            """,
            """
               {
                 "weight": -2.0
               } 
            """,
            MatchResult(false, "-2.0 is beyond the range (-2.0,4E10) at $.\"weight\"")
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
            MatchResult(false, "required field \"lastName\" missing from $.\"son\"")
        ),
        Arguments.of(
            """
               [
                   /[a-zA-Z]+/,
                   integer,
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
            MatchResult(true)
        ),
        Arguments.of(
            """
               [
                   /[a-zA-Z]+/,
                   integer,
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
            MatchResult(false, "unexpected value in array $[1]")
        ),
        Arguments.of(
            """
               [
                   /[a-zA-Z]+/,
                   integer[123,),
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
            MatchResult(false, "unexpected value in array $[2]")
        ),
        Arguments.of(
            """
               [
                   /[a-zA-Z]+/,
                   (integer)+
               ]
            """,
            """
                [
                   "firstEntry"
                ]
            """,
            MatchResult(false, "required value missed in array $[1]")
        ),
        Arguments.of(
            """
               [
                   /[a-zA-Z]+/,
                   (integer(100,200))+
               ]
            """,
            """
                [
                   "firstEntry",
                   123
                ]
            """,
            MatchResult(true)
        ),
        Arguments.of(
            """
               [
                   /[a-zA-Z]+/,
                   (integer)*
               ](1, 2)
            """,
            """
                [
                   "firstEntry"
                ]
            """,
            MatchResult(true)
        ),
        Arguments.of(
            """
               [
                   /[a-zA-Z]+/,
                   (integer)*
               ](1, 2)
            """,
            """
                [
                   "firstEntry",
                   "secondEntry",
                   "thirdEntry"
                ]
            """,
            MatchResult(false, "required maximum array size=2, but the actual array size=3 in array $")
        )
    )

    @ParameterizedTest
    @MethodSource("paramProvider")
    fun `test JsonMatcher match`(jsonMatchStr: String, jsonStr: String, expected: MatchResult) {
        assertThat(jsonMatchStr matchJson jsonStr).isEqualTo(expected)
    }
}