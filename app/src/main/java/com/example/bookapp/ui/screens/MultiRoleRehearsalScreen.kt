package com.example.bookapp.ui.screens

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.bookapp.data.SectionEntity
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect

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
    val prefs = remember(context) {
        context.getSharedPreferences("rehearsal_progress", Context.MODE_PRIVATE)
    }

    val ownSectionIds = remember(turns) {
        turns.filter { it.isOwnRole }.map { it.section.id }.distinct()
    }
    val ownSectionKey = ownSectionIds.joinToString(",")
    val turnSequenceKey = turns.joinToString(",") { "${it.section.roleId}-${it.section.id}" }
    val scrollPrefsKey = "multi_${ownRoleId}_${turnSequenceKey.hashCode()}"
    val ownLinesBySection = remember(turns) {
        turns.filter { it.isOwnRole }.associate { turn ->
            turn.section.id to turn.section.content.lines().filter { it.isNotBlank() }
        }
    }
    val initialRevealCounts = remember(ownSectionKey, ownLinesBySection) {
        ownSectionIds.associateWith { sectionId ->
            val lineCount = ownLinesBySection[sectionId]?.size ?: 0
            (prefs.getInt("start_line_$sectionId", 1) - 1).coerceIn(0, lineCount)
        }
    }
    val revealedBySection = remember(ownRoleId, ownSectionKey) {
        mutableStateMapOf<Long, Int>().apply { putAll(initialRevealCounts) }
    }

    var learnedIds by remember(ownRoleId) {
        mutableStateOf(
            prefs.getStringSet("learned_sections_$ownRoleId", emptySet()).orEmpty().toSet()
        )
    }
    var autoPlay by remember(ownRoleId, ownSectionKey) { mutableStateOf(false) }
    var speed by remember { mutableIntStateOf(3) }
    var focusSectionId by remember { mutableStateOf<Long?>(null) }
    var restoredScrollPosition by remember(scrollPrefsKey) { mutableStateOf(false) }

    val ownTotalLines = ownSectionIds.sumOf { ownLinesBySection[it]?.size ?: 0 }
    val revealedTotal = ownSectionIds.sumOf { sectionId ->
        (revealedBySection[sectionId] ?: 0).coerceIn(0, ownLinesBySection[sectionId]?.size ?: 0)
    }
    val listState = rememberLazyListState()

    LaunchedEffect(turns.size, scrollPrefsKey) {
        if (turns.isNotEmpty() && !restoredScrollPosition) {
            val savedIndex = prefs.getInt("multi_scroll_index_$scrollPrefsKey", 0)
                .coerceIn(0, turns.size)
            val savedOffset = prefs.getInt("multi_scroll_offset_$scrollPrefsKey", 0).coerceAtLeast(0)
            listState.scrollToItem(savedIndex, savedOffset)
            restoredScrollPosition = true
        }
    }

    LaunchedEffect(listState, scrollPrefsKey, restoredScrollPosition) {
        if (!restoredScrollPosition) return@LaunchedEffect
        snapshotFlow { listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset }
            .collect { (index, offset) ->
                prefs.edit()
                    .putInt("multi_scroll_index_$scrollPrefsKey", index)
                    .putInt("multi_scroll_offset_$scrollPrefsKey", offset)
                    .apply()
            }
    }

    fun nextHiddenOwnSectionId(): Long? {
        return turns.firstOrNull { turn ->
            if (!turn.isOwnRole) return@firstOrNull false
            val count = ownLinesBySection[turn.section.id]?.size ?: 0
            (revealedBySection[turn.section.id] ?: 0) < count
        }?.section?.id
    }

    fun revealNextOwnLine() {
        val sectionId = nextHiddenOwnSectionId() ?: run {
            autoPlay = false
            return
        }
        val maxLines = ownLinesBySection[sectionId]?.size ?: 0
        val current = revealedBySection[sectionId] ?: 0
        if (current < maxLines) {
            revealedBySection[sectionId] = current + 1
            focusSectionId = sectionId
        }
    }

    LaunchedEffect(autoPlay, speed, revealedTotal, ownSectionKey, turns) {
        if (!autoPlay) return@LaunchedEffect
        if (nextHiddenOwnSectionId() == null) {
            autoPlay = false
            return@LaunchedEffect
        }
        delay(speed * 1000L)
        revealNextOwnLine()
    }

    LaunchedEffect(focusSectionId, revealedTotal) {
        val sectionId = focusSectionId ?: return@LaunchedEffect
        val index = turns.indexOfFirst { it.isOwnRole && it.section.id == sectionId }
        if (index >= 0) listState.animateScrollToItem(index + 1)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("تمرین چندنقشی", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "بازگشت")
                    }
                }
            )
        },
        bottomBar = {
            Surface(shadowElevation = 8.dp) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    if (ownTotalLines > 0) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("آشکارشده: $revealedTotal از $ownTotalLines خط", style = MaterialTheme.typography.labelSmall)
                            Text("${(revealedTotal * 100 / ownTotalLines)}٪", style = MaterialTheme.typography.labelSmall)
                        }
                        LinearProgressIndicator(
                            progress = { revealedTotal.toFloat() / ownTotalLines.toFloat() },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FilterChip(
                            selected = autoPlay,
                            enabled = ownTotalLines > revealedTotal,
                            onClick = { autoPlay = !autoPlay },
                            label = { Text(if (autoPlay) "توقف" else "خودخوان") }
                        )
                        listOf(2, 3, 5).forEach { seconds ->
                            FilterChip(
                                selected = speed == seconds,
                                onClick = { speed = seconds },
                                label = { Text("${seconds}ث") }
                            )
                        }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        OutlinedButton(
                            onClick = {
                                autoPlay = false
                                revealedBySection.clear()
                                revealedBySection.putAll(initialRevealCounts)
                                focusSectionId = ownSectionIds.firstOrNull()
                            },
                            enabled = revealedTotal > initialRevealCounts.values.sum(),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 5.dp, vertical = 7.dp)
                        ) {
                            Icon(Icons.Filled.VisibilityOff, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text("پنهان‌سازی")
                        }
                        Button(
                            onClick = { autoPlay = false; revealNextOwnLine() },
                            enabled = revealedTotal < ownTotalLines,
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 5.dp, vertical = 7.dp)
                        ) {
                            Icon(Icons.Filled.Visibility, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text("خط بعد")
                        }
                        OutlinedButton(
                            onClick = {
                                autoPlay = false
                                ownSectionIds.forEach { sectionId ->
                                    revealedBySection[sectionId] = ownLinesBySection[sectionId]?.size ?: 0
                                }
                                focusSectionId = ownSectionIds.lastOrNull()
                            },
                            enabled = revealedTotal < ownTotalLines,
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 5.dp, vertical = 7.dp)
                        ) { Text("نمایش کامل") }
                    }
                }
            }
        }
    ) { padding ->
        if (turns.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("برای این تمرین، نوبت یا بخشی انتخاب نشده است.")
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(taziehTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("نقش اصلی تمرین: $ownRoleTitle", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                        turns.firstOrNull()?.orderLabel?.takeIf { it.isNotBlank() }?.let {
                            Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(
                            "متن نقش‌های همراه کامل نمایش داده می‌شود؛ متن نقش اصلی به‌ترتیب خط‌ها آشکار می‌شود.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                itemsIndexed(turns, key = { index, turn -> "${turn.section.id}-$index" }) { _, turn ->
                    val section = turn.section
                    val lines = remember(section.id, section.content) {
                        section.content.lines().filter { it.isNotBlank() }
                    }
                    val shownCount = if (turn.isOwnRole) {
                        (revealedBySection[section.id] ?: initialRevealCounts[section.id] ?: 0).coerceIn(0, lines.size)
                    } else lines.size
                    val learned = section.id.toString() in learnedIds

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(15.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (turn.isOwnRole) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(Modifier.fillMaxWidth().padding(13.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(turn.roleTitle, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                    Text(section.title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                if (turn.isOwnRole) {
                                    AssistChip(onClick = {}, label = { Text("نقش اصلی") }, enabled = false)
                                }
                            }
                            if (turn.isOwnRole) {
                                TextButton(
                                    onClick = {
                                        val key = section.id.toString()
                                        val updated = if (key in learnedIds) learnedIds - key else learnedIds + key
                                        learnedIds = updated
                                        prefs.edit().putStringSet("learned_sections_$ownRoleId", updated).apply()
                                    },
                                    contentPadding = PaddingValues(horizontal = 2.dp, vertical = 0.dp)
                                ) {
                                    Text(if (learned) "✓ این بخش تمرین شده" else "علامت‌گذاری به‌عنوان تمرین‌شده")
                                }
                            }
                            if (turn.isOwnRole && shownCount == 0 && lines.isNotEmpty()) {
                                Text("متن این بخش پنهان است؛ با «خط بعد» آن را مرحله‌به‌مرحله آشکار کنید.", style = MaterialTheme.typography.bodySmall)
                            }
                            lines.take(shownCount).forEach { line ->
                                Text(line, style = MaterialTheme.typography.bodyLarge)
                            }
                            if (turn.isOwnRole && shownCount < lines.size) {
                                Text(
                                    "${lines.size - shownCount} خط پنهان",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
