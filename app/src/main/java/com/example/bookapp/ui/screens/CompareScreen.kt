package com.example.bookapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.bookapp.data.*

data class CompareSectionItem(val section: SectionEntity, val roleTitle: String)

@Composable
private fun <T> CompareDropdown(label: String, selected: T?, items: List<T>, itemLabel: (T) -> String, onSelect: (T) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.labelMedium)
        Box {
            OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) { Text(selected?.let(itemLabel) ?: "انتخاب کنید", modifier = Modifier.fillMaxWidth()) }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                items.forEach { item -> DropdownMenuItem(text = { Text(itemLabel(item)) }, onClick = { onSelect(item); expanded = false }) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompareScreen(taziehTitle: String, roles: List<RoleEntity>, sections: List<CompareSectionItem>, roleSections: Map<Long, List<SectionEntity>>, onBack: () -> Unit) {
    var mode by remember { mutableStateOf("role") }
    var view by remember { mutableStateOf("diff") }
    var granularity by remember { mutableStateOf("line") }
    var roleA by remember(roles) { mutableStateOf(roles.getOrNull(0)) }
    var roleB by remember(roles) { mutableStateOf(roles.getOrNull(1)) }
    var sectionA by remember(sections) { mutableStateOf(sections.getOrNull(0)) }
    var sectionB by remember(sections) { mutableStateOf(sections.getOrNull(1)) }
    var compared by remember { mutableStateOf(false) }
    val leftText = if (mode == "role") roleA?.let { roleSections[it.id].orEmpty().joinToString("\n\n") { s -> "【${s.title}】\n${s.content}" } }.orEmpty() else sectionA?.section?.content.orEmpty()
    val rightText = if (mode == "role") roleB?.let { roleSections[it.id].orEmpty().joinToString("\n\n") { s -> "【${s.title}】\n${s.content}" } }.orEmpty() else sectionB?.section?.content.orEmpty()
    val diff = remember(leftText, rightText) { TaziehTextDiff.compare(leftText, rightText) }

    Scaffold(topBar = { TopAppBar(title = { Text("مقایسه — $taziehTitle") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "بازگشت") } }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            if (!compared) {
                Text("نوع مقایسه", style = MaterialTheme.typography.titleSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(mode == "role", { mode = "role"; compared = false }, label = { Text("مقایسه نقش‌ها") })
                    FilterChip(mode == "section", { mode = "section"; compared = false }, label = { Text("مقایسه بخش‌ها") })
                }
                Spacer(Modifier.height(12.dp))
                if (mode == "role") {
                    CompareDropdown("نقش اول", roleA, roles, { it.title }) { roleA = it; compared = false }
                    Spacer(Modifier.height(8.dp)); CompareDropdown("نقش دوم", roleB, roles, { it.title }) { roleB = it; compared = false }
                } else {
                    CompareDropdown("بخش اول", sectionA, sections, { "${it.roleTitle} ← ${it.section.title}" }) { sectionA = it; compared = false }
                    Spacer(Modifier.height(8.dp)); CompareDropdown("بخش دوم", sectionB, sections, { "${it.roleTitle} ← ${it.section.title}" }) { sectionB = it; compared = false }
                }
                Button(onClick = { compared = true }, enabled = if (mode == "role") roleA != null && roleB != null && roleA?.id != roleB?.id else sectionA != null && sectionB != null && sectionA?.section?.id != sectionB?.section?.id, modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) { Text("نمایش مقایسه") }
                Text("انتخاب‌ها مستقل هستند؛ می‌توانید هر دو نقش یا هر دو بخش را خودتان تعیین کنید.", style = MaterialTheme.typography.bodySmall)
            } else {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    TextButton(onClick = { compared = false }) { Text("تغییر انتخاب") }
                    Row {
                        FilterChip(view == "diff", { view = "diff" }, label = { Text("اختلاف‌ها") })
                        Spacer(Modifier.width(6.dp)); FilterChip(view == "side", { view = "side" }, label = { Text("دو متن") })
                    }
                }
                Text("یکسان: ${diff.same}  |  افزوده: ${diff.added}  |  حذف‌شده: ${diff.removed}", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(vertical = 8.dp))
                if (view == "diff") {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        FilterChip(granularity == "line", { granularity = "line" }, label = { Text("اختلاف سطری") })
                        FilterChip(granularity == "word", { granularity = "word" }, label = { Text("اختلاف واژه‌ای") })
                    }
                    if (granularity == "line") {
                        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())) {
                            diff.lines.forEach { line ->
                                val label = when (line.kind) { DiffKind.SAME -> "="; DiffKind.ADDED -> "+"; DiffKind.REMOVED -> "−" }
                                val color = when (line.kind) { DiffKind.SAME -> MaterialTheme.colorScheme.surface; DiffKind.ADDED -> MaterialTheme.colorScheme.tertiaryContainer; DiffKind.REMOVED -> MaterialTheme.colorScheme.errorContainer }
                                Row(Modifier.fillMaxWidth().background(color).padding(horizontal = 8.dp, vertical = 5.dp)) {
                                    Text(label, style = MaterialTheme.typography.labelLarge, modifier = Modifier.width(24.dp))
                                    Text(line.text.ifBlank { " " }, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    } else {
                        val wordDiff = remember(leftText, rightText) { TaziehWordDiff.compare(leftText, rightText) }
                        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(8.dp)) {
                            wordDiff.forEach { token ->
                                val color = when (token.change) { WordChange.SAME -> MaterialTheme.colorScheme.onSurface; WordChange.ADDED -> MaterialTheme.colorScheme.primary; WordChange.REMOVED -> MaterialTheme.colorScheme.error }
                                Text(token.value, color = color, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                    Text("راهنما: + افزوده‌شده در متن دوم، − حذف‌شده از متن اول، = یکسان", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 6.dp))
                } else {
                    Row(Modifier.weight(1f).fillMaxWidth()) {
                        Column(Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()).padding(6.dp)) { Text(if (mode == "role") roleA?.title.orEmpty() else sectionA?.section?.title.orEmpty(), style = MaterialTheme.typography.titleSmall); Text(leftText) }
                        Box(Modifier.width(1.dp).fillMaxHeight().background(MaterialTheme.colorScheme.outlineVariant))
                        Column(Modifier.weight(1f).fillMaxHeight().background(MaterialTheme.colorScheme.surfaceVariant).verticalScroll(rememberScrollState()).padding(6.dp)) { Text(if (mode == "role") roleB?.title.orEmpty() else sectionB?.section?.title.orEmpty(), style = MaterialTheme.typography.titleSmall); Text(rightText) }
                    }
                }
            }
        }
    }
}
