package net.json.jsonm

import net.json.jsonm.antlr4.JsonmParser

class Json internal constructor(internal val json: JsonmParser.JsonContext) {
    init { validate(json) }

    companion object {
        private fun validate(json: JsonmParser.JsonContext) {
            json.`object`()?.let { validate(it) }
            json.array()?.let { validate(it) }
        }
        private fun validate(obj: JsonmParser.ObjectContext) {
            val pairs = obj.pair().sortedWith(::pairCompare)
            for (i in pairs.indices) {
                if (i > 0) {
                    val key1 = pairs[i-1].STRING().text
                    val key2 = pairs[i].STRING().text
                    if (key1 == key2) {
                        throw JsonValidationException("Duplicated key '$key1' in the object ${obj.locateInJson()}")
                    }
                }
            }
            for (pair in pairs) {
                validate(pair.value())
            }
        }
        private fun validate(array: JsonmParser.ArrayContext) {
            array.value().forEach { value: JsonmParser.ValueContext ->
                validate(value)
            }
        }
        private fun validate(value: JsonmParser.ValueContext) {
            value.`object`()?.let { validate(it) }
            value.array()?.let { validate(it) }
        }
    }
}
