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
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.rules.RuleChain

class CategoryFlowTest {
    private val compose = createAndroidComposeRule<MainActivity>()
    private val fixtures = object : ExternalResource() {
        override fun before() {
            val context = ApplicationProvider.getApplicationContext<Context>()
            context.deleteDatabase("catalog.db")
            context.getSharedPreferences("settings",Context.MODE_PRIVATE).edit().putString("ui_language","en").commit()
            val db = Room.databaseBuilder(context,CatalogDatabase::class.java,"catalog.db").build()
            try { runBlocking {
                CatalogRepository(db).save((1..45).map { ItemDraft(name="Socket $it",category="Category $it",variants=listOf(VariantDraft(spec="20 mm",price="17"))) } +
                    ItemDraft(name="Padlock",category="Locks",variants=listOf(VariantDraft(price="450"))))
            } } finally { db.close() }
        }
    }
    @get:Rule val rules: RuleChain = RuleChain.outerRule(fixtures).around(compose)

    @Test fun searchableCategoriesReachLaterGroupsAndClearFilterWithoutLosingQuery() {
        compose.waitUntil(10000) { compose.onAllNodesWithTag("browse-categories").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("browse-categories").performScrollTo().performClick()
        compose.onNodeWithTag("category-picker-search").performTextInput("Category 45")
        compose.onNodeWithTag("category-option-Category 45").performClick()
        compose.waitUntil(10000) { compose.onAllNodesWithText("Socket 45").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("search").performScrollTo().performTextInput("Padlock")
        compose.waitUntil(10000) { compose.onAllNodesWithText("No matching items").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Search all items").performScrollTo().performClick()
        compose.waitUntil(10000) { compose.onAllNodes(hasText("Padlock") and !hasSetTextAction()).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("search").assertTextContains("Padlock")
        compose.onNodeWithTag("catalog").performScrollToNode(hasText("₹450"))
        compose.onNodeWithText("₹450").assertIsDisplayed()
    }
}
