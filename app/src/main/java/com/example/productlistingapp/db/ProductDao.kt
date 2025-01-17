package com.example.productlistingapp.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ProductDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPendingProduct(product: PendingProduct)

    @Query("SELECT * FROM pending_products ORDER BY timestamp ASC")
    suspend fun getAllPendingProducts(): List<PendingProduct>

    @Delete
    suspend fun deletePendingProduct(product: PendingProduct)

}