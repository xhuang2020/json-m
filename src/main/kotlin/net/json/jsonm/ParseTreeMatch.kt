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


internal fun match(
    objectMatch: JsonmParser.ObjectMatchContext,
    obj: JsonmParser.ObjectContext
): Result<Boolean> {
    val pairMatches: List<JsonmParser.PairMatchContext> = objectMatch.pairMatch().sortedWith(::pairMatchCompare)
    val pairs: List<JsonmParser.PairContext> = obj.pair().sortedWith(::pairCompare)
    return Result.success(true)
}
internal fun match(
    arrayMatch: JsonmParser.ArrayMatchContext,
    array: JsonmParser.ArrayContext
): Result<Boolean> {
    return Result.success(true)
}