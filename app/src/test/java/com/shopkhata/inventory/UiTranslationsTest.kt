package com.shopkhata.inventory

import com.shopkhata.inventory.domain.*
import org.junit.Assert.*
import org.junit.Test

class UiTranslationsTest {
    private val translations=UiTranslations(mapOf("Save {0} items" to LocalizedWord("{0} सामान सेव करें","{0} samaan save karein")),
        mapOf("coupler" to LocalizedWord("कपलर","Coupler",listOf("kaplar"))))
    @Test fun displayModesPreserveUserDataAndTechnicalDetails() {
        assertEquals("Save 2 items",translations.text("Save 2 items","en"))
        assertEquals("2 सामान सेव करें",translations.text("Save 2 items","hi"))
        assertEquals("2 samaan save karein",translations.text("Save 2 items","hinglish"))
        val value="SIM107608 15 mm SDR 11 ₹123.45";assertEquals(value,translations.text(value,"hi"))
        assertEquals("कपलर SDR 11",translations.product("COUPLER SDR 11","hi"))
        assertEquals("Coupler SDR 11",translations.product("COUPLER SDR 11","hinglish"))
        assertEquals("Something else",translations.text("Something else","hi"))
    }
    @Test fun multilingualSearchMapsOnlyKnownAliasesAndKeepsCodes() {
        assertEquals("coupler SIM107608 15 mm",translations.searchQuery("कपलर SIM107608 15 mm"))
        assertEquals("coupler 20mm",translations.searchQuery("kaplar 20mm"))
        assertTrue(translations.searchText("CPVC COUPLER").contains("कपलर"))
        assertEquals("कपलर",UiTranslations(emptyMap(),emptyMap()).product("कपलर","hi"))
    }
}
