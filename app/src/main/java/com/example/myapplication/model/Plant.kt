package com.example.myapplication.model

data class Plant(
    val id: Int,
    val name: String,
    val category: String, // Indoor, Outdoor, etc.
    val price: Double,
    val description: String,
    val imageUrl: String,
    val rating: Double = 4.5,
    // New Professional Attributes
    val light: String = "Bright Indirect", // Low, Bright
    val isPetSafe: Boolean = true,
    val maintenance: String = "Easy", // Hard to Kill, Diva
    val function: String = "Air Purifying"
)