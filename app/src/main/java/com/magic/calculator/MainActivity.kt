package com.magic.calculator
import android.media.ToneGenerator

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.media.AudioManager

import android.os.Build
import java.math.BigDecimal
import java.math.BigInteger
import java.math.MathContext
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.SoundEffectConstants
import android.view.View
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import android.media.SoundPool
import android.net.Uri
import kotlinx.coroutines.asCoroutineDispatcher
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.TextLayoutResult
import java.util.Locale
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import com.magic.calculator.core.*
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import android.provider.Settings
import android.content.Intent
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.magic.calculator.ui.theme.CalculatorTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.E
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tan
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.sqrt
import kotlin.math.pow
import kotlin.math.round
import kotlin.math.roundToInt
import kotlin.math.absoluteValue
import kotlin.time.Duration.Companion.milliseconds

enum class AppTheme(val displayName: String) { 
    SYSTEM("System Default"),
    MATERIAL_YOU("Material You (Dynamic)"),
    LIGHT("Light Theme"), 
    DARK("Dark Theme"), 
    AMOLED("Pure Black (AMOLED)") 
}

val PresetPrimaryColors = listOf(
    0xFFFF9500 to "Vivid Orange",
    0xFF2196F3 to "Electric Blue",
    0xFF10B981 to "Emerald Green",
    0xFF8B5CF6 to "Royal Violet",
    0xFFEC4899 to "Hot Pink",
    0xFF06B6D4 to "Cyan Glow",
    0xFFEF4444 to "Crimson Red",
    0xFFF59E0B to "Amber Gold",
    0xFF14B8A6 to "Teal",
    0xFF6366F1 to "Indigo"
)

val PresetSecondaryColors = listOf(
    0xFF4A4A4A to "Graphite",
    0xFF374151 to "Slate",
    0xFF1E293B to "Midnight",
    0xFF312E81 to "Deep Purple",
    0xFF064E3B to "Forest",
    0xFF27272A to "Charcoal"
)

val PresetNumberColors = listOf(
    0xFF333333 to "Standard Dark",
    0xFF1F2937 to "Dark Slate",
    0xFF121212 to "AMOLED Black",
    0xFF3F3F46 to "Stone Gray",
    0xFF172554 to "Navy Blue"
)

val PresetTextColors = listOf(
    0xFFFFFFFF to "Pure White",
    0xFFE0E0E0 to "Light Gray",
    0xFF000000 to "Pure Black",
    0xFFFFD700 to "Gold",
    0xFF00E676 to "Neon Green",
    0xFF00E5FF to "Cyan",
    0xFFFF4081 to "Pink",
    0xFFFF9100 to "Orange",
    0xFFB388FF to "Lavender"
)

enum class NavBarMode(val displayName: String) {
    HIDE("Immersive (Hidden)"),
    INSETS("Üstünde Dur (Standart)")
}

data class AppSettings(
    val theme: AppTheme = AppTheme.AMOLED,
    val primaryColor: Long? = null,
    val secondaryColor: Long? = null,
    val numberKeyColor: Long? = null,
    val displayTextColor: Long? = null,
    val keyTextColor: Long? = null,
    val navBarMode: NavBarMode = NavBarMode.HIDE,
    val vibrationEnabled: Boolean = true,
    val vibrationStrength: Float = 0.5f,
    val soundEnabled: Boolean = true,
    val soundVolume: Float = 0.7f,
    val customSoundUri: String? = null,
    val customSoundDuration: Int = 100
)


object SoundManager {
    private const val maxStreams = 4 
    private val toneGenerators = arrayOfNulls<ToneGenerator>(maxStreams)
    private var currentIndex = 0
    private var currentVolume = -1
    private var isInitializing = false

    private var soundPool: SoundPool? = null
    private var customSoundId: Int = 0
    private var currentCustomUri: String? = null
    private var isSoundPoolReady = false

    fun init(context: Context, volumePercent: Float, customUri: String?) {
        val volume = (volumePercent * 100).toInt()
        
        CoroutineScope(Dispatchers.IO).launch {
            if (currentCustomUri != customUri || soundPool == null || !isSoundPoolReady) {
                currentCustomUri = customUri
                soundPool?.release()
                
                if (!customUri.isNullOrEmpty()) {
                    val newSoundPool = SoundPool.Builder().setMaxStreams(maxStreams).build()
                    newSoundPool.setOnLoadCompleteListener { _, sampleId, status ->
                        if (status == 0) {
                            isSoundPoolReady = true
                            newSoundPool.play(sampleId, 0f, 0f, 1, 0, 1f)
                        }
                    }
                    soundPool = newSoundPool
                    
                    try {
                        val uri = Uri.parse(customUri)
                        val pfd = context.contentResolver.openFileDescriptor(uri, "r")
                        if (pfd != null) {
                            customSoundId = newSoundPool.load(pfd.fileDescriptor, 0, pfd.statSize, 1)
                            pfd.close()
                        }
                    } catch (e: Exception) {
                        currentCustomUri = null
                    }
                } else {
                    soundPool = null
                }
            }

            // Clean up existing engines if volume changed or on first run.
            // ToneGenerators are loaded lazily on-demand so startup time is not degraded!
            if (currentVolume != volume) {
                for (i in 0 until maxStreams) {
                    toneGenerators[i]?.release()
                    toneGenerators[i] = null
                }
                currentVolume = volume
            }
        }
    }

    fun playTone(symbol: String, volumePercent: Float, customDuration: Int = 100) {
        if (volumePercent <= 0f) return
        
        // Pitch matrix (assigns distinct melodic notes for each key, including custom sounds)
        val pitch = when (symbol) {
            "1" -> 1.1f
            "2" -> 1.2f
            "3" -> 1.3f
            "4" -> 1.4f
            "5" -> 1.5f
            "6" -> 1.6f
            "7" -> 1.7f
            "8" -> 1.8f
            "9" -> 1.9f
            "0" -> 1.0f
            "+/-" -> 1.2f
            "+", "-", "*", "÷", "%", "^", "!" -> 0.8f
            "(", ")" -> 0.9f
            "=", "Copy" -> 0.7f
            "AC", "⌫" -> 0.6f
            else -> 1.0f
        }
        
        // Play with pitch modulation if user provided a custom audio sound
        if (soundPool != null && isSoundPoolReady && customSoundId > 0) {
            val streamId = soundPool?.play(customSoundId, volumePercent, volumePercent, 1, 0, pitch)
            if (streamId != null && streamId > 0 && customDuration > 0) {
                CoroutineScope(Dispatchers.Default).launch {
                    delay(customDuration.toLong())
                    var currentV = volumePercent
                    val fadeSteps = 5
                    val stepSize = volumePercent / fadeSteps
                    for (i in 0 until fadeSteps) {
                        currentV -= stepSize
                        if (currentV < 0f) currentV = 0f
                        soundPool?.setVolume(streamId, currentV, currentV)
                        delay(6) 
                    }
                    soundPool?.stop(streamId)
                }
            }
            return
        }
        
        // Lazy Loading
        var tg = toneGenerators[currentIndex]
        if (tg == null) {
            try {
                tg = ToneGenerator(AudioManager.STREAM_SYSTEM, currentVolume)
                toneGenerators[currentIndex] = tg
            } catch (_: Exception) { return }
        }
        currentIndex = (currentIndex + 1) % maxStreams
        
        try {
            val tone = when (symbol) {
                "0" -> ToneGenerator.TONE_DTMF_0
                "1" -> ToneGenerator.TONE_DTMF_1
                "2" -> ToneGenerator.TONE_DTMF_2
                "3" -> ToneGenerator.TONE_DTMF_3
                "4" -> ToneGenerator.TONE_DTMF_4
                "5" -> ToneGenerator.TONE_DTMF_5
                "6" -> ToneGenerator.TONE_DTMF_6
                "7" -> ToneGenerator.TONE_DTMF_7
                "8" -> ToneGenerator.TONE_DTMF_8
                "9" -> ToneGenerator.TONE_DTMF_9
                "+", "-", "*", "÷", "%", "^", "!" -> ToneGenerator.TONE_DTMF_A
                "AC", "⌫" -> ToneGenerator.TONE_DTMF_B
                "=" -> ToneGenerator.TONE_DTMF_C
                "(", ")" -> ToneGenerator.TONE_DTMF_S 
                "." -> ToneGenerator.TONE_DTMF_P 
                else -> ToneGenerator.TONE_DTMF_D
            }
            tg.stopTone() 
            tg.startTone(tone, 70) 
        } catch (_: Exception) {}
    }
}

class MainActivity : ComponentActivity() {
    private var currentIntentAction by androidx.compose.runtime.mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            currentIntentAction = intent.action
            StartupTracker.markOnCreate()
        } catch (_: Throwable) {}
        enableEdgeToEdge()
        setContent {
            val prefs = getSharedPreferences("CalcSettings", Context.MODE_PRIVATE)
            val savedLang = try { prefs.getString("AppLanguage", null) } catch (_: Throwable) { null }
            if (savedLang != null) {
                try { Translator.currentLang = savedLang } catch (_: Throwable) {}
            }

            CalculatorTheme {
                MainApp(currentIntentAction)
            }
        }
        try {
            StartupTracker.trackFirstFrame(this)
        } catch (_: Throwable) {}
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        currentIntentAction = intent.action
    }
}

enum class Screen { CALCULATOR, SETTINGS, HISTORY, CONVERTER, RULER, DAILY, GRAPHING, CAS }

private var cachedVibrator: Vibrator? = null

fun getVibratorCompat(context: Context): Vibrator {
    if (cachedVibrator == null) {
        cachedVibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.applicationContext.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.applicationContext.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }
    return cachedVibrator!!
}

