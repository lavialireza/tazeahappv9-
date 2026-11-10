
package com.example.bookapp.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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

private data class TaziehCardPalette(
    val background: Color,
    val iconBackground: Color,
    val accent: Color,
    val badgeBackground: Color
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
    val paper = Color(0xFFF7F8F5)

    val green = Color(0xFFE8F2E9)
    val greenDark = Color(0xFF315B45)

    val cardPalettes = listOf(
        // کرم گرم
        TaziehCardPalette(
            background = Color(0xFFFFF3E5),
            iconBackground = Color(0xFFF1DFC5),
            accent = Color(0xFF79552F),
            badgeBackground = Color(0xFFF2E3CD)
        ),

        // یاسی ملایم
        TaziehCardPalette(
            background = Color(0xFFF1EAF5),
            iconBackground = Color(0xFFE2D7EC),
            accent = Color(0xFF654A78),
            badgeBackground = Color(0xFFE7DEEF)
        ),

        // صورتی چرک
        TaziehCardPalette(
            background = Color(0xFFF7E9E8),
            iconBackground = Color(0xFFEED7D6),
            accent = Color(0xFF824D55),
            badgeBackground = Color(0xFFF0DEDD)
        ),

        // خاکستری گرم
        TaziehCardPalette(
            background = Color(0xFFEEECE6),
            iconBackground = Color(0xFFDFDED5),
            accent = Color(0xFF53584F),
            badgeBackground = Color(0xFFE2E1D9)
        ),

        // هلویی روشن
        TaziehCardPalette(
            background = Color(0xFFF8EBDD),
            iconBackground = Color(0xFFF0DCC7),
            accent = Color(0xFF80583C),
            badgeBackground = Color(0xFFF1E1CF)
        )
    )

    val fields = remember(items) {
        items.distinctBy { it.fieldId }
            .sortedBy {
                normalizePersianSearch(it.fieldTitle)
            }
    }

    val selectedFieldTitle = fields
        .firstOrNull { it.fieldId == selectedField }
        ?.fieldTitle

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
            1 -> base.sortedWith(
                compareByDescending<TaziehCatalogItem> {
                    it.roleCount
                }.thenBy {
                    normalizePersianSearch(it.title)
                }
            )

            else -> base.sortedBy {
                normalizePersianSearch(it.title)
            }
        }
    }

    Scaffold(
        containerColor = paper,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (selectedField == null) {
                            "فهرست تعزیه‌ها"
                        } else {
                            "تعزیه‌های این زمینه"
                        },
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
                    Box(
                        modifier = Modifier.size(48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        IconButton(
                            onClick = {
                                menuExpanded = true
                            }
                        ) {
                            Icon(
                                Icons.Filled.FilterList,
                                contentDescription = "فیلتر و مرتب‌سازی",
                                tint = ink
                            )
                        }

                        if (selectedField != null || audioOnly) {
                            Surface(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(top = 8.dp, end = 8.dp)
                                    .size(9.dp),
                                shape = CircleShape,
                                color = Color(0xFFB45A55)
                            ) {}
                        }
                    }

                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = {
                            menuExpanded = false
                        }
                    ) {
                        DropdownMenuItem(
                            text = {
                                Text("همه تعزیه‌ها")
                            },
                            leadingIcon = {
                                if (selectedField == null && !audioOnly) {
                                    Icon(
                                        Icons.Filled.Check,
                                        contentDescription = null
                                    )
                                }
                            },
                            onClick = {
                                selectedField = null
                                audioOnly = false
                                menuExpanded = false
                            }
                        )

                        DropdownMenuItem(
                            text = {
                                Text(
                                    if (audioOnly) {
                                        "نمایش همه تعزیه‌ها"
                                    } else {
                                        "فقط تعزیه‌های دارای صوت"
                                    }
                                )
                            },
                            leadingIcon = {
                                if (audioOnly) {
                                    Icon(
                                        Icons.Filled.Check,
                                        contentDescription = null
                                    )
                                }
                            },
                            onClick = {
                                audioOnly = !audioOnly
                                menuExpanded = false
                            }
                        )

                        HorizontalDivider()

                        DropdownMenuItem(
                            text = {
                                Text("مرتب‌سازی بر اساس عنوان")
                            },
                            leadingIcon = {
                                if (sortMode == 0) {
                                    Icon(
                                        Icons.Filled.Check,
                                        contentDescription = null
                                    )
                                }
                            },
                            onClick = {
                                sortMode = 0
                                menuExpanded = false
                            }
                        )

                        DropdownMenuItem(
                            text = {
                                Text("بیشترین تعداد نقش")
                            },
                            leadingIcon = {
                                if (sortMode == 1) {
                                    Icon(
                                        Icons.Filled.Check,
                                        contentDescription = null
                                    )
                                }
                            },
                            onClick = {
                                sortMode = 1
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

            // تعداد نتایج

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = 18.dp,
                        vertical = 9.dp
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
                    color = Color(0xFFF1E5CE)
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

            // جستجو با امکان پاک‌کردن سریع

            OutlinedTextField(
                value = query,
                onValueChange = {
                    query = it
                },
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
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                query = ""
                            }
                        ) {
                            Icon(
                                Icons.Filled.Close,
                                contentDescription = "پاک‌کردن جستجو",
                                tint = muted
                            )
                        }
                    }
                },
                placeholder = {
                    Text("جستجوی نام تعزیه، نویسنده یا زمینه")
                },
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = Color.White,
                    focusedContainerColor = Color.White,
                    unfocusedBorderColor = Color(0xFFDCDDD8),
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
                                    if (selectedField == field.fieldId) {
                                        null
                                    } else {
                                        field.fieldId
                                    }
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

            // نمایش وضعیت فیلترها و مرتب‌سازی فعال

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = 16.dp,
                        vertical = 7.dp
                    )
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {

                if (selectedField != null) {
                    FilterChip(
                        selected = true,
                        onClick = {
                            selectedField = null
                        },
                        label = {
                            Text(
                                "زمینه: ${selectedFieldTitle ?: "انتخاب‌شده"}"
                            )
                        },
                        trailingIcon = {
                            Icon(
                                Icons.Filled.Close,
                                contentDescription = "حذف فیلتر زمینه",
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFF1E5CE),
                            selectedLabelColor = Color(0xFF735A2A)
                        )
                    )
                }

                if (audioOnly) {
                    FilterChip(
                        selected = true,
                        onClick = {
                            audioOnly = false
                        },
                        label = {
                            Text("فقط دارای صوت")
                        },
                        trailingIcon = {
                            Icon(
                                Icons.Filled.Close,
                                contentDescription = "حذف فیلتر صوت",
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFF4E3E3),
                            selectedLabelColor = Color(0xFF873F49)
                        )
                    )
                }

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFFEAEAF0)
                ) {
                    Text(
                        text = if (sortMode == 0) {
                            "مرتب‌سازی: عنوان"
                        } else {
                            "مرتب‌سازی: تعداد نقش"
                        },
                        modifier = Modifier.padding(
                            horizontal = 11.dp,
                            vertical = 8.dp
                        ),
                        color = ink,
                        fontWeight = FontWeight.Medium,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }

            Spacer(Modifier.height(3.dp))

            // فهرست مجالس

            if (filtered.isEmpty()) {

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(
                        start = 15.dp,
                        end = 15.dp,
                        top = 7.dp,
                        bottom = 22.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {

                    itemsIndexed(
                        filtered,
                        key = { _, item -> item.id }
                    ) { index, item ->

                        val palette =
                            cardPalettes[index % cardPalettes.size]

                        Card(
                            onClick = {
                                onOpen(item)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(19.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = palette.background
                            ),
                            elevation = CardDefaults.cardElevation(
                                defaultElevation = 1.dp
                            )
                        ) {

                            Column(
                                modifier = Modifier.padding(15.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {

                                // عنوان و اطلاعات اصلی مجلس

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {

                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = palette.iconBackground
                                    ) {
                                        Icon(
                                            Icons.Filled.MenuBook,
                                            contentDescription = null,
                                            modifier = Modifier
                                                .padding(10.dp)
                                                .size(24.dp),
                                            tint = palette.accent
                                        )
                                    }

                                    Spacer(Modifier.width(11.dp))

                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = item.title,
                                            color = palette.accent,
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.titleMedium,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )

                                        Text(
                                            text = item.fieldTitle,
                                            color = palette.accent.copy(alpha = 0.85f),
                                            style = MaterialTheme.typography.bodySmall,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    Icon(
                                        Icons.Filled.ChevronLeft,
                                        contentDescription = "مشاهده تعزیه",
                                        tint = palette.accent
                                    )
                                }

                                // نویسنده

                                if (!item.author.isNullOrBlank()) {
                                    Text(
                                        text = "نویسنده: ${item.author}",
                                        color = muted,
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                // نشانگرهای اطلاعاتی

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {

                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = palette.badgeBackground
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
                                                tint = palette.accent
                                            )

                                            Spacer(Modifier.width(5.dp))

                                            Text(
                                                "${item.roleCount} نقش",
                                                color = palette.accent,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }

                                    if (item.hasAudio) {
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = palette.iconBackground
                                        ) {
                                            Text(
                                                "دارای صوت",
                                                modifier = Modifier.padding(
                                                    horizontal = 9.dp,
                                                    vertical = 6.dp
                                                ),
                                                color = palette.accent,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }

                                HorizontalDivider(
                                    color = palette.accent.copy(alpha = 0.15f)
                                )

                                // ورود مشخص به نقش‌های مجلس

                                Button(
                                    onClick = {
                                        onOpen(item)
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = palette.accent,
                                        contentColor = Color.White
                                    ),
                                    contentPadding = PaddingValues(
                                        horizontal = 12.dp,
                                        vertical = 9.dp
                                    )
                                ) {
                                    Text(
                                        "مشاهده نقش‌های مجلس",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.labelLarge
                                    )

                                    Spacer(Modifier.width(6.dp))

                                    Icon(
                                        Icons.Filled.ChevronLeft,
                                        contentDescription = null,
                                        modifier = Modifier.size(19.dp)
                                    )
                                }

                                // ابزارهای مستقل مجلس

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {

                                    FilledTonalButton(
                                        onClick = {
                                            onOpenDialogues(item)
                                        },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.filledTonalButtonColors(
                                            containerColor = palette.badgeBackground,
                                            contentColor = palette.accent
                                        ),
                                        contentPadding = PaddingValues(
                                            horizontal = 5.dp,
                                            vertical = 8.dp
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
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    if (showGallery) {
                                        OutlinedButton(
                                            onClick = {
                                                onOpenGallery(item)
                                            },
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(12.dp),
                                            border = BorderStroke(
                                                1.dp,
                                                palette.accent.copy(alpha = 0.45f)
                                            ),
                                            colors = ButtonDefaults.outlinedButtonColors(
                                                contentColor = palette.accent
                                            ),
                                            contentPadding = PaddingValues(
                                                horizontal = 5.dp,
                                                vertical = 8.dp
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
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.Bold
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
