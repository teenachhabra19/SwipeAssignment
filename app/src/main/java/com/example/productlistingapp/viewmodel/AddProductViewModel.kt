package com.example.productlistingapp.viewmodel

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.productlistingapp.db.AppDatabase
import com.example.productlistingapp.db.NetworkUtils
import com.example.productlistingapp.db.PendingProduct
import com.example.productlistingapp.db.ProductSyncWorker
import com.example.productlistingapp.models.AddProduct
import com.example.productlistingapp.repos.ProductRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import java.io.File

class AddProductViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val _addProductResult = MutableLiveData<Response<AddProduct>>()
    val addProductResult: LiveData<Response<AddProduct>> = _addProductResult

    private val _isOffline = MutableLiveData<Boolean>()
    val isOffline: LiveData<Boolean> = _isOffline

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val context = application.applicationContext

    fun addProduct(
        productName: RequestBody,
        productType: RequestBody,
        price: RequestBody,
        tax: RequestBody,
        files: List<MultipartBody.Part>?
    ) {
        viewModelScope.launch {
            try {
                _isLoading.postValue(true)

                delay(2000)

                if (NetworkUtils.isNetworkAvailable(getApplication())) {
                    handleOnlineSubmission(productName, productType, price, tax, files)
                } else {
                    handleOfflineSubmission(productName, productType, price, tax, files)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error adding product", e)
                _addProductResult.postValue(Response.error(500, okhttp3.ResponseBody.create(null, "Error: ${e.message}")))
            } finally {
                _isLoading.postValue(false)
            }
        }
    }

    private suspend fun handleOnlineSubmission(
        productName: RequestBody,
        productType: RequestBody,
        price: RequestBody,
        tax: RequestBody,
        files: List<MultipartBody.Part>?
    ) {
        val response = ProductRepository.addProduct(productName, productType, price, tax, files)
        _addProductResult.postValue(response)
    }

    private suspend fun handleOfflineSubmission(
        productName: RequestBody,
        productType: RequestBody,
        price: RequestBody,
        tax: RequestBody,
        files: List<MultipartBody.Part>?
    ) {
        val savedImageUris = saveImagesToInternalStorage(files)
        val pendingProduct = PendingProduct(
            productName = extractRequestBodyContent(productName),
            productType = extractRequestBodyContent(productType),
            price = extractRequestBodyContent(price),
            tax = extractRequestBodyContent(tax),
            imageUris = savedImageUris
        )

        database.productDao().insertPendingProduct(pendingProduct)
        scheduleSync()
        _isOffline.postValue(true)
    }

    private suspend fun saveImagesToInternalStorage(files: List<MultipartBody.Part>?): List<String> {
        val savedUris = mutableListOf<String>()

        files?.forEach { part ->
            try {
                val fileName = "product_image_${System.currentTimeMillis()}_${savedUris.size}.jpg"
                val file = File(context.filesDir, fileName)

                context.openFileOutput(fileName, Context.MODE_PRIVATE).use { outputStream ->
                    val buffer = okio.Buffer()
                    part.body().writeTo(buffer)
                    outputStream.write(buffer.readByteArray())
                }

                savedUris.add(file.absolutePath)
                Log.d(TAG, "Saved image to: ${file.absolutePath}")
            } catch (e: Exception) {
                Log.e(TAG, "Error saving image", e)
            }
        }
        return savedUris
    }

    private fun extractRequestBodyContent(requestBody: RequestBody): String {
        val buffer = okio.Buffer()
        requestBody.writeTo(buffer)
        return buffer.readUtf8()
    }

    private fun scheduleSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncWork = OneTimeWorkRequestBuilder<ProductSyncWorker>()
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(getApplication())
            .enqueueUniqueWork(
                "product_sync",
                ExistingWorkPolicy.REPLACE,
                syncWork
            )
    }

    fun retryPendingProducts() {
        viewModelScope.launch {
            if (NetworkUtils.isNetworkAvailable(getApplication())) {
                scheduleSync()
            }
        }
    }

    companion object {
        private const val TAG = "AddProductViewModel"
    }
}