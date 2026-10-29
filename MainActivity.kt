package ru.momnotes.app

import android.Manifest
import android.app.Activity
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.ComponentName
import android.content.pm.PackageManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.Settings
import android.os.Bundle
import android.os.Build
import android.widget.Toast
import android.speech.RecognizerIntent
import android.util.Base64
import android.util.Base64OutputStream
import android.webkit.MimeTypeMap
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Switch
import androidx.compose.material3.Slider
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.zIndex
import kotlin.math.roundToInt
import androidx.compose.ui.unit.sp
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.BufferedWriter
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Calendar
import java.util.Locale
import java.util.UUID
import java.util.concurrent.Executors
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import androidx.compose.runtime.produceState
import io.appwrite.enums.OAuthProvider
import androidx.core.content.FileProvider
import androidx.core.content.ContextCompat

private val categories = listOf("Важное")
private val retiredDefaultCategories = setOf(
    "Работа", "Бассейн", "Подготовка к занятию", "Подготовка к индивидуальному занятию"
)

internal fun normalizeSectionName(name: String): String =
    if (name.trim() in retiredDefaultCategories) "Важное" else name.trim()

private data class TableCellAddress(val row: Int, val column: Int)

internal data class AppSettings(
    val theme: String = "Тёмная",
    val accent: String = "Зелёный",
    val headerColor: String = "",
    val sectionColor: String = "",
    val noteColor: String = "",
    val addButtonColor: String = "",
    val tableColor: String = "",
    val iconBackground: String = "Зелёный",
    val noteTextSize: String = "Средний",
    val density: String = "Просторно",
    val corner: String = "Среднее",
    val sortBy: String = "По дате изменения",
    val listStyle: String = "Карточки",
    val autoSave: Boolean = false,
    val confirmDelete: Boolean = true,
    val showModifiedDate: Boolean = true,
    val favoriteBehavior: String = "Поднимать наверх",
    val aiTransfer: String = "Открыть приложение ИИ",
    val language: String = "Русский",
    val vibration: Boolean = true,
    val voiceLanguage: String = "Русский"
)



internal data class NoteTable(
    val id: String = UUID.randomUUID().toString(),
    val cells: List<List<String>>
)

internal data class Note(
    val id: String,
    val category: String,
    val title: String,
    val body: String,
    val updatedAt: Long,
    val createdAt: Long = updatedAt,
    val isFavorite: Boolean = false,
    val isDeleted: Boolean = false,
    val photoPaths: List<String> = emptyList(),
    val attachmentPaths: List<String> = emptyList(),
    val photoIds: List<String> = emptyList(),
    val attachmentIds: List<String> = emptyList(),
    val tables: List<NoteTable> = emptyList(),
    val aiAssistant: String = "Без ИИ",
    val aiInstruction: String = "",
    val reminderAt: Long = 0L,
    val reminders: List<Long> = emptyList(),
    val reminderTypes: Map<Long, String> = emptyMap()
) {
    val plainBody: String by lazy(LazyThreadSafetyMode.NONE) { RichTextDocument.plainText(body) }
}

private data class DraftSnapshot(val title: String, val body: String)

private data class NoteListItem(val note: Note, val plainBody: String)

private class NoteDragState {
    var origin: Offset = Offset.Zero
    var pointer: Offset = Offset.Zero
    var offset: Offset = Offset.Zero
    val pointerState = mutableStateOf(Offset.Zero)
    val offsetState = mutableStateOf(Offset.Zero)
}

private val launcherIconAliases = listOf(
    "Красный" to "Red", "Оранжевый" to "Orange", "Жёлтый" to "Yellow",
    "Зелёный" to "Green", "Бирюзовый" to "Teal", "Голубой" to "Sky",
    "Синий" to "Blue", "Фиолетовый" to "Purple", "Розовый" to "Pink",
    "Терракотовый" to "Terra", "Серый" to "Gray", "Чёрный" to "Black"
)

private fun setLauncherIconColor(context: Context, colorName: String) {
    val selected = launcherIconAliases.firstOrNull { it.first == colorName }?.second ?: "Green"
    val manager = context.packageManager
    launcherIconAliases.forEach { (_, suffix) ->
        val component = ComponentName(context, "${context.packageName}.MainActivityIcon$suffix")
        manager.setComponentEnabledSetting(
            component,
            if (suffix == selected) PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            else PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.DONT_KILL_APP
        )
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.rgb(21, 59, 41)
        window.navigationBarColor = android.graphics.Color.rgb(16, 22, 18)
        AppwriteService.initialize(applicationContext)
        setLauncherIconColor(applicationContext, readAppSettings(applicationContext).iconBackground)
        setContent { NotesRoot() }
    }

}

@Composable
private fun NotesRoot() {
    val context = LocalContext.current
    var settings by remember { mutableStateOf(readAppSettings(context)) }
    var showStartupAnimation by remember { mutableStateOf(true) }
    val startupColor = remember(settings.iconBackground, settings.accent) {
        themeColorChoices.firstOrNull { it.first == settings.iconBackground }?.second
            ?.let(::parseHexColor)
            ?: accentColor(settings.accent)
    }
    val scope = rememberCoroutineScope()
    var authenticated by remember { mutableStateOf<Boolean?>(null) }
    var accountId by remember { mutableStateOf<String?>(null) }
    var isGuest by remember { mutableStateOf(false) }
    var showWelcome by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        val prefs = context.getSharedPreferences("mom_notes", Context.MODE_PRIVATE)
        if (prefs.getBoolean("active_guest", false)) {
            isGuest = true
            accountId = "guest"
            authenticated = true
            return@LaunchedEffect
        }
        authenticated = try {
            val user = AppwriteService.account.get()
            accountId = user.id
            isGuest = false
            showWelcome = prefs.getString("welcome_pending_user", null) == user.id ||
                    !prefs.getBoolean("welcome_done_${user.id}", false)
            prefs.edit().putString("last_auth_user_id", user.id)
                .putString("last_auth_email", user.email).apply()
            if (!prefs.contains("cloud_migration_owner")) {
                prefs.edit().putString("cloud_migration_owner", user.id).apply()
            }
            true
        } catch (_: Exception) {
            accountId = prefs.getString("last_auth_user_id", null)
            accountId != null
        }
    }
    val isDark = when (settings.theme) {
        "Светлая" -> false
        "Системная" -> isSystemInDarkTheme()
        else -> true
    }
    SideEffect {
        (context as? Activity)?.window?.let { window ->
            val header = roleColor(settings.headerColor, accentColor(settings.accent))
            window.statusBarColor = (if (showStartupAnimation) StartupAnimationBackground else header).toArgb()
            window.navigationBarColor = if (isDark) Color(0xFF101216).toArgb() else Color(0xFFF5F7F5).toArgb()
            val lightBars = android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or android.view.View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
            val useDarkSystemBarIcons = !isDark && !showStartupAnimation
            window.decorView.systemUiVisibility = if (useDarkSystemBarIcons) window.decorView.systemUiVisibility or lightBars else window.decorView.systemUiVisibility and lightBars.inv()
        }
    }
    Box(Modifier.fillMaxSize()) {
        NotesTheme(settings) {
            when (authenticated) {
                null -> AuthLoadingScreen()
                false -> AuthGate(
                    isGuest = isGuest,
                    settings = settings,
                    onSettingsChange = { changed ->
                        settings = changed
                        writeAppSettings(context, changed)
                    },
                    onAuthenticated = { userId, needsWelcome ->
                        accountId = userId
                        isGuest = false
                        showWelcome = needsWelcome
                        authenticated = true
                    },
                    onGuest = {
                        isGuest = true
                        accountId = "guest"
                        showWelcome = !context.getSharedPreferences("mom_notes", Context.MODE_PRIVATE)
                            .getBoolean("welcome_done_guest", false)
                        context.getSharedPreferences("mom_notes", Context.MODE_PRIVATE)
                            .edit().putBoolean("guest_data_pending", true).putBoolean("active_guest", true).apply()
                        Toast.makeText(context, "Вы вошли как гость. Заметки хранятся только на этом телефоне.", Toast.LENGTH_LONG).show()
                        authenticated = true
                    }
                )
                true -> if (showWelcome) {
                    WelcomeSetupFlow(
                        settings = settings,
                        onSettingsChange = { changed ->
                            settings = changed
                            writeAppSettings(context, changed)
                        },
                        onFinish = {
                            context.getSharedPreferences("mom_notes", Context.MODE_PRIVATE).edit()
                                .putBoolean("welcome_done_${accountId.orEmpty()}", true)
                                .remove("welcome_pending_user").apply()
                            showWelcome = false
                        }
                    )
                } else NotesApp(
                    accountId = accountId.orEmpty(),
                    isGuest = isGuest,
                    settings = settings,
                    onSettingsChange = { changed ->
                        settings = changed
                        writeAppSettings(context, changed)
                    },
                    onLogout = {
                        if (isGuest) {
                            context.getSharedPreferences("mom_notes", Context.MODE_PRIVATE)
                                .edit().putBoolean("active_guest", false).commit()
                            isGuest = false
                            authenticated = false
                        } else scope.launch {
                            try {
                                AppwriteService.logout()
                                context.getSharedPreferences("mom_notes", Context.MODE_PRIVATE)
                                    .edit().remove("last_auth_email").remove("last_auth_user_id")
                                    .putBoolean("active_guest", false).apply()
                                accountId = null
                                isGuest = false
                                showWelcome = false
                                authenticated = false
                            } catch (e: Exception) {
                                val reason = e.localizedMessage?.takeIf { it.isNotBlank() }?.take(180)
                                val message = if (reason == null) {
                                    "Не удалось завершить выход. Повторите попытку позже."
                                } else {
                                    "Не удалось завершить выход: $reason"
                                }
                                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                )
            }
        }
        if (showStartupAnimation) {
            StartupAnimation(
                accent = startupColor,
                onFinished = { showStartupAnimation = false }
            )
        }
    }
}

private fun accentColor(name: String): Color = when (name) {
    "Синий" -> Color(0xFF3676C8)
    "Фиолетовый" -> Color(0xFF8055B7)
    "Бирюзовый" -> Color(0xFF168A83)
    "Терракотовый" -> Color(0xFFC26343)
    "Розовый" -> Color(0xFFC14F7A)
    else -> Color(0xFF286542)
}

private fun mixColors(base: Color, tint: Color, amount: Float): Color {
    val fraction = amount.coerceIn(0f, 1f)
    return Color(
        red = base.red * (1f - fraction) + tint.red * fraction,
        green = base.green * (1f - fraction) + tint.green * fraction,
        blue = base.blue * (1f - fraction) + tint.blue * fraction,
        alpha = base.alpha * (1f - fraction) + tint.alpha * fraction
    )
}

private fun parseHexColor(value: String): Color? {
    return try {
        val digits = value.trim().removePrefix("#")
        val expanded = when {
            digits.matches(Regex("[0-9a-fA-F]{3}")) -> digits.map { "$it$it" }.joinToString("")
            digits.matches(Regex("[0-9a-fA-F]{6}")) -> digits
            else -> null
        }
        expanded?.let { Color(android.graphics.Color.parseColor("#$it")) }
    } catch (_: Exception) {
        null
    }
}

private fun colorHex(color: Color): String = String.format(Locale.US, "#%06X", color.toArgb() and 0xFFFFFF)

private fun roleColor(value: String, fallback: Color): Color = parseHexColor(value)?.let { mixColors(it, fallback, 0.10f) } ?: fallback

private fun contrastColor(color: Color): Color {
    fun linear(channel: Float): Float = if (channel <= 0.04045f) channel / 12.92f else Math.pow(((channel + 0.055f) / 1.055f).toDouble(), 2.4).toFloat()
    val luminance = 0.2126f * linear(color.red) + 0.7152f * linear(color.green) + 0.0722f * linear(color.blue)
    return if (luminance > 0.42f) Color(0xFF171717) else Color.White
}

private val themeColorChoices = listOf(
    "Красный" to "#D64545", "Оранжевый" to "#E67828", "Жёлтый" to "#E8C52E",
    "Зелёный" to "#31824B", "Бирюзовый" to "#168A83", "Голубой" to "#258BC4",
    "Синий" to "#3676C8", "Фиолетовый" to "#8055B7", "Розовый" to "#C14F7A",
    "Терракотовый" to "#C26343", "Серый" to "#69747E", "Чёрный" to "#25282C"
)

@Composable
private fun NotesTheme(settings: AppSettings, content: @Composable () -> Unit) {
    val dark = when (settings.theme) {
        "Светлая" -> false
        "Системная" -> isSystemInDarkTheme()
        else -> true
    }
    val accent = accentColor(settings.accent)
    val colors = if (dark) {
        val background = mixColors(Color(0xFF0D1110), accent, 0.035f)
        val surface = mixColors(Color(0xFF151B18), accent, 0.045f)
        val surfaceVariant = mixColors(Color(0xFF202824), accent, 0.07f)
        darkColorScheme(
            primary = accent,
            onPrimary = contrastColor(accent),
            secondary = mixColors(accent, Color(0xFFB7D4C1), 0.28f),
            background = background,
            onBackground = Color(0xFFF1F4F1),
            surface = surface,
            onSurface = Color(0xFFE8EDE9),
            surfaceVariant = surfaceVariant,
            onSurfaceVariant = Color(0xFFB6C1BA),
            outline = mixColors(Color(0xFF46534B), accent, 0.12f),
            outlineVariant = Color(0xFF303A34),
            error = Color(0xFFFF8A80)
        )
    } else {
        val background = mixColors(Color(0xFFF5F4F0), accent, 0.018f)
        val surfaceVariant = mixColors(Color(0xFFE9E9E3), accent, 0.035f)
        lightColorScheme(
            primary = accent,
            onPrimary = contrastColor(accent),
            secondary = mixColors(accent, Color(0xFF52675B), 0.25f),
            background = background,
            onBackground = Color(0xFF202622),
            surface = Color(0xFFFFFEFB),
            onSurface = Color(0xFF202622),
            surfaceVariant = surfaceVariant,
            onSurfaceVariant = Color(0xFF59645D),
            outline = Color(0xFF89938B),
            outlineVariant = Color(0xFFD8DDD7),
            error = Color(0xFFB3261E)
        )
    }
    MaterialTheme(colorScheme = colors, content = content)
}

@Composable
private fun SettingsScreen(
    settings: AppSettings,
    onSettingsChange: (AppSettings) -> Unit,
    noteCount: Int,
    categoryCount: Int,
    onExport: () -> Unit,
    onImport: () -> Unit,
    onAbout: () -> Unit,
    onBack: () -> Unit,
    onReset: () -> Unit,
    onLogout: () -> Unit,
    isGuest: Boolean = false
) {
    val context = LocalContext.current
    var section by remember { mutableStateOf("Внешний вид") }
    var confirmReset by remember { mutableStateOf(false) }
    val sections = listOf("Внешний вид", "Поведение", "Настройки ИИ", "Данные и копии", "Дополнительно")
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(horizontal = 14.dp)) {
        Row(Modifier.fillMaxWidth().height(54.dp), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack, contentPadding = PaddingValues(horizontal = 5.dp)) { Text("←", color = MaterialTheme.colorScheme.onBackground, fontSize = 25.sp) }
            Text("Настройки", color = MaterialTheme.colorScheme.onBackground, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
        }
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            sections.forEach { item ->
                TextButton(onClick = { section = item }, contentPadding = PaddingValues(horizontal = 9.dp)) {
                    Text(item, color = if (section == item) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, maxLines = 1, softWrap = false)
                }
            }
        }
        val settingsListState = rememberLazyListState()
        LaunchedEffect(section) { settingsListState.scrollToItem(0) }
        LazyColumn(
            state = settingsListState,
            modifier = Modifier.fillMaxWidth().weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            when (section) {
                "Внешний вид" -> {
                    item {
                        SettingsCard("Тема") {
                            ChoiceRow("Оформление", settings.theme, listOf("Тёмная", "Светлая", "Системная")) { onSettingsChange(settings.copy(theme = it)) }
                            ChoiceRow("Основной цвет темы", settings.accent, listOf("Зелёный", "Синий", "Фиолетовый", "Бирюзовый", "Терракотовый", "Розовый")) { onSettingsChange(settings.copy(accent = it)) }
                            IconBackgroundSetting(settings.iconBackground) { colorName ->
                                setLauncherIconColor(context, colorName)
                                onSettingsChange(settings.copy(iconBackground = colorName))
                            }
                            ChoiceRow("Размер текста заметок", settings.noteTextSize, listOf("Маленький", "Средний", "Большой")) { onSettingsChange(settings.copy(noteTextSize = it)) }
                        }
                    }
                    item {
                        SettingsCard("Своя палитра") {
                            Text("Задайте отдельный цвет верхней панели, разделов, карточек и кнопки добавления. Можно выбрать готовый оттенок или настроить свой цвет.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                            ColorSetting("Верхняя панель «Мои заметки»", settings.headerColor, accentColor(settings.accent)) { onSettingsChange(settings.copy(headerColor = it)) }
                            ColorSetting("Кнопки разделов", settings.sectionColor, accentColor(settings.accent)) { onSettingsChange(settings.copy(sectionColor = it)) }
                            ColorSetting("Карточки заметок", settings.noteColor, MaterialTheme.colorScheme.surface) { onSettingsChange(settings.copy(noteColor = it)) }
                            ColorSetting("Таблицы в заметках", settings.tableColor, MaterialTheme.colorScheme.surfaceVariant) { onSettingsChange(settings.copy(tableColor = it)) }
                            ColorSetting("Кнопка «+»", settings.addButtonColor, accentColor(settings.accent)) { onSettingsChange(settings.copy(addButtonColor = it)) }
                        }
                    }
                    item {
                        SettingsCard("Список заметок") {
                            ChoiceRow("Плотность", settings.density, listOf("Компактно", "Просторно")) { onSettingsChange(settings.copy(density = it)) }
                            ChoiceRow("Вид", settings.listStyle, listOf("Карточки", "Строки")) { onSettingsChange(settings.copy(listStyle = it)) }
                            ChoiceRow("Скругление карточек", settings.corner, listOf("Маленькое", "Среднее", "Большое")) { onSettingsChange(settings.copy(corner = it)) }
                        }
                    }
                }
                "Поведение" -> item {
                    SettingsCard("Заметки") {
                        ChoiceRow("Сортировка", settings.sortBy, listOf("По дате изменения", "По дате создания", "По названию", "Пользовательский")) { onSettingsChange(settings.copy(sortBy = it)) }
                        ChoiceRow("Избранное", settings.favoriteBehavior, listOf("Поднимать наверх", "Только избранное", "Обычный порядок")) { onSettingsChange(settings.copy(favoriteBehavior = it)) }
                        SettingSwitch("Автоматически сохранять при вводе", settings.autoSave) { onSettingsChange(settings.copy(autoSave = it)) }
                        SettingSwitch("Подтверждать удаление", settings.confirmDelete) { onSettingsChange(settings.copy(confirmDelete = it)) }
                        SettingSwitch("Показывать дату изменения", settings.showModifiedDate) { onSettingsChange(settings.copy(showModifiedDate = it)) }
                    }
                }
                "Настройки ИИ" -> item {
                    SettingsCard("Передача запроса") {
                        Text("Помощника и инструкции теперь можно выбрать отдельно в каждой заметке. Прямой вызов ИИ по API не настроен.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                        ChoiceRow("Действие по кнопке ИИ", settings.aiTransfer, listOf("Открыть приложение ИИ", "Копировать запрос")) { onSettingsChange(settings.copy(aiTransfer = it)) }
                    }
                }
                "Данные и копии" -> item {
                    SettingsCard("Локальные данные") {
                        Text("Заметок: $noteCount · разделов: $categoryCount", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                        Text("Резервная копия JSON включает заметки, разделы и таблицы. Фото и вложенные файлы отдельно не включаются.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                        TextButton(onClick = onExport) { Text("Создать резервную копию") }
                        TextButton(onClick = onImport) { Text("Импортировать копию") }
                        Text("Импорт заменит текущие заметки и разделы.", color = Color(0xFFFFC56D), fontSize = 12.sp)
                    }
                }
                else -> item {
                    SettingsCard("Дополнительно") {
                        ChoiceRow("Язык интерфейса", settings.language, listOf("Русский")) { onSettingsChange(settings.copy(language = it)) }
                        SettingSwitch("Вибрация при действиях", settings.vibration) { onSettingsChange(settings.copy(vibration = it)) }
                        ChoiceRow("Язык голосового ввода", settings.voiceLanguage, listOf("Русский", "Системный")) { onSettingsChange(settings.copy(voiceLanguage = it)) }
                        TextButton(onClick = { confirmReset = true }) { Text("Сбросить настройки") }
                        TextButton(onClick = onLogout) { Text(if (isGuest) "Войти и синхронизировать" else "Выйти из аккаунта") }
                        TextButton(onClick = onAbout) { Text("О приложении · версия и история изменений") }
                    }
                }
            }
        }
    }
    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text("Сбросить настройки?") },
            text = { Text("Настройки оформления и поведения вернутся к исходным. Заметки не удалятся.") },
            confirmButton = { TextButton(onClick = { onReset(); confirmReset = false }) { Text("Сбросить") } },
            dismissButton = { TextButton(onClick = { confirmReset = false }) { Text("Отмена") } }
        )
    }
}

@Composable
private fun AppwriteAccountCard() {
    val scope = rememberCoroutineScope()
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var signedInEmail by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(true) }
    var message by remember { mutableStateOf<String?>(null) }
    var isError by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        try {
            signedInEmail = AppwriteService.currentEmail()
        } catch (_: Exception) {
            signedInEmail = null
        } finally {
            busy = false
        }
    }

    SettingsCard("Аккаунт Appwrite") {
        Text(
            "Вход нужен только для аккаунта. Заметки пока сохраняются только на этом устройстве.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp
        )
        if (busy) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                Text("Проверяю аккаунт…", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
            }
        } else if (signedInEmail != null) {
            Text("Вы вошли как ${signedInEmail}", color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp)
            TextButton(
                enabled = !busy,
                onClick = {
                    scope.launch {
                        busy = true
                        message = null
                        try {
                            AppwriteService.logout()
                            signedInEmail = null
                            password = ""
                            message = "Вы вышли из аккаунта. Локальные заметки сохранены."
                            isError = false
                        } catch (e: Exception) {
                            message = e.message ?: "Не удалось выйти из аккаунта. Проверьте интернет и попробуйте снова."
                            isError = true
                        } finally {
                            busy = false
                        }
                    }
                }
            ) { Text("Выйти") }
        } else {
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Электронная почта") },
                singleLine = true,
                enabled = !busy,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, capitalization = KeyboardCapitalization.None)
            )
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Пароль, минимум 8 символов") },
                singleLine = true,
                enabled = !busy,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, capitalization = KeyboardCapitalization.None)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(
                    enabled = !busy && email.contains("@") && password.length >= 8,
                    onClick = {
                        scope.launch {
                            busy = true
                            message = null
                            try {
                                AppwriteService.login(email, password)
                                signedInEmail = AppwriteService.currentEmail()
                                message = "Вход выполнен. Заметки остаются локальными."
                                isError = false
                            } catch (e: Exception) {
                                message = e.message ?: "Не удалось войти. Проверьте почту, пароль и интернет."
                                isError = true
                            } finally {
                                busy = false
                            }
                        }
                    }
                ) { Text("Войти") }
                TextButton(
                    enabled = !busy && email.contains("@") && password.length >= 8,
                    onClick = {
                        scope.launch {
                            busy = true
                            message = null
                            try {
                                AppwriteService.register(email, password)
                                signedInEmail = AppwriteService.currentEmail()
                                password = ""
                                message = "Аккаунт создан, вход выполнен. Заметки остаются локальными."
                                isError = false
                            } catch (e: Exception) {
                                message = e.message ?: "Не удалось создать аккаунт. Проверьте почту и попробуйте снова."
                                isError = true
                            } finally {
                                busy = false
                            }
                        }
                    }
                ) { Text("Создать аккаунт") }
            }
        }
        if (busy && signedInEmail != null) {
            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
        }
        message?.let {
            Text(
                it,
                color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun SettingsCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(17.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Text(title, color = MaterialTheme.colorScheme.onSurface, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            content()
        }
    }
}

@Composable
internal fun ChoiceRow(label: String, value: String, options: List<String>, onSelect: (String) -> Unit) {
    var expanded by remember(label) { mutableStateOf(false) }
    Column {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
        Box {
            TextButton(onClick = { expanded = true }, contentPadding = PaddingValues(horizontal = 2.dp, vertical = 1.dp)) {
                Text("$value  ▾", color = MaterialTheme.colorScheme.primary, fontSize = 14.sp)
            }
            androidx.compose.material3.DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                options.forEach { option ->
                    androidx.compose.material3.DropdownMenuItem(
                        text = { Text(option) },
                        onClick = { onSelect(option); expanded = false }
                    )
                }
            }
        }
    }
}

