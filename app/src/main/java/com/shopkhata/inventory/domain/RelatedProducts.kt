package com.shopkhata.inventory.domain

/** Browsing suggestions only. Never merge items, variants, source links or prices. */
fun relatedProducts(product: Product, catalog: List<Product>): List<Product> = catalog
    .filter { it.id != product.id && product.category.isNotBlank() && normalize(it.category) == normalize(product.category) }
    .sortedWith(compareByDescending<Product> { product.brand.isNotBlank() && normalize(it.brand) == normalize(product.brand) }
        .thenBy { it.name.lowercase() }.thenBy { it.id })
    .take(6)
