package com.shopkhata.inventory.domain

/** Manufacturer data never becomes a shop item until the owner confirms a draft. */
data class ReferenceVariant(
    val id: String, val code: String?, val label: String, val context: String,
    val fields: List<Pair<String, String>>, val prices: List<Pair<String, String>>,
    val rawRow: String, val issues: List<String>,
) {
    fun shopSpec(): String {
        val size = fields.filter { (key, value) -> Regex("size|capacity|slot|shape|^type|^mtr", RegexOption.IGNORE_CASE).containsMatchIn(key) || Regex("\\d\\s*mm", RegexOption.IGNORE_CASE).containsMatchIn(value) }
            .filter { (_, value) -> value.isNotBlank() && value != "-" }.joinToString(" · ") { (key, value) -> "$key: $value" }
        return listOf(size, context.replace(Regex("item\\s*code", RegexOption.IGNORE_CASE), "").trim(' ', '/'), code ?: "Code not listed")
            .filter { it.isNotBlank() }.joinToString(" · ")
    }
}
data class ReferenceProduct(val id: String, val name: String, val category: String, val page: Int, val variants: List<ReferenceVariant>, val priceListLabel: String = "", val imageAsset: String? = null) {
    val shopName get() = "${category.substringBefore(" · ")} $name"
}
data class ReferenceCatalog(val sourceId: String, val effectiveDate: String, val filenameDate: String, val taxBasis: String,
    val products: List<ReferenceProduct>, val unresolved: Int) {
    val variants by lazy { products.flatMap { it.variants }.associateBy { it.id } }
    fun productFor(referenceId: String?) = if(referenceId==null)null else products.firstOrNull { p -> p.variants.any { it.id == referenceId } }
    fun imageFor(referenceId: String?) = productFor(referenceId)?.imageAsset
}

fun referenceDrafts(catalog: ReferenceCatalog, selected: Set<String>, shop: List<Product>): List<ItemDraft> {
    val active = shop.flatMap { it.variants }.mapNotNull { it.sourceReferenceId }.toSet()
    return catalog.products.mapNotNull { group ->
        val variants = group.variants.filter { it.id in selected && it.id !in active }
        if (variants.isEmpty()) null else ItemDraft(name = group.shopName, brand = "Prince", category = group.category,
            // A price/unit from a supplier list is not a confirmed shop selling price/unit.
            variants = variants.map { VariantDraft(spec = it.shopSpec(), unit = "", price = "", sourceReferenceId = it.id) })
    }.groupBy { it.name to it.category }.values.map { drafts ->
        val draft = drafts.first().copy(variants = drafts.flatMap { it.variants }.distinctBy { it.sourceReferenceId })
        val existing = shop.firstOrNull { normalize(it.name) == normalize(draft.name) && normalize(it.brand) == "prince" }
        draft.copy(extraPhotoPaths=existing?.extraPhotoPaths.orEmpty(), shelfPhotoPaths=existing?.shelfPhotoPaths.orEmpty(), targetId = existing?.id, photoPath = existing?.photoPath, zone = existing?.zone.orEmpty(), locationNote = existing?.locationNote.orEmpty(), aliases = existing?.aliases?.joinToString(", ").orEmpty())
    }
}
