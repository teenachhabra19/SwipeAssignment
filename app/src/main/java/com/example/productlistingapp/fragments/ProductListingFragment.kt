package com.example.productlistingapp.fragments

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Network
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.SearchView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.productlistingapp.R
import com.example.productlistingapp.adapter.ProductAdapter
import com.example.productlistingapp.db.NetworkUtils
import com.example.productlistingapp.viewmodel.AddProductViewModel
import com.example.productlistingapp.viewmodel.ProductViewModel
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton

class ProductListingFragment : Fragment() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var productAdapter: ProductAdapter
    private lateinit var addProductViewModel: AddProductViewModel
    private lateinit var searchView: SearchView
    private lateinit var productViewModel: ProductViewModel
    private lateinit var fab: FloatingActionButton
    private lateinit var shimmerFrameLayout: ShimmerFrameLayout
    private lateinit var contentLayout: ViewGroup
    private lateinit var noInternetLayout: ViewGroup
    private lateinit var noInternetImage: ImageView
    private lateinit var noInternetText: TextView

    private lateinit var connectivityManager: ConnectivityManager
    private lateinit var networkCallback: ConnectivityManager.NetworkCallback

    companion object {
        private const val STORAGE_PERMISSION_CODE = 1001
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_product_listing, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initializeViews(view)
        initializeNetworkCallback()

        if (NetworkUtils.isNetworkAvailable(requireContext())) {
            setupRecyclerView()
            checkPermissions()
            observeData()
            setupSearchView()
            setupFab()
        } else {
            showNoInternetScreen()
        }

        childFragmentManager.setFragmentResultListener("product_added", viewLifecycleOwner) { _, _ ->
            refreshProducts()
        }
    }

    private fun initializeViews(view: View) {
        recyclerView = view.findViewById(R.id.recyclerView)
        searchView = view.findViewById(R.id.searchView)
        fab = view.findViewById(R.id.fab)
        shimmerFrameLayout = view.findViewById(R.id.shimmerLayout)
        contentLayout = view.findViewById(R.id.contentLayout)
        noInternetLayout = view.findViewById(R.id.noInternetLayout)
        noInternetImage = view.findViewById(R.id.noInternetImage)
        noInternetText = view.findViewById(R.id.noInternetText)
        productViewModel = ViewModelProvider(this)[ProductViewModel::class.java]

        connectivityManager =
            requireContext().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    }

    private fun initializeNetworkCallback() {
        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                requireActivity().runOnUiThread {
                    if (noInternetLayout.visibility == View.VISIBLE) {
                        hideNoInternetScreen()
                        refreshProducts()
                    }
                }
            }

            override fun onLost(network: Network) {
                requireActivity().runOnUiThread {
                    showNoInternetScreen()
                }
            }
        }

        connectivityManager.registerDefaultNetworkCallback(networkCallback)
    }

    fun refreshProducts() {
        if (NetworkUtils.isNetworkAvailable(requireContext())) {
            startLoading()
            productViewModel.fetchUsers()
        } else {
            showNoInternetScreen()
        }
    }

    private fun setupRecyclerView() {
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
    }

    private fun checkPermissions() {
        if (!hasStoragePermission()) {
            requestStoragePermission()
        }
    }

    private fun observeData() {
        startLoading()
        productViewModel.fetchUsers()

        productViewModel.product.observe(viewLifecycleOwner) { productList ->
            productAdapter = ProductAdapter(productList)
            recyclerView.adapter = productAdapter
            stopLoading()
        }
    }

    private fun setupSearchView() {
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                productAdapter.filterList(query)
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                productAdapter.filterList(newText)
                return false
            }
        })
    }

    private fun setupFab() {
        fab.setOnClickListener {
            if (NetworkUtils.isNetworkAvailable(requireContext())) {
                AddProductFragment().show(childFragmentManager, "AddProductFragment")
            } else {
                Toast.makeText(requireContext(), "No internet connection", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun startLoading() {
        shimmerFrameLayout.startShimmer()
        shimmerFrameLayout.visibility = View.VISIBLE
        contentLayout.visibility = View.GONE
        noInternetLayout.visibility = View.GONE
    }

    private fun stopLoading() {
        shimmerFrameLayout.stopShimmer()
        shimmerFrameLayout.visibility = View.GONE
        contentLayout.visibility = View.VISIBLE
        noInternetLayout.visibility = View.GONE
    }

    private fun showNoInternetScreen() {
        shimmerFrameLayout.stopShimmer()
        shimmerFrameLayout.visibility = View.GONE
        contentLayout.visibility = View.GONE
        noInternetLayout.visibility = View.VISIBLE
        noInternetImage.setImageResource(R.drawable.nointernet)
        noInternetText.text = getString(R.string.no_internet_message)
    }

    private fun hideNoInternetScreen() {
        noInternetLayout.visibility = View.GONE
        contentLayout.visibility = View.VISIBLE
    }

    private fun hasStoragePermission(): Boolean {
        val readPermission = ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.READ_EXTERNAL_STORAGE
        )
        val writePermission = ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
        return readPermission == PackageManager.PERMISSION_GRANTED &&
                writePermission == PackageManager.PERMISSION_GRANTED
    }

    private fun requestStoragePermission() {
        requestPermissions(
            arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ),
            STORAGE_PERMISSION_CODE
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == STORAGE_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(context, "Permissions granted", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Permissions denied. App may not work properly.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        connectivityManager.unregisterNetworkCallback(networkCallback)
    }
}