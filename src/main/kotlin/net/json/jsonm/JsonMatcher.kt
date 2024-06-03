package net.json.jsonm

import net.json.jsonm.antlr4.JsonmParser

class JsonMatcher internal constructor(private val jsonMatch: JsonmParser.JsonMatchContext) {
    init { validate(jsonMatch) }

    companion object {
        private fun validate(jsonMatch: JsonmParser.JsonMatchContext) {
            jsonMatch.objectMatch()?.let { validate(it) }
            jsonMatch.arrayMatch()?.let { validate(it) }
        }
        private fun validate(obj: JsonmParser.ObjectMatchContext) {
            val pairs = obj.pairMatch().sortedWith(::pairMatchCompare)
            for (i in pairs.indices) {
                if (i > 0) {
                    if (pairMatchCompare(pairs[i-1], pairs[i]) == 0 ||
                        pairs[i-1].keyMatch().STRING() != null && pairs[i].keyMatch().STRING() != null &&
                        pairs[i-1].keyMatch().STRING().text == pairs[i].keyMatch().STRING().text) {
                        throw JsonMatchValidationException(
                            "Duplicated key '${pairs[i].keyMatch().text}' in the object ${obj.locateInJson()}"
                        )
                    }
                }
            }
            for (pair in pairs) {
                pair.valueMatch().singleValueMatch().forEach { value: JsonmParser.SingleValueMatchContext ->
                    validate(value)
                }
            }
        }
        private fun validate(array: JsonmParser.ArrayMatchContext) {
            array.arrayEntryMatch().forEach { entry: JsonmParser.ArrayEntryMatchContext ->
                entry.valueMatch().singleValueMatch().forEach { value: JsonmParser.SingleValueMatchContext ->
                    validate(value)
                }
            }
        }
        private fun validate(value: JsonmParser.SingleValueMatchContext) {
            value.objectMatch()?.let { validate(it) }
            value.arrayMatch()?.let { validate(it) }
        }
    }

    fun match(json: Json): Result<Boolean> = match(jsonMatch, json.json)
}