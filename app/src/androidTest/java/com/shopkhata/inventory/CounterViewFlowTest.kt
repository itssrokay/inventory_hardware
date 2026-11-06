package com.shopkhata.inventory

import android.content.Context
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.shopkhata.inventory.data.CatalogDatabase
import com.shopkhata.inventory.data.CatalogRepository
import com.shopkhata.inventory.domain.ItemDraft
import com.shopkhata.inventory.domain.VariantDraft
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.rules.RuleChain

class CounterViewFlowTest {
    private val compose=createAndroidComposeRule<MainActivity>()
    private val fixtures=object:ExternalResource() {
        override fun before() {
            val context=ApplicationProvider.getApplicationContext<Context>();context.deleteDatabase("catalog.db")
            context.getSharedPreferences("settings",Context.MODE_PRIVATE).edit().putString("ui_language","en").commit()
            val db=Room.databaseBuilder(context,CatalogDatabase::class.java,"catalog.db").build()
            try {runBlocking {CatalogRepository(db).save(listOf(
                ItemDraft("Socket","Prince","Plumbing",variants=listOf(VariantDraft("20 mm","piece","123.45"))),
                ItemDraft("Elbow","Prince","Plumbing",variants=listOf(VariantDraft("20 mm","piece","33")))
            ))}}finally{db.close()}
        }
    }
    @get:Rule val rules:RuleChain=RuleChain.outerRule(fixtures).around(compose)
    @Test fun viewDiscountAndRelatedBrowsingNeverWritePricesOrHistory() {
        compose.onNodeWithTag("search").performTextInput("Socket")
        compose.waitUntil(10000){compose.onAllNodesWithTag("product-1").fetchSemanticsNodes().isNotEmpty()}
        compose.onNodeWithTag("catalog").performScrollToNode(hasText("₹123.45"))
        compose.onNodeWithText("₹123.45").performClick()
        compose.onNodeWithText("Preview only. Saved prices stay unchanged.").assertExists()
        compose.onNodeWithText("Save price").assertDoesNotExist()
        compose.onNodeWithTag("view-discount").performTextInput("25")
        compose.onNodeWithTag("view-net-1").performScrollTo().assertTextContains("₹92.59",substring=true)
        compose.onNodeWithTag("related-2").performScrollTo().performClick()
        compose.onNodeWithText("₹33 / piece").assertExists()
        compose.onNodeWithTag("view-discount").assert(!hasText("25"))
        compose.onNodeWithText("Close").performClick()
        val context=ApplicationProvider.getApplicationContext<Context>()
        val db=Room.databaseBuilder(context,CatalogDatabase::class.java,"catalog.db").build()
        try {runBlocking {
            val socket=db.catalog().all().first{it.item.name=="Socket"};assertEquals(12345L,socket.variants.single().pricePaise)
            assertTrue(db.catalog().history(socket.variants.single().id).isEmpty())
        }}finally{db.close()}
    }
}
