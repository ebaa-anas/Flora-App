package com.example.myapplication.model

import kotlinx.serialization.Serializable

@Serializable
data class OrderRequest(
    val user_id: String,
    val total_amount: Double,
    val address: String,
    val phone_number: String,
    val payment_method: String,
    val is_gift: Boolean,
    val gift_note: String,
    val status: String = "Processing"
)

@Serializable
data class OrderItemRequest(
    val order_id: String,
    val plant_id: Long,
    val quantity: Int,
    val pot_type: String
)