
package com.example.bookapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

data class TaziehCatalogItem(
    val id: Long,
    val fieldId: Long,
    val fieldTitle: String,
    val title: String,
    val author: String?,
    val roleCount: Int,
    val hasAudio: Boolean
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaziehCatalogScreen(
    items: List<TaziehCatalogItem>,
    onOpen: (TaziehCatalogItem) -> Unit,
    onOpenGallery: (TaziehCatalogItem) -> Unit = {},
    onOpenDialogues: (TaziehCatalogItem) -> Unit = {},
    showGallery: Boolean = true,
    onBack: () -> Unit,
    initialFieldId: Long? = null
) {
    var query by remember { mutableStateOf("") }
    var selectedField by remember(initialFieldId) {
        mutableStateOf(initialFieldId)
    }
    var audioOnly by remember { mutableStateOf(false) }
    var sortMode by remember { mutableStateOf(0) }
    var menuExpanded by remember { mutableStateOf(false) }

    val ink = Color(0xFF263B5B)
    val muted = Color(0xFF647184)
    val green = Color(0xFFE8F2E9)
    val greenDark = Color(0xFF315B45)
    val gold = Color(0xFFF4EBD7)
    val blue = Color(0xFFE5EDF5)
    val paper = Color(0xFFF7F8F5)
    val rose = Color(0xFFF4E7E6)

    val fields = remember(items) {
        items.distinctBy { it.fieldId }
            .sortedBy { normalizePersianSearch(it.fieldTitle) }
    }

    val filtered = remember(
        items,
        query,
        selectedField,
        audioOnly,
        sortMode
    ) {
        val q = normalizePersianSearch(query)

        val base = items.filter { item ->
            (selectedField == null || item.fieldId == selectedField) &&
                (!audioOnly || item.hasAudio) &&
                (
                    q.isBlank() ||
                    normalizePersianSearch(item.title).contains(q) ||
                    normalizePersianSearch(item.author.orEmpty()).contains(q) ||
                    normalizePersianSearch(item.fieldTitle).contains(q)
                )
        }

        when (sortMode) {
            1 -> base.sortedBy {
                normalizePersianSearch(it.title)
            }

            2 -> base.sortedByDescending {
                it.roleCount
            }

            else -> base.sortedWith(
                compareBy<TaziehCatalogItem> {
                    normalizePersianSearch(it.fieldTitle)
                }.thenBy {
                    normalizePersianSearch(it.title)
                }
            )
        }
    }

    Scaffold(
        containerColor = paper,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (selectedField == null)
                            "فهرست تعزیه‌ها"
                        else
                            "تعزیه‌های این زمینه",
                        color = ink,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "بازگشت",
                            tint = ink
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { menuExpanded = true }
                    ) {
                        Icon(
                            Icons.Filled.FilterList,
                            contentDescription = "فیلتر و مرتب‌سازی",
                            tint = ink
                        )
                    }

                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = {
                            menuExpanded = false
                        }
                    ) {
                        DropdownMenuItem(
                            text = { Text("همه تعزیه‌ها") },
                            onClick = {
                                selectedField = null
                                audioOnly = false
                                menuExpanded = false
                            }
                        )

                        DropdownMenuItem(
                            text = {
                                Text(
                                    if (audioOnly)
                                        "نمایش همه تعزیه‌ها"
                                    else
                                        "فقط تعزیه‌های دارای صوت"
                                )
                            },
                            onClick = {
                                audioOnly = !audioOnly
                                menuExpanded = false
                            }
                        )

                        HorizontalDivider()

                        DropdownMenuItem(
                            text = { Text("مرتب‌سازی بر اساس نام") },
                            onClick = {
                                sortMode = 1
                                menuExpanded = false
                            }
                        )

                        DropdownMenuItem(
                            text = { Text("بیشترین تعداد نقش") },
                            onClick = {
                                sortMode = 2
                                menuExpanded = false
                            }
                        )

                        DropdownMenuItem(
                            text = { Text("مرتب‌سازی بر اساس زمینه") },
                            onClick = {
                                sortMode = 0
                                menuExpanded = false
                            }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = paper
                )
            )
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {

            // معرفی فهرست

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(
                    containerColor = green
                ),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = 0.dp
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(17.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(15.dp),
                        color = Color.White
                    ) {
                        Icon(
                            Icons.Filled.MenuBook,
                            contentDescription = null,
                            modifier = Modifier
                                .padding(12.dp)
                                .size(29.dp),
                            tint = greenDark
                        )
                    }

                    Spacer(Modifier.width(13.dp))

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Text(
                            "گنجینه مجالس تعزیه",
                            color = ink,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )

                        Text(
                            "مجلس موردنظر را جستجو و انتخاب کنید.",
                            color = muted,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            // تعداد نتایج

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = 18.dp,
                        vertical = 7.dp
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "فهرست مجالس",
                    color = ink,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )

                Spacer(Modifier.weight(1f))

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = gold
                ) {
                    Text(
                        "${filtered.size} تعزیه",
                        modifier = Modifier.padding(
                            horizontal = 12.dp,
                            vertical = 6.dp
                        ),
                        color = Color(0xFF735A2A),
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }

            // جستجو

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 5.dp),
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                leadingIcon = {
                    Icon(
                        Icons.Filled.Search,
                        contentDescription = null,
                        tint = ink
                    )
                },
                placeholder = {
                    Text("جستجوی نام تعزیه، نویسنده یا زمینه")
                },
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = green,
                    focusedContainerColor = Color.White,
                    unfocusedBorderColor = Color.Transparent,
                    focusedBorderColor = Color(0xFF789A7D)
                )
            )

            // فیلتر زمینه‌ها

            if (fields.isNotEmpty()) {

                Text(
                    "زمینه‌ها",
                    modifier = Modifier.padding(
                        start = 17.dp,
                        end = 17.dp,
                        top = 9.dp,
                        bottom = 6.dp
                    ),
                    color = ink,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelLarge
                )

                LazyRow(
                    contentPadding = PaddingValues(
                        horizontal = 14.dp
                    ),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item(key = "all_fields") {
                        FilterChip(
                            selected = selectedField == null,
                            onClick = {
                                selectedField = null
                            },
                            label = {
                                Text("همه")
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = green,
                                selectedLabelColor = greenDark
                            )
                        )
                    }

                    items(
                        fields,
                        key = { it.fieldId }
                    ) { field ->

                        FilterChip(
                            selected = selectedField == field.fieldId,
                            onClick = {
                                selectedField =
                                    if (selectedField == field.fieldId)
                                        null
                                    else
                                        field.fieldId
                            },
                            label = {
                                Text(field.fieldTitle)
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = green,
                                selectedLabelColor = greenDark
                            )
                        )
                    }
                }
            }

            // وضعیت فیلتر صوت

            if (audioOnly) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = rose
                    ) {
                        Text(
                            "فقط مجالس دارای صوت",
                            modifier = Modifier.padding(
                                horizontal = 11.dp,
                                vertical = 6.dp
                            ),
                            color = Color(0xFF873F49),
                            style = MaterialTheme.typography.labelMedium
                        )
                    }

                    Spacer(Modifier.weight(1f))

                    TextButton(
                        onClick = {
                            audioOnly = false
                        }
                    ) {
                        Text("حذف فیلتر")
                    }
                }
            }

            Spacer(Modifier.height(5.dp))

            // فهرست تعزیه‌ها

            if (filtered.isEmpty()) {

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            Icons.Filled.MenuBook,
                            contentDescription = null,
                            tint = muted,
                            modifier = Modifier.size(43.dp)
                        )

                        Text(
                            "تعزیه‌ای با این فیلترها پیدا نشد",
                            color = muted,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

            } else {

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 15.dp,
                        end = 15.dp,
                        top = 7.dp,
                        bottom = 22.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {

                    items(
                        filtered,
                        key = { it.id }
                    ) { item ->

                        Card(
                            onClick = {
                                onOpen(item)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(19.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color.White
                            ),
                            elevation = CardDefaults.cardElevation(
                                defaultElevation = 1.dp
                            )
                        ) {

                            Column(
                                modifier = Modifier.padding(15.dp),
                                verticalArrangement = Arrangement.spacedBy(9.dp)
                            ) {

                                // عنوان تعزیه

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {

                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = green
                                    ) {
                                        Icon(
                                            Icons.Filled.MenuBook,
                                            contentDescription = null,
                                            modifier = Modifier
                                                .padding(9.dp)
                                                .size(23.dp),
                                            tint = greenDark
                                        )
                                    }

                                    Spacer(Modifier.width(11.dp))

                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {

                                        Text(
                                            item.title,
                                            color = ink,
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.titleMedium
                                        )

                                        Text(
                                            item.fieldTitle,
                                            color = greenDark,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }

                                    Icon(
                                        Icons.Filled.ChevronLeft,
                                        contentDescription = "مشاهده تعزیه",
                                        tint = muted
                                    )
                                }

                                // نویسنده

                                if (!item.author.isNullOrBlank()) {
                                    Text(
                                        "نویسنده: ${item.author}",
                                        color = muted,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }

                                // اطلاعات تعزیه

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {

                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = blue
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(
                                                horizontal = 9.dp,
                                                vertical = 6.dp
                                            ),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                Icons.Filled.Groups,
                                                contentDescription = null,
                                                modifier = Modifier.size(15.dp),
                                                tint = ink
                                            )

                                            Spacer(Modifier.width(5.dp))

                                            Text(
                                                "${item.roleCount} نقش",
                                                color = ink,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }

                                    if (item.hasAudio) {
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = gold
                                        ) {
                                            Text(
                                                "دارای صوت",
                                                modifier = Modifier.padding(
                                                    horizontal = 9.dp,
                                                    vertical = 6.dp
                                                ),
                                                color = Color(0xFF735A2A),
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }

                                HorizontalDivider(
                                    color = Color(0xFFEEF0F1)
                                )

                                // ابزارهای تعزیه

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {

                                    TextButton(
                                        onClick = {
                                            onOpenDialogues(item)
                                        },
                                        modifier = Modifier.weight(1f),
                                        contentPadding = PaddingValues(
                                            horizontal = 5.dp
                                        )
                                    ) {
                                        Icon(
                                            Icons.Filled.Groups,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )

                                        Spacer(Modifier.width(5.dp))

                                        Text(
                                            "گفت‌وگوها",
                                            style = MaterialTheme.typography.labelMedium
                                        )
                                    }

                                    if (showGallery) {

                                        TextButton(
                                            onClick = {
                                                onOpenGallery(item)
                                            },
                                            modifier = Modifier.weight(1f),
                                            contentPadding = PaddingValues(
                                                horizontal = 5.dp
                                            )
                                        ) {
                                            Icon(
                                                Icons.Filled.PhotoLibrary,
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp)
                                            )

                                            Spacer(Modifier.width(5.dp))

                                            Text(
                                                "گالری تصاویر",
                                                style = MaterialTheme.typography.labelMedium
                                            )
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