@Composable
private fun ColorSetting(label: String, value: String, defaultColor: Color, onChange: (String) -> Unit) {
    var showPicker by remember(label) { mutableStateOf(false) }
    var customHex by remember(label, value) { mutableStateOf(value.ifBlank { colorHex(defaultColor) }) }
    val chosenColor = parseHexColor(value) ?: defaultColor
    TextButton(onClick = { customHex = value.ifBlank { colorHex(defaultColor) }; showPicker = true }, contentPadding = PaddingValues(horizontal = 2.dp, vertical = 3.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(label, modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp)
            Text(if (value.isBlank()) "Авто" else "Свой", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
            Spacer(Modifier.width(8.dp))
            Box(Modifier.size(20.dp).clip(CircleShape).background(chosenColor).border(1.dp, MaterialTheme.colorScheme.outline, CircleShape))
        }
    }
    if (showPicker) {
        AlertDialog(
            onDismissRequest = { showPicker = false },
            title = { Text(label) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Готовые цвета", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        themeColorChoices.forEach { (_, hex) ->
                            val swatch = parseHexColor(hex) ?: Color.Gray
                            Box(
                                Modifier.size(34.dp).clip(CircleShape).background(swatch)
                                    .border(if (value.equals(hex, true)) 3.dp else 1.dp, if (value.equals(hex, true)) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline, CircleShape)
                                    .clickable { onChange(hex); showPicker = false }
                            )
                        }
                    }
                    OutlinedTextField(
                        value = customHex,
                        onValueChange = { customHex = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Свой цвет, например #E8C52E") },
                        singleLine = true
                    )
                    TextButton(onClick = { onChange(""); showPicker = false }) { Text("Автоматически из темы") }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val normalized = parseHexColor(customHex)?.let(::colorHex)
                    if (normalized != null) { onChange(normalized); showPicker = false }
                }, enabled = parseHexColor(customHex) != null) { Text("Применить") }
            },
            dismissButton = { TextButton(onClick = { showPicker = false }) { Text("Отмена") } }
        )
    }
}

@Composable
private fun IconBackgroundSetting(value: String, onChange: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text("Фон иконки приложения", color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp)
        Text("Выберите цвет фона значка на главном экране телефона.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            themeColorChoices.forEach { (name, hex) ->
                val swatch = parseHexColor(hex) ?: Color.Gray
                Box(
                    Modifier.size(32.dp).clip(CircleShape).background(swatch)
                        .border(if (name == value) 3.dp else 1.dp,
                            if (name == value) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline,
                            CircleShape)
                        .clickable { onChange(name) }
                )
            }
        }
        Text("Выбран: $value", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
    }
}

@Composable
internal fun SettingSwitch(label: String, value: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp)
        Switch(checked = value, onCheckedChange = onChange)
    }
}

private fun readAppSettings(context: Context): AppSettings {
    val prefs = context.getSharedPreferences("mom_notes", Context.MODE_PRIVATE)
    return AppSettings(
        theme = prefs.getString("pref_theme", "Тёмная") ?: "Тёмная",
        accent = prefs.getString("pref_accent", "Зелёный") ?: "Зелёный",
        headerColor = prefs.getString("pref_header_color", "") ?: "",
        sectionColor = prefs.getString("pref_section_color", "") ?: "",
        noteColor = prefs.getString("pref_note_color", "") ?: "",
        addButtonColor = prefs.getString("pref_add_button_color", "") ?: "",
        tableColor = prefs.getString("pref_table_color", "") ?: "",
        iconBackground = prefs.getString("pref_icon_background", "Зелёный") ?: "Зелёный",
        noteTextSize = prefs.getString("pref_text_size", "Средний") ?: "Средний",
        density = prefs.getString("pref_density", "Просторно") ?: "Просторно",
        corner = prefs.getString("pref_corner", "Среднее") ?: "Среднее",
        sortBy = prefs.getString("pref_sort", "По дате изменения") ?: "По дате изменения",
        listStyle = prefs.getString("pref_list_style", "Карточки") ?: "Карточки",
        autoSave = prefs.getBoolean("pref_auto_save", false),
        confirmDelete = prefs.getBoolean("pref_confirm_delete", true),
        showModifiedDate = prefs.getBoolean("pref_show_date", true),
        favoriteBehavior = prefs.getString("pref_favorite", "Поднимать наверх") ?: "Поднимать наверх",
        aiTransfer = prefs.getString("pref_ai_transfer", "Открыть приложение ИИ") ?: "Открыть приложение ИИ",
        language = prefs.getString("pref_language", "Русский") ?: "Русский",
        vibration = prefs.getBoolean("pref_vibration", true),
        voiceLanguage = prefs.getString("pref_voice_language", "Русский") ?: "Русский"
    )
}

private fun writeAppSettings(context: Context, s: AppSettings) {
    context.getSharedPreferences("mom_notes", Context.MODE_PRIVATE).edit()
        .putString("pref_theme", s.theme).putString("pref_accent", s.accent)
        .putString("pref_header_color", s.headerColor).putString("pref_section_color", s.sectionColor)
        .putString("pref_note_color", s.noteColor).putString("pref_add_button_color", s.addButtonColor)
        .putString("pref_table_color", s.tableColor).putString("pref_icon_background", s.iconBackground)
        .putString("pref_text_size", s.noteTextSize).putString("pref_density", s.density)
        .putString("pref_corner", s.corner).putString("pref_sort", s.sortBy)
        .putString("pref_list_style", s.listStyle).putBoolean("pref_auto_save", s.autoSave)
        .putBoolean("pref_confirm_delete", s.confirmDelete).putBoolean("pref_show_date", s.showModifiedDate)
        .putString("pref_favorite", s.favoriteBehavior)
        .putString("pref_ai_transfer", s.aiTransfer).putString("pref_language", s.language)
        .putBoolean("pref_vibration", s.vibration).putString("pref_voice_language", s.voiceLanguage).apply()
}

