package com.example.bookapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.NavigateBefore
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.bookapp.data.SectionEntity
import kotlinx.coroutines.delay

@Immutable
data class RehearsalTurnItem(
    val section: SectionEntity,
    val roleTitle: String,
    val isOwnRole: Boolean,
    val orderLabel: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MultiRoleRehearsalScreen(
    taziehTitle: String,
    ownRoleId: Long,
    ownRoleTitle: String,
    turns: List<RehearsalTurnItem>,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember(context) { context.getSharedPreferences("rehearsal_progress", android.content.Context.MODE_PRIVATE) }
    var currentIndex by remember(ownRoleId) { mutableIntStateOf(prefs.getInt("multi_last_index_$ownRoleId", 0).coerceIn(0, (turns.size - 1).coerceAtLeast(0))) }
    var learnedIds by remember(ownRoleId) { mutableStateOf(prefs.getStringSet("learned_sections_$ownRoleId", emptySet()).orEmpty().toSet()) }
    var autoPlay by remember { mutableStateOf(false) }
    var speed by remember { mutableIntStateOf(3) }
    val current = turns.getOrNull(currentIndex)
    val lines = remember(current?.section?.id) {
        current?.section?.content?.lines()?.filter { it.isNotBlank() } ?: emptyList()
    }
    var revealedLines by remember(currentIndex, current?.section?.id) {
        val start = current?.section?.let { prefs.getInt("start_line_${it.id}", 1) } ?: 1
        mutableIntStateOf(if (current?.isOwnRole == true) (start - 1).coerceIn(0, lines.size) else lines.size)
    }
    val scroll = rememberScrollState()
    var didRestoreTurns by remember(ownRoleId) { mutableStateOf(false) }
    LaunchedEffect(turns.size, ownRoleId) {
        if (turns.isNotEmpty() && !didRestoreTurns) {
            currentIndex = prefs.getInt("multi_last_index_$ownRoleId", 0).coerceIn(0, turns.lastIndex)
            didRestoreTurns = true
        }
    }
    LaunchedEffect(currentIndex, ownRoleId, turns.size, didRestoreTurns) {
        if (turns.isNotEmpty() && didRestoreTurns) prefs.edit().putInt("multi_last_index_$ownRoleId", currentIndex).apply()
    }

    LaunchedEffect(autoPlay, currentIndex, speed, lines.size, revealedLines) {
        if (!autoPlay || current == null) return@LaunchedEffect
        if (current.isOwnRole && revealedLines < lines.size) {
            delay(speed * 1000L)
            revealedLines++
        } else {
            delay(speed * 1000L)
            if (currentIndex < turns.lastIndex) currentIndex++ else autoPlay = false
        }
    }
    LaunchedEffect(currentIndex, revealedLines) {
        scroll.animateScrollTo(scroll.maxValue)
    }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text("تمرین چندنقشی") },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "بازگشت") } }
        )
    }) { padding ->
        if (current == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { Text("بخشی برای تمرین انتخاب نشده است") }
        } else {
            Column(Modifier.fillMaxSize().padding(padding).padding(14.dp)) {
                Text(taziehTitle, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("${currentIndex + 1} از ${turns.size} نوبت تمرین", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(selected = autoPlay, onClick = { autoPlay = !autoPlay }, label = { Text(if (autoPlay) "توقف خودخوان" else "شروع خودخوان") })
                    listOf(2, 3, 5).forEach { s -> FilterChip(selected = speed == s, onClick = { speed = s }, label = { Text("${s}ث") }) }
                }
                Spacer(Modifier.height(8.dp))
                Card(Modifier.fillMaxWidth().weight(1f), shape = RoundedCornerShape(16.dp)) {
                    Column(Modifier.fillMaxSize().verticalScroll(scroll).padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(current.roleTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                            if (current.isOwnRole) Text("نقش من", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall)
                        }
                        Text(current.section.title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (current.orderLabel.isNotBlank()) Text(current.orderLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (current.isOwnRole) {
                            val learnedKey = "learned_sections_$ownRoleId"
                            TextButton(onClick = {
                                val updated = learnedIds.toMutableSet()
                                if (current.section.id.toString() in updated) updated.remove(current.section.id.toString())
                                else updated.add(current.section.id.toString())
                                learnedIds = updated
                                prefs.edit().putStringSet(learnedKey, updated).apply()
                            }) { Text(if (current.section.id.toString() in learnedIds) "✓ این بخش تمرین شده" else "علامت‌گذاری به‌عنوان تمرین‌شده") }
                        }
                        Spacer(Modifier.height(10.dp))
                        if (current.isOwnRole) {
                            if (revealedLines == 0) Text("متن نقش شما پنهان است؛ ابتدا از حفظ اجرا کنید و سپس خط را آشکار کنید.")
                            lines.take(revealedLines).forEach { Text(it, style = MaterialTheme.typography.bodyLarge); Spacer(Modifier.height(5.dp)) }
                        } else {
                            Text("متن این نقش برای دنبال‌کردن مکالمه نمایش داده می‌شود.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            lines.forEach { Text(it, style = MaterialTheme.typography.bodyLarge); Spacer(Modifier.height(5.dp)) }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                if (current.isOwnRole && lines.isNotEmpty()) {
                    LinearProgressIndicator(progress = { revealedLines.toFloat() / lines.size }, modifier = Modifier.fillMaxWidth())
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = {
                            revealedLines = 0
                            prefs.edit().putInt("start_line_${current.section.id}", 1).apply()
                        }, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Filled.VisibilityOff, contentDescription = null); Spacer(Modifier.width(4.dp)); Text("از ابتدا")
                        }
                        Button(onClick = { if (revealedLines < lines.size) revealedLines++ }, enabled = revealedLines < lines.size, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Filled.Visibility, contentDescription = null); Spacer(Modifier.width(4.dp)); Text("خط بعد")
                        }
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    TextButton(onClick = { if (currentIndex > 0) currentIndex-- }, enabled = currentIndex > 0) {
                        Icon(Icons.AutoMirrored.Filled.NavigateBefore, contentDescription = null); Text("قبلی")
                    }
                    TextButton(onClick = { if (currentIndex < turns.lastIndex) currentIndex++ }, enabled = currentIndex < turns.lastIndex) {
                        Text("بعدی"); Icon(Icons.AutoMirrored.Filled.NavigateNext, contentDescription = null)
                    }
                }
            }
        }
    }
}
