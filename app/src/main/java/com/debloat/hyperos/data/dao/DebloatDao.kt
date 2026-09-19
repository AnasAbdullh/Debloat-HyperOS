package com.debloat.hyperos.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.debloat.hyperos.data.entity.DebloatAppEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DebloatDao {

    // ---- Reads ----

    @Query("SELECT * FROM debloat_apps ORDER BY category, name")
    fun observeAll(): Flow<List<DebloatAppEntity>>

    @Query("SELECT * FROM debloat_apps ORDER BY category, name")
    suspend fun getAllOnce(): List<DebloatAppEntity>

    @Query("SELECT * FROM debloat_apps WHERE category = :category ORDER BY name")
    fun observeByCategory(category: String): Flow<List<DebloatAppEntity>>

    @Query("SELECT DISTINCT category FROM debloat_apps ORDER BY category")
    fun observeCategories(): Flow<List<String>>

    @Query("SELECT * FROM debloat_apps WHERE isSelected = 1")
    suspend fun getSelected(): List<DebloatAppEntity>

    @Query("SELECT COUNT(*) FROM debloat_apps")
    suspend fun count(): Int

    // ---- Seeding & Upsert ----

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(apps: List<DebloatAppEntity>)

    // ---- Deletion ----

    @Delete
    suspend fun deleteAll(apps: List<DebloatAppEntity>)

    // ---- Selection ----

    @Query("UPDATE debloat_apps SET isSelected = :selected WHERE packageName = :packageName")
    suspend fun setSelected(packageName: String, selected: Boolean)

    @Query("UPDATE debloat_apps SET isSelected = :selected WHERE category = :category")
    suspend fun setSelectedForCategory(category: String, selected: Boolean)

    @Query("UPDATE debloat_apps SET isSelected = 0")
    suspend fun clearAllSelection()

    // ---- State sync after Shizuku operations ----

    @Query(
        "UPDATE debloat_apps SET isInstalled = :isInstalled, isRemovedByApp = :isRemovedByApp, " +
                "isSelected = 0 WHERE packageName = :packageName"
    )
    suspend fun updateInstallState(packageName: String, isInstalled: Boolean, isRemovedByApp: Boolean)

    @Update
    suspend fun update(app: DebloatAppEntity)

    @Query(
        "UPDATE debloat_apps SET isInstalled = :isInstalled, isRemovedByApp = :isRemovedByApp WHERE packageName = :packageName"
    )
    suspend fun syncInstallState(packageName: String, isInstalled: Boolean, isRemovedByApp: Boolean)
}