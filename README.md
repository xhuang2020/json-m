# What is json-m
json-m is a language to specify matching patterns for Json objects and arrays.
json-m vs json is just like regex vs text. 
json-m syntax is an extension of [json syntax](https://www.json.org/json-en.html) 
with added meta characters to specify matching patterns.
The current implementation of json-m is based on [Antlr 4](https://www.antlr.org/index.html)
and is implemented with [Kotlin](https://kotlinlang.org) language.

# API
The main API is the infix function "matchJson" defined in the package 
"net.json.jsonm.JsonMatcher.Companion" as below (in Kotlin language):
```
import net.json.jsonm.JsonMatcher.Companion.matchJson

fun test() {
   val jsonMatchString = "...some json-m pattern..."
   val json = "...some json..."
   val result: Result<Boolean> = jsonMatchString matchJson json
   ...
}
```

# Patterns

### Exact match
Any json object or array is by definition a json-m pattern exactly matching itself.
```
fun test() {
   val jsonMatchString = """
        {
            "firstName": "Joh",
            "lastName": "Smith"
        }
   """
   val json = """
        {
            "firstName": "Joh",
            "lastName": "Smith"
        }
   """
   assertThat(jsonMatchString matchJson json).isSuccess()
}
```

### Wildcard match
A character "*" matches with any json object and array.
```
fun test() {
   val jsonMatchString = "*"
   val json = """
        {
            "firstName": "Joh",
            "lastName": "Smith"
        }
   """
   assertThat(jsonMatchString matchJson json).isSuccess()
}
```
### Object match
In order to match with a json object, 
the json-m pattern must start with "{" and end with "}", just like a json object.

#### Matching with empty objects
If no key-value pairs are specified between "{" and "}", 
the json-m pattern can only match with an empty json object.
```
fun test() {
   val jsonMatchString = "{ }"
   val json = """
        {
            "firstName": "Joh",
            "lastName": "Smith"
        }
   """
   assertThat(jsonMatchString matchJson json).isFailure()
   assertThat(jsonMatchString matchJson "{}").isSuccess()
}
```

#### Key-value pairs within objects
If key-value pairs are specified between "{" and "}", 
they are matched with the corresponding key-value pairs in the json object as below:

- If the key is a string enclosed by double quotes, it matches with the same key in the target json object.
```
fun test() {
   val jsonMatchString = """
        {
            "firstName": "John" 
        }
        """
   val json = """
        {
            "firstName": "John"
        }
   """
   assertThat(jsonMatchString matchJson json).isSuccess()
}
```

- If the key is a string enclosed by double quotes and followed by a question mark "?", 
it means the key is optional in the target json object.
```
fun test() {
   val jsonMatchString = """
        {
            "firstName"?: "John" 
        }
        """
   val json = """
        {
            "firstName": "John"
        }
   """
   val jsonWithNoKeys = "{}"
   assertThat(jsonMatchString matchJson json).isSuccess()
   assertThat(jsonMatchString matchJson jsonWithNoKeys).isSuccess()
}
```

- If the key is the wildcard character "*", it matches with any key in the target json object, i.e.
the pattern allows any zero, one or more keys appearing in the target json object. 
This provides a mechanism to allow unspecified keys in json objects.
```
fun test() {
   val jsonMatchString = """
        {
            "firstName": "John",
            *: * 
        }
        """
   val jsonWithLastName = """
        {
            "firstName": "John",
            "lastName": "Smith",
            "middleName": "Dan"
        }
   """
   val jsonWithNoLastName = """
        {
            "firstName": "John"
        }
   """
   assertThat(jsonMatchString matchJson jsonWithLastName).isSuccess()
   assertThat(jsonMatchString matchJson jsonWithNoLastName).isSuccess()
}
```
If no wildcard keys appear in the json-m pattern, 
no "lastName" and "middleName" are allowed in the target json object in the above example:
```
fun test() {
   val jsonMatchString = """
        {
            "firstName": "John"
        }
        """
   val jsonWithLastName = """
        {
            "firstName": "John",
            "lastName": "Smith",
            "middleName": "Dan"
        }
   """
   val jsonWithNoLastName = """
        {
            "firstName": "John"
        }
   """
   assertThat(jsonMatchString matchJson jsonWithLastName).isFailure()
   assertThat(jsonMatchString matchJson jsonWithNoLastName).isSuccess()
}
```
Besides the keys, the values in the key-value pairs must also match with the corresponding values 
in the target json object, and it follows the same value matching criteria as defined in [here](#value-match):
```
fun test() {
   val jsonMatchString = """
        {
            "firstName": "John"
        }
        """
   val jsonWithCorrectFirstName = """
        {
            "firstName": "John"
        }
   """
   val jsonWithWrongFirstName = """
        {
            "firstName": "Johnn"
        }
   """
   assertThat(jsonMatchString matchJson jsonWithCorrectFirstName).isSuccess()
   assertThat(jsonMatchString matchJson jsonWithWrongFirstName).isFailure()
}
```

### Array match
In order to match with a json array,
the json-m pattern must start with "[" and end with "]", just like a json array.

#### Matching with the empty array
If no entries are specified between "[" and "]",
the json-m pattern can only match with the empty json array.
```
fun test() {
   val jsonMatchString = "[ ]"
   val emptyArray = "[]"
   val nonEmptyArray = """
        ["John"]
    """
   assertThat(jsonMatchString matchJson emptyArray).isSuccess()
   assertThat(jsonMatchString matchJson nonEmptyArray).isFailure()
}
```

#### Matching with entries in arrays
If entries are specified between "[" and "]",
they are matched with the corresponding entries in the json array as below:
- If the entry is a value, the value must match with the corresponding value of the same index
  in the target json array, and it follows the same value matching criteria as defined in [here](#value-match):
    ```
    fun test() {
       val jsonMatchString = """
        ["John"]
       """
       val array = """
            ["John"]
       """
       assertThat(jsonMatchString matchJson array).isSuccess()
    }
    ```
- If the entry is a value surrounded with "(" and ")", it can be appended with one of the 3 qualifiers:
    1. "?": the entry is optional in the target json array
    2. "+": the entry must appear one or more times in the target json array
    3. "*": the entry can appear zero, one or more times in the target json array
    ```
    fun test() {
       val jsonMatchString = """
        [("John")?, ("Smith")+, ("Dan")*]
       """
       val array = """
            ["John", "Smith", "Dan"]
       """
       assertThat(jsonMatchString matchJson array).isSuccess()
  
       val arrayWithSmithOnly = """
            ["Smith"]
       """
      assertThat(jsonMatchString matchJson arrayWithSmithOnly).isSuccess()
    }
    ```
- The entry can be a wildcard character "*", which must appear at the end of the array. 
   It means the array can optionally contain zero, one or more entries in the target json array:  
  ```
  fun test() {
    val jsonMatchString = """
    ["Smith", *]
    """
    val array = """
    ["Smith", "Dan"]
    """
    assertThat(jsonMatchString matchJson array).isSuccess()
  }
  ```
- The entry can be several values delimited by the character "|", 
  which means the corresponding entry in the target array must match with one of the several values: 
  ```
  fun test() {
    val jsonMatchString = """
    ["Smith"|"John"]
    """
    val arrayOfSmith = """
    ["Smith"]
    """
    val arrayOfJohn = """
    ["John"]
    """
    assertThat(jsonMatchString matchJson arrayOfSmith).isSuccess()
    assertThat(jsonMatchString matchJson arrayOfJohn).isSuccess()
  }
  ```
#### Matching array size
You can specify the size requirement in the array pattern by appending the size range after the array pattern
  - Exact size: appending an integer surrounded by a pair of parentheses
    ```
    fun test() {
        val jsonMatchString = """
            [1, *](4)
        """
        val arrayOfFour = """
            [1, 2, 3, 4]
        """
        val array = """
            [1, 2]
        """
        assertThat(jsonMatchString matchJson arrayOfFour).isSuccess()
        assertThat(jsonMatchString matchJson array).isFailure()
    }
    ```
  - Size range: appending a pair of integers surrounded by a pair of parentheses, 
     where the first integer specifies the lower bound of the target array size 
     and the second integer specifiers the upper bound the target array size
    ```
    fun test() {
        val jsonMatchString = """
            [1, *](2, 3)
        """
        val arrayOfOne = """
           [1]
        """
        val arrayOfTwo = """
            [1, 2]
        """
        val arrayOfThree = """
            [1, 2, 3]
        """
        val arrayOfFour = """
            [1, 2, 3, 4]
        """
        assertThat(jsonMatchString matchJson arrayOfOne).isFailure()
        assertThat(jsonMatchString matchJson arrayOfTwo).isSuccess()
        assertThat(jsonMatchString matchJson arrayOfThree).isSuccess()
        assertThat(jsonMatchString matchJson arrayOfFour).isFailure()
    }
    ```
    Both the upper and lower bounds are optional, in which case no restriction on the lower and upper bounds respectively.
    ```
    fun test() {
        val jsonMatchStringWithNoLowerBound = """
            [1, *](, 3)
        """
        val arrayOfOne = """
           [1]
        """
        val arrayOfTwo = """
            [1, 2]
        """
        val arrayOfThree = """
            [1, 2, 3]
        """
        val arrayOfFour = """
            [1, 2, 3, 4]
        """
        assertThat(jsonMatchStringWithNoLowerBound matchJson arrayOfOne).isSuccess()
        assertThat(jsonMatchStringWithNoLowerBound matchJson arrayOfTwo).isSuccess()
        assertThat(jsonMatchStringWithNoLowerBound matchJson arrayOfThree).isSuccess()
        assertThat(jsonMatchStringWithNoLowerBound matchJson arrayOfFour).isFailure()
    }
    ```
    ```
    fun test() {
        val jsonMatchStringWithNoUpperBound = """
            [1, *](2,)
        """
        val arrayOfOne = """
           [1]
        """
        val arrayOfTwo = """
            [1, 2]
        """
        val arrayOfThree = """
            [1, 2, 3]
        """
        val arrayOfFour = """
            [1, 2, 3, 4]
        """
        assertThat(jsonMatchStringWithNoUpperBound matchJson arrayOfOne).isFailure()
        assertThat(jsonMatchStringWithNoUpperBound matchJson arrayOfTwo).isSuccess()
        assertThat(jsonMatchStringWithNoUpperBound matchJson arrayOfThree).isSuccess()
        assertThat(jsonMatchStringWithNoUpperBound matchJson arrayOfFour).isSuccess()
    }
    ```
### Value match
Value match is used in the key-value pair matching for json object fields and the entry matching for json array.
It can take several formats as below:
1. A string in a valid Json syntax: 
   it matches with the exact same json value.
2. Multiple value matches delimited by "|": 
    e.g. for array entry
    ```
    fun test() {
    val jsonMatchString = """
    ["Smith"|"John"]
    """
    val arrayOfSmith = """
    ["Smith"]
    """
    val arrayOfJohn = """
    ["John"]
    """
    assertThat(jsonMatchString matchJson arrayOfSmith).isSuccess()
    assertThat(jsonMatchString matchJson arrayOfJohn).isSuccess()
    }
    ```
    and for object field:
    ```
    fun test() {
    val jsonMatchString = """
     { 
        "firstName": "Smith"|"John"
     }
    """
    val jsonOfSmith = """
     {
       "firstName": "Smith"
     }
    """
    val jsonOfJohn = """
       {
         "firstName": "John"
       }
    """
    assertThat(jsonMatchString matchJson jsonOfSmith).isSuccess()
    assertThat(jsonMatchString matchJson jsonOfJohn).isSuccess()
    }
    ```
3. A wildcard character "*": 
   it can match with any json values, including strings, numbers, boolean, null, object and array etc.
4. A string enclosed with a pair of double quotes: 
   It matches with the exact same json string
5. A number in json format:
   It matches with the exact same json number
6. A word "true" or "false":
   It matches with the exact same json boolean value
7. A word "number":
   It matches with any json number, including decimal and integer
8. A word "int":
   It matches with any non-negative integer
9. A word "boolean":
   It matches with any boolean value, including true and false
10. A word "string":
   It matches with any string value
11. A regex surrounded with a pair of "/":
   It matches with any json string which matches with the specified regex 
   (The regex syntax follows the [Java convention](https://docs.oracle.com/javase/8/docs/api/java/util/regex/Pattern.html))

### Comment
json-m supports Java-style comments:
 - Single line comments, starts with "//" until the end of the line
 - Multi line comments, starts with "/\*" and ends with "\*/"

Comments are ignored by the json-m parser.

With the above definitions, we can specify the following json-m pattern:
```
{                                  // the target json must be a json object with the following restrictions:
  "lastName": "Smith",             // the field "lastName" is required and its value must be "Smith"
  "firstName": /[a-ZA-Z]+/,        // the field "firstName" is required and its value must match with the regex [a-ZA-Z]+
  "middleName"?: string,           // the field "middleName" is optional and its value can be any text
  "gender": "male"|"female"|"other", // the field "gender" is required and its value must be one of "male", "female" and "other" 
  "employed": boolean|null,        // the field "employed" is required and its value must be one of true, false and null
  "age": int,                      // the field "age" is required and its value must be an non-negative integer
  "weight"?: number,               // the field "weight" is optional and its value must be a number if present
  "address": [(string)+](, 6),     // the field "address" is required and its value must be a string array with the maximum size of 6
  "children"?: [                   // the field "children" is optional and its value must be an array of json object
     ({                            // which contains at least the field "lastName" with the value being "Smith" and optionally
        "lastName": "Smith",       // any other fields with any values
        *: *
     })*
  ]
}
```