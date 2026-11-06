package com.shopkhata.inventory.domain

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.Normalizer
import java.util.Locale

val units = listOf("piece", "kg", "ft", "metre", "box", "dozen", "packet")

data class Product(
    val id: Long, val name: String, val brand: String = "", val category: String = "",
    val aliases: List<String> = emptyList(), val photoPath: String? = null,
    val zone: String = "", val locationNote: String = "", val updatedAt: Long = 0,
    val variants: List<ProductVariant> = emptyList(),
)
data class ProductVariant(val id: Long, val spec: String, val unit: String, val pricePaise: Long, val updatedAt: Long, val sourceReferenceId: String? = null)
data class VariantDraft(val spec: String = "", val unit: String = "piece", val price: String = "", val discount: String = "", val sourceReferenceId: String? = null)
data class ItemDraft(
    val name: String = "", val brand: String = "", val category: String = "",
    val aliases: String = "", val zone: String = "", val locationNote: String = "",
    val photoPath: String? = null, val variants: List<VariantDraft> = listOf(VariantDraft()),
    val targetId: Long? = null,
) {
    fun error(): String? = when {
        name.isBlank() -> "Enter an item name."
        variants.isEmpty() -> "Add at least one size and price."
        variants.any { it.unit !in units } -> "Choose a valid price unit."
        variants.any { discountedPaise(it.price, it.discount) == null } -> "Enter a valid price and a discount from 0 to 100%, with up to two decimal places."
        variants.map { normalize(it.spec) to it.unit }.distinct().size != variants.size -> "Each size and unit must be unique."
        else -> null
    }
}
fun Product.draft() = ItemDraft(name, brand, category, aliases.joinToString(", "), zone, locationNote, photoPath,
    variants.map { VariantDraft(it.spec, it.unit, decimalPrice(it.pricePaise), sourceReferenceId = it.sourceReferenceId) }, id)
fun paise(price: String): Long? = try {
    val decimal = BigDecimal(price.trim())
    if (decimal < BigDecimal.ZERO || decimal > BigDecimal("100000000")) null
    else decimal.setScale(2, RoundingMode.UNNECESSARY).movePointRight(2).longValueExact()
} catch (_: Exception) { null }
fun discountedPaise(price: String, discount: String = ""): Long? {
    val base = paise(price) ?: return null
    if (discount.isBlank()) return base
    val basisPoints = paise(discount)?.takeIf { it <= 10000 } ?: return null
    return BigDecimal.valueOf(base).multiply(BigDecimal.valueOf(10000 - basisPoints))
        .divide(BigDecimal.valueOf(10000), 0, RoundingMode.HALF_UP).longValueExact()
}
fun decimalPrice(value: Long): String = BigDecimal.valueOf(value, 2).stripTrailingZeros().toPlainString()
fun rupees(value: Long): String = "₹" + java.text.NumberFormat.getNumberInstance(Locale.forLanguageTag("en-IN")).apply {
    maximumFractionDigits = 2
}.format(BigDecimal.valueOf(value, 2))

fun normalize(value: String): String {
    var text = value.lowercase(Locale.ROOT).replace("½", ".5").replace("¼", ".25").replace("¾", ".75")
    text = Normalizer.normalize(text, Normalizer.Form.NFKC)
        .map { if (it.isDigit()) Character.getNumericValue(it).toString() else it.toString() }.joinToString("")
    text = text.replace(Regex("(\\d)\\s*[\"″]"), "$1 inch")
        .replace(Regex("(\\d)\\s*[′']"), "$1 ft")
        .replace(Regex("(\\d)\\s*(?:inches|inch|in)\\b"), "$1 inch")
        .replace(Regex("(\\d)\\s*(?:millimeters|millimetres|mm)\\b"), "$1 mm")
        .replace(Regex("(\\d)\\s*(?:feet|foot|ft)\\b"), "$1 ft")
    return text.replace(Regex("[^\\p{L}\\p{M}\\p{N}.]+"), " ").trim().replace(Regex("\\s+"), " ")
}
private val fillers = setOf("price", "of", "the", "rate", "ka", "ki", "ke", "kya", "hai", "kitna", "kitne", "batao", "chahiye", "please", "do", "you", "have", "what", "is", "का", "की", "के", "दाम", "रेट", "है", "क्या", "कितना", "बताओ", "only", "from", "by", "show", "me", "all", "items", "products", "brand", "just", "sirf", "sirph", "केवल", "सिर्फ", "or")
fun searchTokens(query: String): List<String> = normalize(query).split(" ").filter { it.isNotBlank() && it !in fillers }
fun ftsQuery(query: String) = searchTokens(query).joinToString(" OR ") { "\"${it.replace("\"", "") }\"*" }