@Composable
private fun AboutScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val appName = remember {
        context.packageManager.getApplicationLabel(context.applicationInfo).toString()
    }
    val version = context.getString(R.string.app_version)

    Column(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(54.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onBack, contentPadding = PaddingValues(horizontal = 6.dp)) {
                Text("←", color = MaterialTheme.colorScheme.onBackground, fontSize = 25.sp)
            }
            Text("О приложении", color = MaterialTheme.colorScheme.onBackground, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
        }
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(appName, color = MaterialTheme.colorScheme.onSurface, fontSize = 21.sp, fontWeight = FontWeight.Bold)
                Text("Версия $version", color = MaterialTheme.colorScheme.primary, fontSize = 14.sp)
                Text(
                    context.getString(R.string.about_version_description),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
private fun AuthLoadingScreen() {
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            CircularProgressIndicator()
            Text("Проверяю аккаунт…", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun AuthGate(
    isGuest: Boolean,
    settings: AppSettings,
    onSettingsChange: (AppSettings) -> Unit,
    onAuthenticated: (String, Boolean) -> Unit,
    onGuest: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? ComponentActivity
    val scope = rememberCoroutineScope()
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var isError by remember { mutableStateOf(false) }
    var settingsOpen by remember { mutableStateOf(false) }
    BackHandler(enabled = settingsOpen) { settingsOpen = false }

    fun rememberAccount(emailAddress: String, userId: String): Boolean {
        val prefs = context.getSharedPreferences("mom_notes", Context.MODE_PRIVATE)
        val guestNotes = prefs.getString(accountNotesKey("guest"), null)
        val edit = prefs.edit().putString("last_auth_email", emailAddress)
            .putString("last_auth_user_id", userId).putBoolean("active_guest", false)
        if (!prefs.contains(accountNotesKey(userId)) && !guestNotes.isNullOrBlank()) {
            edit.putString(accountNotesKey(userId), guestNotes)
        }
        if (!prefs.contains("cloud_migration_owner")) edit.putString("cloud_migration_owner", userId)
        val needsWelcome = !prefs.getBoolean("welcome_done_$userId", false)
        if (needsWelcome) edit.putString("welcome_pending_user", userId)
        edit.apply()
        return needsWelcome
    }


    fun submit(register: Boolean) {
        scope.launch {
            busy = true
            message = null
            try {
                context.getSharedPreferences("mom_notes", Context.MODE_PRIVATE)
                    .edit().putBoolean("active_guest", false).apply()
                val normalizedEmail = email.trim()
                val session = if (register) {
                    AppwriteService.register(normalizedEmail, password)
                } else {
                    AppwriteService.login(normalizedEmail, password)
                }
                val needsWelcome = rememberAccount(normalizedEmail, session.userId)
                onAuthenticated(session.userId, needsWelcome)
            } catch (e: Exception) {
                message = e.localizedMessage?.takeIf { it.isNotBlank() }?.take(180)
                    ?: if (register) "Не удалось создать аккаунт. Попробуйте ещё раз." else "Не удалось войти. Проверьте почту и пароль."
                isError = true
            } finally {
                busy = false
            }
        }
    }

    fun social(provider: OAuthProvider) {
        if (activity == null) {
            message = "Не удалось открыть окно входа. Перезапустите приложение."
            isError = true
            return
        }
        scope.launch {
            busy = true
            message = null
            try {
                // Leave guest mode before opening the external browser so activity recreation
                // cannot route the completed sign-in back to the guest screen.
                context.getSharedPreferences("mom_notes", Context.MODE_PRIVATE)
                    .edit().putBoolean("active_guest", false).commit()
                try {
                    AppwriteService.logout()
                } catch (_: Exception) {
                    // No current Appwrite session to clear.
                }
                AppwriteService.loginWithProvider(activity, provider)
                val user = AppwriteService.account.get()
                val needsWelcome = rememberAccount(user.email, user.id)
                onAuthenticated(user.id, needsWelcome)
            } catch (e: Exception) {
                message = e.localizedMessage?.takeIf { it.isNotBlank() }?.take(180)
                    ?: "Вход не завершён. Повторите попытку."
                isError = true
            } finally {
                busy = false
            }
        }
    }

    if (settingsOpen) {
        Column(
            Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(horizontal = 14.dp)
        ) {
            Row(Modifier.fillMaxWidth().height(54.dp), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = { settingsOpen = false }, contentPadding = PaddingValues(horizontal = 5.dp)) {
                    Text("←", color = MaterialTheme.colorScheme.onBackground, fontSize = 25.sp)
                }
                Text("Настройки", color = MaterialTheme.colorScheme.onBackground, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
            }
            Column(Modifier.fillMaxWidth().padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SettingsCard("Внешний вид") {
                    ChoiceRow("Оформление", settings.theme, listOf("Тёмная", "Светлая", "Системная")) {
                        onSettingsChange(settings.copy(theme = it))
                    }
                    ChoiceRow("Основной цвет", settings.accent, listOf("Зелёный", "Синий", "Фиолетовый", "Бирюзовый", "Терракотовый", "Розовый")) {
                        onSettingsChange(settings.copy(accent = it))
                    }
                }
            }
        }
        return
    }

    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).verticalScroll(rememberScrollState()).padding(horizontal = 22.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(if (isGuest) "Войдите для синхронизации" else "Войдите в аккаунт", modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onBackground, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
            IconButton(onClick = { settingsOpen = true }, enabled = !busy) {
                Text("⚙", color = MaterialTheme.colorScheme.onBackground, fontSize = 21.sp)
            }
        }
        Text("Чтобы продолжить, войдите или создайте аккаунт. Заметки по-прежнему хранятся только на этом устройстве.", modifier = Modifier.padding(top = 10.dp, bottom = 18.dp), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
        OutlinedTextField(value = email, onValueChange = { email = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Электронная почта") }, singleLine = true, enabled = !busy, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, capitalization = KeyboardCapitalization.None))
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(value = password, onValueChange = { password = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Пароль, минимум 8 символов") }, singleLine = true, enabled = !busy, visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, capitalization = KeyboardCapitalization.None))
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { submit(false) }, modifier = Modifier.weight(1f), enabled = !busy && email.contains("@") && password.length >= 8) { Text("Войти") }
            Button(onClick = { submit(true) }, modifier = Modifier.weight(1f), enabled = !busy && email.contains("@") && password.length >= 8) { Text("Регистрация") }
        }
        Text("Или продолжить через", modifier = Modifier.padding(top = 12.dp), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = { social(OAuthProvider.GOOGLE) }, enabled = !busy, modifier = Modifier.weight(1f)) { Text("Google") }
            TextButton(onClick = { social(OAuthProvider.YANDEX) }, enabled = !busy, modifier = Modifier.weight(1f)) { Text("Яндекс") }
        }
        Text("Почта Mail.ru пока не поддерживается Appwrite как отдельный способ входа.", modifier = Modifier.padding(top = 2.dp), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
        TextButton(onClick = onGuest, enabled = !busy, modifier = Modifier.padding(top = 8.dp)) {
            Text("Продолжить как гость")
        }
        Text("Без аккаунта заметки останутся на этом телефоне.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
        if (busy) CircularProgressIndicator(modifier = Modifier.padding(top = 8.dp).size(22.dp), strokeWidth = 2.dp)
        message?.let { Text(it, modifier = Modifier.padding(top = 10.dp), color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary, fontSize = 12.sp) }
    }
}

@Composable
private fun NotesApp(accountId: String, isGuest: Boolean, settings: AppSettings, onSettingsChange: (AppSettings) -> Unit, onLogout: () -> Unit) {
    val context = LocalContext.current
    val notesKey = remember(accountId) { accountNotesKey(accountId) }
    val notes = remember(accountId) { mutableStateListOf<Note>().apply { addAll(readNotesForAccount(context, accountId)) } }
    val categoryList = remember { mutableStateListOf<String>().apply { addAll(readCategories(context)) } }
    val syncScope = rememberCoroutineScope()
    var cloudSyncJob by remember(accountId) { mutableStateOf<Job?>(null) }
    var pendingCloudSync by remember(accountId) { mutableStateOf<List<Note>?>(null) }
    var notifySyncFailure by remember(accountId) { mutableStateOf(false) }
    val syncStatus = remember(accountId, isGuest) { mutableStateOf(if (isGuest) "Только на устройстве" else "Синхронизация…") }
    var syncFailureDetail by remember(accountId) { mutableStateOf<String?>(null) }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var activeDragNoteId by remember { mutableStateOf<String?>(null) }
    val noteDragState = remember { NoteDragState() }
    val activeDragPointerState = remember { mutableStateOf(Offset.Zero) }
    var noteMoveToConfirm by remember { mutableStateOf<Pair<String, String>?>(null) }
    val noteCardBounds = remember { mutableMapOf<String, Rect>() }
    val categoryDropBounds = remember { mutableMapOf<String, Rect>() }
    val manualOrderByCategory = remember(accountId) {
        mutableStateMapOf<String, List<String>>().apply {
            categoryList.forEach { category ->
                val raw = context.getSharedPreferences("mom_notes", Context.MODE_PRIVATE)
                    .getString("manual_order_${accountId}_$category", null)
                if (!raw.isNullOrBlank()) {
                    runCatching {
                        val array = JSONArray(raw)
                        put(category, (0 until array.length()).map { array.optString(it) }.filter(String::isNotBlank))
                    }
                }
            }
        }
    }
    var manualOrderRevision by remember(accountId) { mutableStateOf(0) }
    var searchText by remember { mutableStateOf("") }
    var appliedSearchText by remember { mutableStateOf("") }
    LaunchedEffect(searchText) {
        delay(120)
        appliedSearchText = searchText
    }
    var sortMode by remember { mutableStateOf(0) }
    var showSortMenu by remember { mutableStateOf(false) }
    var editorOpen by remember { mutableStateOf(false) }
    var noteBeingEdited by remember { mutableStateOf<Note?>(null) }
    var noteToDelete by remember { mutableStateOf<Note?>(null) }
    var showCategoryDialog by remember { mutableStateOf(false) }
    var newCategoryName by remember { mutableStateOf("") }
    var categoryToDelete by remember { mutableStateOf<String?>(null) }
    var aboutOpen by remember { mutableStateOf(false) }
    var settingsOpen by remember { mutableStateOf(false) }
    BackHandler(enabled = aboutOpen || settingsOpen) {
        if (settingsOpen) settingsOpen = false else aboutOpen = false
    }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) {
            try {
                val backup = JSONObject()
                    .put("format", "mom_notes_backup")
                    .put("backupVersion", 1)
                    .put("categories", JSONArray(categoryList.toList()))
                    .put("notes", notesToJson(notes.toList()))
                context.contentResolver.openOutputStream(uri)?.use { it.write(backup.toString(2).toByteArray(Charsets.UTF_8)) }
                Toast.makeText(context, "Резервная копия сохранена", Toast.LENGTH_SHORT).show()
            } catch (_: Exception) {
                Toast.makeText(context, "Не удалось создать резервную копию", Toast.LENGTH_LONG).show()
            }
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            try {
                val raw = context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
                    ?: error("Файл не удалось прочитать")
                val backup = JSONObject(raw)
                require(backup.optString("format") == "mom_notes_backup")
                val importedNotes = backup.optJSONArray("notes")?.toNotes().orEmpty()
                val importedCategories = backup.optJSONArray("categories")?.toStringList().orEmpty()
                require(importedCategories.isNotEmpty())
                notes.clear(); notes.addAll(importedNotes)
                categoryList.clear(); categoryList.addAll(importedCategories)
                persistImported(context, notes.toList(), categoryList.toList(), notesKey)
                selectedCategory = null
                Toast.makeText(context, "Резервная копия импортирована", Toast.LENGTH_SHORT).show()
            } catch (error: Exception) {
                val detail = error.localizedMessage?.takeIf { it.isNotBlank() }?.take(180)
                val text = if (detail == null) "Не удалось импортировать файл резервной копии" else "Не удалось импортировать файл: $detail"
                Toast.makeText(context, text, Toast.LENGTH_LONG).show()
            }
        }
    }

    fun keepUploadedFileIds(current: Note?, edited: Note): Note {
        if (current == null) return edited
        fun keepIds(paths: List<String>, editedIds: List<String>, currentPaths: List<String>, currentIds: List<String>): List<String> =
            paths.mapIndexed { index, path ->
                currentPaths.indexOf(path).takeIf { it >= 0 }?.let { currentIds.getOrNull(it) }
                    ?.takeIf(String::isNotBlank)
                    ?: editedIds.getOrNull(index)?.takeIf(String::isNotBlank).orEmpty()
            }
        return edited.copy(
            photoIds = keepIds(edited.photoPaths, edited.photoIds, current.photoPaths, current.photoIds),
            attachmentIds = keepIds(edited.attachmentPaths, edited.attachmentIds, current.attachmentPaths, current.attachmentIds)
        )
    }

    fun queueCloudSync(snapshot: List<Note>, showFailure: Boolean = true) {
        pendingCloudSync = snapshot
        notifySyncFailure = notifySyncFailure || showFailure
        if (isGuest) {
            syncStatus.value = "Только на устройстве"
            return
        }
        syncStatus.value = "Ожидает синхронизации…"
        if (cloudSyncJob?.isActive == true) return
        cloudSyncJob = syncScope.launch {
            delay(700)
            while (true) {
                val batch = pendingCloudSync
                if (batch == null) {
                    cloudSyncJob = null
                    return@launch
                }
                pendingCloudSync = null
                syncStatus.value = "Синхронизация…"
                try {
                    val synced = AppwriteCloudSync.sync(context, batch)
                    val uploadedById = synced.associateBy { it.id }
                    val newerBatch = pendingCloudSync
                    if (newerBatch != null) {
                        // Carry file IDs from a completed upload into edits queued while it was running.
                        pendingCloudSync = newerBatch.map { newer ->
                            keepUploadedFileIds(uploadedById[newer.id], newer)
                        }
                        val refreshedLocal = notes.map { local ->
                            keepUploadedFileIds(uploadedById[local.id], local)
                        }
                        notes.clear()
                        notes.addAll(refreshedLocal)
                        writeNotes(context, refreshedLocal, notesKey)
                    } else {
                        val syncedWithLocalReminders = synced.map { cloudNote ->
                            val local = notes.firstOrNull { it.id == cloudNote.id }
                            if (local == null) cloudNote else cloudNote.copy(
                                reminderAt = local.reminderAt,
                                reminders = (local.reminders + listOfNotNull(local.reminderAt.takeIf { it > 0L })).distinct().sorted(),
                                reminderTypes = local.reminderTypes
                            )
                        }
                        notes.clear()
                        notes.addAll(syncedWithLocalReminders)
                        synced.map { it.category }.filter(String::isNotBlank).distinct().forEach { category ->
                            if (categoryList.none { it.equals(category, ignoreCase = true) }) categoryList.add(category)
                        }
                        writeCategories(context, categoryList.toList())
                        writeNotes(context, syncedWithLocalReminders, notesKey)
                        syncedWithLocalReminders.forEach { updateReminder(context, it) }
                        syncStatus.value = "Синхронизировано"
                        syncFailureDetail = null
                        notifySyncFailure = false
                    }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (error: Exception) {
                    if (pendingCloudSync == null) {
                        syncStatus.value = "Ошибка синхронизации"
                        syncFailureDetail = error.message?.take(220)
                        if (notifySyncFailure) {
                            val detail = syncFailureDetail?.let { "\n$it" }.orEmpty()
                            Toast.makeText(context, "Ошибка облачной синхронизации.$detail", Toast.LENGTH_LONG).show()
                        }
                        notifySyncFailure = false
                    }
                }
            }
        }
    }

    fun persist() {
        val snapshot = notes.toList()
        writeNotes(context, snapshot, notesKey)
        if (isGuest) {
            syncStatus.value = "Только на устройстве"
            return
        }
        queueCloudSync(snapshot)
    }

    fun finishNoteDrag(note: Note) {
        val point = activeDragPointerState.value
        val destination = categoryDropBounds.entries.firstOrNull { it.value.contains(point) }?.key
        if (destination != null && destination != note.category) {
            noteMoveToConfirm = note.id to destination
        } else {
            val targetId = notes.asSequence()
                .filter { it.category == note.category && !it.isDeleted && it.id != note.id }
                .map { it.id }
                .firstOrNull { noteCardBounds[it]?.contains(point) == true }
            if (targetId != null) {
                val currentIds = notes.asSequence()
                    .filter { it.category == note.category && !it.isDeleted }
                    .map { it.id }
                    .toList()
                val savedOrder = manualOrderByCategory[note.category].orEmpty()
                val rank = savedOrder.withIndex().associate { it.value to it.index }
                val reorderedIds = currentIds.sortedBy { rank[it] ?: Int.MAX_VALUE }.toMutableList()
                val from = reorderedIds.indexOf(note.id)
                val to = reorderedIds.indexOf(targetId)
                if (from >= 0 && to >= 0) {
                    reorderedIds.removeAt(from)
                    reorderedIds.add(to, note.id)
                    manualOrderByCategory[note.category] = reorderedIds
                    context.getSharedPreferences("mom_notes", Context.MODE_PRIVATE).edit()
                        .putString("manual_order_${accountId}_${note.category}", JSONArray(reorderedIds).toString())
                        .apply()
                    manualOrderRevision++
                }
            }
        }
        activeDragNoteId = null
        noteDragState.offset = Offset.Zero
        noteDragState.offsetState.value = Offset.Zero
    }

    LaunchedEffect(accountId, isGuest) {
        if (isGuest) {
            syncStatus.value = "Только на устройстве"
            return@LaunchedEffect
        }
        queueCloudSync(notes.toList(), showFailure = false)
    }

    val notesSnapshot by remember { derivedStateOf { notes.toList() } }
    var indexedNotes by remember(accountId) { mutableStateOf(emptyList<NoteListItem>()) }
    LaunchedEffect(notesSnapshot) {
        val snapshot = notesSnapshot
        indexedNotes = withContext(Dispatchers.Default) {
            snapshot.map { note -> NoteListItem(note = note, plainBody = note.plainBody) }
        }
    }
    var visibleNotes by remember(accountId) { mutableStateOf(emptyList<NoteListItem>()) }
    LaunchedEffect(indexedNotes, selectedCategory, appliedSearchText, settings.favoriteBehavior, settings.sortBy, sortMode, manualOrderRevision) {
        val locale = Locale.getDefault()
        val query = appliedSearchText.trim().lowercase(locale)
        val category = selectedCategory
        val favoriteBehavior = settings.favoriteBehavior
        val sortBy = settings.sortBy
        val activeSortMode = sortMode
        val customOrderRanks = manualOrderByCategory[category.orEmpty()].orEmpty().withIndex().associate { it.value to it.index }
        val filteredAndSorted = withContext(Dispatchers.Default) {
            val found = ArrayList<NoteListItem>(indexedNotes.size)
            indexedNotes.forEach { row ->
                val note = row.note
                if (note.isDeleted || note.category != category) return@forEach
                if (favoriteBehavior == "Только избранное" && !note.isFavorite) return@forEach
                if (activeSortMode == 2 && !note.isFavorite) return@forEach
                if (query.isNotEmpty() && !(note.title + "\n" + row.plainBody).lowercase(locale).contains(query)) return@forEach
                found.add(row)
            }
            val ordered = when (sortBy) {
                "Пользовательский" -> found.sortedBy { customOrderRanks[it.note.id] ?: Int.MAX_VALUE }
                "По дате создания" -> found.sortedByDescending { it.note.createdAt }
                "По названию" -> found.sortedBy { it.note.title.lowercase(locale) }
                else -> found.sortedByDescending { it.note.updatedAt }
            }
            when (activeSortMode) {
                1 -> ordered.asReversed()
                2 -> ordered
                else -> if (sortBy != "Пользовательский" && favoriteBehavior == "Поднимать наверх") {
                    ordered.sortedWith(compareByDescending<NoteListItem> { it.note.isFavorite }.thenByDescending { it.note.updatedAt })
                } else ordered
            }
        }
        visibleNotes = filteredAndSorted
    }

    val noteListState = rememberLazyListState()
    val addButtonScale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (selectedCategory != null) 1f else 0.92f,
        animationSpec = tween(140, easing = FastOutSlowInEasing),
        label = "add-button-scale"
    )
    val sectionMotion = remember { androidx.compose.animation.core.Animatable(1f) }
    LaunchedEffect(selectedCategory) {
        sectionMotion.snapTo(0.99f)
        sectionMotion.animateTo(1f, tween(150, easing = FastOutSlowInEasing))
        noteListState.scrollToItem(0)
    }

    Box(Modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = !aboutOpen && !settingsOpen && !editorOpen,
            enter = fadeIn(tween(140)),
            exit = fadeOut(tween(100))
        ) {
            val categoryItems = remember(categoryList.toList()) { categoryList.toList() }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
            ) {
                val headerColor = roleColor(settings.headerColor, MaterialTheme.colorScheme.surface)
                val headerContentColor = contrastColor(headerColor)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(headerColor)
                        .padding(top = 10.dp, bottom = 11.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 12.dp, top = 8.dp, bottom = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = "Мои заметки",
                                color = headerContentColor,
                                fontSize = 25.sp,
                                fontWeight = FontWeight.Bold
                            )
                            CloudSyncStatusLine(isGuest, syncStatus, headerContentColor.copy(alpha = 0.78f))
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { aboutOpen = true }, modifier = Modifier.size(42.dp)) {
                                Text("ⓘ", color = headerContentColor, fontSize = 20.sp)
                            }
                            IconButton(onClick = { settingsOpen = true }, modifier = Modifier.size(42.dp)) {
                                Text("⚙", color = headerContentColor, fontSize = 19.sp)
                            }
                        }
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 18.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        categoryItems.forEach { category ->
                            val selected = selectedCategory == category
                            val animatedChipColor by androidx.compose.animation.animateColorAsState(
                                targetValue = if (selected) roleColor(settings.sectionColor, accentColor(settings.accent))
                                else mixColors(roleColor(settings.sectionColor, accentColor(settings.accent)), MaterialTheme.colorScheme.background, 0.32f),
                                animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
                                label = "section-chip"
                            )
                            Button(
                                onClick = {
                                    selectedCategory = category
                                    searchText = ""
                                    appliedSearchText = ""
                                    sortMode = 0
                                },
                                modifier = Modifier.height(40.dp).onGloballyPositioned {
                                    categoryDropBounds[category] = it.boundsInRoot()
                                },
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = animatedChipColor,
                                    contentColor = contrastColor(animatedChipColor)
                                ),
                                contentPadding = PaddingValues(horizontal = 15.dp, vertical = 2.dp)
                            ) {
                                Text(category, fontSize = 12.sp, maxLines = 1, softWrap = false)
                            }
                        }
                        IconButton(
                            onClick = { newCategoryName = ""; showCategoryDialog = true },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Text("+", color = headerContentColor, fontSize = 25.sp)
                        }
                        if (selectedCategory != null) {
                            IconButton(
                                onClick = { categoryToDelete = selectedCategory },
                                modifier = Modifier.size(40.dp)
                            ) {
                                Text("−", color = headerContentColor.copy(alpha = 0.8f), fontSize = 24.sp)
                            }
                        }
                    }
                }

                Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                    Column(
                        modifier = Modifier.fillMaxSize().graphicsLayer {
                            scaleX = sectionMotion.value
                            scaleY = sectionMotion.value
                        }
                    ) {
                        if (selectedCategory == null) {
                            Column(
                                modifier = Modifier.fillMaxSize().padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text("Выберите тип заметок", color = MaterialTheme.colorScheme.onBackground, fontSize = 20.sp, fontWeight = FontWeight.Medium)
                                Spacer(Modifier.height(8.dp))
                                Text("Разделы находятся на верхней панели", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                            }
                        } else {
                            Column(
                                modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        selectedCategory.orEmpty(),
                                        modifier = Modifier.weight(1f),
                                        color = MaterialTheme.colorScheme.onBackground,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Box {
                                        TextButton(onClick = { showSortMenu = true }, contentPadding = PaddingValues(horizontal = 9.dp)) {
                                            Text(if (settings.sortBy == "Пользовательский") "Пользовательский ▾" else "Сортировка ▾", fontSize = 12.sp, maxLines = 1, softWrap = false)
                                        }
                                        DropdownMenu(
                                            expanded = showSortMenu,
                                            onDismissRequest = { showSortMenu = false }
                                        ) {
                                            DropdownMenuItem(
                                                text = { Text("Пользовательский порядок") },
                                                onClick = {
                                                    onSettingsChange(settings.copy(sortBy = "Пользовательский"))
                                                    sortMode = 0
                                                    showSortMenu = false
                                                }
                                            )
                                            DropdownMenuItem(
                                                text = { Text("По настройке: ${settings.sortBy}") },
                                                onClick = { sortMode = 0; showSortMenu = false }
                                            )
                                            DropdownMenuItem(
                                                text = { Text("Обратный порядок") },
                                                onClick = { sortMode = 1; showSortMenu = false }
                                            )
                                            DropdownMenuItem(
                                                text = { Text("Только избранное") },
                                                onClick = { sortMode = 2; showSortMenu = false }
                                            )
                                        }
                                    }
                                }
                                OutlinedTextField(
                                    value = searchText,
                                    onValueChange = { searchText = it },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    placeholder = { Text("Найти заметку") },
                                    leadingIcon = { Text("⌕", fontSize = 24.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                                    trailingIcon = {
                                        if (searchText.isNotEmpty()) {
                                            IconButton(onClick = { searchText = ""; appliedSearchText = "" }) {
                                                Text("×", fontSize = 22.sp)
                                            }
                                        }
                                    },
                                    shape = RoundedCornerShape(16.dp),
                                    colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.50f),
                                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                                        unfocusedBorderColor = Color.Transparent
                                    )
                                )
                                Spacer(Modifier.height(8.dp))
                                Box(Modifier.fillMaxWidth().weight(1f)) {
                                    LazyColumn(
                                        state = noteListState,
                                        modifier = Modifier.fillMaxSize(),
                                        verticalArrangement = Arrangement.spacedBy(14.dp),
                                        contentPadding = PaddingValues(top = 4.dp, bottom = 108.dp)
                                    ) {
                                        items(visibleNotes, key = { it.note.id }, contentType = { "note" }) { row ->
                                            val note = row.note
                                            val canDragNotes = settings.sortBy == "Пользовательский" && appliedSearchText.isBlank() && sortMode == 0
                                            Box(
                                                Modifier.fillMaxWidth().animateItem().then(
                                                    if (canDragNotes) Modifier.onGloballyPositioned {
                                                        noteCardBounds[note.id] = it.boundsInRoot()
                                                    } else Modifier
                                                )
                                            ) {
                                                NoteCard(
                                                    note = note,
                                                    settings = settings,
                                                    previewText = row.plainBody,
                                                    dragEnabled = canDragNotes,
                                                    isDragging = activeDragNoteId == note.id,
                                                    dragOffset = Offset.Zero,
                                                    onDragStart = { localPoint ->
                                                        val point = noteCardBounds[note.id]?.topLeft?.plus(localPoint) ?: localPoint
                                                        noteDragState.origin = point
                                                        noteDragState.pointer = point
                                                        noteDragState.offset = Offset.Zero
                                                        noteDragState.pointerState.value = point
                                                        noteDragState.offsetState.value = Offset.Zero
                                                        activeDragPointerState.value = point
                                                        activeDragNoteId = note.id
                                                    },
                                                    onDragMove = { delta ->
                                                        noteDragState.offset += delta
                                                        noteDragState.pointer = noteDragState.origin + noteDragState.offset
                                                        noteDragState.offsetState.value = noteDragState.offset
                                                        noteDragState.pointerState.value = noteDragState.pointer
                                                        activeDragPointerState.value = noteDragState.pointer
                                                    },
                                                    onDragEnd = { finishNoteDrag(note) },
                                                    onDragCancel = {
                                                        activeDragNoteId = null
                                                        noteDragState.offset = Offset.Zero
                                                        noteDragState.offsetState.value = Offset.Zero
                                                    },
                                                    onEdit = {
                                                        noteBeingEdited = note
                                                        editorOpen = true
                                                    },
                                                    onCopy = {
                                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                        val copiedText = buildString {
                                                            if (note.title.isNotBlank()) append(note.title).append("\n\n")
                                                            append(note.plainBody)
                                                        }
                                                        clipboard.setPrimaryClip(ClipData.newPlainText("Заметка", copiedText))
                                                        Toast.makeText(context, "Заметка скопирована", Toast.LENGTH_SHORT).show()
                                                    },
                                                    onDelete = {
                                                        if (settings.confirmDelete) noteToDelete = note else {
                                                            val index = notes.indexOfFirst { it.id == note.id }
                                                            if (index >= 0) { cancelNoteReminders(context, notes[index]); notes[index] = notes[index].copy(isDeleted = true, updatedAt = System.currentTimeMillis(), reminderAt = 0L, reminders = emptyList()) }
                                                            persist()
                                                        }
                                                    },
                                                    onToggleFavorite = {
                                                        val index = notes.indexOfFirst { it.id == note.id }
                                                        if (index >= 0) {
                                                            notes[index] = notes[index].copy(isFavorite = !notes[index].isFavorite)
                                                            persist()
                                                        }
                                                    }
                                                )
                                            }
                                        }
                                    }
                                    if (visibleNotes.isEmpty()) {
                                        Column(
                                            modifier = Modifier.align(Alignment.Center).padding(24.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Text(if (searchText.isBlank()) "В этом разделе пока нет заметок" else "Ничего не найдено", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 15.sp)
                                            if (searchText.isBlank()) Text("Нажмите «+», чтобы добавить первую", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    if (selectedCategory != null) {
                        FloatingActionButton(
                            onClick = {
                                noteBeingEdited = null
                                editorOpen = true
                            },
                            modifier = Modifier.align(Alignment.BottomEnd).padding(end = 16.dp, bottom = 24.dp).size(54.dp).graphicsLayer { scaleX = addButtonScale; scaleY = addButtonScale },
                            containerColor = roleColor(settings.addButtonColor, accentColor(settings.accent)),
                            contentColor = contrastColor(roleColor(settings.addButtonColor, accentColor(settings.accent))),
                            shape = RoundedCornerShape(18.dp)
                        ) { Text("+", fontSize = 29.sp, fontWeight = FontWeight.Normal) }
                    }
                }
            }

            if (showCategoryDialog) {
                AlertDialog(
                    onDismissRequest = { showCategoryDialog = false },
                    title = { Text("Новый раздел") },
                    text = {
                        OutlinedTextField(
                            value = newCategoryName,
                            onValueChange = { newCategoryName = it },
                            label = { Text("Название раздела") },
                            singleLine = true
                        )
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            val name = newCategoryName.trim()
                            if (name.isNotEmpty() && categoryList.none { it.equals(name, ignoreCase = true) }) {
                                categoryList.add(name)
                                writeCategories(context, categoryList.toList())
                                selectedCategory = name
                                searchText = ""
                                appliedSearchText = ""
                                sortMode = 0
                                showCategoryDialog = false
                            }
                        }, enabled = newCategoryName.isNotBlank()) { Text("Добавить") }
                    },
                    dismissButton = { TextButton(onClick = { showCategoryDialog = false }) { Text("Отмена") } }
                )
            }

            categoryToDelete?.let { target ->
                val count = notes.count { it.category == target }
                AlertDialog(
                    onDismissRequest = { categoryToDelete = null },
                    title = { Text("Удалить раздел?") },
                    text = { Text(if (count == 0) "Раздел «$target» будет удалён." else "Раздел «$target» и все его заметки ($count) будут удалены с этого телефона.") },
                    confirmButton = {
                        TextButton(onClick = {
                            categoryList.remove(target)
                            writeCategories(context, categoryList.toList())
                            val now = System.currentTimeMillis()
                            notes.indices.filter { notes[it].category == target && !notes[it].isDeleted }.forEach { index ->
                                cancelNoteReminders(context, notes[index])
                                notes[index] = notes[index].copy(isDeleted = true, updatedAt = now, reminderAt = 0L, reminders = emptyList())
                            }
                            persist()
                            selectedCategory = null
                            categoryToDelete = null
                        }) { Text("Удалить", color = MaterialTheme.colorScheme.error) }
                    },
                    dismissButton = { TextButton(onClick = { categoryToDelete = null }) { Text("Отмена") } }
                )
            }

            noteMoveToConfirm?.let { (noteId, destination) ->
                val target = notes.firstOrNull { it.id == noteId }
                if (target != null) {
                    Dialog(onDismissRequest = { noteMoveToConfirm = null }) {
                        Surface(
                            shape = RoundedCornerShape(26.dp),
                            color = MaterialTheme.colorScheme.surface,
                            tonalElevation = 10.dp
                        ) {
                            Column(
                                Modifier.fillMaxWidth().padding(24.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    Modifier.size(44.dp).clip(CircleShape)
                                        .background(roleColor(settings.sectionColor, accentColor(settings.accent))),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("↗", color = contrastColor(roleColor(settings.sectionColor, accentColor(settings.accent))), fontSize = 24.sp)
                                }
                                Text("Перенести заметку?", color = MaterialTheme.colorScheme.onSurface, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                                Text(
                                    "«${target.title.ifBlank { "Без названия" }}» будет перемещена в раздел «$destination».",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 14.sp
                                )
                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    TextButton(onClick = { noteMoveToConfirm = null }) { Text("Отмена") }
                                    Spacer(Modifier.width(8.dp))
                                    Button(
                                        onClick = {
                                            val index = notes.indexOfFirst { it.id == noteId }
                                            if (index >= 0) {
                                                notes[index] = notes[index].copy(category = destination)
                                                onSettingsChange(settings.copy(sortBy = "Пользовательский"))
                                                val existingDestinationOrder = manualOrderByCategory[destination].orEmpty()
                                                val destinationRanks = existingDestinationOrder.withIndex().associate { it.value to it.index }
                                                val destinationOrder = notes.asSequence()
                                                    .filter { it.category == destination && !it.isDeleted && it.id != noteId }
                                                    .map { it.id }
                                                    .sortedBy { destinationRanks[it] ?: Int.MAX_VALUE }
                                                    .toList() + noteId
                                                manualOrderByCategory[destination] = destinationOrder
                                                context.getSharedPreferences("mom_notes", Context.MODE_PRIVATE).edit()
                                                    .putString("manual_order_${accountId}_$destination", JSONArray(destinationOrder).toString())
                                                    .apply()
                                                manualOrderRevision++
                                                selectedCategory = destination
                                                sortMode = 0
                                                persist()
                                            }
                                            noteMoveToConfirm = null
                                        },
                                        shape = RoundedCornerShape(14.dp)
                                    ) { Text("Перенести") }
                                }
                            }
                        }
                    }
                }
            }

            noteToDelete?.let { target ->
                AlertDialog(
                    onDismissRequest = { noteToDelete = null },
                    title = { Text("Удалить заметку?") },
                    text = { Text("«${target.title.ifBlank { "Без названия" }}» будет удалена с этого телефона.") },
                    confirmButton = {
                        TextButton(onClick = {
                            val index = notes.indexOfFirst { it.id == target.id }
                            if (index >= 0) { cancelNoteReminders(context, notes[index]); notes[index] = notes[index].copy(isDeleted = true, updatedAt = System.currentTimeMillis(), reminderAt = 0L, reminders = emptyList()) }
                            persist()
                            noteToDelete = null
                        }) { Text("Удалить", color = MaterialTheme.colorScheme.error) }
                    },
                    dismissButton = { TextButton(onClick = { noteToDelete = null }) { Text("Отмена") } }
                )
            }
        }
        NoteDragDimOverlay(visible = activeDragNoteId != null)
        activeDragNoteId?.let { draggingId ->
            val draggedNote = notes.firstOrNull { it.id == draggingId }
            val bounds = noteCardBounds[draggingId]
            if (draggedNote != null && bounds != null) {
                NoteDragOverlay(
                    note = draggedNote,
                    bounds = bounds,
                    dragState = noteDragState,
                    categoryBounds = categoryDropBounds.toMap(),
                    settings = settings
                )
            }
        }
        AnimatedVisibility(
            visible = editorOpen,
            enter = fadeIn(tween(140)),
            exit = fadeOut(tween(100))
        ) {
            EditorScreen(
                note = noteBeingEdited,
                category = selectedCategory.orEmpty(),
                allNotes = notes.toList(),
                settings = settings,
                onAutoSave = { draft ->
                    val index = notes.indexOfFirst { it.id == draft.id }
                    val retained = keepUploadedFileIds(notes.getOrNull(index), draft)
                    if (index >= 0) notes[index] = retained else notes.add(retained)
                    updateReminder(context, retained)
                    persist()
                },
                onClose = { editorOpen = false },
                onSave = { edited ->
                    val index = notes.indexOfFirst { it.id == edited.id }
                    val retained = keepUploadedFileIds(notes.getOrNull(index), edited)
                    if (index >= 0) notes[index] = retained else notes.add(retained)
                    updateReminder(context, retained)
                    persist()
                    editorOpen = false
                }
            )
        }
        AnimatedVisibility(
            visible = settingsOpen,
            enter = fadeIn(tween(140)),
            exit = fadeOut(tween(100))
        ) {
            SettingsScreen(
                settings = settings,
                onSettingsChange = onSettingsChange,
                noteCount = notes.size,
                categoryCount = categoryList.size,
                onExport = { exportLauncher.launch("Заметки-резервная-копия.json") },
                onImport = { importLauncher.launch(arrayOf("application/json", "text/json", "*/*")) },
                onAbout = { settingsOpen = false; aboutOpen = true },
                onBack = { settingsOpen = false },
                onReset = { onSettingsChange(AppSettings()) },
                onLogout = onLogout,
                isGuest = isGuest
            )
        }
        AnimatedVisibility(
            visible = aboutOpen,
            enter = fadeIn(tween(140)),
            exit = fadeOut(tween(100))
        ) {
            AboutScreen(onBack = { aboutOpen = false })
        }
    }

}


@Composable
private fun ReminderPickerDialog(onDismiss: () -> Unit, onConfirm: (Long, String) -> Unit) {
    val initial = remember { Calendar.getInstance().apply { add(Calendar.MINUTE, 10); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) } }
    var day by remember { mutableStateOf(initial.get(Calendar.DAY_OF_MONTH).toString()) }
    var month by remember { mutableStateOf((initial.get(Calendar.MONTH) + 1).toString()) }
    var year by remember { mutableStateOf(initial.get(Calendar.YEAR).toString()) }
    var hour by remember { mutableStateOf(initial.get(Calendar.HOUR_OF_DAY).toString().padStart(2, '0')) }
    var minute by remember { mutableStateOf(initial.get(Calendar.MINUTE).toString().padStart(2, '0')) }
    var reminderType by remember { mutableStateOf("Уведомление и будильник") }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Новое напоминание", fontWeight = FontWeight.SemiBold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Выбери дату и время, напоминание сохранится в этой заметке.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(day, { day = it.filter(Char::isDigit).take(2) }, Modifier.weight(0.7f), label = { Text("День") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                    OutlinedTextField(month, { month = it.filter(Char::isDigit).take(2) }, Modifier.weight(0.7f), label = { Text("Месяц") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                    OutlinedTextField(year, { year = it.filter(Char::isDigit).take(4) }, Modifier.weight(1.2f), label = { Text("Год") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(hour, { hour = it.filter(Char::isDigit).take(2) }, Modifier.weight(1f), label = { Text("Часы") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                    Text(":", fontSize = 20.sp)
                    OutlinedTextField(minute, { minute = it.filter(Char::isDigit).take(2) }, Modifier.weight(1f), label = { Text("Минуты") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                }
                ChoiceRow(
                    label = "Как напомнить",
                    value = reminderType,
                    options = listOf("Уведомление", "Будильник", "Уведомление и будильник"),
                    onSelect = { reminderType = it }
                )
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val d = day.toIntOrNull(); val m = month.toIntOrNull(); val y = year.toIntOrNull()
                val h = hour.toIntOrNull(); val min = minute.toIntOrNull()
                if (d == null || m == null || y == null || h == null || min == null || m !in 1..12 || h !in 0..23 || min !in 0..59) {
                    error = "Проверь дату и время."
                } else {
                    val selected = Calendar.getInstance().apply { clear(); set(y, m - 1, d, h, min, 0) }
                    if (selected.get(Calendar.YEAR) != y || selected.get(Calendar.MONTH) != m - 1 || selected.get(Calendar.DAY_OF_MONTH) != d) error = "Такой даты нет в календаре."
                    else if (selected.timeInMillis <= System.currentTimeMillis()) error = "Выбери время в будущем."
                    else onConfirm(selected.timeInMillis, reminderType)
                }
            }) { Text("Добавить") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } }
    )
}

