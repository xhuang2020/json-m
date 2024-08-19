package net.json.jsonm

import net.json.jsonm.antlr4.JsonmParser

internal fun match(
    jsonMatch: JsonmParser.JsonMatchContext,
    json: JsonmParser.JsonContext
): MatchResult =
    if (jsonMatch.WILDCARD() != null) {
        MatchResult(true)

    } else if (jsonMatch.objectMatch() != null) {
        if (json.`object`() != null) {
            match(jsonMatch.objectMatch(), json.`object`())
        } else if (json.array() != null) {
            MatchResult(false, "object mismatched with array at ${json.array().locateInJson()}")
        } else {
            MatchResult(false, "json must be either object or array at ${json.locateInJson()}")
        }

    } else if (jsonMatch.arrayMatch() != null) {
        if (json.array() != null) {
            match(jsonMatch.arrayMatch(), json.array())
        } else if (json.`object`() != null) {
            MatchResult(false, "array mismatched with object at ${json.`object`().locateInJson()}")
        } else {
            MatchResult(false, "json must be either object or array at ${json.locateInJson()}")
        }

    } else {
        MatchResult(false, "jsonMatch cannot be empty")
    }

private fun match(
    objectMatch: JsonmParser.ObjectMatchContext,
    obj: JsonmParser.ObjectContext
): MatchResult {
    val pairMatches: List<JsonmParser.PairMatchContext> = objectMatch.pairMatch().sortedWith(::pairMatchCompare)
    val wildcardValueMatch = pairMatches.lastOrNull()?.let {
        if (it.keyMatch().WILDCARD() != null) {
            it.valueMatch()
        } else {
            null
        }
    }
    val pairs: List<JsonmParser.PairContext> = obj.pair().sortedWith(::pairCompare)
    var pairMatchIndex = 0
    var pairIndex = 0
    while (pairMatchIndex < pairMatches.size && pairIndex < pairs.size) {
        val pairM = pairMatches[pairMatchIndex]
        val pair = pairs[pairIndex]
        val pairMatchKey = pairM.keyMatch()
        val pairKey = pair.STRING()
        if (pairMatchKey.WILDCARD() != null) {
            val valueCompare = match(pairM.valueMatch(), pair.value())
            if (!valueCompare.isSuccess) {
                return valueCompare
            }
            pairIndex++
        } else {
            val pairMatchKeyString = pairMatchKey.STRING().text
            val pairKeyString = pairKey.text
            if (pairMatchKeyString < pairKeyString) {
                if (pairMatchKey.OPTCARD() == null) {
                    return MatchResult(false, "required field ${pairMatchKeyString} missing from ${obj.locateInJson()}")
                }
                pairMatchIndex++
            } else if (pairMatchKeyString == pairKeyString) {
                val valueCompare = match(pairM.valueMatch(), pair.value())
                if (!valueCompare.isSuccess) {
                    return valueCompare
                }
                pairMatchIndex++
                pairIndex++
            } else { // pairMatchKeyString > pairKeyString
                if (wildcardValueMatch != null) {
                    val valueCompare = match(wildcardValueMatch, pair.value())
                    if (!valueCompare.isSuccess) {
                        return valueCompare
                    }
                    pairIndex++
                } else {
                    return MatchResult(false, "unexpected field ${pairKeyString} in object ${obj.locateInJson()}")
                }
            }
        }
    }
    if (pairMatchIndex == pairMatches.size && pairIndex < pairs.size) {
        return MatchResult(false, "unexpected field ${pairs[pairIndex].STRING()} in object ${obj.locateInJson()}")

    } else if (pairMatchIndex < pairMatches.size && pairIndex == pairs.size) {
        val missedRequiredKey = pairMatches.subList(pairMatchIndex, pairMatches.size).find {
            it.keyMatch().WILDCARD() == null && it.keyMatch().OPTCARD() == null
        }
        if (missedRequiredKey != null) {
            return MatchResult(false, "required field ${missedRequiredKey.keyMatch().STRING().text} missing from ${obj.locateInJson()}")
        }
    }
    return MatchResult(true)
}

