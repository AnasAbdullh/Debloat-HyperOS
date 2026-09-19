package com.debloat.hyperos.data

import kotlinx.serialization.Serializable

@Serializable
data class PresetRoot(
    val categories: List<PresetCategory>
)

@Serializable
data class PresetCategory(
    val category: String,
    val apps: List<PresetApp>
)

@Serializable
data class PresetApp(
    val name: String,
    val packageName: String
)
