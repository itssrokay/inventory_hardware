package com.shopkhata.inventory

import android.content.Context
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import com.shopkhata.inventory.data.ReferenceStore
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.rules.RuleChain

class ReferenceFlowTest {
    private val compose=createAndroidComposeRule<MainActivity>()
    private val clean=object:ExternalResource() {
        override fun before(){ApplicationProvider.getApplicationContext<Context>().deleteDatabase("catalog.db")}
    }
    @get:Rule val rules:RuleChain=RuleChain.outerRule(clean).around(compose)
    @Test fun ownerSelectsOneVariantConfirmsUnitAndPriceAndCannotImportItAgain() {
        val context=ApplicationProvider.getApplicationContext<Context>()
        val id=ReferenceStore.load(context).products.first {it.page==8&&it.name.startsWith("COUPLER")}.variants.first {it.code=="SIM107608"}.id
        compose.onNodeWithText("Add your first item").assertExists()
        compose.onNodeWithText("Reference").performClick()
        compose.onNodeWithTag("reference-search").performTextInput("SIM107608")
        compose.waitUntil(10000){compose.onAllNodesWithText("Choose sizes (1)").fetchSemanticsNodes().isNotEmpty()}
        compose.onNodeWithText("Choose sizes (1)").performScrollTo().performClick()
        compose.onNodeWithTag("reference-select-$id").performScrollTo().performClick()
        compose.onNodeWithText("Review carried products").performClick()
        compose.onNodeWithTag("add-list").performScrollToNode(hasText("Save 1 item on this phone"))
        compose.onNodeWithText("Save 1 item on this phone").assertIsNotEnabled()
        compose.onNodeWithTag("item-price-0-0").performScrollTo().performTextInput("9.50")
        compose.onNodeWithText("Choose unit").performScrollTo().performClick()
        compose.onNodeWithText("piece").performClick()
        compose.onNodeWithTag("add-list").performScrollToNode(hasText("Save 1 item on this phone"))
        compose.onNodeWithText("Save 1 item on this phone").performClick()
        compose.waitUntil(10000){compose.onAllNodesWithText("₹9.5").fetchSemanticsNodes().isNotEmpty()}
        compose.activityRule.scenario.recreate()
        compose.waitUntil(10000){compose.onAllNodesWithText("₹9.5").fetchSemanticsNodes().isNotEmpty()}
        compose.onNodeWithText("Reference").performClick()
        compose.onNodeWithTag("reference-search").performTextInput("SIM107608")
        compose.waitUntil(10000){compose.onAllNodesWithText("Choose sizes (1)").fetchSemanticsNodes().isNotEmpty()}
        compose.onNodeWithText("Choose sizes (1)").performScrollTo().performClick()
        compose.onNodeWithTag("reference-select-$id").performScrollTo().assertIsNotEnabled().assertIsOn()
    }
}
