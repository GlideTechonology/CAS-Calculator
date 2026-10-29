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
    var expression by remember { mutableStateOf("") }
    var result by remember { mutableStateOf("") }
    
    Surface(
        modifier = Modifier
            .width(240.dp)
            .clip(RoundedCornerShape(16.dp)),
        color = Color(0xFF1E1E1E).copy(alpha = 0.95f),
        shadowElevation = 8.dp
    ) {
        Column {
            // Header (Drag handle & Close)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF2C2C2C))
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            onDrag(dragAmount.x, dragAmount.y)
                        }
                    }
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.DragHandle, contentDescription = "Drag", tint = Color.Gray)
                Spacer(modifier = Modifier.weight(1f))
                Icon(
                    Icons.Filled.Close, contentDescription = "Close", tint = Color.Gray,
                    modifier = Modifier.clickable { onClose() }
                )
            }
            
            // Display
            Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                Text(
                    text = expression,
                    color = Color.White,
                    fontSize = 20.sp,
                    textAlign = TextAlign.End,
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 1
                )
                Text(
                    text = result,
                    color = AccentOrange,
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
            
            Column(modifier = Modifier.padding(8.dp)) {
                for (row in buttons) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        for (btn in row) {
                            val isAction = btn in listOf("÷", "×", "-", "+", "=", "C")
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .padding(4.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isAction) Color(0xFF3C3C3C) else Color(0xFF2C2C2C))
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
                                Text(btn, color = if (isAction) AccentOrange else Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
