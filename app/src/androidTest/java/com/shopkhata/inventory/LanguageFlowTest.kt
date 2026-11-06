package com.shopkhata.inventory

import android.content.Context
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import com.shopkhata.inventory.data.ReferenceStore
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.rules.RuleChain

class LanguageFlowTest {
    private val compose=createAndroidComposeRule<MainActivity>()
    private val clean=object:ExternalResource() {
        override fun before(){
            val context=ApplicationProvider.getApplicationContext<Context>();context.deleteDatabase("catalog.db")
            context.getSharedPreferences("settings",Context.MODE_PRIVATE).edit().putString("ui_language","en").commit()
        }
        override fun after(){ApplicationProvider.getApplicationContext<Context>().getSharedPreferences("settings",Context.MODE_PRIVATE).edit().putString("ui_language","en").commit()}
    }
    @get:Rule val rules:RuleChain=RuleChain.outerRule(clean).around(compose)
    @Test fun displayModesPersistAndReferenceNamesAndPicturesRemainAvailable() {
        val context=ApplicationProvider.getApplicationContext<Context>()
        val group=ReferenceStore.load(context).products.first {it.page==8&&it.name.startsWith("COUPLER")}
        compose.onNodeWithText("Settings").performClick()
        compose.onNodeWithTag("ui-language-hi").performScrollTo().performClick()
        compose.onNodeWithText("ऐप की भाषा").assertExists()
        assertEquals("hi",context.getSharedPreferences("settings",Context.MODE_PRIVATE).getString("ui_language",null))
        compose.onNodeWithText("रेफरेंस").performClick()
        compose.onNodeWithTag("reference-search").performTextInput("SIM107608")
        compose.waitUntil(10000){compose.onAllNodesWithContentDescription("Source illustration of ${group.name}").fetchSemanticsNodes().isNotEmpty()}
        compose.onNodeWithContentDescription("Source illustration of ${group.name}").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("सेटिंग्स").performClick()
        compose.onNodeWithTag("ui-language-hinglish").performScrollTo().performClick()
        compose.onNodeWithText("App ki bhasha").assertExists()
        compose.activityRule.scenario.recreate()
        compose.waitUntil(10000){compose.onAllNodesWithText("Dukaan ki list").fetchSemanticsNodes().isNotEmpty()}
        assertEquals("hinglish",context.getSharedPreferences("settings",Context.MODE_PRIVATE).getString("ui_language",null))
        compose.onNodeWithText("Reference").performClick()
        compose.onNodeWithTag("reference-search").performTextInput("कपलर")
        compose.waitUntil(10000){compose.onAllNodesWithText("Coupler",substring=true).fetchSemanticsNodes().isNotEmpty()}
        compose.onNodeWithText("Review carried products").assertDoesNotExist()
        compose.onNodeWithText("Dukaan wale samaan check karein").assertIsNotEnabled()
    }
}
