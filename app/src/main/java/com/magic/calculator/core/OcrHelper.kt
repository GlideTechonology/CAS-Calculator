package com.magic.calculator.core

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Base64
import com.googlecode.tesseract.android.TessBaseAPI
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

object OcrHelper {
    fun recognizeText(context: Context, imageUri: Uri, aiOptIn: Int, onSuccess: (String) -> Unit, onError: (Exception) -> Unit) {
        val bitmap = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = ImageDecoder.createSource(context.contentResolver, imageUri)
                ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                }
            } else {
                @Suppress("DEPRECATION")
                MediaStore.Images.Media.getBitmap(context.contentResolver, imageUri)
            }
        } catch (e: Exception) {
            onError(e)
            return
        }
        recognizeBitmap(context, bitmap, aiOptIn, onSuccess, onError)
    }

    fun recognizeBitmap(context: Context, originalBitmap: Bitmap, aiOptIn: Int, onSuccess: (String) -> Unit, onError: (Exception) -> Unit) {
        val prefs = context.getSharedPreferences("ai_prefs", Context.MODE_PRIVATE)
        val apiKey = prefs.getString("ai_api_key", "") ?: ""
        
        // If AI is disabled or API key is absent, use local OCR directly
        if (aiOptIn != 1 || apiKey.isBlank()) {
            runLocalOcr(context, originalBitmap, onSuccess, onError)
            return
        }

        // If AI Opt-in is 1, attempt Cloud OCR (Gemini / OpenAI Vision)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val base64Image = encodeBitmapToBase64(originalBitmap)
                var apiUrl = prefs.getString("ai_api_url", "https://generativelanguage.googleapis.com/v1beta/openai/chat/completions")
                    ?: "https://generativelanguage.googleapis.com/v1beta/openai/chat/completions"
                if (apiUrl.contains("generativelanguage.googleapis.com") && !apiUrl.contains("/openai/") && apiUrl.endsWith("/chat/completions")) {
                    apiUrl = apiUrl.replace("/v1beta/chat/completions", "/v1beta/openai/chat/completions")
                }
                val modelName = prefs.getString("ai_model_name", "gemini-2.5-flash") ?: "gemini-2.5-flash"

                val jsonBody = JSONObject().apply {
                    put("model", modelName)
                    val messages = JSONArray().apply {
                        val messageObj = JSONObject().apply {
                            put("role", "user")
                            val contentArray = JSONArray().apply {
                                val textObj = JSONObject().apply {
                                    put("type", "text")
                                    put("text", "Bu resimdeki matematiksel formülü veya denklemi oku. Sadece formülü düz metin veya LaTeX formatında yaz (Örneğin: \\int \\frac{x^2 + 1}{x^4 + 1} dx). Başka hiçbir açıklama, yorum veya çözüm yapma.")
                                }
                                val imageObj = JSONObject().apply {
                                    put("type", "image_url")
                                    val urlObj = JSONObject().apply {
                                        put("url", "data:image/jpeg;base64,$base64Image")
                                    }
                                    put("image_url", urlObj)
                                }
                                put(textObj)
                                put(imageObj)
                            }
                            put("content", contentArray)
                        }
                        put(messageObj)
                    }
                    put("messages", messages)
                    put("max_tokens", 200)
                }.toString()

                val url = URL(apiUrl)
                val conn = url.openConnection() as HttpURLConnection
                conn.connectTimeout = 15000
                conn.readTimeout = 45000
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json")
                conn.setRequestProperty("Authorization", "Bearer $apiKey")
                conn.doOutput = true

                conn.outputStream.write(jsonBody.toByteArray(Charsets.UTF_8))

                if (conn.responseCode == 200) {
                    val responseJson = conn.inputStream.bufferedReader().readText()
                    val responseObj = JSONObject(responseJson)
                    val content = responseObj.getJSONArray("choices")
                        .getJSONObject(0)
                        .getJSONObject("message")
                        .getString("content")
                    
                    var cleaned = content.trim()
                    cleaned = cleaned.removePrefix("```latex").removePrefix("```math").removePrefix("```")
                    cleaned = cleaned.removeSuffix("```").trim()
                    
                    withContext(Dispatchers.Main) {
                        onSuccess(cleaned)
                    }
                } else {
                    // API returned error; fallback to local Tesseract OCR
                    runLocalOcr(context, originalBitmap, onSuccess, onError)
                }
            } catch (e: Exception) {
                // On any network or translation exception, fallback to local Tesseract OCR engine
                runLocalOcr(context, originalBitmap, onSuccess, onError)
            }
        }
    }

    private fun runLocalOcr(context: Context, bitmap: Bitmap, onSuccess: (String) -> Unit, onError: (Exception) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            var tess: TessBaseAPI? = null
            try {
                // Ensure software bitmap config for native Leptonica/Tesseract compatibility
                val safeBitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && bitmap.config == Bitmap.Config.HARDWARE) {
                    bitmap.copy(Bitmap.Config.ARGB_8888, false)
                } else {
                    bitmap
                }

                val dataPath = ensureTessData(context)
                tess = TessBaseAPI()
                if (!tess.init(dataPath, "eng")) {
                    withContext(Dispatchers.Main) {
                        onError(IllegalStateException("Tesseract initialization failed for path: $dataPath"))
                    }
                    return@launch
                }

                tess.setImage(safeBitmap)
                val rawText = tess.utF8Text ?: ""
                var cleaned = rawText.replace("\n", " ").trim()
                while (cleaned.contains("  ")) {
                    cleaned = cleaned.replace("  ", " ")
                }

                withContext(Dispatchers.Main) {
                    onSuccess(cleaned)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onError(e)
                }
            } finally {
                try {
                    tess?.recycle()
                } catch (_: Exception) {}
            }
        }
    }

    private fun ensureTessData(context: Context): String {
        val tessBaseDir = File(context.filesDir, "tesseract")
        val tessDataDir = File(tessBaseDir, "tessdata")
        if (!tessDataDir.exists()) {
            tessDataDir.mkdirs()
        }
        val targetFile = File(tessDataDir, "eng.traineddata")

        // Copy asset to internal storage if not already present or file is empty
        if (!targetFile.exists() || targetFile.length() == 0L) {
            context.assets.open("tessdata/eng.traineddata").use { input ->
                FileOutputStream(targetFile).use { output ->
                    input.copyTo(output)
                }
            }
        }
        return tessBaseDir.absolutePath
    }

    private fun encodeBitmapToBase64(bitmap: Bitmap): String {
        // Downscale very large images (for memory efficiency and fast transmission)
        val maxWidth = 1024f
        val maxHeight = 1024f
        val scale = Math.min(maxWidth / bitmap.width, maxHeight / bitmap.height)
        
        val scaledBitmap = if (scale < 1f) {
            Bitmap.createScaledBitmap(bitmap, (bitmap.width * scale).toInt(), (bitmap.height * scale).toInt(), true)
        } else {
            bitmap
        }

        val outputStream = ByteArrayOutputStream()
        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
        return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
    }
}
