package com.example.bookapp.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainMenuScreen(
    onOpenLibrary: () -> Unit,
    onOpenActivityCenter: () -> Unit,
    onOpenNotes: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenBookmarks: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenAbout: () -> Unit
) {
    val items = listOf(
        HomeMenuItem("دفتر یادداشت", Icons.Filled.Edit, Color(0xFFE8EDF8), Color(0xFF344B80), onOpenNotes),
        HomeMenuItem("جستجو", Icons.Filled.Search, Color(0xFFF8EBDD), Color(0xFF89552D), onOpenSearch),
        HomeMenuItem("علاقه‌مندی‌ها", Icons.Filled.Favorite, Color(0xFFF4E5EC), Color(0xFF87415F), onOpenBookmarks),
        HomeMenuItem("تنظیمات", Icons.Filled.Settings, Color(0xFFE8ECEE), Color(0xFF46545B), onOpenSettings),
        HomeMenuItem("درباره ما", Icons.Filled.Info, Color(0xFFF0EADB), Color(0xFF725C35), onOpenAbout)
    )

    Scaffold(topBar = { CenterAlignedTopAppBar(title = { Text("تعزیه و شبیه‌خوانی", fontWeight = FontWeight.Bold) }) }) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            HomeFeatureCard(
                title = "کتابخانه تعزیه",
                subtitle = "دسترسی به مجموعه تعزیه‌ها، نسخه‌ها و متن مجالس",
                icon = Icons.AutoMirrored.Filled.MenuBook,
                background = Color(0xFFE8E1D2),
                foreground = Color(0xFF654B2D),
                onClick = onOpenLibrary,
                prominent = true
            )
            HomeFeatureCard(
                title = "مرکز فعالیت‌ها",
                subtitle = "پژوهش، اجرا، مدیریت گروه و نگهداری محتوا",
                icon = Icons.Filled.Apps,
                background = Color(0xFFDCE9E4),
                foreground = Color(0xFF285C4B),
                onClick = onOpenActivityCenter,
                prominent = true
            )
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items.chunked(2).forEach { rowItems ->
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        rowItems.forEach { item ->
                            HomeSmallCard(item, Modifier.weight(1f))
                        }
                        if (rowItems.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

private data class HomeMenuItem(
    val title: String,
    val icon: ImageVector,
    val background: Color,
    val foreground: Color,
    val onClick: () -> Unit
)

@Composable
private fun HomeFeatureCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    background: Color,
    foreground: Color,
    onClick: () -> Unit,
    prominent: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = background)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(if (prominent) 18.dp else 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Icon(icon, contentDescription = null, tint = foreground, modifier = Modifier.size(if (prominent) 34.dp else 26.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, style = if (prominent) MaterialTheme.typography.titleLarge else MaterialTheme.typography.titleMedium, color = foreground, fontWeight = FontWeight.Bold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = foreground)
            }
        }
    }
}

@Composable
private fun HomeSmallCard(item: HomeMenuItem, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.heightIn(min = 88.dp).clickable(onClick = item.onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = item.background)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Icon(item.icon, contentDescription = null, tint = item.foreground, modifier = Modifier.size(25.dp))
            Text(item.title, color = item.foreground, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        }
    }
}
