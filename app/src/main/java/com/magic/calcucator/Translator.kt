package com.magic.calcucator

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

object Translator {
        var currentLang by mutableStateOf(getDeviceLanguage())

    private fun getDeviceLanguage(): String {
        return when (java.util.Locale.getDefault().language) {
            "tr" -> "Türkçe"
            "en" -> "English"
            "es" -> "Español"
            "de" -> "Deutsch"
            "fr" -> "Français"
            "ru" -> "Русский"
            "ar" -> "العربية"
            "zh" -> "中文"
            "ja" -> "日本語"
            else -> "English"
        }
    }

    val supportedLanguages = listOf(
        "Türkçe", "English", "Español", "Deutsch", "Français", "Русский", "العربية", "中文", "日本語"
    )

    private val dictionary = mapOf(
        "No internet connection or API error." to mapOf(
            "Türkçe" to "İnternet bağlantısı yok veya API hatası.",
            "English" to "No internet connection or API error.",
            "Español" to "Sin conexión a internet o error de API.",
            "Deutsch" to "Keine Internetverbindung oder API-Fehler.",
            "Français" to "Pas de connexion internet ou erreur d'API.",
            "Русский" to "Нет подключения к интернету или ошибка API.",
            "العربية" to "لا يوجد اتصال بالإنترنت أو خطأ في API.",
            "中文" to "无网络连接或API错误。",
            "日本語" to "インターネット接続がないかAPIエラーです。"
        ),
        "Enter a valid amount" to mapOf(
            "Türkçe" to "Geçerli bir miktar girin",
            "English" to "Enter a valid amount",
            "Español" to "Ingrese una cantidad válida",
            "Deutsch" to "Geben Sie einen gültigen Betrag ein",
            "Français" to "Entrez un montant valide",
            "Русский" to "Введите действительную сумму",
            "العربية" to "أدخل مبلغا صحيحا",
            "中文" to "请输入有效金额",
            "日本語" to "有効な金額を入力してください"
        ),

        "Calculate from Tax Inclusive" to mapOf(
            "Türkçe" to "Vergi Dahil Hesaptan",
            "English" to "Calculate from Tax Inclusive",
            "Español" to "Calcular con impuestos incluidos",
            "Deutsch" to "Mit Steuern berechnen",
            "Français" to "Calculer TTC",
            "Русский" to "Рассчитать с налогом",
            "العربية" to "حساب من الشامل للضريبة",
            "中文" to "含税计算",
            "日本語" to "税込みから計算"
        ),
        "Calculate from Tax Exclusive" to mapOf(
            "Türkçe" to "Vergi Hariç Hesaptan",
            "English" to "Calculate from Tax Exclusive",
            "Español" to "Calcular sin impuestos",
            "Deutsch" to "Ohne Steuern berechnen",
            "Français" to "Calculer HT",
            "Русский" to "Рассчитать без налога",
            "العربية" to "حساب من غير الشامل للضريبة",
            "中文" to "不含税计算",
            "日本語" to "税抜きから計算"
        ),
        "Calculating..." to mapOf(
            "Türkçe" to "Hesaplanıyor...",
            "English" to "Calculating...",
            "Español" to "Calculando...",
            "Deutsch" to "Berechnung...",
            "Français" to "Calcul en cours...",
            "Русский" to "Вычисление...",
            "العربية" to "جاري الحساب...",
            "中文" to "计算中...",
            "日本語" to "計算中..."
        ),

        "Length" to mapOf(
            "Türkçe" to "Uzunluk",
            "English" to "Length",
            "Español" to "Longitud",
            "Deutsch" to "Länge",
            "Français" to "Longueur",
            "Русский" to "Длина",
            "العربية" to "الطول",
            "中文" to "长度",
            "日本語" to "長さ"
        ),
        "Weight" to mapOf(
            "Türkçe" to "Ağırlık",
            "English" to "Weight",
            "Español" to "Peso",
            "Deutsch" to "Gewicht",
            "Français" to "Poids",
            "Русский" to "Вес",
            "العربية" to "الوزن",
            "中文" to "重量",
            "日本語" to "重量"
        ),
        "Temperature" to mapOf(
            "Türkçe" to "Sıcaklık",
            "English" to "Temperature",
            "Español" to "Temperatura",
            "Deutsch" to "Temperatur",
            "Français" to "Température",
            "Русский" to "Температура",
            "العربية" to "درجة الحرارة",
            "中文" to "温度",
            "日本語" to "温度"
        ),
        "Area" to mapOf(
            "Türkçe" to "Alan",
            "English" to "Area",
            "Español" to "Área",
            "Deutsch" to "Fläche",
            "Français" to "Surface",
            "Русский" to "Площадь",
            "العربية" to "المساحة",
            "中文" to "面积",
            "日本語" to "面積"
        ),
        "Volume" to mapOf(
            "Türkçe" to "Hacim",
            "English" to "Volume",
            "Español" to "Volumen",
            "Deutsch" to "Volumen",
            "Français" to "Volume",
            "Русский" to "Объем",
            "العربية" to "الحجم",
            "中文" to "体积",
            "日本語" to "体積"
        ),
        "Speed" to mapOf(
            "Türkçe" to "Hız",
            "English" to "Speed",
            "Español" to "Velocidad",
            "Deutsch" to "Geschwindigkeit",
            "Français" to "Vitesse",
            "Русский" to "Скорость",
            "العربية" to "السرعة",
            "中文" to "速度",
            "日本語" to "速度"
        ),
        "Time" to mapOf(
            "Türkçe" to "Zaman",
            "English" to "Time",
            "Español" to "Tiempo",
            "Deutsch" to "Zeit",
            "Français" to "Temps",
            "Русский" to "Время",
            "العربية" to "الوقت",
            "中文" to "时间",
            "日本語" to "時間"
        ),
        "Data" to mapOf(
            "Türkçe" to "Veri",
            "English" to "Data",
            "Español" to "Datos",
            "Deutsch" to "Daten",
            "Français" to "Données",
            "Русский" to "Данные",
            "العربية" to "البيانات",
            "中文" to "数据",
            "日本語" to "データ"
        ),
        "Angle" to mapOf(
            "Türkçe" to "Açı",
            "English" to "Angle",
            "Español" to "Ángulo",
            "Deutsch" to "Winkel",
            "Français" to "Angle",
            "Русский" to "Угол",
            "العربية" to "الزاوية",
            "中文" to "角度",
            "日本語" to "角度"
        ),
        "Milimetre" to mapOf(
            "Türkçe" to "Milimetre",
            "English" to "Millimeter",
            "Español" to "Milímetro",
            "Deutsch" to "Millimeter",
            "Français" to "Millimètre",
            "Русский" to "Миллиметр",
            "العربية" to "مليمتر",
            "中文" to "毫米",
            "日本語" to "ミリメートル"
        ),
        "Santimetre" to mapOf(
            "Türkçe" to "Santimetre",
            "English" to "Centimeter",
            "Español" to "Centímetro",
            "Deutsch" to "Zentimeter",
            "Français" to "Centimètre",
            "Русский" to "Сантиметр",
            "العربية" to "سنتيمتر",
            "中文" to "厘米",
            "日本語" to "センチメートル"
        ),
        "Metre" to mapOf(
            "Türkçe" to "Metre",
            "English" to "Meter",
            "Español" to "Metro",
            "Deutsch" to "Meter",
            "Français" to "Mètre",
            "Русский" to "Метр",
            "العربية" to "متر",
            "中文" to "米",
            "日本語" to "メートル"
        ),
        "Kilometre" to mapOf(
            "Türkçe" to "Kilometre",
            "English" to "Kilometer",
            "Español" to "Kilómetro",
            "Deutsch" to "Kilometer",
            "Français" to "Kilomètre",
            "Русский" to "Километр",
            "العربية" to "كيلومتر",
            "中文" to "千米",
            "日本語" to "キロメートル"
        ),
        "İnç" to mapOf(
            "Türkçe" to "İnç",
            "English" to "Inch",
            "Español" to "Pulgada",
            "Deutsch" to "Zoll",
            "Français" to "Pouce",
            "Русский" to "Дюйм",
            "العربية" to "بوصة",
            "中文" to "英寸",
            "日本語" to "インチ"
        ),
        "Fit" to mapOf(
            "Türkçe" to "Fit",
            "English" to "Foot",
            "Español" to "Pie",
            "Deutsch" to "Fuß",
            "Français" to "Pied",
            "Русский" to "Фут",
            "العربية" to "قدم",
            "中文" to "英尺",
            "日本語" to "フィート"
        ),
        "Yarda" to mapOf(
            "Türkçe" to "Yarda",
            "English" to "Yard",
            "Español" to "Yarda",
            "Deutsch" to "Yard",
            "Français" to "Yard",
            "Русский" to "Ярд",
            "العربية" to "ياردة",
            "中文" to "码",
            "日本語" to "ヤード"
        ),
        "Mil" to mapOf(
            "Türkçe" to "Mil",
            "English" to "Mile",
            "Español" to "Milla",
            "Deutsch" to "Meile",
            "Français" to "Mille",
            "Русский" to "Миля",
            "العربية" to "ميل",
            "中文" to "英里",
            "日本語" to "マイル"
        ),
        "Deniz mili" to mapOf(
            "Türkçe" to "Deniz mili",
            "English" to "Nautical mile",
            "Español" to "Milla náutica",
            "Deutsch" to "Seemeile",
            "Français" to "Mille marin",
            "Русский" to "Морская миля",
            "العربية" to "ميل بحري",
            "中文" to "海里",
            "日本語" to "海里"
        ),
        "Miligram" to mapOf(
            "Türkçe" to "Miligram",
            "English" to "Milligram",
            "Español" to "Miligramo",
            "Deutsch" to "Milligramm",
            "Français" to "Milligramme",
            "Русский" to "Миллиграмм",
            "العربية" to "مليغرام",
            "中文" to "毫克",
            "日本語" to "ミリグラム"
        ),
        "Gram" to mapOf(
            "Türkçe" to "Gram",
            "English" to "Gram",
            "Español" to "Gramo",
            "Deutsch" to "Gramm",
            "Français" to "Gramme",
            "Русский" to "Грамм",
            "العربية" to "غرام",
            "中文" to "克",
            "日本語" to "グラム"
        ),
        "Kilogram" to mapOf(
            "Türkçe" to "Kilogram",
            "English" to "Kilogram",
            "Español" to "Kilogramo",
            "Deutsch" to "Kilogramm",
            "Français" to "Kilogramme",
            "Русский" to "Килограмм",
            "العربية" to "كيلوغرام",
            "中文" to "千克",
            "日本語" to "キログラム"
        ),
        "Ton" to mapOf(
            "Türkçe" to "Ton",
            "English" to "Tonne",
            "Español" to "Tonelada",
            "Deutsch" to "Tonne",
            "Français" to "Tonne",
            "Русский" to "Тонна",
            "العربية" to "طن",
            "中文" to "吨",
            "日本語" to "トン"
        ),
        "Ons" to mapOf(
            "Türkçe" to "Ons",
            "English" to "Ounce",
            "Español" to "Onza",
            "Deutsch" to "Unze",
            "Français" to "Once",
            "Русский" to "Унция",
            "العربية" to "أوقية",
            "中文" to "盎司",
            "日本語" to "オンス"
        ),
        "Pound" to mapOf(
            "Türkçe" to "Pound",
            "English" to "Pound",
            "Español" to "Libra",
            "Deutsch" to "Pfund",
            "Français" to "Livre",
            "Русский" to "Фунт",
            "العربية" to "رطل",
            "中文" to "磅",
            "日本語" to "ポンド"
        ),
        "Karat" to mapOf(
            "Türkçe" to "Karat",
            "English" to "Carat",
            "Español" to "Quilate",
            "Deutsch" to "Karat",
            "Français" to "Carat",
            "Русский" to "Карат",
            "العربية" to "قيراط",
            "中文" to "克拉",
            "日本語" to "カラット"
        ),
        "Celsius" to mapOf(
            "Türkçe" to "Celsius",
            "English" to "Celsius",
            "Español" to "Celsius",
            "Deutsch" to "Celsius",
            "Français" to "Celsius",
            "Русский" to "Цельсия",
            "العربية" to "مئوية",
            "中文" to "摄氏度",
            "日本語" to "摂氏"
        ),
        "Fahrenheit" to mapOf(
            "Türkçe" to "Fahrenheit",
            "English" to "Fahrenheit",
            "Español" to "Fahrenheit",
            "Deutsch" to "Fahrenheit",
            "Français" to "Fahrenheit",
            "Русский" to "Фаренгейта",
            "العربية" to "فهرنهايت",
            "中文" to "华氏度",
            "日本語" to "華氏"
        ),
        "Kelvin" to mapOf(
            "Türkçe" to "Kelvin",
            "English" to "Kelvin",
            "Español" to "Kelvin",
            "Deutsch" to "Kelvin",
            "Français" to "Kelvin",
            "Русский" to "Кельвина",
            "العربية" to "كلفن",
            "中文" to "开尔文",
            "日本語" to "ケルビン"
        ),
        "Santimetrekare" to mapOf(
            "Türkçe" to "Santimetrekare",
            "English" to "Square cm",
            "Español" to "Cm cuadrado",
            "Deutsch" to "Quadratzentimeter",
            "Français" to "Cm carré",
            "Русский" to "Квадратный сантиметр",
            "العربية" to "سنتيمتر مربع",
            "中文" to "平方厘米",
            "日本語" to "平方センチメートル"
        ),
        "Metrekare" to mapOf(
            "Türkçe" to "Metrekare",
            "English" to "Square meter",
            "Español" to "Metro cuadrado",
            "Deutsch" to "Quadratmeter",
            "Français" to "Mètre carré",
            "Русский" to "Квадратный метр",
            "العربية" to "متر مربع",
            "中文" to "平方米",
            "日本語" to "平方メートル"
        ),
        "Dönüm" to mapOf(
            "Türkçe" to "Dönüm",
            "English" to "Decare",
            "Español" to "Decárea",
            "Deutsch" to "Dekar",
            "Français" to "Décare",
            "Русский" to "Декар",
            "العربية" to "دونم",
            "中文" to "公亩",
            "日本語" to "デカール"
        ),
        "Hektar" to mapOf(
            "Türkçe" to "Hektar",
            "English" to "Hectare",
            "Español" to "Hectárea",
            "Deutsch" to "Hektar",
            "Français" to "Hectare",
            "Русский" to "Гектар",
            "العربية" to "هكتار",
            "中文" to "公顷",
            "日本語" to "ヘクタール"
        ),
        "Kilometrekare" to mapOf(
            "Türkçe" to "Kilometrekare",
            "English" to "Square km",
            "Español" to "Km cuadrado",
            "Deutsch" to "Quadratkilometer",
            "Français" to "Km carré",
            "Русский" to "Квадратный километр",
            "العربية" to "كيلومتر مربع",
            "中文" to "平方千米",
            "日本語" to "平方キロメートル"
        ),
        "Fitkare" to mapOf(
            "Türkçe" to "Fitkare",
            "English" to "Square foot",
            "Español" to "Pie cuadrado",
            "Deutsch" to "Quadratfuß",
            "Français" to "Pied carré",
            "Русский" to "Квадратный фут",
            "العربية" to "قدم مربع",
            "中文" to "平方英尺",
            "日本語" to "平方フィート"
        ),
        "Akre" to mapOf(
            "Türkçe" to "Akre",
            "English" to "Acre",
            "Español" to "Acre",
            "Deutsch" to "Morgen",
            "Français" to "Acre",
            "Русский" to "Акр",
            "العربية" to "فدان",
            "中文" to "英亩",
            "日本語" to "エーカー"
        ),
        "Mililitre" to mapOf(
            "Türkçe" to "Mililitre",
            "English" to "Milliliter",
            "Español" to "Mililitro",
            "Deutsch" to "Milliliter",
            "Français" to "Millilitre",
            "Русский" to "Миллилитр",
            "العربية" to "مليلتر",
            "中文" to "毫升",
            "日本語" to "ミリリットル"
        ),
        "Litre" to mapOf(
            "Türkçe" to "Litre",
            "English" to "Liter",
            "Español" to "Litro",
            "Deutsch" to "Liter",
            "Français" to "Litre",
            "Русский" to "Литр",
            "العربية" to "لتر",
            "中文" to "升",
            "日本語" to "リットル"
        ),
        "Metreküp" to mapOf(
            "Türkçe" to "Metreküp",
            "English" to "Cubic meter",
            "Español" to "Metro cúbico",
            "Deutsch" to "Kubikmeter",
            "Français" to "Mètre cube",
            "Русский" to "Кубический метр",
            "العربية" to "متر مكعب",
            "中文" to "立方米",
            "日本語" to "立方メートル"
        ),
        "Çay kaşığı" to mapOf(
            "Türkçe" to "Çay kaşığı",
            "English" to "Teaspoon",
            "Español" to "Cucharadita",
            "Deutsch" to "Teelöffel",
            "Français" to "Cuillère à café",
            "Русский" to "Чайная ложка",
            "العربية" to "ملعقة شاي",
            "中文" to "茶匙",
            "日本語" to "小さじ"
        ),
        "Yemek kaşığı" to mapOf(
            "Türkçe" to "Yemek kaşığı",
            "English" to "Tablespoon",
            "Español" to "Cucharada",
            "Deutsch" to "Esslöffel",
            "Français" to "Cuillère à soupe",
            "Русский" to "Столовая ложка",
            "العربية" to "ملعقة طعام",
            "中文" to "汤匙",
            "日本語" to "大さじ"
        ),
        "Su bardağı (200 mL)" to mapOf(
            "Türkçe" to "Su bardağı (200 mL)",
            "English" to "Cup (200 mL)",
            "Español" to "Vaso (200 mL)",
            "Deutsch" to "Tasse (200 mL)",
            "Français" to "Verre (200 mL)",
            "Русский" to "Стакан (200 мл)",
            "العربية" to "كوب (200 مل)",
            "中文" to "杯 (200 mL)",
            "日本語" to "カップ (200 mL)"
        ),
        "Galon (ABD)" to mapOf(
            "Türkçe" to "Galon (ABD)",
            "English" to "Gallon (US)",
            "Español" to "Galón (US)",
            "Deutsch" to "Gallone (US)",
            "Français" to "Gallon (US)",
            "Русский" to "Галлон (США)",
            "العربية" to "جالون (أمريكي)",
            "中文" to "加仑 (美)",
            "日本語" to "ガロン (米国)"
        ),
        "Sıvı ons (ABD)" to mapOf(
            "Türkçe" to "Sıvı ons (ABD)",
            "English" to "Fluid ounce (US)",
            "Español" to "Onza líquida (US)",
            "Deutsch" to "Flüssigunze (US)",
            "Français" to "Once liquide (US)",
            "Русский" to "Жидкая унция (США)",
            "العربية" to "أوقية سائلة (أمريكي)",
            "中文" to "液量盎司 (美)",
            "日本語" to "液量オンス (米国)"
        ),
        "Metre/saniye" to mapOf(
            "Türkçe" to "Metre/saniye",
            "English" to "Meter/second",
            "Español" to "Metro/segundo",
            "Deutsch" to "Meter/Sekunde",
            "Français" to "Mètre/seconde",
            "Русский" to "Метр/секунда",
            "العربية" to "متر/ثانية",
            "中文" to "米/秒",
            "日本語" to "メートル/秒"
        ),
        "Kilometre/saat" to mapOf(
            "Türkçe" to "Kilometre/saat",
            "English" to "Kilometer/hour",
            "Español" to "Kilómetro/hora",
            "Deutsch" to "Kilometer/Stunde",
            "Français" to "Kilomètre/heure",
            "Русский" to "Километр/час",
            "العربية" to "كيلومتر/ساعة",
            "中文" to "千米/小时",
            "日本語" to "キロメートル/時"
        ),
        "Mil/saat" to mapOf(
            "Türkçe" to "Mil/saat",
            "English" to "Mile/hour",
            "Español" to "Milla/hora",
            "Deutsch" to "Meile/Stunde",
            "Français" to "Mille/heure",
            "Русский" to "Миля/час",
            "العربية" to "ميل/ساعة",
            "中文" to "英里/小时",
            "日本語" to "マイル/時"
        ),
        "Knot" to mapOf(
            "Türkçe" to "Knot",
            "English" to "Knot",
            "Español" to "Nudo",
            "Deutsch" to "Knoten",
            "Français" to "Nœud",
            "Русский" to "Узел",
            "العربية" to "عقدة",
            "中文" to "节",
            "日本語" to "ノット"
        ),
        "Milisaniye" to mapOf(
            "Türkçe" to "Milisaniye",
            "English" to "Millisecond",
            "Español" to "Milisegundo",
            "Deutsch" to "Millisekunde",
            "Français" to "Milliseconde",
            "Русский" to "Миллисекунда",
            "العربية" to "ملي ثانية",
            "中文" to "毫秒",
            "日本語" to "ミリ秒"
        ),
        "Saniye" to mapOf(
            "Türkçe" to "Saniye",
            "English" to "Second",
            "Español" to "Segundo",
            "Deutsch" to "Sekunde",
            "Français" to "Seconde",
            "Русский" to "Секунда",
            "العربية" to "ثانية",
            "中文" to "秒",
            "日本語" to "秒"
        ),
        "Dakika" to mapOf(
            "Türkçe" to "Dakika",
            "English" to "Minute",
            "Español" to "Minuto",
            "Deutsch" to "Minute",
            "Français" to "Minute",
            "Русский" to "Минута",
            "العربية" to "دقيقة",
            "中文" to "分钟",
            "日本語" to "分"
        ),
        "Saat" to mapOf(
            "Türkçe" to "Saat",
            "English" to "Hour",
            "Español" to "Hora",
            "Deutsch" to "Stunde",
            "Français" to "Heure",
            "Русский" to "Час",
            "العربية" to "ساعة",
            "中文" to "小时",
            "日本語" to "時間"
        ),
        "Gün" to mapOf(
            "Türkçe" to "Gün",
            "English" to "Day",
            "Español" to "Día",
            "Deutsch" to "Tag",
            "Français" to "Jour",
            "Русский" to "День",
            "العربية" to "يوم",
            "中文" to "天",
            "日本語" to "日"
        ),
        "Hafta" to mapOf(
            "Türkçe" to "Hafta",
            "English" to "Week",
            "Español" to "Semana",
            "Deutsch" to "Woche",
            "Français" to "Semaine",
            "Русский" to "Неделя",
            "العربية" to "أسبوع",
            "中文" to "周",
            "日本語" to "週"
        ),
        "Yıl (365 gün)" to mapOf(
            "Türkçe" to "Yıl (365 gün)",
            "English" to "Year (365 days)",
            "Español" to "Año (365 días)",
            "Deutsch" to "Jahr (365 Tage)",
            "Français" to "Année (365 j)",
            "Русский" to "Год (365 дней)",
            "العربية" to "سنة (365 يوم)",
            "中文" to "年 (365天)",
            "日本語" to "年 (365日)"
        ),
        "Bit" to mapOf(
            "Türkçe" to "Bit",
            "English" to "Bit",
            "Español" to "Bit",
            "Deutsch" to "Bit",
            "Français" to "Bit",
            "Русский" to "Бит",
            "العربية" to "بت",
            "中文" to "比特",
            "日本語" to "ビット"
        ),
        "Bayt" to mapOf(
            "Türkçe" to "Bayt",
            "English" to "Byte",
            "Español" to "Byte",
            "Deutsch" to "Byte",
            "Français" to "Octet",
            "Русский" to "Байт",
            "العربية" to "بايت",
            "中文" to "字节",
            "日本語" to "バイト"
        ),
        "Kilobayt" to mapOf(
            "Türkçe" to "Kilobayt",
            "English" to "Kilobyte",
            "Español" to "Kilobyte",
            "Deutsch" to "Kilobyte",
            "Français" to "Kilo-octet",
            "Русский" to "Килобайт",
            "العربية" to "كيلوبايت",
            "中文" to "千字节",
            "日本語" to "キロバイト"
        ),
        "Megabayt" to mapOf(
            "Türkçe" to "Megabayt",
            "English" to "Megabyte",
            "Español" to "Megabyte",
            "Deutsch" to "Megabyte",
            "Français" to "Méga-octet",
            "Русский" to "Мегабайт",
            "العربية" to "ميجابايت",
            "中文" to "兆字节",
            "日本語" to "メガバイト"
        ),
        "Gigabayt" to mapOf(
            "Türkçe" to "Gigabayt",
            "English" to "Gigabyte",
            "Español" to "Gigabyte",
            "Deutsch" to "Gigabyte",
            "Français" to "Giga-octet",
            "Русский" to "Гигабайт",
            "العربية" to "جيجابايت",
            "中文" to "吉字节",
            "日本語" to "ギガバイト"
        ),
        "Terabayt" to mapOf(
            "Türkçe" to "Terabayt",
            "English" to "Terabyte",
            "Español" to "Terabyte",
            "Deutsch" to "Terabyte",
            "Français" to "Téra-octet",
            "Русский" to "Терабайт",
            "العربية" to "تيرابايت",
            "中文" to "太字节",
            "日本語" to "テラバイト"
        ),
        "Derece" to mapOf(
            "Türkçe" to "Derece",
            "English" to "Degree",
            "Español" to "Grado",
            "Deutsch" to "Grad",
            "Français" to "Degré",
            "Русский" to "Градус",
            "العربية" to "درجة",
            "中文" to "度",
            "日本語" to "度"
        ),
        "Radyan" to mapOf(
            "Türkçe" to "Radyan",
            "English" to "Radian",
            "Español" to "Radián",
            "Deutsch" to "Radiant",
            "Français" to "Radian",
            "Русский" to "Радиан",
            "العربية" to "راديان",
            "中文" to "弧度",
            "日本語" to "ラジアン"
        ),
        "Grad" to mapOf(
            "Türkçe" to "Grad",
            "English" to "Gradian",
            "Español" to "Gradián",
            "Deutsch" to "Neugrad",
            "Français" to "Grade",
            "Русский" to "Град",
            "العربية" to "غراد",
            "中文" to "梯度",
            "日本語" to "グラード"
        ),

        "Daily Tools" to mapOf(
            "Türkçe" to "Günlük Araçlar",
            "English" to "Daily Tools",
            "Español" to "Herramientas Diarias",
            "Deutsch" to "Tägliche Werkzeuge",
            "Français" to "Outils Quotidiens",
            "Русский" to "Ежедневные инструменты",
            "العربية" to "أدوات يومية",
            "中文" to "日常工具",
            "日本語" to "日常ツール"
        ),
        "Graphing Mode" to mapOf(
            "Türkçe" to "Grafik Modu",
            "English" to "Graphing Mode",
            "Español" to "Modo Gráfico",
            "Deutsch" to "Grafikmodus",
            "Français" to "Mode Graphique",
            "Русский" to "Режим графиков",
            "العربية" to "وضع الرسوم البيانية",
            "中文" to "绘图模式",
            "日本語" to "グラフモード"
        ),
        "İşlem Geçmişi" to mapOf(
            "Türkçe" to "İşlem Geçmişi",
            "English" to "Calculation History",
            "Español" to "Historial de Cálculo",
            "Deutsch" to "Berechnungsverlauf",
            "Français" to "Historique des Calculs",
            "Русский" to "История вычислений",
            "العربية" to "سجل الحسابات",
            "中文" to "计算历史",
            "日本語" to "計算履歴"
        ),
        "Unit Converter" to mapOf(
            "Türkçe" to "Birim Dönüştürücü",
            "English" to "Unit Converter",
            "Español" to "Conversor de Unidades",
            "Deutsch" to "Einheitenumrechner",
            "Français" to "Convertisseur d'unités",
            "Русский" to "Конвертер величин",
            "العربية" to "محول الوحدات",
            "中文" to "单位转换器",
            "日本語" to "単位変換器"
        ),
        "Align object to top edge,\nthen tap or drag to its end" to mapOf(
            "Türkçe" to "Nesneyi üst kenara hizalayın,\nardından ucuna dokunun veya kaydırın",
            "English" to "Align object to top edge,\nthen tap or drag to its end",
            "Español" to "Alinee el objeto al borde superior,\nluego toque o arrastre",
            "Deutsch" to "Objekt an der Oberkante ausrichten,\ndann tippen oder ziehen",
            "Français" to "Alignez l'objet en haut,\npuis touchez ou faites glisser",
            "Русский" to "Выровняйте по верхнему краю,\nзатем коснитесь или перетащите",
            "العربية" to "قم بمحاذاة الكائن للحافة العلوية،\nثم اضغط أو اسحب",
            "中文" to "将对象对齐顶部边缘，\n然后点击或拖动",
            "日本語" to "オブジェクトを上端に合わせ、\nタップまたはドラッグしてください"
        ),
        "İnce Ayar (Kalibrasyon): %" to mapOf(
            "Türkçe" to "İnce Ayar (Kalibrasyon): %",
            "English" to "Fine-tune (Calibration): %",
            "Español" to "Ajuste fino (Calibración): %",
            "Deutsch" to "Feineinstellung (Kalibrierung): %",
            "Français" to "Ajustement fin (Calibrage): %",
            "Русский" to "Тонкая настройка (Калибровка): %",
            "العربية" to "ضبط دقيق (معايرة): %",
            "中文" to "微调 (校准): %",
            "日本語" to "微調整 (キャリブレーション): %"
        ),
        "Not: Üreticiler bazen ekran ydpi değerini yanlış raporlar.\nEğer cetvel hatalıysa gerçek bir cetvel koyarak kaydırıcı ile çizgileri eşitleyin." to mapOf(
            "Türkçe" to "Not: Üreticiler bazen ekran ydpi değerini yanlış raporlar.\nEğer cetvel hatalıysa gerçek bir cetvel koyarak kaydırıcı ile çizgileri eşitleyin.",
            "English" to "Note: Manufacturers sometimes report wrong ydpi.\nIf ruler is inaccurate, place a real ruler and adjust slider.",
            "Español" to "Nota: A veces el ydpi es incorrecto.\nSi la regla falla, ajuste con una regla real.",
            "Deutsch" to "Hinweis: ydpi wird oft falsch gemeldet.\nPassen Sie den Schieberegler mit einem echten Lineal an.",
            "Français" to "Note: Le ydpi est parfois incorrect.\nAjustez avec une vraie règle si besoin.",
            "Русский" to "Примечание: ydpi иногда неверный.\nПри ошибке откалибруйте по настоящей линейке.",
            "العربية" to "ملاحظة: تقرير ydpi قد يكون خاطئا.\nاضبط باستخدام مسطرة حقيقية.",
            "中文" to "注意: 制造商有时会报告错误的 ydpi。\n如果不准确，请使用真实尺子调整滑块。",
            "日本語" to "注意: 画面のydpiが間違っていることがあります。\n実物の定規を使ってスライダーを調整してください。"
        ),
        "← Calculator" to mapOf(
            "Türkçe" to "← Hesap Makinesi",
            "English" to "← Calculator",
            "Español" to "← Calculadora",
            "Deutsch" to "← Rechner",
            "Français" to "← Calculatrice",
            "Русский" to "← Калькулятор",
            "العربية" to "← الحاسبة",
            "中文" to "← 计算器",
            "日本語" to "← 電卓"
        ),

        "Hesap Makinesi" to mapOf(
            "Türkçe" to "Hesap Makinesi",
            "English" to "Calculator",
            "Español" to "Calculadora",
            "Deutsch" to "Taschenrechner",
            "Français" to "Calculatrice",
            "Русский" to "Калькулятор",
            "العربية" to "آلة حاسبة",
            "中文" to "计算器",
            "日本語" to "電卓"
        ),
        "Günlük Araçlar" to mapOf(
            "Türkçe" to "Günlük Araçlar",
            "English" to "Daily Tools",
            "Español" to "Herramientas Diarias",
            "Deutsch" to "Tägliche Werkzeuge",
            "Français" to "Outils Quotidiens",
            "Русский" to "Ежедневные инструменты",
            "العربية" to "أدوات يومية",
            "中文" to "日常工具",
            "日本語" to "日常ツール"
        ),
        "Grafik" to mapOf(
            "Türkçe" to "Grafik",
            "English" to "Graphing",
            "Español" to "Gráficos",
            "Deutsch" to "Grafik",
            "Français" to "Graphique",
            "Русский" to "Графики",
            "العربية" to "الرسوم البيانية",
            "中文" to "绘图",
            "日本語" to "グラフ"
        ),
        "Geçmiş" to mapOf(
            "Türkçe" to "Geçmiş",
            "English" to "History",
            "Español" to "Historial",
            "Deutsch" to "Verlauf",
            "Français" to "Historique",
            "Русский" to "История",
            "العربية" to "تاريخ",
            "中文" to "历史",
            "日本語" to "履歴"
        ),
        "Geçmiş silinsin mi?" to mapOf(
            "Türkçe" to "Geçmiş silinsin mi?",
            "English" to "Delete history?",
            "Español" to "¿Eliminar historial?",
            "Deutsch" to "Verlauf löschen?",
            "Français" to "Supprimer l'historique ?",
            "Русский" to "Удалить историю?",
            "العربية" to "حذف السجل؟",
            "中文" to "删除历史记录？",
            "日本語" to "履歴を削除しますか？"
        ),
        "Tüm işlem geçmişi kalıcı olarak silinecek." to mapOf(
            "Türkçe" to "Tüm işlem geçmişi kalıcı olarak silinecek.",
            "English" to "All history will be permanently deleted.",
            "Español" to "Todo el historial se eliminará permanentemente.",
            "Deutsch" to "Der gesamte Verlauf wird dauerhaft gelöscht.",
            "Français" to "Tout l'historique sera supprimé.",
            "Русский" to "Вся история будет удалена навсегда.",
            "العربية" to "سيتم حذف السجل نهائيا.",
            "中文" to "所有历史记录将被永久删除。",
            "日本語" to "すべての履歴が完全に削除されます。"
        ),
        "Sil" to mapOf(
            "Türkçe" to "Sil",
            "English" to "Delete",
            "Español" to "Eliminar",
            "Deutsch" to "Löschen",
            "Français" to "Supprimer",
            "Русский" to "Удалить",
            "العربية" to "حذف",
            "中文" to "删除",
            "日本語" to "削除"
        ),
        "Vazgeç" to mapOf(
            "Türkçe" to "Vazgeç",
            "English" to "Cancel",
            "Español" to "Cancelar",
            "Deutsch" to "Abbrechen",
            "Français" to "Annuler",
            "Русский" to "Отмена",
            "العربية" to "إلغاء",
            "中文" to "取消",
            "日本語" to "キャンセル"
        ),
        "Henüz işlem yok" to mapOf(
            "Türkçe" to "Henüz işlem yok",
            "English" to "No history yet",
            "Español" to "Sin historial",
            "Deutsch" to "Noch kein Verlauf",
            "Français" to "Aucun historique",
            "Русский" to "Нет истории",
            "العربية" to "لا يوجد سجل",
            "中文" to "暂无记录",
            "日本語" to "履歴なし"
        ),
        "HesapMakinesi" to mapOf(
            "Türkçe" to "HesapMakinesi",
            "English" to "Calculator",
            "Español" to "Calculadora",
            "Deutsch" to "Rechner",
            "Français" to "Calculatrice",
            "Русский" to "Калькулятор",
            "العربية" to "الحاسبة",
            "中文" to "计算器",
            "日本語" to "電卓"
        ),
        "Kapatmak için penceredeki çarpıya basın." to mapOf(
            "Türkçe" to "Kapatmak için penceredeki çarpıya basın.",
            "English" to "Press the cross in the window to close.",
            "Español" to "Presione la cruz para cerrar.",
            "Deutsch" to "Klicken Sie zum Schließen auf das Kreuz.",
            "Français" to "Appuyez sur la croix pour fermer.",
            "Русский" to "Нажмите на крестик, чтобы закрыть.",
            "العربية" to "اضغط على علامة X للإغلاق.",
            "中文" to "点击窗口上的叉号关闭。",
            "日本語" to "閉じるにはバツを押してください。"
        ),
        "Add Equation" to mapOf(
            "Türkçe" to "Denklem Ekle",
            "English" to "Add Equation",
            "Español" to "Añadir Ecuación",
            "Deutsch" to "Gleichung hinzufügen",
            "Français" to "Ajouter l'équation",
            "Русский" to "Добавить уравнение",
            "العربية" to "إضافة معادلة",
            "中文" to "添加方程式",
            "日本語" to "方程式を追加"
        ),
        "Grafik Rengi Seç" to mapOf(
            "Türkçe" to "Grafik Rengi Seç",
            "English" to "Select Graph Color",
            "Español" to "Seleccionar color del gráfico",
            "Deutsch" to "Diagrammfarbe wählen",
            "Français" to "Choisir la couleur",
            "Русский" to "Выбрать цвет графика",
            "العربية" to "اختر لون الرسم البياني",
            "中文" to "选择图表颜色",
            "日本語" to "グラフの色を選択"
        ),
        "Renk Değiştir" to mapOf(
            "Türkçe" to "Renk Değiştir",
            "English" to "Change Color",
            "Español" to "Cambiar color",
            "Deutsch" to "Farbe ändern",
            "Français" to "Changer de couleur",
            "Русский" to "Изменить цвет",
            "العربية" to "تغيير اللون",
            "中文" to "更改颜色",
            "日本語" to "色を変更"
        ),
        "Benzersiz Yeni Renk Üret" to mapOf(
            "Türkçe" to "Benzersiz Yeni Renk Üret",
            "English" to "Generate Unique Color",
            "Español" to "Generar color único",
            "Deutsch" to "Eindeutige Farbe generieren",
            "Français" to "Générer une couleur unique",
            "Русский" to "Создать уникальный цвет",
            "العربية" to "توليد لون مميز",
            "中文" to "生成独特色彩",
            "日本語" to "固有の色を生成"
        ),
        "Kapat" to mapOf(
            "Türkçe" to "Kapat",
            "English" to "Close",
            "Español" to "Cerrar",
            "Deutsch" to "Schließen",
            "Français" to "Fermer",
            "Русский" to "Закрыть",
            "العربية" to "إغلاق",
            "中文" to "关闭",
            "日本語" to "閉じる"
        ),
        "Gizle" to mapOf(
            "Türkçe" to "Gizle",
            "English" to "Hide",
            "Español" to "Ocultar",
            "Deutsch" to "Ausblenden",
            "Français" to "Masquer",
            "Русский" to "Скрыть",
            "العربية" to "إخفاء",
            "中文" to "隐藏",
            "日本語" to "非表示"
        ),
        "Göster" to mapOf(
            "Türkçe" to "Göster",
            "English" to "Show",
            "Español" to "Mostrar",
            "Deutsch" to "Einblenden",
            "Français" to "Afficher",
            "Русский" to "Показать",
            "العربية" to "إظهار",
            "中文" to "显示",
            "日本語" to "表示"
        ),
        "Ruler" to mapOf(
            "Türkçe" to "Cetvel",
            "English" to "Ruler",
            "Español" to "Regla",
            "Deutsch" to "Lineal",
            "Français" to "Règle",
            "Русский" to "Линейка",
            "العربية" to "مسطرة",
            "中文" to "尺子",
            "日本語" to "定規"
        ),
        "Sıfırla" to mapOf(
            "Türkçe" to "Sıfırla",
            "English" to "Reset",
            "Español" to "Reiniciar",
            "Deutsch" to "Zurücksetzen",
            "Français" to "Réinitialiser",
            "Русский" to "Сброс",
            "العربية" to "إعادة ضبط",
            "中文" to "重置",
            "日本語" to "リセット"
        ),
        "Birim" to mapOf(
            "Türkçe" to "Birim",
            "English" to "Unit",
            "Español" to "Unidad",
            "Deutsch" to "Einheit",
            "Français" to "Unité",
            "Русский" to "Единица",
            "العربية" to "وحدة",
            "中文" to "单位",
            "日本語" to "単位"
        ),
        "Değer" to mapOf(
            "Türkçe" to "Değer",
            "English" to "Value",
            "Español" to "Valor",
            "Deutsch" to "Wert",
            "Français" to "Valeur",
            "Русский" to "Значение",
            "العربية" to "قيمة",
            "中文" to "值",
            "日本語" to "値"
        ),
        "Kaynak birim" to mapOf(
            "Türkçe" to "Kaynak birim",
            "English" to "Source unit",
            "Español" to "Unidad de origen",
            "Deutsch" to "Quelleinheit",
            "Français" to "Unité source",
            "Русский" to "Исходная единица",
            "العربية" to "الوحدة المصدر",
            "中文" to "源单位",
            "日本語" to "ソース単位"
        ),
        "Base changed to" to mapOf(
            "Türkçe" to "Taban değişti:",
            "English" to "Base changed to",
            "Español" to "Base cambiada a",
            "Deutsch" to "Basis geändert in",
            "Français" to "Base modifiée en",
            "Русский" to "База изменена на",
            "العربية" to "تم تغيير الأساس إلى",
            "中文" to "基数更改为",
            "日本語" to "基準が変更されました"
        ),
        "Kopyalandı" to mapOf(
            "Türkçe" to "Kopyalandı",
            "English" to "Copied",
            "Español" to "Copiado",
            "Deutsch" to "Kopiert",
            "Français" to "Copié",
            "Русский" to "Скопировано",
            "العربية" to "تم النسخ",
            "中文" to "已复制",
            "日本語" to "コピー済み"
        ),
        "Sonuç kopyalandı" to mapOf(
            "Türkçe" to "Sonuç kopyalandı",
            "English" to "Result copied",
            "Español" to "Resultado copiado",
            "Deutsch" to "Ergebnis kopiert",
            "Français" to "Résultat copié",
            "Русский" to "Результат скопирован",
            "العربية" to "تم نسخ النتيجة",
            "中文" to "结果已复制",
            "日本語" to "結果がコピーされました"
        ),
        "Result Copied" to mapOf(
            "Türkçe" to "Sonuç Kopyalandı",
            "English" to "Result Copied",
            "Español" to "Resultado Copiado",
            "Deutsch" to "Ergebnis kopiert",
            "Français" to "Résultat copié",
            "Русский" to "Результат скопирован",
            "العربية" to "تم نسخ النتيجة",
            "中文" to "结果已复制",
            "日本語" to "結果がコピーされました"
        ),
        "Discount Miktarı:" to mapOf(
            "Türkçe" to "İndirim Miktarı:",
            "English" to "Discount Amount:",
            "Español" to "Cantidad Descontada:",
            "Deutsch" to "Rabattbetrag:",
            "Français" to "Montant de la remise:",
            "Русский" to "Сумма скидки:",
            "العربية" to "قيمة الخصم:",
            "中文" to "折扣金额:",
            "日本語" to "割引額:"
        ),
        "Final Price:" to mapOf(
            "Türkçe" to "Son Fiyat:",
            "English" to "Final Price:",
            "Español" to "Precio Final:",
            "Deutsch" to "Endpreis:",
            "Français" to "Prix Final:",
            "Русский" to "Итоговая цена:",
            "العربية" to "السعر النهائي:",
            "中文" to "最终价格:",
            "日本語" to "最終価格:"
        ),
        "Total Price:" to mapOf(
            "Türkçe" to "Toplam Fiyat:",
            "English" to "Total Price:",
            "Español" to "Precio Total:",
            "Deutsch" to "Gesamtpreis:",
            "Français" to "Prix Total:",
            "Русский" to "Общая цена:",
            "العربية" to "السعر الإجمالي:",
            "中文" to "总价:",
            "日本語" to "合計価格:"
        ),
        "Tax Amount:" to mapOf(
            "Türkçe" to "Vergi Miktarı:",
            "English" to "Tax Amount:",
            "Español" to "Monto de Impuesto:",
            "Deutsch" to "Steuerbetrag:",
            "Français" to "Montant Taxe:",
            "Русский" to "Сумма налога:",
            "العربية" to "قيمة الضريبة:",
            "中文" to "税额:",
            "日本語" to "税額:"
        ),
        "Price without Tax:" to mapOf(
            "Türkçe" to "Vergisiz Fiyat:",
            "English" to "Price without Tax:",
            "Español" to "Precio sin Impuesto:",
            "Deutsch" to "Preis ohne Steuer:",
            "Français" to "Prix HT:",
            "Русский" to "Цена без налога:",
            "العربية" to "السعر بدون ضريبة:",
            "中文" to "不含税价格:",
            "日本語" to "税抜価格:"
        ),
        "Total Bill:" to mapOf(
            "Türkçe" to "Toplam Hesap:",
            "English" to "Total Bill:",
            "Español" to "Total de la Cuenta:",
            "Deutsch" to "Gesamtrechnung:",
            "Français" to "Addition Totale:",
            "Русский" to "Общий счет:",
            "العربية" to "إجمالي الفاتورة:",
            "中文" to "总账单:",
            "日本語" to "合計請求額:"
        ),
        "Toplam Tip & Split:" to mapOf(
            "Türkçe" to "Toplam Bahşiş:",
            "English" to "Total Tip:",
            "Español" to "Propina Total:",
            "Deutsch" to "Gesamtes Trinkgeld:",
            "Français" to "Pourboire Total:",
            "Русский" to "Общие чаевые:",
            "العربية" to "إجمالي البقشيش:",
            "中文" to "总小费:",
            "日本語" to "合計チップ:"
        ),
        "Per Person:" to mapOf(
            "Türkçe" to "Kişi Başı:",
            "English" to "Per Person:",
            "Español" to "Por Persona:",
            "Deutsch" to "Pro Person:",
            "Français" to "Par Personne:",
            "Русский" to "С человека:",
            "العربية" to "لكل شخص:",
            "中文" to "每人:",
            "日本語" to "一人当たり:"
        ),
        "Monthly Payment:" to mapOf(
            "Türkçe" to "Aylık Ödeme:",
            "English" to "Monthly Payment:",
            "Español" to "Pago Mensual:",
            "Deutsch" to "Monatliche Rate:",
            "Français" to "Paiement Mensuel:",
            "Русский" to "Ежемесячный платеж:",
            "العربية" to "الدفعة الشهرية:",
            "中文" to "每月还款:",
            "日本語" to "月々の支払い:"
        ),
        "Total Payment:" to mapOf(
            "Türkçe" to "Toplam Ödeme:",
            "English" to "Total Payment:",
            "Español" to "Pago Total:",
            "Deutsch" to "Gesamtzahlung:",
            "Français" to "Paiement Total:",
            "Русский" to "Общая сумма:",
            "العربية" to "إجمالي الدفع:",
            "中文" to "总还款:",
            "日本語" to "総支払額:"
        ),
        "Total Interest:" to mapOf(
            "Türkçe" to "Toplam Faiz:",
            "English" to "Total Interest:",
            "Español" to "Interés Total:",
            "Deutsch" to "Gesamtzinsen:",
            "Français" to "Intérêt Total:",
            "Русский" to "Общий процент:",
            "العربية" to "إجمالي الفائدة:",
            "中文" to "总利息:",
            "日本語" to "総利息:"
        ),
        "Letter Grade:" to mapOf(
            "Türkçe" to "Harf Notu:",
            "English" to "Letter Grade:",
            "Español" to "Calificación:",
            "Deutsch" to "Note:",
            "Français" to "Note (Lettre):",
            "Русский" to "Буквенная оценка:",
            "العربية" to "التقدير:",
            "中文" to "字母等级:",
            "日本語" to "成績:"
        ),
        "Status:" to mapOf(
            "Türkçe" to "Durum:",
            "English" to "Status:",
            "Español" to "Estado:",
            "Deutsch" to "Status:",
            "Français" to "Statut:",
            "Русский" to "Статус:",
            "العربية" to "الحالة:",
            "中文" to "状态:",
            "日本語" to "ステータス:"
        ),
        "Area:" to mapOf(
            "Türkçe" to "Alan:",
            "English" to "Area:",
            "Español" to "Área:",
            "Deutsch" to "Fläche:",
            "Français" to "Surface:",
            "Русский" to "Площадь:",
            "العربية" to "المساحة:",
            "中文" to "面积:",
            "日本語" to "面積:"
        ),
        "Perimeter:" to mapOf(
            "Türkçe" to "Çevre:",
            "English" to "Perimeter:",
            "Español" to "Perímetro:",
            "Deutsch" to "Umfang:",
            "Français" to "Périmètre:",
            "Русский" to "Периметр:",
            "العربية" to "المحيط:",
            "中文" to "周长:",
            "日本語" to "周囲:"
        ),
        "Body Mass Index:" to mapOf(
            "Türkçe" to "Vücut Kitle İndeksi:",
            "English" to "Body Mass Index:",
            "Español" to "Índice de Masa:",
            "Deutsch" to "Body-Mass-Index:",
            "Français" to "Indice de Masse:",
            "Русский" to "Индекс массы тела:",
            "العربية" to "مؤشر كتلة الجسم:",
            "中文" to "体重指数:",
            "日本語" to "BMI:"
        ),

        "Titreşim Gücü:" to mapOf(
            "Türkçe" to "Titreşim Gücü:",
            "English" to "Vibration Strength:",
            "Español" to "Fuerza de Vibración:",
            "Deutsch" to "Vibrationsstärke:",
            "Français" to "Force de Vibration:",
            "Русский" to "Сила вибрации:",
            "العربية" to "قوة الاهتزاز:",
            "中文" to "振动强度:",
            "日本語" to "振動の強さ:"
        ),
        "Özel Ses Dosyası" to mapOf(
            "Türkçe" to "Özel Ses Dosyası",
            "English" to "Custom Sound File",
            "Español" to "Archivo de Audio",
            "Deutsch" to "Eigene Tondatei",
            "Français" to "Fichier Son",
            "Русский" to "Свой аудиофайл",
            "العربية" to "ملف صوت مخصص",
            "中文" to "自定义声音文件",
            "日本語" to "カスタムサウンド"
        ),
        "Sesi Değiştir" to mapOf(
            "Türkçe" to "Sesi Değiştir",
            "English" to "Change Sound",
            "Español" to "Cambiar Sonido",
            "Deutsch" to "Ton ändern",
            "Français" to "Changer de son",
            "Русский" to "Изменить звук",
            "العربية" to "تغيير الصوت",
            "中文" to "更改声音",
            "日本語" to "音を変更する"
        ),
        "Ses Dosyası Seç" to mapOf(
            "Türkçe" to "Ses Dosyası Seç",
            "English" to "Select Sound File",
            "Español" to "Elegir Archivo",
            "Deutsch" to "Datei auswählen",
            "Français" to "Sélectionner",
            "Русский" to "Выбрать файл",
            "العربية" to "اختر ملف الصوت",
            "中文" to "选择声音文件",
            "日本語" to "ファイルを選択"
        ),
        "Aktif: Özel Yüklenen Ses" to mapOf(
            "Türkçe" to "Aktif: Özel Yüklenen Ses",
            "English" to "Active: Custom Sound",
            "Español" to "Activo: Sonido Custom",
            "Deutsch" to "Aktiv: Eigener Ton",
            "Français" to "Actif : Son Personnalisé",
            "Русский" to "Активно: Свой звук",
            "العربية" to "نشط: صوت مخصص",
            "中文" to "活动：自定义声音",
            "日本語" to "アクティブ：カスタムサウンド"
        ),
        "Çalma Süresi (Sesi Kes):" to mapOf(
            "Türkçe" to "Çalma Süresi (Sesi Kes):",
            "English" to "Play Duration (Cutoff):",
            "Español" to "Duración de Repro:",
            "Deutsch" to "Spieldauer (Cutoff):",
            "Français" to "Durée (Coupure):",
            "Русский" to "Длительность:",
            "العربية" to "مدة التشغيل:",
            "中文" to "播放持续时间 (切断):",
            "日本語" to "再生時間 (カットオフ):"
        ),
        "Aktif: Modern Tık Sesi" to mapOf(
            "Türkçe" to "Aktif: Modern Tık Sesi",
            "English" to "Active: Modern Click",
            "Español" to "Activo: Clic Moderno",
            "Deutsch" to "Aktiv: Moderner Klick",
            "Français" to "Actif : Clic Moderne",
            "Русский" to "Активно: Клик",
            "العربية" to "نشط: نقرة حديثة",
            "中文" to "活动：现代点击音",
            "日本語" to "アクティブ：モダンクリック"
        ),
        "Ses Seviyesi:" to mapOf(
            "Türkçe" to "Ses Seviyesi:",
            "English" to "Volume Level:",
            "Español" to "Volumen:",
            "Deutsch" to "Lautstärke:",
            "Français" to "Volume:",
            "Русский" to "Громкость:",
            "العربية" to "مستوى الصوت:",
            "中文" to "音量级别:",
            "日本語" to "音量レベル:"
        ),
        "Cetvel" to mapOf(
            "Türkçe" to "Cetvel",
            "English" to "Ruler",
            "Español" to "Regla",
            "Deutsch" to "Lineal",
            "Français" to "Règle",
            "Русский" to "Линейка",
            "العربية" to "مسطرة",
            "中文" to "尺子",
            "日本語" to "定規"
        ),
        "BMI" to mapOf(
            "Türkçe" to "VKİ (BMI)",
            "English" to "BMI",
            "Español" to "IMC",
            "Deutsch" to "BMI",
            "Français" to "IMC",
            "Русский" to "ИМТ",
            "العربية" to "مؤشر كتلة الجسم",
            "中文" to "BMI",
            "日本語" to "BMI"
        ),
        "BMI Hesaplayıcı" to mapOf(
            "Türkçe" to "BMI Hesaplayıcı",
            "English" to "BMI Calculator",
            "Español" to "Calculadora BMI",
            "Deutsch" to "BMI Rechner",
            "Français" to "Calcul IMC",
            "Русский" to "Калькулятор ИМТ",
            "العربية" to "حاسبة مؤشر كتلة الجسم",
            "中文" to "BMI 计算器",
            "日本語" to "BMI 電卓"
        ),
        "Döviz Çevirici" to mapOf(
            "Türkçe" to "Döviz Çevirici",
            "English" to "Currency Converter",
            "Español" to "Conversor de Moneda",
            "Deutsch" to "Währungsrechner",
            "Français" to "Convertisseur de Devises",
            "Русский" to "Конвертер валют",
            "العربية" to "محول العملات",
            "中文" to "货币转换器",
            "日本語" to "通貨コンバータ"
        ),
        "İndirim Hesaplayıcı" to mapOf(
            "Türkçe" to "İndirim Hesaplayıcı",
            "English" to "Discount Calculator",
            "Español" to "Calculadora Descuento",
            "Deutsch" to "Rabattrechner",
            "Français" to "Calcul de Remise",
            "Русский" to "Калькулятор скидок",
            "العربية" to "حاسبة الخصم",
            "中文" to "折扣计算器",
            "日本語" to "割引電卓"
        ),
        "KDV / Vergi" to mapOf(
            "Türkçe" to "KDV / Vergi",
            "English" to "Sales Tax / VAT",
            "Español" to "Impuesto / IVA",
            "Deutsch" to "Mehrwertsteuer",
            "Français" to "TVA",
            "Русский" to "НДС",
            "العربية" to "ضريبة المبيعات",
            "中文" to "销售税 / 增值税",
            "日本語" to "消費税 / VAT"
        ),
        "Bahşiş & Hesap Bölme" to mapOf(
            "Türkçe" to "Bahşiş & Hesap Bölme",
            "English" to "Tip & Split",
            "Español" to "Propina y Dividir",
            "Deutsch" to "Trinkgeld & Teilen",
            "Français" to "Pourboire & Partager",
            "Русский" to "Чаевые и счет",
            "العربية" to "البقشيش والفاتورة",
            "中文" to "小费与平摊",
            "日本語" to "チップと割り勘"
        ),
        "Kredi Hesaplama" to mapOf(
            "Türkçe" to "Kredi Hesaplama",
            "English" to "Loan Calculator",
            "Español" to "Calculadora Préstamo",
            "Deutsch" to "Kreditrechner",
            "Français" to "Calcul Prêt",
            "Русский" to "Кредитный калькулятор",
            "العربية" to "حاسبة القرض",
            "中文" to "贷款计算器",
            "日本語" to "ローン電卓"
        ),
        "Not / GPA Hesaplama" to mapOf(
            "Türkçe" to "Not / GPA",
            "English" to "GPA Calculator",
            "Español" to "Calculadora GPA",
            "Deutsch" to "GPA Rechner",
            "Français" to "Calcul GPA",
            "Русский" to "Калькулятор GPA",
            "العربية" to "حاسبة المعدل التراكمي",
            "中文" to "GPA 计算器",
            "日本語" to "GPA 電卓"
        ),
        "Şekiller & Alan" to mapOf(
            "Türkçe" to "Şekiller & Alan",
            "English" to "Shapes & Area",
            "Español" to "Formas y Área",
            "Deutsch" to "Formen & Fläche",
            "Français" to "Formes & Surface",
            "Русский" to "Фигуры и площадь",
            "العربية" to "الأشكال والمساحة",
            "中文" to "形状与面积",
            "日本語" to "図形と面積"
        ),
        "System Default" to mapOf(
            "Türkçe" to "Sistem Varsayılanı",
            "English" to "System Default",
            "Español" to "Por defecto del sistema",
            "Deutsch" to "Systemstandard",
            "Français" to "Défaut du système",
            "Русский" to "Системная по умолчанию",
            "العربية" to "النظام الافتراضي",
            "中文" to "系统默认",
            "日本語" to "システムデフォルト"
        ),
        "Light Theme" to mapOf(
            "Türkçe" to "Aydınlık Tema",
            "English" to "Light Theme",
            "Español" to "Tema Claro",
            "Deutsch" to "Helles Design",
            "Français" to "Thème Clair",
            "Русский" to "Светлая тема",
            "العربية" to "مظهر فاتح",
            "中文" to "浅色主题",
            "日本語" to "ライトテーマ"
        ),
        "Dark Theme" to mapOf(
            "Türkçe" to "Karanlık Tema",
            "English" to "Dark Theme",
            "Español" to "Tema Oscuro",
            "Deutsch" to "Dunkles Design",
            "Français" to "Thème Sombre",
            "Русский" to "Темная тема",
            "العربية" to "مظهر داكن",
            "中文" to "深色主题",
            "日本語" to "ダークテーマ"
        ),
        "Pure Black (AMOLED)" to mapOf(
            "Türkçe" to "Simsiyah (AMOLED)",
            "English" to "Pure Black (AMOLED)",
            "Español" to "Negro Puro (AMOLED)",
            "Deutsch" to "Pechschwarz (AMOLED)",
            "Français" to "Noir absolu (AMOLED)",
            "Русский" to "Чистый черный",
            "العربية" to "أسود نقي",
            "中文" to "纯黑 (AMOLED)",
            "日本語" to "ピュアブラック"
        ),
        "Immersive (Hidden)" to mapOf(
            "Türkçe" to "Gizle (Tam Ekran)",
            "English" to "Immersive (Hidden)",
            "Español" to "Inmersivo (Oculto)",
            "Deutsch" to "Immersiv (Versteckt)",
            "Français" to "Immersif (Masqué)",
            "Русский" to "Скрыть (Полный экран)",
            "العربية" to "إخفاء",
            "中文" to "沉浸模式 (隐藏)",
            "日本語" to "没入型 (非表示)"
        ),
        "Üstünde Dur (Standart)" to mapOf(
            "Türkçe" to "Üstünde Dur (Standart)",
            "English" to "Standard (Above)",
            "Español" to "Estándar (Arriba)",
            "Deutsch" to "Standard (Darüber)",
            "Français" to "Standard (Au-dessus)",
            "Русский" to "Стандартный",
            "العربية" to "قياسي",
            "中文" to "标准 (上方)",
            "日本語" to "標準 (上に表示)"
        ),

        "Tema Seçimi" to mapOf(
            "Türkçe" to "Tema Seçimi",
            "English" to "Theme Selection",
            "Español" to "Selección de Tema",
            "Deutsch" to "Themenauswahl",
            "Français" to "Sélection du thème",
            "Русский" to "Выбор темы",
            "العربية" to "اختيار السمة",
            "中文" to "主题选择",
            "日本語" to "テーマの選択"
        ),
        "Select your preferred theme." to mapOf(
            "Türkçe" to "Tercih ettiğiniz temayı seçin.",
            "English" to "Select your preferred theme.",
            "Español" to "Seleccione su tema.",
            "Deutsch" to "Wählen Sie Ihr Design.",
            "Français" to "Sélectionnez votre thème.",
            "Русский" to "Выберите тему.",
            "العربية" to "اختر السمة.",
            "中文" to "选择您的主题。",
            "日本語" to "テーマを選択。"
        ),
        "Karanlık" to mapOf(
            "Türkçe" to "Karanlık",
            "English" to "Dark",
            "Español" to "Oscuro",
            "Deutsch" to "Dunkel",
            "Français" to "Sombre",
            "Русский" to "Темный",
            "العربية" to "داكن",
            "中文" to "暗",
            "日本語" to "ダーク"
        ),
        "Aydınlık" to mapOf(
            "Türkçe" to "Aydınlık",
            "English" to "Light",
            "Español" to "Claro",
            "Deutsch" to "Hell",
            "Français" to "Clair",
            "Русский" to "Светлый",
            "العربية" to "فاتح",
            "中文" to "明",
            "日本語" to "ライト"
        ),
        "Simsiyah" to mapOf(
            "Türkçe" to "Simsiyah",
            "English" to "Pitch Black",
            "Español" to "Negro Puro",
            "Deutsch" to "Pechschwarz",
            "Français" to "Noir absolu",
            "Русский" to "Черный",
            "العربية" to "أسود غامق",
            "中文" to "纯黑",
            "日本語" to "真っ黒"
        ),
        "Navigasyon Çubuğu Modu" to mapOf(
            "Türkçe" to "Navigasyon Çubuğu Modu",
            "English" to "Navigation Bar Mode",
            "Español" to "Modo de Barra de Navegación",
            "Deutsch" to "Navigationsleisten-Modus",
            "Français" to "Mode Barre de Nav",
            "Русский" to "Панель навигации",
            "العربية" to "وضع شريط التنقل",
            "中文" to "导航栏模式",
            "日本語" to "ナビゲーションバー"
        ),
        "Force the bottom navigation bar to be dark." to mapOf(
            "Türkçe" to "Alt navigasyon çubuğunu koyu olmaya zorlar.",
            "English" to "Force the bottom navigation bar to be dark.",
            "Español" to "Fuerza la barra inferior a oscura.",
            "Deutsch" to "Erzwingt dunkle Navigationsleiste.",
            "Français" to "Force la barre en sombre.",
            "Русский" to "Темная нижняя панель.",
            "العربية" to "إجبار شريط التنقل على الوضع الداكن.",
            "中文" to "强制底部导航栏为深色。",
            "日本語" to "ナビゲーションバーを暗くします。"
        ),
        "Tuş Titreşimi" to mapOf(
            "Türkçe" to "Tuş Titreşimi",
            "English" to "Key Vibration",
            "Español" to "Vibración de Teclas",
            "Deutsch" to "Tastenvibration",
            "Français" to "Vibration des touches",
            "Русский" to "Вибрация клавиш",
            "العربية" to "اهتزاز المفاتيح",
            "中文" to "按键振动",
            "日本語" to "キーの振動"
        ),
        "Tuş Sesi" to mapOf(
            "Türkçe" to "Tuş Sesi",
            "English" to "Key Sound",
            "Español" to "Sonido de Tecla",
            "Deutsch" to "Tastenton",
            "Français" to "Son des touches",
            "Русский" to "Звук клавиш",
            "العربية" to "صوت المفاتيح",
            "中文" to "按键音",
            "日本語" to "キーの音"
        ),
        "Tuşlara basıldığında haptic geri bildirim verir." to mapOf(
            "Türkçe" to "Tuşlara basıldığında haptic geri bildirim verir.",
            "English" to "Provides haptic feedback on key presses.",
            "Español" to "Da respuesta háptica al pulsar.",
            "Deutsch" to "Gibt haptisches Feedback.",
            "Français" to "Retour haptique lors de la frappe.",
            "Русский" to "Тактильная отдача при нажатии.",
            "العربية" to "ردود فعل لمسية.",
            "中文" to "按下按键时提供触觉反馈。",
            "日本語" to "タップ時に触覚フィードバックを提供。"
        ),
        "Tuşlara basıldığında ses çıkarır." to mapOf(
            "Türkçe" to "Tuşlara basıldığında ses çıkarır.",
            "English" to "Plays a sound on key presses.",
            "Español" to "Hace sonido al pulsar.",
            "Deutsch" to "Spielt einen Ton beim Drücken.",
            "Français" to "Émet un son lors de la frappe.",
            "Русский" to "Звук при нажатии.",
            "العربية" to "يصدر صوتا عند الضغط.",
            "中文" to "按下按键时播放声音。",
            "日本語" to "タップ時に音を鳴らす。"
        ),
        "Titreşim Gücü" to mapOf(
            "Türkçe" to "Titreşim Gücü",
            "English" to "Vibration Strength",
            "Español" to "Fuerza de Vibración",
            "Deutsch" to "Vibrationsstärke",
            "Français" to "Force de Vibration",
            "Русский" to "Сила вибрации",
            "العربية" to "قوة الاهتزاز",
            "中文" to "振动强度",
            "日本語" to "振動の強さ"
        ),

        "Date Calculator" to mapOf(
            "English" to "Date Calculator",
            "Türkçe" to "Tarih Hesaplayıcı",
            "Español" to "Calculadora de Fecha",
            "Deutsch" to "Datumsrechner",
            "Français" to "Calculatrice de Date",
            "Русский" to "Калькулятор дат",
            "العربية" to "حاسبة التاريخ",
            "中文" to "日期计算器",
            "日本語" to "日付電卓"
        ),
        "Weight (kg)" to mapOf(
            "Türkçe" to "Weight (kg)",
            "English" to "Weight (kg",
            "Türkçe" to "Ağırlık (kg)",
            "Español" to "Peso (kg)",
            "Deutsch" to "Gewicht (kg)",
            "Français" to "Poids (kg)",
            "Русский" to "Вес (кг)",
            "العربية" to "الوزن (كجم)",
            "中文" to "重量 (kg)",
            "日本語" to "体重 (kg)"
        ),
        "Loan" to mapOf(
            "English" to "Loan",
            "Türkçe" to "Kredi",
            "Español" to "Préstamo",
            "Deutsch" to "Kredit",
            "Français" to "Prêt",
            "Русский" to "Кредит",
            "العربية" to "قرض",
            "中文" to "贷款",
            "日本語" to "ローン"
        ),
        "Side Length (a)" to mapOf(
            "Türkçe" to "Side Length (a)",
            "English" to "Side Length (a",
            "Türkçe" to "Kenar (a)",
            "Español" to "Lado (a)",
            "Deutsch" to "Seitenlänge (a)",
            "Français" to "Côté (a)",
            "Русский" to "Сторона (a)",
            "العربية" to "طول الضلع (a)",
            "中文" to "边长 (a)",
            "日本語" to "辺の長さ (a)"
        ),
        "Original Price" to mapOf(
            "English" to "Original Price",
            "Türkçe" to "Orijinal Fiyat",
            "Español" to "Precio Original",
            "Deutsch" to "Originalpreis",
            "Français" to "Prix Original",
            "Русский" to "Начальная цена",
            "العربية" to "السعر الأصلي",
            "中文" to "原价",
            "日本語" to "元の価格"
        ),
        "Height (b)" to mapOf(
            "Türkçe" to "Height (b)",
            "English" to "Height (b",
            "Türkçe" to "Yükseklik (b)",
            "Español" to "Altura (b)",
            "Deutsch" to "Höhe (b)",
            "Français" to "Hauteur (b)",
            "Русский" to "Высота (b)",
            "العربية" to "الارتفاع (b)",
            "中文" to "高度 (b)",
            "日本語" to "高さ (b)"
        ),
        "Height (h)" to mapOf(
            "Türkçe" to "Height (h)",
            "English" to "Height (h",
            "Türkçe" to "Yükseklik (h)",
            "Español" to "Altura (h)",
            "Deutsch" to "Höhe (h)",
            "Français" to "Hauteur (h)",
            "Русский" to "Высота (h)",
            "العربية" to "الارتفاع (h)",
            "中文" to "高度 (h)",
            "日本語" to "高さ (h)"
        ),
        "GPA / Exam" to mapOf(
            "English" to "GPA / Exam",
            "Türkçe" to "Not / GPA",
            "Español" to "Notas / GPA",
            "Deutsch" to "Noten / GPA",
            "Français" to "Notes / GPA",
            "Русский" to "Оценки / GPA",
            "العربية" to "المعدل التراكمي",
            "中文" to "GPA / 考试",
            "日本語" to "GPA / 試験"
        ),
        "Width (a)" to mapOf(
            "Türkçe" to "Width (a)",
            "English" to "Width (a",
            "Türkçe" to "Genişlik (a)",
            "Español" to "Ancho (a)",
            "Deutsch" to "Breite (a)",
            "Français" to "Largeur (a)",
            "Русский" to "Ширина (a)",
            "العربية" to "العرض (a)",
            "中文" to "宽度 (a)",
            "日本語" to "幅 (a)"
        ),
        "Loan Term (Months)" to mapOf(
            "Türkçe" to "Loan Term (Months)",
            "English" to "Loan Term (Months",
            "Türkçe" to "Vade (Ay)",
            "Español" to "Plazo (Meses)",
            "Deutsch" to "Laufzeit (Monate)",
            "Français" to "Durée (Mois)",
            "Русский" to "Срок (месяцев)",
            "العربية" to "مدة القرض (بالأشهر)",
            "中文" to "贷款期限（月）",
            "日本語" to "借入期間（月）"
        ),
        "Radius (r)" to mapOf(
            "Türkçe" to "Radius (r)",
            "English" to "Radius (r",
            "Türkçe" to "Yarıçap (r)",
            "Español" to "Radio (r)",
            "Deutsch" to "Radius (r)",
            "Français" to "Rayon (r)",
            "Русский" to "Радиус (r)",
            "العربية" to "نصف القطر (r)",
            "中文" to "半径 (r)",
            "日本語" to "半径 (r)"
        ),
        "Height (cm)" to mapOf(
            "Türkçe" to "Height (cm)",
            "English" to "Height (cm",
            "Türkçe" to "Boy (cm)",
            "Español" to "Altura (cm)",
            "Deutsch" to "Größe (cm)",
            "Français" to "Taille (cm)",
            "Русский" to "Рост (см)",
            "العربية" to "الطول (سم)",
            "中文" to "身高 (cm)",
            "日本語" to "身長 (cm)"
        ),
        "Annual Interest Rate (%)" to mapOf(
            "Türkçe" to "Annual Interest Rate (%)",
            "English" to "Annual Interest Rate (%",
            "Türkçe" to "Faiz Oranı (%)",
            "Español" to "Tasa de Interés (%)",
            "Deutsch" to "Zinssatz (%)",
            "Français" to "Taux d'Intérêt (%)",
            "Русский" to "Процентная ставка (%)",
            "العربية" to "معدل الفائدة (%)",
            "中文" to "年利率 (%)",
            "日本語" to "年利 (%)"
        ),
        "Tax Rate (%)" to mapOf(
            "Türkçe" to "Tax Rate (%)",
            "English" to "Tax Rate (%",
            "Türkçe" to "Vergi Oranı (%)",
            "Español" to "Tasa de Impuesto (%)",
            "Deutsch" to "Steuersatz (%)",
            "Français" to "Taux d'Imposition (%)",
            "Русский" to "Налоговая ставка (%)",
            "العربية" to "معدل الضريبة (%)",
            "中文" to "税率 (%)",
            "日本語" to "税率 (%)"
        ),
        "Exam Score (0-100)" to mapOf(
            "Türkçe" to "Exam Score (0-100)",
            "English" to "Exam Score (0-100",
            "Türkçe" to "Sınav Notu",
            "Español" to "Nota del Examen",
            "Deutsch" to "Prüfungsergebnis",
            "Français" to "Note d'Examen",
            "Русский" to "Оценка (0-100)",
            "العربية" to "درجة الامتحان",
            "中文" to "考试分数",
            "日本語" to "試験のスコア"
        ),
        "Base (b)" to mapOf(
            "Türkçe" to "Base (b)",
            "English" to "Base (b",
            "Türkçe" to "Taban (b)",
            "Español" to "Base (b)",
            "Deutsch" to "Basis (b)",
            "Français" to "Base (b)",
            "Русский" to "Основание (b)",
            "العربية" to "القاعدة (b)",
            "中文" to "底边 (b)",
            "日本語" to "底辺 (b)"
        ),
        "Currency Converter" to mapOf(
            "English" to "Currency Converter",
            "Türkçe" to "Döviz Çevirici",
            "Español" to "Conversor de Moneda",
            "Deutsch" to "Währungsrechner",
            "Français" to "Convertisseur de Devises",
            "Русский" to "Конвертер валют",
            "العربية" to "محول العملات",
            "中文" to "货币转换器",
            "日本語" to "通貨コンバータ"
        ),
        "Tip & Split (%)" to mapOf(
            "Türkçe" to "Tip & Split (%)",
            "English" to "Tip & Split (%",
            "Türkçe" to "Bahşiş Oranı (%)",
            "Español" to "Propina (%)",
            "Deutsch" to "Trinkgeld (%)",
            "Français" to "Pourboire (%)",
            "Русский" to "Чаевые (%)",
            "العربية" to "نسبة البقشيش (%)",
            "中文" to "小费 (%)",
            "日本語" to "チップ (%)"
        ),
        "Loan Amount" to mapOf(
            "English" to "Loan Amount",
            "Türkçe" to "Kredi Tutarı",
            "Español" to "Monto del Préstamo",
            "Deutsch" to "Kreditbetrag",
            "Français" to "Montant du Prêt",
            "Русский" to "Сумма кредита",
            "العربية" to "مبلغ القرض",
            "中文" to "贷款金额",
            "日本語" to "借入額"
        ),
        "Sales Tax / VAT" to mapOf(
            "English" to "Sales Tax / VAT",
            "Türkçe" to "KDV / Vergi",
            "Español" to "Impuesto / IVA",
            "Deutsch" to "Mehrwertsteuer",
            "Français" to "TVA",
            "Русский" to "НДС",
            "العربية" to "ضريبة المبيعات",
            "中文" to "销售税 / 增值税",
            "日本語" to "消費税 / VAT"
        ),
        "Amount" to mapOf(
            "English" to "Amount",
            "Türkçe" to "Tutar",
            "Español" to "Monto",
            "Deutsch" to "Betrag",
            "Français" to "Montant",
            "Русский" to "Сумма",
            "العربية" to "المبلغ",
            "中文" to "金额",
            "日本語" to "金額"
        ),
        "Shapes & Area" to mapOf(
            "English" to "Shapes & Area",
            "Türkçe" to "Şekiller & Alan",
            "Español" to "Formas y Área",
            "Deutsch" to "Formen & Fläche",
            "Français" to "Formes & Surface",
            "Русский" to "Фигуры и площадь",
            "العربية" to "الأشكال والمساحة",
            "中文" to "形状与面积",
            "日本語" to "図形と面積"
        ),
        "Discount Oranı (%)" to mapOf(
            "Türkçe" to "Discount Oranı (%)",
            "English" to "Discount Rate (%",
            "Türkçe" to "İndirim Oranı (%)",
            "Español" to "Descuento (%)",
            "Deutsch" to "Rabatt (%)",
            "Français" to "Remise (%)",
            "Русский" to "Скидка (%)",
            "العربية" to "نسبة الخصم (%)",
            "中文" to "折扣 (%)",
            "日本語" to "割引率 (%)"
        ),
        "Tip & Split" to mapOf(
            "English" to "Tip & Split",
            "Türkçe" to "Bahşiş & Hesap Bölme",
            "Español" to "Propina y Dividir",
            "Deutsch" to "Trinkgeld & Teilen",
            "Français" to "Pourboire & Partager",
            "Русский" to "Чаевые и разделение счета",
            "العربية" to "البقشيش وتقسيم الفاتورة",
            "中文" to "小费与平摊",
            "日本語" to "チップと割り勘"
        ),
        "Discount" to mapOf(
            "English" to "Discount",
            "Türkçe" to "İndirim",
            "Español" to "Descuento",
            "Deutsch" to "Rabatt",
            "Français" to "Remise",
            "Русский" to "Скидка",
            "العربية" to "خصم",
            "中文" to "折扣",
            "日本語" to "割引"
        ),
        "Number of People" to mapOf(
            "English" to "Number of People",
            "Türkçe" to "Kişi Sayısı",
            "Español" to "Número de Personas",
            "Deutsch" to "Anzahl der Personen",
            "Français" to "Nombre de Personnes",
            "Русский" to "Количество человек",
            "العربية" to "عدد الأشخاص",
            "中文" to "人数",
            "日本語" to "人数"
        ),
        "Hesap Amountı" to mapOf(
            "English" to "Bill Amount",
            "Türkçe" to "Hesap Tutarı",
            "Español" to "Total de la Cuenta",
            "Deutsch" to "Rechnungsbetrag",
            "Français" to "Total de l'addition",
            "Русский" to "Сумма счета",
            "العربية" to "إجمالي الفاتورة",
            "中文" to "账单金额",
            "日本語" to "請求額"
        ),
        "From" to mapOf(
            "English" to "From",
            "Türkçe" to "Kaynak",
            "Español" to "Desde",
            "Deutsch" to "Von",
            "Français" to "De",
            "Русский" to "Из",
            "العربية" to "من",
            "中文" to "从",
            "日本語" to "元"
        ),
        "To" to mapOf(
            "English" to "To",
            "Türkçe" to "Hedef",
            "Español" to "Hacia",
            "Deutsch" to "Nach",
            "Français" to "À",
            "Русский" to "В",
            "العربية" to "إلى",
            "中文" to "到",
            "日本語" to "先"
        ),

        "Widget Yönetimi" to mapOf(
            "Türkçe" to "Widget Yönetimi",
            "English" to "Widget Management",
            "Español" to "Gestión de Widgets",
            "Deutsch" to "Widget-Verwaltung",
            "Français" to "Gestion des Widgets",
            "Русский" to "Управление виджетами",
            "العربية" to "إدارة الأدوات",
            "中文" to "小部件管理",
            "日本語" to "ウィジェット管理"
        ),
        "Hangi araçların widget listesinde görüneceğini seçin." to mapOf(
            "Türkçe" to "Hangi araçların widget listesinde görüneceğini seçin.",
            "English" to "Select which tools appear in the widget list.",
            "Español" to "Seleccione qué herramientas aparecen en la lista.",
            "Deutsch" to "Wählen Sie aus, welche Tools in der Widget-Liste angezeigt werden.",
            "Français" to "Sélectionnez les outils qui apparaissent dans la liste.",
            "Русский" to "Выберите, какие инструменты будут отображаться.",
            "العربية" to "حدد الأدوات التي تظهر في القائمة.",
            "中文" to "选择显示在列表中的工具。",
            "日本語" to "リストに表示するツールを選択します。"
        ),
        "Ayarlar" to mapOf(
            "Türkçe" to "Ayarlar",
            "English" to "Settings",
            "Español" to "Ajustes",
            "Deutsch" to "Einstellungen",
            "Français" to "Paramètres",
            "Русский" to "Настройки",
            "العربية" to "الإعدادات",
            "中文" to "设置",
            "日本語" to "設定"
        ),
        "Titreşim Geri Bildirimi" to mapOf(
            "Türkçe" to "Titreşim Geri Bildirimi",
            "English" to "Vibration Feedback",
            "Español" to "Respuesta de Vibración",
            "Deutsch" to "Vibrationsfeedback",
            "Français" to "Retour de Vibration",
            "Русский" to "Виброотклик",
            "العربية" to "ملاحظات الاهتزاز",
            "中文" to "振动反馈",
            "日本語" to "振動フィードバック"
        ),
        "Tuşlara basıldığında dokunsal geri bildirim sağlar." to mapOf(
            "Türkçe" to "Tuşlara basıldığında dokunsal geri bildirim sağlar.",
            "English" to "Provides haptic feedback on key presses.",
            "Español" to "Proporciona respuesta háptica al presionar teclas.",
            "Deutsch" to "Bietet haptisches Feedback beim Tastendruck.",
            "Français" to "Fournit un retour haptique lors de la pression.",
            "Русский" to "Обеспечивает тактильный отклик.",
            "العربية" to "يوفر ردود فعل لمسية عند الضغط على المفاتيح.",
            "中文" to "按下按键时提供触觉反馈。",
            "日本語" to "キー入力時に触覚フィードバックを提供します。"
        ),
        "Tuş Sesleri" to mapOf(
            "Türkçe" to "Tuş Sesleri",
            "English" to "Key Sounds",
            "Español" to "Sonidos de Teclas",
            "Deutsch" to "Tastentöne",
            "Français" to "Sons des Touches",
            "Русский" to "Звуки клавиш",
            "العربية" to "أصوات المفاتيح",
            "中文" to "按键音",
            "日本語" to "キー音"
        ),
        "Ses Seviyesi" to mapOf(
            "Türkçe" to "Ses Seviyesi",
            "English" to "Volume",
            "Español" to "Volumen",
            "Deutsch" to "Lautstärke",
            "Français" to "Volume",
            "Русский" to "Громкость",
            "العربية" to "مستوى الصوت",
            "中文" to "音量",
            "日本語" to "音量"
        ),
        "Tarih Hesaplayıcı" to mapOf(
            "Türkçe" to "Tarih Hesaplayıcı",
            "English" to "Date Calculator",
            "Español" to "Calculadora de Fecha",
            "Deutsch" to "Datumsrechner",
            "Français" to "Calculatrice de Date",
            "Русский" to "Калькулятор дат",
            "العربية" to "حاسبة التاريخ",
            "中文" to "日期计算器",
            "日本語" to "日付電卓"
        ),
        "Fark Bul" to mapOf(
            "Türkçe" to "Fark Bul",
            "English" to "Find Difference",
            "Español" to "Encontrar Diferencia",
            "Deutsch" to "Differenz finden",
            "Français" to "Trouver la Différence",
            "Русский" to "Найти разницу",
            "العربية" to "أوجد الفرق",
            "中文" to "计算差值",
            "日本語" to "差を計算する"
        ),
        "Tarih Ekle/Çıkar" to mapOf(
            "Türkçe" to "Tarih Ekle/Çıkar",
            "English" to "Add/Subtract Date",
            "Español" to "Añadir/Restar Fecha",
            "Deutsch" to "Datum addieren/subtrahieren",
            "Français" to "Ajouter/Soustraire Date",
            "Русский" to "Добавить/Вычесть дату",
            "العربية" to "إضافة/طرح تاريخ",
            "中文" to "添加/减少日期",
            "日本語" to "日付の加算/減算"
        ),
        "Başlangıç Tarihi" to mapOf(
            "Türkçe" to "Başlangıç Tarihi",
            "English" to "Start Date",
            "Español" to "Fecha de Inicio",
            "Deutsch" to "Startdatum",
            "Français" to "Date de Début",
            "Русский" to "Начальная дата",
            "العربية" to "تاريخ البدء",
            "中文" to "开始日期",
            "日本語" to "開始日"
        ),
        "Bitiş Tarihi" to mapOf(
            "Türkçe" to "Bitiş Tarihi",
            "English" to "End Date",
            "Español" to "Fecha de Fin",
            "Deutsch" to "Enddatum",
            "Français" to "Date de Fin",
            "Русский" to "Конечная дата",
            "العربية" to "تاريخ الانتهاء",
            "中文" to "结束日期",
            "日本語" to "終了日"
        ),
        "Gün" to mapOf(
            "Türkçe" to "Gün",
            "English" to "Day", "Español" to "Día", "Deutsch" to "Tag", "Français" to "Jour", "Русский" to "День", "العربية" to "يوم", "中文" to "天", "日本語" to "日"
        ),
        "Ay" to mapOf(
            "Türkçe" to "Ay",
            "English" to "Month", "Español" to "Mes", "Deutsch" to "Monat", "Français" to "Mois", "Русский" to "Месяц", "العربية" to "شهر", "中文" to "月", "日本語" to "月"
        ),
        "Yıl" to mapOf(
            "Türkçe" to "Yıl",
            "English" to "Year", "Español" to "Año", "Deutsch" to "Jahr", "Français" to "Année", "Русский" to "Год", "العربية" to "سنة", "中文" to "年", "日本語" to "年"
        ),
        "Hesapla" to mapOf(
            "Türkçe" to "Hesapla",
            "English" to "Calculate", "Español" to "Calcular", "Deutsch" to "Berechnen", "Français" to "Calculer", "Русский" to "Вычислить", "العربية" to "احسب", "中文" to "计算", "日本語" to "計算する"
        ),
        "Sonuç: " to mapOf(
            "Türkçe" to "Sonuç: ",
            "English" to "Result: ", "Español" to "Resultado: ", "Deutsch" to "Ergebnis: ", "Français" to "Résultat : ", "Русский" to "Результат: ", "العربية" to "النتيجة: ", "中文" to "结果: ", "日本語" to "結果: "
        ),
        "İşlem:" to mapOf(
            "Türkçe" to "İşlem:",
            "English" to "Operation:", "Español" to "Operación:", "Deutsch" to "Operation:", "Français" to "Opération :", "Русский" to "Действие:", "العربية" to "العملية:", "中文" to "操作:", "日本語" to "操作:"
        ),
        "Ekle (+)" to mapOf(
            "Türkçe" to "Ekle (+)",
            "English" to "Add (+", "Español" to "Añadir (+)", "Deutsch" to "Addieren (+)", "Français" to "Ajouter (+)", "Русский" to "Прибавить (+)", "العربية" to "إضافة (+)", "中文" to "添加 (+)", "日本語" to "加算 (+)"
        ),
        "Çıkar (-)" to mapOf(
            "Türkçe" to "Çıkar (-)",
            "English" to "Subtract (-", "Español" to "Restar (-)", "Deutsch" to "Subtrahieren (-)", "Français" to "Soustraire (-)", "Русский" to "Вычесть (-)", "العربية" to "طرح (-)", "中文" to "减少 (-)", "日本語" to "減算 (-)"
        ),
        "Hedef Tarih" to mapOf(
            "Türkçe" to "Hedef Tarih",
            "English" to "Target Date", "Español" to "Fecha Objetivo", "Deutsch" to "Zieldatum", "Français" to "Date Cible", "Русский" to "Целевая дата", "العربية" to "التاريخ المستهدف", "中文" to "目标日期", "日本語" to "目標日"
        ),
        "Geçersiz değerler!" to mapOf(
            "Türkçe" to "Geçersiz değerler!",
            "English" to "Invalid values!", "Español" to "¡Valores inválidos!", "Deutsch" to "Ungültige Werte!", "Français" to "Valeurs invalides!", "Русский" to "Неверные значения!", "العربية" to "قيم غير صالحة!", "中文" to "无效的值！", "日本語" to "無効な値です！"
        ),
        "Kendi Denklemlerim" to mapOf(
            "Türkçe" to "Kendi Denklemlerim",
            "English" to "Custom Equations",
            "Español" to "Ecuaciones Personalizadas",
            "Deutsch" to "Benutzerdefinierte Gleichungen",
            "Français" to "Équations Personnalisées",
            "Русский" to "Свои уравнения",
            "العربية" to "معادلات مخصصة",
            "中文" to "自定义方程式",
            "日本語" to "カスタム方程式"
        ),
        "Custom Equations" to mapOf(
            "Türkçe" to "Kendi Denklemlerim",
            "English" to "Custom Equations",
            "Español" to "Ecuaciones Personalizadas",
            "Deutsch" to "Benutzerdefinierte Gleichungen",
            "Français" to "Équations Personnalisées",
            "Русский" to "Свои уравнения",
            "العربية" to "معادلات مخصصة",
            "中文" to "自定义方程式",
            "日本語" to "カスタム方程式"
        ),
        "Yeni Denklem Ekle" to mapOf(
            "Türkçe" to "Yeni Denklem Ekle",
            "English" to "Add New Equation",
            "Español" to "Añadir Nueva Ecuación",
            "Deutsch" to "Neue Gleichung Hinzufügen",
            "Français" to "Ajouter une Nouvelle Équation",
            "Русский" to "Добавить новое уравнение",
            "العربية" to "إضافة معادلة جديدة",
            "中文" to "添加新方程式",
            "日本語" to "新しい方程式を追加"
        ),
        "+ Yeni Denklem Ekle" to mapOf(
            "Türkçe" to "+ Yeni Denklem Ekle",
            "English" to "+ Add New Equation",
            "Español" to "+ Añadir Nueva Ecuación",
            "Deutsch" to "+ Neue Gleichung Hinzufügen",
            "Français" to "+ Ajouter une Nouvelle Équation",
            "Русский" to "+ Добавить новое уравнение",
            "العربية" to "+ إضافة معادلة جديدة",
            "中文" to "+ 添加新方程式",
            "日本語" to "+ 新しい方程式を追加"
        ),
        "İsim (örn: Kinetik Enerji)" to mapOf(
            "Türkçe" to "İsim (örn: Kinetik Enerji)",
            "English" to "Name (e.g. Kinetic Energy)",
            "Español" to "Nombre (ej. Energía Cinética)",
            "Deutsch" to "Name (z.B. Kinetische Energie)",
            "Français" to "Nom (ex. Énergie Cinétique)",
            "Русский" to "Имя (напр. Кинетическая энергия)",
            "العربية" to "الاسم (مثل الطاقة الحركية)",
            "中文" to "名称 (例如 动能)",
            "日本語" to "名前 (例: 運動エネルギー)"
        ),
        "Formül (örn: 0.5 * x * x)" to mapOf(
            "Türkçe" to "Formül (örn: 0.5 * x * x)",
            "English" to "Formula (e.g. 0.5 * x * x)",
            "Español" to "Fórmula (ej. 0.5 * x * x)",
            "Deutsch" to "Formel (z.B. 0.5 * x * x)",
            "Français" to "Formule (ex. 0.5 * x * x)",
            "Русский" to "Формула (напр. 0.5 * x * x)",
            "العربية" to "المعادلة (مثل 0.5 * x * x)",
            "中文" to "公式 (例如 0.5 * x * x)",
            "日本語" to "式 (例: 0.5 * x * x)"
        ),
        "Değişken olarak 'x' kullanabilirsiniz." to mapOf(
            "Türkçe" to "Değişken olarak 'x' kullanabilirsiniz.",
            "English" to "You can use 'x' as a variable.",
            "Español" to "Puedes usar 'x' como variable.",
            "Deutsch" to "Sie können 'x' als Variable verwenden.",
            "Français" to "Vous pouvez utiliser 'x' comme variable.",
            "Русский" to "Вы можете использовать 'x' как переменную.",
            "العربية" to "يمكنك استخدام 'x' كمتغير.",
            "中文" to "你可以使用 'x' 作为变量。",
            "日本語" to "変数として 'x' を使用できます。"
        ),
        "Kaydet" to mapOf(
            "Türkçe" to "Kaydet",
            "English" to "Save",
            "Español" to "Guardar",
            "Deutsch" to "Speichern",
            "Français" to "Enregistrer",
            "Русский" to "Сохранить",
            "العربية" to "حفظ",
            "中文" to "保存",
            "日本語" to "保存"
        ),
        "İptal" to mapOf(
            "Türkçe" to "İptal",
            "English" to "Cancel",
            "Español" to "Cancelar",
            "Deutsch" to "Abbrechen",
            "Français" to "Annuler",
            "Русский" to "Отмена",
            "العربية" to "إلغاء",
            "中文" to "取消",
            "日本語" to "キャンセル"
        ),
        "Henüz kayıtlı bir denkleminiz yok." to mapOf(
            "Türkçe" to "Henüz kayıtlı bir denkleminiz yok.",
            "English" to "You have no saved equations yet.",
            "Español" to "Aún no tienes ecuaciones guardadas.",
            "Deutsch" to "Sie haben noch keine gespeicherten Gleichungen.",
            "Français" to "Vous n'avez pas encore d'équations enregistrées.",
            "Русский" to "У вас пока нет сохраненных уравнений.",
            "العربية" to "ليس لديك معادلات محفوظة بعد.",
            "中文" to "您还没有保存的方程式。",
            "日本語" to "保存された方程式はまだありません。"
        ),
        "Pipeline (Zincirleme Çözüm)" to mapOf(
            "Türkçe" to "Pipeline (Zincirleme Çözüm)",
            "English" to "Pipeline (Chained Solution)",
            "Español" to "Canalización (Solución Encadenada)",
            "Deutsch" to "Pipeline (Verkettete Lösung)",
            "Français" to "Pipeline (Solution Chaînée)",
            "Русский" to "Конвейер (Цепное решение)",
            "العربية" to "خط الأنابيب (حل متسلسل)",
            "中文" to "流水线（链式解决方案）",
            "日本語" to "パイプライン（連鎖的な解決策）"
        ),
        "Aşağıdaki tüm denklemleri sırasıyla birbirine bağlar. (Çıktı -> Girdi)" to mapOf(
            "Türkçe" to "Aşağıdaki tüm denklemleri sırasıyla birbirine bağlar. (Çıktı -> Girdi)",
            "English" to "Connects all the equations below sequentially. (Output -> Input)",
            "Español" to "Conecta todas las ecuaciones a continuación de forma secuencial. (Salida -> Entrada)",
            "Deutsch" to "Verbindet alle untenstehenden Gleichungen nacheinander. (Ausgabe -> Eingabe)",
            "Français" to "Connecte toutes les équations ci-dessous de manière séquentielle. (Sortie -> Entrée)",
            "Русский" to "Последовательно связывает все уравнения ниже. (Выход -> Вход)",
            "العربية" to "يربط جميع المعادلات أدناه بالتسلسل. (مخرجات -> مدخلات)",
            "中文" to "依次连接以下所有方程式。（输出 -> 输入）",
            "日本語" to "以下のすべての方程式を順番に接続します。（出力 -> 入力）"
        ),
        "Başlangıç x değeri" to mapOf(
            "Türkçe" to "Başlangıç x değeri",
            "English" to "Starting x value",
            "Español" to "Valor x inicial",
            "Deutsch" to "Startwert x",
            "Français" to "Valeur x de départ",
            "Русский" to "Начальное значение x",
            "العربية" to "قيمة x الأولية",
            "中文" to "初始 x 值",
            "日本語" to "初期 x 値"
        ),
        "x değeri" to mapOf(
            "Türkçe" to "x değeri",
            "English" to "x value",
            "Español" to "Valor x",
            "Deutsch" to "x-Wert",
            "Français" to "Valeur x",
            "Русский" to "Значение x",
            "العربية" to "قيمة x",
            "中文" to "x 值",
            "日本語" to "x 値"
        ),
        "Final Sonuç:" to mapOf(
            "Türkçe" to "Final Sonuç:",
            "English" to "Final Result:",
            "Español" to "Resultado Final:",
            "Deutsch" to "Endergebnis:",
            "Français" to "Résultat Final :",
            "Русский" to "Итоговый результат:",
            "العربية" to "النتيجة النهائية:",
            "中文" to "最终结果:",
            "日本語" to "最終結果:"
        ),
        "Zincirde" to mapOf(
            "Türkçe" to "Zincirde",
            "English" to "In Pipeline",
            "Español" to "En Canalización",
            "Deutsch" to "In Pipeline",
            "Français" to "Dans Pipeline",
            "Русский" to "В конвейере",
            "العربية" to "في خط الأنابيب",
            "中文" to "在流水线中",
            "日本語" to "パイプライン内"
        ),
        "Square" to mapOf(
            "Türkçe" to "Kare",
            "English" to "Square",
            "Español" to "Cuadrado",
            "Deutsch" to "Quadrat",
            "Français" to "Carré",
            "Русский" to "Квадрат",
            "العربية" to "مربع",
            "中文" to "正方形",
            "日本語" to "正方形"
        ),
        "Rectangle" to mapOf(
            "Türkçe" to "Dikdörtgen",
            "English" to "Rectangle",
            "Español" to "Rectángulo",
            "Deutsch" to "Rechteck",
            "Français" to "Rectangle",
            "Русский" to "Прямоугольник",
            "العربية" to "مستطيل",
            "中文" to "长方形",
            "日本語" to "長方形"
        ),
        "Circle" to mapOf(
            "Türkçe" to "Daire",
            "English" to "Circle",
            "Español" to "Círculo",
            "Deutsch" to "Kreis",
            "Français" to "Cercle",
            "Русский" to "Круг",
            "العربية" to "دائرة",
            "中文" to "圆形",
            "日本語" to "円"
        ),
        "Triangle" to mapOf(
            "Türkçe" to "Üçgen",
            "English" to "Triangle",
            "Español" to "Triángulo",
            "Deutsch" to "Dreieck",
            "Français" to "Triangle",
            "Русский" to "Треугольник",
            "العربية" to "مثلث",
            "中文" to "三角形",
            "日本語" to "三角形"
        ),
        "Aylık Faiz" to mapOf(
            "Türkçe" to "Aylık Faiz",
            "English" to "Monthly Rate",
            "Español" to "Tasa Mensual",
            "Deutsch" to "Monatlicher Zins",
            "Français" to "Taux Mensuel",
            "Русский" to "Ежемесячная ставка",
            "العربية" to "سعر الفائدة الشهري",
            "中文" to "月利率",
            "日本語" to "月利"
        ),
        "Yıllık Faiz" to mapOf(
            "Türkçe" to "Yıllık Faiz",
            "English" to "Annual Rate",
            "Español" to "Tasa Anual",
            "Deutsch" to "Jährlicher Zins",
            "Français" to "Taux Annuel",
            "Русский" to "Годовая ставка",
            "العربية" to "سعر الفائدة السنوي",
            "中文" to "年利率",
            "日本語" to "年利"
        ),
        "Monthly Interest Rate (%)" to mapOf(
            "Türkçe" to "Aylık Faiz Oranı (%)",
            "English" to "Monthly Interest Rate (%)",
            "Español" to "Tasa de Interés Mensual (%)",
            "Deutsch" to "Monatlicher Zinssatz (%)",
            "Français" to "Taux d'Intérêt Mensuel (%)",
            "Русский" to "Ежемесячная процентная ставка (%)",
            "العربية" to "معدل الفائدة الشهري (%)",
            "中文" to "月利率 (%)",
            "日本語" to "月利 (%)"
        ),
        "Dil Seçimi (Language)" to mapOf(
            "Türkçe" to "Dil Seçimi (Language)",
            "English" to "Language", "Español" to "Idioma", "Deutsch" to "Sprache", "Français" to "Langue", "Русский" to "Язык", "العربية" to "اللغة", "中文" to "语言", "日本語" to "言語"
        )
    )

    fun tr(text: String): String {
        val entry = dictionary[text] ?: casTranslationsPart1[text] ?: casTranslationsPart2[text] ?: casTranslationsPart3[text] ?: return text
        return entry[currentLang] ?: entry["English"] ?: text
    }
}
