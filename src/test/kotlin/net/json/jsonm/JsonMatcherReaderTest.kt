package net.json.jsonm

import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.isSuccess
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

@TestInstance(PER_CLASS)
class JsonMatcherReaderTest {
    @TestInstance(PER_CLASS)
    @Nested
    inner class ReadAsJsonMatcherTestWithValidInput {
        private fun paramProvider() = listOf(
            "*",
            """
                { }
            """,
            """
            {
                "firstName"?: /[a-zA-Z]*/,
                "lastName": "Smith",
                "weight": number,
                "children"?: [{*:*}](,)
            }
            """,
            """
                [ ]    
            """,

            """
                [*](3,4)
            """,
            """
               {
                 "firstName"?: "John",
                 "student": boolean | null,
                 *: [*]
               }
            """,
            """
                {
                    *: *,
                    "firstName"?: "John",
                    "lastName": "Smith"
                }
            """
        )

        @ParameterizedTest
        @MethodSource("paramProvider")
        fun `test readAsJsonMatch`(jsonStr: String) {
            assertThat(
                runCatching {
                    JsonmReader.fromString(jsonStr).readAsJsonMatcher()
                }
            ).isSuccess()
        }
    }

    @TestInstance(PER_CLASS)
    @Nested
    inner class ReadAsJsonMatcherTestWithInValidInput {
        private fun paramProvider() = listOf(
            """
                
            """,
            """
            {
                "firstName": null,
                "lastName": "Smith,
                "weight": 123.5E+10
            }
            """,
            """
                { ]    
            """,

            """
            [
                "John"
                null
            ]
            """,
            """
               {
                 "firstName": "John",
                 "lastName: "Smith"
               }
            """,
            """
               {
                 "firstName": "John",
                 "children": [
                    {
                        "firstName": "John",
                        "lastName": "Smith"
                    },
                    {
                        "firstName": "John",
                        "firstName": "Smith"
                    }
                 ]
               }
            """
        )

        @ParameterizedTest
        @MethodSource("paramProvider")
        fun `test readAsJsonMatcher`(jsonStr: String) {
            assertFailure {
                JsonmReader.fromString(jsonStr).readAsJsonMatcher()
            }
        }
    }
}