@Composable
fun MainApp(intentAction: String? = null) {
    StartupTracker.markFirstComposition()
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("CalcSettings", Context.MODE_PRIVATE)
    
    var settings by remember { 
        mutableStateOf(
            AppSettings(
                theme = try {
                    AppTheme.valueOf(prefs.getString("theme", AppTheme.SYSTEM.name) ?: AppTheme.SYSTEM.name)
                } catch (_: Throwable) {
                    AppTheme.SYSTEM
                },
                primaryColor = if (prefs.contains("primaryColor")) prefs.getLong("primaryColor", 0L) else null,
                secondaryColor = if (prefs.contains("secondaryColor")) prefs.getLong("secondaryColor", 0L) else null,
                numberKeyColor = if (prefs.contains("numberKeyColor")) prefs.getLong("numberKeyColor", 0L) else null,
                displayTextColor = if (prefs.contains("displayTextColor")) prefs.getLong("displayTextColor", 0L) else null,
                keyTextColor = if (prefs.contains("keyTextColor")) prefs.getLong("keyTextColor", 0L) else null,
                navBarMode = try {
                    NavBarMode.valueOf(prefs.getString("navBarMode", NavBarMode.HIDE.name) ?: NavBarMode.HIDE.name)
                } catch (_: Throwable) {
                    NavBarMode.HIDE
                },
                vibrationEnabled = prefs.getBoolean("vibrationEnabled", true),
                vibrationStrength = prefs.getFloat("vibrationStrength", 0.5f),
                soundEnabled = prefs.getBoolean("soundEnabled", true),
                soundVolume = prefs.getFloat("soundVolume", 0.7f),
                customSoundUri = prefs.getString("customSoundUri", null),
                customSoundDuration = prefs.getInt("customSoundDuration", 100)
            )
        )
    }
    
    val view = LocalView.current
    val window = (context as? ComponentActivity)?.window
    LaunchedEffect(settings.navBarMode) {
        if (window != null) {
            val controller = androidx.core.view.WindowCompat.getInsetsController(window, view)
            if (settings.navBarMode == NavBarMode.HIDE) {
                controller.systemBarsBehavior = androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                controller.hide(androidx.core.view.WindowInsetsCompat.Type.navigationBars())
            } else {
                controller.show(androidx.core.view.WindowInsetsCompat.Type.navigationBars())
            }
        }
    }
    
    LaunchedEffect(settings.soundVolume, settings.customSoundUri) {
        SoundManager.init(context, settings.soundVolume, settings.customSoundUri)
    }
    
    fun saveSettings(newSettings: AppSettings) {
        settings = newSettings
        prefs.edit().apply {
            putString("theme", newSettings.theme.name)
            newSettings.primaryColor?.let { putLong("primaryColor", it) } ?: remove("primaryColor")
            newSettings.secondaryColor?.let { putLong("secondaryColor", it) } ?: remove("secondaryColor")
            newSettings.numberKeyColor?.let { putLong("numberKeyColor", it) } ?: remove("numberKeyColor")
            newSettings.displayTextColor?.let { putLong("displayTextColor", it) } ?: remove("displayTextColor")
            newSettings.keyTextColor?.let { putLong("keyTextColor", it) } ?: remove("keyTextColor")
            putString("navBarMode", newSettings.navBarMode.name)
            putBoolean("vibrationEnabled", newSettings.vibrationEnabled)
            putFloat("vibrationStrength", newSettings.vibrationStrength)
            putBoolean("soundEnabled", newSettings.soundEnabled)
            putFloat("soundVolume", newSettings.soundVolume)
            putString("customSoundUri", newSettings.customSoundUri)
            putInt("customSoundDuration", newSettings.customSoundDuration)
            apply()
        }
    }

    var currentScreen by remember { mutableStateOf(Screen.CALCULATOR) }
    var initialToolName by remember { mutableStateOf<String?>(null) }
    
    LaunchedEffect(intentAction) {
        if (intentAction?.startsWith("OPEN_TOOL_") == true) {
            val tool = intentAction.removePrefix("OPEN_TOOL_")
            when(tool) {
                "RULER" -> currentScreen = Screen.RULER
                "BMI" -> { currentScreen = Screen.DAILY; initialToolName = Translator.tr("BMI") }
                "CURRENCY" -> { currentScreen = Screen.DAILY; initialToolName = Translator.tr("Currency Converter") }
                "DISCOUNT" -> { currentScreen = Screen.DAILY; initialToolName = Translator.tr("Discount") }
                "VAT" -> { currentScreen = Screen.DAILY; initialToolName = Translator.tr("Sales Tax / VAT") }
                "TIP" -> { currentScreen = Screen.DAILY; initialToolName = Translator.tr("Tip & Split") }
                "LOAN" -> { currentScreen = Screen.DAILY; initialToolName = Translator.tr("Loan") }
                "GPA" -> { currentScreen = Screen.DAILY; initialToolName = Translator.tr("GPA / Exam") }
                "AREA" -> { currentScreen = Screen.DAILY; initialToolName = Translator.tr("Shapes & Area") }
                "DATE" -> { currentScreen = Screen.DAILY; initialToolName = Translator.tr("Date Calculator") }
                "CUSTOMEQ" -> { currentScreen = Screen.DAILY; initialToolName = Translator.tr("Custom Equations") }
            }
        }
    }

    
    androidx.activity.compose.BackHandler(enabled = currentScreen != Screen.CALCULATOR) {
        currentScreen = Screen.CALCULATOR
    }
    val historyStore = remember { HistoryStore(context) }
    var historyEntries by remember { mutableStateOf(historyStore.load()) }
    var initialExpression by remember { mutableStateOf("") }

    val isSystemDark = androidx.compose.foundation.isSystemInDarkTheme()
    val isMaterialYou = settings.theme == AppTheme.MATERIAL_YOU && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val resolvedTheme = when (settings.theme) {
        AppTheme.SYSTEM -> if (isSystemDark) AppTheme.AMOLED else AppTheme.LIGHT
        AppTheme.MATERIAL_YOU -> if (isSystemDark) AppTheme.DARK else AppTheme.LIGHT
        else -> settings.theme
    }

    val dynamicColorScheme = if (isMaterialYou) {
        if (isSystemDark) androidx.compose.material3.dynamicDarkColorScheme(context)
        else androidx.compose.material3.dynamicLightColorScheme(context)
    } else null

    val bgColor = when {
        dynamicColorScheme != null -> if (isSystemDark) dynamicColorScheme.background else dynamicColorScheme.surface
        resolvedTheme == AppTheme.LIGHT -> Color(0xFFF0F0F0)
        resolvedTheme == AppTheme.DARK -> Color(0xFF1C1C1C)
        else -> Color.Black
    }
    
    val textColor = settings.displayTextColor?.let { Color(it) }
        ?: when {
            dynamicColorScheme != null -> if (isSystemDark) dynamicColorScheme.onBackground else dynamicColorScheme.onSurface
            resolvedTheme == AppTheme.LIGHT -> Color.Black
            else -> Color.White
        }

    val effectivePrimary = settings.primaryColor?.let { Color(it) }
        ?: dynamicColorScheme?.primary
        ?: AccentOrange

    val baseModifier = Modifier.fillMaxSize().let {
        if (settings.navBarMode == NavBarMode.INSETS) it.navigationBarsPadding() else it
    }

    val appBody: @Composable () -> Unit = {
        Surface(color = bgColor, modifier = baseModifier) {
            AnimatedContent(
                targetState = currentScreen,
                transitionSpec = {
                    if (targetState != Screen.CALCULATOR) {
                        slideInHorizontally { width -> width } + fadeIn() togetherWith slideOutHorizontally { width -> -width } + fadeOut()
                    } else {
                        slideInHorizontally { width -> -width } + fadeIn() togetherWith slideOutHorizontally { width -> width } + fadeOut()
                    }
                },
                label = "screen_transition"
            ) { screen ->
                when (screen) {
                    Screen.SETTINGS -> {
                        SettingsScreen(
                            settings = settings,
                            textColor = textColor,
                            onSettingsChange = { saveSettings(it) },
                            onBack = { currentScreen = Screen.CALCULATOR }
                        )
                    }
                    Screen.HISTORY -> {
                        HistoryScreen(
                            entries = historyEntries,
                            textColor = textColor,
                            onBack = { currentScreen = Screen.CALCULATOR },
                            onUse = { exp -> 
                                initialExpression = exp
                                currentScreen = Screen.CALCULATOR
                            },
                            onDelete = { entry ->
                                historyEntries = historyEntries.filter { it != entry }
                                historyStore.save(historyEntries)
                            },
                            onClearAll = {
                                historyEntries = emptyList()
                                historyStore.save(historyEntries)
                            }
                        )
                    }
                    Screen.CONVERTER -> {
                        UnitConverterScreen(
                            textColor = textColor,
                            onBack = { currentScreen = Screen.CALCULATOR }
                        )
                    }
                    Screen.RULER -> {
                        RulerScreen(
                            settings = settings,
                            textColor = textColor,
                            onBack = { currentScreen = Screen.CALCULATOR }
                        )
                    }
                    Screen.DAILY -> {
                        DailyToolsScreen(
                            textColor = textColor,
                            initialToolName = initialToolName,
                            onBack = { currentScreen = Screen.CALCULATOR; initialToolName = null }
                        )
                    }
                    Screen.GRAPHING -> {
                        GraphingScreen(
                            textColor = textColor,
                            onBack = { currentScreen = Screen.CALCULATOR }
                        )
                    }
                    Screen.CAS -> {
                        CasScreen(
                            appSettings = settings,
                            textColor = textColor,
                            onBack = { currentScreen = Screen.CALCULATOR }
                        )
                    }
                    Screen.CALCULATOR -> {
                        CalculatorScreen(
                            settings = settings,
                            textColor = textColor,
                            initialExpression = initialExpression,
                            onClearInitialExpression = { initialExpression = "" },
                            onOpenSettings = { currentScreen = Screen.SETTINGS },
                            onOpenHistory = { currentScreen = Screen.HISTORY },
                            onOpenConverter = { currentScreen = Screen.CONVERTER },
                            onOpenRuler = { currentScreen = Screen.RULER },
                            onOpenDaily = { currentScreen = Screen.DAILY },
                            onOpenGraphing = { currentScreen = Screen.GRAPHING },
                            onOpenCas = { currentScreen = Screen.CAS },
                            onSaveHistory = { expr, res ->
                                val entry = HistoryEntry(expr, res, System.currentTimeMillis())
                                historyEntries = listOf(entry) + historyEntries
                                historyStore.save(historyEntries)
                            },
                            modifier = Modifier.fillMaxSize().padding(top = 32.dp)
                        )
                    }
                }
            }
        }
    }

    val isLight = resolvedTheme == AppTheme.LIGHT
    val baseScheme = if (isLight) lightColorScheme() else darkColorScheme()
    val appColorScheme = (dynamicColorScheme ?: baseScheme).let { scheme ->
        val customPrim = settings.primaryColor?.let { Color(it) }
        if (customPrim != null) {
            scheme.copy(
                primary = customPrim,
                primaryContainer = customPrim.copy(alpha = 0.25f),
                onPrimaryContainer = customPrim
            )
        } else if (dynamicColorScheme == null) {
            scheme.copy(
                primary = AccentOrange,
                primaryContainer = AccentOrange.copy(alpha = 0.25f),
                onPrimaryContainer = AccentOrange
            )
        } else {
            scheme
        }
    }.copy(
        background = bgColor,
        surface = bgColor,
        onBackground = textColor,
        onSurface = textColor
    )

    CompositionLocalProvider(LocalAccentColor provides effectivePrimary) {
        MaterialTheme(colorScheme = appColorScheme) {
            appBody()
        }
    }
}

enum class ColorPickerTarget {
    PRIMARY,
    SECONDARY,
    NUMBER_KEYS,
    DISPLAY_TEXT,
    KEY_TEXT
}

