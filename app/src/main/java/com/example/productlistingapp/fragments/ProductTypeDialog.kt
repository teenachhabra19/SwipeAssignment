package com.example.productlistingapp.fragments

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup.LayoutParams
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.ScrollView
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import com.example.productlistingapp.models.ProductCategories
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class ProductTypeDialog : DialogFragment() {
    private var onProductTypeSelected: ((String) -> Unit)? = null

    fun setOnProductTypeSelectedListener(listener: (String) -> Unit) {
        onProductTypeSelected = listener
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val productCategories = ProductCategories()

        // Create ScrollView with white background and fixed height
        val scrollView = ScrollView(requireContext()).apply {
            setPadding(0, 16, 0, 16)
            setBackgroundColor(ContextCompat.getColor(requireContext(), android.R.color.white))
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, resources.displayMetrics.heightPixels * 2 / 3)
        }

        // Create RadioGroup dynamically
        val radioGroup = RadioGroup(requireContext()).apply {
            orientation = RadioGroup.VERTICAL
            setPadding(32, 0, 32, 0)
        }

        // Add radio buttons for each product type
        productCategories.productTypes.forEach { productType ->
            val radioButton = RadioButton(requireContext()).apply {
                text = productType
                textSize = 16f
                setPadding(24, 12, 24, 12) // Slightly reduced vertical padding
                setTextColor(ContextCompat.getColor(requireContext(), android.R.color.black))
                minHeight = 0 // Remove minimum height constraint
            }
            radioGroup.addView(radioButton)
        }

        // Add RadioGroup to ScrollView
        scrollView.addView(radioGroup)

        return MaterialAlertDialogBuilder(requireContext(), com.google.android.material.R.style.ThemeOverlay_Material3_Light)
            .setTitle("Select Product Type")
            .setView(scrollView)
            .setPositiveButton("SELECT") { _, _ ->
                val selectedId = radioGroup.checkedRadioButtonId
                if (selectedId != -1) {
                    val selectedRadioButton = radioGroup.findViewById<RadioButton>(selectedId)
                    onProductTypeSelected?.invoke(selectedRadioButton.text.toString())
                }
            }
            .setNegativeButton("CANCEL", null)
            .create()
            .apply {
                // Set dialog background to white
                window?.setBackgroundDrawableResource(android.R.color.white)

                // Set the dialog size when it's shown
                setOnShowListener {
                    val width = resources.displayMetrics.widthPixels * 9 / 10 // 90% of screen width
                    val height = resources.displayMetrics.heightPixels * 2 / 3 // About 67% of screen height
                    window?.setLayout(width, height)
                }
            }
    }
}