private fun cancelNoteReminders(context: Context, note: Note) {
    (note.reminders + listOfNotNull(note.reminderAt.takeIf { it > 0L })).distinct().forEach { time ->
        NoteReminderScheduler.cancel(context, note.id, time)
    }
}

private fun updateReminder(context: Context, note: Note) {
    cancelNoteReminders(context, note)
    if (!note.isDeleted) {
        (note.reminders + listOfNotNull(note.reminderAt.takeIf { it > 0L })).distinct().forEach { time ->
            NoteReminderScheduler.schedule(context, note.id, note.title.ifBlank { "Напоминание из заметок" }, time, note.reminderTypes[time] ?: "Уведомление и будильник")
        }
    }
}

@Composable
private fun CloudSyncStatusLine(isGuest: Boolean, status: androidx.compose.runtime.State<String>, color: Color) {
    val label = if (isGuest) "Только на устройстве" else when (status.value) {
        "Синхронизировано" -> "☁ Синхронизировано"
        "Ошибка синхронизации" -> "☁ Ошибка синхронизации"
        else -> "☁ Синхронизация…"
    }
    Text(label, color = color, fontSize = 10.sp, maxLines = 1)
}

@Composable
private fun NoteDragDimOverlay(visible: Boolean) {
    val alpha by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (visible) 0.12f else 0f,
        animationSpec = tween(140, easing = FastOutSlowInEasing),
        label = "note-drag-dim"
    )
    if (alpha > 0f) {
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = alpha)))
    }
}

