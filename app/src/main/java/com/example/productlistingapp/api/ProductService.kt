package com.example.productlistingapp.api

import com.example.productlistingapp.models.AddProduct
import com.example.productlistingapp.models.Product
import okhttp3.MultipartBody
import okhttp3.RequestBody

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Query

interface ProductService {
    @GET("get")
    suspend fun getProducts():Response<List<Product>>
    @Multipart
    @POST("add")
    suspend fun addProduct(
        @Part("product_name") productName: RequestBody,
        @Part("product_type") productType: RequestBody,
        @Part("price") price: RequestBody,
        @Part("tax") tax: RequestBody,
        @Part files: List<MultipartBody.Part>?
    ): Response<AddProduct>


}