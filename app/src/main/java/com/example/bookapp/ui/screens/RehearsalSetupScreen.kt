package com.example.bookapp.ui.screens

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.bookapp.data.RoleEntity
import com.example.bookapp.data.SectionEntity

@Immutable
data class RehearsalRoleOption(
    val role: RoleEntity,
    val sections: List<SectionEntity>
)

@Immutable
data class RehearsalTaziehOption(
    val id: Long,
    val title: String,
    val fieldTitle: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RehearsalSetupScreen(
    taziehs: List<RehearsalTaziehOption>,
    selectedTaziehId: Long?,
    roles: List<RehearsalRoleOption>,
    rolesLoading: Boolean,
    onTaziehSelected: (Long) -> Unit,
    onBack: () -> Unit,
    onStart: (
        taziehId: Long,
        primaryRoleId: Long,
        primaryRoleTitle: String,
        selectedSectionIds: List<Long>,
        multiRole: Boolean
    ) -> Unit
) {
    val context = LocalContext.current
    val prefs = remember(context) {
        context.getSharedPreferences("rehearsal_progress", Context.MODE_PRIVATE)
    }

    var showTaziehPicker by remember { mutableStateOf(false) }
    var taziehQuery by remember { mutableStateOf("") }
    var roleQuery by remember(selectedTaziehId) { mutableStateOf("") }

    val selectedTazieh = taziehs.firstOrNull { it.id == selectedTaziehId }
    val visibleRoles = roles.filter { it.role.title.contains(roleQuery.trim(), ignoreCase = true) }
    val selectedSectionIds = remember(selectedTaziehId) { mutableStateListOf<Long>() }
    val expandedRoleIds = remember(selectedTaziehId) { mutableStateListOf<Long>() }
    val startLines: SnapshotStateMap<Long, Int> = remember(selectedTaziehId) { mutableStateMapOf() }
    var primaryRoleId by remember(selectedTaziehId) { mutableStateOf<Long?>(null) }
    var linePickerSection by remember { mutableStateOf<SectionEntity?>(null) }

    val selectedRoles = roles.filter { option ->
        option.sections.any { it.id in selectedSectionIds }
    }
    val selectedRoleCount = selectedRoles.size
    val selectedCount = selectedSectionIds.size

    fun currentStartLine(section: SectionEntity): Int {
        val lineCount = section.content.lines().count { it.isNotBlank() }.coerceAtLeast(1)
        return (startLines[section.id] ?: prefs.getInt("start_line_${section.id}", 1))
            .coerceIn(1, lineCount)
    }

    fun toggleSection(roleId: Long, section: SectionEntity, checked: Boolean) {
        if (checked) {
            if (section.id !in selectedSectionIds) selectedSectionIds.add(section.id)
            val primaryStillSelected = primaryRoleId?.let { currentPrimary ->
                roles.firstOrNull { it.role.id == currentPrimary }
                    ?.sections?.any { it.id in selectedSectionIds } == true
            } ?: false
            if (!primaryStillSelected) primaryRoleId = roleId
        } else {
            selectedSectionIds.remove(section.id)
            val primaryStillSelected = roles.firstOrNull { it.role.id == roleId }
                ?.sections?.any { it.id in selectedSectionIds } == true
            if (primaryRoleId == roleId && !primaryStillSelected) {
                primaryRoleId = roles.firstOrNull { option ->
                    option.sections.any { it.id in selectedSectionIds }
                }?.role?.id
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("مرور و حفظ متن", fontWeight = FontWeight.Bold) },
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
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Text(
                        when {
                            selectedCount == 0 -> "هنوز بخشی انتخاب نشده است"
                            selectedRoleCount > 1 -> "$selectedCount بخش از $selectedRoleCount نقش · تمرین چندنقشی"
                            else -> "$selectedCount بخش انتخاب شده · تمرین انفرادی"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(6.dp))
                    Button(
                        onClick = {
                            val tazieh = selectedTazieh ?: return@Button
                            val chosenRoleIds = selectedRoles.map { it.role.id }
                            val primaryId = primaryRoleId?.takeIf { it in chosenRoleIds }
                                ?: chosenRoleIds.firstOrNull()
                                ?: return@Button
                            val primaryTitle = roles.firstOrNull { it.role.id == primaryId }
                                ?.role?.title.orEmpty()

                            selectedSectionIds.forEach { sectionId ->
                                val section = roles.asSequence()
                                    .flatMap { it.sections.asSequence() }
                                    .firstOrNull { it.id == sectionId }
                                if (section != null) {
                                    val lineCount = section.content.lines()
                                        .count { it.isNotBlank() }.coerceAtLeast(1)
                                    val chosenLine = currentStartLine(section).coerceIn(1, lineCount)
                                    prefs.edit().putInt("start_line_$sectionId", chosenLine).apply()
                                }
                            }
                            onStart(
                                tazieh.id,
                                primaryId,
                                primaryTitle,
                                selectedSectionIds.toList(),
                                selectedRoleCount > 1
                            )
                        },
                        enabled = selectedTazieh != null && selectedCount > 0 && selectedRoleCount > 0,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (selectedRoleCount > 1) "شروع تمرین چندنقشی" else "شروع تمرین انفرادی")
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Card(shape = RoundedCornerShape(16.dp)) {
                    Column(Modifier.fillMaxWidth().padding(14.dp)) {
                        Text("انتخاب تعزیه", fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(7.dp))
                        OutlinedButton(
                            onClick = { showTaziehPicker = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                selectedTazieh?.let {
                                    if (it.fieldTitle.isBlank()) it.title else "${it.fieldTitle}  ـ  ${it.title}"
                                } ?: "از میان همه تعزیه‌ها انتخاب کنید",
                                modifier = Modifier.weight(1f)
                            )
                            Icon(Icons.Filled.ExpandMore, contentDescription = null)
                        }
                        Text(
                            "ابتدا تعزیه را انتخاب کنید؛ سپس نقش و بخش‌های موردنظر را برگزینید.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (selectedTazieh != null) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("نقش‌ها و بخش‌ها", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.weight(1f))
                        Text("${roles.size} نقش", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(
                        "برای تمرین انفرادی یک نقش و برای تمرین چندنقشی دو یا چند نقش را انتخاب کنید. اولین نقش انتخاب‌شده، نقش اصلی است؛ در صورت نیاز می‌توانید آن را تغییر دهید.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (rolesLoading) {
                    item {
                        Row(Modifier.fillMaxWidth().padding(18.dp), horizontalArrangement = Arrangement.Center) {
                            CircularProgressIndicator()
                        }
                    }
                } else if (roles.isEmpty()) {
                    item {
                        Text("برای این تعزیه هنوز نقشی ثبت نشده است.", modifier = Modifier.padding(12.dp))
                    }
                } else {
                    if (roles.size > 8) {
                        item {
                            OutlinedTextField(
                                value = roleQuery,
                                onValueChange = { roleQuery = it },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                label = { Text("جستجوی نقش") }
                            )
                        }
                    }
                    if (visibleRoles.isEmpty()) {
                        item { Text("نقشی با این عنوان پیدا نشد.", modifier = Modifier.padding(12.dp)) }
                    }
                    items(visibleRoles, key = { it.role.id }) { option ->
                    val roleId = option.role.id
                    val selectedInRole = option.sections.count { it.id in selectedSectionIds }
                    val roleIsSelected = selectedInRole > 0
                    val expanded = roleId in expandedRoleIds
                    val isPrimary = primaryRoleId == roleId && roleIsSelected

                    Card(shape = RoundedCornerShape(14.dp)) {
                        Column(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 7.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(
                                    Modifier.weight(1f).clickable {
                                        if (expanded) expandedRoleIds.remove(roleId)
                                        else expandedRoleIds.add(roleId)
                                    }.padding(vertical = 5.dp)
                                ) {
                                    Text(option.role.title, fontWeight = FontWeight.SemiBold)
                                    Text(
                                        if (option.sections.isEmpty()) "بخشی برای تمرین ندارد"
                                        else "$selectedInRole از ${option.sections.size} بخش انتخاب شده",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                if (roleIsSelected) {
                                    if (isPrimary) {
                                        AssistChip(onClick = {}, label = { Text("نقش اصلی") }, enabled = false)
                                    } else {
                                        TextButton(onClick = { primaryRoleId = roleId }) {
                                            Text("نقش اصلی")
                                        }
                                    }
                                }
                                IconButton(onClick = {
                                    if (expanded) expandedRoleIds.remove(roleId)
                                    else expandedRoleIds.add(roleId)
                                }) {
                                    Icon(
                                        if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                                        contentDescription = if (expanded) "بستن بخش‌ها" else "نمایش بخش‌ها"
                                    )
                                }
                            }

                            if (expanded) {
                                if (option.sections.isEmpty()) {
                                    Text("متنی برای این نقش ثبت نشده است.", style = MaterialTheme.typography.bodySmall)
                                } else {
                                    option.sections.forEach { section ->
                                        Divider()
                                        val checked = section.id in selectedSectionIds
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Checkbox(
                                                checked = checked,
                                                onCheckedChange = { toggleSection(roleId, section, it) }
                                            )
                                            Text(
                                                section.title,
                                                modifier = Modifier.weight(1f).clickable {
                                                    toggleSection(roleId, section, !checked)
                                                },
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                            TextButton(
                                                enabled = checked,
                                                onClick = { linePickerSection = section }
                                            ) {
                                                Text("خط ${currentStartLine(section)}")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    }
                }
            }
        }
    }

    if (showTaziehPicker) {
        val filteredTaziehs = remember(taziehs, taziehQuery) {
            taziehs.filter {
                it.title.contains(taziehQuery.trim(), ignoreCase = true) ||
                    it.fieldTitle.contains(taziehQuery.trim(), ignoreCase = true)
            }
        }
        AlertDialog(
            onDismissRequest = { showTaziehPicker = false },
            title = { Text("انتخاب تعزیه") },
            text = {
                Column {
                    OutlinedTextField(
                        value = taziehQuery,
                        onValueChange = { taziehQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text("جستجوی تعزیه یا زمینه") }
                    )
                    Spacer(Modifier.height(8.dp))
                    LazyColumn(Modifier.heightIn(max = 390.dp)) {
                        items(filteredTaziehs, key = { it.id }) { item ->
                            ListItem(
                                headlineContent = { Text(item.title) },
                                supportingContent = { if (item.fieldTitle.isNotBlank()) Text(item.fieldTitle) },
                                modifier = Modifier.fillMaxWidth().clickable {
                                    onTaziehSelected(item.id)
                                    showTaziehPicker = false
                                    taziehQuery = ""
                                }
                            )
                        }
                        if (taziehs.isEmpty()) {
                            item {
                                Row(Modifier.fillMaxWidth().padding(18.dp), horizontalArrangement = Arrangement.Center) {
                                    CircularProgressIndicator()
                                }
                            }
                        } else if (filteredTaziehs.isEmpty()) {
                            item { Text("تعزیه‌ای با این عنوان پیدا نشد.", modifier = Modifier.padding(12.dp)) }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showTaziehPicker = false }) { Text("بستن") }
            }
        )
    }

    linePickerSection?.let { section ->
        val lines = remember(section.id, section.content) {
            section.content.lines().filter { it.isNotBlank() }
        }
        val savedLine = currentStartLine(section).coerceIn(1, lines.size.coerceAtLeast(1))
        AlertDialog(
            onDismissRequest = { linePickerSection = null },
            title = { Text("انتخاب خط شروع") },
            text = {
                Column {
                    Text(
                        "خط انتخاب‌شده اولین خطی است که باید تمرین شود؛ خط‌های قبل از آن آشکار خواهند بود.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    if (lines.isEmpty()) {
                        Text("متنی برای این بخش ثبت نشده است.")
                    } else {
                        LazyColumn(Modifier.heightIn(max = 390.dp)) {
                            items(lines.size) { index ->
                                Row(
                                    Modifier.fillMaxWidth().clickable {
                                        val selectedLine = index + 1
                                        startLines[section.id] = selectedLine
                                        prefs.edit().putInt("start_line_${section.id}", selectedLine).apply()
                                        linePickerSection = null
                                    }.padding(vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = savedLine == index + 1,
                                        onClick = {
                                            val selectedLine = index + 1
                                            startLines[section.id] = selectedLine
                                            prefs.edit().putInt("start_line_${section.id}", selectedLine).apply()
                                            linePickerSection = null
                                        }
                                    )
                                    Text("${index + 1}. ${lines[index]}", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    startLines[section.id] = 1
                    prefs.edit().putInt("start_line_${section.id}", 1).apply()
                    linePickerSection = null
                }) { Text("شروع از ابتدا") }
            },
            dismissButton = {
                TextButton(onClick = { linePickerSection = null }) { Text("انصراف") }
            }
        )
    }
}
