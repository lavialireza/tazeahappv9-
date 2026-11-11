package com.example.bookapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

data class DialogueSummary(
    val id: Long,
    val title: String,
    val turnsCount: Int
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DialoguesScreen(
    taziehTitle: String,
    dialogues: List<DialogueSummary>,
    onOpenDialogue: (DialogueSummary) -> Unit,
    onEditDialogue: (DialogueSummary, String) -> Unit,
    onDeleteDialogue: (DialogueSummary) -> Unit,
    onCreateNew: () -> Unit,
    onBack: () -> Unit,
    readOnly: Boolean = false,
    canCreate: Boolean = !readOnly,
    canEdit: Boolean = !readOnly,
    canDelete: Boolean = !readOnly
) {
    var editTarget by remember {
        mutableStateOf<DialogueSummary?>(null)
    }
    var editTitle by remember {
        mutableStateOf("")
    }
    var deleteTarget by remember {
        mutableStateOf<DialogueSummary?>(null)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("گفتگوها: $taziehTitle")
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
        },
        floatingActionButton = {
            if (canCreate) {
                ExtendedFloatingActionButton(
                    text = { Text("گفتگوی جدید") },
                    icon = {
                        Icon(
                            Icons.Filled.Add,
                            contentDescription = null
                        )
                    },
                    onClick = onCreateNew
                )
            }
        }
    ) { padding ->

        if (dialogues.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Forum,
                        contentDescription = null,
                        modifier = Modifier.size(44.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )

                    Text(
                        text = if (canCreate) {
                            "هنوز گفتگویی نساخته‌اید.\nبرای شروع، گفتگوی جدید بسازید."
                        } else {
                            "هنوز گفتگویی برای نمایش وجود ندارد."
                        },
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyLarge
                    )

                    if (canCreate) {
                        Button(onClick = onCreateNew) {
                            Icon(
                                Icons.Filled.Add,
                                contentDescription = null
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("ساخت گفتگوی جدید")
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    items = dialogues,
                    key = { it.id }
                ) { dialogue ->

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        onClick = {
                            onOpenDialogue(dialogue)
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Filled.Forum,
                                contentDescription = null
                            )

                            Spacer(Modifier.width(10.dp))

                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    dialogue.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )

                                Text(
                                    "${dialogue.turnsCount} نوبت",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            if (canEdit) {
                                IconButton(
                                    onClick = {
                                        editTarget = dialogue
                                        editTitle = dialogue.title
                                    }
                                ) {
                                    Icon(
                                        Icons.Filled.Edit,
                                        contentDescription = "ویرایش نام"
                                    )
                                }
                            }

                            if (canDelete) {
                                IconButton(
                                    onClick = {
                                        deleteTarget = dialogue
                                    }
                                ) {
                                    Icon(
                                        Icons.Filled.Delete,
                                        contentDescription = "حذف گفتگو"
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    editTarget?.let { target ->
        AlertDialog(
            onDismissRequest = {
                editTarget = null
            },
            title = {
                Text("ویرایش نام گفتگو")
            },
            text = {
                OutlinedTextField(
                    value = editTitle,
                    onValueChange = {
                        editTitle = it
                    },
                    label = {
                        Text("نام گفتگو")
                    },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val newTitle = editTitle.trim()

                        if (newTitle.isNotBlank()) {
                            onEditDialogue(target, newTitle)
                            editTarget = null
                        }
                    },
                    enabled = editTitle.isNotBlank()
                ) {
                    Text("ذخیره")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        editTarget = null
                    }
                ) {
                    Text("انصراف")
                }
            }
        )
    }

    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = {
                deleteTarget = null
            },
            title = {
                Text("حذف گفتگو")
            },
            text = {
                Text(
                    "آیا گفتگو «${target.title}» حذف شود؟\n" +
                        "بخش‌های اصلی تعزیه حذف نخواهند شد."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteDialogue(target)
                        deleteTarget = null
                    }
                ) {
                    Text("حذف")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        deleteTarget = null
                    }
                ) {
                    Text("انصراف")
                }
            }
        )
    }
}
