package com.example.bookapp.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import com.example.bookapp.data.SearchResult

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainMenuScreen(
    randomVerse: SearchResult?,
    recentItems: List<SearchResult>,
    onOpenActivityCenter: () -> Unit,
    onOpenTaziehList: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenMyRole: () -> Unit,
    onItemClick: (SearchResult) -> Unit,
    featureEnabled: (String) -> Boolean = { true }
) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("تعزیه و شبیه‌خوانی") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // کتابخانه، مقصد اصلی برنامه است و عمداً برجسته‌تر از سایر امکانات نمایش داده می‌شود.
            Card(
                modifier = Modifier.fillMaxWidth().clickable(enabled = featureEnabled("read.view")) {
                    if (featureEnabled("read.view")) onOpenTaziehList()
                },
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(42.dp))
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text("کتابخانه تعزیه", style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Spacer(Modifier.height(4.dp))
                        Text("زمینه‌ها، تعزیه‌ها، نسخه‌ها، نقش‌ها و متن مجالس",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
            }

            if (randomVerse != null) {
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { onItemClick(randomVerse) },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(Modifier.padding(14.dp)) {
                        Text("بیت امروز", style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(5.dp))
                        Text(randomVerse.sectionTitle, style = MaterialTheme.typography.bodyLarge,
                            fontStyle = FontStyle.Italic)
                        Text("${randomVerse.taziehTitle} ← ${randomVerse.roleTitle}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (featureEnabled("search.basic")) {
                    QuickActionCard("جستجو", Icons.Filled.Search, onOpenSearch, Modifier.weight(1f))
                }
                if (featureEnabled("myRole.view")) {
                    QuickActionCard("نقش من", Icons.Filled.School, onOpenMyRole, Modifier.weight(1f))
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth().clickable(onClick = onOpenActivityCenter),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Storage, contentDescription = null)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("مرکز فعالیت‌ها", style = MaterialTheme.typography.titleMedium)
                        Text("پژوهش، اجرا، مدیریت و ابزارهای برنامه",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text("ورود", color = MaterialTheme.colorScheme.primary)
                }
            }

            if (recentItems.isNotEmpty()) {
                Text("ادامه مطالعه و موارد اخیر", style = MaterialTheme.typography.titleSmall)
                recentItems.take(3).forEach { item ->
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable { onItemClick(item) },
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.History, contentDescription = null)
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(item.sectionTitle, style = MaterialTheme.typography.bodyLarge)
                                Text("${item.taziehTitle} ← ${item.roleTitle}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}


@Composable
private fun QuickActionCard(title: String, icon: ImageVector, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(modifier = modifier.clickable(onClick = onClick), shape = RoundedCornerShape(14.dp)) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null)
            Spacer(Modifier.width(9.dp))
            Text(title, style = MaterialTheme.typography.titleSmall)
        }
    }
}