/** Optimal-string-alignment distance includes common transposition typos. */
fun similarity(a: String, b: String): Double {
    if (a == b) return 1.0
    if (a.isEmpty() || b.isEmpty()) return 0.0
    val d = Array(a.length + 1) { IntArray(b.length + 1) }
    for (i in 0..a.length) d[i][0] = i
    for (j in 0..b.length) d[0][j] = j
    for (i in 1..a.length) for (j in 1..b.length) {
        d[i][j] = minOf(d[i-1][j]+1, d[i][j-1]+1, d[i-1][j-1]+if (a[i-1] == b[j-1]) 0 else 1)
        if (i > 1 && j > 1 && a[i-1] == b[j-2] && a[i-2] == b[j-1]) d[i][j] = minOf(d[i][j], d[i-2][j-2]+1)
    }
    return 1.0 - d[a.length][b.length].toDouble() / maxOf(a.length, b.length)
}
data class SearchIntent(val brands: Set<String>, val keywords: String)
fun searchIntent(products: List<Product>, query: String): SearchIntent {
    var remaining = " ${normalize(query)} "
    val found = linkedSetOf<String>()
    products.map { it.brand }.filter { it.isNotBlank() }.distinctBy(::normalize).sortedByDescending { it.length }.forEach { brand ->
        val phrase = " ${normalize(brand)} "
        if (remaining.contains(phrase)) { found += normalize(brand); remaining = remaining.replace(phrase, " ") }
    }
    return SearchIntent(found, remaining.trim())
}
fun singularWord(value: String): String = when {
    value.length > 4 && Regex("(?:ches|shes|xes|zes|sses)$").containsMatchIn(value) -> value.dropLast(2)
    value.length > 3 && value.endsWith("s") && !value.endsWith("ss") -> value.dropLast(1)
    else -> value
}
data class SearchHit(val product: Product, val variants: List<ProductVariant>, val score: Double)
object CatalogSearch {
    fun search(products: List<Product>, query: String, ftsIds: Set<Long> = emptySet()): List<SearchHit> {
        val intent = searchIntent(products, query)
        val catalog = products.filter { intent.brands.isEmpty() || normalize(it.brand) in intent.brands }
        val tokens = searchTokens(intent.keywords).map(::singularWord)
        if (tokens.isEmpty()) return catalog.sortedByDescending { it.updatedAt }.map { SearchHit(it, it.variants, 1.0) }
        val phrase = tokens.joinToString(" ")
        val size = Regex("\\d+(?:\\.\\d+)?(?:\\s+(?:inch|mm|ft))?").find(phrase)?.value
        return catalog.mapNotNull { product ->
            val terms = listOf(product.name, product.brand, product.category) + product.aliases + product.variants.map { it.spec }
            val normalized = terms.map(::normalize)
            val words = normalized.flatMap { it.split(" ") }.filter { it.isNotBlank() }.map(::singularWord)
            val scores = tokens.map { token -> words.maxOfOrNull { word ->
                when {
                    word == token -> 1.0
                    word.startsWith(token) -> .92
                    token.length >= 3 && word.length >= 3 && !token.any(Char::isDigit) -> similarity(token, word).let { if (it >= .6) it * .8 else 0.0 }
                    else -> 0.0
                }
            } ?: 0.0 }
            // Require every meaningful token, so "Godrej screw" cannot return every Godrej item.
            if (scores.any { it == 0.0 }) return@mapNotNull null
            val selected = if (size != null) product.variants.filter {
                val spec = normalize(it.spec)
                if (size.contains(' ')) Regex("(?<![\\d.])${Regex.escape(size)}(?![\\p{L}\\d.])").containsMatchIn(spec)
                else Regex("(?<![\\d.])${Regex.escape(size)}(?![\\d.])").containsMatchIn(spec)
            } else product.variants
            if (selected.isEmpty()) return@mapNotNull null
            val exact = normalized.any { it == phrase }
            SearchHit(product, selected, scores.average() + (if (exact) 1.0 else 0.0) + if (product.id in ftsIds) .01 else 0.0)
        }.sortedByDescending { it.score }.take(10)
    }
    fun duplicate(draft: ItemDraft, products: List<Product>): Product? = products.firstOrNull {
        normalize(it.brand) == normalize(draft.brand) &&
            (normalize(it.name) == normalize(draft.name) || it.aliases.any { alias -> normalize(alias) == normalize(draft.name) })
    }
}
