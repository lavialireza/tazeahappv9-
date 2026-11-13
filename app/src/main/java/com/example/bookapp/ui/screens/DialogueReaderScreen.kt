package com.example.bookapp.ui.screens

import com.example.bookapp.data.DialogueStageCueEntity
import com.example.bookapp.data.DialogueTurnMetadataEntity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.relocation.rememberBringIntoViewRequester
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
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

data class DialogueTurnDisplay(
    val turnId: Long,
    val sectionId: Long,
    val roleTitle: String,
    val sectionTitle: String,
    val content: String,
    val roleId: Long = 0L,
    val versionLabel: String = "",
    val sourceSequenceIndex: Int? = null,
    val sourceSequenceStatus: String = DialogueTurnMetadataEntity.SOURCE_UNVERIFIED,
    val rehearsalOrderIndex: Int = 0,
    val expectsResponse: Boolean = false,
    val replyToTurnId: Long? = null,
    val expectedResponderRoleId: Long? = null,
    val expectedResponderRoleTitle: String? = null,
    val sequenceEvidence: String = "",
    val directorNote: String = "",
    val stageCues: List<DialogueStageCueEntity> = emptyList()
)

private enum class DialogueViewMode { GROUP, PERSONAL }

private data class DialogueSearchMatch(
    val turnId: Long,
    val start: Int,
    val end: Int
)

