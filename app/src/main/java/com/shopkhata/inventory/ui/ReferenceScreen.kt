package com.shopkhata.inventory.ui

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.shopkhata.inventory.domain.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import coil.compose.AsyncImage

@Composable
fun ReferenceScreen(vm: InventoryViewModel) {
    val catalog by vm.referenceCatalog.collectAsStateWithLifecycle()
    val error by vm.referenceError.collectAsStateWithLifecycle()
    val shop by vm.products.collectAsStateWithLifecycle()
    val selected by vm.referenceSelected.collectAsStateWithLifecycle()
    val translations=LocalUiTranslations.current
    var query by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf("") }
    var limit by rememberSaveable(query, category) { mutableIntStateOf(20) }
    var page by remember { mutableStateOf<Int?>(null) }
    val active = remember(shop) { shop.flatMap { it.variants }.mapNotNull { it.sourceReferenceId }.toSet() }
    val groups = remember(catalog,query,category) {
        val terms = normalize(query).split(' ').filter { it.isNotEmpty() }
        val code = query.trim().takeIf { Regex("(?:SIM|IM|IT|IP)\\d{5,8}",RegexOption.IGNORE_CASE).matches(it) }
        catalog?.products.orEmpty().filter { category.isBlank() || it.category == category }.mapNotNull { group ->
            val names=translations.searchText("Prince ${group.name} ${group.category}")
            val variants = group.variants.filter { v -> if(code!=null) v.code?.equals(code,ignoreCase=true)==true else terms.all { it in normalize("$names ${v.label} ${v.code} ${v.rawRow}") } }
            if (variants.isEmpty()) null else group.copy(variants = variants)
        }
    }
    Column(Modifier.fillMaxSize()) {
        LazyColumn(Modifier.weight(1f).testTag("reference-list"),contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
            item {
                Text(ui("Reference library"),style=MaterialTheme.typography.headlineMedium)
                Text(ui("Prince pricelist · effective 1 Jan 2026"),style=MaterialTheme.typography.titleSmall)
                Text(ui("Stock unconfirmed. Hidden from your shop catalog until you select the variants you carry and confirm their selling prices. No quantity is assumed."))
                Text(ui("Source filename date: 30 Jan 2026. GST extra in the list; your shop tax label is separate."),style=MaterialTheme.typography.bodySmall)
                TextButton(onClick={page=1}) { Text(ui("Read original PDF")) }
            }
            item { OutlinedTextField(query,{query=it},Modifier.fillMaxWidth().testTag("reference-search"),label={Text(ui("Search reference · name, size or code"))},singleLine=true) }
            item {
                var open by remember { mutableStateOf(false) }
                OutlinedButton(onClick={open=true}) { Text(category.ifBlank { "All product families" }) }
                DropdownMenu(expanded=open,onDismissRequest={open=false}) {
                    (listOf("")+catalog?.products.orEmpty().map { it.category }.distinct()).forEach { c ->
                        DropdownMenuItem(text={Text(if(c.isBlank())ui("All product families")else productLabel(c))},onClick={category=c;open=false})
                    }
                }
                Text(ui("${groups.size} groups · ${groups.sumOf { it.variants.size }} variants · ${active.size} linked shop variants"),style=MaterialTheme.typography.bodySmall)
            }
            if (error != null) item { Text(ui(error!!),color=MaterialTheme.colorScheme.error) }
            if (catalog==null && error==null) item { Text(ui("Loading local reference library…")) }
            items(groups.take(limit),key={it.id}) { group ->
                var expanded by rememberSaveable(group.id) { mutableStateOf(false) }
                Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
                    group.imageAsset?.let { AsyncImage("file:///android_asset/$it","Source illustration of ${group.name}",Modifier.fillMaxWidth().height(130.dp)) }
                    Text(ui(if(group.imageAsset!=null) "Manufacturer illustration; size or colour may vary." else "No matched product picture"),style=MaterialTheme.typography.labelSmall)
                    Text(productLabel(group.name),style=MaterialTheme.typography.titleMedium)
                    if(LocalUiLanguage.current!="en")Text(group.name,style=MaterialTheme.typography.labelSmall)
                    Text(ui("${group.category} · PDF p.${group.page}"),style=MaterialTheme.typography.bodySmall)
                    if(group.priceListLabel.isNotBlank()) Text(group.priceListLabel,style=MaterialTheme.typography.bodySmall)
                    Row {
                        TextButton(onClick={expanded=!expanded}) { Text(ui(if(expanded) "Hide variants" else "Choose sizes (${group.variants.size})")) }
                        TextButton(onClick={page=group.page}) { Text(ui("Source page")) }
                    }
                    if(expanded) group.variants.forEach { variant ->
                        HorizontalDivider()
                        Row {
                            Checkbox(checked=variant.id in selected || variant.id in active,enabled=variant.id !in active,
                                onCheckedChange={vm.selectReference(variant.id,it)},modifier=Modifier.testTag("reference-select-${variant.id}"))
                            Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(5.dp)) {
                                Text(variant.shopSpec(),style=MaterialTheme.typography.bodyMedium)
                                Text(ui(if(variant.id in active) "Carried · shop price confirmed · quantity unknown" else "Stock unconfirmed · reference only"),style=MaterialTheme.typography.labelSmall)
                                Text(variant.prices.joinToString("\n") { (label,value) -> "Source $label: ₹$value" }.ifBlank { "Source price not resolved. Read the PDF." },style=MaterialTheme.typography.bodySmall)
                                if(variant.issues.isNotEmpty()) Text(ui(variant.issues.joinToString("\n")),color=MaterialTheme.colorScheme.error,style=MaterialTheme.typography.bodySmall)
                                var details by remember { mutableStateOf(false) }
                                TextButton(onClick={details=!details}) { Text(ui(if(details) "Hide source fields" else "Show exact source row")) }
                                if(details) Text(variant.fields.joinToString("\n") { (k,v) -> "$k: $v" }.ifBlank { variant.rawRow },style=MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                } }
            }
            if(groups.size>limit) item { OutlinedButton(onClick={limit+=20}) { Text(ui("Show more groups")) } }
        }
        Surface(shadowElevation=4.dp) { Column(Modifier.fillMaxWidth().padding(14.dp)) {
            Text(ui("${selected.count { it !in active }} selected · not saved yet"),style=MaterialTheme.typography.bodySmall)
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                Button(onClick=vm::reviewReferences,enabled=selected.any { it !in active },modifier=Modifier.weight(1f)) { Text(ui("Review carried products")) }
                TextButton(onClick={vm.referenceSelected.value=emptySet()}) { Text(ui("Clear")) }
            }
        } }
    }
    page?.let { PdfSourceDialog(it) { page=null } }
}

