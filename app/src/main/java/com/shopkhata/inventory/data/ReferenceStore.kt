package com.shopkhata.inventory.data

import android.content.Context
import com.shopkhata.inventory.domain.*
import org.json.JSONArray
import org.json.JSONObject

object ReferenceStore {
    fun load(context: Context): ReferenceCatalog {
        val root = JSONObject(context.assets.open("reference/prince-2026.json").bufferedReader().use { it.readText() })
        val source = root.getJSONObject("source")
        val pictures = runCatching { JSONObject(context.assets.open("reference/images.json").bufferedReader().use { it.readText() }).also {
            require(it.getString("sourceSha256")==source.getString("sha256"))
        }.getJSONObject("images") }.getOrNull()
        return ReferenceCatalog(source.getString("id"), source.getString("effectiveDate"), source.getString("filenameDate"), source.getString("taxBasis"),
            root.getJSONArray("products").values().map { p -> ReferenceProduct(p.getString("id"), p.getString("name"), p.getString("category"), p.getInt("page"),
                p.getJSONArray("variants").values().map { v -> ReferenceVariant(v.getString("id"), if (v.isNull("code")) null else v.getString("code"),
                    v.getString("label"), v.getString("context"), v.getJSONArray("fields").values().map { it.getString("label") to it.getString("text") },
                    v.getJSONArray("sourcePrices").values().map { it.getString("label") to it.getString("text") }, v.getString("rawRow"),
                    v.getJSONArray("issues").let { a -> (0 until a.length()).map { a.getString(it) } }) }, p.optString("priceListLabel"), pictures?.optJSONObject(p.getString("id"))?.optString("asset")?.takeIf(::validReferenceImage)) }, root.getJSONObject("audit").getInt("unresolvedCodeOccurrences"))
    }
}
fun validReferenceImage(value: String) = Regex("reference/products/[a-f0-9]{24}\\.webp").matches(value)
private fun JSONArray.values() = (0 until length()).map { getJSONObject(it) }