@Composable
private fun NoteDragOverlay(
    note: Note,
    bounds: Rect,
    dragState: NoteDragState,
    categoryBounds: Map<String, Rect>,
    settings: AppSettings
) {
    val hoveredCategory by remember(categoryBounds, bounds, note.category, dragState) {
        derivedStateOf {
            val dragOffset = dragState.offsetState.value
            val draggedBounds = Rect(
                bounds.left + dragOffset.x,
                bounds.top + dragOffset.y,
                bounds.right + dragOffset.x,
                bounds.bottom + dragOffset.y
            )
            categoryBounds.entries.firstOrNull { (category, tabBounds) ->
                category != note.category && draggedBounds.overlaps(tabBounds)
            }?.key
        }
    }
    val overAnotherSection = hoveredCategory != null && hoveredCategory != note.category
    val scale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (overAnotherSection) 0.52f else 1f,
        animationSpec = tween(170, easing = FastOutSlowInEasing),
        label = "note-drag-scale"
    )
    val density = LocalDensity.current
    Box(
        Modifier
            .offset {
                IntOffset(
                    bounds.left.roundToInt() + dragState.offsetState.value.x.roundToInt(),
                    bounds.top.roundToInt() + dragState.offsetState.value.y.roundToInt()
                )
            }
            .width(with(density) { bounds.width.toDp() })
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                transformOrigin = TransformOrigin(0.5f, 0.5f)
            }
            .zIndex(5f)
    ) {
        NoteCard(
            note = note,
            settings = settings,
            previewText = note.plainBody,
            dragEnabled = false,
            isDragging = true,
            dragOffset = Offset.Zero,
            isDragPreview = true,
            onDragStart = {},
            onDragMove = {},
            onDragEnd = {},
            onDragCancel = {},
            onEdit = {},
            onCopy = {},
            onDelete = {},
            onToggleFavorite = {}
        )
    }
}

