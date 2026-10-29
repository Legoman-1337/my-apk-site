package ru.momnotes.app

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
internal fun WelcomeSetupFlow(
    settings: AppSettings,
    onSettingsChange: (AppSettings) -> Unit,
    onFinish: () -> Unit
) {
    var page by remember { mutableIntStateOf(0) }
    var showTitle by remember { mutableStateOf(false) }
    BackHandler(enabled = true) {
        if (page > 0) page--
    }

    LaunchedEffect(Unit) {
        delay(350)
        showTitle = true
        delay(900)
        page = 1
    }

    val title by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (page == 0) 1f else 0.86f,
        animationSpec = tween(650, easing = FastOutSlowInEasing), label = "welcome-title"
    )

    Column(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        AnimatedContent(
            targetState = page,
            transitionSpec = {
                (fadeIn(tween(420)) + slideInVertically(tween(500)) { it / 8 }) togetherWith
                    (fadeOut(tween(220)) + slideOutVertically(tween(300)) { -it / 10 })
            }, label = "welcome-pages"
        ) { current ->
            if (current == 0) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CheckmarkMark(Modifier.size(94.dp))
                    AnimatedVisibility(
                        visible = showTitle,
                        enter = fadeIn(tween(450)) + slideInVertically(tween(500)) { it / 3 }
                    ) {
                        Text(
                            "Отлично!",
                            modifier = Modifier.padding(top = 22.dp).scale(title),
                            color = MaterialTheme.colorScheme.onBackground,
                            fontSize = 29.sp
                        )
                    }
                    Text("Подготовьте заметки для себя", modifier = Modifier.padding(top = 10.dp), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 17.sp)
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                    Text("Подготовьте заметки для себя", color = MaterialTheme.colorScheme.onBackground, fontSize = 25.sp)
                    Text("Выберите базовые настройки. Каждый шаг можно пропустить, они доступны позже в меню.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                    when (current) {
                        1 -> {
                            Text("Поведение", color = MaterialTheme.colorScheme.primary, fontSize = 17.sp)
                            ChoiceRow("Сортировка заметок", settings.sortBy, listOf("По дате изменения", "По дате создания", "По названию")) {
                                onSettingsChange(settings.copy(sortBy = it))
                            }
                            ChoiceRow("Избранное", settings.favoriteBehavior, listOf("Поднимать наверх", "Только избранное", "Обычный порядок")) {
                                onSettingsChange(settings.copy(favoriteBehavior = it))
                            }
                            ChoiceRow("Показывать дату изменения", if (settings.showModifiedDate) "Да" else "Нет", listOf("Да", "Нет")) {
                                onSettingsChange(settings.copy(showModifiedDate = it == "Да"))
                            }
                            SettingSwitch("Автосохранение при вводе", settings.autoSave) {
                                onSettingsChange(settings.copy(autoSave = it))
                            }
                            ChoiceRow("Подтверждать удаление", if (settings.confirmDelete) "Да" else "Нет", listOf("Да", "Нет")) {
                                onSettingsChange(settings.copy(confirmDelete = it == "Да"))
                            }
                        }
                        2 -> {
                            Text("Настройки ИИ", color = MaterialTheme.colorScheme.primary, fontSize = 17.sp)
                            Text("Выберите, что делать при отправке заметки выбранному помощнику.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                            ChoiceRow("Действие по кнопке ИИ", settings.aiTransfer, listOf("Открыть приложение ИИ", "Копировать запрос")) {
                                onSettingsChange(settings.copy(aiTransfer = it))
                            }
                        }
                        else -> Text("Готово", color = MaterialTheme.colorScheme.primary, fontSize = 20.sp)
                    }
                }
            }
        }
        if (page > 0) {
            Spacer(Modifier.height(24.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = { if (page < 3) page++ else onFinish() }) {
                    Text(if (page < 3) "Пропустить пункт" else "Пропустить настройку")
                }
                Button(onClick = { if (page < 3) page++ else onFinish() }) {
                    Text(if (page < 3) "Далее" else "Готово")
                }
            }
            TextButton(onClick = onFinish, modifier = Modifier.align(Alignment.End)) { Text("Пропустить всё") }
        }
    }
}

@Composable
private fun CheckmarkMark(modifier: Modifier = Modifier) {
    val scale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(720, easing = FastOutSlowInEasing),
        label = "success-mark-scale"
    )
    val markColor = MaterialTheme.colorScheme.primary
    androidx.compose.foundation.Canvas(modifier.scale(scale)) {
        val stroke = Stroke(width = size.minDimension * 0.075f)
        drawCircle(color = markColor, style = stroke)
        drawLine(
            color = markColor,
            start = Offset(size.width * 0.27f, size.height * 0.52f),
            end = Offset(size.width * 0.44f, size.height * 0.68f),
            strokeWidth = stroke.width,
            cap = androidx.compose.ui.graphics.StrokeCap.Round
        )
        drawLine(
            color = markColor,
            start = Offset(size.width * 0.44f, size.height * 0.68f),
            end = Offset(size.width * 0.74f, size.height * 0.34f),
            strokeWidth = stroke.width,
            cap = androidx.compose.ui.graphics.StrokeCap.Round
        )
    }
}
