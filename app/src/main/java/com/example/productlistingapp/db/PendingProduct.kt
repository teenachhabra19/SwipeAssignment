package com.example.productlistingapp.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pending_products")
data class PendingProduct(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val productName: String,
    val productType: String,
    val price: String,
    val tax: String,
    val imageUris: List<String>?,
    val timestamp: Long = System.currentTimeMillis()
)
