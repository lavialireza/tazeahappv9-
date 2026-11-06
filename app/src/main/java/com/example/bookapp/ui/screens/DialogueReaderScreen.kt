
package com.example.bookapp.ui.screens

import androidx.compose.foundation.BorderStroke
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
import com.example.bookapp.data.DialogueStageCueEntity

data class DialogueTurnDisplay(
    val turnId: Long,
    val sectionId: Long,
    val roleTitle: String,
    val sectionTitle: String,
    val content: String,
    val roleId: Long = 0L,

    // اطلاعات مستقل ترتیب و تمرین
    val sourceSequenceIndex: Int? = null,
    val sourceSequenceStatus: String =
        DialogueTurnMetadataStatus.UNVERIFIED,
    val rehearsalOrderIndex: Int = 0,

    // ارتباط صریح پرسش و پاسخ
    val expectsResponse: Boolean = false,
    val replyToTurnId: Long? = null,
    val expectedResponderRoleId: Long? = null,
    val expectedResponderRoleTitle: String? = null,

    // اطلاعات تکمیلی
    val sequenceEvidence: String = "",
    val directorNote: String = "",
    val stageCues: List<DialogueStageCueEntity> = emptyList()
)

object DialogueTurnMetadataStatus {
    const val UNVERIFIED = "UNVERIFIED"
    const val DOCUMENTED = "DOCUMENTED"
    const val DIRECTOR_CONFIRMED = "DIRECTOR_CONFIRMED"
}

private enum class DialogueViewMode {
    GROUP,
    PERSONAL
}

