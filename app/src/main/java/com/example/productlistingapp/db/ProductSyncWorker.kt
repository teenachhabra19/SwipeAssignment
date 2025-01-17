package com.example.productlistingapp.db

import android.content.ContentValues.TAG
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.productlistingapp.repos.ProductRepository
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import java.io.File

class ProductSyncWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val database = AppDatabase.getDatabase(applicationContext)
        val pendingProducts = database.productDao().getAllPendingProducts()
        var hasErrors = false

        pendingProducts.forEach { product ->
            try {
                val imageFiles = product.imageUris?.mapNotNull { filePath ->
                    try {
                        val file = File(filePath)
                        if (file.exists() && file.length() > 0) {
                            RequestBody.create(
                                MediaType.parse("image/*"),
                                file
                            ).let { requestBody ->
                                MultipartBody.Part.createFormData(
                                    "files[]",
                                    file.name,
                                    requestBody
                                )
                            }
                        } else {
                            null
                        }
                    } catch (e: Exception) {
                        Log.e("ProductSyncWorker", "Error processing file: $filePath", e)
                        null
                    }
                }

                val response = ProductRepository.addProduct(
                    RequestBody.create(MediaType.parse("text/plain"), product.productName),
                    RequestBody.create(MediaType.parse("text/plain"), product.productType),
                    RequestBody.create(MediaType.parse("text/plain"), product.price),
                    RequestBody.create(MediaType.parse("text/plain"), product.tax),
                    imageFiles
                )

                if (response.isSuccessful) {
                    // Clean up stored images and delete pending product
                    product.imageUris?.forEach { filePath ->
                        try {
                            File(filePath).takeIf { it.exists() }?.delete()
                        } catch (e: Exception) {
                            Log.e("ProductSyncWorker", "Error deleting file: $filePath", e)
                        }
                    }
                    database.productDao().deletePendingProduct(product)
                } else {
                    hasErrors = true
                    Log.e("ProductSyncWorker", "Failed to sync product: ${response.message()}")
                }
            } catch (e: Exception) {
                hasErrors = true
                Log.e("ProductSyncWorker", "Error syncing product: ${product.productName}", e)
            }
        }

        return if (hasErrors) Result.failure() else Result.success()
    }
}
//done