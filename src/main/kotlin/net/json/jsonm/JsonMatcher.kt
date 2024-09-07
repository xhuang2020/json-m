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
                if (pairs[i].keyMatch().NEGATION() != null) {
                    if (pairs[i].valueMatch().WILDCARD() == null) {
                        throw JsonMatchValidationException(
                            "Negative key '${pairs[i].keyMatch().text}' must have the wild card value in the object ${obj.locateInJson()}"
                        )
                    }
                }
                if (i > 0) {
                    if (pairMatchCompare(pairs[i-1], pairs[i]) == 0 ||
                        pairs[i-1].keyMatch().STRING() != null && pairs[i].keyMatch().STRING() != null &&
                        pairs[i-1].keyMatch().STRING().text == pairs[i].keyMatch().STRING().text) {
                        throw JsonMatchValidationException(
                            "Duplicated keys '${pairs[i-1].keyMatch().text}', '${pairs[i].keyMatch().text}' in the object ${obj.locateInJson()}"
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
            if (!array.arrayEntryMatch().isNullOrEmpty()) {
                array.arrayEntryMatch().forEach { entry: JsonmParser.ArrayEntryMatchContext ->
                    entry.valueMatch().singleValueMatch().forEach { value: JsonmParser.SingleValueMatchContext ->
                        validate(value)
                    }
                }
                val wildCardIndex = array.arrayEntryMatch().indexOfFirst {
                    it.valueMatch().WILDCARD() != null
                }
                if (wildCardIndex >= 0 && wildCardIndex < array.arrayEntryMatch().lastIndex) {
                    throw JsonMatchValidationException(
                        "wildcard entry match must be the last one in the array ${array.locateInJson()}"
                    )
                }
            }
        }
        private fun validate(value: JsonmParser.SingleValueMatchContext) {
            value.objectMatch()?.let { validate(it) }
            value.arrayMatch()?.let { validate(it) }
        }

        infix fun String.matchJson(jsonStr: String): MatchResult {
            val jsonMatcher = JsonmReader.fromString(this).readAsJsonMatcher()
            val json = JsonmReader.fromString(jsonStr).readAsJson()
            return jsonMatcher.match(json)
        }
    }

    fun match(json: Json): MatchResult = match(jsonMatch, json.json)
}