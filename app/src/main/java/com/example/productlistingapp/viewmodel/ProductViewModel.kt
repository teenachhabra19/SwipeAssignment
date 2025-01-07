package com.example.productlistingapp.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.productlistingapp.models.Product
import com.example.productlistingapp.repos.ProductRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ProductViewModel : ViewModel() {

    val product = MutableLiveData<List<Product>>() // For fetchUsers

    fun fetchUsers() {
        viewModelScope.launch {
            val response = withContext(Dispatchers.IO) { ProductRepository.getProducts() }
            if (response.isSuccessful) {
                response.body()?.let {
                    product.postValue(it)
                }
            }
        }
    }
}
