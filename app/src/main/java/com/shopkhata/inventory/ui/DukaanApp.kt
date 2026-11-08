@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.shopkhata.inventory.ui

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.speech.RecognizerIntent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.shopkhata.inventory.BuildConfig
import com.shopkhata.inventory.data.PriceHistoryEntity
import com.shopkhata.inventory.data.SearchMissEntity
import com.shopkhata.inventory.domain.*
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val Green = Color(0xFF246654)
private val Cream = Color(0xFFF6F5EF)
private val Ink = Color(0xFF202C26)
private val Muted = Color(0xFF647169)
private val Border = Color(0xFFDEE3D9)
private val colors = lightColorScheme(primary = Green, onPrimary = Color.White, primaryContainer = Color(0xFFE1EEE3),
    secondary = Color(0xFF9A652D), secondaryContainer = Color(0xFFE1EEE3), onSecondaryContainer = Green, onPrimaryContainer = Green, surface = Color.White, background = Cream, onSurface = Ink,
    onBackground = Ink, surfaceVariant = Color(0xFFEDF0E8), outline = Color(0xFF7B877E), outlineVariant = Border)

@Composable
fun DukaanApp(vm: InventoryViewModel) {
    val uiLanguage by vm.uiLanguage.collectAsStateWithLifecycle()
    CompositionLocalProvider(LocalUiLanguage provides uiLanguage,LocalUiTranslations provides vm.uiTranslations) {
    MaterialTheme(colorScheme = colors, typography = Typography()) {
        val context = LocalContext.current
        val tab by vm.tab.collectAsStateWithLifecycle()
        val busy by vm.busy.collectAsStateWithLifecycle()
        val language by vm.language.collectAsStateWithLifecycle()
        val file by vm.shareFile.collectAsStateWithLifecycle()
        val snackbar = remember { SnackbarHostState() }
        var voiceForListing by remember { mutableStateOf(false) }
        val speech = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val text = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
                if (!text.isNullOrBlank()) {
                    if (voiceForListing) vm.listing.value = listOf(vm.listing.value, text).filter(String::isNotBlank).joinToString(". ")
                    else { vm.query.value = text; vm.submitSearch() }
                }
            }
        }
        fun voice(listing: Boolean) {
            voiceForListing = listing
            try {
                speech.launch(Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, language)
                    putExtra(RecognizerIntent.EXTRA_PROMPT, if (listing) "Speak items, sizes and prices" else "Ask for an item's price")
                })
            } catch (_: ActivityNotFoundException) { vm.message("Voice input isn't installed. You can type, or enable a speech service on your phone.") }
        }
        LaunchedEffect(vm,uiLanguage) { vm.messages.collect { snackbar.showSnackbar(vm.uiTranslations.text(it,uiLanguage)) } }
        LaunchedEffect(file) { file?.let {
            try {
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", it)
                context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                    type = "application/zip"; putExtra(Intent.EXTRA_STREAM, uri); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }, "Save your catalog backup"))
            } catch (_: Exception) { vm.message("Could not open the share sheet. Please try exporting again.") }
            finally { vm.shareFile.value = null }
        } }
        BackHandler(enabled = tab != 0 && !busy) { vm.tab.value = 0 }
        Scaffold(containerColor = Cream, snackbarHost = { SnackbarHost(snackbar) }, bottomBar = {
            NavigationBar(containerColor = Color.White, tonalElevation = 0.dp) {
                listOf("Catalog" to Icons.Outlined.Storefront, "Add items" to Icons.Outlined.AddCircleOutline, "Settings" to Icons.Outlined.Tune, "Reference" to Icons.AutoMirrored.Outlined.MenuBook).forEachIndexed { i, (label, icon) ->
                    NavigationBarItem(selected = tab == i, enabled = !busy, onClick = { vm.tab.value = i },
                        icon = { Icon(icon, label) }, label = { Text(ui(label)) }, colors = NavigationBarItemDefaults.colors(indicatorColor = Color(0xFFE1EEE3), selectedIconColor = Green, selectedTextColor = Green))
                }
            }
        }) { padding ->
            Column(Modifier.fillMaxSize().padding(padding)) {
                if (busy) LinearProgressIndicator(Modifier.fillMaxWidth(), color = Green)
                when (tab) {
                    0 -> CatalogScreen(vm, { voice(false) })
                    1 -> AddScreen(vm, { voice(true) })
                    3 -> ReferenceScreen(vm)
                    else -> SettingsScreen(vm)
                }
            }
        }
    }
    }
}

