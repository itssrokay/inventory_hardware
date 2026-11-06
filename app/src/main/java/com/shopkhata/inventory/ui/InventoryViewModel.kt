package com.shopkhata.inventory.ui

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.shopkhata.inventory.ai.OpenAiClient
import com.shopkhata.inventory.backup.BackupManager
import com.shopkhata.inventory.data.*
import com.shopkhata.inventory.domain.*
import com.shopkhata.inventory.security.ApiKeyStore
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import java.io.File

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class InventoryViewModel(application: Application) : AndroidViewModel(application) {
    private val db = CatalogDatabase.open(application)
    private val repository = CatalogRepository(db)
    private val keys = ApiKeyStore(application)
    private val ai = OpenAiClient(application, keys)
    private val backups = BackupManager(application, db)
    private val photos = PhotoStore(application)
    private val prefs = application.getSharedPreferences("settings", Context.MODE_PRIVATE)
    val products = repository.products.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val misses = repository.misses.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val query = MutableStateFlow("")
    val searchCategory = MutableStateFlow<String?>(null)
    val listing = MutableStateFlow("")
    val review = MutableStateFlow<List<ItemDraft>>(emptyList())
    val warnings = MutableStateFlow<List<String>>(emptyList())
    val busy = MutableStateFlow(false)
    val rewriting = MutableStateFlow(false)
    val tab = MutableStateFlow(0)
    val keyConfigured = MutableStateFlow(false)
    val taxLabel = MutableStateFlow(prefs.getString("tax_label", "GST not specified")!!)
    val language = MutableStateFlow(prefs.getString("language", "hi-IN")!!)
    val model = MutableStateFlow(prefs.getString("model", "gpt-4.1-mini")!!)
    val shareFile = MutableStateFlow<File?>(null)
    private val rewrites = MutableStateFlow<Pair<String, List<String>>?>(null)
    private var fullEdit = false
    private val messagesChannel = Channel<String>(Channel.BUFFERED)
    val messages = messagesChannel.receiveAsFlow()
    val hits = combine(products, query.debounce(100), rewrites, searchCategory) { products, query, rewrite, category ->
        Triple(products.filter { category == null || it.category == category }, query, rewrite)
    }
        .mapLatest { (catalog, query, rewrite) -> withContext(Dispatchers.Default) {
            val local = repository.search(catalog, query)
            if (rewrite?.first == query) (local + rewrite.second.flatMap { repository.search(catalog, it) }).distinctBy { it.product.id }.sortedByDescending { it.score }.take(10)
            else local
        } }.catch { message("Search failed. Please try again."); emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    init {
        runCatching { keys.importDebugKey() }.onFailure { message("Could not import the development key. Add it in Settings.") }
        keyConfigured.value = keys.read() != null
    }
    fun message(value: String) { messagesChannel.trySend(value) }
    private fun operation(block: suspend () -> Unit) {
        if (busy.value) return
        viewModelScope.launch {
            busy.value = true
            try { block() }
            catch (error: CancellationException) { throw error }
            catch (error: Exception) { message(error.message ?: "Something went wrong. Please try again.") }
            finally { busy.value = false }
        }
    }
    fun manual() {
        if (busy.value) return
        fullEdit = false; review.value = listOf(ItemDraft()); warnings.value = emptyList(); tab.value = 1
    }
    fun edit(product: Product) {
        if (busy.value) return
        fullEdit = true; review.value = listOf(product.draft()); warnings.value = emptyList(); tab.value = 1
    }
    fun updateDraft(index: Int, value: ItemDraft) { review.value = review.value.toMutableList().also { it[index] = value } }
    fun removeDraft(index: Int) { review.value = review.value.filterIndexed { i, _ -> i != index } }
    fun cancelReview() { if (!busy.value) { review.value = emptyList(); warnings.value = emptyList(); fullEdit = false } }
    fun parse() = operation {
        val parsed = ai.parse(listing.value)
        fullEdit = false
        review.value = parsed.items.map { draft ->
            val duplicate = CatalogSearch.duplicate(draft, products.value)
            if (duplicate == null) draft else draft.copy(targetId = duplicate.id, photoPath = duplicate.photoPath, zone = duplicate.zone, locationNote = duplicate.locationNote,
                aliases = (duplicate.aliases + draft.aliases.split(',').map { it.trim() }).distinct().joinToString(", "))
        }
        warnings.value = parsed.warnings
        if (parsed.items.isEmpty()) message(parsed.warnings.firstOrNull() ?: "No items found. Try a name, size and price.")
    }
    fun save() = operation {
        val items = review.value
        items.forEach { require(it.error() == null) { it.error() ?: "Check the item." } }
        require(items.map { normalize(it.name) to normalize(it.brand) }.distinct().size == items.size) { "Combine duplicate items into one card before saving." }
        val result = repository.save(items, fullEdit)
        review.value = emptyList(); warnings.value = emptyList(); listing.value = ""; fullEdit = false
        query.value = ""; searchCategory.value = null; rewrites.value = null; tab.value = 0
        message("${result.size} ${if (result.size == 1) "item" else "items"} saved on this phone.")
    }
    fun photo(index: Int, uri: Uri) = operation {
        val path = photos.import(uri)
        if (index in review.value.indices) updateDraft(index, review.value[index].copy(photoPath = path))
    }
    fun price(product: Product, variant: ProductVariant, value: String, onSaved: () -> Unit) = operation {
        repository.updatePrice(product.id, variant.id, value); onSaved(); message("Price updated.")
    }
    suspend fun history(id: Long) = repository.history(id)
    fun submitSearch() {
        val text = query.value.trim()
        if (text.isBlank() || rewriting.value) return
        viewModelScope.launch {
            rewriting.value = true
            try {
                val local = repository.search(products.value, text)
                if (local.isEmpty()) repository.logMiss(text)
                if ((local.isEmpty() || local.first().score < .72) && keyConfigured.value) {
                    val phrases = ai.rewrite(text, products.value.map { it.category }.distinct())
                    if (query.value.trim() == text) rewrites.value = query.value to phrases
                } else if (local.isEmpty()) message("No local match. Try another name, or add an alias from Settings → Missed searches.")
            } catch (error: CancellationException) { throw error }
            catch (error: Exception) { message(error.message ?: "Could not improve the search. Local search still works.") }
            finally { rewriting.value = false }
        }
    }
    fun learn(miss: SearchMissEntity, product: Product) = operation {
        repository.learnAlias(miss, product.id); message("Saved “${miss.query}” as another name for ${product.name}.")
    }
    fun export() = operation { shareFile.value = backups.export(); prefs.edit().putLong("last_backup", System.currentTimeMillis()).apply() }
    fun restore(uri: Uri) = operation {
        val count = backups.restore(uri)
        taxLabel.value = prefs.getString("tax_label", "GST not specified")!!
        query.value = ""; searchCategory.value = null; rewrites.value = null; review.value = emptyList(); warnings.value = emptyList(); fullEdit = false; tab.value = 0
        message("Restored $count items, photos and history.")
    }
    fun saveKey(value: String) = operation {
        withContext(Dispatchers.IO) { keys.save(value) }; keyConfigured.value = true; message("OpenAI key stored securely on this phone.")
    }
    fun forgetKey() { keys.clear(); keyConfigured.value = false; message("OpenAI key removed from this phone.") }
    fun tax(value: String) { taxLabel.value = value; prefs.edit().putString("tax_label", value).apply() }
    fun voiceLanguage(value: String) { language.value = value; prefs.edit().putString("language", value).apply() }
    fun setModel(value: String) {
        if (!Regex("[a-zA-Z0-9._-]{1,100}").matches(value.trim())) { message("Enter a valid model ID."); return }
        model.value = value.trim(); prefs.edit().putString("model", value.trim()).apply(); message("Model updated.")
    }
    override fun onCleared() { db.close(); super.onCleared() }
}
