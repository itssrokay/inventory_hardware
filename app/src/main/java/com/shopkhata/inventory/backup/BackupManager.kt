package com.shopkhata.inventory.backup

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import com.shopkhata.inventory.ai.objects
import com.shopkhata.inventory.data.*
import com.shopkhata.inventory.domain.units
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/** A portable, versioned archive. No API credentials are included. */
class BackupManager(private val context: Context, private val db: CatalogDatabase) {
    suspend fun export(): File = withContext(Dispatchers.IO) {
        val dao = db.catalog()
        val (rows, histories, misses) = db.withTransaction { Triple(dao.all(), dao.allHistory(), dao.allMisses()) }
        val root = JSONObject().put("version", 2).put("exported_at", System.currentTimeMillis())
            .put("tax_label", context.getSharedPreferences("settings", Context.MODE_PRIVATE).getString("tax_label", "GST not specified"))
            .put("items", JSONArray(rows.map { row ->
                val item = row.item
                JSONObject().put("id", item.id).put("name", item.name).put("brand", item.brand).put("category", item.category)
                    .put("zone", item.zone).put("location_note", item.locationNote).put("created_at", item.createdAt).put("updated_at", item.updatedAt)
                    .put("photo", if (item.photoPath != null) "photos/${item.id}.jpg" else JSONObject.NULL)
                    .put("aliases", JSONArray(row.aliases.map { it.text }))
                    .put("variants", JSONArray(row.variants.map {
                        JSONObject().put("id", it.id).put("spec", it.spec).put("unit", it.unit).put("price_paise", it.pricePaise).put("updated_at", it.priceUpdatedAt).put("source_reference_id", it.sourceReferenceId ?: JSONObject.NULL)
                    }))
            })).put("price_history", JSONArray(histories.map {
                JSONObject().put("variant_id", it.variantId).put("old_price", it.oldPrice).put("new_price", it.newPrice).put("changed_at", it.changedAt)
            })).put("search_misses", JSONArray(misses.map {
                JSONObject().put("query", it.query).put("searched_at", it.searchedAt).put("resolved_item_id", it.resolvedItemId ?: JSONObject.NULL)
            }))
        val directory = File(context.cacheDir, "backups").apply { mkdirs() }
        directory.listFiles()?.filter { it.lastModified() < System.currentTimeMillis() - 86400000 }?.forEach { it.delete() }
        val file = File(directory, "dukaan-${System.currentTimeMillis()}.zip")
        try {
            ZipOutputStream(file.outputStream().buffered()).use { zip ->
                zip.putNextEntry(ZipEntry("catalog.json")); zip.write(root.toString(2).toByteArray()); zip.closeEntry()
                rows.forEach { row -> row.item.photoPath?.let { path ->
                    val photo = File(context.filesDir, path)
                    check(photo.isFile) { "A product photo is missing. Update that photo before backing up." }
                    zip.putNextEntry(ZipEntry("photos/${row.item.id}.jpg")); photo.inputStream().use { it.copyTo(zip) }; zip.closeEntry()
                } }
            }
            file
        } catch (error: Exception) { file.delete(); throw error }
    }
    /** Validate the entire archive before replacing anything; database replacement is one transaction. */
    suspend fun restore(uri: Uri): Int = withContext(Dispatchers.IO) {
        val directory = File(context.filesDir, "photos/restore-${UUID.randomUUID()}").apply { mkdirs() }
        var committed = false
        try {
            var manifest: String? = null
            val seen = mutableSetOf<String>()
            var totalBytes = 0L
            ZipInputStream(requireNotNull(context.contentResolver.openInputStream(uri))).use { zip ->
                while (true) {
                    val entry = zip.nextEntry ?: break
                    require(seen.add(entry.name) && seen.size <= 2001) { "Duplicate or excessive backup entries." }
                    require(entry.name == "catalog.json" || Regex("photos/[1-9][0-9]*\\.jpg").matches(entry.name)) { "Invalid backup file." }
                    val limit = if (entry.name == "catalog.json") 10_000_000 else 2_000_000
                    val out = java.io.ByteArrayOutputStream()
                    val buffer = ByteArray(8192)
                    while (true) {
                        val count = zip.read(buffer)
                        if (count < 0) break
                        totalBytes += count
                        require(out.size() + count <= limit && totalBytes <= 350_000_000L) { "Backup is too large." }
                        out.write(buffer, 0, count)
                    }
                    if (entry.name == "catalog.json") manifest = out.toString("UTF-8")
                    else File(directory, entry.name.substringAfter('/')).writeBytes(out.toByteArray())
                    zip.closeEntry()
                }
            }
            val json = JSONObject(requireNotNull(manifest) { "catalog.json is missing." })
            require(json.getInt("version") in 1..2) { "Unsupported backup version." }
            val itemIds = mutableSetOf<Long>()
            val variantIds = mutableSetOf<Long>()
            val referenceIds = mutableSetOf<String>()
            val rows = json.getJSONArray("items").objects().map { item ->
                val id = item.exactLong("id")
                require(id > 0 && itemIds.add(id) && item.getString("name").isNotBlank()) { "Invalid catalog item." }
                val photo = if (item.isNull("photo")) null else item.getString("photo").let {
                    require(it == "photos/$id.jpg" && File(directory, "$id.jpg").isFile) { "Missing product photo." }
                    "${directory.relativeTo(context.filesDir).path}/$id.jpg"
                }
                val entity = ItemEntity(id, item.getString("name"), item.getString("brand"), item.getString("category"), photo, item.getString("zone"),
                    item.getString("location_note"), item.getLong("created_at"), item.getLong("updated_at"))
                val variants = item.getJSONArray("variants").objects().map { value ->
                    val variantId = value.exactLong("id")
                    val price = value.exactLong("price_paise")
                    require(variantId > 0 && variantIds.add(variantId) && price in 0..10_000_000_000L && value.getString("unit") in units) { "Invalid price or unit in backup." }
                    val referenceId = if (!value.has("source_reference_id") || value.isNull("source_reference_id")) null else value.getString("source_reference_id").also {
                        require(it.length in 1..250 && referenceIds.add(it)) { "Invalid or duplicate reference link." }
                    }
                    VariantEntity(variantId, id, value.getString("spec"), value.getString("unit"), price, value.getLong("updated_at"), referenceId)
                }
                require(variants.isNotEmpty() && variants.map { it.spec to it.unit }.distinct().size == variants.size) { "Invalid variants." }
                val aliases = item.getJSONArray("aliases").let { a -> (0 until a.length()).map { AliasEntity(id, a.getString(it)) } }
                ItemWithRelations(entity, variants, aliases)
            }
            val histories = json.getJSONArray("price_history").objects().map {
                require(it.getLong("variant_id") in variantIds && it.getLong("old_price") >= 0 && it.getLong("new_price") >= 0) { "Invalid price history." }
                PriceHistoryEntity(variantId = it.getLong("variant_id"), oldPrice = it.getLong("old_price"), newPrice = it.getLong("new_price"), changedAt = it.getLong("changed_at"))
            }
            val misses = json.getJSONArray("search_misses").objects().map {
                val resolved = if (it.isNull("resolved_item_id")) null else it.getLong("resolved_item_id")
                require(resolved == null || resolved in itemIds) { "Invalid search history." }
                SearchMissEntity(query = it.getString("query"), searchedAt = it.getLong("searched_at"), resolvedItemId = resolved)
            }
            val dao = db.catalog()
            val oldPhotos = dao.all().mapNotNull { it.item.photoPath }.toSet()
            db.withTransaction {
                dao.clearIndex(); dao.clearItems(); dao.clearMisses()
                rows.forEach { row ->
                    dao.insertItem(row.item)
                    row.variants.forEach { dao.insertVariant(it) }
                    dao.insertAliases(row.aliases)
                    dao.index(ItemFts(row.item.id, com.shopkhata.inventory.domain.normalize(row.item.name), row.aliases.joinToString(" ") { com.shopkhata.inventory.domain.normalize(it.text) },
                        com.shopkhata.inventory.domain.normalize(row.item.brand), com.shopkhata.inventory.domain.normalize(row.item.category), row.variants.joinToString(" ") { com.shopkhata.inventory.domain.normalize(it.spec) }))
                }
                histories.forEach { dao.insertHistory(it) }; misses.forEach { dao.insertMiss(it) }
            }
            committed = true
            // Reclaim only old catalog-owned photos, after the replacement has committed.
            val retainedPhotos = rows.mapNotNull { it.item.photoPath }.toSet()
            (oldPhotos - retainedPhotos).forEach { path ->
                runCatching {
                    val oldPhoto = File(context.filesDir, path)
                    val photoRoot = File(context.filesDir, "photos").canonicalPath + File.separator
                    if (oldPhoto.canonicalPath.startsWith(photoRoot)) oldPhoto.delete()
                }
            }
            val tax = json.optString("tax_label", "GST not specified")
            context.getSharedPreferences("settings", Context.MODE_PRIVATE).edit().putString("tax_label", tax).apply()
            rows.size
        } finally { if (!committed) directory.deleteRecursively() }
    }
}

private fun JSONObject.exactLong(name: String): Long = try { get(name).toString().toBigDecimal().longValueExact() } catch (_: Exception) { throw IllegalArgumentException("Invalid whole number in backup: $name") }