private data class CueOption(val type: String, val title: String)

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
    roleOrder: List<Long> = emptyList(),
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
    onAddStageCue: (turn: DialogueTurnDisplay, cueType: String, cueText: String) -> Unit =
        { _, _, _ -> },
    onDeleteStageCue: (cue: DialogueStageCueEntity) -> Unit = {}
) {
    var mode by remember { mutableStateOf(DialogueViewMode.GROUP) }
    var selectedRole by remember(turns) {
        mutableStateOf(turns.firstOrNull { it.roleId != 0L }?.roleId ?: 0L)
    }
    var activeTurnId by remember(turns) {
        mutableStateOf(turns.firstOrNull()?.turnId)
    }
    var relationEditorTurn by remember { mutableStateOf<DialogueTurnDisplay?>(null) }
    var cueEditorTurn by remember { mutableStateOf<DialogueTurnDisplay?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var activeSearchIndex by remember { mutableStateOf(0) }

    val orderedTurns = remember(turns) {
        turns.sortedWith(compareBy<DialogueTurnDisplay> { it.rehearsalOrderIndex }.thenBy { it.turnId })
    }
    val roleOptions = remember(orderedTurns, roleOrder) {
        val options = orderedTurns.filter { it.roleId != 0L }
            .distinctBy { it.roleId }
            .map { it.roleId to it.roleTitle }
        options.sortedBy { (id, _) ->
            val index = roleOrder.indexOf(id)
            if (index >= 0) index else Int.MAX_VALUE
        }
    }
    val personalTurns = remember(orderedTurns, selectedRole) {
        orderedTurns.filter { it.roleId == selectedRole }
    }
    // فقط رابطه صریح metadata ملاک است؛ از ترتیب متن پاسخ‌دهنده استنباط نمی‌شود.
    val incomingExpectedTurns = remember(orderedTurns, selectedRole) {
        orderedTurns.filter {
            it.expectsResponse && it.expectedResponderRoleId == selectedRole
        }
    }
    val activeSequence = if (mode == DialogueViewMode.PERSONAL) personalTurns else orderedTurns

    LaunchedEffect(mode, selectedRole, activeSequence) {
        if (activeSequence.none { it.turnId == activeTurnId }) {
            activeTurnId = activeSequence.firstOrNull()?.turnId
        }
    }
    val activeIndex = activeSequence.indexOfFirst { it.turnId == activeTurnId }
        .let { if (it < 0) 0 else it }
    val activeTurn = activeSequence.getOrNull(activeIndex)
    val searchMatches = remember(activeSequence, searchQuery) {
        findDialogueSearchMatches(activeSequence, searchQuery)
    }
    val safeSearchIndex = if (searchMatches.isEmpty()) 0 else activeSearchIndex.coerceIn(0, searchMatches.lastIndex)
    val activeSearchMatch = searchMatches.getOrNull(safeSearchIndex)

    LaunchedEffect(activeSearchMatch?.turnId, activeSearchMatch?.start) {
        activeSearchMatch?.let { activeTurnId = it.turnId }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(dialogueTitle, maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "بازگشت")
                    }
                },
                actions = {
                    if (!readOnly) {
                        IconButton(onClick = onAddTurn) {
                            Icon(Icons.Default.Add, contentDescription = "افزودن نوبت")
                        }
                    }
                    if (showPdf) {
                        IconButton(onClick = onExportPdf) {
                            Icon(Icons.Default.Share, contentDescription = "خروجی PDF")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("شیوه نمایش و تمرین", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = mode == DialogueViewMode.GROUP,
                    onClick = { mode = DialogueViewMode.GROUP },
                    label = { Text("تمرین گروهی") }
                )
                FilterChip(
                    selected = mode == DialogueViewMode.PERSONAL,
                    onClick = { mode = DialogueViewMode.PERSONAL },
                    label = { Text("تمرین نقش من") }
                )
            }

            if (mode == DialogueViewMode.PERSONAL) {
                Text("انتخاب نقش:", style = MaterialTheme.typography.titleSmall)
                if (roleOptions.isEmpty()) {
                    Text("برای این گفت‌وگو نقشی ثبت نشده است.", color = MaterialTheme.colorScheme.error)
                } else {
                    roleOptions.forEach { (roleId, roleTitle) ->
                        FilterChip(
                            selected = selectedRole == roleId,
                            onClick = { selectedRole = roleId },
                            label = { Text(roleTitle) }
                        )
                    }
                }
                Text(
                    "در تمرین شخصی، نوبت‌های نقش انتخاب‌شده و پرسش‌هایی که صریحاً پاسخ را از این نقش می‌خواهند نمایش داده می‌شوند. پاسخ‌دهنده از روی ترتیب متن حدس زده نمی‌شود.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (incomingExpectedTurns.isNotEmpty()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
                    ) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("نوبت‌هایی که پاسخ آن‌ها از نقش شما انتظار می‌رود", fontWeight = FontWeight.Bold)
                            incomingExpectedTurns.forEach { item ->
                                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                    Text("${item.roleTitle} — ${item.sectionTitle}", style = MaterialTheme.typography.titleSmall)
                                    Text(item.content, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    }
                }
            }

            HorizontalDivider()

            OutlinedTextField(
                value = searchQuery,
                onValueChange = {
                    searchQuery = it
                    activeSearchIndex = 0
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("جستجو در متن‌های این حالت") }
            )
            if (searchQuery.isNotBlank()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        if (searchMatches.isEmpty()) "نتیجه‌ای پیدا نشد."
                        else "نتیجه ${safeSearchIndex + 1} از ${searchMatches.size}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (searchMatches.isNotEmpty()) {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            TextButton(onClick = { activeSearchIndex = (safeSearchIndex - 1).mod(searchMatches.size) }) { Text("قبلی") }
                            TextButton(onClick = { activeSearchIndex = (safeSearchIndex + 1).mod(searchMatches.size) }) { Text("بعدی") }
                        }
                    }
                }
            }

            if (mode == DialogueViewMode.GROUP) {
                if (orderedTurns.isEmpty()) Text("هنوز نوبتی برای این گفت‌وگو ثبت نشده است.")
                orderedTurns.forEachIndexed { index, turn ->
                    TurnCard(
                        turn = turn, allTurns = orderedTurns, index = index,
                        active = activeTurnId == turn.turnId, canEdit = !readOnly,
                        itemCount = orderedTurns.size, onMove = onMoveTurn,
                        onDelete = onDeleteTurn,
                        onActivate = { activeTurnId = turn.turnId },
                        onEditRelation = { relationEditorTurn = turn },
                        onEditCues = { cueEditorTurn = turn },
                        searchMatches = searchMatches.filter { it.turnId == turn.turnId },
                        activeSearchMatch = activeSearchMatch?.takeIf { it.turnId == turn.turnId },
                        label = "${index + 1}. ${turn.roleTitle}"
                    )
                }
            } else {
                if (personalTurns.isEmpty()) {
                    Text("برای این نقش نوبتی در گفت‌وگوی انتخاب‌شده ثبت نشده است.", color = MaterialTheme.colorScheme.error)
                }
                personalTurns.forEachIndexed { index, turn ->
                    val responseSource = turn.replyToTurnId?.let { id ->
                        orderedTurns.firstOrNull { it.turnId == id }
                    }
                    if (turn.replyToTurnId != null && responseSource == null) {
                        RelationWarning("نوبت مرجع این پاسخ در فهرست فعلی پیدا نشد؛ رابطه نیازمند بررسی است.")
                    }
                    if (responseSource != null) {
                        RelatedTurnCard("این نوبت پاسخِ نوبت زیر است", responseSource)
                    }
                    TurnCard(
                        turn = turn, allTurns = orderedTurns,
                        index = orderedTurns.indexOfFirst { it.turnId == turn.turnId },
                        active = activeTurnId == turn.turnId, canEdit = false,
                        itemCount = orderedTurns.size, onMove = onMoveTurn,
                        onDelete = onDeleteTurn,
                        onActivate = { activeTurnId = turn.turnId },
                        onEditRelation = { relationEditorTurn = turn },
                        onEditCues = { cueEditorTurn = turn },
                        searchMatches = searchMatches.filter { it.turnId == turn.turnId },
                        activeSearchMatch = activeSearchMatch?.takeIf { it.turnId == turn.turnId },
                        label = "نوبت شما ${index + 1} — ${turn.sectionTitle}"
                    )
                }
            }

            HorizontalDivider()
            Text("کنترل تمرین", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("نوبت ${if (activeSequence.isEmpty()) 0 else activeIndex + 1} از ${activeSequence.size}")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        enabled = activeIndex > 0 && activeSequence.isNotEmpty(),
                        onClick = { activeTurnId = activeSequence.getOrNull(activeIndex - 1)?.turnId }
                    ) { Text("قبلی") }
                    Button(
                        enabled = activeIndex < activeSequence.lastIndex,
                        onClick = { activeTurnId = activeSequence.getOrNull(activeIndex + 1)?.turnId }
                    ) { Text("نوبت بعد") }
                }
            }

            activeTurn?.let { current ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("گوینده نوبت فعال: ${current.roleTitle}", fontWeight = FontWeight.Bold)
                        Text(current.sectionTitle)
                        if (current.expectsResponse) {
                            Text(
                                current.expectedResponderRoleTitle?.let { "پاسخ مورد انتظار: $it" }
                                    ?: "پاسخ مورد انتظار ثبت شده، اما نقش پاسخ‌دهنده مشخص نشده است."
                            )
                        }
                        if (current.replyToTurnId != null) {
                            val source = orderedTurns.firstOrNull { it.turnId == current.replyToTurnId }
                            if (source != null) {
                                Text("این نوبت در پاسخ به: ${source.roleTitle} — ${source.sectionTitle}", style = MaterialTheme.typography.bodySmall)
                            } else {
                                RelationWarning("نوبت مرجع پاسخ پیدا نشد.")
                            }
                        }
                    }
                }
            }
        }
    }

    relationEditorTurn?.let { turn ->
        ResponseRelationDialog(
            turn = turn, allTurns = orderedTurns, roles = roleOptions,
            onDismiss = { relationEditorTurn = null },
            onSave = { expects, replyTo, responder ->
                onSaveResponseRelation(turn, expects, replyTo, responder)
                relationEditorTurn = null
            }
        )
    }

    cueEditorTurn?.let { turn ->
        StageCueDialog(
            turn = turn,
            onDismiss = { cueEditorTurn = null },
            onAdd = { type, text ->
                onAddStageCue(turn, type, text)
                cueEditorTurn = null
            },
            onDelete = { cue ->
                if (cue.origin == DialogueStageCueEntity.ORIGIN_DIRECTOR_ADDED) {
                    onDeleteStageCue(cue)
                    cueEditorTurn = null
                }
            }
        )
    }
}