private fun match(
    arrayMatch: JsonmParser.ArrayMatchContext,
    array: JsonmParser.ArrayContext
): MatchResult {
    if (arrayMatch.arrayEntryMatch().isNullOrEmpty()) {
        if (!array.value().isNullOrEmpty()) {
            return MatchResult(false, "unexpected non-empty array ${array.locateInJson()}")
        }
    } else {
        if (arrayMatch.sizeRange() != null) {
            val sizeRangeContext = arrayMatch.sizeRange()
            if (sizeRangeContext.sizeBound != null) {
                val sizeBound = sizeRangeContext.sizeBound.text.toInt()
                if (sizeBound != array.value().size) {
                    return MatchResult(false, "required array size=$sizeBound, but the actual array size=${array.value().size} in array ${array.locateInJson()}")
                }
            }
            if (sizeRangeContext.lowerBound != null) {
                val lowerBound = sizeRangeContext.lowerBound.text.toInt()
                if (lowerBound > array.value().size) {
                    return MatchResult(false, "required minimum array size=$lowerBound, but the actual array size=${array.value().size} in array ${array.locateInJson()}")
                }
            }
            if (sizeRangeContext.uppperBound != null) {
                val upperBound = sizeRangeContext.uppperBound.text.toInt()
                if (upperBound < array.value().size) {
                    return MatchResult(false, "required maximum array size=$upperBound, but the actual array size=${array.value().size} in array ${array.locateInJson()}")
                }
            }
        }
        val arrayEntryMatches = arrayMatch.arrayEntryMatch()
        val arrayValues = array.value()
        var entryMatchIndex = 0
        var arrayValueIndex = 0
        var plusMatched = false
        while (entryMatchIndex < arrayEntryMatches.size && arrayValueIndex < arrayValues.size) {
            val entryMatch: JsonmParser.ArrayEntryMatchContext = arrayEntryMatches[entryMatchIndex]
            val value: JsonmParser.ValueContext = arrayValues[arrayValueIndex]
            if (entryMatch.valueMatch().WILDCARD() != null) {
                arrayValueIndex++
                continue
            }
            if (!match(entryMatch.valueMatch(), value).isSuccess) {
                when (entryMatch.op?.text) {
                    null -> {
                        return MatchResult(false, "unexpected value in array ${value.locateInJson()}")
                    }
                    "?" -> {
                        entryMatchIndex++
                    }
                    "+" -> {
                        if (plusMatched) {
                            plusMatched = false
                            entryMatchIndex++
                        } else {
                            return MatchResult(false, "unexpected value in array ${value.locateInJson()}")
                        }
                    }
                    "*" -> {
                        entryMatchIndex++
                    }
                }
            } else {
                when (entryMatch.op?.text) {
                    null -> {
                        entryMatchIndex++
                        arrayValueIndex++
                    }
                    "?" -> {
                        entryMatchIndex++
                        arrayValueIndex++
                    }
                    "+" -> {
                        plusMatched = true
                        arrayValueIndex++
                    }
                    "*" -> {
                        arrayValueIndex++
                    }
                }
            }
        }

        if (entryMatchIndex == arrayEntryMatches.size && arrayValueIndex < arrayValues.size) {
            return MatchResult(false, "unexpected value in array ${arrayValues[arrayValueIndex].locateInJson()}")

        } else if (entryMatchIndex < arrayEntryMatches.size && arrayValueIndex == arrayValues.size) {
            val missedRequired = arrayEntryMatches.subList(entryMatchIndex, arrayEntryMatches.size).find {
                it.valueMatch().WILDCARD() == null && (it.op == null || it.op.text == "+" && !plusMatched)
            }
            if (missedRequired != null) {
                return MatchResult(false, "required value missed in array ${missedRequired.locateInJson()}")
            }
        }
    }
    return MatchResult(true)
}

private fun match(
    valueMatch: JsonmParser.ValueMatchContext,
    value: JsonmParser.ValueContext
): MatchResult {
    if (valueMatch.WILDCARD() == null) {
        val singleValues: List<JsonmParser.SingleValueMatchContext> = valueMatch.singleValueMatch()
        if (singleValues.size == 1) {
            val rtn = match(singleValues[0], value)
            if (!rtn.isSuccess) {
                return rtn
            }
        } else {
            if (singleValues.all { !match(it, value).isSuccess }) {
                return MatchResult(false, "mismatched value at ${value.locateInJson()}")
            }
        }
    }
    return MatchResult(true)
}

