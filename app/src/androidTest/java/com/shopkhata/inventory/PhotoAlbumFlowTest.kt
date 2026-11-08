package com.shopkhata.inventory

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ApplicationProvider
import com.shopkhata.inventory.domain.productPhotos
import com.shopkhata.inventory.ui.InventoryViewModel
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.rules.RuleChain

class PhotoAlbumFlowTest {
    private val compose=createAndroidComposeRule<MainActivity>()
    private val clean=object:ExternalResource(){override fun before(){
        val context=ApplicationProvider.getApplicationContext<Context>();context.deleteDatabase("catalog.db")
        context.getSharedPreferences("settings",Context.MODE_PRIVATE).edit().putString("ui_language","en").commit()
    }}
    @get:Rule val rules:RuleChain=RuleChain.outerRule(clean).around(compose)
    @Test fun croppedAlbumsChangeCoverSaveAndOpenShelfGallery() {
        compose.onNodeWithText("Add your first item").performClick()
        compose.onNodeWithTag("item-name-0").performTextInput("Album Socket")
        compose.onNodeWithTag("item-price-0-0").performScrollTo().performTextInput("17")
        val vm=ViewModelProvider(compose.activity)[InventoryViewModel::class.java]
        for(i in 0..2) {
            compose.runOnUiThread {vm.croppedPhoto(0,Bitmap.createBitmap(20,20,Bitmap.Config.ARGB_8888),shelf=i==2)}
            compose.waitUntil(10000){!vm.busy.value&&vm.review.value.single().productPhotos().size+vm.review.value.single().shelfPhotoPaths.size==i+1}
        }
        val second=vm.review.value.single().productPhotos()[1]
        compose.onNodeWithTag("add-Product photos").performScrollTo()
        compose.onNodeWithText("Make cover").assertIsEnabled().assertIsDisplayed().performClick()
        compose.waitUntil(10000){vm.review.value.single().photoPath==second}
        assertEquals(second,vm.review.value.single().photoPath)
        compose.onNodeWithTag("add-list").performScrollToNode(hasText("Save 1 item on this phone"))
        compose.onNodeWithText("Save 1 item on this phone").performClick()
        compose.waitUntil(10000){compose.onAllNodesWithTag("product-1").fetchSemanticsNodes().isNotEmpty()}
        compose.onNodeWithTag("product-1").performScrollTo().performClick()
        compose.onNodeWithText("Product photos (2)").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Shelf photos (1)").performScrollTo().assertIsDisplayed()
        compose.onNode(hasText("GST not specified") and hasAnyAncestor(hasTestTag("product-detail-list"))).performScrollTo()
        compose.onNodeWithContentDescription("Shelf photos 1").assertIsDisplayed().performClick()
        compose.onNodeWithText("Shelf photos 1/1").assertExists()
    }
}
