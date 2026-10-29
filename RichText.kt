package ru.momnotes.app

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.text.Editable
import android.text.InputType
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.TextPaint
import android.text.TextWatcher
import android.text.style.CharacterStyle
import android.text.style.UpdateAppearance
import android.util.TypedValue
import android.view.Gravity
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.widget.AppCompatEditText
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import org.json.JSONArray
import org.json.JSONObject

internal data class RichRun(
    val start: Int,
    val end: Int,
    val color: String? = null,
    val size: Float? = null,
    val font: String? = null,
    val bold: Boolean = false,
    val italic: Boolean = false,
    val underline: String? = null,
    val strike: Boolean = false
)

internal object RichTextDocument {
    private fun parse(value: String): Pair<String, List<RichRun>>? {
        if (!value.startsWith("{\"text\":")) return null
        return try {
            val json = JSONObject(value)
            val text = json.optString("text", "")
            val sourceRuns = json.optJSONArray("runs") ?: JSONArray()
            val runs = (0 until sourceRuns.length()).mapNotNull { index ->
                val item = sourceRuns.optJSONObject(index) ?: return@mapNotNull null
                val start = item.optInt("start", -1)
                val end = item.optInt("end", -1)
                if (start < 0 || end <= start || end > text.length) return@mapNotNull null
                RichRun(
                    start = start,
                    end = end,
                    color = item.optString("color").takeIf { it.matches(Regex("#[0-9A-Fa-f]{6}")) },
                    size = item.optDouble("size", Double.NaN).takeIf { it.isFinite() }?.toFloat(),
                    font = item.optString("font").takeIf(String::isNotBlank),
                    bold = item.optBoolean("bold"),
                    italic = item.optBoolean("italic"),
                    underline = item.optString("underline").takeIf { it in setOf("single", "double", "wavy") },
                    strike = item.optBoolean("strike")
                )
            }
            text to runs
        } catch (_: Exception) {
            null
        }
    }

    fun plainText(value: String): String = parse(value)?.first ?: value

    fun canonical(value: String): String {
        val (text, runs) = decode(value)
        return if (runs.isEmpty()) text else encode(text, runs)
    }

    fun appendText(value: String, addition: String): String {
        val (text, runs) = decode(value)
        return if (runs.isEmpty()) text + addition else encode(text + addition, runs)
    }

    fun encode(text: String, runs: List<RichRun> = emptyList()): String = JSONObject().apply {
        put("text", text)
        put("runs", JSONArray().apply {
            runs.filter { it.start >= 0 && it.end <= text.length && it.end > it.start }
                .sortedWith(compareBy<RichRun> { it.start }.thenBy { it.end }.thenBy { it.color ?: it.font ?: it.underline ?: "" })
                .forEach { run ->
                    put(JSONObject().apply {
                        put("start", run.start)
                        put("end", run.end)
                        run.color?.let { put("color", it) }
                        run.size?.let { put("size", it.toDouble()) }
                        run.font?.let { put("font", it) }
                        if (run.bold) put("bold", true)
                        if (run.italic) put("italic", true)
                        run.underline?.let { put("underline", it) }
                        if (run.strike) put("strike", true)
                    })
                }
        })
    }.toString()

    fun annotatedString(value: String): AnnotatedString {
        val (text, runs) = parse(value) ?: (value to emptyList())
        return buildAnnotatedString {
            append(text)
            runs.forEach { run ->
                val decorations = buildList {
                    if (run.underline == "single") add(TextDecoration.Underline)
                    if (run.strike) add(TextDecoration.LineThrough)
                }
                addStyle(
                    SpanStyle(
                        color = run.color?.let(::colorFromHex) ?: Color.Unspecified,
                        fontSize = run.size?.sp ?: androidx.compose.ui.unit.TextUnit.Unspecified,
                        fontFamily = run.font?.let(::fontFamily),
                        fontWeight = if (run.bold) FontWeight.Bold else null,
                        fontStyle = if (run.italic) FontStyle.Italic else null,
                        textDecoration = if (decorations.isEmpty()) null else TextDecoration.combine(decorations)
                    ),
                    run.start.coerceIn(0, text.length),
                    run.end.coerceIn(0, text.length)
                )
            }
        }
    }

    internal fun decode(value: String): Pair<String, List<RichRun>> = parse(value) ?: (value to emptyList())

    private fun colorFromHex(value: String): Color = try {
        Color(android.graphics.Color.parseColor(value))
    } catch (_: Exception) {
        Color.Unspecified
    }