@Composable
private fun NoteCard(
    note: Note,
    settings: AppSettings,
    previewText: String,
    dragEnabled: Boolean,
    isDragging: Boolean,
    dragOffset: Offset,
    isDragPreview: Boolean = false,
    onDragStart: (Offset) -> Unit,
    onDragMove: (Offset) -> Unit,
    onDragEnd: () -> Unit,
    onDragCancel: () -> Unit,
    onEdit: () -> Unit,
    onCopy: () -> Unit,
    onDelete: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    val bodyPreview = remember(previewText) {
        previewText.lineSequence().take(3).joinToString("\n").take(240)
    }
    val formattedDate = remember(note.updatedAt) { formatDate(note.updatedAt) }
    val futureReminders = remember(note.reminders, note.reminderAt) {
        (note.reminders + listOfNotNull(note.reminderAt.takeIf { it > 0L }))
            .distinct()
            .filter { it > System.currentTimeMillis() }
            .sorted()
    }
    var actionsExpanded by remember(note.id) { mutableStateOf(false) }
    val baseCardColor = roleColor(settings.noteColor, MaterialTheme.colorScheme.surface)
    val colorChroma = maxOf(baseCardColor.red, baseCardColor.green, baseCardColor.blue) -
            minOf(baseCardColor.red, baseCardColor.green, baseCardColor.blue)
    val colorSoftening = if (settings.noteColor.isNotBlank() && colorChroma > 0.22f) 0.42f else 0.14f
    val cardColor = mixColors(baseCardColor, MaterialTheme.colorScheme.surface, colorSoftening)
    val textColor = if (settings.noteColor.isBlank()) MaterialTheme.colorScheme.onSurface else contrastColor(cardColor)
    val focusColor = Color(1f - cardColor.red, 1f - cardColor.green, 1f - cardColor.blue)
    val cardBorderColor by androidx.compose.animation.animateColorAsState(
        targetValue = if (isDragging) focusColor else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.32f),
        animationSpec = tween(180, easing = FastOutSlowInEasing),
        label = "note-focus-border"
    )
    val dragScale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (isDragging) 1.025f else 1f,
        animationSpec = tween(190, easing = FastOutSlowInEasing),
        label = "note-drag-scale"
    )
    val cardSubtextColor = if (settings.noteColor.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant else textColor.copy(alpha = 0.78f)
    val bodySize = when (settings.noteTextSize) { "Маленький" -> 12; "Большой" -> 17; else -> 14 }
    val corner = when (settings.corner) { "Маленькое" -> 12; "Большое" -> 24; else -> 20 }
    val gap = if (settings.density == "Компактно") 4.dp else 10.dp

    val dragGestureModifier = Modifier.pointerInput(note.id, dragEnabled) {
        if (dragEnabled) {
            detectDragGesturesAfterLongPress(
                onDragStart = onDragStart,
                onDragEnd = onDragEnd,
                onDragCancel = onDragCancel,
                onDrag = { change, dragAmount ->
                    onDragMove(dragAmount)
                    change.consume()
                }
            )
        }
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(dragGestureModifier)
            .graphicsLayer {
                scaleX = dragScale
                scaleY = dragScale
                translationX = if (isDragging) dragOffset.x else 0f
                translationY = if (isDragging) dragOffset.y else 0f
                shadowElevation = if (isDragging) 18.dp.toPx() else 2.dp.toPx()
                alpha = if (isDragging && !isDragPreview) 0f else 1f
            }
            .zIndex(if (isDragging) 2f else 0f)
            .clickable(onClick = onEdit),
        shape = RoundedCornerShape(corner.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isDragging) 8.dp else 2.dp),
        border = BorderStroke(if (isDragging) 3.dp else 1.dp, cardBorderColor)
    ) {
        if (settings.listStyle == "Строки") {
            Row(
                Modifier.fillMaxWidth().padding(start = 14.dp, end = 5.dp, top = gap, bottom = gap),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        note.title.ifBlank { "Без названия" },
                        color = textColor,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = (bodySize + 2).sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = (bodyPreview.lineSequence().firstOrNull().orEmpty().ifBlank { note.category }).take(140),
                        color = cardSubtextColor,
                        fontSize = bodySize.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (settings.showModifiedDate) {
                        Text(formattedDate, color = cardSubtextColor, fontSize = 11.sp)
                    }
                }
                IconButton(onClick = onToggleFavorite, modifier = Modifier.size(40.dp)) {
                    Text(if (note.isFavorite) "★" else "☆", color = Color(0xFFFFC857), fontSize = 23.sp)
                }
                NoteCardActions(
                    expanded = actionsExpanded,
                    onExpand = { actionsExpanded = true },
                    onDismiss = { actionsExpanded = false },
                    onOpen = onEdit,
                    onCopy = onCopy,
                    onDelete = onDelete
                )
            }
        } else {
            Column(Modifier.padding(start = 15.dp, end = 6.dp, top = gap + 5.dp, bottom = gap)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = note.title.ifBlank { "Без названия" },
                        modifier = Modifier.weight(1f),
                        color = textColor,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = (bodySize + 3).sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    IconButton(onClick = onToggleFavorite, modifier = Modifier.size(40.dp)) {
                        Text(if (note.isFavorite) "★" else "☆", color = Color(0xFFFFC857), fontSize = 24.sp)
                    }
                    NoteCardActions(
                        expanded = actionsExpanded,
                        onExpand = { actionsExpanded = true },
                        onDismiss = { actionsExpanded = false },
                        onOpen = onEdit,
                        onCopy = onCopy,
                        onDelete = onDelete
                    )
                }
                if (bodyPreview.isNotBlank()) {
                    Text(
                        text = bodyPreview,
                        color = cardSubtextColor,
                        fontSize = bodySize.sp,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = (bodySize + 6).sp
                    )
                }
                if (futureReminders.isNotEmpty() || note.photoPaths.isNotEmpty() || note.attachmentPaths.isNotEmpty()) {
                    Row(
                        modifier = Modifier.padding(top = 7.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (futureReminders.isNotEmpty()) {
                            Text(
                                "◷ ${futureReminders.size} · ${formatReminderDate(futureReminders.first())}",
                                color = cardSubtextColor,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        if (note.photoPaths.isNotEmpty() || note.attachmentPaths.isNotEmpty()) {
                            Text(
                                "Вложения ${note.photoPaths.size + note.attachmentPaths.size}",
                                color = cardSubtextColor,
                                fontSize = 11.sp,
                                maxLines = 1
                            )
                        }
                    }
                }
                if (settings.showModifiedDate) {
                    Text(
                        formattedDate,
                        modifier = Modifier.padding(top = 8.dp),
                        color = cardSubtextColor.copy(alpha = 0.82f),
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun NoteCardActions(
    expanded: Boolean,
    onExpand: () -> Unit,
    onDismiss: () -> Unit,
    onOpen: () -> Unit,
    onCopy: () -> Unit,
    onDelete: () -> Unit
) {
    Box {
        IconButton(onClick = onExpand, modifier = Modifier.size(40.dp)) {
            Text("⋯", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 25.sp)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
            DropdownMenuItem(
                text = { Text("Открыть заметку") },
                onClick = { onDismiss(); onOpen() }
            )
            DropdownMenuItem(
                text = { Text("Скопировать текст") },
                onClick = { onDismiss(); onCopy() }
            )
            DropdownMenuItem(
                text = { Text("Удалить", color = MaterialTheme.colorScheme.error) },
                onClick = { onDismiss(); onDelete() }
            )
        }
    }
}

@Composable
private fun EditorScreen(
    note: Note?,
    category: String,
    allNotes: List<Note>,
    settings: AppSettings,
    onAutoSave: (Note) -> Unit,
    onClose: () -> Unit,
    onSave: (Note) -> Unit
) {
    val context = LocalContext.current
    val mediaScope = rememberCoroutineScope()
    var title by remember(note?.id) { mutableStateOf(note?.title.orEmpty()) }
    var body by remember(note?.id) { mutableStateOf(note?.body.orEmpty()) }
    val reminderTimes = remember(note?.id) {
        mutableStateListOf<Long>().apply {
            addAll((note?.reminders.orEmpty() + listOfNotNull(note?.reminderAt?.takeIf { it > 0L })).distinct().sorted())
        }
    }
    val reminderTypes = remember(note?.id) {
        mutableStateMapOf<Long, String>().apply {
            putAll(note?.reminderTypes.orEmpty())
            (note?.reminders.orEmpty() + listOfNotNull(note?.reminderAt?.takeIf { it > 0L })).forEach {
                if (get(it).isNullOrBlank()) put(it, "Уведомление и будильник")
            }
        }
    }
    val bodyHasContent = remember(body) { body.isNotBlank() && RichTextDocument.plainText(body).isNotBlank() }
    var selectedAi by remember(note?.id) { mutableStateOf(note?.aiAssistant ?: "Без ИИ") }
    var aiInstruction by remember(note?.id) { mutableStateOf(note?.aiInstruction.orEmpty()) }
    val exactAlarmSettingsLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        // Any saved reminders are re-armed after access changes; unsaved editor state is not scheduled.
        NoteReminderScheduler.rescheduleSaved(context)
    }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !NoteReminderScheduler.canScheduleExactly(context)) {
            runCatching { exactAlarmSettingsLauncher.launch(NoteReminderScheduler.exactAlarmSettingsIntent(context)) }
        }
    }
    fun requestReminderPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !NoteReminderScheduler.canScheduleExactly(context)) {
            runCatching { exactAlarmSettingsLauncher.launch(NoteReminderScheduler.exactAlarmSettingsIntent(context)) }
        }
    }
    val photoPaths = remember(note?.id) { mutableStateListOf<String>().apply { addAll(note?.photoPaths.orEmpty()) } }
    val attachmentPaths = remember(note?.id) { mutableStateListOf<String>().apply { addAll(note?.attachmentPaths.orEmpty()) } }
    val photoIds = remember(note?.id) { mutableStateListOf<String>().apply { addAll(note?.photoIds.orEmpty()) } }
    val attachmentIds = remember(note?.id) { mutableStateListOf<String>().apply { addAll(note?.attachmentIds.orEmpty()) } }
    val tableList = remember(note?.id) { mutableStateListOf<NoteTable>().apply { addAll(note?.tables.orEmpty()) } }
    var showTableSizeDialog by remember { mutableStateOf(false) }
    var tableRowCount by remember { mutableStateOf(3) }
    var tableColumnCount by remember { mutableStateOf(3) }
    val history = remember(note?.id) { mutableStateListOf(DraftSnapshot(title, body)) }
    var historyIndex by remember(note?.id) { mutableStateOf(0) }
    var showExitDialog by remember { mutableStateOf(false) }
    var showInsertMenu by remember { mutableStateOf(false) }
    var showShareMenu by remember { mutableStateOf(false) }
    var showLinkDialog by remember { mutableStateOf(false) }
    var showAttachNoteDialog by remember { mutableStateOf(false) }
    var linkLabel by remember { mutableStateOf("") }
    var linkAddress by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }
    var showReminderPicker by remember { mutableStateOf(false) }

    var historyJob by remember(note?.id) { mutableStateOf<Job?>(null) }
    fun record(nextTitle: String = title, nextBody: String = body) {
        historyJob?.cancel()
        historyJob = mediaScope.launch {
            delay(450)
            val snapshot = DraftSnapshot(nextTitle, nextBody)
            if (history.getOrNull(historyIndex) == snapshot) return@launch
            while (history.lastIndex > historyIndex) history.removeAt(history.lastIndex)
            history.add(snapshot)
            if (history.size > 50) history.removeAt(0) else historyIndex++
            if (historyIndex >= history.size) historyIndex = history.lastIndex
        }
    }

    val draftId = remember(note?.id) { note?.id ?: UUID.randomUUID().toString() }

    fun currentDraft(): Note {
        val now = System.currentTimeMillis()
        return Note(
            id = draftId,
            category = category,
            title = title.trim(),
            body = body,
            reminderAt = reminderTimes.firstOrNull() ?: 0L,
            reminders = reminderTimes.toList(),
            reminderTypes = reminderTypes.toMap(),
            updatedAt = now,
            createdAt = note?.createdAt ?: now,
            isFavorite = note?.isFavorite ?: false,
            isDeleted = note?.isDeleted ?: false,
            photoPaths = photoPaths.toList(),
            attachmentPaths = attachmentPaths.toList(),
            photoIds = photoIds.toList(),
            attachmentIds = attachmentIds.toList(),
            tables = tableList.toList(),
            aiAssistant = selectedAi,
            aiInstruction = aiInstruction.trim()
        )
    }

    fun setTitle(value: String) {
        title = value
        record(nextTitle = value)
    }

    fun setBody(value: String) {
        body = value
        record(nextBody = value)
    }

    fun insertIntoBody(value: String) {
        val plain = RichTextDocument.plainText(body)
        val prefix = if (plain.isBlank()) "" else if (plain.endsWith("\n")) "" else "\n"
        setBody(RichTextDocument.appendText(body, prefix + value))
    }

    LaunchedEffect(title, body, settings.autoSave) {
        if (settings.autoSave && (title.isNotBlank() || bodyHasContent || reminderTimes.isNotEmpty() || tableList.isNotEmpty())) {
            delay(700)
            onAutoSave(currentDraft())
        }
    }

    fun saveDraft() {
        historyJob?.cancel()
        onSave(currentDraft())
    }

    val speechLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val recognized = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
            if (!recognized.isNullOrBlank()) insertIntoBody(recognized.trim())
        }
    }
    fun launchSpeech() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, if (settings.voiceLanguage == "Системный") Locale.getDefault().toLanguageTag() else "ru-RU")
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Продиктуйте текст заметки")
        }
        try {
            speechLauncher.launch(intent)
        } catch (_: Exception) {
            message = "На телефоне не найдено приложение для распознавания речи. Можно использовать микрофон клавиатуры."
        }
    }
    val photoLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
        if (uri != null) mediaScope.launch {
            val result = withContext(Dispatchers.IO) { runCatching { copyUriToPrivateFile(context, uri, "note_photos") } }
            val copied = result.getOrNull()
            if (copied != null) {
                photoPaths.add(copied.absolutePath)
                if (settings.autoSave) onAutoSave(currentDraft())
            } else {
                val detail = result.exceptionOrNull()?.localizedMessage?.takeIf { it.isNotBlank() }?.take(140)
                message = if (detail == null) "Не удалось добавить фотографию." else "Не удалось прочитать фотографию: $detail"
            }
        }
    }
    val documentLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) mediaScope.launch {
            val result = withContext(Dispatchers.IO) { runCatching { copyUriToPrivateFile(context, uri, "note_files") } }
            val copied = result.getOrNull()
            if (copied != null) {
                attachmentPaths.add(copied.absolutePath)
                if (settings.autoSave) onAutoSave(currentDraft())
            } else {
                val detail = result.exceptionOrNull()?.localizedMessage?.takeIf { it.isNotBlank() }?.take(140)
                message = if (detail == null) "Не удалось добавить вложение." else "Не удалось прочитать файл: $detail"
            }
        }
    }

    fun hasUnsavedChanges(): Boolean {
        if (title.trim() != note?.title.orEmpty() || RichTextDocument.canonical(body) != RichTextDocument.canonical(note?.body.orEmpty()) || reminderTimes.toList() != (note?.reminders.orEmpty() + listOfNotNull(note?.reminderAt?.takeIf { it > 0L })).distinct().sorted() || reminderTypes.toMap() != note?.reminderTypes.orEmpty()) return true
        if (selectedAi != (note?.aiAssistant ?: "Без ИИ") || aiInstruction.trim() != note?.aiInstruction.orEmpty()) return true
        if (photoPaths.toList() != note?.photoPaths.orEmpty() || attachmentPaths.toList() != note?.attachmentPaths.orEmpty()) return true
        if (tableList.toList() != note?.tables.orEmpty()) return true
        return false
    }

    fun requestExit() {
        if (!hasUnsavedChanges()) onClose() else showExitDialog = true
    }
    BackHandler { requestExit() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(58.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TextButton(onClick = { requestExit() }, contentPadding = PaddingValues(horizontal = 5.dp)) {
                Text("←", color = MaterialTheme.colorScheme.onBackground, fontSize = 27.sp)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box {
                    TextButton(
                        onClick = { showShareMenu = true },
                        enabled = title.isNotBlank() || bodyHasContent || tableList.isNotEmpty() || photoPaths.isNotEmpty() || attachmentPaths.isNotEmpty(),
                        contentPadding = PaddingValues(horizontal = 6.dp)
                    ) { Text("↗", color = MaterialTheme.colorScheme.primary, fontSize = 22.sp) }
                    DropdownMenu(expanded = showShareMenu, onDismissRequest = { showShareMenu = false }) {
                        DropdownMenuItem(text = { Text("PDF") }, onClick = { showShareMenu = false; shareNoteAsPdf(context, mediaScope, currentDraft()) })
                        DropdownMenuItem(text = { Text("Markdown (.md)") }, onClick = { showShareMenu = false; shareNoteAsMarkdown(context, currentDraft()) })
                        DropdownMenuItem(text = { Text("Текст (.txt)") }, onClick = { showShareMenu = false; shareNoteAsTextFile(context, currentDraft()) })
                        DropdownMenuItem(text = { Text("HTML") }, onClick = { showShareMenu = false; shareNoteAsHtml(context, mediaScope, currentDraft()) })
                        DropdownMenuItem(text = { Text("XML") }, onClick = { showShareMenu = false; shareNoteAsXml(context, currentDraft()) })
                    }
                }
                TextButton(
                    onClick = {
                        historyJob?.cancel()
                        val current = DraftSnapshot(title, body)
                        if (history.getOrNull(historyIndex) != current) {
                            while (history.lastIndex > historyIndex) history.removeAt(history.lastIndex)
                            history.add(current)
                            historyIndex = history.lastIndex
                        }
                        if (historyIndex > 0) {
                            historyIndex--
                            val snapshot = history[historyIndex]
                            title = snapshot.title
                            body = snapshot.body
                        }
                    },
                    enabled = historyIndex > 0,
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) { Text("↶", fontSize = 23.sp) }
                TextButton(
                    onClick = {
                        if (historyIndex < history.lastIndex) {
                            historyIndex++
                            val snapshot = history[historyIndex]
                            title = snapshot.title
                            body = snapshot.body
                        }
                    },
                    enabled = historyIndex < history.lastIndex,
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) { Text("↷", fontSize = 23.sp) }
            }
            TextButton(
                onClick = { saveDraft() },
                enabled = title.isNotBlank() || bodyHasContent || reminderTimes.isNotEmpty() || photoPaths.isNotEmpty() || attachmentPaths.isNotEmpty() || tableList.isNotEmpty() || aiInstruction.isNotBlank(),
                contentPadding = PaddingValues(horizontal = 7.dp)
            ) { Text("✓", color = MaterialTheme.colorScheme.primary, fontSize = 27.sp) }
        }

        OutlinedTextField(
            value = title,
            onValueChange = { setTitle(it) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = { Text("Название") },
            shape = RoundedCornerShape(16.dp),
            colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f)
            ),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
        )
        Text(
            text = "${category}  •  ${formatDate(note?.updatedAt ?: System.currentTimeMillis())}",
            modifier = Modifier.padding(start = 7.dp, top = 6.dp, bottom = 7.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        androidx.compose.material3.HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f))

        Column(
            modifier = Modifier.fillMaxWidth().weight(1f).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            RichTextField(
                value = body,
                onValueChange = { setBody(it) },
                label = "",
                editorHeight = 360.dp
            )
            tableList.toList().forEachIndexed { tableIndex, table ->
                NoteTableEditor(
                    table = table,
                    index = tableIndex + 1,
                    tableTint = roleColor(settings.tableColor, MaterialTheme.colorScheme.surfaceVariant),
                    onCellChanged = { row, column, value ->
                        val index = tableList.indexOfFirst { it.id == table.id }
                        if (index >= 0) {
                            val old = tableList[index]
                            val changed = old.cells.mapIndexed { rowIndex, cells ->
                                if (rowIndex == row) cells.mapIndexed { colIndex, cell -> if (colIndex == column) value else cell } else cells
                            }
                            tableList[index] = old.copy(cells = changed)
                            if (settings.autoSave) onAutoSave(currentDraft())
                        }
                    },
                    onDelete = { tableList.removeAll { it.id == table.id } }
                )
            }
            if (photoPaths.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(vertical = 7.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    photoPaths.toList().forEach { path ->
                        PhotoAttachment(path = path, onRemove = {
                            val index = photoPaths.indexOf(path)
                            if (index >= 0) {
                                photoPaths.removeAt(index)
                                if (index < photoIds.size) photoIds.removeAt(index)
                            }
                        })
                    }
                }
            }
            if (attachmentPaths.isNotEmpty()) {
                Column(modifier = Modifier.fillMaxWidth().padding(bottom = 5.dp)) {
                    attachmentPaths.toList().forEach { path ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("📎 ${File(path).name}", modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            TextButton(onClick = {
                                val index = attachmentPaths.indexOf(path)
                                if (index >= 0) {
                                    attachmentPaths.removeAt(index)
                                    if (index < attachmentIds.size) attachmentIds.removeAt(index)
                                }
                            }) { Text("Убрать") }
                        }
                    }
                }
            }
            if (message != null) {
                Text(message.orEmpty(), modifier = Modifier.padding(bottom = 4.dp), color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
            }
            Column(Modifier.fillMaxWidth().padding(bottom = 5.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Напоминания", modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    TextButton(onClick = { showReminderPicker = true }) { Text("＋ Добавить") }
                }
                reminderTimes.forEach { time ->
                    Surface(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(Modifier.padding(start = 12.dp, end = 4.dp, top = 1.dp, bottom = 1.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text("◷   ${formatReminderDate(time)}", color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp)
                                Text(reminderTypes[time] ?: "Уведомление и будильник", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                            }
                            TextButton(onClick = {
                                NoteReminderScheduler.cancel(context, draftId, time)
                                reminderTimes.remove(time)
                                reminderTypes.remove(time)
                                if (settings.autoSave) onAutoSave(currentDraft())
                            }) { Text("Убрать", color = MaterialTheme.colorScheme.error, fontSize = 12.sp) }
                        }
                    }
                }
            }
            ChoiceRow(
                label = "Использовать ИИ",
                value = selectedAi,
                options = listOf("Без ИИ", "Алиса", "Gemini", "ChatGPT", "DeepSeek"),
                onSelect = { selectedAi = it }
            )
            if (selectedAi != "Без ИИ") {
                OutlinedTextField(
                    value = aiInstruction,
                    onValueChange = { aiInstruction = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Инструкция для ИИ") },
                    placeholder = { Text("Например: исправь ошибки и сохрани мой стиль") },
                    minLines = 2,
                    maxLines = 4,
                    shape = RoundedCornerShape(13.dp)
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().height(58.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            TextButton(onClick = {
                photoLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            }) { Text("▧  Фото", fontSize = 13.sp) }
            TextButton(onClick = { showInsertMenu = true }) { Text("＋  Вставить", fontSize = 13.sp) }
            TextButton(onClick = { launchSpeech() }) { Text("🎙", fontSize = 19.sp) }
        }
    }

    if (showReminderPicker) {
        ReminderPickerDialog(
            onDismiss = { showReminderPicker = false },
            onConfirm = { timestamp, type ->
                if (reminderTimes.none { kotlin.math.abs(it - timestamp) < 60_000L }) {
                    reminderTimes.add(timestamp)
                    reminderTimes.sort()
                    reminderTypes[timestamp] = type
                    requestReminderPermissions()
                    if (settings.autoSave && (title.isNotBlank() || bodyHasContent || reminderTimes.isNotEmpty() || tableList.isNotEmpty())) onAutoSave(currentDraft())
                }
                showReminderPicker = false
            }
        )
    }


    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text("Сохранить заметку?") },
            text = { Text("Можно сохранить изменения или выйти без сохранения.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showExitDialog = false
                        if (title.isNotBlank() || bodyHasContent || reminderTimes.isNotEmpty() || photoPaths.isNotEmpty() || attachmentPaths.isNotEmpty() || tableList.isNotEmpty() || aiInstruction.isNotBlank()) saveDraft()
                        else { showExitDialog = false; onClose() }
                    },
                    enabled = title.isNotBlank() || bodyHasContent || reminderTimes.isNotEmpty() || photoPaths.isNotEmpty() || attachmentPaths.isNotEmpty() || tableList.isNotEmpty() || aiInstruction.isNotBlank()
                ) { Text("Сохранить") }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = { showExitDialog = false }) { Text("Остаться") }
                    TextButton(onClick = { showExitDialog = false; onClose() }) { Text("Выйти без сохранения") }
                }
            }
        )
    }

    if (showInsertMenu) {
        AlertDialog(
            onDismissRequest = { showInsertMenu = false },
            title = { Text("Добавить в заметку") },
            text = {
                Column {
                    TextButton(onClick = {
                        showInsertMenu = false
                        tableRowCount = 3
                        tableColumnCount = 3
                        showTableSizeDialog = true
                    }) { Text("▦  Таблицу") }
                    TextButton(onClick = { showInsertMenu = false; showLinkDialog = true }) { Text("↗  Гиперссылку") }
                    TextButton(onClick = {
                        showInsertMenu = false
                        documentLauncher.launch(arrayOf("*/*"))
                    }) { Text("📎  Импортировать файл") }
                    TextButton(onClick = { showInsertMenu = false; showAttachNoteDialog = true }) { Text("▤  Вложить заметку") }
                }
            },
            confirmButton = { TextButton(onClick = { showInsertMenu = false }) { Text("Закрыть") } }
        )
    }

    if (showTableSizeDialog) {
        AlertDialog(
            onDismissRequest = { showTableSizeDialog = false },
            title = { Text("Размер таблицы") },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Строк: $tableRowCount", color = Color.White)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = { tableRowCount = (tableRowCount - 1).coerceAtLeast(1) }) { Text("−") }
                        Text("$tableRowCount", modifier = Modifier.padding(horizontal = 18.dp))
                        TextButton(onClick = { tableRowCount = (tableRowCount + 1).coerceAtMost(12) }) { Text("+") }
                    }
                    Spacer(Modifier.height(6.dp))
                    Text("Столбцов: $tableColumnCount", color = Color.White)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = { tableColumnCount = (tableColumnCount - 1).coerceAtLeast(1) }) { Text("−") }
                        Text("$tableColumnCount", modifier = Modifier.padding(horizontal = 18.dp))
                        TextButton(onClick = { tableColumnCount = (tableColumnCount + 1).coerceAtMost(8) }) { Text("+") }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    tableList.add(NoteTable(cells = List(tableRowCount) { List(tableColumnCount) { "" } }))
                    if (settings.autoSave) onAutoSave(currentDraft())
                    showTableSizeDialog = false
                }) { Text("Создать таблицу") }
            },
            dismissButton = { TextButton(onClick = { showTableSizeDialog = false }) { Text("Отмена") } }
        )
    }

    if (showLinkDialog) {
        AlertDialog(
            onDismissRequest = { showLinkDialog = false },
            title = { Text("Вставить ссылку") },
            text = {
                Column {
                    OutlinedTextField(value = linkLabel, onValueChange = { linkLabel = it }, label = { Text("Текст ссылки") }, singleLine = true)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(value = linkAddress, onValueChange = { linkAddress = it }, label = { Text("Адрес сайта") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri))
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val label = linkLabel.ifBlank { linkAddress }
                    insertIntoBody("[$label]($linkAddress)")
                    linkLabel = ""
                    linkAddress = ""
                    showLinkDialog = false
                }, enabled = linkAddress.startsWith("https://") || linkAddress.startsWith("http://")) { Text("Вставить") }
            },
            dismissButton = { TextButton(onClick = { showLinkDialog = false }) { Text("Отмена") } }
        )
    }

    if (showAttachNoteDialog) {
        val otherNotes = allNotes.filter { it.id != note?.id }
        AlertDialog(
            onDismissRequest = { showAttachNoteDialog = false },
            title = { Text("Выберите заметку") },
            text = {
                if (otherNotes.isEmpty()) Text("Пока нет других сохранённых заметок.")
                else Column {
                    otherNotes.take(8).forEach { saved ->
                        TextButton(onClick = {
                            val excerpt = saved.body.take(500)
                            insertIntoBody("Заметка «${saved.title.ifBlank { "Без названия" }}» (${saved.category}):\n$excerpt")
                            showAttachNoteDialog = false
                        }) {
                            Text("${saved.title.ifBlank { "Без названия" }} · ${saved.category}", maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showAttachNoteDialog = false }) { Text("Закрыть") } }
        )
    }
}

@Composable
private fun NoteTableEditor(
    table: NoteTable,
    index: Int,
    tableTint: Color,
    onCellChanged: (Int, Int, String) -> Unit,
    onDelete: () -> Unit
) {
    var editingCell by remember(table.id) { mutableStateOf<TableCellAddress?>(null) }
    var cellDraft by remember(table.id) { mutableStateOf("") }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = mixColors(MaterialTheme.colorScheme.surface, tableTint, 0.30f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(Modifier.padding(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Таблица $index", modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.SemiBold)
                TextButton(onClick = onDelete, contentPadding = PaddingValues(horizontal = 5.dp)) {
                    Text("Удалить таблицу", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                }
            }
            Column(Modifier.horizontalScroll(rememberScrollState())) {
                table.cells.forEachIndexed { rowIndex, cells ->
                    Row(verticalAlignment = Alignment.Top) {
                        cells.forEachIndexed { columnIndex, cell ->
                            Card(
                                modifier = Modifier
                                    .width(220.dp)
                                    .heightIn(min = 72.dp)
                                    .padding(end = 5.dp, bottom = 5.dp)
                                    .clickable {
                                        editingCell = TableCellAddress(rowIndex, columnIndex)
                                        cellDraft = cell
                                    },
                                shape = RoundedCornerShape(9.dp),
                                colors = CardDefaults.cardColors(containerColor = mixColors(tableTint, MaterialTheme.colorScheme.background, 0.18f)),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                            ) {
                                Text(
                                    text = RichTextDocument.annotatedString(
                                        if (RichTextDocument.plainText(cell).isBlank()) "Нажмите, чтобы ввести текст" else cell
                                    ),
                                    modifier = Modifier.fillMaxWidth().padding(10.dp),
                                    color = if (RichTextDocument.plainText(cell).isBlank()) MaterialTheme.colorScheme.onSurfaceVariant else contrastColor(mixColors(tableTint, MaterialTheme.colorScheme.background, 0.18f)),
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }
            }
            Text("Нажмите на ячейку, чтобы изменить её содержимое", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
        }
    }
    editingCell?.let { address ->
        AlertDialog(
            onDismissRequest = { editingCell = null },
            title = { Text("Ячейка ${address.row + 1}, ${address.column + 1}") },
            text = {
                RichTextField(
                    value = cellDraft,
                    onValueChange = { cellDraft = it },
                    label = "Содержимое ячейки",
                    editorHeight = 250.dp
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onCellChanged(address.row, address.column, cellDraft)
                    editingCell = null
                }) { Text("Готово") }
            },
            dismissButton = { TextButton(onClick = { editingCell = null }) { Text("Отмена") } }
        )
    }
}

@Composable
private fun PhotoAttachment(path: String, onRemove: () -> Unit) {
    val bitmapState = produceState<Bitmap?>(initialValue = null, key1 = path) {
        value = withContext(Dispatchers.IO) { decodeThumbnail(path) }
    }
    val decodedBitmap: Bitmap? = bitmapState.value

    Box(modifier = Modifier.size(78.dp)) {
        if (decodedBitmap != null) {
            Image(
                bitmap = decodedBitmap.asImageBitmap(),
                contentDescription = "Фото во вложении",
                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(10.dp))
            )
        } else {
            Surface(
                modifier = Modifier.fillMaxSize(),
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFF26342B)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("Фото", color = Color.White, fontSize = 12.sp)
                }
            }
        }
        IconButton(
            onClick = onRemove,
            modifier = Modifier.align(Alignment.TopEnd).size(28.dp)
                .background(Color(0xCC101612), CircleShape)
        ) {
            Text("×", color = Color.White, fontSize = 17.sp)
        }
    }
}

private fun decodeThumbnail(path: String): Bitmap? = try {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(path, bounds)
    var sample = 1
    while (bounds.outWidth / sample > 320 || bounds.outHeight / sample > 320) sample *= 2
    BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = sample })
} catch (_: Exception) {
    null
}

private fun copyUriToPrivateFile(context: Context, uri: Uri, folderName: String): File {
    val folder = File(context.filesDir, folderName)
    check(folder.exists() || folder.mkdirs()) { "Не удалось создать папку для вложения" }
    val mime = context.contentResolver.getType(uri)
    val extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mime)?.let { ".$it" } ?: ".bin"
    val destination = File(folder, "${UUID.randomUUID()}$extension")
    val input = context.contentResolver.openInputStream(uri)
        ?: throw java.io.IOException("Хранилище не предоставило доступ к выбранному файлу")
    input.use { source -> FileOutputStream(destination).use { output -> source.copyTo(output) } }
    return destination
}

