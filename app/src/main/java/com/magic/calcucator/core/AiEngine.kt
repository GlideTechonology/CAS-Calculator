package com.magic.calcucator.core

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.text.InputType
import android.webkit.JavascriptInterface
import android.widget.EditText
import android.widget.LinearLayout
import com.magic.calcucator.Translator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

fun encodeForAskAI(equation: String): String {
    val b64 = android.util.Base64.encodeToString(equation.toByteArray(Charsets.UTF_8), android.util.Base64.NO_WRAP)
    return "B64:$b64"
}

class WebAppInterface(
    private val context: Context,
    private val onRequestSettings: () -> Unit,
    private val onAiResult: (String) -> Unit
) {
    @JavascriptInterface
    fun askAI(equation: String) {
        val decodedEquation = when {
            equation.startsWith("B64:") -> {
                try {
                    String(android.util.Base64.decode(equation.substring(4), android.util.Base64.NO_WRAP), Charsets.UTF_8)
                } catch (_: Exception) {
                    equation
                }
            }
            equation.startsWith("\"") && equation.endsWith("\"") -> {
                try {
                    org.json.JSONTokener(equation).nextValue().toString()
                } catch (_: Exception) {
                    equation
                }
            }
            else -> equation
        }

        var activity: Activity? = null
        var currentContext = context
        while (currentContext is android.content.ContextWrapper) {
            if (currentContext is Activity) {
                activity = currentContext
                break
            }
            currentContext = currentContext.baseContext
        }
        if (activity == null && context is Activity) {
            activity = context
        }
        if (activity == null) return
        activity.runOnUiThread {
            val prefs = activity.getSharedPreferences("ai_prefs", Context.MODE_PRIVATE)
            val optIn = prefs.getInt("ai_opt_in", -1)
            val accepted = prefs.getBoolean("ai_accepted", false)
            if (optIn != 1 && !accepted) {
                onRequestSettings()
            } else {
                AiEngine.showGeminiDialog(activity, decodedEquation) { res -> onAiResult(res) }
            }
        }
    }
}

object AiEngine {
    fun showGeminiDialog(activity: Activity, equation: String, onResult: (String) -> Unit) {
        val prefs = activity.getSharedPreferences("ai_prefs", Context.MODE_PRIVATE)
        val savedKey = prefs.getString("ai_api_key", "") ?: ""

        if (savedKey.isBlank()) {
            askForApiKey(activity) { newKey ->
                prefs.edit()
                    .putInt("ai_opt_in", 1)
                    .putBoolean("ai_accepted", true)
                    .putString("ai_api_key", newKey)
                    .apply()
                solveWithGemini(activity, newKey, equation, onResult)
            }
        } else {
            solveWithGemini(activity, savedKey, equation, onResult)
        }
    }

