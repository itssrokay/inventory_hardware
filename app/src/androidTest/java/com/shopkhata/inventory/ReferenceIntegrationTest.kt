package com.shopkhata.inventory

import android.content.Context
import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.shopkhata.inventory.backup.BackupManager
import com.shopkhata.inventory.data.*
import com.shopkhata.inventory.domain.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.json.JSONObject

class ReferenceIntegrationTest {
    @Test fun displayPreferenceSurvivesBackupWithoutChangingPrices() = runBlocking {
        val context=ApplicationProvider.getApplicationContext<Context>()
        val prefs=context.getSharedPreferences("settings",Context.MODE_PRIVATE)
        val old=prefs.getString("ui_language","en")!!
        val db=Room.inMemoryDatabaseBuilder(context,CatalogDatabase::class.java).build()
        try {
            CatalogRepository(db).save(listOf(ItemDraft(name="Custom name",variants=listOf(VariantDraft(spec="15 mm",price="12.50")))))
            prefs.edit().putString("ui_language","hinglish").commit()
            val manager=BackupManager(context,db);val file=manager.export()
            prefs.edit().putString("ui_language","en").commit();manager.restore(Uri.fromFile(file))
            assertEquals("hinglish",prefs.getString("ui_language",null))
            assertEquals("Custom name",db.catalog().all().single().item.name)
            assertEquals(1250L,db.catalog().all().single().variants.single().pricePaise)
        } finally {prefs.edit().putString("ui_language",old).commit();db.close()}
    }
    @Test fun oldCatalogMigratesWithoutChangingPricesOrAddingReferenceStock() = runBlocking {
        val context=ApplicationProvider.getApplicationContext<Context>()
        val name="reference-migration"
        context.deleteDatabase(name)
        val schema=JSONObject(InstrumentationRegistry.getInstrumentation().context.assets.open("com.shopkhata.inventory.data.CatalogDatabase/1.json").bufferedReader().use { it.readText() }).getJSONObject("database")
        context.openOrCreateDatabase(name,Context.MODE_PRIVATE,null).apply {
            val entities=schema.getJSONArray("entities")
            for(i in 0 until entities.length()) {
                val entity=entities.getJSONObject(i);val table=entity.getString("tableName")
                execSQL(entity.getString("createSql").replace("\${TABLE_NAME}",table))
                val indices=entity.optJSONArray("indices")
                if(indices!=null) for(n in 0 until indices.length()) execSQL(indices.getJSONObject(n).getString("createSql").replace("\${TABLE_NAME}",table))
            }
            val setup=schema.getJSONArray("setupQueries");for(i in 0 until setup.length())execSQL(setup.getString(i))
            execSQL("INSERT INTO item(id,name,brand,category,photoPath,zone,locationNote,createdAt,updatedAt) VALUES(1,'Existing','Prince','Plumbing',NULL,'A2','Shelf',1,2)")
            execSQL("INSERT INTO variant(id,itemId,spec,unit,pricePaise,priceUpdatedAt) VALUES(1,1,'15 mm','piece',12345,2)")
            version=1
            close()
        }
        val db=Room.databaseBuilder(context,CatalogDatabase::class.java,name).addMigrations(CatalogDatabase.MIGRATION_1_2,CatalogDatabase.MIGRATION_2_3).build()
        try { val row=db.catalog().all().single();assertEquals("A2",row.item.zone);assertEquals(12345L,row.variants.single().pricePaise);assertNull(row.variants.single().sourceReferenceId) }
        finally { db.close();context.deleteDatabase(name) }
    }
    @Test fun referenceLoadIsSeparateAndPartialActivationBacksUpAndCannotDuplicate() = runBlocking {
        val context=ApplicationProvider.getApplicationContext<Context>()
        val db=Room.inMemoryDatabaseBuilder(context,CatalogDatabase::class.java).build()
        try {
            val catalog=ReferenceStore.load(context);val repo=CatalogRepository(db)
            assertTrue(catalog.products.size>600)
            assertTrue(repo.products.first().isEmpty())
            val group=catalog.products.first { it.page==8&&it.name.startsWith("COUPLER") }
            val selected=group.variants.first { it.code=="SIM107608" }
            assertNotNull(group.imageAsset)
            val sourcePhoto=PhotoStore(context).reference(requireNotNull(group.imageAsset))
            assertNotNull(android.graphics.BitmapFactory.decodeFile(java.io.File(context.filesDir,sourcePhoto).path))
            val draft=referenceDrafts(catalog,setOf(selected.id),emptyList()).single()
            assertNotNull(draft.error())
            val confirmed=draft.copy(photoPath=sourcePhoto,variants=draft.variants.map { it.copy(unit="piece",price="9.50") })
            repo.save(listOf(confirmed))
            val shop=repo.products.first();assertEquals(1,shop.size);assertEquals(1,shop.single().variants.size)
            assertEquals(950L,shop.single().variants.single().pricePaise)
            assertTrue(referenceDrafts(catalog,setOf(selected.id),shop).isEmpty())
            try {repo.save(listOf(confirmed));fail("Repeated activation accepted")} catch (_:IllegalArgumentException) { }
            assertEquals(1,db.catalog().all().size)
            val manager=BackupManager(context,db);val backup=manager.export()
            db.catalog().clearItems();manager.restore(Uri.fromFile(backup))
            val restored=repo.products.first().single();assertEquals(selected.id,restored.variants.single().sourceReferenceId)
            assertNotNull(restored.photoPath)
            assertNotNull(android.graphics.BitmapFactory.decodeFile(java.io.File(context.filesDir,restored.photoPath!!).path))
            assertEquals(950L,restored.variants.single().pricePaise)
            assertEquals("12.00",catalog.variants.getValue(selected.id).prices.single().second)
        } finally {db.close()}
    }
}
