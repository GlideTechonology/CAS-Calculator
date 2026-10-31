package com.magic.calculator

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import com.magic.calculator.core.*
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationCompat
import androidx.lifecycle.Lifecycle
import com.magic.calculator.ui.theme.CalculatorTheme
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class FloatingLifecycleOwner : LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    private val store = ViewModelStore()

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry
    override val viewModelStore: ViewModelStore get() = store

    fun onCreate() {
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
    }
    fun onStart() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
    }
    fun onResume() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }
    fun onDestroy() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        store.clear()
    }
}

class FloatingService : Service() {
    private lateinit var windowManager: WindowManager
    private var composeView: ComposeView? = null
    private val floatingLifecycleOwner = FloatingLifecycleOwner()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        startForegroundService()
        
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        
        @Suppress("DEPRECATION")
        val layoutFlag: Int = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            WindowManager.LayoutParams.TYPE_PHONE
        }
        
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )
        
        params.gravity = Gravity.TOP or Gravity.START
        params.x = 100
        params.y = 100
        
        floatingLifecycleOwner.onCreate()
        floatingLifecycleOwner.onStart()
        floatingLifecycleOwner.onResume()
        
        try {
            composeView = ComposeView(this).apply {
                setViewTreeLifecycleOwner(floatingLifecycleOwner)
                setViewTreeViewModelStoreOwner(floatingLifecycleOwner)
                setViewTreeSavedStateRegistryOwner(floatingLifecycleOwner)
                
                setContent {
                    CalculatorTheme {
                        FloatingCalculatorUI(
                            onClose = { stopSelf() },
                            onDrag = { dx, dy ->
                                params.x += dx.toInt()
                                params.y += dy.toInt()
                                windowManager.updateViewLayout(this, params)
                            }
                        )
                    }
                }
            }
            
            windowManager.addView(composeView, params)
        } catch (e: Exception) {
            e.printStackTrace()
            stopSelf()
        }
    }
    
    private fun startForegroundService() {
        val channelId = "floating_calc_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Floating Calculator", NotificationManager.IMPORTANCE_LOW)
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle(Translator.tr("Yüzen Hesap Makinesi Aktif"))
            .setContentText(Translator.tr("Kapatmak için penceredeki çarpıya basın."))
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .build()
        startForeground(1, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        composeView?.let { windowManager.removeView(it) }
        floatingLifecycleOwner.onDestroy()
    }
}