@Composable
private fun RelatedTurnCard(title: String, turn: DialogueTurnDisplay) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(title, fontWeight = FontWeight.Bold)
            Text("${turn.roleTitle} — ${turn.sectionTitle}")
            Text(turn.content)
        }
    }
}

@Composable
private fun RelationWarning(text: String) {
    Text(text, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
}

@Composable
private fun TurnCard(
    turn: DialogueTurnDisplay,
    allTurns: List<DialogueTurnDisplay>,
    index: Int,
    active: Boolean,
    canEdit: Boolean,
    itemCount: Int,
    onMove: (Int, Int) -> Unit,
    onDelete: (DialogueTurnDisplay) -> Unit,
    onActivate: () -> Unit,
    onEditRelation: () -> Unit,
    onEditCues: () -> Unit,
    searchMatches: List<DialogueSearchMatch> = emptyList(),
    activeSearchMatch: DialogueSearchMatch? = null,
    label: String
) {
    val replySource = turn.replyToTurnId?.let { id -> allTurns.firstOrNull { it.turnId == id } }
    var showFullText by remember(turn.turnId) { mutableStateOf(false) }
    val bringIntoViewRequester = rememberBringIntoViewRequester()

    LaunchedEffect(activeSearchMatch?.start, activeSearchMatch?.end) {
        if (activeSearchMatch != null) {
            bringIntoViewRequester.bringIntoView()
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .bringIntoViewRequester(bringIntoViewRequester),
        colors = CardDefaults.cardColors(
            containerColor = if (active) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
        ),
        onClick = onActivate
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(label, modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
                if (canEdit) {
                    IconButton(enabled = index > 0, onClick = { onMove(index, -1) }) {
                        Icon(Icons.Default.KeyboardArrowUp, contentDescription = "بالا بردن در ترتیب تمرین")
                    }
                    IconButton(enabled = index >= 0 && index < itemCount - 1, onClick = { onMove(index, 1) }) {
                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = "پایین بردن در ترتیب تمرین")
                    }
                    IconButton(onClick = { onDelete(turn) }) {
                        Icon(Icons.Default.Delete, contentDescription = "حذف نوبت")
                    }
                }
            }

            Text(turn.sectionTitle, style = MaterialTheme.typography.titleSmall)
            if (turn.versionLabel.isNotBlank()) {
                Text("منبع: ${turn.versionLabel}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(
                text = buildDialogueHighlightedText(
                    text = turn.content,
                    matches = searchMatches,
                    activeMatch = activeSearchMatch,
                    normalHighlight = MaterialTheme.colorScheme.tertiaryContainer,
                    activeHighlight = MaterialTheme.colorScheme.primaryContainer,
                    activeTextColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                style = MaterialTheme.typography.bodyLarge
            )
            TextButton(onClick = { showFullText = true }) { Text("نمایش تمام‌صفحه متن") }

            if (turn.expectsResponse) {
                Text(
                    turn.expectedResponderRoleTitle?.let { "پاسخ مورد انتظار از: $it" }
                        ?: "این نوبت انتظار پاسخ دارد؛ نقش پاسخ‌دهنده مشخص نشده است.",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            if (turn.replyToTurnId != null) {
                if (replySource != null) {
                    Text("پاسخ به نوبت: ${replySource.roleTitle} — ${replySource.sectionTitle}", style = MaterialTheme.typography.bodySmall)
                } else {
                    RelationWarning("نوبت مرجع پاسخ پیدا نشد؛ رابطه نیازمند بررسی است.")
                }
            }

            when (turn.sourceSequenceStatus) {
                DialogueTurnMetadataEntity.SOURCE_DOCUMENTED -> Text(
                    "وضعیت ترتیب: مستند به منبع",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
                DialogueTurnMetadataEntity.SOURCE_DIRECTOR_CONFIRMED -> Text(
                    "وضعیت ترتیب: تأییدشده توسط کارگردان",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
                DialogueTurnMetadataEntity.SOURCE_UNVERIFIED -> Text(
                    "ترتیب اجرای مستند این نوبت هنوز تأیید نشده است.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
                else -> Text(
                    "وضعیت مستندبودن ترتیب مشخص نیست: ${turn.sourceSequenceStatus}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            if (turn.sequenceEvidence.isNotBlank()) {
                Text("مستند ترتیب: ${turn.sequenceEvidence}", style = MaterialTheme.typography.bodySmall)
            }
            if (turn.directorNote.isNotBlank()) {
                Text("یادداشت کارگردان: ${turn.directorNote}", style = MaterialTheme.typography.bodySmall)
            }

            if (turn.stageCues.isNotEmpty()) {
                HorizontalDivider()
                Text("نشانه‌های اجرایی", fontWeight = FontWeight.Bold)
                turn.stageCues.sortedBy { it.orderIndex }.forEach { cue ->
                    val cueTitle = dialogueCueOptions.firstOrNull { it.type == cue.cueType }?.title ?: "نشانه اجرایی"
                    val fromSource = cue.origin == DialogueStageCueEntity.ORIGIN_SOURCE_TEXT
                    val originLabel = if (fromSource) " (متن منبع)" else " (افزوده‌شده توسط مدیر)"
                    Text(
                        if (cue.cueText.isBlank()) "• $cueTitle$originLabel" else "• $cueTitle: ${cue.cueText}$originLabel",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            if (canEdit) {
                HorizontalDivider()
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(modifier = Modifier.weight(1f), onClick = onEditRelation) {
                        Icon(Icons.Default.Edit, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("تنظیم رابطه پاسخ")
                    }
                    OutlinedButton(modifier = Modifier.weight(1f), onClick = onEditCues) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("نشانه صحنه")
                    }
                }
            }
        }
    }
    if (showFullText) {
        Dialog(
            onDismissRequest = { showFullText = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "${turn.roleTitle} — ${turn.sectionTitle}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    if (turn.versionLabel.isNotBlank()) {
                        Text("منبع: ${turn.versionLabel}", style = MaterialTheme.typography.bodySmall)
                    }
                    Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())) {
                        Text(
                            buildDialogueHighlightedText(
                                text = turn.content,
                                matches = searchMatches,
                                activeMatch = activeSearchMatch,
                                normalHighlight = MaterialTheme.colorScheme.tertiaryContainer,
                                activeHighlight = MaterialTheme.colorScheme.primaryContainer,
                                activeTextColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                    Button(
                        onClick = { showFullText = false },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("بازگشت به گفتگو") }
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
    onSave: (expectsResponse: Boolean, replyToTurnId: Long?, expectedResponderRoleId: Long?) -> Unit
) {
    var expectsResponse by remember(turn.turnId) { mutableStateOf(turn.expectsResponse) }
    var replyToTurnId by remember(turn.turnId) { mutableStateOf(turn.replyToTurnId) }
    var responderRoleId by remember(turn.turnId) { mutableStateOf(turn.expectedResponderRoleId) }
    val possibleReplies = allTurns.filter { it.turnId != turn.turnId }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("تنظیم رابطه پاسخ") },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 480.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("نوبت: ${turn.roleTitle} — ${turn.sectionTitle}", fontWeight = FontWeight.Bold)
                HorizontalDivider()
                Text("آیا این نوبت انتظار پاسخ دارد؟")
                Row(verticalAlignment = Alignment.CenterVertically) {
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
                    Text("نقش پاسخ‌دهنده مورد انتظار:", fontWeight = FontWeight.Bold)
                    if (roles.isEmpty()) Text("نقشی برای انتخاب وجود ندارد.")
                    roles.forEach { (id, title) ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = responderRoleId == id, onClick = { responderRoleId = id })
                            Text(title)
                        }
                    }
                    if (responderRoleId == null) {
                        RelationWarning("نقش پاسخ‌دهنده هنوز انتخاب نشده است؛ با این حال می‌توانید رابطه ناقص را ذخیره کنید.")
                    }
                }

                HorizontalDivider()
                Text("این نوبت پاسخ کدام نوبت است؟", fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = replyToTurnId == null, onClick = { replyToTurnId = null })
                    Text("رابطه پاسخی ثبت نشود")
                }
                possibleReplies.forEach { candidate ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = replyToTurnId == candidate.turnId,
                            onClick = { replyToTurnId = candidate.turnId }
                        )
                        Column {
                            Text(candidate.roleTitle)
                            Text(candidate.sectionTitle, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(expectsResponse, replyToTurnId, if (expectsResponse) responderRoleId else null)
            }) { Text("ذخیره") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("انصراف") } }
    )
}

@Composable
private fun StageCueDialog(
    turn: DialogueTurnDisplay,
    onDismiss: () -> Unit,
    onAdd: (String, String) -> Unit,
    onDelete: (DialogueStageCueEntity) -> Unit
) {
    var selectedType by remember(turn.turnId) { mutableStateOf(DialogueStageCueEntity.STAGE_DIRECTION) }
    var cueText by remember(turn.turnId) { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("نشانه‌های اجرایی") },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 480.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("${turn.roleTitle} — ${turn.sectionTitle}", fontWeight = FontWeight.Bold)
                Text("نشانه‌های ثبت‌شده:")
                if (turn.stageCues.isEmpty()) {
                    Text("هنوز نشانه‌ای برای این نوبت ثبت نشده است.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                turn.stageCues.sortedBy { it.orderIndex }.forEach { cue ->
                    val cueTitle = dialogueCueOptions.firstOrNull { it.type == cue.cueType }?.title ?: "سایر"
                    val directorAdded = cue.origin == DialogueStageCueEntity.ORIGIN_DIRECTOR_ADDED
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("$cueTitle: ${cue.cueText}")
                            Text(
                                if (directorAdded) "افزوده‌شده توسط مدیر" else "استخراج‌شده از متن منبع — غیرقابل حذف از این صفحه",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (directorAdded) {
                            TextButton(onClick = { onDelete(cue) }) { Text("حذف") }
                        }
                    }
                }

                HorizontalDivider()
                Text("افزودن نشانه جدید:", fontWeight = FontWeight.Bold)
                dialogueCueOptions.forEach { option ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = selectedType == option.type, onClick = { selectedType = option.type })
                        Text(option.title)
                    }
                }
                OutlinedTextField(
                    value = cueText,
                    onValueChange = { cueText = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("توضیح نشانه (اختیاری)") },
                    minLines = 2
                )
                Text(
                    "این نشانه فقط به اطلاعات اجرایی افزوده می‌شود و متن اصلی تعزیه را تغییر نمی‌دهد.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = cueText.isNotBlank() || selectedType != DialogueStageCueEntity.STAGE_DIRECTION,
                onClick = { onAdd(selectedType, cueText.trim()) }
            ) { Text("ثبت نشانه") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("بستن") } }
    )
}


private fun findDialogueSearchMatches(
    turns: List<DialogueTurnDisplay>,
    query: String
): List<DialogueSearchMatch> {
    val needle = normalizeDialogueSearchText(query.trim())
    if (needle.isBlank()) return emptyList()
    val results = mutableListOf<DialogueSearchMatch>()
    turns.forEach { turn ->
        val normalized = normalizeDialogueSearchText(turn.content)
        var startAt = 0
        while (startAt <= normalized.length - needle.length) {
            val index = normalized.indexOf(needle, startAt)
            if (index < 0) break
            results += DialogueSearchMatch(turn.turnId, index, index + needle.length)
            startAt = index + needle.length.coerceAtLeast(1)
        }
    }
    return results
}

private fun normalizeDialogueSearchText(value: String): String = buildString(value.length) {
    value.forEach { character ->
        append(when (character) {
            'ي', 'ى' -> 'ی'
            'ك' -> 'ک'
            else -> character.lowercaseChar()
        })
    }
}

private fun buildDialogueHighlightedText(
    text: String,
    matches: List<DialogueSearchMatch>,
    activeMatch: DialogueSearchMatch?,
    normalHighlight: Color,
    activeHighlight: Color,
    activeTextColor: Color
): AnnotatedString = buildAnnotatedString {
    var cursor = 0
    matches.sortedBy { it.start }.forEach { match ->
        if (match.start < cursor || match.start !in text.indices || match.end > text.length) return@forEach
        append(text.substring(cursor, match.start))
        val active = activeMatch?.let { it.start == match.start && it.end == match.end } == true
        withStyle(SpanStyle(
            background = if (active) activeHighlight else normalHighlight,
            color = if (active) activeTextColor else Color.Unspecified,
            fontWeight = if (active) FontWeight.Bold else FontWeight.Normal
        )) { append(text.substring(match.start, match.end)) }
        cursor = match.end
    }
    if (cursor < text.length) append(text.substring(cursor))
}
