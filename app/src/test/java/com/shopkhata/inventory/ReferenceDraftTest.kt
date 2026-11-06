package com.shopkhata.inventory

import com.shopkhata.inventory.domain.*
import org.junit.Assert.*
import org.junit.Test

class ReferenceDraftTest {
    private val first=ReferenceVariant("ref:first","SIM107608","15 mm","Item Code",listOf("Size (mm)" to "15"),listOf("Rate Per PC" to "12.00"),"15 | SIM107608 | 12.00",emptyList())
    private val second=first.copy(id="ref:second",code="SIM107609",fields=listOf("Size (mm)" to "20"))
    private val catalog=ReferenceCatalog("source","2026-01-01","2026-01-30","GST extra",listOf(ReferenceProduct("group","Coupler","CPVC · Plumbing",8,listOf(first,second))),0)
    @Test fun selectionCreatesOnlyChosenDraftsWithUnconfirmedPriceAndUnit() {
        val draft=referenceDrafts(catalog,setOf(first.id),emptyList()).single()
        assertEquals(1,draft.variants.size)
        assertEquals("",draft.variants.single().price)
        assertEquals("",draft.variants.single().unit)
        assertNotNull(draft.error())
        assertEquals("12.00",catalog.variants.getValue(first.id).prices.single().second)
        assertEquals(first.id,draft.variants.single().sourceReferenceId)
    }
    @Test fun carriedVariantsAreNotActivatedAgainAndRemainingSizesMergeWithoutLosingLocation() {
        val shop=Product(1,"CPVC Coupler","Prince","CPVC · Plumbing",listOf("socket"),zone="A2",locationNote="Top shelf",variants=listOf(ProductVariant(1,"15 mm","piece",900,123,first.id)))
        assertTrue(referenceDrafts(catalog,setOf(first.id),listOf(shop)).isEmpty())
        val draft=referenceDrafts(catalog,setOf(first.id,second.id),listOf(shop)).single()
        assertEquals(1L,draft.targetId)
        assertEquals(second.id,draft.variants.single().sourceReferenceId)
        assertEquals("A2",draft.zone)
        assertEquals("socket",draft.aliases)
        assertEquals(first.id,shop.draft().variants.single().sourceReferenceId)
    }
}
