package com.magic.calcucator

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.magic.calcucator.core.*

class CalculatorWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onAppWidgetOptionsChanged(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int, newOptions: android.os.Bundle) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions)
        updateAppWidget(context, appWidgetManager, appWidgetId)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_CLICK) {
            val btn = intent.getStringExtra(EXTRA_BTN) ?: return
            
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            var expr = prefs.getString(PREF_EXPR, "") ?: ""
            
            when (btn) {
                "C" -> expr = ""
                "DEL" -> if (expr.isNotEmpty()) expr = expr.dropLast(1)
                "=" -> {
                    val res = evaluateStrict(expr)
                    if (res.isNotEmpty() && res != "Err") expr = res
                }
                else -> expr += btn
            }
            
            val result = if (expr.isNotEmpty()) safeEvaluate(expr) else "0"
            
            prefs.edit().putString(PREF_EXPR, expr).apply()
            
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, CalculatorWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            
            for (appWidgetId in appWidgetIds) {
                updateAppWidget(context, appWidgetManager, appWidgetId, expr, result)
            }
        }
    }

    companion object {
        const val ACTION_CLICK = "com.magic.calcucator.WIDGET_CLICK"
        const val EXTRA_BTN = "EXTRA_BTN"
        const val PREFS_NAME = "WidgetPrefs"
        const val PREF_EXPR = "widget_expr"

        fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int, currentExpr: String? = null, currentResult: String? = null) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val expr = currentExpr ?: prefs.getString(PREF_EXPR, "") ?: ""
            val result = currentResult ?: if (expr.isNotEmpty()) safeEvaluate(expr) else "0"

                        val options = appWidgetManager.getAppWidgetOptions(appWidgetId)
            val minWidth = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 110)
            val minHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 110)
            val layoutId = if (minWidth >= 220 || minHeight >= 220) R.layout.widget_calculator else R.layout.widget_calculator_compact
            val views = RemoteViews(context.packageName, layoutId)
            views.setTextViewText(R.id.tv_widget_display, expr)
            views.setTextViewText(R.id.tv_widget_result, result)

            val buttons = mapOf(
                R.id.btn_0 to "0", R.id.btn_1 to "1", R.id.btn_2 to "2", R.id.btn_3 to "3",
                R.id.btn_4 to "4", R.id.btn_5 to "5", R.id.btn_6 to "6", R.id.btn_7 to "7",
                R.id.btn_8 to "8", R.id.btn_9 to "9", R.id.btn_dot to ".",
                R.id.btn_add to "+", R.id.btn_sub to "-", R.id.btn_mul to "×", R.id.btn_div to "÷",
                R.id.btn_c to "C", R.id.btn_del to "DEL", R.id.btn_eq to "="
            )

            for ((viewId, btnVal) in buttons) {
                val intent = Intent(context, CalculatorWidgetProvider::class.java).apply {
                    action = ACTION_CLICK
                    putExtra(EXTRA_BTN, btnVal)
                }
                val pendingIntent = PendingIntent.getBroadcast(
                    context, viewId, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                views.setOnClickPendingIntent(viewId, pendingIntent)
            }

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }
}