private fun match(
    singleValueMatch: JsonmParser.SingleValueMatchContext,
    value: JsonmParser.ValueContext
): MatchResult {
    if (singleValueMatch.NULL_WORD() != null) {
        if (value.NULL_WORD() == null) {
            return MatchResult(false, "expect null at ${value.locateInJson()}")
        }
    } else if (singleValueMatch.NUMBER_WORD() != null || singleValueMatch.FLOAT_WORD() != null) {
        if (value.number() == null) {
            if (singleValueMatch.NUMBER_WORD() != null) {
                return MatchResult(false, "expect number at ${value.locateInJson()}")
            } else if (singleValueMatch.FLOAT_WORD() != null) {
                return MatchResult(false, "expect float at ${value.locateInJson()}")
            }
        }
        if (singleValueMatch.FLOAT_WORD() != null && value.number().FRACTION() == null && value.number().EXP() == null) {
            return MatchResult(false, "expect float at ${value.locateInJson()}")
        }
        if (singleValueMatch.numberRange() != null) {
            val num = value.number().text.toDouble()
            val range = singleValueMatch.numberRange()
            if (range.lowerBound != null) {
                val lower = range.lowerBound.text.toDouble()
                if (range.openChar.text == "(" && num <= lower ||
                    range.openChar.text == "[" && num < lower) {
                    return MatchResult(false, "$num is beyond the range ${range.text} at ${value.locateInJson()}")
                }
            }
            if (range.uppperBound != null) {
                val upper = range.uppperBound.text.toDouble()
                if (range.closeChar.text == ")" && num >= upper ||
                    range.closeChar.text == "]" && num > upper) {
                    return MatchResult(false, "$num is beyond the range ${range.text} at ${value.locateInJson()}")
                }
            }
        }
    } else if (singleValueMatch.INT_WORD() != null) {
        val num = value.number()
        if (num == null || num.FRACTION() != null || num.EXP() != null) {
            return MatchResult(false, "expect integer at ${value.locateInJson()}")
        }
        if (singleValueMatch.intRange() != null) {
            val intValue = num.integer().text.toLong()
            val range = singleValueMatch.intRange()
            if (range.lowerBound != null) {
                val lower = range.lowerBound.text.toLong()
                if (range.openChar.text == "(" && intValue <= lower ||
                    range.openChar.text == "[" && intValue < lower) {
                    return MatchResult(false, "$intValue is beyond the range ${range.text} at ${value.locateInJson()}")
                }
            }
            if (range.uppperBound != null) {
                val upper = range.uppperBound.text.toLong()
                if (range.closeChar.text == ")" && intValue >= upper ||
                    range.closeChar.text == "]" && intValue > upper) {
                    return MatchResult(false,  "$intValue is beyond the range ${range.text} at ${value.locateInJson()}")
                }
            }
        }
    } else if (singleValueMatch.BOOLEAN_WORD() != null) {
        if (value.BOOLEAN() == null) {
            return MatchResult(false, "expect boolean at ${value.locateInJson()}")
        }
    } else if (singleValueMatch.STRING_WORD() != null) {
        if (value.STRING() == null) {
            return MatchResult(false, "expect string at ${value.locateInJson()}")
        }
    } else if (singleValueMatch.BOOLEAN() != null) {
        val expected = singleValueMatch.BOOLEAN().text
        val bool = value.BOOLEAN()
        if (bool == null || bool.text != expected) {
            return MatchResult(false, "expect boolean value $expected at ${value.locateInJson()}")
        }
    } else if (singleValueMatch.number() != null) {
        val expected = singleValueMatch.number().text
        val num = value.number()
        if (num == null || num.text != expected) {
            return MatchResult(false, "expect number value $expected at ${value.locateInJson()}")
        }
    } else if (singleValueMatch.STRING() != null) {
        val expected = singleValueMatch.STRING().text
        val str = value.STRING()
        if (str == null || str.text != expected) {
            return MatchResult(false, "expect string value $expected at ${value.locateInJson()}")
        }
    } else if (singleValueMatch.REGEX() != null) {
        val expected = singleValueMatch.REGEX().text
        val str = value.STRING()
        if (str == null || !expected.removeDelimit().toRegex().matches(str.text.removeDelimit())) {
            return MatchResult(false, "expect string value matched with the regex $expected at ${value.locateInJson()}")
        }
    } else if (singleValueMatch.objectMatch() != null) {
        val obj = value.`object`() ?: return MatchResult(false, "expect object at ${value.locateInJson()}")
        val rtn = match(singleValueMatch.objectMatch(), obj)
        if (!rtn.isSuccess) {
            return rtn
        }
    } else if (singleValueMatch.arrayMatch() != null) {
        val array = value.array() ?: return MatchResult(false, "expect array at ${value.locateInJson()}")
        val rtn = match(singleValueMatch.arrayMatch(), array)
        if (!rtn.isSuccess) {
            return rtn
        }
    }
    return MatchResult(true)
}

private fun String.removeDelimit() = substring(1, length - 1)