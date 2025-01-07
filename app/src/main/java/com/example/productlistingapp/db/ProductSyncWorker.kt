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

        pendingProducts.forEach { product ->
            try {
                // Convert stored file paths to MultipartBody.Parts
                val imageFiles = product.imageUris?.mapNotNull { filePath ->
                    try {
                        val file = File(filePath)
                        if (file.exists() && file.length() > 0) {
                            Log.d("ProductSyncWorker", "Processing file: ${file.absolutePath}, Size: ${file.length()}")

                            val requestBody = RequestBody.create(
                                MediaType.parse("image/*"),
                                file
                            )

                            MultipartBody.Part.createFormData(
                                "files[]", // Make sure this matches your API expectation
                                file.name,
                                requestBody
                            )
                        } else {
                            Log.e("ProductSyncWorker", "File doesn't exist or is empty: $filePath")
                            null
                        }
                    } catch (e: Exception) {
                        Log.e("ProductSyncWorker", "Error processing file: $filePath", e)
                        null
                    }
                }

                Log.d("ProductSyncWorker", "Syncing product with ${imageFiles?.size ?: 0} images")

                val response = ProductRepository.addProduct(
                    RequestBody.create(MediaType.parse("text/plain"), product.productName),
                    RequestBody.create(MediaType.parse("text/plain"), product.productType),
                    RequestBody.create(MediaType.parse("text/plain"), product.price),
                    RequestBody.create(MediaType.parse("text/plain"), product.tax),
                    imageFiles
                )

                if (response.isSuccessful) {
                    // Clean up stored images
                    product.imageUris?.forEach { filePath ->
                        try {
                            val file = File(filePath)
                            if (file.exists()) {
                                file.delete()
                                Log.d("ProductSyncWorker", "Deleted file: $filePath")
                            }
                        } catch (e: Exception) {
                            Log.e("ProductSyncWorker", "Error deleting file: $filePath", e)
                        }
                    }
                    database.productDao().deletePendingProduct(product)
                    Log.d("ProductSyncWorker", "Successfully synced product: ${product.productName}")
                } else {
                    Log.e("ProductSyncWorker", "Failed to sync product: ${response.message()}")
                }
            } catch (e: Exception) {
                Log.e("ProductSyncWorker", "Error syncing product: ${product.productName}", e)
            }
        }

        return Result.success()
    }
}