@Composable
private fun Eyebrow(text: String) { Text(ui(text).uppercase(), fontSize = 11.sp, letterSpacing = 1.8.sp, color = Green, fontWeight = FontWeight.Bold) }
@Composable
private fun Header(eyebrow: String, title: String, subtitle: String) {
    Column(Modifier.fillMaxWidth().padding(top = 28.dp, bottom = 22.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Eyebrow(eyebrow)
        Text(ui(title), fontFamily = FontFamily.Serif, fontSize = 34.sp, lineHeight = 40.sp, color = Ink)
        Text(ui(subtitle), color = Muted, style = MaterialTheme.typography.bodyMedium)
    }
}
@Composable
private fun Panel(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Surface(modifier, shape = RoundedCornerShape(20.dp), color = Color.White, border = BorderStroke(1.dp, Border)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
    }
}
@Composable
private fun EmptyState(icon: ImageVector, title: String, subtitle: String, action: @Composable () -> Unit = {}) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 28.dp), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Box(Modifier.size(82.dp).clip(RoundedCornerShape(24.dp)).background(Color(0xFFE1EEE3)), contentAlignment = Alignment.Center) {
            Icon(icon, null, Modifier.size(38.dp), tint = Green)
        }
        Text(ui(title), fontFamily = FontFamily.Serif, fontSize = 25.sp)
        Text(ui(subtitle), color = Muted, style = MaterialTheme.typography.bodyMedium, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        action()
    }
}
@Composable
private fun CatalogScreen(vm: InventoryViewModel, voice: () -> Unit) {
    val query by vm.query.collectAsStateWithLifecycle()
    val products by vm.products.collectAsStateWithLifecycle()
    val reference by vm.referenceCatalog.collectAsStateWithLifecycle()
    val hits by vm.hits.collectAsStateWithLifecycle()
    val rewriting by vm.rewriting.collectAsStateWithLifecycle()
    val tax by vm.taxLabel.collectAsStateWithLifecycle()
    val category by vm.searchCategory.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<Pair<Product, ProductVariant>?>(null) }
    var viewingId by rememberSaveable { mutableStateOf<Long?>(null) }
    var choosingCategory by rememberSaveable { mutableStateOf(false) }
    val categories = remember(products) { products.groupingBy { it.category }.eachCount().toSortedMap().map { CategoryChoice(it.key, it.value) } }
    val displayed = hits
    LaunchedEffect(products, category) {
        if (category != null && products.isNotEmpty() && products.none { it.category == category }) vm.searchCategory.value = null
    }
    LazyColumn(Modifier.fillMaxSize().testTag("catalog"), contentPadding = PaddingValues(start = 22.dp, end = 22.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Header("Your shop, in your pocket", "Dukaan", "Find the price. Get back to your customer.")
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                Box(Modifier.size(7.dp).background(Green, CircleShape))
                Text(ui("Search works offline"), color = Green, fontSize = 12.sp)
                Text(ui("·  ${products.size} ${if (products.size == 1) "item" else "items"}"), color = Muted, fontSize = 12.sp)
            }
        }
        item {
            OutlinedTextField(value = query, onValueChange = { vm.query.value = it }, modifier = Modifier.fillMaxWidth().testTag("search"),
                placeholder = { Text(ui("Search “tala 50mm”")) }, leadingIcon = { Icon(Icons.Outlined.Search, null) },
                trailingIcon = { Row { if (query.isNotBlank()) IconButton(onClick = { vm.query.value = "" }) { Icon(Icons.Outlined.Close, "Clear search") }
                    IconButton(onClick = voice) { Icon(Icons.Outlined.MicNone, "Speak a search", tint = Green) } } },
                singleLine = true, shape = RoundedCornerShape(16.dp), keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { vm.submitSearch() }), colors = OutlinedTextFieldDefaults.colors(unfocusedContainerColor = Color.White, focusedContainerColor = Color.White))
        }
        if (products.isNotEmpty()) item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(onClick = { choosingCategory = true }, modifier = Modifier.weight(1f).testTag("browse-categories")) {
                        Icon(Icons.Outlined.FilterList, null); Spacer(Modifier.width(6.dp))
                        Text(ui("Browse categories") + " (${categories.size})")
                    }
                    if (category != null) TextButton(onClick = { vm.searchCategory.value = null }) { Text(ui("All items")) }
                }
                if (category != null) Text(ui("Searching in") + ": " + if (category!!.isBlank()) ui("Uncategorized") else category!!, color = Green)
                else Text(ui("Search across all categories"), color = Muted, fontSize = 12.sp)
            }
        }

        if (products.isEmpty()) item {
            Panel {
                EmptyState(Icons.Outlined.Inventory2, "Your shelves, searchable", "Add your first item with its sizes and selling prices. Everything stays on this phone.") {
                    Button(onClick = vm::manual) { Icon(Icons.Outlined.Add, null); Spacer(Modifier.width(6.dp)); Text(ui("Add your first item")) }
                    TextButton(onClick = { vm.tab.value = 1 }) { Text(ui("Or speak a list of items")) }
                }
            }
            Text(ui("ताला हो या पेच — बस नाम से ढूँढें।"), Modifier.fillMaxWidth().padding(top = 22.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center, color = Muted, fontSize = 14.sp)
        }
        else {
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Eyebrow(if (query.isBlank()) "On your shelves" else "${displayed.size} matches")
                    TextButton(onClick = vm::manual) { Icon(Icons.Outlined.Add, null, Modifier.size(17.dp)); Text(ui("Add item")) }
                }
            }
            if (displayed.isEmpty()) item {
                EmptyState(Icons.Outlined.SearchOff, "No matching items", "Try another name, Hindi spelling or size.") {
                    if (category != null) OutlinedButton(onClick = { vm.searchCategory.value = null }) { Text(ui("Search all items")) }
                    if (query.isNotBlank()) OutlinedButton(onClick = vm::submitSearch, enabled = !rewriting) { Text(ui(if (rewriting) "Understanding your search…" else "Try smart search")) }
                    TextButton(onClick = vm::manual) { Text(ui("Add an item")) }
                }
            }
            if (rewriting) item { LinearProgressIndicator(Modifier.fillMaxWidth()); Text(ui("Understanding your search…"), color = Muted, fontSize = 12.sp) }
            items(displayed, key = { it.product.id }) { hit -> ProductCard(hit, tax, reference?.productFor(hit.product.variants.firstOrNull { it.sourceReferenceId!=null }?.sourceReferenceId), { vm.edit(hit.product) }, { viewingId = hit.product.id }) { editing = hit.product to it } }
        }
    }
    if (choosingCategory) CategoryPickerDialog(categories, category, { vm.searchCategory.value = it }, { choosingCategory = false })
    products.firstOrNull { it.id == viewingId }?.let { product ->
        ProductDetailsDialog(product, products, reference?.productFor(product.variants.firstOrNull { it.sourceReferenceId != null }?.sourceReferenceId), tax,
            { viewingId = null }, { viewingId = null; vm.edit(product) }, { viewingId = null; editing = product to it },
            { viewingId = it.id }, { viewingId = null; vm.query.value = ""; vm.searchCategory.value = product.category })
    }
    editing?.let { (product, variant) -> PriceDialog(vm, product, variant) { editing = null } }
}
@Composable
private fun ProductCard(hit: SearchHit, tax: String, sourceProduct: ReferenceProduct?, edit: () -> Unit, view: () -> Unit, price: (ProductVariant) -> Unit) {
    val context = LocalContext.current
    val imageAsset=sourceProduct?.imageAsset
    val displayName=if(hit.product.name==sourceProduct?.shopName) productLabel(hit.product.name) else hit.product.name
    var expanded by rememberSaveable(hit.product.id) { mutableStateOf(false) }
    Panel(Modifier.fillMaxWidth().clickable(onClick = view).testTag("product-${hit.product.id}")) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            if (hit.product.photoPath != null || imageAsset != null) AsyncImage(hit.product.photoPath?.let { File(context.filesDir, it) } ?: "file:///android_asset/$imageAsset", "Photo of ${hit.product.name}", Modifier.size(62.dp).clip(RoundedCornerShape(14.dp)), contentScale = ContentScale.Fit)
            else Box(Modifier.size(62.dp).clip(RoundedCornerShape(14.dp)).background(Cream), contentAlignment = Alignment.Center) { Icon(Icons.Outlined.Handyman, null, tint = Green) }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(displayName, fontWeight = FontWeight.SemiBold, fontSize = 19.sp)
                if(displayName!=hit.product.name)Text(hit.product.name,fontSize=11.sp,color=Muted)
                Text(listOf(hit.product.brand, hit.product.category).filter(String::isNotBlank).joinToString(" · "), color = Muted, fontSize = 12.sp)
            }
            IconButton(onClick = edit) { Icon(Icons.Outlined.Edit, "Edit ${hit.product.name}", tint = Muted, modifier = Modifier.size(20.dp)) }
        }
        HorizontalDivider(color = Border)
        (if (expanded) hit.variants else hit.variants.take(4)).forEach { variant ->
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable(onClick = view).padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(variant.spec.ifBlank { "Standard" }, fontSize = 14.sp)
                    Text(ui("Price updated ${date(variant.updatedAt)}"), color = Muted, fontSize = 10.sp)
                    if (variant.sourceReferenceId != null) Text(ui("Confirmed shop price · reference linked"), color = Green, fontSize = 10.sp)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(rupees(variant.pricePaise), fontWeight = FontWeight.Bold, fontSize = 26.sp, color = Green)
                    Text(ui("per ${ui(variant.unit)}"), fontSize = 11.sp, color = Muted)
                }
                Spacer(Modifier.width(8.dp)); IconButton(onClick = { price(variant) }) { Icon(Icons.Outlined.Edit, ui("Edit price"), Modifier.size(18.dp), tint = Muted) }
            }
        }
        if (hit.variants.size > 4) TextButton(onClick = { expanded = !expanded }) { Text(ui(if (expanded) "Show fewer sizes" else "Show all ${hit.variants.size} sizes")) }
        Text(ui("Latest price update: ${date(hit.product.variants.maxOfOrNull { it.updatedAt } ?: hit.product.updatedAt)}"), fontSize = 10.sp, color = Muted)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(tax, fontSize = 10.sp, color = Muted)
            val location = listOf(hit.product.zone, hit.product.locationNote).filter(String::isNotBlank).joinToString(" · ")
            if (location.isNotBlank()) Text(location, fontSize = 10.sp, color = Green, modifier = Modifier.weight(1f).padding(start = 12.dp), textAlign = androidx.compose.ui.text.style.TextAlign.End)
        }
    }
}
@Composable
private fun PriceDialog(vm: InventoryViewModel, product: Product, variant: ProductVariant, dismiss: () -> Unit) {
    var amount by remember(variant.id) { mutableStateOf(decimalPrice(variant.pricePaise)) }
    var discount by remember(variant.id) { mutableStateOf("") }
    var history by remember { mutableStateOf<List<PriceHistoryEntity>?>(null) }
    val busy by vm.busy.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    AlertDialog(onDismissRequest = { if (!busy) dismiss() }, title = { Text(ui("Update selling price")) }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(ui("${product.name} · ${variant.spec.ifBlank { "Standard" }}"))
            OutlinedTextField(amount, { amount = it }, label = { Text(ui("Rupees per ${ui(variant.unit)}")) }, singleLine = true,
                enabled = !busy, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), isError = paise(amount) == null)
            OutlinedTextField(discount, { discount = it }, label = { Text(ui("Discount (%) — optional")) }, singleLine = true,
                enabled = !busy, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), isError = discount.isNotBlank() && discountedPaise(amount, discount) == null)
            discountedPaise(amount, discount)?.let { Text(ui("Selling price: ${rupees(it)} per ${variant.unit}"), color = Green, fontWeight = FontWeight.SemiBold) }
            Text(ui("Last price update: ${date(variant.updatedAt)}. Discount applies once to the entered price."), fontSize = 11.sp, color = Muted)
            TextButton(onClick = { scope.launch { runCatching { vm.history(variant.id) }.onSuccess { history = it }.onFailure { vm.message("Could not load price history.") } } }) { Text(ui("View price history")) }
            history?.let { rows -> Column(Modifier.heightIn(max = 180.dp).verticalScroll(rememberScrollState())) {
                if (rows.isEmpty()) Text(ui("No price changes yet."), color = Muted)
                rows.forEach { Text(ui("${rupees(it.oldPrice)} → ${rupees(it.newPrice)} · ${date(it.changedAt)}"), fontSize = 12.sp, modifier = Modifier.padding(vertical = 4.dp)) }
            } }
        }
    }, confirmButton = { Button(onClick = { vm.price(product, variant, decimalPrice(requireNotNull(discountedPaise(amount, discount))), dismiss) }, enabled = !busy && discountedPaise(amount, discount) != null) { Text(ui("Save price")) } },
        dismissButton = { TextButton(onClick = dismiss, enabled = !busy) { Text(ui("Cancel")) } })
}

