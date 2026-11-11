package com.example.bookapp.ui

import android.content.Context
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.room.withTransaction
import com.example.bookapp.data.AppDatabase
import com.example.bookapp.data.DialogueRepository
import com.example.bookapp.data.DialogueStageCueEntity
import com.example.bookapp.data.DialogueTurnEntity
import com.example.bookapp.data.DialogueTurnMetadataEntity
import com.example.bookapp.data.ViewerAccessPolicy
import com.example.bookapp.ui.screens.DialogueSummary
import com.example.bookapp.ui.screens.DialogueTurnDisplay
import com.example.bookapp.ui.screens.DialogueBuilderScreen
import com.example.bookapp.ui.screens.DialoguesScreen
import com.example.bookapp.ui.screens.DialogueReaderScreen
import com.example.bookapp.ui.screens.SectionPickerItem
import kotlinx.coroutines.launch

private const val ROUTE_DIALOGUES_SHARED = "dialogues/{taziehId}/{taziehTitle}"
private const val ROUTE_DIALOGUE_BUILDER_SHARED = "dialogue_builder/{taziehId}/{taziehTitle}"
private const val ROUTE_DIALOGUE_READER_SHARED = "dialogue_reader/{dialogueId}"

/**
 * Shared dialogue routes. Admin can manage all dialogues; Viewer can manage
 * only dialogues whose ownerInstallationId matches this installation.
 */
