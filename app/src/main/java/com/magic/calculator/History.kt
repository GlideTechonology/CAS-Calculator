package com.magic.calculator

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class HistoryEntry(val expression: String, val result: String, val timestamp: Long)

/** Stores calculation history as JSON in SharedPreferences. */
class HistoryStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("CalcHistory", Context.MODE_PRIVATE)

    fun load(): List<HistoryEntry> {
        val raw = prefs.getString(KEY, null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            List(arr.length()) { i ->
                val o = arr.getJSONObject(i)
                HistoryEntry(o.getString("e"), o.getString("r"), o.getLong("t"))
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun save(entries: List<HistoryEntry>) {
        val arr = JSONArray()
        entries.take(MAX_ENTRIES).forEach {
            arr.put(JSONObject().put("e", it.expression).put("r", it.result).put("t", it.timestamp))
        }
        prefs.edit().putString(KEY, arr.toString()).apply()
    }

    companion object {
        private const val KEY = "entries"
        const val MAX_ENTRIES = 200
    }
}

@Composable
fun HistoryScreen(
    entries: List<HistoryEntry>,
    textColor: Color,
    onBack: () -> Unit,
    onUse: (String) -> Unit,
    onDelete: (HistoryEntry) -> Unit,
    onClearAll: () -> Unit
) {
    val context = LocalContext.current
    val accentColor = LocalAccentColor.current
    var confirmClear by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp).padding(top = 32.dp)) {
        ToolTopBar(title = Translator.tr("İşlem Geçmişi"), textColor = textColor, onBack = onBack) {
            if (entries.isNotEmpty()) {
                IconButton(onClick = { confirmClear = true }) {
                    Icon(Icons.Filled.DeleteSweep, contentDescription = Translator.tr("Tümünü sil"), tint = textColor)
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))

        if (entries.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Filled.History,
                        contentDescription = null,
                        tint = textColor.copy(alpha = 0.3f),
                        modifier = Modifier.size(72.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(Translator.tr("Henüz işlem yok"), color = textColor.copy(alpha = 0.5f), fontSize = 16.sp)
                    Text(
                        Translator.tr("\"=\" tuşuna bastığınız işlemler burada görünür"),
                        color = textColor.copy(alpha = 0.35f),
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
            return@Column
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            var lastHeader: String? = null
            entries.forEach { entry ->
                val header = dayLabel(entry.timestamp)
                if (header != lastHeader) {
                    lastHeader = header
                    item(key = "h_${entry.timestamp}_$header") {
                        Text(
                            header,
                            color = accentColor,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(top = 16.dp, bottom = 4.dp, start = 4.dp)
                        )
                    }
                }
                item(key = "e_${entry.timestamp}_${entry.expression.hashCode()}") {
                    HistoryRow(
                        entry = entry,
                        textColor = textColor,
                        onUse = onUse,
                        onCopy = {
                            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            cm.setPrimaryClip(ClipData.newPlainText(Translator.tr("HesapMakinesi"), entry.result))
                            Toast.makeText(context, Translator.tr("Sonuç kopyalandı"), Toast.LENGTH_SHORT).show()
                        },
                        onDelete = { onDelete(entry) }
                    )
                }
            }
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text(Translator.tr("Geçmiş silinsin mi?")) },
            text = { Text(Translator.tr("Tüm işlem geçmişi kalıcı olarak silinecek.")) },
            confirmButton = {
                TextButton(onClick = { confirmClear = false; onClearAll() }) {
                    Text(Translator.tr("Sil"), color = Color(0xFFE53935))
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmClear = false }) { Text(Translator.tr("Vazgeç")) }
            }
        )
    }
}

@Composable
private fun HistoryRow(
    entry: HistoryEntry,
    textColor: Color,
    onUse: (String) -> Unit,
    onCopy: () -> Unit,
    onDelete: () -> Unit
) {
    val timeFmt = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f).padding(start = 4.dp), horizontalAlignment = Alignment.End) {
                // Tap expression → load into calculator for editing
                Text(
                    text = entry.expression,
                    color = textColor.copy(alpha = 0.55f),
                    fontSize = 18.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.End,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onUse(entry.expression) }
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
                // Tap result → load result into calculator
                Text(
                    text = "= ${entry.result}",
                    color = textColor,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onUse(entry.result) }
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
                Text(
                    text = timeFmt.format(Date(entry.timestamp)),
                    color = textColor.copy(alpha = 0.35f),
                    fontSize = 11.sp,
                    modifier = Modifier.padding(end = 6.dp)
                )
            }
            Column {
                IconButton(onClick = onCopy) {
                    Icon(Icons.Filled.ContentCopy, contentDescription = Translator.tr("Kopyala"), tint = textColor.copy(alpha = 0.6f), modifier = Modifier.size(20.dp))
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Filled.Delete, contentDescription = Translator.tr("Sil"), tint = textColor.copy(alpha = 0.6f), modifier = Modifier.size(20.dp))
                }
            }
        }
        HorizontalDivider(color = textColor.copy(alpha = 0.08f))
    }
}

private fun dayLabel(timestamp: Long): String {
    val cal = Calendar.getInstance().apply { timeInMillis = timestamp }
    val today = Calendar.getInstance()
    val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
    fun same(a: Calendar, b: Calendar) =
        a.get(Calendar.YEAR) == b.get(Calendar.YEAR) && a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR)
    return when {
        same(cal, today) -> Translator.tr("Bugün")
        same(cal, yesterday) -> Translator.tr("Dün")
        else -> SimpleDateFormat("d MMMM yyyy", Locale.getDefault()).format(Date(timestamp))
    }
}
