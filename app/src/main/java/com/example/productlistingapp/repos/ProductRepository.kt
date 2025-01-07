package com.example.productlistingapp.repos

import com.example.productlistingapp.api.Client
import com.example.productlistingapp.models.AddProduct
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response

object ProductRepository {
suspend fun getProducts()=Client.api.getProducts()
    // Add a product
    suspend fun addProduct(
        productName: RequestBody,
        productType: RequestBody,
        price: RequestBody,
        tax: RequestBody,
        files: List<MultipartBody.Part>?
    ): Response<AddProduct> {
        return Client.api.addProduct(
            productName = productName,
            productType = productType,
            price = price,
            tax = tax,
            files=files
        )
    }
}