fun NavGraphBuilder.dialogueFeatureRoutes(
    navController: NavHostController,
    db: AppDatabase,
    context: Context,
    publicViewer: Boolean,
    featureEnabled: (String) -> Boolean
) {
    val isAdmin = !publicViewer
    val ownerId = if (publicViewer) ViewerAccessPolicy.installationId(context) else ""

    if (featureEnabled("dialogues.view")) {
        composable(ROUTE_DIALOGUES_SHARED) { entry ->
            val taziehId = entry.arguments?.getString("taziehId")?.toLongOrNull() ?: 0L
            val taziehTitle = entry.arguments?.getString("taziehTitle").orEmpty()
            var dialogues by remember(taziehId, ownerId) { mutableStateOf(emptyList<DialogueSummary>()) }
            var loadError by remember(taziehId) { mutableStateOf<String?>(null) }
            val scope = rememberCoroutineScope()

            suspend fun reload() {
                val rows = DialogueRepository.loadDialogues(db, taziehId, isAdmin, ownerId)
                dialogues = rows.map { row ->
                    DialogueSummary(
                        id = row.id,
                        title = row.title,
                        turnsCount = db.dialogueTurnDao().getByDialogue(row.id).size
                    )
                }
            }

            LaunchedEffect(taziehId, ownerId) {
                runCatching { reload() }.onFailure {
                    loadError = it.message ?: "بارگذاری گفتگوها انجام نشد."
                }
            }

            DialoguesScreen(
                taziehTitle = taziehTitle,
                dialogues = dialogues,
                onOpenDialogue = { dialogue ->
                    navController.navigate("dialogue_reader/${dialogue.id}")
                },
                onEditDialogue = { dialogue, newTitle ->
                    scope.launch {
                        runCatching {
                            if (!featureEnabled("dialogues.edit")) {
                                throw SecurityException("مجوز ویرایش گفتگو فعال نیست.")
                            }
                            DialogueRepository.renameDialogue(
                                db, dialogue.id, newTitle, isAdmin, ownerId
                            )
                            reload()
                        }.onFailure {
                            Toast.makeText(context, it.message ?: "ویرایش گفتگو انجام نشد.", Toast.LENGTH_LONG).show()
                        }
                    }
                },
                onDeleteDialogue = { dialogue ->
                    scope.launch {
                        runCatching {
                            if (!featureEnabled("dialogues.delete")) {
                                throw SecurityException("مجوز حذف گفتگو فعال نیست.")
                            }
                            DialogueRepository.deleteDialogue(db, dialogue.id, isAdmin, ownerId)
                            reload()
                        }.onFailure {
                            Toast.makeText(context, it.message ?: "حذف گفتگو انجام نشد.", Toast.LENGTH_LONG).show()
                        }
                    }
                },
                onCreateNew = {
                    if (featureEnabled("dialogues.create")) {
                        navController.navigate(
                            "dialogue_builder/$taziehId/${Uri.encode(taziehTitle)}"
                        )
                    }
                },
                onBack = { navController.popBackStack() },
                readOnly = false,
                canCreate = featureEnabled("dialogues.create"),
                canEdit = featureEnabled("dialogues.edit"),
                canDelete = featureEnabled("dialogues.delete")
            )

            loadError?.let { message ->
                AlertDialog(
                    onDismissRequest = { loadError = null },
                    title = { Text("خطا در بارگذاری گفتگوها") },
                    text = { Text(message) },
                    confirmButton = {
                        TextButton(onClick = {
                            loadError = null
                            scope.launch { runCatching { reload() }.onFailure { loadError = it.message } }
                        }) { Text("تلاش دوباره") }
                    },
                    dismissButton = {
                        TextButton(onClick = { loadError = null }) { Text("بستن") }
                    }
                )
            }
        }
    }

    if (featureEnabled("dialogues.create")) {
        composable(ROUTE_DIALOGUE_BUILDER_SHARED) { entry ->
            val taziehId = entry.arguments?.getString("taziehId")?.toLongOrNull() ?: 0L
            val taziehTitle = entry.arguments?.getString("taziehTitle").orEmpty()
            var sections by remember(taziehId) { mutableStateOf(emptyList<SectionPickerItem>()) }
            var loading by remember(taziehId) { mutableStateOf(true) }
            var error by remember(taziehId) { mutableStateOf<String?>(null) }
            val scope = rememberCoroutineScope()

            LaunchedEffect(taziehId) {
                loading = true
                runCatching { DialogueRepository.loadSectionSources(db, taziehId) }
                    .onSuccess { sections = it }
                    .onFailure { error = it.message ?: "بارگذاری نسخه‌های آرشیو انجام نشد." }
                loading = false
            }

            when {
                loading -> Text("در حال بارگذاری نسخه‌ها و نقش‌های تعزیه…")
                error != null -> Column(Modifier.padding(20.dp)) {
                    Text(error.orEmpty(), color = MaterialTheme.colorScheme.error)
                    TextButton(onClick = { navController.popBackStack() }) { Text("بازگشت") }
                }
                sections.isEmpty() -> Column(Modifier.padding(20.dp)) {
                    Text("برای این تعزیه نسخهٔ آرشیوی دارای بخش پیدا نشد.")
                    TextButton(onClick = { navController.popBackStack() }) { Text("بازگشت") }
                }
                else -> DialogueBuilderScreen(
                    allSections = sections,
                    onBack = { navController.popBackStack() },
                    onSave = { dialogueTitle, orderedSections ->
                        scope.launch {
                            runCatching {
                                if (!featureEnabled("dialogues.create")) {
                                    throw SecurityException("مجوز ساخت گفتگو فعال نیست.")
                                }
                                if (publicViewer && ownerId.isBlank()) {
                                    throw SecurityException("شناسه نصب برای ثبت گفتگوی شخصی در دسترس نیست.")
                                }
                                DialogueRepository.createDialogue(
                                    db = db,
                                    taziehId = taziehId,
                                    title = dialogueTitle,
                                    orderedSections = orderedSections,
                                    ownerInstallationId = ownerId
                                )
                            }.onSuccess {
                                // با ایجاد مقصد تازه، فهرست قبلی نیز دوباره از دیتابیس خوانده می‌شود.
                                navController.navigate(
                                    "dialogues/$taziehId/${Uri.encode(taziehTitle)}"
                                ) {
                                    popUpTo(ROUTE_DIALOGUE_BUILDER_SHARED) { inclusive = true }
                                    launchSingleTop = true
                                }
                            }.onFailure {
                                Toast.makeText(context, it.message ?: "ذخیره گفتگو انجام نشد.", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                )
            }
        }
    }

    if (featureEnabled("dialogues.view")) {
        composable(ROUTE_DIALOGUE_READER_SHARED) { entry ->
            val dialogueId = entry.arguments?.getString("dialogueId")?.toLongOrNull() ?: 0L
            var dialogueTitle by remember(dialogueId) { mutableStateOf("در حال بارگذاری گفتگو…") }
            var turns by remember(dialogueId) { mutableStateOf(emptyList<DialogueTurnDisplay>()) }
            var allSections by remember(dialogueId) { mutableStateOf(emptyList<SectionPickerItem>()) }
            var showAddTurn by remember(dialogueId) { mutableStateOf(false) }
            var canEdit by remember(dialogueId) { mutableStateOf(false) }
            var canPractice by remember(dialogueId) { mutableStateOf(false) }
            var loadError by remember(dialogueId) { mutableStateOf<String?>(null) }
            val scope = rememberCoroutineScope()

            suspend fun ensureTurnMetadata(turnId: Long, fallbackOrder: Int): DialogueTurnMetadataEntity {
                val dao = db.dialogueTurnMetadataDao()
                return dao.getByTurn(turnId) ?: DialogueTurnMetadataEntity(
                    turnId = turnId,
                    rehearsalOrderIndex = fallbackOrder
                ).also { dao.upsert(it) }
            }

            suspend fun reloadTurns() {
                val dialogue = DialogueRepository.requireAccessibleDialogue(
                    db, dialogueId, isAdmin, ownerId
                )
                dialogueTitle = dialogue.title
                canEdit = isAdmin || featureEnabled("dialogues.edit")
                canPractice = isAdmin || featureEnabled("dialogues.practice")

                val allowed = DialogueRepository.loadAllowedSections(db, dialogue)
                allSections = allowed
                val allowedRoleIds = allowed.map { it.roleId }.toSet()
                val sourceTurns = db.dialogueTurnDao().getByDialogue(dialogueId)
                val metadataById = sourceTurns.mapIndexed { index, source ->
                    source.id to ensureTurnMetadata(source.id, index)
                }.toMap()
                val orderedTurns = sourceTurns.sortedWith(
                    compareBy<DialogueTurnEntity> {
                        metadataById[it.id]?.rehearsalOrderIndex ?: it.orderIndex
                    }.thenBy { it.orderIndex }.thenBy { it.id }
                )

                turns = orderedTurns.mapNotNull { source ->
                    val section = runCatching { db.sectionDao().getById(source.sectionId) }.getOrNull()
                        ?: return@mapNotNull null
                    if (section.roleId !in allowedRoleIds) return@mapNotNull null
                    val role = runCatching { db.roleDao().getById(section.roleId) }.getOrNull()
                        ?: return@mapNotNull null
                    val metadata = metadataById[source.id]
                    val cues = db.dialogueStageCueDao().getByTurn(source.id)
                    val expectedRoleTitle = metadata?.expectedResponderRoleId
                        ?.takeIf { it in allowedRoleIds }
                        ?.let { roleId -> runCatching { db.roleDao().getById(roleId).title }.getOrNull() }

                    DialogueTurnDisplay(
                        turnId = source.id,
                        sectionId = section.id,
                        roleTitle = role.title,
                        sectionTitle = section.title,
                        content = section.content,
                        roleId = role.id,
                        sourceSequenceIndex = metadata?.sourceSequenceIndex,
                        sourceSequenceStatus = metadata?.sourceSequenceStatus
                            ?: DialogueTurnMetadataEntity.SOURCE_UNVERIFIED,
                        rehearsalOrderIndex = metadata?.rehearsalOrderIndex ?: source.orderIndex,
                        expectsResponse = metadata?.expectsResponse ?: false,
                        replyToTurnId = metadata?.replyToTurnId,
                        expectedResponderRoleId = metadata?.expectedResponderRoleId
                            ?.takeIf { it in allowedRoleIds },
                        expectedResponderRoleTitle = expectedRoleTitle,
                        sequenceEvidence = metadata?.sequenceEvidence.orEmpty(),
                        directorNote = metadata?.directorNote.orEmpty(),
                        stageCues = cues
                    )
                }
            }

            LaunchedEffect(dialogueId, ownerId) {
                runCatching { reloadTurns() }.onFailure {
                    loadError = it.message ?: "بارگذاری گفتگو انجام نشد."
                    dialogueTitle = "گفتگو در دسترس نیست"
                    turns = emptyList()
                    allSections = emptyList()
                }
            }

            DialogueReaderScreen(
                dialogueTitle = dialogueTitle,
                turns = turns,
                onBack = { navController.popBackStack() },
                readOnly = !canEdit,
                allowPractice = canPractice,
                showPdf = featureEnabled("pdf") && featureEnabled("pdf.create") && featureEnabled("pdf.save"),
                onExportPdf = {
                    if (featureEnabled("pdf") && featureEnabled("pdf.create") && featureEnabled("pdf.save")) {
                        scope.launch {
                            runCatching {
                                com.example.bookapp.data.exportDialogueToPdf(
                                    context,
                                    dialogueTitle,
                                    turns.map { Triple(it.roleTitle, it.sectionTitle, it.content) }
                                )
                            }.onFailure {
                                Toast.makeText(context, it.message ?: "خروجی PDF انجام نشد.", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                },
                onMoveTurn = { index, direction ->
                    if (canEdit) scope.launch {
                        runCatching {
                            db.withTransaction {
                                DialogueRepository.requireAccessibleDialogue(db, dialogueId, isAdmin, ownerId)
                                val current = turns
                                val targetIndex = index + direction
                                require(index in current.indices && targetIndex in current.indices)
                                val first = current[index]
                                val second = current[targetIndex]
                                val firstMeta = ensureTurnMetadata(first.turnId, first.rehearsalOrderIndex)
                                val secondMeta = ensureTurnMetadata(second.turnId, second.rehearsalOrderIndex)
                                val dao = db.dialogueTurnMetadataDao()
                                dao.updateRehearsalOrder(first.turnId, Int.MIN_VALUE)
                                dao.updateRehearsalOrder(second.turnId, firstMeta.rehearsalOrderIndex)
                                dao.updateRehearsalOrder(first.turnId, secondMeta.rehearsalOrderIndex)
                            }
                            reloadTurns()
                        }.onFailure {
                            Toast.makeText(context, it.message ?: "تغییر ترتیب انجام نشد.", Toast.LENGTH_LONG).show()
                        }
                    }
                },
                onDeleteTurn = { turn ->
                    if (canEdit) scope.launch {
                        runCatching {
                            db.withTransaction {
                                DialogueRepository.requireAccessibleDialogue(db, dialogueId, isAdmin, ownerId)
                                val existing = db.dialogueTurnDao().getByDialogue(dialogueId)
                                require(existing.any { it.id == turn.turnId }) { "این نوبت متعلق به گفتگو نیست." }
                                db.dialogueTurnDao().deleteTurn(turn.turnId)
                                db.dialogueTurnDao().getByDialogue(dialogueId).forEachIndexed { index, item ->
                                    ensureTurnMetadata(item.id, index)
                                    db.dialogueTurnMetadataDao().updateRehearsalOrder(item.id, index)
                                }
                            }
                            reloadTurns()
                        }.onFailure {
                            Toast.makeText(context, it.message ?: "حذف نوبت انجام نشد.", Toast.LENGTH_LONG).show()
                        }
                    }
                },
                onAddTurn = { if (canEdit) showAddTurn = true },
                onSaveResponseRelation = { turn, expectsResponse, replyToTurnId, expectedResponderRoleId ->
                    if (canEdit) scope.launch {
                        runCatching {
                            db.withTransaction {
                                val dialogue = DialogueRepository.requireAccessibleDialogue(db, dialogueId, isAdmin, ownerId)
                                val currentTurns = db.dialogueTurnDao().getByDialogue(dialogueId)
                                val currentIds = currentTurns.map { it.id }.toSet()
                                require(turn.turnId in currentIds) { "نوبت انتخاب‌شده متعلق به این گفتگو نیست." }
                                require(replyToTurnId == null || (replyToTurnId in currentIds && replyToTurnId != turn.turnId)) {
                                    "نوبت مرجع پاسخ معتبر نیست."
                                }
                                val selectedRoles = db.dialogueRoleSelectionDao().getByDialogue(dialogueId)
                                    .map { it.roleId }.toSet().ifEmpty {
                                        db.roleDao().getByVersionUid(dialogue.versionUid).map { it.id }.toSet()
                                    }
                                require(expectedResponderRoleId == null || expectedResponderRoleId in selectedRoles) {
                                    "نقش پاسخ‌دهنده در میان نقش‌های این گفتگو انتخاب نشده است."
                                }
                                var cursor = replyToTurnId
                                val visited = mutableSetOf<Long>()
                                while (cursor != null) {
                                    require(cursor != turn.turnId) { "این رابطه باعث ایجاد چرخه در زنجیره پاسخ‌ها می‌شود." }
                                    require(cursor in currentIds && visited.add(cursor)) {
                                        "زنجیره پاسخ به نوبت نامعتبر یا چرخه‌ای می‌رسد."
                                    }
                                    cursor = db.dialogueTurnMetadataDao().getByTurn(cursor)?.replyToTurnId
                                }
                                val old = ensureTurnMetadata(turn.turnId, turn.rehearsalOrderIndex)
                                db.dialogueTurnMetadataDao().upsert(
                                    old.copy(
                                        expectsResponse = expectsResponse,
                                        replyToTurnId = if (expectsResponse) replyToTurnId else null,
                                        expectedResponderRoleId = if (expectsResponse) expectedResponderRoleId else null
                                    )
                                )
                            }
                            reloadTurns()
                        }.onFailure {
                            Log.e("DialogueFeature", "ثبت رابطه پاسخ ناموفق بود", it)
                            Toast.makeText(context, it.message ?: "ثبت رابطه پاسخ انجام نشد.", Toast.LENGTH_LONG).show()
                        }
                    }
                },
                onAddStageCue = { turn, cueType, cueText ->
                    if (canEdit) scope.launch {
                        runCatching {
                            db.withTransaction {
                                DialogueRepository.requireAccessibleDialogue(db, dialogueId, isAdmin, ownerId)
                                val current = db.dialogueTurnDao().getByDialogue(dialogueId)
                                require(current.any { it.id == turn.turnId }) { "نوبت انتخاب‌شده متعلق به این گفتگو نیست." }
                                val existing = db.dialogueStageCueDao().getByTurn(turn.turnId)
                                db.dialogueStageCueDao().insert(
                                    DialogueStageCueEntity(
                                        turnId = turn.turnId,
                                        cueType = cueType,
                                        cueText = cueText.trim(),
                                        orderIndex = existing.size,
                                        origin = DialogueStageCueEntity.ORIGIN_DIRECTOR_ADDED,
                                        isVerified = false
                                    )
                                )
                            }
                            reloadTurns()
                        }.onFailure {
                            Toast.makeText(context, it.message ?: "ثبت نشانه اجرایی انجام نشد.", Toast.LENGTH_LONG).show()
                        }
                    }
                },
                onDeleteStageCue = { cue ->
                    if (canEdit && cue.origin == DialogueStageCueEntity.ORIGIN_DIRECTOR_ADDED) scope.launch {
                        runCatching {
                            db.withTransaction {
                                DialogueRepository.requireAccessibleDialogue(db, dialogueId, isAdmin, ownerId)
                                val turnIds = db.dialogueTurnDao().getByDialogue(dialogueId).map { it.id }.toSet()
                                require(cue.turnId in turnIds) { "این نشانه متعلق به گفتگو نیست." }
                                db.dialogueStageCueDao().delete(cue.id)
                            }
                            reloadTurns()
                        }.onFailure {
                            Toast.makeText(context, it.message ?: "حذف نشانه اجرایی انجام نشد.", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            )

            if (showAddTurn && canEdit) {
                var chosen by remember(dialogueId) { mutableStateOf<SectionPickerItem?>(null) }
                AlertDialog(
                    onDismissRequest = { showAddTurn = false },
                    title = { Text("افزودن نوبت به گفتگو") },
                    text = {
                        androidx.compose.foundation.lazy.LazyColumn(Modifier.heightIn(max = 420.dp)) {
                            items(allSections, key = { it.sectionId }) { item ->
                                ListItem(
                                    headlineContent = { Text(item.sectionTitle) },
                                    supportingContent = { Text("${item.roleTitle} · ${item.versionLabel}") },
                                    modifier = Modifier.fillMaxWidth().clickable { chosen = item }
                                )
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(enabled = chosen != null, onClick = {
                            val selected = chosen ?: return@TextButton
                            scope.launch {
                                runCatching {
                                    db.withTransaction {
                                        DialogueRepository.requireAccessibleDialogue(db, dialogueId, isAdmin, ownerId)
                                        val allowedFresh = DialogueRepository.loadAllowedSections(db, dialogue)
                                        require(allowedFresh.any {
                                            it.sectionId == selected.sectionId &&
                                                it.roleId == selected.roleId &&
                                                it.versionUid == selected.versionUid
                                        }) {
                                            "بخش انتخاب‌شده جزو نقش‌ها و نسخه‌های این گفتگو نیست."
                                        }
                                        val existing = db.dialogueTurnDao().getByDialogue(dialogueId)
                                        val sourceOrder = (existing.maxOfOrNull { it.orderIndex } ?: -1) + 1
                                        val newId = db.dialogueTurnDao().insert(
                                            DialogueTurnEntity(dialogueId = dialogueId, sectionId = selected.sectionId, orderIndex = sourceOrder)
                                        )
                                        require(newId > 0L) { "ثبت نوبت جدید انجام نشد." }
                                        val rehearsalOrder = (turns.maxOfOrNull { it.rehearsalOrderIndex } ?: -1) + 1
                                        db.dialogueTurnMetadataDao().upsert(
                                            DialogueTurnMetadataEntity(turnId = newId, rehearsalOrderIndex = rehearsalOrder)
                                        )
                                    }
                                    reloadTurns()
                                    showAddTurn = false
                                }.onFailure {
                                    Toast.makeText(context, it.message ?: "افزودن نوبت انجام نشد.", Toast.LENGTH_LONG).show()
                                }
                            }
                        }) { Text("افزودن") }
                    },
                    dismissButton = {
                        TextButton(onClick = { showAddTurn = false }) { Text("انصراف") }
                    }
                )
            }

            loadError?.let { message ->
                AlertDialog(
                    onDismissRequest = { loadError = null },
                    title = { Text("دسترسی به گفتگو ممکن نیست") },
                    text = { Text(message) },
                    confirmButton = {
                        TextButton(onClick = { loadError = null; navController.popBackStack() }) { Text("بازگشت") }
                    }
                )
            }
        }
    }
}