@Composable
private fun AddScreen(vm: InventoryViewModel, voice: () -> Unit) {
    val listing by vm.listing.collectAsStateWithLifecycle()
    val review by vm.review.collectAsStateWithLifecycle()
    val warnings by vm.warnings.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()
    val products by vm.products.collectAsStateWithLifecycle()
    val configured by vm.keyConfigured.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val listingImages by vm.listingImages.collectAsStateWithLifecycle()
    var photoIndex by remember { mutableStateOf<Int?>(null) }
    var cropUri by remember { mutableStateOf<Uri?>(null) }
    var photoShelf by remember { mutableStateOf(false) }
    var replacePhoto by remember { mutableStateOf<Int?>(null) }
    var cropQueue by remember { mutableStateOf<List<Uri>>(emptyList()) }
    fun nextCrop() {cropUri=cropQueue.firstOrNull();cropQueue=cropQueue.drop(1);replacePhoto=null}
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(6)) { uris ->
        val size=photoIndex?.let {review.getOrNull(it)?.let {d->if(photoShelf)d.shelfPhotoPaths.size else d.productPhotos().size}} ?: listingImages.size
        if(uris.size>6-size)vm.message("Choose up to 6 photos per album.")
        else {cropUri=uris.firstOrNull();cropQueue=uris.drop(1);replacePhoto=null}
    }
    LazyColumn(Modifier.fillMaxSize().testTag("add-list"), contentPadding = PaddingValues(start = 22.dp, end = 22.dp, bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { Header("Less typing, more selling", if (review.isEmpty()) "Stock your catalog" else "Check before saving", if (review.isEmpty()) "Speak or type the way you do at the counter." else "Confirm names, sizes and every selling price.") }
        if (review.isEmpty()) {
            item { Panel {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) { Icon(Icons.Outlined.AutoAwesome, null, tint = Green, modifier = Modifier.size(20.dp)); Text(ui("Tell us what's on the shelf"), fontWeight = FontWeight.SemiBold) }
                OutlinedTextField(listing, { vm.listing.value = it }, modifier = Modifier.fillMaxWidth().testTag("listing-input"), enabled = !busy,
                    placeholder = { Text(ui("Godrej tala 50mm 450, 65mm 600. Wood screw 1 inch 90 per packet.")) }, minLines = 5, maxLines = 10, shape = RoundedCornerShape(12.dp))
                PhotoAlbumEditor("AI input images",listingImages,busy,
                    {photoIndex=null;photoShelf=false;picker.launch(androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))},
                    {i->photoIndex=null;replacePhoto=i;cropUri=Uri.fromFile(File(context.filesDir,listingImages[i]))},
                    {i->vm.listingImages.value=listingImages.filterIndexed {index,_->index!=i}})
                Text(ui("Only these selected images are sent when you request AI review."),style=MaterialTheme.typography.bodySmall)
                Text(ui("Text or image · AI suggests categories · Always reviewed before saving"), color = Muted, fontSize = 11.sp)
                if (!configured) TextButton(onClick = { vm.tab.value = 2 }) { Text(ui("Set up OpenAI to use natural entry")) }
            } }
            item { Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                HorizontalDivider(Modifier.weight(1f), color = Border); Text(ui("  or  "), color = Muted); HorizontalDivider(Modifier.weight(1f), color = Border)
            } }
            item { OutlinedButton(onClick = vm::manual, enabled = !busy, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(14.dp)) {
                Icon(Icons.Outlined.EditNote, null); Spacer(Modifier.width(8.dp)); Text(ui("Add an item manually"))
            }; Text(ui("Works without internet. Add a name, size and price."), color = Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 10.dp)) }
        } else {
            if (warnings.isNotEmpty()) item { Surface(color = Color(0xFFFFEACB), shape = RoundedCornerShape(12.dp)) {
                Text(ui(warnings.joinToString("\n")), Modifier.padding(14.dp), color = Color(0xFF755020), fontSize = 13.sp)
            } }
            items(review.size) { index ->
                val draft = review[index]
                ItemEditor(draft, index, busy, CatalogSearch.duplicate(draft, products), { vm.updateDraft(index, it) },
                    { vm.removeDraft(index) }, { shelf->photoIndex=index;photoShelf=shelf;replacePhoto=null;picker.launch(androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                    { shelf,i ->photoIndex=index;photoShelf=shelf;replacePhoto=i;val path=if(shelf)draft.shelfPhotoPaths[i]else draft.productPhotos()[i];cropUri=Uri.fromFile(File(context.filesDir,path)) },
                    { vm.categorize(index) }, configured, vm)
            }
            item {
                val error = review.firstNotNullOfOrNull { it.error() }
                if (error != null) Text(ui(error), color = MaterialTheme.colorScheme.error, fontSize = 12.sp, modifier = Modifier.padding(bottom = 10.dp))
                Button(onClick = vm::save, enabled = !busy && error == null, modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(14.dp)) {
                    Icon(Icons.Outlined.Check, null); Spacer(Modifier.width(8.dp)); Text(ui(if (busy) "Saving…" else "Save ${review.size} ${if (review.size == 1) "item" else "items"} on this phone"))
                }
                TextButton(onClick = vm::cancelReview, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text(ui("Discard this review")) }
            }
        }
    }
    cropUri?.let { uri -> ImageCropDialog(uri, { cropUri=null;cropQueue=emptyList();replacePhoto=null }) { bitmap -> vm.croppedPhoto(photoIndex, bitmap, photoShelf, replacePhoto) { nextCrop() } } }
}

