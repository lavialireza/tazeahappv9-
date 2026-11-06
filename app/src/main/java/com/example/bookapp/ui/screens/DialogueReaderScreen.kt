
package com.example.bookapp.ui.screens

import com.example.bookapp.data.DialogueStageCueEntity
import com.example.bookapp.data.DialogueTurnMetadataEntity

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
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
    val roleId: Long = 0L,
    val sourceSequenceIndex: Int? = null,
    val sourceSequenceStatus: String = "UNVERIFIED",
    val rehearsalOrderIndex: Int = 0,
    val expectsResponse: Boolean = false,
    val replyToTurnId: Long? = null,
    val expectedResponderRoleId: Long? = null,
    val expectedResponderRoleTitle: String? = null,
    val sequenceEvidence: String = "",
    val directorNote: String = "",
    val stageCues: List<DialogueStageCueEntity> = emptyList()
)

private enum class DialogueViewMode {
    GROUP,
    PERSONAL
}

private data class CueOption(
    val type: String,
    val title: String
)

private val dialogueCueOptions = listOf(
    CueOption(DialogueStageCueEntity.ENTER, "ورود"),
    CueOption(DialogueStageCueEntity.EXIT, "خروج"),
    CueOption(DialogueStageCueEntity.PAUSE, "مکث"),
    CueOption(DialogueStageCueEntity.SILENCE, "سکوت"),
    CueOption(DialogueStageCueEntity.MOVEMENT, "حرکت"),
    CueOption(DialogueStageCueEntity.STAGE_DIRECTION, "دستور صحنه"),
    CueOption(DialogueStageCueEntity.OTHER, "سایر")
)

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
    showPdf: Boolean = true,

    onSaveResponseRelation: (
        turn: DialogueTurnDisplay,
        expectsResponse: Boolean,
        replyToTurnId: Long?,
        expectedResponderRoleId: Long?
    ) -> Unit = { _, _, _, _ -> },

    onAddStageCue: (
        turn: DialogueTurnDisplay,
        cueType: String,
        cueText: String
    ) -> Unit = { _, _, _ -> },

    onDeleteStageCue: (
        cue: DialogueStageCueEntity
    ) -> Unit = {}
) {
    var mode by remember {
        mutableStateOf(DialogueViewMode.GROUP)
    }

    var selectedRole by remember(turns) {
        mutableStateOf(turns.firstOrNull()?.roleId ?: 0L)
    }

    var activeTurnId by remember(turns) {
        mutableStateOf(turns.firstOrNull()?.turnId)
    }

    var relationEditorTurn by remember {
        mutableStateOf<DialogueTurnDisplay?>(null)
    }

    var cueEditorTurn by remember {
        mutableStateOf<DialogueTurnDisplay?>(null)
    }

    val roleOptions = remember(turns) {
        turns.filter { it.roleId != 0L }
            .distinctBy { it.roleId }
            .map { it.roleId to it.roleTitle }
    }

    val personalTurns = remember(turns, selectedRole) {
        turns.filter { it.roleId == selectedRole }
    }

    val activeIndex = turns.indexOfFirst {
        it.turnId == activeTurnId
    }.let { if (it < 0) 0 else it }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(dialogueTitle, maxLines = 1)
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
                                Icons.Default.Add,
                                contentDescription = "افزودن نوبت"
                            )
                        }
                    }

                    if (showPdf) {
                        IconButton(onClick = onExportPdf) {
                            Icon(
                                Icons.Default.Share,
                                contentDescription = "خروجی PDF"
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            Text(
                "شیوه نمایش و تمرین",
                style = MaterialTheme.typography.titleMedium,
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
                        Text("تمرین گروهی")
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
                    "انتخاب نقش:",
                    style = MaterialTheme.typography.titleSmall
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
                    "در این حالت نوبت‌های نقش انتخاب‌شده نمایش داده می‌شوند. نوبت مرتبط با پاسخ فقط در صورت ثبت رابطه صریح نمایش داده خواهد شد.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            HorizontalDivider()

            if (mode == DialogueViewMode.GROUP) {

                if (turns.isEmpty()) {
                    Text("هنوز نوبتی برای این گفت‌وگو ثبت نشده است.")
                }

                turns.forEachIndexed { index, turn ->
                    TurnCard(
                        turn = turn,
                        index = index,
                        active = activeTurnId == turn.turnId,
                        canEdit = !readOnly,
                        itemCount = turns.size,
                        onMove = onMoveTurn,
                        onDelete = onDeleteTurn,
                        onActivate = {
                            activeTurnId = turn.turnId
                        },
                        onEditRelation = {
                            relationEditorTurn = turn
                        },
                        onEditCues = {
                            cueEditorTurn = turn
                        },
                        label = "${index + 1}. ${turn.roleTitle}"
                    )
                }

            } else {

                if (personalTurns.isEmpty()) {
                    Text(
                        "برای این نقش نوبتی در گفت‌وگوی انتخاب‌شده ثبت نشده است.",
                        color = MaterialTheme.colorScheme.error
                    )
                }

                personalTurns.forEachIndexed { index, turn ->

                    val responseSource = turn.replyToTurnId?.let { replyId ->
                        turns.firstOrNull { it.turnId == replyId }
                    }

                    if (responseSource != null) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor =
                                    MaterialTheme.colorScheme.tertiaryContainer
                            )
                        ) {
                            Column(
                                Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    "نوبت مرتبط با پاسخ — ${responseSource.roleTitle}",
                                    fontWeight = FontWeight.Bold
                                )
                                Text(responseSource.sectionTitle)
                                Text(responseSource.content)
                            }
                        }
                    }

                    TurnCard(
                        turn = turn,
                        index = turns.indexOf(turn),
                        active = activeTurnId == turn.turnId,
                        canEdit = false,
                        itemCount = turns.size,
                        onMove = onMoveTurn,
                        onDelete = onDeleteTurn,
                        onActivate = {
                            activeTurnId = turn.turnId
                        },
                        onEditRelation = {
                            relationEditorTurn = turn
                        },
                        onEditCues = {
                            cueEditorTurn = turn
                        },
                        label = "نوبت شما ${index + 1} — ${turn.sectionTitle}"
                    )
                }
            }

            HorizontalDivider()

            Text(
                "کنترل تمرین",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "نوبت ${if (turns.isEmpty()) 0 else activeIndex + 1} از ${turns.size}"
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        enabled = activeIndex > 0,
                        onClick = {
                            activeTurnId =
                                turns.getOrNull(activeIndex - 1)?.turnId
                        }
                    ) {
                        Text("قبلی")
                    }

                    Button(
                        enabled = activeIndex < turns.lastIndex,
                        onClick = {
                            activeTurnId =
                                turns.getOrNull(activeIndex + 1)?.turnId
                        }
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
                        Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            "گوینده نوبت فعال: ${current.roleTitle}",
                            fontWeight = FontWeight.Bold
                        )
                        Text(current.sectionTitle)

                        if (current.expectsResponse) {
                            Text(
                                current.expectedResponderRoleTitle?.let {
                                    "پاسخ مورد انتظار: $it"
                                } ?: "پاسخ مورد انتظار ثبت شده، اما نقش پاسخ‌دهنده مشخص نشده است."
                            )
                        }
                    }
                }
            }
        }
    }

    relationEditorTurn?.let { turn ->
        ResponseRelationDialog(
            turn = turn,
            allTurns = turns,
            roles = roleOptions,
            onDismiss = {
                relationEditorTurn = null
            },
            onSave = { expects, replyTo, responder ->
                onSaveResponseRelation(
                    turn,
                    expects,
                    replyTo,
                    responder
                )
                relationEditorTurn = null
            }
        )
    }

    cueEditorTurn?.let { turn ->
        StageCueDialog(
            turn = turn,
            onDismiss = {
                cueEditorTurn = null
            },
            onAdd = { type, text ->
                onAddStageCue(turn, type, text)
                cueEditorTurn = null
            },
            onDelete = { cue ->
                onDeleteStageCue(cue)
                cueEditorTurn = null
            }
        )
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
    onActivate: () -> Unit,
    onEditRelation: () -> Unit,
    onEditCues: () -> Unit,
    label: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (active) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        ),
        onClick = onActivate
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    label,
                    modifier = Modifier.weight(1f),
                    fontWeight = FontWeight.Bold
                )

                if (canEdit) {
                    IconButton(
                        enabled = index > 0,
                        onClick = { onMove(index, -1) }
                    ) {
                        Icon(
                            Icons.Default.KeyboardArrowUp,
                            contentDescription = "بالا بردن در ترتیب تمرین"
                        )
                    }

                    IconButton(
                        enabled = index < itemCount - 1,
                        onClick = { onMove(index, 1) }
                    ) {
                        Icon(
                            Icons.Default.KeyboardArrowDown,
                            contentDescription = "پایین بردن در ترتیب تمرین"
                        )
                    }

                    IconButton(onClick = { onDelete(turn) }) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "حذف نوبت"
                        )
                    }
                }
            }

            Text(
                turn.sectionTitle,
                style = MaterialTheme.typography.titleSmall
            )

            Text(
                turn.content,
                style = MaterialTheme.typography.bodyLarge
            )

            if (turn.expectsResponse) {
                Text(
                    turn.expectedResponderRoleTitle?.let {
                        "پاسخ مورد انتظار از: $it"
                    } ?: "این نوبت انتظار پاسخ دارد؛ نقش پاسخ‌دهنده مشخص نشده است.",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            turn.replyToTurnId?.let { replyId ->
                Text(
                    "شناسه نوبت مرجع پاسخ: $replyId",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            when (turn.sourceSequenceStatus) {
                DialogueTurnMetadataEntity.SOURCE_DOCUMENTED -> {
                    Text(
                        "وضعیت ترتیب: مستند به منبع",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                DialogueTurnMetadataEntity.SOURCE_DIRECTOR_CONFIRMED -> {
                    Text(
                        "وضعیت ترتیب: تأییدشده توسط کارگردان",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                DialogueTurnMetadataEntity.SOURCE_UNVERIFIED -> {
                    Text(
                        "ترتیب اجرای مستند این نوبت هنوز تأیید نشده است.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
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
                    style = MaterialTheme.typography.bodySmall
                )
            }

            if (turn.stageCues.isNotEmpty()) {
                HorizontalDivider()

                Text(
                    "نشانه‌های اجرایی",
                    fontWeight = FontWeight.Bold
                )

                turn.stageCues.forEach { cue ->
                    val title = dialogueCueOptions
                        .firstOrNull { it.type == cue.cueType }
                        ?.title ?: "نشانه اجرایی"

                    val sourceLabel =
                        if (cue.origin ==
                            DialogueStageCueEntity.ORIGIN_SOURCE_TEXT
                        ) {
                            " (متن منبع)"
                        } else {
                            " (افزوده‌شده)"
                        }

                    Text(
                        if (cue.cueText.isBlank()) {
                            "• $title$sourceLabel"
                        } else {
                            "• $title: ${cue.cueText}$sourceLabel"
                        },
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            if (canEdit) {
                HorizontalDivider()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        modifier = Modifier.weight(1f),
                        onClick = onEditRelation
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = null
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("تنظیم رابطه پاسخ")
                    }

                    OutlinedButton(
                        modifier = Modifier.weight(1f),
                        onClick = onEditCues
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = null
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("نشانه صحنه")
                    }
                }
            }
        }
    }
}

@Composable
private fun ResponseRelationDialog(
    turn: DialogueTurnDisplay,
    allTurns: List<DialogueTurnDisplay>,
    roles: List<Pair<Long, String>>,
    onDismiss: () -> Unit,
    onSave: (
        expectsResponse: Boolean,
        replyToTurnId: Long?,
        expectedResponderRoleId: Long?
    ) -> Unit
) {
    var expectsResponse by remember(turn.turnId) {
        mutableStateOf(turn.expectsResponse)
    }

    var replyToTurnId by remember(turn.turnId) {
        mutableStateOf(turn.replyToTurnId)
    }

    var responderRoleId by remember(turn.turnId) {
        mutableStateOf(turn.expectedResponderRoleId)
    }

    val possibleReplies = allTurns.filter {
        it.turnId != turn.turnId
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("تنظیم رابطه پاسخ")
        },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 480.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "نوبت: ${turn.roleTitle} — ${turn.sectionTitle}",
                    fontWeight = FontWeight.Bold
                )

                HorizontalDivider()

                Text("آیا این نوبت انتظار پاسخ دارد؟")

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = expectsResponse,
                        onCheckedChange = {
                            expectsResponse = it
                            if (!it) responderRoleId = null
                        }
                    )
                    Text("بله، پاسخ مورد انتظار دارد")
                }

                if (expectsResponse) {
                    Text(
                        "نقش پاسخ‌دهنده مورد انتظار:",
                        fontWeight = FontWeight.Bold
                    )

                    if (roles.isEmpty()) {
                        Text("نقشی برای انتخاب وجود ندارد.")
                    }

                    roles.forEach { (id, title) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = responderRoleId == id,
                                onClick = {
                                    responderRoleId = id
                                }
                            )
                            Text(title)
                        }
                    }
                }

                HorizontalDivider()

                Text(
                    "این نوبت پاسخ کدام نوبت است؟",
                    fontWeight = FontWeight.Bold
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = replyToTurnId == null,
                        onClick = {
                            replyToTurnId = null
                        }
                    )
                    Text("رابطه پاسخی ثبت نشود")
                }

                possibleReplies.forEach { candidate ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = replyToTurnId == candidate.turnId,
                            onClick = {
                                replyToTurnId = candidate.turnId
                            }
                        )

                        Column {
                            Text(candidate.roleTitle)
                            Text(
                                candidate.sectionTitle,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(
                        expectsResponse,
                        replyToTurnId,
                        responderRoleId
                    )
                }
            ) {
                Text("ذخیره")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("انصراف")
            }
        }
    )
}