    private fun fontFamily(value: String): FontFamily = when {
        value.contains("serif") && !value.startsWith("sans") -> FontFamily.Serif
        value.contains("mono") -> FontFamily.Monospace
        value.contains("cursive") -> FontFamily.Cursive
        else -> FontFamily.SansSerif
    }
}

private class RichColorSpan(val value: String) : CharacterStyle(), UpdateAppearance {
    override fun updateDrawState(tp: TextPaint) { runCatching { tp.color = android.graphics.Color.parseColor(value) } }
}
private class RichSizeSpan(val value: Float, private val scaledDensity: Float) : CharacterStyle(), UpdateAppearance {
    override fun updateDrawState(tp: TextPaint) { tp.textSize = value * scaledDensity }
}
private class RichFontSpan(val value: String) : CharacterStyle(), UpdateAppearance {
    override fun updateDrawState(tp: TextPaint) {
        val current = tp.typeface?.style ?: Typeface.NORMAL
        tp.typeface = Typeface.create(value, current)
    }
}
private class RichBoldSpan : CharacterStyle(), UpdateAppearance {
    override fun updateDrawState(tp: TextPaint) {
        tp.isFakeBoldText = true
    }
}
private class RichItalicSpan : CharacterStyle(), UpdateAppearance {
    override fun updateDrawState(tp: TextPaint) {
        tp.textSkewX = -0.25f
    }
}
private class RichUnderlineSpan(val value: String) : CharacterStyle(), UpdateAppearance {
    override fun updateDrawState(tp: TextPaint) { tp.isUnderlineText = value == "single" }
}
private class RichStrikeSpan : CharacterStyle(), UpdateAppearance {
    override fun updateDrawState(tp: TextPaint) { tp.isStrikeThruText = true }
}

private class RichEditText(context: Context) : AppCompatEditText(context) {
    private var suppressCallback = false
    private val pendingFormats = linkedMapOf<String, String>()
    private var lastSelectionStart = 0
    private var lastSelectionEnd = 0
    private var changedStart = 0
    private var insertedLength = 0
    var onRichTextChanged: ((String) -> Unit)? = null
    var onSelectionChanged: (() -> Unit)? = null

