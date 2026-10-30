# ===================================================================
# Comprehensive ProGuard / R8 Rules for Magic Calculator
# ===================================================================

# Disable aggressive R8 bytecode mutating optimizations that cause runtime breakages
-dontoptimize

# -------------------------------------------------------------------
# 1. Application Code (Preserve All App Classes, Enums & Members)
# -------------------------------------------------------------------
-keep class com.magic.calculator.** { *; }
-keepclassmembers class com.magic.calculator.** { *; }
-keepclassmembernames class com.magic.calculator.** { *; }
-dontwarn com.magic.calculator.**

# -------------------------------------------------------------------
# 2. Enums (Crucial: Prevent Enum Unboxing & Name Stripping for SharedPreferences)
# -------------------------------------------------------------------
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
    **[] $VALUES;
    public *;
}
-keep enum * { *; }

# -------------------------------------------------------------------
# 3. Android Framework, Activities, Services & Widgets
# -------------------------------------------------------------------
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.appwidget.AppWidgetProvider
-keep public class * extends android.view.View {
    public <init>(android.content.Context);
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
}
-keepclasseswithmembernames class * {
    native <methods>;
}
-keepclassmembers class * implements android.os.Parcelable {
    public static final ** CREATOR;
}
-keepclassmembers class **.R$* {
    public static <fields>;
}

# -------------------------------------------------------------------
# 4. Jetpack Compose & Kotlin Lambdas
# -------------------------------------------------------------------
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**
-keep class androidx.compose.runtime.** { *; }
-keepclassmembers class * extends kotlin.jvm.internal.Lambda { *; }

# -------------------------------------------------------------------
# 5. Symja Computer Algebra System & Mathematical Core
# -------------------------------------------------------------------
-keep class org.matheclipse.** { *; }
-keepclassmembers class org.matheclipse.** { *; }
-dontwarn org.matheclipse.**

-keep class org.hipparchus.** { *; }
-keepclassmembers class org.hipparchus.** { *; }
-dontwarn org.hipparchus.**

-keep class edu.jas.** { *; }
-keepclassmembers class edu.jas.** { *; }
-dontwarn edu.jas.**

-keep class org.apfloat.** { *; }
-keepclassmembers class org.apfloat.** { *; }
-dontwarn org.apfloat.**

-keep class org.logicng.** { *; }
-dontwarn org.logicng.**

-keep class org.chocosolver.** { *; }
-dontwarn org.chocosolver.**

-keep class uk.ac.ed.ph.snuggletex.** { *; }
-dontwarn uk.ac.ed.ph.snuggletex.**

-keep class com.duy.** { *; }
-dontwarn com.duy.**

-keep class org.apache.commons.** { *; }
-dontwarn org.apache.commons.**

-keep class org.apache.logging.** { *; }
-dontwarn org.apache.logging.**
-dontwarn org.apache.logging.log4j.**

# -------------------------------------------------------------------
# 6. Missing JVM / Desktop APIs from Transitive Dependencies
# -------------------------------------------------------------------
-dontwarn java.lang.management.**
-dontwarn javax.management.**
-dontwarn org.ehcache.**
-dontwarn org.terracotta.**
-dontwarn net.sf.ehcache.**
-dontwarn javax.lang.model.**
-dontwarn com.squareup.javapoet.**
-dontwarn org.osgi.framework.**
-dontwarn javax.annotation.**
-dontwarn javax.naming.**
-dontwarn javax.xml.**
-dontwarn org.codehaus.commons.**
-dontwarn com.google.errorprone.annotations.**
-dontwarn java.beans.**
-dontwarn java.awt.**
-dontwarn javax.swing.**
-dontwarn sun.misc.**

# -------------------------------------------------------------------
# 7. WebView JavaScript Interface Bridge (Steps & AI)
# -------------------------------------------------------------------
-keepattributes JavascriptInterface
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}
-keep class com.magic.calculator.core.WebAppInterface { *; }
-keepclassmembers class com.magic.calculator.core.WebAppInterface { *; }

# -------------------------------------------------------------------
# 8. Tesseract4Android (Offline OCR) & Image Cropper
# -------------------------------------------------------------------
-keep class com.googlecode.tesseract.android.** { *; }
-keep class cz.adaptech.tesseract4android.** { *; }
-dontwarn com.googlecode.tesseract.android.**
-dontwarn cz.adaptech.tesseract4android.**
-keepclasseswithmembernames class * {
    native <methods>;
}
-keep class com.canhub.cropper.** { *; }
-dontwarn com.canhub.cropper.**
-keep class com.vanniktech.** { *; }
-dontwarn com.vanniktech.**

# -------------------------------------------------------------------
# 9. Google Generative AI (Gemini SDK) & MediaPipe GenAI
# -------------------------------------------------------------------
-keep class com.google.ai.client.generativeai.** { *; }
-dontwarn com.google.ai.client.generativeai.**
-keep class com.google.mediapipe.** { *; }
-dontwarn com.google.mediapipe.**
-keep class com.google.protobuf.** { *; }
-dontwarn com.google.protobuf.**
-dontwarn okhttp3.**
-dontwarn okio.**

# -------------------------------------------------------------------
# 10. Kotlin Coroutines, Reflection & General Attributes
# -------------------------------------------------------------------
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod,SourceFile,LineNumberTable
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-dontwarn kotlinx.coroutines.**
-keep class kotlin.reflect.** { *; }
-dontwarn kotlin.reflect.**
