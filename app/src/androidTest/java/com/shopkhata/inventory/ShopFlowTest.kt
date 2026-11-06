package com.shopkhata.inventory

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import android.content.Context
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.rules.RuleChain
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ShopFlowTest {
    private val compose = createAndroidComposeRule<MainActivity>()
    private val cleanCatalog = object : ExternalResource() {
        override fun before() { ApplicationProvider.getApplicationContext<Context>().deleteDatabase("catalog.db") }
    }
    @get:Rule val rules: RuleChain = RuleChain.outerRule(cleanCatalog).around(compose)

    @Test fun manualAddSearchAndQuickPriceUpdateWorkOffline() {
        compose.onNodeWithText("Add your first item").performClick()
        compose.onNodeWithTag("item-name-0").performTextInput("Padlock")
        compose.onNodeWithTag("item-price-0-0").performScrollTo().performTextInput("450")
        compose.onNodeWithText("Other names, separated by commas").performScrollTo().performTextInput("tala, ताला")
        compose.onNodeWithTag("add-list").performScrollToNode(hasText("Save 1 item on this phone"))
        compose.onNodeWithText("Save 1 item on this phone").performClick()
        compose.waitUntil(10000) { compose.onAllNodesWithText("Padlock").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("search").performTextInput("tala")
        compose.waitUntil(10000) { compose.onAllNodesWithText("₹450").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("₹450").performClick()
        compose.onAllNodesWithText("Edit price").onFirst().performScrollTo().performClick()
        compose.onNodeWithText("Rupees per piece").performTextReplacement("475.50")
        compose.onNodeWithText("Save price").performClick()
        compose.waitUntil(10000) { compose.onAllNodesWithText("₹475.5").fetchSemanticsNodes().isNotEmpty() }
        compose.activityRule.scenario.recreate()
        compose.waitUntil(10000) { compose.onAllNodesWithText("₹475.5").fetchSemanticsNodes().isNotEmpty() }
    }
}
