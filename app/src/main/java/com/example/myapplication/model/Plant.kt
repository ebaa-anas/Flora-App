package com.example.myapplication.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Plant(
    val id: Int? = null,
    val name: String,
    val category: String,
    val price: Double,
    val description: String,
    val isPetSafe: Boolean = true,
    val light: String = "All",
    @SerialName("image_url") // Matches SQL column name
    val imageUrl: String,
    val rating: Double = 5.0
)

@Serializable
data class CareGuide(
    val id: Int? = null,
    @SerialName("plant_id")
    val plantId: Int,
    @SerialName("watering_frequency")
    val watering: String,
    @SerialName("light_requirement")
    val light: String,
    @SerialName("temperature_range")
    val temp: String
)