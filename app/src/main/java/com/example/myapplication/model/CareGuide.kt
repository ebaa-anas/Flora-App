package com.example.myapplication.model

import kotlinx.serialization.Serializable

@Serializable
data class CareGuide(
    val id: Long? = null,
    val plant_id: Long,
    val watering_frequency: String,
    val light_requirement: String,
    val temperature_range: String,
    val humidity_level: String? = null,
    val pro_tip: String? = null
)