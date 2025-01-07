package com.example.productlistingapp.db

import android.content.Context
import android.net.Uri
import android.util.Log
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import java.io.File

object ProductUtils {

    fun createRequestBody(value: String): RequestBody {
        return RequestBody.create(MediaType.parse("text/plain"), value)
    }

    // This function can be used to convert Uri to MultipartBody.Part
    fun getImageMultiparts(context: Context, selectedImageUris: List<Uri>?): List<MultipartBody.Part>? {
        return selectedImageUris?.mapNotNull { uri ->
            try {
                val file = when (uri.scheme) {
                    "content" -> {
                        val fileDescriptor = context.contentResolver.openFileDescriptor(uri, "r")
                        fileDescriptor?.use {
                            val tempFile = File(context.cacheDir, "temp_image_${System.currentTimeMillis()}.jpg")
                            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                                tempFile.outputStream().use { outputStream ->
                                    inputStream.copyTo(outputStream)
                                }
                            }
                            tempFile
                        } ?: run {
                            Log.e("ProductUtils", "Unable to resolve file descriptor for URI: $uri")
                            null
                        }
                    }
                    "file" -> File(uri.path)
                    else -> {
                        Log.e("ProductUtils", "Unsupported URI scheme: ${uri.scheme}")
                        null
                    }
                }

                if (file != null && file.exists() && file.length() > 0) {
                    val requestBody = RequestBody.create(MediaType.parse("image/*"), file)
                    MultipartBody.Part.createFormData("files[]", file.name, requestBody)
                } else {
                    Log.e("ProductUtils", "File creation failed or is empty: ${file?.absolutePath}")
                    null
                }
            } catch (e: Exception) {
                Log.e("ProductUtils", "Error creating MultipartBody.Part", e)
                null
            }
        }
    }

}
