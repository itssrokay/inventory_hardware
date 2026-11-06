package com.shopkhata.inventory.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import com.shopkhata.inventory.domain.UiTranslations

val LocalUiLanguage=staticCompositionLocalOf {"en"}
val LocalUiTranslations=staticCompositionLocalOf {UiTranslations(emptyMap(),emptyMap())}
@Composable fun ui(text:String)=LocalUiTranslations.current.text(text,LocalUiLanguage.current)
@Composable fun productLabel(text:String)=LocalUiTranslations.current.product(text,LocalUiLanguage.current)
