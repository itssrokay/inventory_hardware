package com.shopkhata.inventory.domain

data class LocalizedWord(val hi: String, val hinglish: String, val aliases: List<String> = emptyList()) {
    fun value(mode: String) = if(mode=="hi") hi else hinglish
}
class UiTranslations(val labels: Map<String,LocalizedWord>, val terms: Map<String,LocalizedWord>) {
    private val labelsIgnoreCase=labels.mapKeys {it.key.lowercase()}
    private val termsIgnoreCase=terms.mapKeys {it.key.lowercase()}
    private val productPattern=if(terms.isEmpty())null else Regex("(?<![A-Za-z])(?:${terms.keys.sortedByDescending {it.length}.joinToString("|") {Regex.escape(it)}})(?![A-Za-z])",RegexOption.IGNORE_CASE)
    private val templates=labels.filterKeys { Regex("\\{[0-9]+\\}").containsMatchIn(it) }.entries.sortedByDescending { it.key.replace(Regex("\\{[0-9]+\\}"), "").length }.map { (key,value) ->
        val pattern=Regex("\\{[0-9]+\\}");val matches=pattern.findAll(key).toList()
        var last=0;val expression=buildString {append('^');matches.forEach { m ->append(Regex.escape(key.substring(last,m.range.first)));append("(.+?)");last=m.range.last+1};append(Regex.escape(key.substring(last)));append('$')}
        Triple(Regex(expression,RegexOption.DOT_MATCHES_ALL),value,matches.map { it.value })
    }
    private val termPatterns=terms.entries.sortedByDescending { it.key.length }.map { (key,value) ->
        Triple(Regex("(?<![A-Za-z])${Regex.escape(key)}(?![A-Za-z])",RegexOption.IGNORE_CASE),key,value)
    }
    fun text(original: String,mode: String): String {
        if(mode !in listOf("hi","hinglish"))return original
        (labels[original] ?: labelsIgnoreCase[original.lowercase()])?.let { return it.value(mode) }
        templates.forEach { (pattern,value,placeholders) ->
            val match=pattern.matchEntire(original)?:return@forEach
            val values=placeholders.mapIndexed {index,key ->key to match.groupValues[index+1]}.toMap()
            return Regex("\\{[0-9]+\\}").replace(value.value(mode)) { values[it.value] ?: it.value }
        }
        return if(original.contains('\n')) original.split('\n').joinToString("\n") {text(it,mode)} else original
    }
    fun product(original:String,mode:String):String {
        if(mode !in listOf("hi","hinglish") || terms.isEmpty())return original
        // One pass over the original text avoids translating already replaced words.
        return productPattern!!.replace(original) { match -> termsIgnoreCase.getValue(match.value.lowercase()).value(mode) }
    }
    fun searchText(original:String):String = original+" "+product(original,"hi")+" "+product(original,"hinglish")+" "+termPatterns.filter { it.first.containsMatchIn(original) }.flatMap { it.third.aliases }.joinToString(" ")
    fun searchQuery(original:String):String {
        var result=original
        terms.forEach { (term,value) ->
            (value.aliases+value.hi).distinct().filter { it.isNotBlank() && terms.keys.none { key->key.equals(it,true) } }.sortedByDescending { it.length }.forEach { alias ->
                val pattern=Regex("(?<![\\p{L}\\p{N}])${Regex.escape(alias)}(?![\\p{L}\\p{N}])",RegexOption.IGNORE_CASE)
                result=pattern.replace(result) { term }
            }
        }
        return result
    }
}
