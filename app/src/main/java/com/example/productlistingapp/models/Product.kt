package com.example.productlistingapp.models

import com.google.gson.annotations.SerializedName

data class Product(

	@field:SerializedName("image")
	val image: String? = null,

	@field:SerializedName("product_type")
	val productType: String? = null,

	@field:SerializedName("price")
	val price: Any? = null,

	@field:SerializedName("tax")
	val tax: Any? = null,

	@field:SerializedName("product_name")
	val productName: String? = null
)
