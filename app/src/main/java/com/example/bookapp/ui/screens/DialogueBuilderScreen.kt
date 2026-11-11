package com.example.bookapp.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.util.Locale

private data class DialogueRoleSourceGroup(
    val key: String,
    val title: String,
    val sourcesByVersion: List<Pair<String, List<SectionPickerItem>>>
)

private fun dialogueRoleKey(value: String): String =
    value.trim()
        .replace('ي', 'ی')
        .replace('ك', 'ک')
        .replace("ـ", "")
        .lowercase(Locale.ROOT)
        .filterNot { it.isWhitespace() || it == '\u200c' }

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun DialogueBuilderScreen(
    allSections: List<SectionPickerItem>,
    onSave: (title: String, orderedSections: List<SectionPickerItem>) -> Unit,
    onBack: () -> Unit
) {
    // وضعیت‌ها بدون delegated properties نگهداری می‌شوند.
    val selectedSequenceState = remember {
        mutableStateOf<List<SectionPickerItem>>(emptyList())
    }

    val titleState = remember {
        mutableStateOf("")
    }

    val expandedVersionRoleKeyState = remember {
        mutableStateOf<String?>(null)
    }

    val selectedSequence = selectedSequenceState.value
    val title = titleState.value
    val expandedVersionRoleKey = expandedVersionRoleKeyState.value

    val roleGroups = remember(allSections) {
        allSections
            .filter {
                it.roleId > 0L && it.versionUid.isNotBlank()
            }
            .groupBy {
                dialogueRoleKey(it.roleTitle)
            }
            .map { (key, roleItems) ->
                val versions = roleItems
                    .groupBy { it.versionUid }
                    .map { (versionUid, sections) ->
                        versionUid to sections
                    }
                    .sortedWith(
                        compareByDescending<Pair<String, List<SectionPickerItem>>> {
                            it.second.firstOrNull()?.isCurrentVersion == true
                        }.thenByDescending {
                            it.second.firstOrNull()?.versionNumber ?: -1
                        }
                    )

                DialogueRoleSourceGroup(
                    key = key,
                    title = roleItems.first().roleTitle,
                    sourcesByVersion = versions
                )
            }
            .sortedBy { it.title }
    }

    val selectedVersionByRole = remember(roleGroups) {
        mutableStateMapOf<String, String>().apply {
            roleGroups.forEach { group ->
                group.sourcesByVersion.firstOrNull()?.let { source ->
                    put(group.key, source.first)
                }
            }
        }
    }

    val selectedRoleKeys = selectedSequence
        .map { dialogueRoleKey(it.roleTitle) }
        .distinct()

    val selectedRoleCount = selectedRoleKeys.size

    val valid = title.isNotBlank() &&
        selectedSequence.size >= 2 &&
        selectedRoleCount in 2..4

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("ساخت گفتگوی چندنسخه‌ای")
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
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
        ) {

            OutlinedTextField(
                value = title,
                onValueChange = {
                    titleState.value = it
                },
                label = {
                    Text("نام گفتگو")
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            )

            Text(
                text = "توالی انتخاب‌شده: ${selectedSequence.size} نوبت از $selectedRoleCount نقش",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            if (selectedSequence.isEmpty()) {
                Text(
                    text = "برای هر نقش، نسخهٔ منبع را انتخاب کنید؛ سپس بخش‌ها را به ترتیب اجرای گفتگو اضافه کنید.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(
                        horizontal = 16.dp,
                        vertical = 6.dp
                    )
                )
            } else {
                LazyRow(
                    contentPadding = PaddingValues(
                        horizontal = 16.dp,
                        vertical = 8.dp
                    ),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        selectedSequence.indices.toList(),
                        key = { index -> index }
                    ) { index ->

                        val item = selectedSequence[index]

                        AssistChip(
                            onClick = {
                                selectedSequenceState.value =
                                    selectedSequenceState.value.toMutableList().apply {
                                        removeAt(index)
                                    }
                            },
                            label = {
                                Text(
                                    "${index + 1}. ${item.roleTitle} · ${item.sectionTitle}"
                                )
                            },
                            trailingIcon = {
                                Icon(
                                    Icons.Filled.Close,
                                    contentDescription = "حذف نوبت"
                                )
                            }
                        )
                    }
                }
            }

            HorizontalDivider()

            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {

                items(
                    items = roleGroups,
                    key = { group -> group.key }
                ) { group ->

                    val selectedVersionUid =
                        selectedVersionByRole[group.key]
                            ?: group.sourcesByVersion.firstOrNull()?.first.orEmpty()

                    val selectedVersionSections =
                        group.sourcesByVersion
                            .firstOrNull { source ->
                                source.first == selectedVersionUid
                            }
                            ?.second
                            .orEmpty()

                    val versionMenuExpanded =
                        expandedVersionRoleKey == group.key

                    val roleAlreadySelected =
                        group.key in selectedRoleKeys

                    val roleLimitReached =
                        !roleAlreadySelected && selectedRoleCount >= 4

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {

                            Text(
                                text = group.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {

                                Column(
                                    modifier = Modifier.weight(1f)
                                ) {

                                    Text(
                                        text = "نسخهٔ منبع این نقش",
                                        style = MaterialTheme.typography.labelSmall
                                    )

                                    OutlinedButton(
                                        onClick = {
                                            expandedVersionRoleKeyState.value =
                                                if (versionMenuExpanded) {
                                                    null
                                                } else {
                                                    group.key
                                                }
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = selectedVersionSections
                                                .firstOrNull()
                                                ?.versionLabel
                                                ?: "انتخاب نسخه",
                                            maxLines = 2
                                        )
                                    }

                                    DropdownMenu(
                                        expanded = versionMenuExpanded,
                                        onDismissRequest = {
                                            expandedVersionRoleKeyState.value = null
                                        }
                                    ) {

                                        group.sourcesByVersion.forEach { source ->

                                            val versionUid = source.first
                                            val sections = source.second

                                            DropdownMenuItem(
                                                text = {
                                                    Text(
                                                        sections.firstOrNull()
                                                            ?.versionLabel
                                                            ?: "نسخه آرشیو"
                                                    )
                                                },
                                                onClick = {

                                                    selectedVersionByRole[group.key] =
                                                        versionUid

                                                    // با تغییر نسخهٔ نقش،
                                                    // نوبت‌های قبلی همان نقش حذف می‌شوند.
                                                    selectedSequenceState.value =
                                                        selectedSequenceState.value.filter { item ->
                                                            dialogueRoleKey(item.roleTitle) !=
                                                                group.key
                                                        }

                                                    expandedVersionRoleKeyState.value = null
                                                }
                                            )
                                        }
                                    }
                                }

                                Column {

                                    Spacer(
                                        modifier = Modifier.height(18.dp)
                                    )

                                    Text(
                                        text = when {
                                            roleAlreadySelected -> "در گفتگو"
                                            roleLimitReached -> "حداکثر ۴ نقش"
                                            else -> "قابل افزودن"
                                        },
                                        color = if (roleLimitReached) {
                                            MaterialTheme.colorScheme.error
                                        } else {
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        },
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }

                            if (selectedVersionSections.isEmpty()) {

                                Text(
                                    text = "برای این نقش در نسخه انتخاب‌شده بخشی ثبت نشده است.",
                                    style = MaterialTheme.typography.bodySmall
                                )

                            } else {

                                selectedVersionSections.forEach { item ->

                                    val alreadyAdded =
                                        selectedSequence.any {
                                            it.sectionId == item.sectionId
                                        }

                                    OutlinedButton(
                                        onClick = {
                                            if (!alreadyAdded && !roleLimitReached) {
                                                selectedSequenceState.value =
                                                    selectedSequenceState.value + item
                                            }
                                        },
                                        enabled = !alreadyAdded && !roleLimitReached,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {

                                        Column(
                                            modifier = Modifier.fillMaxWidth()
                                        ) {

                                            Text(
                                                text = item.sectionTitle,
                                                fontWeight = FontWeight.SemiBold
                                            )

                                            Text(
                                                text = if (alreadyAdded) {
                                                    "به توالی افزوده شده"
                                                } else {
                                                    item.versionLabel
                                                },
                                                style = MaterialTheme.typography.labelSmall
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Button(
                onClick = {
                    onSave(
                        titleState.value.trim(),
                        selectedSequenceState.value
                    )
                },
                enabled = valid,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Text("ذخیره گفتگو")
            }

            if (!valid) {

                Text(
                    text = when {
                        title.isBlank() ->
                            "نام گفتگو را وارد کنید."

                        selectedSequence.size < 2 ->
                            "حداقل دو نوبت از گفتگو انتخاب کنید."

                        selectedRoleCount < 2 ->
                            "برای ساخت گفتگو حداقل دو نقش متفاوت انتخاب کنید."

                        selectedRoleCount > 4 ->
                            "حداکثر چهار نقش متفاوت قابل انتخاب است."

                        else ->
                            "انتخاب نقش‌ها و نوبت‌ها را بررسی کنید."
                    },
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(
                        horizontal = 16.dp,
                        vertical = 6.dp
                    )
                )
            }

            Spacer(
                modifier = Modifier.height(8.dp)
            )
        }
    }
}