@Composable
private fun ItemEditor(draft: ItemDraft, index: Int, busy: Boolean, duplicate: Product?, change: (ItemDraft) -> Unit, remove: () -> Unit, photo: (Boolean) -> Unit, recrop: (Boolean, Int) -> Unit, categorize: () -> Unit, aiConfigured: Boolean, vm: InventoryViewModel) {
    val context = LocalContext.current
    val catalogProducts by vm.products.collectAsStateWithLifecycle()
    val aiImages by vm.listingImages.collectAsStateWithLifecycle()
    var more by rememberSaveable(index) { mutableStateOf(false) }
    Panel(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Eyebrow(if (draft.targetId != null) "Update existing item" else "New item · ${index + 1}")
            IconButton(onClick = remove, enabled = !busy) { Icon(Icons.Outlined.Close, "Remove item from review", Modifier.size(18.dp)) }
        }
        OutlinedTextField(draft.name, { change(draft.copy(name = it)) }, Modifier.fillMaxWidth().testTag("item-name-$index"), enabled = !busy, label = { Text(ui("Item name *")) }, singleLine = true)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(draft.brand, { change(draft.copy(brand = it)) }, Modifier.weight(1f), enabled = !busy, label = { Text(ui("Brand")) }, singleLine = true)
            OutlinedTextField(draft.category, { change(draft.copy(category = it)) }, Modifier.weight(1f), enabled = !busy, label = { Text(ui("Category")) }, singleLine = true)
        }
        var choosingCategory by remember { mutableStateOf(false) }
        if (catalogProducts.isNotEmpty()) TextButton(onClick = { choosingCategory = true }, enabled = !busy) { Text(ui("Use an existing category")) }
        if (choosingCategory) CategoryPickerDialog(
            catalogProducts.groupingBy { it.category }.eachCount().toSortedMap().map { CategoryChoice(it.key, it.value) },
            draft.category, { change(draft.copy(category = it.orEmpty())) }, { choosingCategory = false }, allowAll = false)
        TextButton(onClick = categorize, enabled = !busy && aiConfigured && (draft.name.isNotBlank() || draft.photoPath != null)) {
            Icon(Icons.Outlined.AutoAwesome, null, Modifier.size(16.dp)); Spacer(Modifier.width(6.dp)); Text(ui("Suggest category & aliases"))
        }
        if (draft.targetId != null) {
            Text(ui("Prices for matching sizes will update. Review carefully before saving."), color = Green, fontSize = 12.sp)
            TextButton(onClick = { change(draft.copy(targetId = null)) }, enabled = !busy) { Text(ui("Save as a separate item instead")) }
        } else if (duplicate != null) {
            Text(ui("${duplicate.name}${if (duplicate.brand.isNotBlank()) " · ${duplicate.brand}" else ""} already exists."), color = Color(0xFF9A652D), fontSize = 12.sp)
            TextButton(onClick = { change(draft.copy(targetId = duplicate.id, extraPhotoPaths=duplicate.extraPhotoPaths, shelfPhotoPaths=duplicate.shelfPhotoPaths, photoPath = duplicate.photoPath, zone = duplicate.zone, locationNote = duplicate.locationNote,
                aliases = (duplicate.aliases + draft.aliases.split(',')).distinct().joinToString(", "))) }, enabled = !busy) { Text(ui("Update that item instead")) }
        }
        Text(ui("SIZES & SELLING PRICES"), fontSize = 10.sp, letterSpacing = 1.sp, color = Muted, fontWeight = FontWeight.Bold)
        draft.variants.forEachIndexed { variantIndex, variant ->
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                variant.sourceReferenceId?.let { SourcePriceNote(vm, it) }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(variant.spec, { value -> change(draft.copy(variants = draft.variants.toMutableList().also { it[variantIndex] = variant.copy(spec = value) })) },
                        Modifier.weight(1f), enabled = !busy, label = { Text(ui("Size / spec")) }, placeholder = { Text(ui("e.g. 50 mm")) }, singleLine = true)
                    if (draft.variants.size > 1) IconButton(onClick = { change(draft.copy(variants = draft.variants.filterIndexed { i, _ -> i != variantIndex })) }, enabled = !busy) { Icon(Icons.Outlined.RemoveCircleOutline, "Remove size") }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(variant.price, { value -> change(draft.copy(variants = draft.variants.toMutableList().also { it[variantIndex] = variant.copy(price = value) })) },
                        Modifier.weight(1f).testTag("item-price-$index-$variantIndex"), enabled = !busy, label = { Text(ui("Price (₹) *")) }, singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                    UnitPicker(variant.unit, !busy, Modifier.weight(1f)) { value -> change(draft.copy(variants = draft.variants.toMutableList().also { it[variantIndex] = variant.copy(unit = value) })) }
                }
                OutlinedTextField(variant.discount, { value -> change(draft.copy(variants = draft.variants.toMutableList().also { it[variantIndex] = variant.copy(discount = value) })) },
                    Modifier.fillMaxWidth().testTag("item-discount-$index-$variantIndex"), enabled = !busy, label = { Text(ui("Discount (%) — optional")) }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                discountedPaise(variant.price, variant.discount)?.let { Text(ui("Selling price: ${rupees(it)} per ${variant.unit}"), color = Green, fontSize = 13.sp) }
            }
        }
        TextButton(onClick = { change(draft.copy(variants = draft.variants + VariantDraft())) }, enabled = !busy) { Icon(Icons.Outlined.Add, null, Modifier.size(18.dp)); Text(ui("Add another size")) }
        OutlinedTextField(draft.aliases, { change(draft.copy(aliases = it)) }, Modifier.fillMaxWidth(), enabled = !busy,
            label = { Text(ui("Other names, separated by commas")) }, placeholder = { Text(ui("tala, ताला, lock")) }, minLines = 1, maxLines = 3)
        if(aiImages.isNotEmpty()) TextButton(onClick={change(draft.withProductPhotos(draft.productPhotos()+aiImages))},
            enabled=!busy&&(draft.productPhotos()+aiImages).distinct().size<=6) {Text(ui("Use AI images as product photos"))}
        PhotoAlbumEditor("Product photos",draft.productPhotos(),busy,{photo(false)},{recrop(false,it)},
            {i->change(draft.withProductPhotos(draft.productPhotos().filterIndexed {index,_->index!=i}))},
            {i->val album=draft.productPhotos();change(draft.withProductPhotos(listOf(album[i])+album.filterIndexed {index,_->index!=i}))})
        PhotoAlbumEditor("Shelf photos",draft.shelfPhotoPaths,busy,{photo(true)},{recrop(true,it)},
            {i->change(draft.copy(shelfPhotoPaths=draft.shelfPhotoPaths.filterIndexed {index,_->index!=i}))})
        Text(ui("Shelf photos stay local and are not sent with category suggestions."),style=MaterialTheme.typography.labelSmall)
        TextButton(onClick = { more = !more }) { Text(ui(if (more) "Hide shelf location" else "Add shelf location (optional)")) }
        if (more || draft.zone.isNotBlank() || draft.locationNote.isNotBlank()) {
            OutlinedTextField(draft.zone, { change(draft.copy(zone = it)) }, Modifier.fillMaxWidth(), enabled = !busy, label = { Text(ui("Zone")) }, singleLine = true)
            OutlinedTextField(draft.locationNote, { change(draft.copy(locationNote = it)) }, Modifier.fillMaxWidth(), enabled = !busy, label = { Text(ui("Location note")) }, singleLine = true)
        }
    }
}
@Composable
private fun UnitPicker(unit: String, enabled: Boolean, modifier: Modifier = Modifier, change: (String) -> Unit) {
    var open by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = open, onExpandedChange = { if (enabled) open = !open }, modifier = modifier) {
        OutlinedTextField(ui(unit.ifBlank { "Choose unit" }), {}, modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable), readOnly = true, enabled = enabled,
            label = { Text(ui("Per unit")) }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(open) })
        ExposedDropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            units.forEach { value -> DropdownMenuItem(text = { Text(ui(value)) }, onClick = { change(value); open = false }) }
        }
    }
}

