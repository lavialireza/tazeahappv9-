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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RehearsalSetupScreen(
    taziehTitle: String,
    ownRoleId: Long,
    roles: List<RehearsalRoleOption>,
    onBack: () -> Unit,
    onStart: (selectedSectionIds: List<Long>, multiRole: Boolean) -> Unit
) {
    val context = LocalContext.current
    val prefs = remember(context) {
        context.getSharedPreferences("rehearsal_progress", Context.MODE_PRIVATE)
    }
    val selectedRoleIds = remember(ownRoleId) { mutableStateListOf(ownRoleId) }
    val selectedSectionIds = remember(roles, ownRoleId) {
        mutableStateListOf<Long>().apply {
            roles.firstOrNull { it.role.id == ownRoleId }?.sections?.forEach { add(it.id) }
        }
    }
    val expandedRoles = remember { mutableStateListOf<Long>(ownRoleId) }
    val expandedSections = remember { mutableStateListOf<Long>() }
    val startLines: SnapshotStateMap<Long, Int> = remember { mutableStateMapOf() }
    var multiRole by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("تنظیم تمرین") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "بازگشت")
                    }
                }
            )
        },
        bottomBar = {
            Surface(shadowElevation = 6.dp) {
                Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(12.dp)) {
                    Text(
                        "${selectedSectionIds.size} بخش انتخاب شده",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(6.dp))
                    Button(
                        onClick = {
                            selectedSectionIds.forEach { id ->
                                val section = roles.asSequence().flatMap { it.sections.asSequence() }.firstOrNull { it.id == id }
                                if (section != null) {
                                    val lineCount = section.content.lines().count { it.isNotBlank() }.coerceAtLeast(1)
                                    val chosen = startLines[id] ?: prefs.getInt("start_line_$id", 1)
                                    prefs.edit().putInt("start_line_$id", chosen.coerceIn(1, lineCount)).apply()
                                }
                            }
                            onStart(selectedSectionIds.toList(), multiRole)
                        },
                        enabled = selectedSectionIds.isNotEmpty(),
                        modifier = Modifier.fillMaxWidth()
                    ) { Text(if (multiRole) "شروع تمرین چندنقشی" else "شروع تمرین نقش من") }
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
                Text(taziehTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text("ابتدا نقش‌ها و بخش‌های موردنظر را انتخاب کنید. نقطه شروع هر بخش مستقل ذخیره می‌شود.", style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(10.dp))
                Card(shape = RoundedCornerShape(14.dp)) {
                    Column(Modifier.padding(12.dp)) {
                        Text("نوع تمرین", fontWeight = FontWeight.Bold)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            FilterChip(selected = !multiRole, onClick = { multiRole = false }, label = { Text("تمرین انفرادی") })
                            Spacer(Modifier.width(8.dp))
                            FilterChip(selected = multiRole, onClick = { multiRole = true }, label = { Text("تمرین چندنقشی") })
                        }
                        Text(
                            if (multiRole) "متن نقش‌های دیگر کامل دیده می‌شود و متن نقش من خط‌به‌خط آشکار خواهد شد."
                            else "فقط بخش‌های انتخاب‌شده از نقش خودتان تمرین می‌شود.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            items(roles, key = { it.role.id }) { option ->
                val roleId = option.role.id
                val roleSelected = roleId in selectedRoleIds
                Card(shape = RoundedCornerShape(14.dp)) {
                    Column(Modifier.padding(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = roleSelected,
                                onCheckedChange = { checked ->
                                    if (roleId == ownRoleId) return@Checkbox
                                    if (checked) {
                                        if (roleId !in selectedRoleIds) selectedRoleIds.add(roleId)
                                        if (roleId !in expandedRoles) expandedRoles.add(roleId)
                                    } else {
                                        selectedRoleIds.remove(roleId)
                                        option.sections.forEach { selectedSectionIds.remove(it.id) }
                                    }
                                },
                                enabled = roleId != ownRoleId
                            )
                            Column(Modifier.weight(1f).clickable {
                                if (roleId in expandedRoles) expandedRoles.remove(roleId) else expandedRoles.add(roleId)
                            }) {
                                Text(option.role.title, fontWeight = FontWeight.Bold)
                                Text(
                                    if (roleId == ownRoleId) "نقش من" else "${option.sections.size} بخش",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = {
                                if (roleId in expandedRoles) expandedRoles.remove(roleId) else expandedRoles.add(roleId)
                            }) {
                                Icon(if (roleId in expandedRoles) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore, contentDescription = null)
                            }
                        }
                        if (roleId in expandedRoles) {
                            option.sections.forEach { section ->
                                Divider()
                                val chosen = section.id in selectedSectionIds
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(
                                        checked = chosen,
                                        onCheckedChange = { checked ->
                                            if (checked) {
                                                if (roleId !in selectedRoleIds) selectedRoleIds.add(roleId)
                                                if (section.id !in selectedSectionIds) selectedSectionIds.add(section.id)
                                            } else selectedSectionIds.remove(section.id)
                                        }
                                    )
                                    Text(section.title, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                                    TextButton(onClick = {
                                        if (section.id in expandedSections) expandedSections.remove(section.id)
                                        else expandedSections.add(section.id)
                                    }) { Text(if (section.id in expandedSections) "بستن متن" else "انتخاب خط شروع") }
                                }
                                if (section.id in expandedSections) {
                                    val lines = remember(section.id, section.content) { section.content.lines().filter { it.isNotBlank() } }
                                    val saved = startLines[section.id] ?: prefs.getInt("start_line_${section.id}", 1).coerceIn(1, lines.size.coerceAtLeast(1))
                                    Text("خط انتخابی، اولین خطی است که باید دوباره تمرین شود؛ خطوط قبل از آن آشکار می‌مانند.", style = MaterialTheme.typography.labelSmall)
                                    lines.forEachIndexed { index, line ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth().clickable { startLines[section.id] = index + 1 }.padding(vertical = 3.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            RadioButton(selected = saved == index + 1, onClick = { startLines[section.id] = index + 1 })
                                            Text("${index + 1}. $line", style = MaterialTheme.typography.bodySmall)
                                        }
                                    }
                                    TextButton(onClick = { startLines[section.id] = 1 }) { Text("شروع این بخش از ابتدا") }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