@Composable
fun CustomColorPickerDialog(
    title: String,
    initialColor: Color,
    textColor: Color,
    onDismiss: () -> Unit,
    onColorSelected: (Long) -> Unit
) {
    val hsv = remember(initialColor) {
        val arr = FloatArray(3)
        val colorInt = android.graphics.Color.argb(
            255,
            (initialColor.red * 255).toInt().coerceIn(0, 255),
            (initialColor.green * 255).toInt().coerceIn(0, 255),
            (initialColor.blue * 255).toInt().coerceIn(0, 255)
        )
        android.graphics.Color.colorToHSV(colorInt, arr)
        arr
    }

    var hue by remember { mutableFloatStateOf(hsv[0]) }
    var saturation by remember { mutableFloatStateOf(hsv[1].coerceIn(0.01f, 1f)) }
    var value by remember { mutableFloatStateOf(hsv[2].coerceIn(0.01f, 1f)) }

    val currentColorInt = remember(hue, saturation, value) {
        android.graphics.Color.HSVToColor(floatArrayOf(hue, saturation, value))
    }
    val currentColor = remember(currentColorInt) { Color(currentColorInt) }
    var hexInput by remember(currentColorInt) {
        mutableStateOf(String.format("%06X", 0xFFFFFF and currentColorInt))
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Palette, contentDescription = null, tint = currentColor, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(title, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Live Preview Card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(currentColor)
                        .border(1.dp, textColor.copy(alpha = 0.2f), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "#${hexInput.uppercase()}",
                        color = currentColor.contrastTextColor(),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // HEX Input
                OutlinedTextField(
                    value = hexInput,
                    onValueChange = { newHex ->
                        val filtered = newHex.filter { it.isDigit() || it in 'a'..'f' || it in 'A'..'F' }.take(6)
                        hexInput = filtered
                        if (filtered.length == 6) {
                            try {
                                val parsedInt = (filtered.toLong(16) or 0xFF000000L).toInt()
                                val newHsv = FloatArray(3)
                                android.graphics.Color.colorToHSV(parsedInt, newHsv)
                                hue = newHsv[0]
                                saturation = newHsv[1]
                                value = newHsv[2]
                            } catch (_: Exception) {}
                        }
                    },
                    label = { Text(Translator.tr("HEX Color Code")) },
                    prefix = { Text("#") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Hue Slider
                Column {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(Translator.tr("Hue"), fontSize = 12.sp, color = textColor.copy(alpha = 0.7f))
                        Text("${hue.toInt()}°", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = textColor)
                    }
                    val rainbowGradient = remember {
                        Brush.horizontalGradient(
                            listOf(
                                Color(0xFFFF0000), Color(0xFFFFFF00), Color(0xFF00FF00),
                                Color(0xFF00FFFF), Color(0xFF0000FF), Color(0xFFFF00FF),
                                Color(0xFFFF0000)
                            )
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(rainbowGradient)
                    )
                    Slider(
                        value = hue,
                        onValueChange = { hue = it },
                        valueRange = 0f..360f,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Saturation Slider
                Column {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(Translator.tr("Saturation"), fontSize = 12.sp, color = textColor.copy(alpha = 0.7f))
                        Text("${(saturation * 100).toInt()}%", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = textColor)
                    }
                    val satGradient = remember(hue, value) {
                        val baseHueColor = Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, 1f, value)))
                        val grayColor = Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, 0f, value)))
                        Brush.horizontalGradient(listOf(grayColor, baseHueColor))
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(satGradient)
                    )
                    Slider(
                        value = saturation,
                        onValueChange = { saturation = it },
                        valueRange = 0f..1f,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Brightness / Value Slider
                Column {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(Translator.tr("Brightness"), fontSize = 12.sp, color = textColor.copy(alpha = 0.7f))
                        Text("${(value * 100).toInt()}%", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = textColor)
                    }
                    val valGradient = remember(hue, saturation) {
                        val fullColor = Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, saturation, 1f)))
                        Brush.horizontalGradient(listOf(Color.Black, fullColor))
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(valGradient)
                    )
                    Slider(
                        value = value,
                        onValueChange = { value = it },
                        valueRange = 0f..1f,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Quick Palette Swatches
                Text(Translator.tr("Quick Swatches"), fontSize = 12.sp, color = textColor.copy(alpha = 0.7f))
                val quickColors = listOf(
                    0xFFF44336L, 0xFFE91E63L, 0xFF9C27B0L, 0xFF673AB7L, 0xFF3F51B5L, 0xFF2196F3L,
                    0xFF00BCD4L, 0xFF009688L, 0xFF4CAF50L, 0xFF8BC34AL, 0xFFFFEB3BL, 0xFFFF9800L,
                    0xFFFF5722L, 0xFF795548L, 0xFF607D8BL, 0xFF212121L, 0xFFFFFFFFL, 0xFF000000L
                )
                quickColors.chunked(6).forEach { rowColors ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        rowColors.forEach { qColorLong ->
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .background(Color(qColorLong))
                                    .border(1.dp, textColor.copy(alpha = 0.25f), CircleShape)
                                    .clickable {
                                        val newHsv = FloatArray(3)
                                        android.graphics.Color.colorToHSV((qColorLong or 0xFF000000L).toInt(), newHsv)
                                        hue = newHsv[0]
                                        saturation = newHsv[1]
                                        value = newHsv[2]
                                    }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalColorLong = (currentColorInt.toLong() and 0xFFFFFFFFL)
                    onColorSelected(finalColorLong)
                }
            ) {
                Text(Translator.tr("Kaydet"))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(Translator.tr("İptal"))
            }
        }
    )
}