private fun formatDate(timestamp: Long): String =
    SimpleDateFormat("d MMM yyyy, HH:mm", Locale("ru")).format(Date(timestamp))

private fun escapeHtml(value: String): String = value
    .replace("&", "&amp;")
    .replace("<", "&lt;")
    .replace(">", "&gt;")
    .replace("\"", "&quot;")
    .replace("'", "&#39;")

private fun appendDataUri(writer: BufferedWriter, file: File, mimeType: String) {
    writer.write("data:$mimeType;base64,")
    val bridge = object : OutputStream() {
        override fun write(value: Int) { writer.write(value) }
        override fun write(buffer: ByteArray, offset: Int, length: Int) {
            writer.write(String(buffer, offset, length, Charsets.US_ASCII))
        }
        override fun close() { flush() }
        override fun flush() { writer.flush() }
    }
    Base64OutputStream(bridge, Base64.NO_WRAP).use { encoded ->
        file.inputStream().buffered().use { input -> input.copyTo(encoded) }
    }
}

private fun shareNoteAsPlainText(context: Context, note: Note) {
    val payload = buildString {
        if (note.title.isNotBlank()) append(note.title.trim()).append("\n\n")
        if (note.body.isNotBlank()) append(RichTextDocument.plainText(note.body).trim()).append('\n')
        note.tables.forEachIndexed { index, table ->
            append("\nТаблица ${index + 1}:\n")
            table.cells.forEach { row -> append(row.joinToString("\t") { RichTextDocument.plainText(it) }).append('\n') }
        }
        note.photoPaths.filter { File(it).isFile }.forEach { append("\nФото: ").append(File(it).name) }
        note.attachmentPaths.filter { File(it).isFile }.forEach { append("\nВложение: ").append(File(it).name) }
    }.trim()
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, payload)
        putExtra(Intent.EXTRA_SUBJECT, note.title.ifBlank { "Заметка" })
    }
    context.startActivity(Intent.createChooser(send, "Поделиться заметкой"))
}

private fun shareNoteAsTextFile(context: Context, note: Note) {
    try {
        val folder = File(context.cacheDir, "shared_notes").apply { mkdirs() }
        val safeTitle = note.title.ifBlank { "Заметка" }.replace(Regex("[\\/:*?\"<>|]"), "_").take(80)
        val file = File(folder, "$safeTitle-${System.currentTimeMillis()}.txt")
        val payload = buildString {
            if (note.title.isNotBlank()) append(note.title.trim()).append("\n\n")
            if (note.body.isNotBlank()) append(RichTextDocument.plainText(note.body).trim()).append('\n')
            note.tables.forEachIndexed { index, table ->
                append("\nТаблица ${index + 1}:\n")
                table.cells.forEach { row -> append(row.joinToString("\t") { RichTextDocument.plainText(it) }).append('\n') }
            }
            note.photoPaths.filter { File(it).isFile }.forEach { append("\nФото: ").append(File(it).name) }
            note.attachmentPaths.filter { File(it).isFile }.forEach { append("\nВложение: ").append(File(it).name) }
        }.trim()
        file.writeText(payload, Charsets.UTF_8)
        shareNoteFile(context, file, note.title, "text/plain", "Поделиться TXT заметки")
    } catch (_: Exception) {
        Toast.makeText(context, "Не удалось подготовить TXT-файл заметки.", Toast.LENGTH_LONG).show()
    }
}

private fun shareNoteAsXml(context: Context, note: Note) {
    try {
        val folder = File(context.cacheDir, "shared_notes").apply { mkdirs() }
        val safeTitle = note.title.ifBlank { "Заметка" }.replace(Regex("[\\/:*?\"<>|]"), "_").take(80)
        val file = File(folder, "$safeTitle-${System.currentTimeMillis()}.xml")
        fun xml(value: String): String = value.replace("&", "&amp;").replace("<", "&lt;")
            .replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&apos;")
        val payload = buildString {
            append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<note>\n")
            append("  <title>").append(xml(note.title)).append("</title>\n")
            append("  <body>").append(xml(RichTextDocument.plainText(note.body))).append("</body>\n")
            append("  <tables>\n")
            note.tables.forEach { table ->
                append("    <table>\n")
                table.cells.forEach { row ->
                    append("      <row>")
                    row.forEach { cell -> append("<cell>").append(xml(RichTextDocument.plainText(cell))).append("</cell>") }
                    append("</row>\n")
                }
                append("    </table>\n")
            }
            append("  </tables>\n  <photos>\n")
            note.photoPaths.filter { File(it).isFile }.forEach { append("    <file name=\"").append(xml(File(it).name)).append("\"/>\n") }
            append("  </photos>\n  <attachments>\n")
            note.attachmentPaths.filter { File(it).isFile }.forEach { append("    <file name=\"").append(xml(File(it).name)).append("\"/>\n") }
            append("  </attachments>\n</note>")
        }
        file.writeText(payload, Charsets.UTF_8)
        shareNoteFile(context, file, note.title, "application/xml", "Поделиться XML заметки")
    } catch (_: Exception) {
        Toast.makeText(context, "Не удалось подготовить XML-файл заметки.", Toast.LENGTH_LONG).show()
    }
}

private fun shareNoteAsMarkdown(context: Context, note: Note) {
    try {
        val folder = File(context.cacheDir, "shared_notes").apply { mkdirs() }
        val safeTitle = note.title.ifBlank { "Заметка" }.replace(Regex("[\\\\/:*?\"<>|]"), "_").take(80)
        val file = File(folder, "$safeTitle-${System.currentTimeMillis()}.md")
        val payload = buildString {
            if (note.title.isNotBlank()) append("# ${note.title.trim()}\n\n")
            if (note.body.isNotBlank()) append(RichTextDocument.plainText(note.body).trim()).append("\n\n")
            note.tables.forEachIndexed { index, table -> append("## Таблица ${index + 1}\n\n").append(table.toMarkdownTable()).append("\n\n") }
            note.photoPaths.filter { File(it).isFile }.forEach { append("Фото: ${File(it).name}\n") }
            note.attachmentPaths.filter { File(it).isFile }.forEach { append("Вложение: ${File(it).name}\n") }
        }.trim()
        file.writeText(payload, Charsets.UTF_8)
        shareNoteFile(context, file, note.title, "text/markdown", "Поделиться заметкой Markdown")
    } catch (_: Exception) {
        Toast.makeText(context, "Не удалось подготовить Markdown-файл заметки.", Toast.LENGTH_LONG).show()
    }
}

private fun shareNoteAsHtml(context: Context, scope: kotlinx.coroutines.CoroutineScope, note: Note) {
    scope.launch {
        try {
            val htmlFile = withContext(Dispatchers.IO) { createShareableNoteHtml(context, note) }
            shareNoteHtmlFile(context, htmlFile, note.title)
        } catch (_: Exception) {
            Toast.makeText(context, "Не удалось подготовить HTML-файл заметки.", Toast.LENGTH_LONG).show()
        }
    }
}

private fun shareNoteAsPdf(context: Context, scope: kotlinx.coroutines.CoroutineScope, note: Note) {
    scope.launch {
        try {
            val pdfFile = withContext(Dispatchers.IO) { createShareableNotePdf(context, note) }
            shareNotePdfFile(context, pdfFile, note.title)
        } catch (_: Exception) {
            Toast.makeText(context, "Не удалось подготовить PDF заметки.", Toast.LENGTH_LONG).show()
        }
    }
}

private fun createShareableNotePdf(context: Context, note: Note): File {
    val folder = File(context.cacheDir, "shared_notes").apply { mkdirs() }
    val output = File(folder, "Заметка-${System.currentTimeMillis()}.pdf")
    val document = android.graphics.pdf.PdfDocument()
    val pageWidth = 595
    val pageHeight = 842
    val margin = 44f
    val bodyPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.rgb(34, 42, 36)
        textSize = 12f
    }
    val titlePaint = android.graphics.Paint(bodyPaint).apply { textSize = 20f; typeface = android.graphics.Typeface.DEFAULT_BOLD }
    val headingPaint = android.graphics.Paint(bodyPaint).apply { textSize = 14f; typeface = android.graphics.Typeface.DEFAULT_BOLD }
    val linePaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply { color = android.graphics.Color.rgb(180, 190, 181); strokeWidth = 0.7f }
    val imagePaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG or android.graphics.Paint.FILTER_BITMAP_FLAG)
    var pageNumber = 0
    var page: android.graphics.pdf.PdfDocument.Page? = null
    var canvas: android.graphics.Canvas? = null
    var y = margin
    fun newPage() {
        page?.let(document::finishPage)
        pageNumber++
        page = document.startPage(android.graphics.pdf.PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
        canvas = page!!.canvas
        y = margin
    }
    fun ensureRoom(height: Float) {
        if (page == null || y + height > pageHeight - margin) newPage()
    }
    fun drawWrapped(text: String, paint: android.graphics.Paint, indent: Float = 0f, gap: Float = 6f) {
        val maxWidth = pageWidth - margin * 2 - indent
        text.split('\n').forEach { paragraph ->
            if (paragraph.isEmpty()) { y += paint.textSize * 0.55f; return@forEach }
            val words = paragraph.split(Regex("\\s+"))
            var line = StringBuilder()
            words.forEach { word ->
                val candidate = if (line.isEmpty()) word else "$line $word"
                if (paint.measureText(candidate) > maxWidth && line.isNotEmpty()) {
                    ensureRoom(paint.textSize * 1.5f)
                    canvas!!.drawText(line.toString(), margin + indent, y + paint.textSize, paint)
                    y += paint.textSize * 1.5f
                    line = StringBuilder(word)
                } else line = StringBuilder(candidate)
            }
            ensureRoom(paint.textSize * 1.5f)
            canvas!!.drawText(line.toString(), margin + indent, y + paint.textSize, paint)
            y += paint.textSize * 1.5f
            y += gap
        }
    }
    fun drawTable(table: NoteTable) {
        val columns = table.cells.maxOfOrNull { it.size } ?: return
        if (columns == 0) return
        val width = pageWidth - margin * 2
        val colWidth = width / columns
        table.cells.forEachIndexed { rowIndex, row ->
            val prepared = List(columns) { col -> row.getOrNull(col).orEmpty() }
            val wrapped = prepared.map { cell ->
                val words = RichTextDocument.plainText(cell).replace('\n',' ').split(Regex("\\s+"))
                val maxText = colWidth - 14f
                val result = mutableListOf<String>()
                var line = StringBuilder()
                words.forEach { word ->
                    val candidate = if (line.isEmpty()) word else "$line $word"
                    if (bodyPaint.measureText(candidate) > maxText && line.isNotEmpty()) {
                        result += line.toString(); line = StringBuilder(word)
                    } else line = StringBuilder(candidate)
                }
                if (line.isNotEmpty()) result += line.toString()
                result.ifEmpty { listOf("") }
            }
            val rowHeight = maxOf(32f, (wrapped.maxOfOrNull { it.size } ?: 1) * 16f + 16f)
            ensureRoom(rowHeight + 2f)
            val top = y
            val fill = if (rowIndex == 0) android.graphics.Color.rgb(231, 239, 232) else android.graphics.Color.WHITE
            val bg = android.graphics.Paint().apply { color = fill; style = android.graphics.Paint.Style.FILL }
            canvas!!.drawRect(margin, top, pageWidth - margin, top + rowHeight, bg)
            for (col in 0..columns) canvas!!.drawLine(margin + col * colWidth, top, margin + col * colWidth, top + rowHeight, linePaint)
            canvas!!.drawLine(margin, top, pageWidth - margin, top, linePaint)
            canvas!!.drawLine(margin, top + rowHeight, pageWidth - margin, top + rowHeight, linePaint)
            wrapped.forEachIndexed { col, cellLines ->
                val paint = if (rowIndex == 0) headingPaint else bodyPaint
                cellLines.forEachIndexed { lineIndex, text ->
                    canvas!!.drawText(text, margin + col * colWidth + 7f, top + 15f + lineIndex * 16f, paint)
                }
            }
            y += rowHeight
        }
        y += 16f
    }
    try {
        if (note.title.isNotBlank()) drawWrapped(note.title, titlePaint, gap = 12f)
        if (RichTextDocument.plainText(note.body).isNotBlank()) drawWrapped(RichTextDocument.plainText(note.body), bodyPaint)
        note.tables.forEachIndexed { index, table ->
            drawWrapped("Таблица ${index + 1}", headingPaint, gap = 5f)
            drawTable(table)
        }
        note.photoPaths.forEach { path ->
            val bitmap = runCatching { BitmapFactory.decodeFile(path) }.getOrNull() ?: return@forEach
            val ratio = minOf((pageWidth - margin * 2) / bitmap.width.toFloat(), 240f / bitmap.height.toFloat(), 1f)
            val h = bitmap.height * ratio
            ensureRoom(h + 18f)
            val dest = android.graphics.RectF(margin, y, margin + bitmap.width * ratio, y + h)
            canvas!!.drawBitmap(bitmap, null, dest, imagePaint)
            y += h + 18f
            bitmap.recycle()
        }
        if (note.attachmentPaths.isNotEmpty()) {
            drawWrapped("Вложения", headingPaint)
            note.attachmentPaths.filter { File(it).isFile }.forEach { drawWrapped("• ${File(it).name}", bodyPaint, 8f) }
        }
        if (page == null) newPage()
        page?.let(document::finishPage)
        FileOutputStream(output).use { document.writeTo(it) }
    } finally {
        document.close()
    }
    return output
}