@Composable
fun SourcePriceNote(vm: InventoryViewModel, id: String) {
    val catalog by vm.referenceCatalog.collectAsStateWithLifecycle()
    val variant=catalog?.variants?.get(id)
    val product=catalog?.products?.firstOrNull { p -> p.variants.any { it.id==id } }
    var page by remember { mutableStateOf<Int?>(null) }
    Surface(color=MaterialTheme.colorScheme.secondaryContainer,shape=MaterialTheme.shapes.small) {
        Column(Modifier.fillMaxWidth().padding(12.dp)) {
            Text(ui("Reference source · PDF p.${product?.page ?: "?"} · cover effective 1 Jan 2026"),style=MaterialTheme.typography.labelMedium)
            product?.priceListLabel?.takeIf { it.isNotBlank() }?.let { Text(it,style=MaterialTheme.typography.bodySmall) }
            Text(variant?.prices?.joinToString("\n") { (key,value)->"$key: ₹$value" }?.ifBlank { "No resolved source price" } ?: "Source link: $id",style=MaterialTheme.typography.bodySmall)
            Text(ui("Source prices are not your confirmed selling price. Confirm that you carry this variant, its specification, selling unit and tax label. Quantity remains unknown."),style=MaterialTheme.typography.bodySmall)
            product?.let { TextButton(onClick={page=it.page}) { Text(ui("Read source page")) } }
        }
    }
    page?.let { PdfSourceDialog(it) {page=null} }
}

@Composable
private fun PdfSourceDialog(initialPage: Int, close: () -> Unit) {
    val context=LocalContext.current
    var page by remember { mutableIntStateOf(initialPage) }
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var zoom by remember(page) { mutableFloatStateOf(1f) }
    var pan by remember(page) { mutableStateOf(Offset.Zero) }
    val transform=rememberTransformableState { scale,offset,_ -> zoom=(zoom*scale).coerceIn(1f,4f);pan+=offset }
    LaunchedEffect(page) {
        bitmap=null;error=null
        try {
            bitmap=withContext(Dispatchers.IO) {
                val file=File(context.cacheDir,"reference/prince-2026.pdf")
                if(!file.exists()) { file.parentFile?.mkdirs();context.assets.open("reference/prince-2026.pdf").use { input -> file.outputStream().use { input.copyTo(it) } } }
                ParcelFileDescriptor.open(file,ParcelFileDescriptor.MODE_READ_ONLY).use { fd ->
                    PdfRenderer(fd).use { renderer -> renderer.openPage(page-1).use { source ->
                        Bitmap.createBitmap(source.width*2,source.height*2,Bitmap.Config.ARGB_8888).also { source.render(it,null,null,PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY) }
                    } }
                }
            }
        } catch (_:Exception) {error="Source PDF is unavailable in this build. Reference rows and page numbers remain available."}
    }
    androidx.compose.ui.window.Dialog(onDismissRequest=close,properties=androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth=false)) {
        Surface(Modifier.fillMaxSize().padding(8.dp)) { Column(Modifier.padding(10.dp)) {
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
                TextButton(onClick={page--},enabled=page>1) {Text(ui("Previous"))}
                Text(ui("PDF page $page / 92"))
                TextButton(onClick={page++},enabled=page<92) {Text(ui("Next"))}
            }
            bitmap?.let { image ->
                Box(Modifier.weight(1f).fillMaxWidth().clipToBounds().transformable(transform)) {
                    Image(image.asImageBitmap(),"Original pricelist page $page",Modifier.fillMaxSize().graphicsLayer(scaleX=zoom,scaleY=zoom,translationX=pan.x,translationY=pan.y))
                }
                Text(ui("Zoom to read · drag to move"),style=MaterialTheme.typography.bodySmall)
                Slider(value=zoom,onValueChange={zoom=it;if(it==1f)pan=Offset.Zero},valueRange=1f..4f)
            }
            if(bitmap==null) Text(error ?: "Loading page…",Modifier.weight(1f))
            TextButton(onClick=close,modifier=Modifier.fillMaxWidth()) {Text(ui("Close source"))}
        } }
    }
}
