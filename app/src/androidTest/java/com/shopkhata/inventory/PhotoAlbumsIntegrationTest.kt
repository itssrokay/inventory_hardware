package com.shopkhata.inventory

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.shopkhata.inventory.ai.OpenAiClient
import com.shopkhata.inventory.backup.BackupManager
import com.shopkhata.inventory.data.*
import com.shopkhata.inventory.domain.*
import com.shopkhata.inventory.security.ApiKeyStore
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.json.JSONArray
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.util.zip.*

class PhotoAlbumsIntegrationTest {
    @Test fun versionTwoCatalogMigratesWithCoverPriceAndSourceLinkIntact() = runBlocking<Unit> {
        val context=ApplicationProvider.getApplicationContext<Context>();val name="albums-migration";context.deleteDatabase(name)
        val schema=JSONObject(InstrumentationRegistry.getInstrumentation().context.assets.open("com.shopkhata.inventory.data.CatalogDatabase/2.json").bufferedReader().use{it.readText()}).getJSONObject("database")
        context.openOrCreateDatabase(name,Context.MODE_PRIVATE,null).apply {
            val entities=schema.getJSONArray("entities")
            for(i in 0 until entities.length()) {
                val entity=entities.getJSONObject(i);val table=entity.getString("tableName")
                execSQL(entity.getString("createSql").replace("\${TABLE_NAME}",table))
                val indices=entity.optJSONArray("indices");if(indices!=null)for(j in 0 until indices.length())execSQL(indices.getJSONObject(j).getString("createSql").replace("\${TABLE_NAME}",table))
            }
            val setup=schema.getJSONArray("setupQueries");for(i in 0 until setup.length())execSQL(setup.getString(i))
            execSQL("INSERT INTO item(id,name,brand,category,photoPath,zone,locationNote,createdAt,updatedAt) VALUES(1,'Socket','Prince','Plumbing','photos/old.jpg','A2','Top',1,2)")
            execSQL("INSERT INTO variant(id,itemId,spec,unit,pricePaise,priceUpdatedAt,sourceReferenceId) VALUES(1,1,'20 mm','piece',1700,2,'source-1')")
            version=2;close()
        }
        val db=Room.databaseBuilder(context,CatalogDatabase::class.java,name).addMigrations(CatalogDatabase.MIGRATION_2_3).build()
        try {val product=db.catalog().all().single().product();assertEquals(listOf("photos/old.jpg"),product.productPhotos());assertTrue(product.shelfPhotoPaths.isEmpty());assertEquals(1700L,product.variants.single().pricePaise);assertEquals("source-1",product.variants.single().sourceReferenceId)}
        finally{db.close();context.deleteDatabase(name)}
    }
    @Test fun albumsSurviveEditBackupRestoreAndCanBeExplicitlyCleared() = runBlocking<Unit> {
        val context=ApplicationProvider.getApplicationContext<Context>();val db=Room.inMemoryDatabaseBuilder(context,CatalogDatabase::class.java).build()
        val store=PhotoStore(context)
        val paths=listOf(Color.RED,Color.BLUE,Color.GREEN).map {color->store.save(Bitmap.createBitmap(20,20,Bitmap.Config.ARGB_8888).apply{eraseColor(color)})}
        try {
            val repo=CatalogRepository(db)
            val id=repo.save(listOf(ItemDraft("Socket","Prince","Plumbing",photoPath=paths[0],extraPhotoPaths=listOf(paths[1]),shelfPhotoPaths=listOf(paths[2]),variants=listOf(VariantDraft("20 mm","piece","17"))))).single()
            val manager=BackupManager(context,db);val archive=manager.export()
            ZipFile(archive).use{zip->val json=JSONObject(zip.getInputStream(zip.getEntry("catalog.json")).bufferedReader().readText());assertEquals(3,json.getInt("version"));assertEquals(1,json.getJSONArray("items").getJSONObject(0).getJSONArray("shelf_photos").length())}
            manager.restore(Uri.fromFile(archive));val restored=db.catalog().all().single().product()
            assertEquals(2,restored.productPhotos().size);assertEquals(1,restored.shelfPhotoPaths.size)
            restored.productPhotos().plus(restored.shelfPhotoPaths).forEach{assertTrue(File(context.filesDir,it).isFile)}
            assertEquals(1700L,restored.variants.single().pricePaise);assertTrue(db.catalog().allHistory().isEmpty())
            repo.save(listOf(restored.draft().withProductPhotos(emptyList()).copy(shelfPhotoPaths=emptyList())),replaceVariants=true)
            assertTrue(db.catalog().all().single().product().productPhotos().isEmpty());assertTrue(db.catalog().all().single().product().shelfPhotoPaths.isEmpty())
            val legacy=File(context.cacheDir,"legacy-photo-album.zip")
            ZipFile(archive).use {source -> ZipOutputStream(legacy.outputStream()).use {zip ->
                val json=JSONObject(source.getInputStream(source.getEntry("catalog.json")).bufferedReader().readText()).put("version",2)
                val item=json.getJSONArray("items").getJSONObject(0);item.remove("extra_photos");item.remove("shelf_photos")
                zip.putNextEntry(ZipEntry("catalog.json"));zip.write(json.toString().toByteArray());zip.closeEntry()
                val cover=item.getString("photo");zip.putNextEntry(ZipEntry(cover));source.getInputStream(source.getEntry(cover)).use{it.copyTo(zip)};zip.closeEntry()
            }}
            manager.restore(Uri.fromFile(legacy));val old=db.catalog().all().single().product()
            assertEquals(1,old.productPhotos().size);assertTrue(old.shelfPhotoPaths.isEmpty());assertEquals(1700L,old.variants.single().pricePaise)
            legacy.delete();archive.delete()
        }finally{db.close();paths.forEach{File(context.filesDir,it).delete()}}
    }
    @Test fun aiPayloadContainsSelectedProductImagesOnlyAndRejectsOutsidePaths() = runBlocking<Unit> {
        val context=ApplicationProvider.getApplicationContext<Context>();val store=PhotoStore(context)
        val paths=(1..3).map{store.save(Bitmap.createBitmap(10,10,Bitmap.Config.ARGB_8888))}
        try {
            val draft=ItemDraft(photoPath=paths[0],extraPhotoPaths=listOf(paths[1]),shelfPhotoPaths=listOf(paths[2]))
            val client=OpenAiClient(context,ApiKeyStore(context))
            val payload=client.imageInput("Socket",draft.productPhotos()) as JSONArray
            val content=payload.getJSONObject(0).getJSONArray("content");assertEquals(3,content.length())
            assertEquals("input_text",content.getJSONObject(0).getString("type"));assertEquals("input_image",content.getJSONObject(2).getString("type"))
            try{client.imageInput("bad",listOf("../outside.jpg"));fail("Outside path accepted")}catch(_:IllegalArgumentException){}
            try{client.imageInput("many",List(7){paths[0]});fail("Too many images accepted")}catch(_:IllegalArgumentException){}
        }finally{paths.forEach{File(context.filesDir,it).delete()}}
    }
}
