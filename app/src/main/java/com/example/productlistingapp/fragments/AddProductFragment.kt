package com.example.productlistingapp.fragments

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import com.example.productlistingapp.databinding.LayoutAddProductBottomSheetBinding
import com.example.productlistingapp.models.AddProduct
import com.example.productlistingapp.viewmodel.AddProductViewModel
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import java.io.File

class AddProductFragment : BottomSheetDialogFragment() {
    private var _binding: LayoutAddProductBottomSheetBinding? = null
    private val binding get() = _binding!!
    private val addProductViewModel: AddProductViewModel by viewModels {
        ViewModelProvider.AndroidViewModelFactory.getInstance(requireActivity().application)
    }
    private var selectedImageUri: Uri? = null

    companion object {
        private const val IMAGE_PICK_CODE = 1001
        private const val TAG = "AddProductFragment"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = LayoutAddProductBottomSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupClickListeners()
        observeViewModel()
    }

    private fun setupClickListeners() {
        binding.apply {
            btnAddImage.setOnClickListener {
                pickImage()
            }

            btnSubmit.setOnClickListener {
                handleSubmit()
            }

            selectTypeCard.setOnClickListener {
                showProductTypeDialog()
            }
        }
    }

    private fun showProductTypeDialog() {
        ProductTypeDialog().apply {
            setOnProductTypeSelectedListener { selectedType ->
                binding.tvProductType.text = selectedType
            }
        }.show(parentFragmentManager, "ProductTypeDialog")
    }

    private fun pickImage() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI).apply {
            type = "image/*"
        }
        startActivityForResult(intent, IMAGE_PICK_CODE)
    }

    private fun handleSubmit() {
        if (validateInputs()) {
            showLoading(true)

            val productName = createRequestBody(binding.etProductName.text.toString())
            val productType = createRequestBody(binding.tvProductType.text.toString())
            val price = createRequestBody(binding.etPrice.text.toString())
            val tax = createRequestBody(binding.etTax.text.toString())
            val imagePart = getImageMultipart()

            addProductViewModel.addProduct(productName, productType, price, tax, imagePart?.let { listOf(it) })
        }
    }

    private fun showLoading(show: Boolean) {
        binding.apply {
            btnSubmit.visibility = if (show) View.INVISIBLE else View.VISIBLE
            sendingProgress.visibility = if (show) View.VISIBLE else View.GONE
        }
    }

    private fun validateInputs(): Boolean {
        return when {
            TextUtils.isEmpty(binding.etProductName.text) -> {
                showToast("Please enter product name")
                false
            }
            binding.tvProductType.text.toString() == "Select Product Type" -> {
                showToast("Please select a product type")
                false
            }
            TextUtils.isEmpty(binding.etPrice.text) -> {
                showToast("Please enter product price")
                false
            }
            TextUtils.isEmpty(binding.etTax.text) -> {
                showToast("Please enter tax percentage")
                false
            }
            selectedImageUri == null -> {
                showToast("Please select an image")
                false
            }
            else -> true
        }
    }

    private fun getImageMultipart(): MultipartBody.Part? {
        return selectedImageUri?.let { uri ->
            context?.let { ctx ->
                try {
                    val file = File(ctx.cacheDir, "temp_image_${System.currentTimeMillis()}.jpg")
                    ctx.contentResolver.openInputStream(uri)?.use { inputStream ->
                        file.outputStream().use { outputStream ->
                            inputStream.copyTo(outputStream)
                        }
                    }

                    if (!file.exists() || file.length() <= 0) {
                        Log.e(TAG, "File creation failed or is empty")
                        return@let null
                    }

                    Log.d(TAG, "File created: ${file.absolutePath}, Size: ${file.length()}")
                    val requestBody = RequestBody.create(MediaType.parse("image/*"), file)
                    MultipartBody.Part.createFormData("files[]", file.name, requestBody)
                } catch (e: Exception) {
                    Log.e(TAG, "Error creating MultipartBody.Part", e)
                    null
                }
            }
        }
    }

    private fun createRequestBody(value: String): RequestBody {
        return RequestBody.create(MediaType.parse("text/plain"), value)
    }

    private fun observeViewModel() {
        addProductViewModel.addProductResult.observe(viewLifecycleOwner) { response ->
            showLoading(false)
            handleApiResponse(response)
        }

        addProductViewModel.isOffline.observe(viewLifecycleOwner) { isOffline ->
            if (isOffline) {
                showLoading(false)
                showToast("Product saved for later sync")
                dismiss()
            }
        }
    }

    private fun handleApiResponse(response: Response<AddProduct>) {
        if (response.isSuccessful) {
            showToast("Product added successfully")
            (parentFragment as? ProductListingFragment)?.refreshProducts()
            dismiss()
        } else {
            val errorBody = response.errorBody()?.string()
            Log.e(TAG, "API Error: $errorBody")
            showToast("Failed to add product: ${response.message()}")
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == IMAGE_PICK_CODE && resultCode == Activity.RESULT_OK) {
            selectedImageUri = data?.data
            selectedImageUri?.let {
                Log.d(TAG, "Selected Image: $it")
            }
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}