package net.json.jsonm

import net.json.jsonm.antlr4.JsonmParser

internal fun match(
    jsonMatch: JsonmParser.JsonMatchContext,
    json: JsonmParser.JsonContext
): Result<Boolean> =
    if (jsonMatch.WILDCARD() != null) {
        Result.success(true)

    } else if (jsonMatch.objectMatch() != null) {
        if (json.`object`() != null) {
            match(jsonMatch.objectMatch(), json.`object`())
        } else if (json.array() != null) {
            Result.failure(MismatchException("object mismatched with array at ${json.array().locateInJson()}"))
        } else {
            Result.failure(MismatchException("json must be either object or array at ${json.locateInJson()}"))
        }

    } else if (jsonMatch.arrayMatch() != null) {
        if (json.array() != null) {
            match(jsonMatch.arrayMatch(), json.array())
        } else if (json.`object`() != null) {
            Result.failure(MismatchException("array mismatched with object at ${json.`object`().locateInJson()}"))
        } else {
            Result.failure(MismatchException("json must be either object or array at ${json.locateInJson()}"))
        }

    } else {
        Result.failure(MismatchException("jsonMatch cannot be empty"))
    }

private fun match(
    objectMatch: JsonmParser.ObjectMatchContext,
    obj: JsonmParser.ObjectContext
): Result<Boolean> {
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
            if (valueCompare.isFailure) {
                return valueCompare
            }
            pairIndex++
        } else {
            val pairMatchKeyString = pairMatchKey.STRING().text
            val pairKeyString = pairKey.text
            if (pairMatchKeyString < pairKeyString) {
                if (pairMatchKey.OPTCARD() == null) {
                    return Result.failure(MismatchException("required field ${pairMatchKeyString} missing from ${obj.locateInJson()}"))
                }
                pairMatchIndex++
            } else if (pairMatchKeyString == pairKeyString) {
                val valueCompare = match(pairM.valueMatch(), pair.value())
                if (valueCompare.isFailure) {
                    return valueCompare
                }
                pairMatchIndex++
                pairIndex++
            } else { // pairMatchKeyString > pairKeyString
                if (wildcardValueMatch != null) {
                    val valueCompare = match(wildcardValueMatch, pair.value())
                    if (valueCompare.isFailure) {
                        return valueCompare
                    }
                    pairIndex++
                } else {
                    return Result.failure(MismatchException("unexpected field ${pairKeyString} in object ${obj.locateInJson()}"))
                }
            }
        }
    }
    if (pairMatchIndex == pairMatches.size && pairIndex < pairs.size) {
        return Result.failure(MismatchException("unexpected field ${pairs[pairIndex].STRING()} in object ${obj.locateInJson()}"))

    } else if (pairMatchIndex < pairMatches.size && pairIndex == pairs.size) {
        val missedRequiredKey = pairMatches.subList(pairMatchIndex, pairMatches.size).find {
            it.keyMatch().WILDCARD() == null && it.keyMatch().OPTCARD() == null
        }
        if (missedRequiredKey != null) {
            return Result.failure(MismatchException("required field ${missedRequiredKey.keyMatch().STRING().text} missing from ${obj.locateInJson()}"))
        }
    }
    return Result.success(true)
}

