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
class JsonReaderTest {
    @TestInstance(PER_CLASS)
    @Nested
    inner class ReadAsJsonTestWithValidInput {
        private fun paramProvider() = listOf(
            """
                { }
            """,
            """
            {
                "firstName": null,
                "lastName": "Smith",
                "weight": 123.5E+10
            }
            """,
            """
                [ ]    
            """,

            """
            [
                "John",
                null
            ]
            """,
            """
               {
                 "firstName": "John",
                 "lastName": "Smith",
                 "children": [
                    {
                        "firstName": "Henry",
                        "lastName": "Smith",
                        "student": true
                    },
                    {
                        "firstName": "Mary",
                        "lastName": "Smith",
                        "student": false
                    }
                 ]
               }
            """
        )

        @ParameterizedTest
        @MethodSource("paramProvider")
        fun `test readAsJson`(jsonStr: String) {
            assertThat(
                runCatching {
                    JsonmReader.fromString(jsonStr).readAsJson()
                }
            ).isSuccess()
        }
    }

    @TestInstance(PER_CLASS)
    @Nested
    inner class ReadAsJsonTestWithInValidInput {
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
                 "firstName": "Smith"
               }
            """
        )

        @ParameterizedTest
        @MethodSource("paramProvider")
        fun `test readAsJson`(jsonStr: String) {
            assertFailure {
                JsonmReader.fromString(jsonStr).readAsJson()
            }
        }
    }
}