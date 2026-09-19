package com.debloat.hyperos.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "debloat_apps")
data class DebloatAppEntity(
    @PrimaryKey val packageName: String,
    val name: String,
    val category: String,
    val manufacturer: String = "Xiaomi",
    val isInstalled: Boolean,
    val isRemovedByApp: Boolean = false,
    val isSelected: Boolean = false,
    val isSystemApp: Boolean = true

  )
