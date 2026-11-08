package com.shopkhata.inventory.data

import android.content.Context
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "item")
data class ItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String, val brand: String = "", val category: String = "",
    val photoPath: String? = null, val zone: String = "", val locationNote: String = "",
    val createdAt: Long, val updatedAt: Long,
    @ColumnInfo(defaultValue = "'[]'") val extraPhotoPathsJson: String = "[]",
    @ColumnInfo(defaultValue = "'[]'") val shelfPhotoPathsJson: String = "[]",
)
@Entity(tableName = "variant", foreignKeys = [ForeignKey(entity = ItemEntity::class, parentColumns = ["id"], childColumns = ["itemId"], onDelete = ForeignKey.CASCADE)], indices = [Index("itemId"), Index(value = ["sourceReferenceId"], unique = true)])
data class VariantEntity(@PrimaryKey(autoGenerate = true) val id: Long = 0, val itemId: Long, val spec: String, val unit: String, val pricePaise: Long, val priceUpdatedAt: Long, val sourceReferenceId: String? = null)
@Entity(tableName = "alias", primaryKeys = ["itemId", "text"], foreignKeys = [ForeignKey(entity = ItemEntity::class, parentColumns = ["id"], childColumns = ["itemId"], onDelete = ForeignKey.CASCADE)], indices = [Index("itemId")])
data class AliasEntity(val itemId: Long, val text: String)
@Entity(tableName = "price_history", foreignKeys = [ForeignKey(entity = VariantEntity::class, parentColumns = ["id"], childColumns = ["variantId"], onDelete = ForeignKey.CASCADE)], indices = [Index("variantId")])
data class PriceHistoryEntity(@PrimaryKey(autoGenerate = true) val id: Long = 0, val variantId: Long, val oldPrice: Long, val newPrice: Long, val changedAt: Long)
@Entity(tableName = "search_miss")
data class SearchMissEntity(@PrimaryKey(autoGenerate = true) val id: Long = 0, val query: String, val searchedAt: Long, val resolvedItemId: Long? = null)
@Fts4(tokenizer = FtsOptions.TOKENIZER_UNICODE61)
@Entity(tableName = "item_fts")
data class ItemFts(@PrimaryKey @ColumnInfo(name = "rowid") val rowId: Long, val name: String, val aliases: String, val brand: String, val category: String, val specs: String)
data class ItemWithRelations(@Embedded val item: ItemEntity,
    @Relation(parentColumn = "id", entityColumn = "itemId") val variants: List<VariantEntity>,
    @Relation(parentColumn = "id", entityColumn = "itemId") val aliases: List<AliasEntity>)

@Dao
interface CatalogDao {
    @Transaction @Query("SELECT * FROM item ORDER BY updatedAt DESC") fun observe(): Flow<List<ItemWithRelations>>
    @Transaction @Query("SELECT * FROM item") suspend fun all(): List<ItemWithRelations>
    @Transaction @Query("SELECT * FROM item WHERE id = :id") suspend fun item(id: Long): ItemWithRelations?
    @Insert suspend fun insertItem(item: ItemEntity): Long
    @Update suspend fun updateItem(item: ItemEntity)
    @Insert suspend fun insertVariant(variant: VariantEntity): Long
    @Update suspend fun updateVariant(variant: VariantEntity)
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertAliases(aliases: List<AliasEntity>)
    @Query("DELETE FROM alias WHERE itemId = :id") suspend fun clearAliases(id: Long)
    @Query("DELETE FROM variant WHERE id = :id") suspend fun deleteVariant(id: Long)
    @Insert suspend fun insertHistory(history: PriceHistoryEntity)
    @Query("SELECT * FROM price_history WHERE variantId = :id ORDER BY changedAt DESC") suspend fun history(id: Long): List<PriceHistoryEntity>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun index(row: ItemFts)
    @Query("SELECT rowid FROM item_fts WHERE item_fts MATCH :query") suspend fun match(query: String): List<Long>
    @Insert suspend fun insertMiss(miss: SearchMissEntity): Long
    @Query("SELECT * FROM search_miss WHERE resolvedItemId IS NULL ORDER BY searchedAt DESC LIMIT 100") fun misses(): Flow<List<SearchMissEntity>>
    @Query("UPDATE search_miss SET resolvedItemId = :itemId WHERE id = :id") suspend fun resolveMiss(id: Long, itemId: Long)
    @Query("DELETE FROM item_fts") suspend fun clearIndex()
    @Query("DELETE FROM item") suspend fun clearItems()
    @Query("DELETE FROM search_miss") suspend fun clearMisses()
    @Query("SELECT * FROM price_history") suspend fun allHistory(): List<PriceHistoryEntity>
    @Query("SELECT * FROM variant WHERE sourceReferenceId = :referenceId LIMIT 1") suspend fun referenceVariant(referenceId: String): VariantEntity?
    @Query("SELECT * FROM search_miss") suspend fun allMisses(): List<SearchMissEntity>
}
@Database(entities = [ItemEntity::class, VariantEntity::class, AliasEntity::class, PriceHistoryEntity::class, SearchMissEntity::class, ItemFts::class], version = 3, exportSchema = true)
abstract class CatalogDatabase : RoomDatabase() {
    abstract fun catalog(): CatalogDao
    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE variant ADD COLUMN sourceReferenceId TEXT")
                db.execSQL("CREATE UNIQUE INDEX index_variant_sourceReferenceId ON variant(sourceReferenceId)")
            }
        }
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE item ADD COLUMN extraPhotoPathsJson TEXT NOT NULL DEFAULT '[]'")
                db.execSQL("ALTER TABLE item ADD COLUMN shelfPhotoPathsJson TEXT NOT NULL DEFAULT '[]'")
            }
        }
        fun open(context: Context) = Room.databaseBuilder(context, CatalogDatabase::class.java, "catalog.db").addMigrations(MIGRATION_1_2, MIGRATION_2_3).build()
    }
}
