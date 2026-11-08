package com.shopkhata.inventory.data

import androidx.room.withTransaction
import com.shopkhata.inventory.domain.*
import kotlinx.coroutines.flow.map
import org.json.JSONArray

fun ItemWithRelations.product() = Product(item.id, item.name, item.brand, item.category, aliases.map { it.text }, item.photoPath, item.zone, item.locationNote, item.updatedAt,
    variants.sortedBy { it.id }.map { ProductVariant(it.id, it.spec, it.unit, it.pricePaise, it.priceUpdatedAt, it.sourceReferenceId) }, jsonPaths(item.extraPhotoPathsJson), jsonPaths(item.shelfPhotoPathsJson))
class CatalogRepository(val db: CatalogDatabase) {
    private val dao = db.catalog()
    val products = dao.observe().map { list -> list.map { it.product() } }
    val misses = dao.misses()
    suspend fun search(products: List<Product>, query: String): List<SearchHit> {
        val fts = ftsQuery(query)
        val ids = if (fts.isBlank()) emptySet() else dao.match(fts).toSet()
        return CatalogSearch.search(products, query, ids)
    }
    /** A batch is atomic. AI updates merge sizes; a full item edit replaces the size list. */
    suspend fun save(drafts: List<ItemDraft>, replaceVariants: Boolean = false): List<Long> = db.withTransaction {
        require(drafts.isNotEmpty()) { "Nothing to save." }
        drafts.forEach { require(it.error() == null) { it.error() ?: "Invalid item" } }
        drafts.map { draft ->
            val now = System.currentTimeMillis()
            val existing = draft.targetId?.let { requireNotNull(dao.item(it)) { "The item no longer exists. Reopen the editor." } }
            val oldProduct = existing?.product()
            val album = if (replaceVariants || draft.productPhotos().isNotEmpty()) draft.productPhotos() else oldProduct?.productPhotos().orEmpty()
            val shelf = if (replaceVariants || draft.shelfPhotoPaths.isNotEmpty()) draft.shelfPhotoPaths else oldProduct?.shelfPhotoPaths.orEmpty()
            require(album.size <= 6 && shelf.size <= 6) { "Choose up to 6 photos per album." }
            val item = ItemEntity(existing?.item?.id ?: 0, draft.name.trim(), draft.brand.trim(), draft.category.trim(),
                album.firstOrNull(), draft.zone.trim(), draft.locationNote.trim(), existing?.item?.createdAt ?: now, now, JSONArray(album.drop(1)).toString(), JSONArray(shelf).toString())
            val id = if (existing == null) dao.insertItem(item) else item.id.also { dao.updateItem(item) }
            val keptIds = mutableSetOf<Long>()
            draft.variants.forEach { value ->
                val linked = value.sourceReferenceId?.let { dao.referenceVariant(it) }
                require(linked == null || linked.itemId == id) { "This reference variant is already carried. Edit its existing shop item instead." }
                val old = linked ?: existing?.variants?.firstOrNull { normalize(it.spec) == normalize(value.spec) && it.unit == value.unit }
                require(old?.sourceReferenceId == null || value.sourceReferenceId == null || old.sourceReferenceId == value.sourceReferenceId) { "These source variants need distinct specifications." }
                val price = requireNotNull(discountedPaise(value.price, value.discount))
                val variant = VariantEntity(old?.id ?: 0, id, value.spec.trim(), value.unit, price,
                    if (old?.pricePaise == price) old.priceUpdatedAt else now, value.sourceReferenceId ?: old?.sourceReferenceId)
                if (old == null) keptIds += dao.insertVariant(variant)
                else {
                    keptIds += old.id
                    dao.updateVariant(variant)
                    if (old.pricePaise != price) dao.insertHistory(PriceHistoryEntity(variantId = old.id, oldPrice = old.pricePaise, newPrice = price, changedAt = now))
                }
            }
            if (replaceVariants) existing?.variants?.filter { it.id !in keptIds }?.forEach { dao.deleteVariant(it.id) }
            val aliases = draft.aliases.split(',', '\n').map { it.trim() }.filter { it.isNotBlank() }.distinctBy(::normalize)
            dao.clearAliases(id)
            dao.insertAliases((aliases + if (!replaceVariants) existing?.aliases?.map { it.text }.orEmpty() else emptyList()).distinct().map { AliasEntity(id, it) })
            reindex(id)
            id
        }
    }
    suspend fun updatePrice(id: Long, variantId: Long, price: String) = db.withTransaction {
        val item = requireNotNull(dao.item(id))
        val old = item.variants.first { it.id == variantId }
        val amount = requireNotNull(paise(price)) { "Enter a valid price." }
        if (old.pricePaise != amount) {
            val now = System.currentTimeMillis()
            dao.updateVariant(old.copy(pricePaise = amount, priceUpdatedAt = now))
            dao.updateItem(item.item.copy(updatedAt = now))
            dao.insertHistory(PriceHistoryEntity(variantId = variantId, oldPrice = old.pricePaise, newPrice = amount, changedAt = now))
        }
    }
    suspend fun logMiss(query: String): Long = dao.insertMiss(SearchMissEntity(query = query.trim(), searchedAt = System.currentTimeMillis()))
    suspend fun learnAlias(miss: SearchMissEntity, itemId: Long) = db.withTransaction {
        requireNotNull(dao.item(itemId))
        dao.insertAliases(listOf(AliasEntity(itemId, miss.query.trim())))
        dao.resolveMiss(miss.id, itemId)
        reindex(itemId)
    }
    suspend fun history(variantId: Long) = dao.history(variantId)
    private suspend fun reindex(id: Long) {
        val row = requireNotNull(dao.item(id))
        dao.index(ItemFts(id, normalize(row.item.name), row.aliases.joinToString(" ") { normalize(it.text) }, normalize(row.item.brand),
            normalize(row.item.category), row.variants.joinToString(" ") { normalize(it.spec) }))
    }
}

fun jsonPaths(json: String): List<String> = JSONArray(json).let { a -> (0 until a.length()).map { a.getString(it) } }