@Composable
private fun SettingsScreen(vm: InventoryViewModel) {
    val uiLanguage by vm.uiLanguage.collectAsStateWithLifecycle()
    val configured by vm.keyConfigured.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()
    val tax by vm.taxLabel.collectAsStateWithLifecycle()
    val language by vm.language.collectAsStateWithLifecycle()
    val currentModel by vm.model.collectAsStateWithLifecycle()
    val misses by vm.misses.collectAsStateWithLifecycle()
    var key by remember { mutableStateOf("") }
    var model by remember(currentModel) { mutableStateOf(currentModel) }
    var restoreUri by remember { mutableStateOf<Uri?>(null) }
    var showMisses by rememberSaveable { mutableStateOf(false) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { restoreUri = it }
    if (showMisses) {
        MissesScreen(vm) { showMisses = false }
        return
    }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 22.dp, end = 22.dp, bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { Header("Made for your counter", "Your shop settings", "Your catalog lives on this phone. Keep a backup.") }
        item { Panel {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Outlined.Key, null, tint = Green); Text(ui("OpenAI"), fontWeight = FontWeight.SemiBold, fontSize = 18.sp) }
            Text(ui(if (configured) "Key saved securely on this phone" else "Connect natural entry and smart search"), color = Green, fontSize = 12.sp)
            Text(ui("Submitted text and images are sent to OpenAI for parsing or category suggestions. Your full catalog stays on this phone."), color = Muted, fontSize = 12.sp)
            OutlinedTextField(key, { key = it }, modifier = Modifier.fillMaxWidth(), enabled = !busy, label = { Text(ui(if (configured) "Replace API key" else "OpenAI API key")) },
                singleLine = true, visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, autoCorrectEnabled = false))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { vm.saveKey(key); key = "" }, enabled = key.startsWith("sk-") && !busy) { Text(ui("Save key")) }
                if (configured) TextButton(onClick = vm::forgetKey, enabled = !busy) { Text(ui("Remove key")) }
            }
            OutlinedTextField(model, { model = it }, Modifier.fillMaxWidth(), enabled = !busy, label = { Text(ui("Model")) }, singleLine = true)
            if (model != currentModel) TextButton(onClick = { vm.setModel(model) }, enabled = !busy) { Text(ui("Save model")) }
        } }
        item { Panel {
            Text(ui("App language"),fontWeight=FontWeight.SemiBold,fontSize=18.sp)
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                listOf("en" to "English","hi" to "हिन्दी","hinglish" to "Hinglish").forEach { (code,label) ->
                    FilterChip(selected=uiLanguage==code,onClick={vm.displayLanguage(code)},label={Text(ui(label))},modifier=Modifier.testTag("ui-language-$code"),enabled=!busy)
                }
            }
            Text(ui("Display language only; your saved names and original source stay unchanged."),fontSize=12.sp,color=Muted)
        } }
        item { Panel {
            Text(ui("Prices & voice"), fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
            Text(ui("How are your selling prices recorded?"), color = Muted, fontSize = 12.sp)
            listOf("GST not specified", "incl. GST", "excl. GST").forEach { value ->
                Row(Modifier.fillMaxWidth().clickable(enabled = !busy) { vm.tax(value) }, verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = tax == value, onClick = { vm.tax(value) }, enabled = !busy); Text(ui(value))
                }
            }
            Text(ui("Voice language"), color = Muted, fontSize = 12.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = language == "hi-IN", onClick = { vm.voiceLanguage("hi-IN") }, label = { Text(ui("हिन्दी / Hinglish")) }, enabled = !busy)
                FilterChip(selected = language == "en-IN", onClick = { vm.voiceLanguage("en-IN") }, label = { Text(ui("English")) }, enabled = !busy)
            }
        } }
        item { Panel {
            Text(ui("Keep your catalog safe"), fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
            Text(ui("Export a ZIP with all items, photos and price history. Save it to Drive, WhatsApp or another safe place. API keys are excluded."), color = Muted, fontSize = 12.sp)
            Button(onClick = vm::export, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Outlined.IosShare, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text(ui("Export backup ZIP")) }
            OutlinedButton(onClick = { picker.launch(arrayOf("application/zip", "application/octet-stream", "application/x-zip-compressed")) }, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Outlined.Restore, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text(ui("Restore from backup"))
            }
        } }
        item { Panel {
            Text(ui("Help your catalog learn"), fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
            Text(ui("Save a customer's words as another name for an item. Future searches will find it offline."), color = Muted, fontSize = 12.sp)
            OutlinedButton(onClick = { showMisses = true }, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text(ui("Missed searches · ${misses.size}")) }
        } }
        item { Text(ui("Dukaan · v${BuildConfig.VERSION_NAME}\nBuilt for one phone, one shop."), color = Muted, fontSize = 11.sp, modifier = Modifier.fillMaxWidth(), textAlign = androidx.compose.ui.text.style.TextAlign.Center) }
    }
    restoreUri?.let { uri -> AlertDialog(onDismissRequest = { restoreUri = null }, title = { Text(ui("Replace this phone's catalog?")) },
        text = { Text(ui("Restoring replaces all current items and history with the backup. Export your current catalog first if you need to keep it. Your OpenAI key stays unchanged.")) },
        confirmButton = { Button(onClick = { restoreUri = null; vm.restore(uri) }, enabled = !busy) { Text(ui("Restore backup")) } },
        dismissButton = { TextButton(onClick = { restoreUri = null }) { Text(ui("Cancel")) } }) }
}
@Composable
private fun MissesScreen(vm: InventoryViewModel, back: () -> Unit) {
    val misses by vm.misses.collectAsStateWithLifecycle()
    val products by vm.products.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()
    var selected by remember { mutableStateOf<SearchMissEntity?>(null) }
    var query by remember { mutableStateOf("") }
    BackHandler(onBack = back)
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(22.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { TextButton(onClick = back) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, null); Text(ui("Settings")) }; Header("A catalog that learns", "Missed searches", "Connect a customer's words to the right item.") }
        if (misses.isEmpty()) item { EmptyState(Icons.Outlined.CheckCircleOutline, "All caught up", "When a submitted search has no local match, it will appear here.") }
        items(misses, key = { it.id }) { miss -> Panel(Modifier.fillMaxWidth()) {
            Text(ui("“${miss.query}”"), fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
            Text(date(miss.searchedAt), color = Muted, fontSize = 12.sp)
            TextButton(onClick = { selected = miss; query = "" }, enabled = !busy && products.isNotEmpty()) { Text(ui("Choose item & save as alias")) }
        } }
    }
    selected?.let { miss -> AlertDialog(onDismissRequest = { selected = null }, title = { Text(ui("Which item is “${miss.query}”?")) }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(query, { query = it }, label = { Text(ui("Find an item")) }, singleLine = true)
            LazyColumn(Modifier.heightIn(max = 320.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                val matches = if (query.isBlank()) products else CatalogSearch.search(products, query).map { it.product }
                items(matches, key = { it.id }) { product -> TextButton(onClick = { vm.learn(miss, product); selected = null }, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
                    Text(listOf(product.name, product.brand).filter(String::isNotBlank).joinToString(" · "))
                } }
            }
        }
    }, confirmButton = {}, dismissButton = { TextButton(onClick = { selected = null }) { Text(ui("Cancel")) } }) }
}
private fun date(timestamp: Long) = SimpleDateFormat("d MMM yyyy", Locale.forLanguageTag("en-IN")).format(Date(timestamp))
