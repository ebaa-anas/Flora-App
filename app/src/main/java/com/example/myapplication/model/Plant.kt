package com.example.myapplication.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Plant(
    val id: Long? = null,
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
