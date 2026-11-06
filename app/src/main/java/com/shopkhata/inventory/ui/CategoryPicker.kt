package com.shopkhata.inventory.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.shopkhata.inventory.domain.normalize

/** Values are original category names; null alone means all categories. */
data class CategoryChoice(val value: String, val count: Int)

@Composable
fun CategoryPickerDialog(
    choices: List<CategoryChoice>, selected: String?, onSelect: (String?) -> Unit,
    onDismiss: () -> Unit, reference: Boolean = false, allowAll: Boolean = true,
) {
    var query by remember { mutableStateOf("") }
    val translations = LocalUiTranslations.current
    val terms = normalize(query).split(' ').filter(String::isNotBlank)
    val matching = choices.filter { choice ->
        val label = choice.value.ifBlank { "Uncategorized" }
        val words = normalize(translations.searchText(label) + " " + translations.text(label, "hi") + " " + translations.text(label, "hinglish"))
        terms.all { it in words }
    }
    AlertDialog(onDismissRequest = onDismiss,
        title = { Text(ui(if (reference) "Choose a product family" else "Choose a category")) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth().testTag("category-picker-search"),
                    label = { Text(ui("Find a category")) }, singleLine = true)
                LazyColumn(Modifier.fillMaxWidth().heightIn(max = 360.dp)) {
                    if (allowAll) item {
                        TextButton(onClick = { onSelect(null); onDismiss() }, modifier = Modifier.fillMaxWidth().testTag("category-all")) {
                            Text(ui(if (reference) "All product families" else "All items"), Modifier.weight(1f))
                            Text(choices.sumOf { it.count }.toString())
                        }
                    }
                    items(matching, key = { it.value }) { choice ->
                        TextButton(onClick = { onSelect(choice.value); onDismiss() },
                            modifier = Modifier.fillMaxWidth().testTag("category-option-${choice.value}")) {
                            RadioButton(selected = selected == choice.value, onClick = null)
                            Text(if (choice.value.isBlank()) ui("Uncategorized") else if (reference) productLabel(choice.value) else choice.value,
                                Modifier.weight(1f).padding(horizontal = 8.dp))
                            Text("${choice.count} " + ui(if (reference) "variants" else "items"))
                        }
                    }
                    if (matching.isEmpty()) item { Text(ui("No categories match. Try another name."), Modifier.padding(12.dp)) }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(ui("Close")) } })
}