private fun match(
    arrayMatch: JsonmParser.ArrayMatchContext,
    array: JsonmParser.ArrayContext
): Result<Boolean> {
    if (arrayMatch.arrayEntryMatch().isNullOrEmpty()) {
        if (!array.value().isNullOrEmpty()) {
            return Result.failure(MismatchException("unexpected non-empty array ${array.locateInJson()}"))
        }
    } else {
        if (arrayMatch.sizeRange() != null) {
            val sizeRangeContext = arrayMatch.sizeRange()
            if (sizeRangeContext.sizeBound != null) {
                val sizeBound = sizeRangeContext.sizeBound.text.toInt()
                if (sizeBound != array.value().size) {
                    return Result.failure(MismatchException("required array size=$sizeBound, but the actual array size=${array.value().size} in array ${array.locateInJson()}"))
                }
            }
            if (sizeRangeContext.lowerBound != null) {
                val lowerBound = sizeRangeContext.lowerBound.text.toInt()
                if (lowerBound > array.value().size) {
                    return Result.failure(MismatchException("required minimum array size=$lowerBound, but the actual array size=${array.value().size} in array ${array.locateInJson()}"))
                }
            }
            if (sizeRangeContext.uppperBound != null) {
                val upperBound = sizeRangeContext.uppperBound.text.toInt()
                if (upperBound < array.value().size) {
                    return Result.failure(MismatchException("required maximum array size=$upperBound, but the actual array size=${array.value().size} in array ${array.locateInJson()}"))
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
            if (match(entryMatch.valueMatch(), value).isFailure) {
                when (entryMatch.op?.text) {
                    null -> {
                        return Result.failure(MismatchException("unexpected value in array ${value.locateInJson()}"))
                    }
                    "?" -> {
                        entryMatchIndex++
                    }
                    "+" -> {
                        if (plusMatched) {
                            plusMatched = false
                            entryMatchIndex++
                        } else {
                            return Result.failure(MismatchException("unexpected value in array ${value.locateInJson()}"))
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
            return Result.failure(MismatchException("unexpected value in array ${arrayValues[arrayValueIndex].locateInJson()}"))

        } else if (entryMatchIndex < arrayEntryMatches.size && arrayValueIndex == arrayValues.size) {
            val missedRequired = arrayEntryMatches.subList(entryMatchIndex, arrayEntryMatches.size).find {
                it.valueMatch().WILDCARD() == null && (it.op == null || it.op.text == "+" && !plusMatched)
            }
            if (missedRequired != null) {
                return Result.failure(MismatchException("required value missed in array ${missedRequired.locateInJson()}"))
            }
        }
    }
    return Result.success(true)
}

private fun match(
    valueMatch: JsonmParser.ValueMatchContext,
    value: JsonmParser.ValueContext
): Result<Boolean> {
    if (valueMatch.WILDCARD() == null) {
        val singleValues: List<JsonmParser.SingleValueMatchContext> = valueMatch.singleValueMatch()
        if (singleValues.size == 1) {
            val rtn = match(singleValues[0], value)
            if (rtn.isFailure) {
                return rtn
            }
        } else {
            if (singleValues.all { match(it, value).isFailure }) {
                return Result.failure(MismatchException("mismatched value at ${value.locateInJson()}"))
            }
        }
    }
    return Result.success(true)
}

private fun match(
    singleValueMatch: JsonmParser.SingleValueMatchContext,
    value: JsonmParser.ValueContext
): Result<Boolean> {
    if (singleValueMatch.NULL_WORD() != null) {
        if (value.NULL_WORD() == null) {
            return Result.failure(MismatchException("expect null at ${value.locateInJson()}"))
        }
    } else if (singleValueMatch.NUMBER_WORD() != null || singleValueMatch.FLOAT_WORD() != null) {
        if (value.number() == null) {
            if (singleValueMatch.NUMBER_WORD() != null) {
                return Result.failure(MismatchException("expect number at ${value.locateInJson()}"))
            } else if (singleValueMatch.FLOAT_WORD() != null) {
                return Result.failure(MismatchException("expect float at ${value.locateInJson()}"))
            }
        }
        if (singleValueMatch.FLOAT_WORD() != null && value.number().FRACTION() == null && value.number().EXP() == null) {
            return Result.failure(MismatchException("expect float at ${value.locateInJson()}"))
        }
        if (singleValueMatch.numberRange() != null) {
            val num = value.number().text.toDouble()
            val range = singleValueMatch.numberRange()
            if (range.lowerBound != null) {
                val lower = range.lowerBound.text.toDouble()
                if (range.openChar.text == "(" && num <= lower ||
                    range.openChar.text == "[" && num < lower) {
                    return Result.failure(MismatchException("$num is beyond the range ${range.text} at ${value.locateInJson()}"))
                }
            }
            if (range.uppperBound != null) {
                val upper = range.uppperBound.text.toDouble()
                if (range.closeChar.text == ")" && num >= upper ||
                    range.closeChar.text == "]" && num > upper) {
                    return Result.failure(MismatchException("$num is beyond the range ${range.text} at ${value.locateInJson()}"))
                }
            }
        }
    } else if (singleValueMatch.INT_WORD() != null) {
        val num = value.number()
        if (num == null || num.FRACTION() != null || num.EXP() != null) {
            return Result.failure(MismatchException("expect integer at ${value.locateInJson()}"))
        }
        if (singleValueMatch.intRange() != null) {
            val intValue = num.integer().text.toLong()
            val range = singleValueMatch.intRange()
            if (range.lowerBound != null) {
                val lower = range.lowerBound.text.toLong()
                if (range.openChar.text == "(" && intValue <= lower ||
                    range.openChar.text == "[" && intValue < lower) {
                    return Result.failure(MismatchException("$intValue is beyond the range ${range.text} at ${value.locateInJson()}"))
                }
            }
            if (range.uppperBound != null) {
                val upper = range.uppperBound.text.toLong()
                if (range.closeChar.text == ")" && intValue >= upper ||
                    range.closeChar.text == "]" && intValue > upper) {
                    return Result.failure(MismatchException("$intValue is beyond the range ${range.text} at ${value.locateInJson()}"))
                }
            }
        }
    } else if (singleValueMatch.BOOLEAN_WORD() != null) {
        if (value.BOOLEAN() == null) {
            return Result.failure(MismatchException("expect boolean at ${value.locateInJson()}"))
        }
    } else if (singleValueMatch.STRING_WORD() != null) {
        if (value.STRING() == null) {
            return Result.failure(MismatchException("expect string at ${value.locateInJson()}"))
        }
    } else if (singleValueMatch.BOOLEAN() != null) {
        val expected = singleValueMatch.BOOLEAN().text
        val bool = value.BOOLEAN()
        if (bool == null || bool.text != expected) {
            return Result.failure(MismatchException("expect boolean value $expected at ${value.locateInJson()}"))
        }
    } else if (singleValueMatch.number() != null) {
        val expected = singleValueMatch.number().text
        val num = value.number()
        if (num == null || num.text != expected) {
            return Result.failure(MismatchException("expect number value $expected at ${value.locateInJson()}"))
        }
    } else if (singleValueMatch.STRING() != null) {
        val expected = singleValueMatch.STRING().text
        val str = value.STRING()
        if (str == null || str.text != expected) {
            return Result.failure(MismatchException("expect string value $expected at ${value.locateInJson()}"))
        }
    } else if (singleValueMatch.REGEX() != null) {
        val expected = singleValueMatch.REGEX().text
        val str = value.STRING()
        if (str == null || !expected.removeDelimit().toRegex().matches(str.text.removeDelimit())) {
            return Result.failure(MismatchException("expect string value matched with the regex $expected at ${value.locateInJson()}"))
        }
    } else if (singleValueMatch.objectMatch() != null) {
        val obj = value.`object`() ?: return Result.failure(MismatchException("expect object at ${value.locateInJson()}"))
        val rtn = match(singleValueMatch.objectMatch(), obj)
        if (rtn.isFailure) {
            return rtn
        }
    } else if (singleValueMatch.arrayMatch() != null) {
        val array = value.array() ?: return Result.failure(MismatchException("expect array at ${value.locateInJson()}"))
        val rtn = match(singleValueMatch.arrayMatch(), array)
        if (rtn.isFailure) {
            return rtn
        }
    }
    return Result.success(true)
}

private fun String.removeDelimit() = substring(1, length - 1)