@Composable
fun FloatingCalculatorUI(onClose: () -> Unit, onDrag: (Float, Float) -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val isSystemDark = androidx.compose.foundation.isSystemInDarkTheme()

    val prefs = remember { context.getSharedPreferences("CalcSettings", Context.MODE_PRIVATE) }
    val themeName = remember { prefs.getString("theme", AppTheme.AMOLED.name) }
    val appTheme = remember(themeName) {
        try { AppTheme.valueOf(themeName ?: AppTheme.AMOLED.name) } catch (_: Exception) { AppTheme.AMOLED }
    }
    val isMaterialYou = appTheme == AppTheme.MATERIAL_YOU && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val resolvedTheme = when (appTheme) {
        AppTheme.SYSTEM -> if (isSystemDark) AppTheme.AMOLED else AppTheme.LIGHT
        AppTheme.MATERIAL_YOU -> if (isSystemDark) AppTheme.DARK else AppTheme.LIGHT
        else -> appTheme
    }
    val isLight = resolvedTheme == AppTheme.LIGHT

    val dynamicColorScheme = remember(isMaterialYou, isSystemDark) {
        if (isMaterialYou) {
            if (isSystemDark) androidx.compose.material3.dynamicDarkColorScheme(context)
            else androidx.compose.material3.dynamicLightColorScheme(context)
        } else null
    }

    val customPrimary = remember { if (prefs.contains("primaryColor")) Color(prefs.getLong("primaryColor", 0L)) else null }
    val customSecondary = remember { if (prefs.contains("secondaryColor")) Color(prefs.getLong("secondaryColor", 0L)) else null }
    val customNumberKey = remember { if (prefs.contains("numberKeyColor")) Color(prefs.getLong("numberKeyColor", 0L)) else null }
    val customDisplayText = remember { if (prefs.contains("displayTextColor")) Color(prefs.getLong("displayTextColor", 0L)) else null }
    val customKeyText = remember { if (prefs.contains("keyTextColor")) Color(prefs.getLong("keyTextColor", 0L)) else null }

    val effectivePrimary = customPrimary ?: dynamicColorScheme?.primary ?: AccentOrange

    val windowBg = when {
        dynamicColorScheme != null -> if (isSystemDark) dynamicColorScheme.surface.copy(alpha = 0.95f) else dynamicColorScheme.surface.copy(alpha = 0.97f)
        resolvedTheme == AppTheme.LIGHT -> Color(0xFFF7F7F7).copy(alpha = 0.97f)
        resolvedTheme == AppTheme.DARK -> Color(0xFF222222).copy(alpha = 0.95f)
        else -> Color(0xFF141414).copy(alpha = 0.96f)
    }

    val headerBg = when {
        dynamicColorScheme != null -> dynamicColorScheme.surfaceVariant.copy(alpha = 0.7f)
        isLight -> Color(0xFFE8E8E8)
        else -> Color(0xFF2C2C2C)
    }

    val headerIconColor = customDisplayText
        ?: (if (dynamicColorScheme != null) dynamicColorScheme.onSurfaceVariant else if (isLight) Color(0xFF555555) else Color(0xFFAAAAAA))

    val displayTextColor = customDisplayText
        ?: when {
            dynamicColorScheme != null -> dynamicColorScheme.onSurface
            isLight -> Color.Black
            else -> Color.White
        }

    val resultColor = effectivePrimary

    var expression by remember { mutableStateOf("") }
    var result by remember { mutableStateOf("") }
    
    Surface(
        modifier = Modifier
            .width(248.dp)
            .clip(RoundedCornerShape(16.dp))
            .then(
                if (isLight) Modifier.border(1.dp, Color(0x33000000), RoundedCornerShape(16.dp))
                else Modifier.border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(16.dp))
            ),
        color = windowBg,
        shadowElevation = 10.dp
    ) {
        Column {
            // Header (Drag handle & Close)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(headerBg)
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            onDrag(dragAmount.x, dragAmount.y)
                        }
                    }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.DragHandle, contentDescription = "Drag", tint = headerIconColor, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Calc", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = headerIconColor)
                Spacer(modifier = Modifier.weight(1f))
                Icon(
                    Icons.Filled.Close, contentDescription = "Close", tint = headerIconColor,
                    modifier = Modifier.size(20.dp).clickable { onClose() }
                )
            }
            
            // Display
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)) {
                Text(
                    text = expression.ifEmpty { "0" },
                    color = displayTextColor.copy(alpha = if (expression.isEmpty()) 0.4f else 0.85f),
                    fontSize = 18.sp,
                    textAlign = TextAlign.End,
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 1
                )
                Text(
                    text = result.ifEmpty { " " },
                    color = resultColor,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.End,
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 1
                )
            }
            
            // Buttons
            val buttons = listOf(
                listOf("7", "8", "9", "÷"),
                listOf("4", "5", "6", "×"),
                listOf("1", "2", "3", "-"),
                listOf("C", "0", "=", "+")
            )
            
            Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
                for (row in buttons) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        for (btn in row) {
                            val isOperator = btn in listOf("÷", "×", "-", "+")
                            val isEquals = btn == "="
                            val isClear = btn == "C"

                            val (btnBg, defaultBtnFg) = when {
                                isEquals -> {
                                    val bg = customPrimary ?: dynamicColorScheme?.primary ?: AccentOrange
                                    val fg = if (customPrimary != null) bg.contrastTextColor() else (dynamicColorScheme?.onPrimary ?: Color.White)
                                    bg to fg
                                }
                                isOperator -> {
                                    if (customPrimary != null) {
                                        customPrimary to customPrimary.contrastTextColor()
                                    } else if (dynamicColorScheme != null) {
                                        dynamicColorScheme.primaryContainer to dynamicColorScheme.onPrimaryContainer
                                    } else {
                                        AccentOrange to Color.White
                                    }
                                }
                                isClear -> {
                                    if (customSecondary != null) {
                                        customSecondary to customSecondary.contrastTextColor()
                                    } else if (dynamicColorScheme != null) {
                                        dynamicColorScheme.secondaryContainer to dynamicColorScheme.onSecondaryContainer
                                    } else {
                                        (if (isLight) Color(0xFFCFCFD4) else Color(0xFFA5A5A5)) to Color.Black
                                    }
                                }
                                else -> {
                                    if (customNumberKey != null) {
                                        customNumberKey to customNumberKey.contrastTextColor()
                                    } else if (dynamicColorScheme != null) {
                                        val numBg = if (isLight) dynamicColorScheme.surfaceVariant.copy(alpha = 0.55f) else dynamicColorScheme.surfaceVariant.copy(alpha = 0.7f)
                                        numBg to dynamicColorScheme.onSurface
                                    } else {
                                        (if (isLight) Color.White else Color(0xFF333333)) to (if (isLight) Color.Black else Color.White)
                                    }
                                }
                            }

                            val btnFg = customKeyText ?: defaultBtnFg

                            Box(
                                modifier = Modifier
                                    .size(50.dp, 44.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(btnBg)
                                    .then(
                                        if (isLight && !isOperator && !isEquals && dynamicColorScheme == null)
                                            Modifier.border(1.dp, Color(0x1A000000), RoundedCornerShape(10.dp))
                                        else Modifier
                                    )
                                    .clickable {
                                        when (btn) {
                                            "C" -> { expression = ""; result = "" }
                                            "=" -> {
                                                val ev = evaluateStrict(expression)
                                                if (ev.isNotEmpty() && ev != "Err") {
                                                    result = ev
                                                    expression = ev
                                                } else {
                                                    result = "Err"
                                                }
                                            }
                                            else -> {
                                                expression += btn
                                                result = safeEvaluate(expression)
                                            }
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = btn,
                                    color = btnFg,
                                    fontSize = 19.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
