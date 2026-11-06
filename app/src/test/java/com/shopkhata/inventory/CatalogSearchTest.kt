package com.shopkhata.inventory

import com.shopkhata.inventory.domain.*
import org.junit.Assert.*
import org.junit.Test

class CatalogSearchTest {
    private val lock = Product(1, "Padlock", "Godrej", "Locks", listOf("tala", "ताला", "lock"), variants = listOf(
        ProductVariant(11, "50 mm", "piece", 45000, 1), ProductVariant(12, "65 mm", "piece", 60000, 1)))
    private val screw = Product(2, "Wood screw", category = "Fasteners", aliases = listOf("pench", "पेंच"), variants = listOf(
        ProductVariant(21, "1.5 inch", "packet", 9000, 1), ProductVariant(22, "2 inch", "packet", 12000, 1)))
    private val catalog = listOf(lock, screw)
    @Test fun `Hinglish query selects the requested size`() {
        val hits = CatalogSearch.search(catalog, "Godrej tala 50mm ka rate")
        assertEquals(1, hits.size); assertEquals(45000L, hits.single().variants.single().pricePaise)
    }
    @Test fun `Devanagari aliases and numerals match offline`() {
        assertEquals(lock.id, CatalogSearch.search(catalog, "ताला ६५mm का दाम").single().product.id)
    }
    @Test fun `common transposition typo matches`() {
        assertEquals(screw.id, CatalogSearch.search(catalog, "wodo screw").single().product.id)
    }
    @Test fun `decimal and fractional sizes remain exact`() {
        assertEquals("1.5 inch", normalize("1½\""))
        assertEquals(21L, CatalogSearch.search(catalog, "1.5 in screw").single().variants.single().id)
        assertTrue(CatalogSearch.search(catalog, "5 inch screw").isEmpty())
    }
    @Test fun `unmatched brand cannot return other products`() {
        assertTrue(CatalogSearch.search(catalog, "Godrej screw").isEmpty())
    }
    @Test fun `exact aliases rank ahead of fuzzy names`() {
        val similarlyNamed = lock.copy(id = 3, name = "Tala holder", aliases = emptyList())
        assertEquals(lock.id, CatalogSearch.search(listOf(similarlyNamed, lock), "tala").first().product.id)
    }
    @Test fun `size filter never matches part of a larger size`() {
        assertTrue(CatalogSearch.search(catalog, "5mm tala").isEmpty())
    }
    @Test fun `decimal prices use exact paise and reject invalid values`() {
        assertEquals(12345L, paise("123.45")); assertEquals(10L, paise("0.10"))
        listOf("", "-1", "1.999", "NaN", "100000001").forEach { assertNull(paise(it)) }
    }
    @Test fun `invalid drafts cannot be saved`() {
        assertNotNull(ItemDraft().error())
        assertNotNull(ItemDraft("Bolt", variants = listOf(VariantDraft(price = "4"), VariantDraft(price = "5"))).error())
        assertNull(ItemDraft("Bolt", variants = listOf(VariantDraft(price = "4.50"))).error())
    }
    @Test fun `duplicate matching respects brands`() {
        assertEquals(lock, CatalogSearch.duplicate(ItemDraft(name = "Tala", brand = "godrej"), catalog))
        assertNull(CatalogSearch.duplicate(ItemDraft(name = "Padlock", brand = "Yale"), catalog))
    }
    @Test fun `fts query safely quotes punctuation`() {
        assertEquals("\"tala\"* OR \"50\"* OR \"mm\"*", ftsQuery("tala \"50mm\""))
    }
    @Test fun `thousand item search stays responsive`() {
        val thousand = (1..1000).map { screw.copy(id = it.toLong()) }
        val start = System.nanoTime()
        repeat(5) { assertEquals(10, CatalogSearch.search(thousand, "wodo screw 2 inch").size) }
        assertTrue("Five 1,000-item searches should finish within 5 seconds", (System.nanoTime() - start) / 1_000_000 < 5000)
    }
}