/**
 * نمایش گفت‌وگوی چندنقشی:
 *
 * GROUP:
 * نمایش همه نوبت‌ها در ترتیب تمرین.
 *
 * PERSONAL:
 * نمایش نوبت‌های نقش انتخابی.
 * راهنمای پاسخ فقط بر اساس رابطه صریح replyToTurnId نمایش داده می‌شود.
 *
 * ترتیب اصلی متن و نسخه منبع تغییر نمی‌کند.
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
    var mode by remember {
        mutableStateOf(DialogueViewMode.GROUP)
    }

    var selectedRole by remember(turns) {
        mutableStateOf(turns.firstOrNull()?.roleId ?: 0L)
    }

    var activeIndex by remember(turns) {
        mutableIntStateOf(0)
    }

    val roleOptions = remember(turns) {
        turns.distinctBy {
            if (it.roleId != 0L) {
                "id:${it.roleId}"
            } else {
                it.roleTitle
            }
        }.map {
            it.roleId to it.roleTitle
        }
    }

    val personalTurns = remember(turns, selectedRole) {
        turns.mapIndexedNotNull { index, turn ->
            val isMine =
                if (selectedRole != 0L) {
                    turn.roleId == selectedRole
                } else {
                    turn.roleTitle ==
                        roleOptions.firstOrNull {
                            it.first == selectedRole
                        }?.second
                }

            if (isMine) index to turn else null
        }
    }

    val allSequenceVerified = turns.isNotEmpty() &&
        turns.all {
            it.sourceSequenceStatus ==
                DialogueTurnMetadataStatus.DOCUMENTED ||
                it.sourceSequenceStatus ==
                DialogueTurnMetadataStatus.DIRECTOR_CONFIRMED
        }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(dialogueTitle)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "بازگشت"
                        )
                    }
                },
                actions = {
                    if (!readOnly) {
                        IconButton(onClick = onAddTurn) {
                            Icon(
                                Icons.Filled.Add,
                                contentDescription = "افزودن نوبت"
                            )
                        }
                    }

                    if (showPdf) {
                        IconButton(onClick = onExportPdf) {
                            Icon(
                                Icons.Filled.Share,
                                contentDescription = "خروجی PDF گفتگو"
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {

            Text(
                "حالت نمایش گفتگو",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = mode == DialogueViewMode.GROUP,
                    onClick = {
                        mode = DialogueViewMode.GROUP
                    },
                    label = {
                        Text("نمایش همه نقش‌ها")
                    }
                )

                FilterChip(
                    selected = mode == DialogueViewMode.PERSONAL,
                    onClick = {
                        mode = DialogueViewMode.PERSONAL
                    },
                    label = {
                        Text("تمرین نقش من")
                    }
                )
            }

            if (mode == DialogueViewMode.PERSONAL) {

                Text(
                    "نقش شبیه‌خوان:",
                    style = MaterialTheme.typography.labelLarge
                )

                roleOptions.forEach { (roleId, roleTitle) ->
                    FilterChip(
                        selected = selectedRole == roleId,
                        onClick = {
                            selectedRole = roleId
                        },
                        label = {
                            Text(roleTitle)
                        }
                    )
                }

                Text(
                    "در این حالت فقط نوبت‌های نقش انتخابی برجسته می‌شوند. " +
                        "راهنمای پاسخ صرفاً بر اساس ارتباط ثبت‌شده نمایش داده می‌شود.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

            } else {

                Text(
                    "ترتیب نمایش بر اساس ترتیب تمرین ثبت‌شده است؛ " +
                        "این ترتیب لزوماً ترتیب تاریخی اجرای تعزیه نیست.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            HorizontalDivider()

            if (turns.isNotEmpty() && !allSequenceVerified) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor =
                            MaterialTheme.colorScheme.secondaryContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            "هشدار درباره ترتیب اجرای اصلی",
                            fontWeight = FontWeight.Bold,
                            color =
                                MaterialTheme.colorScheme.onSecondaryContainer
                        )

                        Text(
                            "ترتیب تاریخی یا مستند همه نوبت‌ها تأیید نشده است. " +
                                "ترتیب فعلی فقط برای نمایش و تمرین استفاده می‌شود.",
                            style = MaterialTheme.typography.bodySmall,
                            color =
                                MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }

            if (mode == DialogueViewMode.GROUP) {

                if (turns.isEmpty()) {
                    Text("هنوز نوبتی برای این گفتگو ثبت نشده است.")
                }

                turns.forEachIndexed { index, turn ->
                    TurnCard(
                        turn = turn,
                        index = index,
                        active = activeIndex == index,
                        canEdit = !readOnly,
                        itemCount = turns.size,
                        onMove = onMoveTurn,
                        onDelete = onDeleteTurn,
                        label = "${index + 1}. ${turn.roleTitle}"
                    )
                }

            } else {

                if (personalTurns.isEmpty()) {
                    Text(
                        "برای این نقش نوبتی در گفتگوی انتخاب‌شده ثبت نشده است.",
                        color = MaterialTheme.colorScheme.error
                    )
                }

                personalTurns.forEachIndexed { personalIndex, indexedTurn ->

                    val (sourceIndex, turn) = indexedTurn

                    // فقط رابطه صریح ثبت‌شده معتبر است.
                    val responseSource = turn.replyToTurnId?.let { replyId ->
                        turns.firstOrNull {
                            it.turnId == replyId
                        }
                    }

                    if (responseSource != null) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor =
                                    MaterialTheme.colorScheme.tertiaryContainer
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                Modifier.padding(12.dp),
                                verticalArrangement =
                                    Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    "راهنمای پاسخ — ${responseSource.roleTitle}",
                                    fontWeight = FontWeight.Bold,
                                    style =
                                        MaterialTheme.typography.labelLarge
                                )

                                Text(
                                    responseSource.content,
                                    style =
                                        MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    } else {
                        Text(
                            "برای این نوبت رابطه پاسخ مشخصی ثبت نشده است.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    TurnCard(
                        turn = turn,
                        index = sourceIndex,
                        active = activeIndex == sourceIndex,
                        canEdit = false,
                        itemCount = turns.size,
                        onMove = onMoveTurn,
                        onDelete = onDeleteTurn,
                        label =
                            "نوبت شما ${personalIndex + 1} — ${turn.sectionTitle}"
                    )
                }
            }

            HorizontalDivider()

            Text(
                "کنترل تمرین",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {

                Text(
                    "نوبت ${if (turns.isEmpty()) 0 else activeIndex + 1} از ${turns.size}"
                )

                Row {
                    OutlinedButton(
                        onClick = {
                            activeIndex =
                                (activeIndex - 1).coerceAtLeast(0)
                        },
                        enabled = activeIndex > 0
                    ) {
                        Text("قبلی")
                    }

                    Spacer(Modifier.width(8.dp))

                    Button(
                        onClick = {
                            activeIndex =
                                (activeIndex + 1)
                                    .coerceAtMost(
                                        (turns.size - 1).coerceAtLeast(0)
                                    )
                        },
                        enabled = activeIndex < turns.lastIndex
                    ) {
                        Text("نوبت بعد")
                    }
                }
            }

            turns.getOrNull(activeIndex)?.let { current ->

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor =
                            MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            "گوینده نوبت فعال: ${current.roleTitle}",
                            fontWeight = FontWeight.Bold,
                            color =
                                MaterialTheme.colorScheme.onPrimaryContainer
                        )

                        Text(
                            current.content,
                            color =
                                MaterialTheme.colorScheme.onPrimaryContainer
                        )

                        if (current.expectedResponderRoleTitle != null) {
                            Text(
                                "پاسخ‌دهنده مورد انتظار: " +
                                    current.expectedResponderRoleTitle,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
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
        border = if (active) {
            BorderStroke(
                2.dp,
                MaterialTheme.colorScheme.primary
            )
        } else {
            null
        },
        colors = CardDefaults.cardColors(
            containerColor = if (active) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {

        Column(
            Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {

                Text(
                    label,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                if (canEdit) {
                    Row {
                        IconButton(
                            onClick = {
                                onMove(index, -1)
                            },
                            enabled = index > 0
                        ) {
                            Icon(
                                Icons.Filled.KeyboardArrowUp,
                                contentDescription = "جابه‌جایی به بالا"
                            )
                        }

                        IconButton(
                            onClick = {
                                onMove(index, 1)
                            },
                            enabled = index < itemCount - 1
                        ) {
                            Icon(
                                Icons.Filled.KeyboardArrowDown,
                                contentDescription = "جابه‌جایی به پایین"
                            )
                        }

                        IconButton(
                            onClick = {
                                onDelete(turn)
                            }
                        ) {
                            Icon(
                                Icons.Filled.Delete,
                                contentDescription = "حذف نوبت از گفتگو"
                            )
                        }
                    }
                }
            }

            Text(
                turn.content,
                style = MaterialTheme.typography.bodyLarge
            )

            if (turn.sourceSequenceIndex != null) {
                Text(
                    "شماره ترتیب مستند: ${turn.sourceSequenceIndex + 1}",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            when (turn.sourceSequenceStatus) {
                DialogueTurnMetadataStatus.DOCUMENTED -> {
                    Text(
                        "ترتیب مستند",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelSmall
                    )
                }

                DialogueTurnMetadataStatus.DIRECTOR_CONFIRMED -> {
                    Text(
                        "تأییدشده توسط کارگردان",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelSmall
                    )
                }

                else -> {
                    Text(
                        "ترتیب اجرای اصلی تأیید نشده",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }

            if (turn.sequenceEvidence.isNotBlank()) {
                Text(
                    "مستند ترتیب: ${turn.sequenceEvidence}",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            if (turn.directorNote.isNotBlank()) {
                Text(
                    "یادداشت کارگردان: ${turn.directorNote}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium
                )
            }

            turn.stageCues.forEach { cue ->
                val cueTitle = when (cue.cueType) {
                    DialogueStageCueEntity.ENTER -> "ورود"
                    DialogueStageCueEntity.EXIT -> "خروج"
                    DialogueStageCueEntity.PAUSE -> "مکث"
                    DialogueStageCueEntity.SILENCE -> "سکوت"
                    DialogueStageCueEntity.MOVEMENT -> "حرکت"
                    DialogueStageCueEntity.STAGE_DIRECTION -> "دستور صحنه"
                    else -> "نشانه اجرایی"
                }

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor =
                            MaterialTheme.colorScheme.tertiaryContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        Modifier.padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text(
                            cueTitle,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelMedium
                        )

                        if (cue.cueText.isNotBlank()) {
                            Text(
                                cue.cueText,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        Text(
                            if (cue.isVerified) {
                                "تأیید شده"
                            } else {
                                "تأیید نشده"
                            },
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
        }
    }
}
