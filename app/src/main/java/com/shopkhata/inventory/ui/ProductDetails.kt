package com.shopkhata.inventory.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.shopkhata.inventory.domain.*
import java.io.File

@Composable
fun ProductDetailsDialog(product: Product, catalog: List<Product>, source: ReferenceProduct?, tax: String,
    dismiss: () -> Unit, edit: () -> Unit, editPrice: (ProductVariant) -> Unit,
    openRelated: (Product) -> Unit, browseCategory: () -> Unit) {
    var discount by rememberSaveable(product.id) { mutableStateOf("") }
    val context = LocalContext.current
    val related = remember(product, catalog) { relatedProducts(product, catalog) }
    AlertDialog(onDismissRequest = dismiss,
        title = { Text(if (product.name == source?.shopName) productLabel(product.name) else product.name) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(listOf(product.brand, product.category).filter(String::isNotBlank).joinToString(" · "))
                OutlinedTextField(discount, { discount = it }, Modifier.fillMaxWidth().testTag("view-discount"),
                    label = { Text(ui("Discount (%)")) }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                Text(ui("Preview only. Saved prices stay unchanged."), style = MaterialTheme.typography.labelSmall)
                if (discountedPaise("1", discount) == null) Text(ui("Enter a discount from 0 to 100, with up to two decimal places."), color = MaterialTheme.colorScheme.error)
                LazyColumn(Modifier.fillMaxWidth().heightIn(max = 360.dp).testTag("product-detail-list"), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    item {
                        if(product.productPhotos().isNotEmpty())PhotoGallery("Product photos",product.productPhotos())
                        else if(source?.imageAsset!=null)AsyncImage("file:///android_asset/${source.imageAsset}","Photo of ${product.name}",Modifier.fillMaxWidth().height(100.dp))
                        PhotoGallery("Shelf photos",product.shelfPhotoPaths)
                        Text(tax)
                        val location = listOf(product.zone, product.locationNote).filter(String::isNotBlank).joinToString(" · ")
                        if (location.isNotBlank()) Text(location)
                    }
                    items(product.variants, key = { it.id }) { variant ->
                        HorizontalDivider()
                        Text(variant.spec.ifBlank { ui("Standard") }, style = MaterialTheme.typography.titleSmall)
                        Text(rupees(variant.pricePaise) + " / " + ui(variant.unit), style = MaterialTheme.typography.titleLarge)
                        if (discount.isNotBlank()) discountedPaise(decimalPrice(variant.pricePaise), discount)?.let { net ->
                            Text(ui("After discount") + ": " + rupees(net) + " / " + ui(variant.unit), Modifier.testTag("view-net-${variant.id}"), color = MaterialTheme.colorScheme.primary)
                        }
                        TextButton(onClick = { editPrice(variant) }, modifier = Modifier.testTag("view-edit-price-${variant.id}")) { Text(ui("Edit price")) }
                    }
                    if (product.category.isNotBlank()) item {
                        HorizontalDivider()
                        Text(ui("Related products"), style = MaterialTheme.typography.titleSmall)
                        Text(ui("Suggested from the same category; matching brands appear first."), style = MaterialTheme.typography.bodySmall)
                        TextButton(onClick = browseCategory) { Text(ui("Browse this category")) }
                    }
                    items(related, key = { "related-${it.id}" }) { other ->
                        OutlinedButton(onClick = { openRelated(other) }, modifier = Modifier.fillMaxWidth().testTag("related-${other.id}")) {
                            Text(listOf(other.name, other.brand).filter(String::isNotBlank).joinToString(" · "))
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = dismiss) { Text(ui("Close")) } },
        dismissButton = { TextButton(onClick = edit) { Text(ui("Edit item")) } })
}