private fun shareNotePdfFile(context: Context, file: File, title: String) {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "application/pdf"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_SUBJECT, title.ifBlank { "Заметка" })
        clipData = ClipData.newUri(context.contentResolver, title.ifBlank { "Заметка" }, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        if (context !is Activity) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(Intent.createChooser(send, "Поделиться PDF заметки"))
}

private fun createShareableNoteHtml(context: Context, note: Note): File {
    val folder = File(context.cacheDir, "shared_notes").apply { mkdirs() }
    val output = File(folder, "Заметка-${System.currentTimeMillis()}.html")
    val mimeMap = MimeTypeMap.getSingleton()
    BufferedWriter(OutputStreamWriter(FileOutputStream(output), Charsets.UTF_8)).use { writer ->
        writer.write("""<!doctype html><html lang="ru"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>${escapeHtml(note.title.ifBlank { "Заметка" })}</title><style>body{font:16px/1.6 system-ui,sans-serif;max-width:900px;margin:32px auto;padding:0 18px;color:#202720}h1{line-height:1.25}p{white-space:pre-wrap}table{border-collapse:collapse;width:100%;margin:18px 0}th,td{border:1px solid #aeb8af;padding:9px;text-align:left;vertical-align:top;white-space:pre-wrap}th{background:#e9f0ea}img{display:block;max-width:100%;height:auto;margin:14px 0;border-radius:8px}.attachment{display:block;margin:8px 0;overflow-wrap:anywhere}</style></head><body>""")
        if (note.title.isNotBlank()) writer.write("<h1>${escapeHtml(note.title)}</h1>")
        if (RichTextDocument.plainText(note.body).isNotBlank()) writer.write("<p>${escapeHtml(RichTextDocument.plainText(note.body))}</p>")
        note.tables.forEachIndexed { tableIndex, table ->
            if (table.cells.isNotEmpty()) {
                writer.write("<h2>Таблица ${tableIndex + 1}</h2><table><tbody>")
                table.cells.forEachIndexed { rowIndex, row ->
                    writer.write("<tr>")
                    row.forEach { cell ->
                        val tag = if (rowIndex == 0) "th" else "td"
                        writer.write("<$tag>${escapeHtml(RichTextDocument.plainText(cell))}</$tag>")
                    }
                    writer.write("</tr>")
                }
                writer.write("</tbody></table>")
            }
        }
        note.photoPaths.forEach { path ->
            val file = File(path)
            if (file.isFile) {
                val extension = file.extension.lowercase(Locale.ROOT)
                val mime = mimeMap.getMimeTypeFromExtension(extension) ?: "image/jpeg"
                writer.write("<img alt=\"Фото заметки\" src=\"")
                appendDataUri(writer, file, mime)
                writer.write("\">")
            }
        }
        note.attachmentPaths.forEach { path ->
            val file = File(path)
            if (file.isFile) {
                val extension = file.extension.lowercase(Locale.ROOT)
                val mime = mimeMap.getMimeTypeFromExtension(extension) ?: "application/octet-stream"
                writer.write("<a class=\"attachment\" download=\"${escapeHtml(file.name)}\" href=\"")
                appendDataUri(writer, file, mime)
                writer.write("\">${escapeHtml(file.name)}</a>")
            }
        }
        writer.write("</body></html>")
    }
    return output
}

private fun shareNoteHtmlFile(context: Context, file: File, title: String) =
    shareNoteFile(context, file, title, "text/html", "Поделиться заметкой HTML")


private fun shareNoteFile(context: Context, file: File, title: String, mimeType: String, chooserTitle: String) {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val send = Intent(Intent.ACTION_SEND).apply {
        type = mimeType
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_SUBJECT, title.ifBlank { "Заметка" })
        clipData = ClipData.newUri(context.contentResolver, title.ifBlank { "Заметка" }, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        if (context !is Activity) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(Intent.createChooser(send, chooserTitle))
}

private fun readNotes(context: Context, storageKey: String = "notes"): List<Note> {
    val raw = context.getSharedPreferences("mom_notes", Context.MODE_PRIVATE)
        .getString(storageKey, "[]") ?: "[]"
    return try {
        val array = JSONArray(raw)
        (0 until array.length()).mapNotNull { index ->
            val item = array.optJSONObject(index) ?: return@mapNotNull null
            Note(
                id = item.optString("id"),
                category = normalizeSectionName(item.optString("category")),
                title = item.optString("title"),
                body = item.optString("body"),
                updatedAt = item.optLong("updatedAt"),
                createdAt = item.optLong("createdAt", item.optLong("updatedAt")),
                isFavorite = item.optBoolean("isFavorite", false),
                isDeleted = item.optBoolean("isDeleted", false),
                photoPaths = item.optJSONArray("photoPaths").toStringList(),
                attachmentPaths = item.optJSONArray("attachmentPaths").toStringList(),
                photoIds = item.optJSONArray("photoIds").toStringList(),
                attachmentIds = item.optJSONArray("attachmentIds").toStringList(),
                tables = item.optJSONArray("tables").toNoteTables(),
                aiAssistant = item.optString("aiAssistant", "Без ИИ"),
                aiInstruction = item.optString("aiInstruction", ""),
                reminderAt = item.optLong("reminderAt", 0L),
                reminders = item.optJSONArray("reminders").toLongList(),
                reminderTypes = item.optJSONObject("reminderTypes").toReminderTypes()
            )
        }
    } catch (_: Exception) {
        emptyList()
    }
}

private fun JSONArray?.toStringList(): List<String> {
    if (this == null) return emptyList()
    return (0 until length()).mapNotNull { optString(it).takeIf(String::isNotBlank) }
}

private fun JSONArray?.toLongList(): List<Long> {
    if (this == null) return emptyList()
    return (0 until length()).map { optLong(it) }.filter { it > 0L }.distinct().sorted()
}

private fun JSONObject?.toReminderTypes(): Map<Long, String> {
    if (this == null) return emptyMap()
    return keys().asSequence().mapNotNull { key ->
        val time = key.toLongOrNull() ?: return@mapNotNull null
        val type = optString(key, "Уведомление и будильник")
        if (time > 0L && type in setOf("Уведомление", "Будильник", "Уведомление и будильник")) time to type else null
    }.toMap()
}

private fun notesToJson(notes: List<Note>): JSONArray {
    return JSONArray().apply {
        notes.forEach { note ->
            val photos = JSONArray().apply { note.photoPaths.forEach(::put) }
            val files = JSONArray().apply { note.attachmentPaths.forEach(::put) }
            val tables = JSONArray().apply {
                note.tables.forEach { table ->
                    val rows = JSONArray().apply {
                        table.cells.forEach { cells -> put(JSONArray().apply { cells.forEach(::put) }) }
                    }
                    put(JSONObject().put("id", table.id).put("cells", rows))
                }
            }
            put(JSONObject()
                .put("id", note.id)
                .put("category", note.category)
                .put("title", note.title)
                .put("body", note.body)
                .put("updatedAt", note.updatedAt)
                .put("createdAt", note.createdAt)
                .put("isFavorite", note.isFavorite)
                .put("isDeleted", note.isDeleted)
                .put("photoPaths", photos)
                .put("attachmentPaths", files)
                .put("photoIds", JSONArray(note.photoIds))
                .put("attachmentIds", JSONArray(note.attachmentIds))
                .put("tables", tables)
                .put("aiAssistant", note.aiAssistant)
                .put("aiInstruction", note.aiInstruction)
                .put("reminderAt", note.reminderAt)
                .put("reminders", JSONArray(note.reminders))
                .put("reminderTypes", JSONObject().apply { note.reminderTypes.forEach { (time, type) -> put(time.toString(), type) } }))
        }
    }
}

private val noteStorageExecutor = Executors.newSingleThreadExecutor()

private fun writeNotes(context: Context, notes: List<Note>, storageKey: String = "notes") {
    val snapshot = notes.toList()
    noteStorageExecutor.execute {
        val serialized = notesToJson(snapshot).toString()
        context.getSharedPreferences("mom_notes", Context.MODE_PRIVATE).edit()
            .putString(storageKey, serialized).apply()
    }
}

private fun readNotesForAccount(context: Context, accountId: String): List<Note> {
    val prefs = context.getSharedPreferences("mom_notes", Context.MODE_PRIVATE)
    val key = accountNotesKey(accountId)
    if (prefs.contains(key)) return readNotes(context, key)
    if (accountId == "guest") return readNotes(context)
    return if (prefs.getString("cloud_migration_owner", null) == accountId) readNotes(context) else emptyList()
}

internal fun accountNotesKey(accountId: String) = "notes_user_$accountId"

private fun persistImported(context: Context, notes: List<Note>, categories: List<String>, notesKey: String = "notes") {
    writeNotes(context, notes, notesKey)
    writeCategories(context, categories)
}

private fun JSONArray?.toNotes(): List<Note> {
    if (this == null) return emptyList()
    return (0 until length()).mapNotNull { index ->
        val item = optJSONObject(index) ?: return@mapNotNull null
        val updatedAt = item.optLong("updatedAt")
        Note(
            id = item.optString("id"),
            category = normalizeSectionName(item.optString("category")),
            title = item.optString("title"),
            body = item.optString("body"),
            updatedAt = updatedAt,
            createdAt = item.optLong("createdAt", updatedAt),
            isFavorite = item.optBoolean("isFavorite", false),
            isDeleted = item.optBoolean("isDeleted", false),
            photoPaths = item.optJSONArray("photoPaths").toStringList(),
            attachmentPaths = item.optJSONArray("attachmentPaths").toStringList(),
            photoIds = item.optJSONArray("photoIds").toStringList(),
            attachmentIds = item.optJSONArray("attachmentIds").toStringList(),
            tables = item.optJSONArray("tables").toNoteTables(),
            aiAssistant = item.optString("aiAssistant", "Без ИИ"),
            aiInstruction = item.optString("aiInstruction", ""),
            reminderAt = item.optLong("reminderAt", 0L),
            reminders = item.optJSONArray("reminders").toLongList(),
            reminderTypes = item.optJSONObject("reminderTypes").toReminderTypes()
        )
    }
}

private fun JSONArray?.toNoteTables(): List<NoteTable> {
    if (this == null) return emptyList()
    return (0 until length()).mapNotNull { index ->
        val table = optJSONObject(index) ?: return@mapNotNull null
        val rows = table.optJSONArray("cells") ?: return@mapNotNull null
        val cells = (0 until rows.length()).map { rowIndex ->
            val row = rows.optJSONArray(rowIndex) ?: JSONArray()
            (0 until row.length()).map { colIndex -> row.optString(colIndex) }
        }
        NoteTable(id = table.optString("id", UUID.randomUUID().toString()), cells = cells)
    }
}

private fun readCategories(context: Context): List<String> {
    val raw = context.getSharedPreferences("mom_notes", Context.MODE_PRIVATE).getString("categories", null)
        ?: return categories
    val values = try {
        val array = JSONArray(raw)
        (0 until array.length()).mapNotNull { array.optString(it).takeIf(String::isNotBlank) }
    } catch (_: Exception) {
        emptyList()
    }
    return (categories + values.map(::normalizeSectionName))
        .distinctBy { it.lowercase(Locale.ROOT) }
}

private fun writeCategories(context: Context, categories: List<String>) {
    val array = JSONArray().apply { categories.forEach(::put) }
    context.getSharedPreferences("mom_notes", Context.MODE_PRIVATE).edit().putString("categories", array.toString()).apply()
}


private fun escapeMarkdownTableCell(value: String): String =
    value
        .replace("\\", "\\\\")
        .replace("|", "\\|")
        .replace("\r\n", "\n")
        .replace('\r', '\n')
        .replace("\n", "<br>")

private fun NoteTable.toMarkdownTable(): String {
    val columnCount = cells.maxOfOrNull { it.size } ?: return ""
    if (columnCount == 0) return ""

    // Рваные строки дополняются пустыми ячейками, порядок исходных строк и столбцов сохранён.
    val rows = cells.map { row ->
        List(columnCount) { column -> escapeMarkdownTableCell(row.getOrNull(column).orEmpty()) }
    }
    val header = rows.firstOrNull() ?: return ""
    val markdownRows = buildList {
        add(header)
        add(List(columnCount) { "---" })
        addAll(rows.drop(1))
    }
    return markdownRows.joinToString("\n") { row -> "| " + row.joinToString(" | ") + " |" }
}

private fun shareNoteWithAssistant(
    context: Context,
    title: String,
    body: String,
    tables: List<NoteTable>,
    assistant: String,
    instruction: String,
    transferMode: String
) {
    if (assistant == "Без ИИ") return
    val tableText = tables.mapIndexed { index, table ->
        "Таблица ${index + 1}:\n${table.toMarkdownTable()}"
    }.filter { it.substringAfter('\n').isNotBlank() }.joinToString("\n\n")
    val prompt = buildString {
        append("Этот текст предоставлен из приложения Удобные заметки напрямую тебе.\n\n")
        if (instruction.isNotBlank()) append("Правки для ИИ: ${instruction.trim()}\n\n")
        if (title.isNotBlank()) append("Заголовок заметки: $title\n")
        if (body.isNotBlank()) append("Текст заметки: $body\n")
        if (tableText.isNotBlank()) {
            if (isNotEmpty()) append('\n')
            append("Таблицы заметки:\n")
            append(tableText)
        }
    }
    if (transferMode == "Копировать запрос") {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("Запрос для $assistant", prompt))
        Toast.makeText(context, "Запрос для $assistant скопирован", Toast.LENGTH_SHORT).show()
        return
    }
    val sendIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, prompt)
        putExtra(Intent.EXTRA_SUBJECT, title.ifBlank { "Заметка" })
    }
    val aiPackage = when (assistant) {
        "Алиса" -> "com.yandex.aliceapp"
        "ChatGPT" -> "com.openai.chatgpt"
        "Gemini" -> "com.google.android.apps.bard"
        "DeepSeek" -> "com.deepseek.chat"
        else -> null
    }
    if (aiPackage != null) sendIntent.setPackage(aiPackage)
    try {
        context.startActivity(sendIntent)
    } catch (_: android.content.ActivityNotFoundException) {
        sendIntent.setPackage(null)
        context.startActivity(Intent.createChooser(sendIntent, "Отправить запрос в $assistant"))
    }
}
