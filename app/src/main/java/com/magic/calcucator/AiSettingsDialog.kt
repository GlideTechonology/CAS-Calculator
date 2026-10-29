package com.magic.calcucator

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AiSettingsDialog(
    onDismiss: () -> Unit,
    onSave: () -> Unit
) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("ai_prefs", Context.MODE_PRIVATE)

    var selectedProvider by remember { mutableStateOf(prefs.getString("ai_provider", "gemini") ?: "gemini") }
    var apiUrl by remember {
        val saved = prefs.getString("ai_api_url", "https://generativelanguage.googleapis.com/v1beta/openai/chat/completions")
            ?: "https://generativelanguage.googleapis.com/v1beta/openai/chat/completions"
        mutableStateOf(
            if (saved.contains("generativelanguage.googleapis.com") && !saved.contains("/openai/")) {
                "https://generativelanguage.googleapis.com/v1beta/openai/chat/completions"
            } else saved
        )
    }
    var apiKey by remember { mutableStateOf(prefs.getString("ai_api_key", "") ?: "") }
    var apiKeyVisible by remember { mutableStateOf(false) }
    var modelName by remember { mutableStateOf(prefs.getString("ai_model_name", "gemini-2.5-flash") ?: "gemini-2.5-flash") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("⚙️ ${Translator.tr("AI Ayarları")}", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    Translator.tr("Platform Seçimi (Otomatik Doldurur):"),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            selectedProvider = "gemini"
                            apiUrl = "https://generativelanguage.googleapis.com/v1beta/openai/chat/completions"
                            modelName = "gemini-2.5-flash"
                        }
                ) {
                    RadioButton(
                        selected = selectedProvider == "gemini",
                        onClick = {
                            selectedProvider = "gemini"
                            apiUrl = "https://generativelanguage.googleapis.com/v1beta/openai/chat/completions"
                            modelName = "gemini-2.5-flash"
                        }
                    )
                    Text("Google Gemini")
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            selectedProvider = "openai"
                            apiUrl = "https://api.openai.com/v1/chat/completions"
                            modelName = "gpt-4o-mini"
                        }
                ) {
                    RadioButton(
                        selected = selectedProvider == "openai",
                        onClick = {
                            selectedProvider = "openai"
                            apiUrl = "https://api.openai.com/v1/chat/completions"
                            modelName = "gpt-4o-mini"
                        }
                    )
                    Text("OpenAI (ChatGPT)")
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { selectedProvider = "custom" }
                ) {
                    RadioButton(
                        selected = selectedProvider == "custom",
                        onClick = { selectedProvider = "custom" }
                    )
                    Text(Translator.tr("Özel (Custom / DeepSeek / OpenRouter)"))
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = apiUrl,
                    onValueChange = { apiUrl = it },
                    label = { Text("API Endpoint URL") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    label = { Text(Translator.tr("API Anahtarı (Zorunlu)")) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    visualTransformation = if (apiKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        IconButton(onClick = { apiKeyVisible = !apiKeyVisible }) {
                            Icon(
                                imageVector = if (apiKeyVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                contentDescription = if (apiKeyVisible) Translator.tr("Gizle") else Translator.tr("Göster")
                            )
                        }
                    }
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = modelName,
                    onValueChange = { modelName = it },
                    label = { Text(Translator.tr("Model Adı")) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                prefs.edit()
                    .putInt("ai_opt_in", 1)
                    .putBoolean("ai_accepted", true)
                    .putString("ai_provider", selectedProvider)
                    .putString("ai_api_url", apiUrl.trim())
                    .putString("ai_api_key", apiKey.trim())
                    .putString("ai_model_name", modelName.trim())
                    .apply()
                onSave()
            }) {
                Text(Translator.tr("Kaydet"))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(Translator.tr("İptal")) }
        }
    )
}
