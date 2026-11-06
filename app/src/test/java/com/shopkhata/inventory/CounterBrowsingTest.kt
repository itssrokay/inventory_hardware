package com.shopkhata.inventory

import com.shopkhata.inventory.domain.*
import org.junit.Assert.*
import org.junit.Test

class CounterBrowsingTest {
    @Test fun cachedReferenceSearchKeepsCodesAliasesAndFamilyBoundaries() {
        val row=ReferenceVariant("source-1","SIM107608","15 mm","",emptyList(),emptyList(),"15 mm SIM107608",emptyList())
        val group=ReferenceProduct("g","COUPLER","Smartfit Plus · CPVC",8,listOf(row))
        val catalog=ReferenceCatalog("source","2026-01-01","2026-01-30","GST extra",listOf(group),0)
        val translations=UiTranslations(emptyMap(),mapOf("coupler" to LocalizedWord("कपलर","Coupler",listOf("kaplar"))))
        val index=ReferenceSearchIndex(catalog,translations)
        for(query in listOf("coupler","कपलर","kaplar","sim107608"))assertEquals(listOf(row),index.search(query).single().variants)
        assertTrue(index.search("SIM107608","Other").isEmpty());assertTrue(index.search("SIM10760").isEmpty())
        assertEquals(listOf(group),catalog.products)
    }
    @Test fun relatedSuggestionsKeepProductsAndVariantPricesSeparate() {
        val base=Product(1,"Socket","Prince","Plumbing",variants=listOf(ProductVariant(1,"20 mm","piece",1700,1)))
        val same=base.copy(id=2,name="Elbow");val other=base.copy(id=3,name="Adapter",brand="Other")
        val wrong=base.copy(id=4,category="Electrical")
        val catalog=listOf(base,other,wrong,same)
        assertEquals(listOf(same,other),relatedProducts(base,catalog));assertEquals(4,catalog.size)
        assertTrue(relatedProducts(base.copy(category=""),catalog).isEmpty())
        assertEquals(1275L,discountedPaise(decimalPrice(base.variants.single().pricePaise),"25"))
        assertEquals(1700L,base.variants.single().pricePaise)
    }
}
