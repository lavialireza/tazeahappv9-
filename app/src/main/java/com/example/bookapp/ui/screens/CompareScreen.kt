package com.example.bookapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bookapp.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class CompareSectionItem(
    val section: SectionEntity,
    val roleTitle: String
)

@Composable
private fun <T> CompareDropdown(
    label: String,
    selected: T?,
    items: List<T>,
    itemLabel: (T) -> String,
    onSelect: (T) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.labelMedium)

        Box {
            OutlinedButton(
                onClick = { expanded = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    selected?.let(itemLabel) ?: "انتخاب کنید",
                    modifier = Modifier.fillMaxWidth()
                )
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                items.forEach { item ->
                    DropdownMenuItem(
                        text = { Text(itemLabel(item)) },
                        onClick = {
                            onSelect(item)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun CompareRoleMultiDropdown(
    roles: List<RoleEntity>,
    selectedRoles: List<RoleEntity>,
    onToggle: (RoleEntity) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxWidth()) {
        Text(
            "انتخاب نقش‌ها (۲ تا ۴ نقش)",
            style = MaterialTheme.typography.labelMedium
        )

        Box {
            OutlinedButton(
                onClick = { expanded = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (selectedRoles.isEmpty()) "انتخاب نقش‌ها"
                    else selectedRoles.joinToString("، ") { it.title },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2
                )
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                roles.forEach { role ->
                    val checked = selectedRoles.any { it.id == role.id }
                    DropdownMenuItem(
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = checked, onCheckedChange = null)
                                Spacer(Modifier.width(8.dp))
                                Text(role.title)
                            }
                        },
                        onClick = { onToggle(role) }
                    )
                }
            }
        }

        Text(
            "انتخاب‌شده: ${selectedRoles.size} نقش",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompareScreen(
    taziehTitle: String,
    roles: List<RoleEntity>,
    sections: List<CompareSectionItem>,
    roleSections: Map<Long, List<SectionEntity>>,
    onBack: () -> Unit
) {
    var mode by remember { mutableStateOf("role") }
    var view by remember { mutableStateOf("diff") }
    var granularity by remember { mutableStateOf("line") }

    var roleA by remember(roles) {
        mutableStateOf(roles.getOrNull(0))
    }

    var roleB by remember(roles) {
        mutableStateOf(roles.getOrNull(1))
    }

    var conversationRoles by remember(roles) {
        mutableStateOf(roles.take(2))
    }
    var conversationTexts by remember { mutableStateOf(emptyList<Pair<String, String>>()) }
    var isPreparingConversation by remember { mutableStateOf(false) }
    var conversationFontSize by remember { mutableStateOf(14f) }

    var sectionA by remember(sections) {
        mutableStateOf(sections.getOrNull(0))
    }

    var sectionB by remember(sections) {
        mutableStateOf(sections.getOrNull(1))
    }

    var compared by remember { mutableStateOf(false) }
    var isCalculating by remember { mutableStateOf(false) }

    var leftText by remember { mutableStateOf("") }
    var rightText by remember { mutableStateOf("") }

    var diff by remember {
        mutableStateOf(TaziehTextDiff.compare("", ""))
    }

    var wordDiff by remember {
        mutableStateOf(TaziehWordDiff.compare("", ""))
    }

    // محاسبه اختلاف سطری در پس‌زمینه
    // فقط پس از انتخاب دو مورد و زدن دکمه مقایسه اجرا می‌شود.
    LaunchedEffect(
        compared,
        mode,
        roleA?.id,
        roleB?.id,
        sectionA?.section?.id,
        sectionB?.section?.id,
        roleSections
    ) {
        if (!compared || mode == "conversation") return@LaunchedEffect

        isCalculating = true

        try {
            val result = withContext(Dispatchers.Default) {
                val left = if (mode == "role") {
                    roleA?.let { role ->
                        roleSections[role.id].orEmpty().joinToString("\n\n") { section ->
                            "【${section.title}】\n${section.content}"
                        }
                    }.orEmpty()
                } else {
                    sectionA?.section?.content.orEmpty()
                }

                val right = if (mode == "role") {
                    roleB?.let { role ->
                        roleSections[role.id].orEmpty().joinToString("\n\n") { section ->
                            "【${section.title}】\n${section.content}"
                        }
                    }.orEmpty()
                } else {
                    sectionB?.section?.content.orEmpty()
                }

                Triple(
                    left,
                    right,
                    TaziehTextDiff.compare(left, right)
                )
            }

            leftText = result.first
            rightText = result.second
            diff = result.third
        } finally {
            isCalculating = false
        }
    }

    // در حالت نمایش مکالمه، فقط متن نقش‌ها آماده می‌شود و هیچ مقایسه‌ای انجام نمی‌گیرد.
    LaunchedEffect(compared, mode, conversationRoles.map { it.id }, roleSections) {
        if (!compared || mode != "conversation") {
            conversationTexts = emptyList()
            isPreparingConversation = false
            return@LaunchedEffect
        }

        isPreparingConversation = true
        try {
            val prepared = withContext(Dispatchers.Default) {
                conversationRoles.map { role ->
                    val text = roleSections[role.id].orEmpty().joinToString("\n\n") { section ->
                        "【${section.title}】\n${section.content}"
                    }
                    role.title to text
                }
            }
            conversationTexts = prepared
        } finally {
            isPreparingConversation = false
        }
    }

    // محاسبه اختلاف واژه‌ای فقط در صورت انتخاب این حالت
    LaunchedEffect(
        compared,
        granularity,
        leftText,
        rightText
    ) {
        if (compared && mode != "conversation" && granularity == "word" && !isCalculating) {
            wordDiff = withContext(Dispatchers.Default) {
                TaziehWordDiff.compare(leftText, rightText)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(if (mode == "conversation") "نمایش هم‌زمان مکالمه نقش‌ها — $taziehTitle" else "مقایسه — $taziehTitle")
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "بازگشت"
                        )
                    }
                }
            )
        }
    ) { padding ->

        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {

            if (!compared) {

                Text(
                    "انتخاب قابلیت",
                    style = MaterialTheme.typography.titleSmall
                )

                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = mode == "role",
                        onClick = {
                            mode = "role"
                            compared = false
                        },
                        label = { Text("مقایسه نقش‌ها") }
                    )

                    FilterChip(
                        selected = mode == "section",
                        onClick = {
                            mode = "section"
                            compared = false
                        },
                        label = { Text("مقایسه بخش‌ها") }
                    )

                    FilterChip(
                        selected = mode == "conversation",
                        onClick = {
                            mode = "conversation"
                            compared = false
                        },
                        label = { Text("نمایش هم‌زمان مکالمه نقش‌ها") }
                    )
                }

                Spacer(Modifier.height(12.dp))

                if (mode == "conversation") {

                    CompareRoleMultiDropdown(
                        roles = roles,
                        selectedRoles = conversationRoles,
                        onToggle = { role ->
                            val exists = conversationRoles.any { it.id == role.id }
                            if (exists) {
                                conversationRoles = conversationRoles.filterNot { it.id == role.id }
                            } else if (conversationRoles.size < 4) {
                                conversationRoles = conversationRoles + role
                            }
                            compared = false
                        }
                    )

                } else if (mode == "role") {

                    CompareDropdown(
                        label = "نقش اول",
                        selected = roleA,
                        items = roles,
                        itemLabel = { it.title }
                    ) {
                        roleA = it
                        compared = false
                    }

                    Spacer(Modifier.height(8.dp))

                    CompareDropdown(
                        label = "نقش دوم",
                        selected = roleB,
                        items = roles,
                        itemLabel = { it.title }
                    ) {
                        roleB = it
                        compared = false
                    }

                } else {

                    CompareDropdown(
                        label = "بخش اول",
                        selected = sectionA,
                        items = sections,
                        itemLabel = {
                            "${it.roleTitle} ← ${it.section.title}"
                        }
                    ) {
                        sectionA = it
                        compared = false
                    }

                    Spacer(Modifier.height(8.dp))

                    CompareDropdown(
                        label = "بخش دوم",
                        selected = sectionB,
                        items = sections,
                        itemLabel = {
                            "${it.roleTitle} ← ${it.section.title}"
                        }
                    ) {
                        sectionB = it
                        compared = false
                    }
                }

                Button(
                    onClick = {
                        compared = true
                    },
                    enabled = when (mode) {
                        "conversation" -> conversationRoles.size in 2..4
                        "role" -> roleA != null && roleB != null && roleA?.id != roleB?.id
                        else -> sectionA != null && sectionB != null &&
                                sectionA?.section?.id != sectionB?.section?.id
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                ) {
                    Text(if (mode == "conversation") "نمایش مکالمه نقش‌ها" else "نمایش مقایسه")
                }

                Text(
                    if (mode == "conversation") {
                        "متن ۲ تا ۴ نقش به‌صورت کادرهای جداگانه و هم‌زمان نمایش داده می‌شود."
                    } else {
                        "انتخاب‌ها مستقل هستند؛ می‌توانید هر دو نقش یا هر دو بخش را خودتان تعیین کنید."
                    },
                    style = MaterialTheme.typography.bodySmall
                )

            } else if (isCalculating || isPreparingConversation) {

                Box(
                    modifier = Modifier
                        .fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator()
                        Spacer(Modifier.height(12.dp))
                        Text(
                            if (mode == "conversation") "در حال آماده‌سازی متن نقش‌ها..."
                            else "در حال آماده‌سازی و مقایسه متن‌ها..."
                        )
                    }
                }

            } else {

                if (mode == "conversation") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { compared = false }) {
                            Text("تغییر انتخاب نقش‌ها")
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("قلم", style = MaterialTheme.typography.labelMedium)
                            TextButton(
                                onClick = { conversationFontSize = (conversationFontSize - 1f).coerceAtLeast(10f) },
                                enabled = conversationFontSize > 10f
                            ) { Text("−") }
                            Text("${conversationFontSize.toInt()}", style = MaterialTheme.typography.labelLarge)
                            TextButton(
                                onClick = { conversationFontSize = (conversationFontSize + 1f).coerceAtMost(22f) },
                                enabled = conversationFontSize < 22f
                            ) { Text("+") }
                        }
                    }

                    Text(
                        "برای دنبال کردن گفت‌وگو، متن هر نقش را در کادر مستقل آن بخوانید.",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        conversationTexts.forEach { (roleTitle, roleText) ->
                            key(roleTitle) {
                                Card(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .verticalScroll(rememberScrollState())
                                            .padding(10.dp)
                                    ) {
                                        Text(
                                            roleTitle,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Divider(modifier = Modifier.padding(vertical = 5.dp))
                                        Text(
                                            text = roleText.ifBlank { "برای این نقش متنی ثبت نشده است." },
                                            fontSize = conversationFontSize.sp,
                                            lineHeight = (conversationFontSize * 1.65f).sp
                                        )
                                    }
                                }
                            }
                        }
                    }

                } else {

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(
                        onClick = {
                            compared = false
                        }
                    ) {
                        Text("تغییر انتخاب")
                    }

                    Row {
                        FilterChip(
                            selected = view == "diff",
                            onClick = { view = "diff" },
                            label = { Text("اختلاف‌ها") }
                        )

                        Spacer(Modifier.width(6.dp))

                        FilterChip(
                            selected = view == "side",
                            onClick = { view = "side" },
                            label = { Text("دو متن") }
                        )
                    }
                }

                Text(
                    "یکسان: ${diff.same}  |  افزوده: ${diff.added}  |  حذف‌شده: ${diff.removed}",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                if (view == "diff") {

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = granularity == "line",
                            onClick = { granularity = "line" },
                            label = { Text("اختلاف سطری") }
                        )

                        FilterChip(
                            selected = granularity == "word",
                            onClick = { granularity = "word" },
                            label = { Text("اختلاف واژه‌ای") }
                        )
                    }

                    if (granularity == "line") {

                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                        ) {
                            itemsIndexed(diff.lines) { _, line ->

                                val label = when (line.kind) {
                                    DiffKind.SAME -> "="
                                    DiffKind.ADDED -> "+"
                                    DiffKind.REMOVED -> "−"
                                }

                                val color = when (line.kind) {
                                    DiffKind.SAME ->
                                        MaterialTheme.colorScheme.surface

                                    DiffKind.ADDED ->
                                        MaterialTheme.colorScheme.tertiaryContainer

                                    DiffKind.REMOVED ->
                                        MaterialTheme.colorScheme.errorContainer
                                }

                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .background(color)
                                        .padding(
                                            horizontal = 8.dp,
                                            vertical = 5.dp
                                        )
                                ) {
                                    Text(
                                        label,
                                        style = MaterialTheme.typography.labelLarge,
                                        modifier = Modifier.width(24.dp)
                                    )

                                    Text(
                                        line.text.ifBlank { " " },
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }

                    } else {

                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .padding(8.dp)
                        ) {
                            itemsIndexed(wordDiff) { _, token ->

                                val color = when (token.change) {
                                    WordChange.SAME ->
                                        MaterialTheme.colorScheme.onSurface

                                    WordChange.ADDED ->
                                        MaterialTheme.colorScheme.primary

                                    WordChange.REMOVED ->
                                        MaterialTheme.colorScheme.error
                                }

                                Text(
                                    token.value,
                                    color = color,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }

                    Text(
                        "راهنما: + افزوده‌شده در متن دوم، − حذف‌شده از متن اول، = یکسان",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 6.dp)
                    )

                } else {

                    Row(
                        Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {

                        Column(
                            Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .verticalScroll(rememberScrollState())
                                .padding(6.dp)
                        ) {
                            Text(
                                if (mode == "role") {
                                    roleA?.title.orEmpty()
                                } else {
                                    sectionA?.section?.title.orEmpty()
                                },
                                style = MaterialTheme.typography.titleSmall
                            )

                            Text(leftText)
                        }

                        Box(
                            Modifier
                                .width(1.dp)
                                .fillMaxHeight()
                                .background(
                                    MaterialTheme.colorScheme.outlineVariant
                                )
                        )

                        Column(
                            Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .background(
                                    MaterialTheme.colorScheme.surfaceVariant
                                )
                                .verticalScroll(rememberScrollState())
                                .padding(6.dp)
                        ) {
                            Text(
                                if (mode == "role") {
                                    roleB?.title.orEmpty()
                                } else {
                                    sectionB?.section?.title.orEmpty()
                                },
                                style = MaterialTheme.typography.titleSmall
                            )

                            Text(rightText)
                        }
                    }
                }
                }
            }
        }
    }
}
