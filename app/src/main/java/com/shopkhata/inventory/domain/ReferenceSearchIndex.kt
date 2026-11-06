package com.shopkhata.inventory.domain

/** Build once off the UI thread. Filtering never normalizes the supplier rows again. */
class ReferenceSearchIndex(catalog: ReferenceCatalog, translations: UiTranslations) {
    private data class Row(val variant: ReferenceVariant, val words: String)
    private data class Group(val product: ReferenceProduct, val rows: List<Row>)
    private val groups = catalog.products.map { product ->
        val names = translations.searchText("Prince ${product.name} ${product.category}")
        Group(product, product.variants.map { Row(it, normalize("$names ${it.label} ${it.code.orEmpty()} ${it.rawRow}")) })
    }
    fun search(query: String, category: String? = null): List<ReferenceProduct> {
        val terms = normalize(query).split(' ').filter(String::isNotBlank)
        val code = query.trim().takeIf { Regex("(?:SIM|IM|IT|IP)\\d{5,8}", RegexOption.IGNORE_CASE).matches(it) }
        return groups.filter { category == null || it.product.category == category }.mapNotNull { group ->
            if (terms.isEmpty()) group.product else {
                val variants = group.rows.filter { row ->
                    if (code != null) row.variant.code?.equals(code, ignoreCase = true) == true
                    else terms.all { it in row.words }
                }.map { it.variant }
                if (variants.isEmpty()) null else group.product.copy(variants = variants)
            }
        }
    }
}
