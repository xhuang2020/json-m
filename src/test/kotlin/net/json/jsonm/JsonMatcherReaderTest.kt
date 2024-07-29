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
                
                [ ]  // Singe line comment will be ignored
            """,
            """
                [1, 2, * ]    
            """,
            """
                /* 
                    Multi-line comment will be ignored
                */
                [*](3,4)
            """,
            """
               {
                 "firstName"?: "John",
                 "student": boolean | null,
                 *: [*](6)
               }
            """,
            """
                {
                    *: *,
                    "firstName"?: "John",
                    "lastName": "Smith"
                }
            """,
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
               {                        /* the target json must be a json object with the following restrictions: */
                  "lastName": "Smith", // the field "lastName" is required and its value must be "Smith"
                  "firstName": /[a-ZA-Z]+/, // the field "firstName" is required and its value must match with the regex [a-ZA-Z]+
                  "middleName"?: string,   // the field "middleName" is optional and its value can be any text
                  "gender": "male"|"female"|"other", // the field "gender" is required and its value must be one of "male", "female" and "other" 
                  "employed": boolean|null,  // the field "employed" is required and its value must be one of true, false and null
                  "age": int,  // the field "age" is required and its value must be an non-negative integer
                  "weight"?: number,  // the field "weight" is optional and its value must be a number if present
                  "address": [(string)+](, 6), // the field "address" is required and its value must be a string array with the maximum size of 6
                  "children"?: [               // the field "children" is optional and its value must be an array of json object
                        ({                    // which contains at least the field "lastName" with the value being "Smith" and optionally
                           "lastName": "Smith",       // any other fields with any values
                           *: *
                        })*
                  ]
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
            [
                *,
                "John"
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