    private fun askForApiKey(activity: Activity, onKeyProvided: (String) -> Unit) {
        val input = EditText(activity).apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            hint = "AI Studio (Gemini) / OpenAI API Key"
        }
        val layout = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 40, 50, 10)
            addView(input)
        }

        AlertDialog.Builder(activity)
            .setTitle(Translator.tr("Bulut AI Bağlantısı (Gemini)"))
            .setMessage(Translator.tr("Adım adım çözüm üretebilmek için Google Gemini API anahtarına ihtiyacımız var. (Ücretsiz olarak Google AI Studio'dan alabilirsiniz)."))
            .setView(layout)
            .setPositiveButton(Translator.tr("Kaydet ve Çöz")) { _, _ ->
                val key = input.text.toString().trim()
                if (key.isNotEmpty()) onKeyProvided(key)
            }
            .setNegativeButton(Translator.tr("İptal"), null)
            .show()
    }

    private fun solveWithGemini(activity: Activity, apiKey: String, equation: String, onResult: (String) -> Unit) {
        val prefs = activity.getSharedPreferences("ai_prefs", Context.MODE_PRIVATE)
        var apiUrl = prefs.getString("ai_api_url", "https://generativelanguage.googleapis.com/v1beta/openai/chat/completions")
            ?: "https://generativelanguage.googleapis.com/v1beta/openai/chat/completions"
        if (apiUrl.contains("generativelanguage.googleapis.com") && !apiUrl.contains("/openai/") && apiUrl.endsWith("/chat/completions")) {
            apiUrl = apiUrl.replace("/v1beta/chat/completions", "/v1beta/openai/chat/completions")
        }
        val modelName = prefs.getString("ai_model_name", "gemini-2.5-flash") ?: "gemini-2.5-flash"

        onResult("<div class=\"ai-loading\">${Translator.tr("AI Adımları Üretiyor...")}</div>")

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val currentLanguage = Translator.currentLang
                val prompt = "The user wants to solve this mathematical expression: $equation.\nPlease explain step-by-step how to solve it like an academic professor. Respond in $currentLanguage. Only provide the solution steps, and use LaTeX for formulas ($...$ or $$...$$)."

                var responseText = ""

                val url = java.net.URL(apiUrl)
                val conn = url.openConnection() as java.net.HttpURLConnection
                conn.connectTimeout = 15000
                conn.readTimeout = 60000
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json")
                conn.setRequestProperty("Authorization", "Bearer $apiKey")
                conn.doOutput = true

                val jsonPayload = org.json.JSONObject().apply {
                    put("model", modelName)
                    val msgs = org.json.JSONArray()
                    msgs.put(org.json.JSONObject().apply {
                        put("role", "user")
                        put("content", prompt)
                    })
                    put("messages", msgs)
                }.toString()

                conn.outputStream.write(jsonPayload.toByteArray(Charsets.UTF_8))

                if (conn.responseCode == 200) {
                    val responseJson = conn.inputStream.bufferedReader().readText()
                    try {
                        val jsonObject = org.json.JSONObject(responseJson)
                        val choices = jsonObject.getJSONArray("choices")
                        val message = choices.getJSONObject(0).getJSONObject("message")
                        responseText = message.getString("content")
                    } catch (e: Exception) {
                        responseText = "${Translator.tr("Format hatası: JSON ayrıştırılamadı.")} ${e.message}"
                    }
                } else {
                    val code = conn.responseCode
                    val errText = conn.errorStream?.bufferedReader()?.readText() ?: ""

                    val userFriendlyError = when {
                        code == 401 || errText.contains("invalid_api_key") || errText.contains("authentication") ->
                            Translator.tr("Geçersiz API Anahtarı: Lütfen Ayarlar (⚙️) menüsünden bilgilerinizi kontrol edin.")
                        code == 429 || errText.contains("quota") || errText.contains("rate_limit") ->
                            Translator.tr("Kota Aşıldı: API kullanım limitiniz dolmuş veya çok hızlı istek atıyorsunuz.")
                        code == 503 || errText.contains("high demand") || errText.contains("overloaded") ->
                            Translator.tr("Sunucu Yoğunluğu: AI sunucuları şu an çok yoğun. Lütfen kısa bir süre sonra tekrar deneyin.")
                        code == 400 || errText.contains("invalid_request") ->
                            Translator.tr("Geçersiz İstek: Gönderilen denklem API tarafından desteklenmiyor olabilir.")
                        else -> "${Translator.tr("API Bağlantı Hatası: Beklenmeyen bir sunucu hatası oluştu.")} ($code)"
                    }

                    val safeErrText = errText.replace("<", "&lt;").replace(">", "&gt;")
                    val safeEqEncoded = encodeForAskAI(equation)
                    val retryBtn = "<div style=\"margin-top: 10px;\"><button onclick=\"Android.askAI('$safeEqEncoded')\" style=\"background: transparent; border: 1px solid var(--error-color); color: var(--error-color); padding: 8px 16px; border-radius: 20px; cursor: pointer; font-weight: bold; font-size: 0.9em; margin-top: 8px;\">${Translator.tr("🔄 Tekrar Dene")}</button></div>"
                    responseText = "<div style=\"color: var(--error-color); background: var(--error-bg); padding: 16px; margin-top: 10px; border-radius: 16px; box-shadow: 0 4px 6px rgba(0,0,0,0.1);\">" +
                        "☁️ $userFriendlyError" +
                        retryBtn +
                        "<details style=\"margin-top: 8px;\"><summary style=\"cursor: pointer; opacity: 0.8; font-size: 0.85em;\">${Translator.tr("Geliştirici Detayları")}</summary><pre style=\"font-size: 0.75em; overflow-x: auto; margin-top: 5px; opacity: 0.7;\">$safeErrText</pre></details>" +
                        "</div>"
                }

                withContext(Dispatchers.Main) {
                    var formatted = responseText
                    formatted = formatted.replace(Regex("(?m)^### (.*)$"), "<div style=\"font-weight: bold; font-size: 1.1em; color: var(--ai-color); margin-top: 15px; margin-bottom: 5px;\">$1</div>")
                    formatted = formatted.replace(Regex("\\*\\*(.*?)\\*\\*"), "<b>$1</b>")
                    formatted = formatted.replace("\n", "<br/>")
                    formatted = formatted.replace("<br/>---<br/>", "<hr style=\"border-top: 1px dashed var(--ai-color); opacity: 0.3; margin: 15px 0;\"/>")
                    formatted = formatted.replace("---", "<hr style=\"border-top: 1px dashed var(--ai-color); opacity: 0.3; margin: 15px 0;\"/>")
                    onResult(formatted)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    val msg = e.message ?: "Error"
                    val userFriendly = if (msg.contains("Unable to resolve host") || msg.contains("timeout")) {
                        Translator.tr("İnternet Bağlantısı Yok: Lütfen cihazınızın internete bağlı olduğundan emin olun.")
                    } else if (msg.contains("API key") || msg.contains("401")) {
                        activity.getSharedPreferences("ai_prefs", Context.MODE_PRIVATE).edit().remove("ai_api_key").apply()
                        Translator.tr("Geçersiz API Anahtarı: Lütfen Ayarlar (⚙️) menüsünden bilgilerinizi kontrol edin.")
                    } else {
                        Translator.tr("Sistem Hatası: İstek oluşturulamadı.")
                    }

                    val safeMsg = msg.replace("<", "&lt;").replace(">", "&gt;")
                    val safeEqEncoded = encodeForAskAI(equation)
                    val retryBtn = "<div style=\"margin-top: 10px;\"><button onclick=\"Android.askAI('$safeEqEncoded')\" style=\"background: transparent; border: 1px solid var(--error-color); color: var(--error-color); padding: 8px 16px; border-radius: 20px; cursor: pointer; font-weight: bold; font-size: 0.9em; margin-top: 8px;\">${Translator.tr("🔄 Tekrar Dene")}</button></div>"
                    val errorHtml = "<div style=\"color: var(--error-color); background: var(--error-bg); padding: 16px; margin-top: 10px; border-radius: 16px; box-shadow: 0 4px 6px rgba(0,0,0,0.1);\">" +
                        "☁️ $userFriendly" +
                        retryBtn +
                        "<details style=\"margin-top: 8px;\"><summary style=\"cursor: pointer; opacity: 0.8; font-size: 0.85em;\">${Translator.tr("Geliştirici Detayları")}</summary><pre style=\"font-size: 0.75em; overflow-x: auto; margin-top: 5px; opacity: 0.7;\">$safeMsg</pre></details>" +
                        "</div>"
                    onResult(errorHtml)
                }
            }
        }
    }
}
