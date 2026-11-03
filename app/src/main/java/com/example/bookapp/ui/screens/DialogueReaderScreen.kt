package com.example.bookapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

 data class DialogueTurnDisplay(
    val turnId: Long,
    val sectionId: Long,
    val roleTitle: String,
    val sectionTitle: String,
    val content: String,
    val roleId: Long = 0L
)

private enum class DialogueViewMode { GROUP, PERSONAL }

/**
 * نمایش گفت‌وگوی چندنقشی در دو حالت:
 * GROUP: همه نوبت‌ها به ترتیب تاریخی/ثبت‌شده نمایش داده می‌شوند.
 * PERSONAL: فقط نوبت‌های نقش انتخابی برجسته‌اند و گفتار بلافاصله قبل از هر نوبت
 * به‌عنوان «راهنمای پاسخ» نشان داده می‌شود؛ متن اصلی هیچ‌گاه تغییر نمی‌کند.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DialogueReaderScreen(
    dialogueTitle: String,
    turns: List<DialogueTurnDisplay>,
    onMoveTurn: (index: Int, direction: Int) -> Unit,
    onDeleteTurn: (DialogueTurnDisplay) -> Unit,
    onAddTurn: () -> Unit = {},
    onExportPdf: () -> Unit = {},
    onBack: () -> Unit,
    readOnly: Boolean = false,
    showPdf: Boolean = true
) {
    var mode by remember { mutableStateOf(DialogueViewMode.GROUP) }
    var selectedRole by remember(turns) { mutableStateOf(turns.firstOrNull()?.roleId ?: 0L) }
    var activeIndex by remember(turns) { mutableIntStateOf(0) }
    val roleOptions = remember(turns) { turns.distinctBy { if (it.roleId != 0L) "id:${it.roleId}" else it.roleTitle }.map { it.roleId to it.roleTitle } }
    val personalTurns = remember(turns, selectedRole) {
        turns.mapIndexedNotNull { index, turn ->
            val isMine = if (selectedRole != 0L) turn.roleId == selectedRole else turn.roleTitle == roleOptions.firstOrNull { it.first == selectedRole }?.second
            if (!isMine) null else index to turn
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(dialogueTitle) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "بازگشت")
                    }
                },
                actions = {
                    if (!readOnly) {
                        IconButton(onClick = onAddTurn) { Icon(Icons.Filled.Add, contentDescription = "افزودن نوبت") }
                    }
                    if (showPdf) {
                        IconButton(onClick = onExportPdf) { Icon(Icons.Filled.Share, contentDescription = "خروجی PDF گفتگو") }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("حالت نمایش گفتگو", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = mode == DialogueViewMode.GROUP, onClick = { mode = DialogueViewMode.GROUP }, label = { Text("نمایش همه نقش‌ها") })
                FilterChip(selected = mode == DialogueViewMode.PERSONAL, onClick = { mode = DialogueViewMode.PERSONAL }, label = { Text("تمرین نقش من") })
            }
            if (mode == DialogueViewMode.PERSONAL) {
                Text("نقش شبیه‌خوان:", style = MaterialTheme.typography.labelLarge)
                roleOptions.forEach { (roleId, roleTitle) ->
                    FilterChip(
                        selected = selectedRole == roleId,
                        onClick = { selectedRole = roleId },
                        label = { Text(roleTitle) }
                    )
                }
                Text("در این حالت، گفتار نقش مقابل پیش از نوبت شما به‌عنوان راهنمای پاسخ نمایش داده می‌شود.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                Text("ترتیب نوبت‌ها مطابق توالی ثبت‌شده در گفتگو است.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            HorizontalDivider()

            if (mode == DialogueViewMode.GROUP) {
                turns.forEachIndexed { index, turn ->
                    TurnCard(turn = turn, index = index, active = activeIndex == index,
                        canEdit = !readOnly, itemCount = turns.size, onMove = onMoveTurn, onDelete = onDeleteTurn,
                        label = "${index + 1}. ${turn.roleTitle}")
                }
            } else {
                if (personalTurns.isEmpty()) {
                    Text("برای این نقش نوبتی در گفتگوی انتخاب‌شده ثبت نشده است.", color = MaterialTheme.colorScheme.error)
                }
                personalTurns.forEachIndexed { personalIndex, indexedTurn ->
                    val (sourceIndex, turn) = indexedTurn
                    val cue = turns.getOrNull(sourceIndex - 1)
                    if (cue != null) {
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer), modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(12.dp)) {
                                Text("راهنمای پاسخ — ${cue.roleTitle}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                                Text(cue.content, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                    TurnCard(turn = turn, index = sourceIndex, active = activeIndex == sourceIndex,
                        canEdit = false, itemCount = turns.size, onMove = onMoveTurn, onDelete = onDeleteTurn,
                        label = "نوبت شما ${personalIndex + 1} — ${turn.sectionTitle}")
                }
            }

            HorizontalDivider()
            Text("کنترل تمرین", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("نوبت ${if (turns.isEmpty()) 0 else activeIndex + 1} از ${turns.size}")
                Row {
                    OutlinedButton(onClick = { activeIndex = (activeIndex - 1).coerceAtLeast(0) }, enabled = activeIndex > 0) { Text("قبلی") }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = { activeIndex = (activeIndex + 1).coerceAtMost((turns.size - 1).coerceAtLeast(0)) }, enabled = activeIndex < turns.lastIndex) { Text("نوبت بعد") }
                }
            }
            turns.getOrNull(activeIndex)?.let { current ->
                Text("گوینده نوبت فعال: ${current.roleTitle}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
private fun TurnCard(
    turn: DialogueTurnDisplay,
    index: Int,
    active: Boolean,
    canEdit: Boolean,
    itemCount: Int,
    onMove: (Int, Int) -> Unit,
    onDelete: (DialogueTurnDisplay) -> Unit,
    label: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        border = if (active) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
        colors = CardDefaults.cardColors(containerColor = if (active) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                if (canEdit) {
                    Row {
                        IconButton(onClick = { onMove(index, -1) }, enabled = index > 0) { Icon(Icons.Filled.KeyboardArrowUp, contentDescription = "جابه‌جایی به بالا") }
                        IconButton(onClick = { onMove(index, 1) }, enabled = index < itemCount - 1) { Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "جابه‌جایی به پایین") }
                        IconButton(onClick = { onDelete(turn) }) { Icon(Icons.Filled.Delete, contentDescription = "حذف نوبت از گفتگو") }
                    }
                }
            }
            Text(turn.content, style = MaterialTheme.typography.bodyLarge)
        }
    }
}
