
package com.example.bookapp.ui.screens

import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

data class ProfessionalRoleItem(
    val id: Long,
    val title: String,
    val sectionCount: Int,
    val firstVerse: String,
    val isMine: Boolean
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfessionalRoleScreen(
    taziehTitle: String,
    items: List<ProfessionalRoleItem>,
    onOpen: (ProfessionalRoleItem) -> Unit,
    onSetMine: (ProfessionalRoleItem) -> Unit,
    onCompare: (ProfessionalRoleItem) -> Unit,
    onOpenGallery: () -> Unit = {},
    showGallery: Boolean = true,
    onBack: () -> Unit,
    readOnly: Boolean = false,
    showCompare: Boolean = true,
    canSelectMine: Boolean = true
) {
    var query by remember { mutableStateOf("") }

    val ink = Color(0xFF263B5B)
    val muted = Color(0xFF647184)
    val lightGreen = Color(0xFFE8F2E9)
    val greenDark = Color(0xFF315B45)
    val softBlue = Color(0xFFE5EDF5)
    val softGold = Color(0xFFF4EBD7)
    val paper = Color(0xFFF7F8F5)

    val filtered = remember(items, query) {
        val normalizedQuery = normalizePersianSearch(query)
        items.filter {
            normalizedQuery.isBlank() ||
                normalizePersianSearch(it.title).contains(normalizedQuery)
        }
    }

    Scaffold(
        containerColor = paper,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "نقش‌های تعزیه",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = ink
                        )
                        Text(
                            text = taziehTitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = muted,
                            maxLines = 1
                        )
                    }
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
                    if (showGallery) {
                        IconButton(onClick = onOpenGallery) {
                            Icon(
                                Icons.Filled.PhotoLibrary,
                                contentDescription = "گالری تصاویر این تعزیه",
                                tint = ink
                            )
                        }
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

            // Search
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                leadingIcon = {
                    Icon(
                        Icons.Filled.Search,
                        contentDescription = null,
                        tint = greenDark
                    )
                },
                placeholder = {
                    Text("جستجو در میان نقش‌ها")
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = greenDark,
                    unfocusedBorderColor = Color(0xFFD8E1D9),
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White
                )
            )

            // Role information
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = lightGreen
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Filled.School,
                        contentDescription = null,
                        tint = greenDark
                    )

                    Spacer(Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "فهرست نقش‌های این تعزیه",
                            fontWeight = FontWeight.Bold,
                            color = ink,
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            "نقش من با علامت مشخص می‌شود.",
                            color = muted,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White
                    ) {
                        Text(
                            "${filtered.size} نقش",
                            modifier = Modifier.padding(
                                horizontal = 12.dp,
                                vertical = 7.dp
                            ),
                            color = greenDark,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }

            // Gallery
            if (showGallery) {
                Card(
                    onClick = onOpenGallery,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 7.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = softBlue
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(15.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color.White
                        ) {
                            Icon(
                                Icons.Filled.PhotoLibrary,
                                contentDescription = null,
                                modifier = Modifier.padding(10.dp),
                                tint = ink
                            )
                        }

                        Spacer(Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "گالری تصاویر این تعزیه",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = ink
                            )
                            Text(
                                if (readOnly)
                                    "مشاهده تصاویر اختصاصی این تعزیه"
                                else
                                    "افزودن و مدیریت تصاویر اختصاصی این تعزیه",
                                style = MaterialTheme.typography.bodySmall,
                                color = muted
                            )
                        }

                        Text(
                            "ورود",
                            color = ink,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }

            // Roles list
            if (filtered.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Filled.Search,
                            contentDescription = null,
                            tint = muted,
                            modifier = Modifier.size(38.dp)
                        )
                        Spacer(Modifier.height(10.dp))
                        Text(
                            if (items.isEmpty())
                                "هنوز نقشی برای این تعزیه ثبت نشده است."
                            else
                                "نقشی با این عنوان پیدا نشد.",
                            color = muted,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 8.dp,
                        bottom = 24.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                   itemsIndexed(
                    items = filtered,
                    key = { _, role -> role.id }
                ) { index, role ->

                        
Card(
    onClick = { onOpen(role) },
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.cardColors(
        containerColor = if (index % 2 == 0) {
            Color.White
        } else {
            Color(0xFFE8F2E9)
        }
    ),
    elevation = CardDefaults.cardElevation(
        defaultElevation = 1.dp
    )
) {
    Column(
        modifier = Modifier.padding(15.dp)
    ) {

                            Column(
                                modifier = Modifier.padding(15.dp)
                            ) {

                                // Role title and selected marker
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        role.title,
                                        modifier = Modifier.weight(1f),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = ink
                                    )

                                    if (role.isMine) {
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = lightGreen
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(
                                                    horizontal = 9.dp,
                                                    vertical = 5.dp
                                                ),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    Icons.Filled.CheckCircle,
                                                    contentDescription = "نقش من",
                                                    tint = greenDark,
                                                    modifier = Modifier.size(17.dp)
                                                )
                                                Spacer(Modifier.width(4.dp))
                                                Text(
                                                    "نقش من",
                                                    color = greenDark,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }

                                Spacer(Modifier.height(9.dp))

                                // Section count
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "${role.sectionCount} بخش",
                                        color = muted,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }

                                // First verse
                                if (role.firstVerse.isNotBlank()) {
                                    Spacer(Modifier.height(8.dp))

                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = Color(0xFFF5F6F4)
                                    ) {
                                        Text(
                                            text = role.firstVerse,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(10.dp),
                                            maxLines = 2,
                                            color = muted,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }

                                // Actions
                                if (canSelectMine || showCompare) {
                                    Spacer(Modifier.height(12.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        if (canSelectMine) {
                                            OutlinedButton(
                                                onClick = { onSetMine(role) },
                                                shape = RoundedCornerShape(12.dp),
                                                colors = ButtonDefaults.outlinedButtonColors(
                                                    contentColor = greenDark
                                                ),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Icon(
                                                    Icons.Filled.School,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Spacer(Modifier.width(5.dp))
                                                Text(
                                                    if (role.isMine)
                                                        "نقش من"
                                                    else
                                                        "انتخاب نقش من"
                                                )
                                            }
                                        }

                                        if (showCompare) {
                                            OutlinedButton(
                                                onClick = { onCompare(role) },
                                                shape = RoundedCornerShape(12.dp),
                                                colors = ButtonDefaults.outlinedButtonColors(
                                                    contentColor = ink
                                                ),
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text("مقایسه نقش")
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
