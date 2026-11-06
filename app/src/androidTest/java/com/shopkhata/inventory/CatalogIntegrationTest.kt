package com.shopkhata.inventory

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.shopkhata.inventory.backup.BackupManager
import com.shopkhata.inventory.data.*
import com.shopkhata.inventory.domain.*
import com.shopkhata.inventory.security.ApiKeyStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@RunWith(AndroidJUnit4::class)
class CatalogIntegrationTest {
    private lateinit var context: Context
    private lateinit var db: CatalogDatabase
    private lateinit var repo: CatalogRepository
    @Before fun setup() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, CatalogDatabase::class.java).build()
        repo = CatalogRepository(db)
    }
    @After fun cleanup() { db.close() }
    private fun draft() = ItemDraft("Padlock", "Godrej", "Locks", "tala, ताला", variants = listOf(VariantDraft("50 mm", "piece", "450"), VariantDraft("65 mm", "piece", "600")))
    @Test fun hindiFtsAndPriceHistorySurviveEdits() = runBlocking {
        val id = repo.save(listOf(draft())).single()
        val items = repo.products.first()
        assertEquals(listOf(id), db.catalog().match(ftsQuery("ताला")))
        assertEquals(45000L, repo.search(items, "Godrej tala 50mm").single().variants.single().pricePaise)
        val variant = items.single().variants.first()
        repo.updatePrice(id, variant.id, "475.50")
        assertEquals(45000L, repo.history(variant.id).single().oldPrice)
        assertEquals(47550L, repo.history(variant.id).single().newPrice)
        repo.updatePrice(id, variant.id, "475.50")
        assertEquals(1, repo.history(variant.id).size)
    }
    @Test fun batchValidationIsAtomic() = runBlocking {
        try { repo.save(listOf(draft(), ItemDraft("Bad", variants = listOf(VariantDraft(price = "-1"))))) } catch (_: IllegalArgumentException) { }
        assertTrue(db.catalog().all().isEmpty())
    }
    @Test fun duplicateUpdateMergesSizesAndKeepsHistory() = runBlocking {
        val id = repo.save(listOf(draft())).single()
        repo.save(listOf(draft().copy(targetId = id, variants = listOf(VariantDraft("50 mm", "piece", "499")))))
        val item = db.catalog().all().single()
        assertEquals(2, item.variants.size)
        assertEquals(1, db.catalog().allHistory().size)
    }
    @Test fun learningAnAliasUpdatesFtsAndResolvesMiss() = runBlocking {
        val id = repo.save(listOf(draft())).single()
        val missId = repo.logMiss("chota tala")
        val miss = db.catalog().allMisses().single()
        assertEquals(missId, miss.id)
        repo.learnAlias(miss, id)
        assertEquals(listOf(id), db.catalog().match(ftsQuery("chota")))
        assertTrue(repo.misses.first().isEmpty())
    }
    @Test fun backupRoundTripRestoresPhotosPricesHistoryAndAliases() = runBlocking {
        val photo = File(context.filesDir, "photos/test.jpg").apply { parentFile!!.mkdirs(); writeBytes(byteArrayOf(1,2,3,4)) }
        val id = repo.save(listOf(draft().copy(photoPath = "photos/test.jpg"))).single()
        val variantId = db.catalog().all().single().variants.first().id
        repo.updatePrice(id, variantId, "460")
        repo.logMiss("missing")
        val manager = BackupManager(context, db)
        val archive = manager.export()
        repo.save(listOf(ItemDraft("Extra", variants = listOf(VariantDraft(price = "10")))))
        assertEquals(1, manager.restore(android.net.Uri.fromFile(archive)))
        val restored = db.catalog().all().single()
        assertEquals("Padlock", restored.item.name)
        assertEquals(2, restored.variants.size)
        assertEquals(1, db.catalog().allHistory().size)
        assertEquals(1, db.catalog().allMisses().size)
        assertArrayEquals(byteArrayOf(1,2,3,4), File(context.filesDir, restored.item.photoPath!!).readBytes())
        assertEquals(listOf(id), db.catalog().match(ftsQuery("ताला")))
        photo.delete(); archive.delete(); Unit
    }
    @Test fun maliciousBackupNeverReplacesCatalogOrEscapesPhotoDirectory() = runBlocking {
        repo.save(listOf(draft()))
        val archive = File(context.cacheDir, "invalid.zip")
        ZipOutputStream(archive.outputStream()).use { it.putNextEntry(ZipEntry("../escaped")); it.write("oops".toByteArray()); it.closeEntry() }
        try { BackupManager(context, db).restore(android.net.Uri.fromFile(archive)); fail("Invalid backup accepted") } catch (_: Exception) { }
        assertEquals(1, db.catalog().all().size)
        assertFalse(File(context.filesDir, "escaped").exists())
        archive.delete(); Unit
    }
    @Test fun fractionalPaiseInBackupAreRejectedAtomically() = runBlocking {
        repo.save(listOf(draft()))
        val manager = BackupManager(context, db)
        val source = manager.export()
        val json = java.util.zip.ZipFile(source).use { zip ->
            org.json.JSONObject(zip.getInputStream(zip.getEntry("catalog.json")).bufferedReader().use { it.readText() })
        }
        json.getJSONArray("items").getJSONObject(0).getJSONArray("variants").getJSONObject(0).put("price_paise", 123.45)
        val invalid = File(context.cacheDir, "fractional.zip")
        ZipOutputStream(invalid.outputStream()).use { it.putNextEntry(ZipEntry("catalog.json")); it.write(json.toString().toByteArray()); it.closeEntry() }
        try { manager.restore(android.net.Uri.fromFile(invalid)); fail("Fractional paise were silently truncated") } catch (_: IllegalArgumentException) { }
        assertEquals(45000L, db.catalog().all().single().variants.first().pricePaise)
        source.delete(); invalid.delete(); Unit
    }
    @Test fun discountSavesNetPriceAndDoesNotCompoundOnReopen() = runBlocking {
        val id=repo.save(listOf(ItemDraft("Socket",brand="Prince",variants=listOf(VariantDraft("20 mm","piece","100","25"))))).single()
        val product=db.catalog().item(id)!!.product()
        assertEquals(7500L,product.variants.single().pricePaise)
        val reopened=product.draft()
        assertEquals("75",reopened.variants.single().price)
        assertEquals("",reopened.variants.single().discount)
        repo.save(listOf(reopened),true)
        assertEquals(7500L,db.catalog().item(id)!!.variants.single().pricePaise)
        assertTrue(db.catalog().allHistory().isEmpty())
        repo.save(listOf(reopened.copy(variants=listOf(VariantDraft("20 mm","piece","100","50")))),true)
        val history=db.catalog().allHistory().single()
        assertEquals(7500L,history.oldPrice);assertEquals(5000L,history.newPrice)
    }
    @Test fun apiKeyIsEncryptedAtRest() {
        // Synthetic key only; no live credentials or network access in tests.
        val keys = ApiKeyStore(context)
        keys.save("sk-test-synthetic-not-a-live-key")
        assertEquals("sk-test-synthetic-not-a-live-key", keys.read())
        assertFalse(File(context.applicationInfo.dataDir, "shared_prefs/secure_api.xml").readText().contains("sk-test"))
        keys.clear()
    }
}
