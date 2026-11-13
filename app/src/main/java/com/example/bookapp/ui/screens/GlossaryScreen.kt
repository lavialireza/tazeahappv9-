package com.example.bookapp.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.bookapp.data.*

private data class GlossaryPlace(
    val sectionId: Long,
    val field: String,
    val tazieh: String,
    val role: String,
    val section: String,
    val quote: String,
    val explicit: Boolean
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlossaryScreen(
    onBack: () -> Unit,
    db: AppDatabase,
    termUid: String? = null,
    onOpenSection: (Long) -> Unit,
    canAdd: Boolean = false,
    canEdit: Boolean = false,
    canDelete: Boolean = false
) {
    var query by remember { mutableStateOf("") }
    var terms by remember { mutableStateOf(emptyList<GlossaryTermEntity>()) }
    var spellings by remember { mutableStateOf(emptyList<GlossarySpellingEntity>()) }
    var meanings by remember { mutableStateOf(emptyList<GlossaryMeaningEntity>()) }
    var selectedUid by remember(termUid) { mutableStateOf(termUid) }
    var places by remember(selectedUid) { mutableStateOf(emptyList<GlossaryPlace>()) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(db) {
        loading = true
        runCatching {
            terms = db.glossaryDao().allTerms().filter { it.reviewStatus == GlossaryTermEntity.STATUS_VERIFIED }
            spellings = db.glossaryDao().allSpellings()
            meanings = db.glossaryDao().allMeanings().filter { it.reviewStatus == GlossaryTermEntity.STATUS_VERIFIED }
        }
        loading = false
    }

    val selected = terms.firstOrNull { it.uid == selectedUid }
    LaunchedEffect(selected?.id, terms, meanings, spellings) {
        val term = selected ?: run { places = emptyList(); return@LaunchedEffect }
        val output = mutableListOf<GlossaryPlace>()
        val allFields = db.fieldDao().getAll().associateBy { it.id }
        val allTaziehs = db.taziehDao().getAll().associateBy { it.id }
        val allRoles = db.roleDao().getAllForSync().associateBy { it.id }
        val allSections = db.sectionDao().getAll()
        val linkedMeanings = meanings.filter { it.termId == term.id }
        val explicitSectionUids = linkedMeanings.mapNotNull { it.usageSectionUid }.toSet()
        val explicitRows = allSections.filter { it.uid in explicitSectionUids }
        val explicitIds = explicitRows.map { it.id }.toSet()
        allSections.forEach { section ->
            val role = allRoles[section.roleId] ?: return@forEach
            val tazieh = allTaziehs[role.taziehId] ?: return@forEach
            val field = allFields[tazieh.fieldId]
            val isExplicit = section.id in explicitIds
            val automaticMatch = GlossaryTextLinker.link(section.content, terms, spellings, meanings)
                .firstOrNull { it.termUid == term.uid }
            if (isExplicit || automaticMatch != null) {
                val quote = linkedMeanings.firstOrNull { it.usageSectionUid == section.uid }?.usageQuote
                    ?.takeIf { it.isNotBlank() }
                    ?: automaticMatch?.let { occurrence ->
                        val from = (occurrence.start - 45).coerceAtLeast(0)
                        val to = (occurrence.endExclusive + 65).coerceAtMost(section.content.length)
                        section.content.substring(from, to)
                    }.orEmpty()
                output += GlossaryPlace(section.id, field?.title.orEmpty(), tazieh.title, role.title, section.title, quote, isExplicit)
            }
        }
        places = output.distinctBy { it.sectionId }.sortedWith(compareBy({ it.field }, { it.tazieh }, { it.role }, { it.section }))
    }

    val filtered = remember(query, terms) {
        val q = normalizeSearchText(query)
        if (q.isBlank()) terms else terms.filter { normalizeSearchText(it.headword).contains(q) || normalizeSearchText(it.notes).contains(q) }
    }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text(if (selected == null) "فرهنگ اصطلاحات تعزیه" else selected.headword) },
            navigationIcon = {
                IconButton(onClick = { if (selectedUid != null) selectedUid = null else onBack() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "بازگشت")
                }
            }
        )
    }) { padding ->
        if (loading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = androidx.compose.ui.Alignment.Center) { CircularProgressIndicator() }
        } else if (selected == null) {
            Column(Modifier.fillMaxSize().padding(padding)) {
                OutlinedTextField(
                    value = query, onValueChange = { query = it },
                    label = { Text("جستجوی واژه") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(16.dp)
                )
                if (filtered.isEmpty()) Text("واژه تأییدشده‌ای پیدا نشد.", modifier = Modifier.padding(16.dp))
                LazyColumn(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(filtered, key = { it.uid }) { item ->
                        Card(Modifier.fillMaxWidth().clickable { selectedUid = item.uid }) {
                            Column(Modifier.padding(14.dp)) {
                                Text(item.headword, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                if (item.partOfSpeech.isNotBlank()) Text(item.partOfSpeech, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                                if (item.notes.isNotBlank()) Text(item.notes, style = MaterialTheme.typography.bodyMedium)
                                val count = meanings.count { it.termId == item.id }
                                Text("$count معنی تأییدشده · مشاهده محل‌های کاربرد", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        } else {
            LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item {
                    Text(selected.headword, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    if (selected.partOfSpeech.isNotBlank()) Text("نوع واژه: ${selected.partOfSpeech}")
                    if (selected.pronunciation.isNotBlank()) Text("تلفظ: ${selected.pronunciation}")
                    if (selected.etymology.isNotBlank()) Text("ریشه‌شناسی: ${selected.etymology}")
                    if (selected.notes.isNotBlank()) Text(selected.notes)
                    val variants = spellings.filter { it.termId == selected.id }
                    if (variants.isNotEmpty()) Text("صورت‌های دیگر: ${variants.joinToString("، ") { it.spelling }}")
                }
                item { Text("معانی و منابع", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
                val selectedMeanings = meanings.filter { it.termId == selected.id }
                if (selectedMeanings.isEmpty()) item { Text("برای این اصطلاح هنوز معنی تأییدشده‌ای ثبت نشده است.") }
                items(selectedMeanings, key = { "meaning-${it.id}" }) { meaning ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(meaning.meaning)
                            val source = listOf(meaning.sourceTitle, meaning.sourceAuthor, meaning.sourcePage).filter { it.isNotBlank() }.joinToString(" · ")
                            if (source.isNotBlank()) Text("منبع: $source", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                            if (meaning.usageQuote.isNotBlank()) Text("شاهد کاربرد: ${meaning.usageQuote}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                item { Text("محل‌های استفاده در متن تعزیه", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
                if (places.isEmpty()) item { Text("در متن‌های موجود، محل کاربردی برای این اصطلاح پیدا نشد.") }
                items(places, key = { "place-${it.sectionId}" }) { place ->
                    Card(Modifier.fillMaxWidth().clickable { onOpenSection(place.sectionId) }) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(place.section, fontWeight = FontWeight.Bold)
                                AssistChip(onClick = { onOpenSection(place.sectionId) }, label = { Text(if (place.explicit) "ارجاع ثبت‌شده" else "یافت‌شده در متن") })
                            }
                            Text(listOf(place.field, place.tazieh, place.role).filter { it.isNotBlank() }.joinToString(" ← "), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                            if (place.quote.isNotBlank()) Text(place.quote, style = MaterialTheme.typography.bodySmall, maxLines = 3)
                            Text("بازکردن بخش متن", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }
    }
}
