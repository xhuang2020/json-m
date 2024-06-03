package net.json.jsonm

import net.json.jsonm.antlr4.JsonmParser
import org.antlr.v4.runtime.RuleContext

internal fun pairMatchCompare(a: JsonmParser.PairMatchContext, b: JsonmParser.PairMatchContext): Int {
    if (a.keyMatch().WILDCARD() != null && b.keyMatch().WILDCARD() != null) return 0
    if (a.keyMatch().WILDCARD() != null) return +1
    if (b.keyMatch().WILDCARD() != null) return -1
    val thisStr = a.keyMatch().STRING().text
    val otherStr = b.keyMatch().STRING().text
    val rtn: Int = thisStr.compareTo(otherStr)
    if (rtn != 0) return rtn
    if (a.keyMatch().OPTCARD() == null && b.keyMatch().OPTCARD() != null) return -1
    if (a.keyMatch().OPTCARD() != null && b.keyMatch().OPTCARD() == null) return +1
    return 0
}

internal fun pairCompare(a: JsonmParser.PairContext, b: JsonmParser.PairContext): Int =
    a.STRING().text.compareTo(b.STRING().text)

internal fun RuleContext?.locateInJson(): String =
    if (this == null) "$"

    else if (parent is JsonmParser.ArrayContext) {
        val index = (parent as JsonmParser.ArrayContext).value().indexOf(this)
        parent.locateInJson() + "[" + index + "]"

    } else if (parent is JsonmParser.ArrayMatchContext) {
        val index = (parent as JsonmParser.ArrayMatchContext).arrayEntryMatch().indexOf(this)
        parent.locateInJson() + "[" + index + "]"

    } else if ( this is JsonmParser.PairContext) {
        parent.locateInJson() + "." + this.STRING().text

    } else if (this is JsonmParser.PairMatchContext) {
        parent.locateInJson() + "." + this.keyMatch().text

    } else {
        parent.locateInJson()
    }