@Composable
private fun StageCueDialog(
    turn: DialogueTurnDisplay,
    onDismiss: () -> Unit,
    onAdd: (String, String) -> Unit,
    onDelete: (DialogueStageCueEntity) -> Unit
) {
    var selectedType by remember(turn.turnId) {
        mutableStateOf(DialogueStageCueEntity.STAGE_DIRECTION)
    }

    var cueText by remember(turn.turnId) {
        mutableStateOf("")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("نشانه‌های اجرایی")
        },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 480.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "${turn.roleTitle} — ${turn.sectionTitle}",
                    fontWeight = FontWeight.Bold
                )

                Text("نشانه‌های ثبت‌شده:")

                if (turn.stageCues.isEmpty()) {
                    Text(
                        "هنوز نشانه‌ای برای این نوبت ثبت نشده است.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                turn.stageCues.forEach { cue ->
                    val title = dialogueCueOptions
                        .firstOrNull { it.type == cue.cueType }
                        ?.title ?: "سایر"

                    val isDirectorAdded =
                        cue.origin ==
                            DialogueStageCueEntity.ORIGIN_DIRECTOR_ADDED

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                "$title: ${cue.cueText}"
                            )
                            Text(
                                if (isDirectorAdded) {
                                    "افزوده‌شده توسط کارگردان"
                                } else {
                                    "استخراج‌شده از متن منبع"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        if (isDirectorAdded) {
                            TextButton(
                                onClick = {
                                    onDelete(cue)
                                }
                            ) {
                                Text("حذف")
                            }
                        }
                    }
                }

                HorizontalDivider()

                Text(
                    "افزودن نشانه جدید:",
                    fontWeight = FontWeight.Bold
                )

                dialogueCueOptions.forEach { option ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedType == option.type,
                            onClick = {
                                selectedType = option.type
                            }
                        )
                        Text(option.title)
                    }
                }

                OutlinedTextField(
                    value = cueText,
                    onValueChange = {
                        cueText = it
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = {
                        Text("توضیح نشانه (اختیاری)")
                    },
                    minLines = 2
                )

                Text(
                    "این نشانه به‌عنوان اطلاعات اجرایی افزوده می‌شود و متن اصلی تعزیه را تغییر نمی‌دهد.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = cueText.isNotBlank() ||
                    selectedType != DialogueStageCueEntity.STAGE_DIRECTION,
                onClick = {
                    onAdd(selectedType, cueText.trim())
                }
            ) {
                Text("ثبت نشانه")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("بستن")
            }
        }
    )
}