    init {
        gravity = Gravity.TOP or Gravity.START
        isFocusable = true
        isFocusableInTouchMode = true
        isCursorVisible = true
        showSoftInputOnFocus = true
        inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
        setPadding((12 * resources.displayMetrics.density).toInt(), (11 * resources.displayMetrics.density).toInt(), (12 * resources.displayMetrics.density).toInt(), (11 * resources.displayMetrics.density).toInt())
        background = null
        setOnClickListener {
            requestFocus()
            post {
                (context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager)
                    ?.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)
            }
        }
        addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                changedStart = start
                insertedLength = after
            }
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) {
                if (suppressCallback) return
                applyPendingFormats(s, changedStart, insertedLength)
                onRichTextChanged?.invoke(currentRichText())
            }
        })
    }

    override fun onSelectionChanged(selStart: Int, selEnd: Int) {
        super.onSelectionChanged(selStart, selEnd)
        if (selStart < 0 || selEnd < 0) return
        if (selStart != lastSelectionStart || selEnd != lastSelectionEnd) {
            lastSelectionStart = selStart
            lastSelectionEnd = selEnd
            onSelectionChanged?.invoke()
        }
    }

    private fun applyPendingFormats(editable: Editable?, start: Int, inserted: Int) {
        if (editable == null || pendingFormats.isEmpty() || inserted <= 0) return
        val safeStart = start.coerceIn(0, editable.length)
        val end = (safeStart + inserted).coerceIn(safeStart, editable.length)
        if (end <= safeStart) return
        pendingFormats.toMap().forEach { (attribute, value) ->
            createFormatSpan(attribute, value)?.let {
                editable.setSpan(it, safeStart, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        }
    }

    private fun createFormatSpan(attribute: String, value: String): CharacterStyle? = when (attribute) {
        "color" -> RichColorSpan(value)
        "size" -> value.toFloatOrNull()?.let { RichSizeSpan(it, resources.displayMetrics.scaledDensity) }
        "font" -> RichFontSpan(value)
        "bold" -> RichBoldSpan()
        "italic" -> RichItalicSpan()
        "underline" -> value.takeUnless { it == "none" }?.let(::RichUnderlineSpan)
        "strike" -> RichStrikeSpan()
        else -> null
    }

    fun clearFormatting(): String {
        val editable = text as? Spannable ?: return currentRichText()
        val start = minOf(selectionStart.coerceAtLeast(0), selectionEnd.coerceAtLeast(0))
        val end = maxOf(selectionStart.coerceAtLeast(0), selectionEnd.coerceAtLeast(0))
        if (start == end) {
            pendingFormats.clear()
            invalidate()
            return currentRichText()
        }
        editable.getSpans(start, end, CharacterStyle::class.java).forEach { span ->
            if (editable.getSpanStart(span) < end && editable.getSpanEnd(span) > start) {
                editable.removeSpan(span)
            }
        }
        invalidate()
        return currentRichText()
    }

    fun currentRichText(): String {
        val editable = text as? Spannable ?: return RichTextDocument.encode(text.toString())
        val runs = mutableListOf<RichRun>()
        fun range(span: CharacterStyle, make: (Int, Int) -> RichRun) {
            val start = editable.getSpanStart(span)
            val end = editable.getSpanEnd(span)
            if (start >= 0 && end > start) runs += make(start, end)
        }
        editable.getSpans(0, editable.length, RichColorSpan::class.java).forEach { span -> range(span) { a, b -> RichRun(a, b, color = span.value) } }
        editable.getSpans(0, editable.length, RichSizeSpan::class.java).forEach { span -> range(span) { a, b -> RichRun(a, b, size = span.value) } }
        editable.getSpans(0, editable.length, RichFontSpan::class.java).forEach { span -> range(span) { a, b -> RichRun(a, b, font = span.value) } }
        editable.getSpans(0, editable.length, RichBoldSpan::class.java).forEach { span -> range(span) { a, b -> RichRun(a, b, bold = true) } }
        editable.getSpans(0, editable.length, RichItalicSpan::class.java).forEach { span -> range(span) { a, b -> RichRun(a, b, italic = true) } }
        editable.getSpans(0, editable.length, RichUnderlineSpan::class.java).forEach { span -> range(span) { a, b -> RichRun(a, b, underline = span.value) } }
        editable.getSpans(0, editable.length, RichStrikeSpan::class.java).forEach { span -> range(span) { a, b -> RichRun(a, b, strike = true) } }
        return if (runs.isEmpty()) editable.toString() else RichTextDocument.encode(editable.toString(), runs)
    }

    fun loadRichText(encoded: String) {
        val (plain, runs) = RichTextDocument.decode(encoded)
        suppressCallback = true
        setText(plain)
        val editable = text as? SpannableStringBuilder
        if (editable != null) runs.forEach { run ->
            val spans = buildList<CharacterStyle> {
                run.color?.let { add(RichColorSpan(it)) }
                run.size?.let { add(RichSizeSpan(it, resources.displayMetrics.scaledDensity)) }
                run.font?.let { add(RichFontSpan(it)) }
                if (run.bold) add(RichBoldSpan())
                if (run.italic) add(RichItalicSpan())
                run.underline?.let { add(RichUnderlineSpan(it)) }
                if (run.strike) add(RichStrikeSpan())
            }
            spans.forEach { editable.setSpan(it, run.start, run.end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE) }
        }
        setSelection(editable?.length ?: plain.length)
        suppressCallback = false
        invalidate()
    }

    fun applyFormat(attribute: String, value: String = "toggle"): String {
        val editable = text as? Spannable ?: return currentRichText()
        val selectionStart = selectionStart.coerceIn(0, editable.length)
        val selectionEnd = selectionEnd.coerceIn(0, editable.length)
        if (selectionStart == selectionEnd) {
            val previous = pendingFormats[attribute]
            val turnOff = (attribute in setOf("bold", "italic", "strike", "underline") || value == "toggle") && previous != null
            if (value == "none") pendingFormats.remove(attribute)
            else if (turnOff) pendingFormats.remove(attribute)
            else pendingFormats[attribute] = value
            invalidate()
            return currentRichText()
        }

        val start = minOf(selectionStart, selectionEnd)
        val end = maxOf(selectionStart, selectionEnd)
        val kind: Class<out CharacterStyle> = when (attribute) {
            "color" -> RichColorSpan::class.java
            "size" -> RichSizeSpan::class.java
            "font" -> RichFontSpan::class.java
            "bold" -> RichBoldSpan::class.java
            "italic" -> RichItalicSpan::class.java
            "underline" -> RichUnderlineSpan::class.java
            else -> RichStrikeSpan::class.java
        }
        val existing = editable.getSpans(start, end, kind).toList()
        val isToggle = attribute in setOf("bold", "italic", "strike", "underline") || value == "toggle"
        val turnOff = isToggle && existing.any { editable.getSpanStart(it) < end && editable.getSpanEnd(it) > start }
        existing.forEach { old ->
            val oldStart = editable.getSpanStart(old)
            val oldEnd = editable.getSpanEnd(old)
            editable.removeSpan(old)
            if (oldStart < start) cloneSpan(old).forEach { editable.setSpan(it, oldStart, minOf(start, oldEnd), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE) }
            if (oldEnd > end) cloneSpan(old).forEach { editable.setSpan(it, maxOf(end, oldStart), oldEnd, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE) }
        }
        if (!turnOff) {
            createFormatSpan(attribute, value)?.let {
                editable.setSpan(it, start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
        }
        setSelection(selectionStart, selectionEnd)
        invalidate()
        return currentRichText()
    }


    private fun cloneSpan(span: CharacterStyle): List<CharacterStyle> = when (span) {
        is RichColorSpan -> listOf(RichColorSpan(span.value))
        is RichSizeSpan -> listOf(RichSizeSpan(span.value, resources.displayMetrics.scaledDensity))
        is RichFontSpan -> listOf(RichFontSpan(span.value))
        is RichBoldSpan -> listOf(RichBoldSpan())
        is RichItalicSpan -> listOf(RichItalicSpan())
        is RichUnderlineSpan -> listOf(RichUnderlineSpan(span.value))
        is RichStrikeSpan -> listOf(RichStrikeSpan())
        else -> emptyList()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val layout = layout ?: return
        val editable = text as? Spannable ?: return
        val custom = editable.getSpans(0, editable.length, RichUnderlineSpan::class.java).filter { it.value == "double" || it.value == "wavy" }
        if (custom.isEmpty()) return
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = currentTextColor
            style = Paint.Style.STROKE
            strokeWidth = resources.displayMetrics.density
        }
        val density = resources.displayMetrics.density
        canvas.save()
        canvas.translate((compoundPaddingLeft - scrollX).toFloat(), (compoundPaddingTop - scrollY).toFloat())
        custom.forEach { span ->
            val start = editable.getSpanStart(span).coerceAtLeast(0)
            val end = editable.getSpanEnd(span).coerceAtMost(editable.length)
            if (end <= start) return@forEach
            val firstLine = layout.getLineForOffset(start)
            val lastLine = layout.getLineForOffset((end - 1).coerceAtLeast(start))
            for (line in firstLine..lastLine) {
                val from = maxOf(start, layout.getLineStart(line))
                val to = minOf(end, layout.getLineEnd(line))
                if (to <= from) continue
                val x1 = layout.getPrimaryHorizontal(from)
                val x2 = layout.getPrimaryHorizontal(to)
                val y = layout.getLineBaseline(line) + 3.5f * density
                if (span.value == "double") {
                    canvas.drawLine(x1, y + 2.4f * density, x2, y + 2.4f * density, paint)
                } else {
                    val path = android.graphics.Path().apply {
                        moveTo(x1, y)
                        var x = x1
                        var up = true
                        while (x < x2) {
                            val next = (x + 3.5f * density).coerceAtMost(x2)
                            lineTo(next, y + if (up) -1.3f * density else 1.3f * density)
                            x = next
                            up = !up
                        }
                    }
                    canvas.drawPath(path, paint)
                }
            }
        }
        canvas.restore()
    }
}

@Composable
internal fun RichTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    editorHeight: Dp = 190.dp
) {
    val latestChange by rememberUpdatedState(onValueChange)
    val editorRef = remember { mutableStateOf<RichEditText?>(null) }
    val editorTextColor = MaterialTheme.colorScheme.onSurface.toArgb()
    val editorHintColor = MaterialTheme.colorScheme.onSurfaceVariant.toArgb()
    var chooser by remember { mutableStateOf<String?>(null) }
    var customSize by remember { mutableStateOf("16") }
    var selectionCollapsed by remember { mutableStateOf(true) }
    val colors = listOf(
        "#202124", "#FFFFFF", "#E53935", "#C62828", "#F57C00", "#FFB300", "#FDD835",
        "#43A047", "#00897B", "#00ACC1", "#1E88E5", "#3949AB", "#5E35B1", "#8E24AA", "#D81B60"
    )
    val sizeOptions = listOf("12", "14", "16", "18", "22", "26")

    fun apply(attribute: String, style: String = "toggle") {
        val editor = editorRef.value ?: return
        latestChange(editor.applyFormat(attribute, style))
    }

    Column {
        Text(
            label,
            modifier = Modifier.padding(start = 4.dp, bottom = 6.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = { chooser = "size" },
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 5.dp)
                    ) { Text("Размер текста ▾") }
                    TextButton(
                        onClick = { chooser = "color" },
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 5.dp)
                    ) { Text("Цвет текста ▾") }
                    TextButton(
                        onClick = { editorRef.value?.clearFormatting()?.let(latestChange) },
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 5.dp)
                    ) { Text("Сбросить оформление") }
                }
                androidx.compose.material3.HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f))
                AndroidView(
                    modifier = Modifier.fillMaxWidth().height(editorHeight),
                    factory = { context ->
                        RichEditText(context).apply {
                            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
                            setTextColor(editorTextColor)
                            setHintTextColor(editorHintColor)
                            hint = "Начните писать заметку…"
                            loadRichText(value)
                            isFocusable = true
                            isFocusableInTouchMode = true
                            onRichTextChanged = { latestChange(it) }
                            onSelectionChanged = { selectionCollapsed = selectionStart == selectionEnd }
                            editorRef.value = this
                        }
                    },
                    update = { editor ->
                        editor.setTextColor(editorTextColor)
                        editor.setHintTextColor(editorHintColor)
                        val currentPlain = editor.text?.toString().orEmpty()
                        val nextPlain = RichTextDocument.plainText(value)
                        if (currentPlain != nextPlain) {
                            val selectionStart = editor.selectionStart.coerceAtLeast(0)
                            val selectionEnd = editor.selectionEnd.coerceAtLeast(0)
                            editor.loadRichText(value)
                            editor.setSelection(selectionStart.coerceAtMost(editor.length()), selectionEnd.coerceAtMost(editor.length()))
                        } else if (editor.currentRichText() != value &&
                            RichTextDocument.canonical(editor.currentRichText()) != RichTextDocument.canonical(value)) {
                            // Synchronize formatting updates without replacing the user's active text buffer.
                            val selectionStart = editor.selectionStart.coerceAtLeast(0)
                            val selectionEnd = editor.selectionEnd.coerceAtLeast(0)
                            editor.loadRichText(value)
                            editor.setSelection(selectionStart.coerceAtMost(editor.length()), selectionEnd.coerceAtMost(editor.length()))
                        }
                        editor.onRichTextChanged = { latestChange(it) }
                        editor.onSelectionChanged = { selectionCollapsed = editor.selectionStart == editor.selectionEnd }
                        editorRef.value = editor
                    }
                )
                androidx.compose.material3.HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f))
            }
        }
    }

    chooser?.let { mode ->
        val title = when (mode) {
            "size" -> "Размер текста"
            else -> "Цвет текста"
        }
        AlertDialog(
            onDismissRequest = { chooser = null },
            title = { Text(title) },
            text = {
                when (mode) {
                    "color" -> Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        colors.forEach { hex ->
                            Surface(
                                modifier = Modifier.padding(2.dp),
                                shape = RoundedCornerShape(50),
                                color = Color(android.graphics.Color.parseColor(hex)),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                onClick = { apply("color", hex); chooser = null }
                            ) {
                                Text("A", modifier = Modifier.padding(horizontal = 9.dp, vertical = 7.dp), color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    else -> Column {
                        sizeOptions.forEach { size ->
                            TextButton(
                                onClick = { apply("size", size); chooser = null },
                                modifier = Modifier.fillMaxWidth()
                            ) { Text("$size sp", fontSize = size.toInt().sp) }
                        }
                        androidx.compose.material3.OutlinedTextField(
                            value = customSize,
                            onValueChange = { customSize = it.filter(Char::isDigit).take(2) },
                            label = { Text("Другой размер, 8–48 sp") },
                            singleLine = true
                        )
                    }
                }
            },
            confirmButton = {
                Row {
                    if (mode == "size") {
                        TextButton(onClick = {
                            val size = customSize.toIntOrNull()?.coerceIn(8, 48)
                            if (size != null) apply("size", size.toString())
                            chooser = null
                        }) { Text("Применить") }
                    }
                    TextButton(onClick = { chooser = null }) { Text("Закрыть") }
                }
            }
        )
    }
}