@Composable
fun SettingsScreen(
    settings: AppSettings,
    textColor: Color,
    onSettingsChange: (AppSettings) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    var activeColorPickerTarget by remember { mutableStateOf<ColorPickerTarget?>(null) }

    activeColorPickerTarget?.let { target ->
        val title = when (target) {
            ColorPickerTarget.PRIMARY -> Translator.tr("Primary Accent Color")
            ColorPickerTarget.SECONDARY -> Translator.tr("Secondary Function Keys")
            ColorPickerTarget.NUMBER_KEYS -> Translator.tr("Number Keys Color")
            ColorPickerTarget.DISPLAY_TEXT -> Translator.tr("Display & Icons Color")
            ColorPickerTarget.KEY_TEXT -> Translator.tr("Keypad Text & Symbols")
        }
        val initialColor = when (target) {
            ColorPickerTarget.PRIMARY -> settings.primaryColor?.let { Color(it) } ?: AccentOrange
            ColorPickerTarget.SECONDARY -> settings.secondaryColor?.let { Color(it) } ?: Color(0xFF4A4A4A)
            ColorPickerTarget.NUMBER_KEYS -> settings.numberKeyColor?.let { Color(it) } ?: Color(0xFF333333)
            ColorPickerTarget.DISPLAY_TEXT -> settings.displayTextColor?.let { Color(it) } ?: textColor
            ColorPickerTarget.KEY_TEXT -> settings.keyTextColor?.let { Color(it) } ?: Color.White
        }
        CustomColorPickerDialog(
            title = title,
            initialColor = initialColor,
            textColor = textColor,
            onDismiss = { activeColorPickerTarget = null },
            onColorSelected = { pickedColorLong ->
                when (target) {
                    ColorPickerTarget.PRIMARY -> onSettingsChange(settings.copy(primaryColor = pickedColorLong))
                    ColorPickerTarget.SECONDARY -> onSettingsChange(settings.copy(secondaryColor = pickedColorLong))
                    ColorPickerTarget.NUMBER_KEYS -> onSettingsChange(settings.copy(numberKeyColor = pickedColorLong))
                    ColorPickerTarget.DISPLAY_TEXT -> onSettingsChange(settings.copy(displayTextColor = pickedColorLong))
                    ColorPickerTarget.KEY_TEXT -> onSettingsChange(settings.copy(keyTextColor = pickedColorLong))
                }
                activeColorPickerTarget = null
            }
        )
    }
    
    Column(modifier = Modifier.fillMaxSize().padding(16.dp).padding(top = 32.dp).verticalScroll(scrollState)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                @Suppress("DEPRECATION")
                Icon(Icons.Filled.ArrowBack, contentDescription = "Geri", tint = textColor)
            }
            Text(Translator.tr("Ayarlar"), fontSize = 24.sp, fontWeight = FontWeight.Bold, color = textColor)
        }
        Spacer(modifier = Modifier.height(24.dp))
        
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Palette, contentDescription = null, tint = textColor, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(Translator.tr("Tema Seçimi"), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = textColor)
        }
        Spacer(modifier = Modifier.height(8.dp))
        AppTheme.entries.forEach { theme ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onSettingsChange(settings.copy(theme = theme)) }
                    .padding(vertical = 8.dp, horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = settings.theme == theme,
                    onClick = { onSettingsChange(settings.copy(theme = theme)) },
                    colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary)
                )
                Spacer(modifier = Modifier.width(8.dp))
                val extraHint = if (theme == AppTheme.MATERIAL_YOU && Build.VERSION.SDK_INT < Build.VERSION_CODES.S) " (Android 12+)" else ""
                Text(Translator.tr(theme.displayName) + extraHint, fontSize = 16.sp, color = textColor)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider(color = textColor.copy(alpha = 0.2f))
        Spacer(modifier = Modifier.height(16.dp))

        // Custom Colors Section
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Filled.ColorLens, contentDescription = null, tint = textColor, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(Translator.tr("Custom Colors"), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = textColor, modifier = Modifier.weight(1f))
            if (settings.primaryColor != null || settings.secondaryColor != null || settings.numberKeyColor != null || settings.displayTextColor != null || settings.keyTextColor != null) {
                TextButton(
                    onClick = {
                        onSettingsChange(settings.copy(primaryColor = null, secondaryColor = null, numberKeyColor = null, displayTextColor = null, keyTextColor = null))
                    }
                ) {
                    Text(Translator.tr("Reset Colors to Default"), fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
        Spacer(modifier = Modifier.height(10.dp))

        // Primary Accent Color
        Text(Translator.tr("Primary Accent Color"), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = textColor.copy(alpha = 0.8f))
        Spacer(modifier = Modifier.height(8.dp))
        val isCustomPrimary = settings.primaryColor != null && settings.primaryColor !in PresetPrimaryColors.map { it.first }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            item {
                val isSelected = settings.primaryColor == null
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(AccentOrange)
                        .border(if (isSelected) 3.dp else 1.dp, if (isSelected) textColor else Color.Transparent, CircleShape)
                        .clickable { onSettingsChange(settings.copy(primaryColor = null)) },
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                }
            }
            items(PresetPrimaryColors) { (colorLong, _) ->
                val isSelected = settings.primaryColor == colorLong
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(colorLong))
                        .border(if (isSelected) 3.dp else 1.dp, if (isSelected) textColor else Color.Transparent, CircleShape)
                        .clickable { onSettingsChange(settings.copy(primaryColor = colorLong)) },
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Icon(Icons.Filled.Check, contentDescription = null, tint = Color(colorLong).contrastTextColor(), modifier = Modifier.size(20.dp))
                    }
                }
            }
            if (isCustomPrimary && settings.primaryColor != null) {
                item {
                    val customVal = settings.primaryColor!!
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(customVal))
                            .border(3.dp, textColor, CircleShape)
                            .clickable { activeColorPickerTarget = ColorPickerTarget.PRIMARY },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Check, contentDescription = null, tint = Color(customVal).contrastTextColor(), modifier = Modifier.size(20.dp))
                    }
                }
            }
            item {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(
                            brush = Brush.sweepGradient(
                                listOf(
                                    Color(0xFFFF0000), Color(0xFFFFFF00), Color(0xFF00FF00),
                                    Color(0xFF00FFFF), Color(0xFF0000FF), Color(0xFFFF00FF),
                                    Color(0xFFFF0000)
                                )
                            )
                        )
                        .border(if (isCustomPrimary) 2.dp else 1.dp, textColor.copy(alpha = 0.4f), CircleShape)
                        .clickable { activeColorPickerTarget = ColorPickerTarget.PRIMARY },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Palette, contentDescription = Translator.tr("Pick Any Color"), tint = Color.White, modifier = Modifier.size(20.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Secondary / Function Keys
        Text(Translator.tr("Secondary Function Keys"), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = textColor.copy(alpha = 0.8f))
        Spacer(modifier = Modifier.height(8.dp))
        val isCustomSecondary = settings.secondaryColor != null && settings.secondaryColor !in PresetSecondaryColors.map { it.first }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            item {
                val isSelected = settings.secondaryColor == null
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF4A4A4A))
                        .border(if (isSelected) 3.dp else 1.dp, if (isSelected) textColor else Color.Transparent, CircleShape)
                        .clickable { onSettingsChange(settings.copy(secondaryColor = null)) },
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                }
            }
            items(PresetSecondaryColors) { (colorLong, _) ->
                val isSelected = settings.secondaryColor == colorLong
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(colorLong))
                        .border(if (isSelected) 3.dp else 1.dp, if (isSelected) textColor else Color.Transparent, CircleShape)
                        .clickable { onSettingsChange(settings.copy(secondaryColor = colorLong)) },
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                }
            }
            if (isCustomSecondary && settings.secondaryColor != null) {
                item {
                    val customVal = settings.secondaryColor!!
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(customVal))
                            .border(3.dp, textColor, CircleShape)
                            .clickable { activeColorPickerTarget = ColorPickerTarget.SECONDARY },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Check, contentDescription = null, tint = Color(customVal).contrastTextColor(), modifier = Modifier.size(20.dp))
                    }
                }
            }
            item {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(
                            brush = Brush.sweepGradient(
                                listOf(
                                    Color(0xFFFF0000), Color(0xFFFFFF00), Color(0xFF00FF00),
                                    Color(0xFF00FFFF), Color(0xFF0000FF), Color(0xFFFF00FF),
                                    Color(0xFFFF0000)
                                )
                            )
                        )
                        .border(if (isCustomSecondary) 2.dp else 1.dp, textColor.copy(alpha = 0.4f), CircleShape)
                        .clickable { activeColorPickerTarget = ColorPickerTarget.SECONDARY },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Palette, contentDescription = Translator.tr("Pick Any Color"), tint = Color.White, modifier = Modifier.size(20.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Number Keys
        Text(Translator.tr("Number Keys Color"), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = textColor.copy(alpha = 0.8f))
        Spacer(modifier = Modifier.height(8.dp))
        val isCustomNumber = settings.numberKeyColor != null && settings.numberKeyColor !in PresetNumberColors.map { it.first }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            item {
                val isSelected = settings.numberKeyColor == null
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF333333))
                        .border(if (isSelected) 3.dp else 1.dp, if (isSelected) textColor else Color.Transparent, CircleShape)
                        .clickable { onSettingsChange(settings.copy(numberKeyColor = null)) },
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                }
            }
            items(PresetNumberColors) { (colorLong, _) ->
                val isSelected = settings.numberKeyColor == colorLong
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(colorLong))
                        .border(if (isSelected) 3.dp else 1.dp, if (isSelected) textColor else Color.Transparent, CircleShape)
                        .clickable { onSettingsChange(settings.copy(numberKeyColor = colorLong)) },
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                }
            }
            if (isCustomNumber && settings.numberKeyColor != null) {
                item {
                    val customVal = settings.numberKeyColor!!
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(customVal))
                            .border(3.dp, textColor, CircleShape)
                            .clickable { activeColorPickerTarget = ColorPickerTarget.NUMBER_KEYS },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Check, contentDescription = null, tint = Color(customVal).contrastTextColor(), modifier = Modifier.size(20.dp))
                    }
                }
            }
            item {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(
                            brush = Brush.sweepGradient(
                                listOf(
                                    Color(0xFFFF0000), Color(0xFFFFFF00), Color(0xFF00FF00),
                                    Color(0xFF00FFFF), Color(0xFF0000FF), Color(0xFFFF00FF),
                                    Color(0xFFFF0000)
                                )
                            )
                        )
                        .border(if (isCustomNumber) 2.dp else 1.dp, textColor.copy(alpha = 0.4f), CircleShape)
                        .clickable { activeColorPickerTarget = ColorPickerTarget.NUMBER_KEYS },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Palette, contentDescription = Translator.tr("Pick Any Color"), tint = Color.White, modifier = Modifier.size(20.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Display & Icons Color
        Text(Translator.tr("Display & Icons Color"), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = textColor.copy(alpha = 0.8f))
        Spacer(modifier = Modifier.height(8.dp))
        val isCustomDisplayText = settings.displayTextColor != null && settings.displayTextColor !in PresetTextColors.map { it.first }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            item {
                val isSelected = settings.displayTextColor == null
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(textColor)
                        .border(if (isSelected) 3.dp else 1.dp, if (isSelected) textColor else Color.Transparent, CircleShape)
                        .clickable { onSettingsChange(settings.copy(displayTextColor = null)) },
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Icon(Icons.Filled.Check, contentDescription = null, tint = textColor.contrastTextColor(), modifier = Modifier.size(20.dp))
                    }
                }
            }
            items(PresetTextColors) { (colorLong, _) ->
                val isSelected = settings.displayTextColor == colorLong
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(colorLong))
                        .border(if (isSelected) 3.dp else 1.dp, if (isSelected) textColor else Color.Transparent, CircleShape)
                        .clickable { onSettingsChange(settings.copy(displayTextColor = colorLong)) },
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Icon(Icons.Filled.Check, contentDescription = null, tint = Color(colorLong).contrastTextColor(), modifier = Modifier.size(20.dp))
                    }
                }
            }
            if (isCustomDisplayText && settings.displayTextColor != null) {
                item {
                    val customVal = settings.displayTextColor!!
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(customVal))
                            .border(3.dp, textColor, CircleShape)
                            .clickable { activeColorPickerTarget = ColorPickerTarget.DISPLAY_TEXT },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Check, contentDescription = null, tint = Color(customVal).contrastTextColor(), modifier = Modifier.size(20.dp))
                    }
                }
            }
            item {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(
                            brush = Brush.sweepGradient(
                                listOf(
                                    Color(0xFFFF0000), Color(0xFFFFFF00), Color(0xFF00FF00),
                                    Color(0xFF00FFFF), Color(0xFF0000FF), Color(0xFFFF00FF),
                                    Color(0xFFFF0000)
                                )
                            )
                        )
                        .border(if (isCustomDisplayText) 2.dp else 1.dp, textColor.copy(alpha = 0.4f), CircleShape)
                        .clickable { activeColorPickerTarget = ColorPickerTarget.DISPLAY_TEXT },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Palette, contentDescription = Translator.tr("Pick Any Color"), tint = Color.White, modifier = Modifier.size(20.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Keypad Text & Symbols
        Text(Translator.tr("Keypad Text & Symbols"), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = textColor.copy(alpha = 0.8f))
        Spacer(modifier = Modifier.height(8.dp))
        val isCustomKeyText = settings.keyTextColor != null && settings.keyTextColor !in PresetTextColors.map { it.first }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            item {
                val isSelected = settings.keyTextColor == null
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF666666))
                        .border(if (isSelected) 3.dp else 1.dp, if (isSelected) textColor else Color.Transparent, CircleShape)
                        .clickable { onSettingsChange(settings.copy(keyTextColor = null)) },
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                    }
                }
            }
            items(PresetTextColors) { (colorLong, _) ->
                val isSelected = settings.keyTextColor == colorLong
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(colorLong))
                        .border(if (isSelected) 3.dp else 1.dp, if (isSelected) textColor else Color.Transparent, CircleShape)
                        .clickable { onSettingsChange(settings.copy(keyTextColor = colorLong)) },
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Icon(Icons.Filled.Check, contentDescription = null, tint = Color(colorLong).contrastTextColor(), modifier = Modifier.size(20.dp))
                    }
                }
            }
            if (isCustomKeyText && settings.keyTextColor != null) {
                item {
                    val customVal = settings.keyTextColor!!
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(customVal))
                            .border(3.dp, textColor, CircleShape)
                            .clickable { activeColorPickerTarget = ColorPickerTarget.KEY_TEXT },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Check, contentDescription = null, tint = Color(customVal).contrastTextColor(), modifier = Modifier.size(20.dp))
                    }
                }
            }
            item {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(
                            brush = Brush.sweepGradient(
                                listOf(
                                    Color(0xFFFF0000), Color(0xFFFFFF00), Color(0xFF00FF00),
                                    Color(0xFF00FFFF), Color(0xFF0000FF), Color(0xFFFF00FF),
                                    Color(0xFFFF0000)
                                )
                            )
                        )
                        .border(if (isCustomKeyText) 2.dp else 1.dp, textColor.copy(alpha = 0.4f), CircleShape)
                        .clickable { activeColorPickerTarget = ColorPickerTarget.KEY_TEXT },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Palette, contentDescription = Translator.tr("Pick Any Color"), tint = Color.White, modifier = Modifier.size(20.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider(color = textColor.copy(alpha = 0.2f))
        Spacer(modifier = Modifier.height(16.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Fullscreen, contentDescription = null, tint = textColor, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(Translator.tr("Navigasyon Çubuğu Modu"), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = textColor)
        }
        Spacer(modifier = Modifier.height(8.dp))
        NavBarMode.entries.forEach { mode ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onSettingsChange(settings.copy(navBarMode = mode)) }
                    .padding(vertical = 8.dp, horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = settings.navBarMode == mode,
                    onClick = { onSettingsChange(settings.copy(navBarMode = mode)) },
                    colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(Translator.tr(mode.displayName), fontSize = 16.sp, color = textColor)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider(color = textColor.copy(alpha = 0.2f))
        Spacer(modifier = Modifier.height(16.dp))

        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Filled.Vibration, contentDescription = null, tint = textColor, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(Translator.tr("Tuş Titreşimi"), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = textColor, modifier = Modifier.weight(1f))
            Switch(
                checked = settings.vibrationEnabled,
                onCheckedChange = { onSettingsChange(settings.copy(vibrationEnabled = it)) }
            )
        }
        
        AnimatedVisibility(visible = settings.vibrationEnabled) {
            Column(modifier = Modifier.padding(top = 8.dp, start = 8.dp, end = 8.dp)) {
                Text("${Translator.tr("Titreşim Gücü:")} %${(settings.vibrationStrength * 100).roundToInt()}", fontSize = 14.sp, color = textColor.copy(alpha = 0.7f))
                Slider(
                    value = settings.vibrationStrength,
                    onValueChange = { newValue ->
                        // Preview haptic vibration whenever stepping to the next 10% interval
                        val oldStep = (settings.vibrationStrength * 10).roundToInt()
                        val newStep = (newValue * 10).roundToInt()
                        onSettingsChange(settings.copy(vibrationStrength = newValue))
                        if (newStep != oldStep) vibrateWithStrength(context, newValue)
                    },
                    valueRange = 0.1f..1.0f,
                    steps = 8 // 10%, 20%, ... 100% -> 10 discrete steps
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider(color = textColor.copy(alpha = 0.2f))
        Spacer(modifier = Modifier.height(16.dp))

        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            @Suppress("DEPRECATION")
            Icon(Icons.Filled.VolumeUp, contentDescription = null, tint = textColor, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(Translator.tr("Tuş Sesi"), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = textColor, modifier = Modifier.weight(1f))
            Switch(
                checked = settings.soundEnabled,
                onCheckedChange = { onSettingsChange(settings.copy(soundEnabled = it)) }
            )
        }

        AnimatedVisibility(visible = settings.soundEnabled) {
            Column(modifier = Modifier.padding(top = 8.dp, start = 8.dp, end = 8.dp)) {
                val audioLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                    androidx.activity.result.contract.ActivityResultContracts.OpenDocument()
                ) { uri ->
                    if (uri != null) {
                        try {
                            context.contentResolver.takePersistableUriPermission(
                                uri,
                                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                            )
                            onSettingsChange(settings.copy(customSoundUri = uri.toString()))
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
                
                Text(Translator.tr("Özel Ses Dosyası"), fontSize = 14.sp, color = textColor.copy(alpha = 0.7f))
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Button(onClick = { audioLauncher.launch(arrayOf("audio/*")) }) {
                        Text(if (settings.customSoundUri != null) Translator.tr("Sesi Değiştir") else Translator.tr("Ses Dosyası Seç"))
                    }
                    if (settings.customSoundUri != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(onClick = { onSettingsChange(settings.copy(customSoundUri = null)) }) {
                            Icon(Icons.Filled.Delete, contentDescription = Translator.tr("Sıfırla"), tint = Color.Red)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                if (settings.customSoundUri != null) {
                    Text(Translator.tr("Aktif: Özel Yüklenen Ses"), fontSize = 12.sp, color = Color.Gray)
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("${Translator.tr("Çalma Süresi (Sesi Kes):")} ${settings.customSoundDuration}ms", fontSize = 14.sp, color = textColor.copy(alpha = 0.7f))
                    Slider(
                        value = settings.customSoundDuration.toFloat(),
                        onValueChange = { onSettingsChange(settings.copy(customSoundDuration = it.toInt())) },
                        onValueChangeFinished = {
                            SoundManager.playTone("=", settings.soundVolume, settings.customSoundDuration)
                        },
                        valueRange = 10f..1000f
                    )
                } else {
                    Text(Translator.tr("Aktif: Modern Tık Sesi"), fontSize = 12.sp, color = Color.Gray)
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text("${Translator.tr("Ses Seviyesi:")} %${(settings.soundVolume * 100).toInt()}", fontSize = 14.sp, color = textColor.copy(alpha = 0.7f))
                Slider(
                    value = settings.soundVolume,
                    onValueChange = { onSettingsChange(settings.copy(soundVolume = it)) },
                    onValueChangeFinished = {
                        SoundManager.playTone("=", settings.soundVolume, settings.customSoundDuration)
                    },
                    valueRange = 0.1f..1.0f
                )
            }
        }
        

                Spacer(modifier = Modifier.height(24.dp))
        Text(Translator.tr("Dil Seçimi (Language)"), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = textColor)
        Spacer(modifier = Modifier.height(8.dp))
        var expandedLang by remember { mutableStateOf(false) }
        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = { expandedLang = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(Translator.currentLang, color = textColor)
            }
            DropdownMenu(
                expanded = expandedLang,
                onDismissRequest = { expandedLang = false },
                modifier = Modifier.fillMaxWidth(0.9f)
            ) {
                Translator.supportedLanguages.forEach { lang ->
                    DropdownMenuItem(
                        text = { Text(lang) },
                        onClick = { 
                            Translator.currentLang = lang
                            context.getSharedPreferences("CalcSettings", Context.MODE_PRIVATE).edit().putString("AppLanguage", lang).apply()
                            expandedLang = false 
                        }
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        HorizontalDivider(color = textColor.copy(alpha = 0.2f))
        Spacer(modifier = Modifier.height(24.dp))
        Text(Translator.tr("Widget Yönetimi"), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = textColor)
        Text(Translator.tr("Hangi araçların widget listesinde görüneceğini seçin."), fontSize = 12.sp, color = textColor.copy(alpha = 0.6f))
        Spacer(modifier = Modifier.height(8.dp))
        
        val pm = context.packageManager
        val widgets = listOf(
            "Cetvel" to RulerWidgetProvider::class.java,
            "BMI Hesaplayıcı" to BMIWidgetProvider::class.java,
            "Döviz Çevirici" to CurrencyWidgetProvider::class.java,
            "İndirim Hesaplayıcı" to DiscountWidgetProvider::class.java,
            "KDV / Vergi" to VatWidgetProvider::class.java,
            "Bahşiş & Hesap Bölme" to TipWidgetProvider::class.java,
            "Kredi Hesaplama" to LoanWidgetProvider::class.java,
            "Not / GPA Hesaplama" to GpaWidgetProvider::class.java,
            "Şekiller & Alan" to AreaWidgetProvider::class.java,
            "Tarih Hesaplayıcı" to DateWidgetProvider::class.java,
            "Kendi Denklemlerim" to CustomEqWidgetProvider::class.java
        )
        
        widgets.forEach { (name, cls) ->
            val comp = android.content.ComponentName(context, cls)
            val isEnabled = pm.getComponentEnabledSetting(comp) == android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            var checked by remember { mutableStateOf(isEnabled) }
            
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(Translator.tr(name), color = textColor, modifier = Modifier.weight(1f))
                Switch(
                    checked = checked,
                    onCheckedChange = {
                        checked = it
                        val state = if (it) android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_ENABLED else android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DISABLED
                        pm.setComponentEnabledSetting(comp, state, android.content.pm.PackageManager.DONT_KILL_APP)
                    },
                    colors = SwitchDefaults.colors(checkedThumbColor = LocalAccentColor.current, checkedTrackColor = LocalAccentColor.current.copy(alpha = 0.5f))
                )
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        HorizontalDivider(color = textColor.copy(alpha = 0.2f))
        Spacer(modifier = Modifier.height(16.dp))
        
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(androidx.compose.material.icons.Icons.Filled.Psychology, contentDescription = null, tint = textColor, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(Translator.tr("Yapay Zeka (AI) Seçimi"), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = textColor)
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = {
                context.getSharedPreferences("ai_prefs", android.content.Context.MODE_PRIVATE)
                    .edit()
                    .putInt("ai_opt_in", -1)
                    .putBoolean("ai_accepted", false)
                    .apply()
                android.widget.Toast.makeText(context, Translator.tr("AI Ayarları sıfırlandı. CAS ekranında tekrar sorulacak."), android.widget.Toast.LENGTH_LONG).show()
            },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Text(Translator.tr("AI Asistanı Tercihini Sıfırla"), color = Color.White)
        }
        
        Spacer(modifier = Modifier.height(32.dp))
    }
}


// Feedback (Haptic and Audio) operations invoke low-level Android IPC (Binder IPC and JNI).
// Executing these on the Main Thread causes UI jank during rapid typing.
// Therefore, we offload all feedback execution to a dedicated single-threaded dispatcher.
private val feedbackDispatcher = java.util.concurrent.Executors.newSingleThreadExecutor().asCoroutineDispatcher()

/**
 * Unified haptic vibration handler for key presses and settings slider preview.
 * strength: 0.1 .. 1.0 -> increases both amplitude and duration for clearly perceptible feedback.
 */
@Suppress("DEPRECATION")
fun vibrateWithStrength(context: Context, strength: Float) {
    try {
        val vibrator = getVibratorCompat(context)
        if (!vibrator.hasVibrator()) return

        val s = strength.coerceIn(0f, 1f)
        val durationMs = 20L + (s * 60f).toLong()          // 20ms .. 80ms
        val amplitude = (s * 255f).toInt().coerceIn(1, 255)

        if (vibrator.hasAmplitudeControl()) {
            val effect = VibrationEffect.createOneShot(durationMs, amplitude)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val attr = android.os.VibrationAttributes.Builder()
                    .setUsage(android.os.VibrationAttributes.USAGE_TOUCH)
                    .build()
                vibrator.vibrate(effect, attr)
            } else {
                val attr = android.media.AudioAttributes.Builder()
                    .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(android.media.AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                    .build()
                vibrator.vibrate(effect, attr)
            }
        } else {
            vibrator.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
        }
    } catch (_: Exception) {}
}

fun performAdvancedFeedback(context: Context, view: View, settings: AppSettings, symbol: String = "") {
    if (settings.vibrationEnabled) {
        vibrateWithStrength(context, settings.vibrationStrength)
    }

    // Audio operations (JNI and AudioFlinger) are heavyweight; run asynchronously on feedback dispatcher:
    if (settings.soundEnabled) {
        kotlinx.coroutines.CoroutineScope(feedbackDispatcher).launch {
            try {
                SoundManager.playTone(symbol, settings.soundVolume, settings.customSoundDuration)
            } catch (_: Exception) {}
        }
    }
}

private fun isButtonEnabledForBase(symbol: String, currentBase: String, isProgrammerPage: Boolean): Boolean {
    if (!isProgrammerPage) return true
    return when (currentBase) {
        "BIN" -> symbol !in setOf("2", "3", "4", "5", "6", "7", "8", "9", "A", "B", "C", "D", "E", "F")
        "OCT" -> symbol !in setOf("8", "9", "A", "B", "C", "D", "E", "F")
        "DEC" -> symbol !in setOf("A", "B", "C", "D", "E", "F")
        else -> true // HEX allows 0-9 and A-F
    }
}

private fun formatGroupedBaseString(value: BigInteger, radix: Int): String {
    val isNeg = value.signum() < 0
    val groupSize = when (radix) {
        2, 16 -> 4
        8 -> 3
        else -> 0
    }
    if (radix != 10 && isNeg) {
        // 64-bit Two's Complement bitmask for negative numbers in binary, octal, hex
        val mask64 = BigInteger("FFFFFFFFFFFFFFFF", 16)
        val unsignedVal = value.and(mask64)
        val str = unsignedVal.toString(radix).uppercase()
        val grouped = if (groupSize > 0 && str.length > groupSize) {
            str.reversed().chunked(groupSize).joinToString(" ").reversed()
        } else str
        return grouped
    }

    val absStr = value.abs().toString(radix).uppercase()
    val grouped = if (groupSize > 0 && absStr.length > groupSize) {
        absStr.reversed().chunked(groupSize).joinToString(" ").reversed()
    } else absStr
    return if (isNeg) "-$grouped" else grouped
}

@Composable
fun CalculatorScreen(
    settings: AppSettings,
    textColor: Color,
    initialExpression: String = "",
    onClearInitialExpression: () -> Unit = {},
    onOpenSettings: () -> Unit,
    onOpenHistory: () -> Unit = {},
    onOpenConverter: () -> Unit = {},
    onOpenRuler: () -> Unit = {},
    onOpenDaily: () -> Unit = {},
    onOpenGraphing: () -> Unit = {},
    onOpenCas: () -> Unit = {},
    onSaveHistory: (String, String) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    val pagerState = rememberPagerState(initialPage = 1, pageCount = { 3 })
    val isProgrammerMode = pagerState.targetPage == 2

    var currentBase by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf("DEC") }
    val baseInt = when (currentBase) {
        "HEX" -> 16
        "OCT" -> 8
        "BIN" -> 2
        else -> 10
    }

    var expressionValue by androidx.compose.runtime.saveable.rememberSaveable(stateSaver = androidx.compose.ui.text.input.TextFieldValue.Saver) { mutableStateOf(TextFieldValue("")) }
    var showAiSettingsDialog by androidx.compose.runtime.remember { mutableStateOf(false) }

    if (showAiSettingsDialog) {
        AiSettingsDialog(
            onDismiss = { showAiSettingsDialog = false },
            onSave = { showAiSettingsDialog = false }
        )
    }

    // When leaving Programmer Mode (Page 2), automatically convert back to DEC so standard/scientific mode works normally
    LaunchedEffect(pagerState.settledPage) {
        if (pagerState.settledPage != 2 && currentBase != "DEC") {
            if (expressionValue.text.isNotEmpty()) {
                try {
                    val eval = evaluateForBaseConversions(expressionValue.text, baseInt)
                    if (eval != null) {
                        val decStr = eval.toString(10)
                        expressionValue = TextFieldValue(text = decStr, selection = TextRange(decStr.length))
                    }
                } catch (_: Exception) {}
            }
            currentBase = "DEC"
        }
    }

    LaunchedEffect(initialExpression) {
        if (initialExpression.isNotEmpty()) {
            expressionValue = TextFieldValue(text = initialExpression, selection = TextRange(initialExpression.length))
            onClearInitialExpression()
        }
    }

    val expression = expressionValue.text

    var calculatedResult by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf("") }
    var isCalculating by remember { mutableStateOf(false) }
    LaunchedEffect(expression, baseInt) {
        kotlinx.coroutines.delay(150) // Debounce: Kullanıcı hızlı yazarken sürekli hesaplama yapmasını engeller
        isCalculating = true
        calculatedResult = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
            kotlinx.coroutines.runInterruptible {
                safeEvaluate(expression, base = baseInt)
            }
        }
        isCalculating = false
    }

    val liveBigInt = remember(expression, baseInt, isProgrammerMode) {
        if (isProgrammerMode) evaluateForBaseConversions(expression, baseInt) else null
    }

    val context = LocalContext.current
    val view = LocalView.current
    val effectivePrimary = LocalAccentColor.current
    val isSystemDark = androidx.compose.foundation.isSystemInDarkTheme()
    val isMaterialYou = settings.theme == AppTheme.MATERIAL_YOU && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val dynamicColorScheme = if (isMaterialYou) {
        if (isSystemDark) androidx.compose.material3.dynamicDarkColorScheme(context)
        else androidx.compose.material3.dynamicLightColorScheme(context)
    } else null
    val effectiveSecondary = settings.secondaryColor?.let { Color(it) }
    val effectiveNumber = settings.numberKeyColor?.let { Color(it) }
    val customKeyTextColor = settings.keyTextColor?.let { Color(it) }
    val androidClipboard = remember { context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager }

    var dropTrigger by remember { mutableIntStateOf(0) }

    val focusRequester = remember { FocusRequester() }

    val textYOffset = remember { androidx.compose.animation.core.Animatable(0f) }
    val textAlpha = remember { androidx.compose.animation.core.Animatable(1f) }
    val typeOffsetX = remember { androidx.compose.animation.core.Animatable(0f) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(dropTrigger) {
        if (dropTrigger > 0) {
            textYOffset.snapTo(-60f)
            textAlpha.snapTo(0f)

            launch { textYOffset.animateTo(0f, spring(dampingRatio = 0.6f, stiffness = 300f)) }
            launch { textAlpha.animateTo(1f, tween(250)) }
        }
    }

    LaunchedEffect(Unit) {
        try { focusRequester.requestFocus() } catch (_: Exception) {}
    }

    fun onAction(action: String) {
        if (!isButtonEnabledForBase(action, currentBase, isProgrammerMode)) return

        performAdvancedFeedback(context, view, settings, action)

        // Refocus to preserve cursor position
        try { focusRequester.requestFocus() } catch (_: Exception) {}

        if (expressionValue.text == "Err") {
            scope.launch {
                typeOffsetX.snapTo(40f)
                typeOffsetX.animateTo(0f, spring(dampingRatio = 0.7f, stiffness = 600f))
            }
            expressionValue = TextFieldValue("")
        }

        val currentText = expressionValue.text
        val selection = expressionValue.selection

        fun insert(str: String) {
            scope.launch {
                typeOffsetX.snapTo(40f) // Sağa doğru kaydır (yeni gelen sayının illüzyonu)
                typeOffsetX.animateTo(0f, spring(dampingRatio = 0.7f, stiffness = 600f))
            }
            val newText = currentText.replaceRange(selection.start, selection.end, str)
            val newCursorPos = selection.start + str.length
            expressionValue = TextFieldValue(text = newText, selection = TextRange(newCursorPos))
        }

        when (action) {
            "AC" -> {
                expressionValue = TextFieldValue("")
            }
            "⌫" -> {
                scope.launch {
                    typeOffsetX.snapTo(-30f) // Silmede sola doğru çekilme illüzyonu
                    typeOffsetX.animateTo(0f, spring(dampingRatio = 0.7f, stiffness = 600f))
                }
                if (selection.start != selection.end) {
                    val newText = currentText.replaceRange(selection.start, selection.end, "")
                    expressionValue = TextFieldValue(text = newText, selection = TextRange(selection.start))
                } else if (selection.start > 0) {
                    val newText = currentText.replaceRange(selection.start - 1, selection.start, "")
                    expressionValue = TextFieldValue(text = newText, selection = TextRange(selection.start - 1))
                }
            }
            "+/-" -> {
                val text = currentText.substring(0, selection.start)
                val numberMatch = Regex("([0-9a-fA-F]+\\.?[0-9a-fA-F]*)$").find(text)
                if (numberMatch != null) {
                    val num = numberMatch.value
                    val numStart = numberMatch.range.first
                    if (numStart >= 2 && text.substring(numStart - 2, numStart) == "(-") {
                        val newText = currentText.removeRange(numStart - 2, numStart)
                        expressionValue = TextFieldValue(text = newText, selection = TextRange(selection.start - 2))
                    } else {
                        val newText = currentText.substring(0, numStart) + "(-" + num + currentText.substring(selection.start)
                        expressionValue = TextFieldValue(text = newText, selection = TextRange(selection.start + 2))
                    }
                } else {
                    if (text.endsWith("(-")) {
                        val newText = currentText.removeRange(selection.start - 2, selection.start)
                        expressionValue = TextFieldValue(text = newText, selection = TextRange(selection.start - 2))
                    } else {
                        insert("(-")
                    }
                }
            }
            "=" -> {
                val strictRes = try {
                    evaluateStrict(expressionValue.text, base = baseInt)
                } catch (_: Exception) {
                    "Err"
                }
                if (strictRes.isNotEmpty() && strictRes != "Err") {
                    onSaveHistory(expressionValue.text, strictRes)
                    expressionValue = TextFieldValue(text = strictRes, selection = TextRange(strictRes.length))
                    calculatedResult = strictRes
                    dropTrigger++
                } else {
                    calculatedResult = "Err"
                    performAdvancedFeedback(context, view, settings)
                }
            }
            "x²" -> insert("^2")
            "x³" -> insert("^3")
            "AND" -> insert(" & ")
            "OR" -> insert(" | ")
            "XOR" -> insert(" XOR ")
            "NOT" -> insert("~")
            "<<" -> insert(" << ")
            ">>" -> insert(" >> ")
            "A", "B", "C", "D", "E", "F" -> insert(action)
            "HEX", "DEC", "OCT", "BIN" -> {
                if (action != currentBase && expressionValue.text.isNotEmpty()) {
                    try {
                        val currentEval = evaluateForBaseConversions(expressionValue.text, baseInt)
                        val newBaseInt = when (action) {
                            "HEX" -> 16
                            "OCT" -> 8
                            "BIN" -> 2
                            else -> 10
                        }
                        if (currentEval != null) {
                            val newStr = if (newBaseInt != 10 && currentEval.signum() < 0) {
                                val mask64 = BigInteger("FFFFFFFFFFFFFFFF", 16)
                                currentEval.and(mask64).toString(newBaseInt).uppercase()
                            } else {
                                currentEval.toString(newBaseInt).uppercase()
                            }
                            expressionValue = TextFieldValue(text = newStr, selection = TextRange(newStr.length))
                        }
                    } catch (_: Exception) {}
                }
                currentBase = action
            }
            "sin", "cos", "tan", "cot", "ln", "log", "√", "∛", "π", "e", "(" -> {
                var toInsert = ""
                if (selection.start > 0) {
                    val prevChar = currentText[selection.start - 1]
                    if (prevChar.isDigit() || prevChar == ')' || prevChar == 'π' || prevChar == 'e') {
                        toInsert += "*"
                    }
                }
                toInsert += action
                if (action in listOf("sin", "cos", "tan", "cot", "ln", "log", "√", "∛")) {
                    toInsert += "("
                }
                insert(toInsert)
            }
            ".", "," -> {
                if (currentBase != "DEC") return
                var i = selection.start - 1
                var hasDot = false
                while (i >= 0 && (currentText[i].isDigit() || currentText[i] == '.' || currentText[i] == ',')) {
                    if (currentText[i] == '.' || currentText[i] == ',') hasDot = true
                    i--
                }
                var j = selection.start
                while (j < currentText.length && (currentText[j].isDigit() || currentText[j] == '.' || currentText[j] == ',')) {
                    if (currentText[j] == '.' || currentText[j] == ',') hasDot = true
                    j++
                }
                if (!hasDot) insert(".")
            }
            "+", "-", "*", "÷", "^", "%" -> {
                val start = selection.start
                val end = selection.end
                if (start == end && start > 0) {
                    val prevChar = currentText[start - 1]
                    val isOp = { c: Char -> c == '+' || c == '-' || c == '*' || c == '÷' || c == '^' || c == '%' }
                    if (isOp(prevChar)) {
                        if (action == "-" || action == "+") {
                            if (prevChar == '*' || prevChar == '÷' || prevChar == '^' || prevChar == '%') {
                                insert(action)
                            } else {
                                scope.launch {
                                    typeOffsetX.snapTo(40f)
                                    typeOffsetX.animateTo(0f, spring(dampingRatio = 0.7f, stiffness = 600f))
                                }
                                val newText = currentText.replaceRange(start - 1, start, action)
                                expressionValue = TextFieldValue(text = newText, selection = TextRange(start))
                            }
                        } else {
                            var opStart = start - 1
                            while (opStart > 0 && isOp(currentText[opStart - 1])) {
                                opStart--
                            }
                            scope.launch {
                                typeOffsetX.snapTo(40f)
                                typeOffsetX.animateTo(0f, spring(dampingRatio = 0.7f, stiffness = 600f))
                            }
                            val newText = currentText.replaceRange(opStart, start, action)
                            expressionValue = TextFieldValue(text = newText, selection = TextRange(opStart + action.length))
                        }
                    } else {
                        insert(action)
                    }
                } else {
                    insert(action)
                }
            }
            else -> insert(action)
        }
    }

    val topScrollState = rememberScrollState()

    LaunchedEffect(calculatedResult) {
        topScrollState.animateScrollTo(topScrollState.maxValue)
    }

    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

    @Composable
    fun ColumnScope.TopSection() {
        Row(
            modifier = Modifier.fillMaxWidth().weight(1f),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Row {
                Column {
                    IconButton(onClick = onOpenHistory) {
                        Icon(androidx.compose.material.icons.Icons.Filled.History, contentDescription = "History", tint = textColor)
                    }
                    IconButton(onClick = onOpenCas) {
                        Icon(androidx.compose.material.icons.Icons.Filled.Functions, contentDescription = "CAS", tint = textColor)
                    }
                }
                IconButton(onClick = onOpenConverter) {
                    Icon(androidx.compose.material.icons.Icons.Filled.SwapHoriz, contentDescription = "Converter", tint = textColor)
                }
                IconButton(onClick = onOpenRuler) {
                    Icon(androidx.compose.material.icons.Icons.Filled.Straighten, contentDescription = "Ruler", tint = textColor)
                }
                IconButton(onClick = onOpenDaily) {
                    Icon(androidx.compose.material.icons.Icons.Filled.Calculate, contentDescription = "Tools", tint = textColor)
                }
                IconButton(onClick = onOpenGraphing) {
                    Icon(androidx.compose.material.icons.Icons.AutoMirrored.Filled.ShowChart, contentDescription = "Graph", tint = textColor)
                }
            }
            Row {
                val ctx = LocalContext.current
                IconButton(onClick = {
                    if (Settings.canDrawOverlays(ctx)) {
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                            ctx.startForegroundService(Intent(ctx, FloatingService::class.java))
                        } else {
                            ctx.startService(Intent(ctx, FloatingService::class.java))
                        }
                        (ctx as? android.app.Activity)?.moveTaskToBack(true)
                    } else {
                        val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${ctx.packageName}"))
                        ctx.startActivity(intent)
                    }
                }) {
                    Icon(Icons.Filled.PictureInPicture, contentDescription = "Floating Window", tint = textColor)
                }
                IconButton(onClick = onOpenSettings) {
                    Icon(Icons.Filled.Settings, contentDescription = "Settings", tint = textColor)
                }
            }
        }

        val isSimpleNumber = expression.all { it.isDigit() || it == '.' || (baseInt == 16 && it.uppercaseChar() in 'A'..'F') }
        AnimatedVisibility(
            visible = (calculatedResult.isNotEmpty() && !isSimpleNumber && calculatedResult != expression) || isCalculating,
            enter = fadeIn(tween(200)) + slideInVertically { -20 },
            exit = fadeOut(tween(200)) + slideOutVertically { -20 }
        ) {
            if (isCalculating) {
                androidx.compose.material3.CircularProgressIndicator(
                    modifier = Modifier.size(24.dp).padding(end = 16.dp),
                    color = textColor,
                    strokeWidth = 2.dp
                )
            } else {
                val animatedResultFont by animateFloatAsState(
                    targetValue = if (isProgrammerMode) 24f else 32f,
                    animationSpec = spring(dampingRatio = 0.8f, stiffness = 380f),
                    label = "resultFont"
                )
                Text(
                    text = calculatedResult,
                    fontSize = animatedResultFont.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (dynamicColorScheme != null && settings.displayTextColor == null) dynamicColorScheme.onSurfaceVariant else textColor.copy(alpha = 0.7f),
                    textAlign = TextAlign.End,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 4.dp)
                        .horizontalScroll(topScrollState)
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onLongPress = {
                                    val clip = ClipData.newPlainText("CalculatorApp", calculatedResult)
                                    androidClipboard.setPrimaryClip(clip)
                                    Toast.makeText(context, Translator.tr("Result Copied"), Toast.LENGTH_SHORT).show()
                                    performAdvancedFeedback(context, view, settings)
                                }
                            )
                        },
                    maxLines = 1
                )
            }
        }

        var textLayoutResult by remember { mutableStateOf<androidx.compose.ui.text.TextLayoutResult?>(null) }
        val infiniteTransition = rememberInfiniteTransition()
        val cursorAlpha by infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = 0f,
            animationSpec = infiniteRepeatable(
                animation = keyframes {
                    durationMillis = 1000
                    1f at 0
                    1f at 500
                    0f at 501
                    0f at 1000
                },
                repeatMode = RepeatMode.Restart
            ),
            label = "cursorAlpha"
        )

        val textMeasurer = rememberTextMeasurer()
        val animatedBaseFont by animateFloatAsState(
            targetValue = if (isProgrammerMode) 44f else 64f,
            animationSpec = spring(dampingRatio = 0.8f, stiffness = 380f),
            label = "baseFont"
        )
        val animatedMaxHeight by animateDpAsState(
            targetValue = if (isProgrammerMode) 110.dp else 220.dp,
            animationSpec = spring(dampingRatio = 0.8f, stiffness = 380f),
            label = "displayMaxHeight"
        )
        val animatedBottomPadding by animateDpAsState(
            targetValue = if (isProgrammerMode) 6.dp else 12.dp,
            animationSpec = spring(dampingRatio = 0.8f, stiffness = 380f),
            label = "displayBottomPadding"
        )
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = animatedMaxHeight)
                .padding(bottom = animatedBottomPadding)
                .animateContentSize(animationSpec = spring(dampingRatio = 0.8f, stiffness = 380f))
        ) {
            val maxW = constraints.maxWidth
            val baseStyle = TextStyle(
                fontSize = animatedBaseFont.sp,
                fontWeight = FontWeight.Bold,
                color = textColor,
                textAlign = TextAlign.End
            )

            val measuredText = remember(expressionValue.text, maxW, animatedBaseFont) {
                textMeasurer.measure(
                    text = expressionValue.text.ifEmpty { "0" },
                    style = baseStyle,
                    maxLines = 1,
                    softWrap = false
                )
            }

            val targetFontSize = if (measuredText.size.width > maxW && maxW > 0) {
                val safeWidth = (maxW - 20).coerceAtLeast(1)
                val scale = safeWidth.toFloat() / measuredText.size.width.toFloat()
                (animatedBaseFont * scale).coerceAtLeast(22f).sp
            } else {
                animatedBaseFont.sp
            }

            BasicTextField(
                value = expressionValue,
                onValueChange = { newValue ->
                    expressionValue = newValue
                },
                onTextLayout = { textLayoutResult = it },
                textStyle = TextStyle(
                    fontSize = targetFontSize,
                    fontWeight = FontWeight.Bold,
                    color = textColor,
                    textAlign = TextAlign.End
                ),
                readOnly = true,
                singleLine = false,
                maxLines = if (isProgrammerMode) 2 else 4,
                decorationBox = { innerTextField ->
                    if (expressionValue.text.isEmpty()) {
                        Text("0", fontSize = targetFontSize, fontWeight = FontWeight.Bold, color = textColor, textAlign = TextAlign.End)
                    } else {
                        innerTextField()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(x = typeOffsetX.value.dp, y = textYOffset.value.dp)
                    .alpha(textAlpha.value)
                    .animateContentSize(animationSpec = spring(dampingRatio = 0.7f, stiffness = 400f))
                    .verticalScroll(rememberScrollState())
                    .focusRequester(focusRequester)
                    .onKeyEvent { event ->
                        if (event.type == KeyEventType.KeyDown) {
                            val char = event.utf16CodePoint.toChar()
                            when {
                                char.isDigit() || char.uppercaseChar() in 'A'..'F' || char in setOf('+', '-', '*', '/', '^', '%', '!', '.', ',', '(', ')') -> {
                                    val action = if (char == '/') "÷" else char.uppercaseChar().toString()
                                    onAction(action)
                                    true
                                }
                                event.key == Key.Enter || event.key == Key.NumPadEnter || event.key == Key.Equals -> {
                                    onAction("=")
                                    true
                                }
                                event.key == Key.Backspace -> {
                                    onAction("⌫")
                                    true
                                }
                                event.key == Key.Delete || event.key == Key.Escape -> {
                                    onAction("AC")
                                    true
                                }
                                else -> false
                            }
                        } else false
                    }
                    .drawWithContent {
                        drawContent()
                        if (expressionValue.selection.start == expressionValue.selection.end) {
                            textLayoutResult?.let { layout ->
                                try {
                                    val rect = layout.getCursorRect(expressionValue.selection.start)
                                    drawLine(
                                        color = textColor.copy(alpha = cursorAlpha),
                                        start = Offset(rect.left, rect.top),
                                        end = Offset(rect.left, rect.bottom),
                                        strokeWidth = 2.dp.toPx()
                                    )
                                } catch (_: Exception) {}
                            }
                        }
                    }
            )
        }

        // Live Base Conversion Panel in Programmer Mode
        AnimatedVisibility(
            visible = isProgrammerMode,
            enter = fadeIn(tween(220)) + expandVertically(animationSpec = spring(dampingRatio = 0.8f, stiffness = 380f)),
            exit = fadeOut(tween(180)) + shrinkVertically(animationSpec = spring(dampingRatio = 0.8f, stiffness = 380f))
        ) {
            val baseItems = listOf(
                "HEX" to 16,
                "DEC" to 10,
                "OCT" to 8,
                "BIN" to 2
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(textColor.copy(alpha = 0.06f))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                baseItems.forEach { (baseLabel, radix) ->
                    val isSelected = currentBase == baseLabel
                    val formattedVal = remember(liveBigInt, radix) {
                        liveBigInt?.let { formatGroupedBaseString(it, radix) } ?: "—"
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                if (isSelected) {
                                    if (dynamicColorScheme != null) dynamicColorScheme.primaryContainer.copy(alpha = 0.5f) else effectivePrimary.copy(alpha = 0.16f)
                                } else Color.Transparent
                            )
                            .clickable { onAction(baseLabel) }
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .height(14.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(if (isSelected) effectivePrimary else Color.Transparent)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = baseLabel,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) effectivePrimary else if (dynamicColorScheme != null && settings.displayTextColor == null) dynamicColorScheme.onSurfaceVariant else textColor.copy(alpha = 0.6f),
                            modifier = Modifier.width(34.dp)
                        )
                        Text(
                            text = formattedVal,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) textColor else textColor.copy(alpha = 0.8f),
                            maxLines = 1,
                            modifier = Modifier
                                .weight(1f)
                                .horizontalScroll(rememberScrollState()),
                            textAlign = TextAlign.End
                        )
                    }
                }
            }
        }
    }

    @Composable
    fun ColumnScope.KeypadSection() {
        val modeLabels = listOf(
            Translator.tr("Bilimsel"),
            Translator.tr("Standart"),
            Translator.tr("Programcı")
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            modeLabels.forEachIndexed { index, label ->
                val isSelected = pagerState.targetPage == index
                val tabBgColor by animateColorAsState(
                    targetValue = if (isSelected) {
                        if (dynamicColorScheme != null) dynamicColorScheme.primaryContainer else effectivePrimary.copy(alpha = 0.2f)
                    } else {
                        if (dynamicColorScheme != null) dynamicColorScheme.surfaceVariant.copy(alpha = 0.35f) else textColor.copy(alpha = 0.06f)
                    },
                    animationSpec = tween(220),
                    label = "tabBg"
                )
                val tabTextColor by animateColorAsState(
                    targetValue = if (isSelected) {
                        if (dynamicColorScheme != null) dynamicColorScheme.onPrimaryContainer else effectivePrimary
                    } else {
                        if (dynamicColorScheme != null && settings.displayTextColor == null) dynamicColorScheme.onSurfaceVariant else textColor.copy(alpha = 0.55f)
                    },
                    animationSpec = tween(220),
                    label = "tabText"
                )
                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(tabBgColor)
                        .clickable {
                            scope.launch { pagerState.animateScrollToPage(index) }
                        }
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = label,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = tabTextColor
                    )
                }
            }
        }

        HorizontalPager(
            state = pagerState,
            modifier = if (isLandscape) {
                Modifier.fillMaxWidth().weight(1f)
            } else {
                Modifier.fillMaxWidth().animateContentSize(animationSpec = spring(dampingRatio = 0.82f, stiffness = 380f))
            }
        ) { page ->
            val buttons = if (page == 1) {
                listOf(
                    listOf("AC", "⌫", "%", "÷"),
                    listOf("7", "8", "9", "*"),
                    listOf("4", "5", "6", "-"),
                    listOf("1", "2", "3", "+"),
                    listOf("+/-", "0", ".", "=")
                )
            } else if (page == 0) {
                listOf(
                    listOf("sin", "cos", "tan", "cot"),
                    listOf("ln", "log", "√", "∛"),
                    listOf("x²", "x³", "^", "!"),
                    listOf("π", "e", "(", ")"),
                    listOf("AC", "⌫", ".", "=")
                )
            } else {
                // Full Programmer Keypad: Bitwise top row + 5x5 Hex & Decimal Numpad
                listOf(
                    listOf("AND", "OR", "XOR", "NOT", "<<", ">>"),
                    listOf("A", "B", "AC", "⌫", "÷"),
                    listOf("C", "7", "8", "9", "*"),
                    listOf("D", "4", "5", "6", "-"),
                    listOf("E", "1", "2", "3", "+"),
                    listOf("F", "(", "0", ")", "=")
                )
            }

            val isProgPage = (page == 2)
            val rowSpacing = if (isProgPage) 6.dp else 12.dp

            Column(
                modifier = (if (isLandscape) Modifier.fillMaxSize() else Modifier.fillMaxWidth())
                    .graphicsLayer {
                        val pageOffset = abs((pagerState.currentPage - page) + pagerState.currentPageOffsetFraction).coerceIn(0f, 1f)
                        alpha = 1f - (pageOffset * 0.35f)
                        val s = 1f - (pageOffset * 0.05f)
                        scaleX = s
                        scaleY = s
                    }
            ) {
                buttons.forEachIndexed { rowIndex, row ->
                    val isBitwiseRow = isProgPage && rowIndex == 0
                    Row(
                        modifier = if (isLandscape) {
                            Modifier.fillMaxWidth().weight(if (isBitwiseRow) 0.75f else 1f).padding(bottom = rowSpacing)
                        } else {
                            Modifier.fillMaxWidth().padding(bottom = rowSpacing)
                        },
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        row.forEach { btn ->
                            val enabled = isButtonEnabledForBase(btn, currentBase, isProgPage)
                            val btnModifier = if (isLandscape) {
                                Modifier.weight(1f).fillMaxHeight().padding(3.dp)
                            } else if (isBitwiseRow) {
                                Modifier.weight(1f).height(40.dp).padding(horizontal = 3.dp, vertical = 2.dp)
                            } else if (isProgPage) {
                                Modifier.weight(1f).aspectRatio(1.22f).padding(3.dp)
                            } else {
                                Modifier.weight(1f).aspectRatio(1f).padding(4.dp)
                            }
                            if (btn.isEmpty()) {
                                Spacer(modifier = btnModifier)
                            } else {
                                CalculatorButton(
                                    symbol = btn,
                                    currentTheme = settings.theme,
                                    isActive = (btn == currentBase),
                                    enabled = enabled,
                                    isCompact = isProgPage,
                                    dynamicColorScheme = dynamicColorScheme,
                                    customPrimary = settings.primaryColor?.let { Color(it) },
                                    customSecondary = effectiveSecondary,
                                    customNumberKey = effectiveNumber,
                                    customKeyTextColor = customKeyTextColor,
                                    modifier = btnModifier,
                                    onClick = { onAction(btn) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (isLandscape) {
        Row(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Column(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                TopSection()
            }
            Column(
                modifier = Modifier.weight(1.2f).fillMaxHeight(),
                verticalArrangement = Arrangement.Bottom
            ) {
                KeypadSection()
            }
        }
    } else {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.Bottom
        ) {
            TopSection()
            KeypadSection()
        }
    }
}

@Composable
fun CalculatorButton(
    symbol: String,
    currentTheme: AppTheme,
    isActive: Boolean = false,
    enabled: Boolean = true,
    isCompact: Boolean = false,
    dynamicColorScheme: ColorScheme? = null,
    customPrimary: Color? = null,
    customSecondary: Color? = null,
    customNumberKey: Color? = null,
    customKeyTextColor: Color? = null,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val isSystemDark = androidx.compose.foundation.isSystemInDarkTheme()
    val resolvedTheme = when (currentTheme) {
        AppTheme.SYSTEM -> if (isSystemDark) AppTheme.AMOLED else AppTheme.LIGHT
        AppTheme.MATERIAL_YOU -> if (isSystemDark) AppTheme.DARK else AppTheme.LIGHT
        else -> currentTheme
    }
    val isLight = resolvedTheme == AppTheme.LIGHT
    val isOperator = symbol in setOf("=", "+", "-", "*", "÷", "%", "^", "!")
    val isBitwise = symbol in setOf("AND", "OR", "XOR", "NOT", "<<", ">>")
    val isHexLetter = symbol in setOf("A", "B", "C", "D", "E", "F")
    val isScientific = symbol in setOf("sin", "cos", "tan", "cot", "π", "e", "x²", "x³", "√", "∛", "ln", "log", "(", ")")
    val isClearOrBack = symbol == "AC" || symbol == "⌫"
    val isEquals = symbol == "="

    val (backgroundColor, defaultTextColor) = when {
        // Active programmer mode base indicator
        isActive -> {
            val bg = customPrimary ?: dynamicColorScheme?.primary ?: AccentOrange
            val fg = if (customPrimary != null) bg.contrastTextColor() else (dynamicColorScheme?.onPrimary ?: Color.White)
            bg to fg
        }
        // Equals button (=)
        isEquals -> {
            val bg = customPrimary ?: dynamicColorScheme?.primary ?: AccentOrange
            val fg = if (customPrimary != null) bg.contrastTextColor() else (dynamicColorScheme?.onPrimary ?: Color.White)
            bg to fg
        }
        // Operators (+, -, *, ÷, %, ^, !)
        isOperator -> {
            if (customPrimary != null) {
                customPrimary to customPrimary.contrastTextColor()
            } else if (dynamicColorScheme != null) {
                dynamicColorScheme.primaryContainer to dynamicColorScheme.onPrimaryContainer
            } else {
                AccentOrange to Color.White
            }
        }
        // Clear / Backspace (AC, ⌫)
        isClearOrBack -> {
            if (customSecondary != null) {
                customSecondary to customSecondary.contrastTextColor()
            } else if (dynamicColorScheme != null) {
                dynamicColorScheme.secondaryContainer to dynamicColorScheme.onSecondaryContainer
            } else {
                (if (isLight) Color(0xFFCFCFD4) else Color(0xFFA5A5A5)) to Color.Black
            }
        }
        // Scientific & Bitwise
        isScientific || isBitwise -> {
            if (customSecondary != null) {
                customSecondary to customSecondary.contrastTextColor()
            } else if (dynamicColorScheme != null) {
                dynamicColorScheme.tertiaryContainer to dynamicColorScheme.onTertiaryContainer
            } else {
                (if (isLight) Color(0xFFE1E1E6) else Color(0xFF4A4A4A)) to (if (isLight) Color.Black else Color.White)
            }
        }
        // Hex letters (A-F)
        isHexLetter -> {
            if (customSecondary != null) {
                customSecondary to customSecondary.contrastTextColor()
            } else if (dynamicColorScheme != null) {
                dynamicColorScheme.tertiaryContainer to dynamicColorScheme.primary
            } else {
                (if (isLight) Color(0xFFE1E1E6) else Color(0xFF4A4A4A)) to (customPrimary ?: AccentOrange)
            }
        }
        // Number keys (0-9, ., +/-)
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

    val finalTextColor = customKeyTextColor ?: defaultTextColor

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.85f else 1.0f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 800f),
        label = "scale"
    )

    val shape = if (isCompact) RoundedCornerShape(18.dp) else CircleShape
    val fontSize = when {
        symbol.length >= 3 -> if (isCompact) 13.sp else 18.sp
        symbol.length == 2 -> if (isCompact) 16.sp else 22.sp
        else -> if (isCompact) 22.sp else 30.sp
    }

    val animatedAlpha by animateFloatAsState(
        targetValue = if (enabled) 1f else 0.25f,
        animationSpec = tween(180),
        label = "btnAlpha"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .scale(scale)
            .alpha(animatedAlpha)
            .clip(shape)
            .background(backgroundColor)
            .then(
                if (isLight && !isOperator && !isActive && dynamicColorScheme == null) Modifier.border(1.dp, Color(0x1A000000), shape)
                else Modifier
            )
            .then(
                if (enabled) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = LocalIndication.current
                    ) { onClick() }
                } else Modifier
            )
    ) {
        Text(
            text = symbol,
            fontSize = fontSize,
            fontWeight = if (isHexLetter) FontWeight.Bold else FontWeight.Medium,
            color = finalTextColor,
            maxLines = 1
        )
    }
}