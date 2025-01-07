package com.example.productlistingapp.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.productlistingapp.R
import com.example.productlistingapp.models.Product
import com.squareup.picasso.Picasso

class ProductAdapter(private var data: List<Product>) : RecyclerView.Adapter<ProductAdapter.UserViewHolder>() {

    // This method is used to update the product list based on the search query
    fun filterList(query: String?) {
        if (query.isNullOrEmpty()) {
            // If query is empty, reset the list to show all products
            data = originalData // 'originalData' stores the complete list of products
        } else {
            data = originalData.filter {
                it.productName!!.contains(query, ignoreCase = true)
            }
        }
        notifyDataSetChanged()
    }



    private val originalData = data // Store the original list

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_product, parent, false)
        return UserViewHolder(view)
    }

    override fun getItemCount(): Int = data.size

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        holder.bind(data[position])
    }

    inner class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(item: Product) = with(itemView) {
            val productNameTv: TextView = itemView.findViewById(R.id.productTv)
            val productImageView: ImageView = itemView.findViewById(R.id.productImgView)
            productNameTv.text = item.productName
            if (item.image.isNullOrEmpty()) {
                Picasso.get().load(R.drawable.swipe_logo_light).into(productImageView)
            } else {
                Picasso.get().load(item.image).error(R.drawable.ic_launcher_background).into(productImageView)
            }
        }
    }
}
