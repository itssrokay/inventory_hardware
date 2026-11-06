package com.shopkhata.inventory.data

import android.content.Context
import com.shopkhata.inventory.domain.LocalizedWord
import com.shopkhata.inventory.domain.UiTranslations
import org.json.JSONObject

object UiTranslationStore {
    fun load(context:Context):UiTranslations {
        val root=JSONObject(context.assets.open("ui/translations.json").bufferedReader().use {it.readText()})
        fun words(name:String):Map<String,LocalizedWord> {
            val entries=root.getJSONObject(name)
            return entries.keys().asSequence().associateWith { key -> val value=entries.getJSONObject(key)
                LocalizedWord(value.getString("hi"),value.getString("hinglish"),value.optJSONArray("aliases")?.let { a ->(0 until a.length()).map {a.getString(it)} }.orEmpty()) }
        }
        return UiTranslations(words("labels"),words("terms"))
    }
}
