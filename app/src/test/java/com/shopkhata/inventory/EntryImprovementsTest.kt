package com.shopkhata.inventory

import com.shopkhata.inventory.domain.*
import org.junit.Assert.*
import org.junit.Test

class EntryImprovementsTest {
    private val socket = Product(1, "PVC socket", "Prince", "Plumbing", variants = listOf(ProductVariant(11,"20 mm","piece",10000,1)))
    private val valve = socket.copy(id=2,name="Ball valve",variants=listOf(ProductVariant(21,"25 mm","piece",20000,1)))
    private val other = socket.copy(id=3,name="Prince compatible socket",brand="Astral")
    @Test fun `natural brand only filter is strict and works offline`() {
        assertEquals(setOf(1L,2L),CatalogSearch.search(listOf(socket,valve,other),"show me Prince only").map { it.product.id }.toSet())
    }
    @Test fun `plural product and brand sentence combine correctly`() {
        assertEquals(listOf(1L),CatalogSearch.search(listOf(socket,valve,other),"sockets from Prince only").map { it.product.id })
        assertEquals(11L,CatalogSearch.search(listOf(socket,valve,other),"Prince sockets 20mm").single().variants.single().id)
    }
    @Test fun `multiword brands stay together`() {
        val product=socket.copy(brand="Prince Flowguard")
        assertEquals(setOf("prince flowguard"),searchIntent(listOf(product),"sockets from Prince Flowguard only").brands)
        assertEquals(1L,CatalogSearch.search(listOf(product),"sockets from Prince Flowguard only").single().product.id)
    }
    @Test fun `discount calculator is exact bounded and applied once`() {
        assertEquals(7500L,discountedPaise("100","25"))
        assertEquals(7500L,discountedPaise("75",""))
        assertEquals(1L,discountedPaise("0.01","50"))
        assertEquals(0L,discountedPaise("450","100"))
        assertEquals(44955L,discountedPaise("450","0.10"))
        listOf("-1","100.01","25.555","oops").forEach { assertNull(discountedPaise("100",it)) }
        assertNotNull(ItemDraft("Socket",variants=listOf(VariantDraft(price="100",discount="101"))).error())
    }
    @Test fun `crop coordinates match center zoom and panning without escaping source`() {
        assertEquals(CropRegion(50,0,100,100),cropRegion(200,100,100.0,100.0,1.0,0.0,0.0))
        assertEquals(CropRegion(75,25,50,50),cropRegion(200,100,100.0,100.0,2.0,0.0,0.0))
        assertEquals(0,cropRegion(200,100,100.0,100.0,2.0,10000.0,0.0).left)
        assertEquals(150,cropRegion(200,100,100.0,100.0,2.0,-10000.0,0.0).left)
    }
}
