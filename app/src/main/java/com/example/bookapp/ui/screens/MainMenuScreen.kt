
package com.example.bookapp.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

// رنگ‌های اصلی صفحه
private val HomeBackground = Color(0xFFF5F8F4)
private val SpringGreen = Color(0xFFE5F1E5)
private val SpringGreenDark = Color(0xFF426B50)

private val NavyBackground = Color(0xFFE8EDF5)
private val NavyDark = Color(0xFF354B68)

private val GoldBackground = Color(0xFFF2EDDF)
private val GoldDark = Color(0xFF806A3D)

private val BlueBackground = Color(0xFFE5EFF5)
private val BlueDark = Color(0xFF45677D)

private val RoseBackground = Color(0xFFF5E9E9)
private val RoseDark = Color(0xFF955D5D)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainMenuScreen(
    onOpenLibrary: () -> Unit,
    onOpenActivityCenter: () -> Unit,
    onOpenNotes: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenBookmarks: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenAbout: () -> Unit,
    onExit: () -> Unit
) {
    var showExitDialog by remember { mutableStateOf(false) }

    val smallItems = listOf(
        HomeMenuItem(
            title = "دفتر یادداشت",
            icon = Icons.Filled.Edit,
            background = SpringGreen,
            foreground = SpringGreenDark,
            onClick = onOpenNotes
        ),
        HomeMenuItem(
            title = "جستجو",
            icon = Icons.Filled.Search,
            background = BlueBackground,
            foreground = BlueDark,
            onClick = onOpenSearch
        ),
        HomeMenuItem(
            title = "علاقه‌مندی‌ها",
            icon = Icons.Filled.Favorite,
            background = RoseBackground,
            foreground = RoseDark,
            onClick = onOpenBookmarks
        ),
        HomeMenuItem(
            title = "تنظیمات",
            icon = Icons.Filled.Settings,
            background = NavyBackground,
            foreground = NavyDark,
            onClick = onOpenSettings
        )
    )

    Scaffold(
        containerColor = HomeBackground,

        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "تعزیه و شبیه‌خوانی",
                            fontWeight = FontWeight.Bold,
                            color = NavyDark,
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = "میراثی برای شناخت، پژوهش و ماندگاری",
                            color = Color(0xFF788477),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = HomeBackground
                )
            )
        },

        bottomBar = {
            Surface(
                color = HomeBackground,
                shadowElevation = 5.dp
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    TextButton(
                        onClick = { showExitDialog = true },
                        modifier = Modifier
                            .height(42.dp)
                    ) {
                        Text(
                            text = "خروج از برنامه",
                            color = RoseDark,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(top = 10.dp, bottom = 18.dp),

            verticalArrangement = Arrangement.spacedBy(13.dp)
        ) {

            // کارت اول: کتابخانه
            HomeFeatureCard(
                title = "کتابخانه تعزیه",
                subtitle = "مجموعه تعزیه‌ها، نسخه‌ها و متن مجالس",
                icon = Icons.AutoMirrored.Filled.MenuBook,
                background = GoldBackground,
                foreground = GoldDark,
                onClick = onOpenLibrary
            )

            // کارت دوم: مرکز فعالیت‌ها
            HomeFeatureCard(
                title = "مرکز فعالیت‌ها",
                subtitle = "پژوهش، اجرا، مدیریت گروه و فعالیت‌های تخصصی",
                icon = Icons.Filled.Apps,
                background = SpringGreen,
                foreground = SpringGreenDark,
                onClick = onOpenActivityCenter
            )

            // کارت سوم: درباره برنامه
            HomeFeatureCard(
                title = "درباره برنامه",
                subtitle = "معرفی سامانه، اهداف و اطلاعات برنامه",
                icon = Icons.Filled.Info,
                background = NavyBackground,
                foreground = NavyDark,
                onClick = onOpenAbout
            )

            // عنوان بخش ابزارها
            Text(
                text = "ابزارهای کاربردی",
                color = NavyDark,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(
                    top = 5.dp,
                    bottom = 1.dp,
                    start = 4.dp
                )
            )

            // ردیف اول کارت‌های متوسط
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                HomeSmallCard(
                    item = smallItems[0],
                    modifier = Modifier.weight(1f)
                )

                HomeSmallCard(
                    item = smallItems[1],
                    modifier = Modifier.weight(1f)
                )
            }

            // ردیف دوم کارت‌های متوسط
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                HomeSmallCard(
                    item = smallItems[2],
                    modifier = Modifier.weight(1f)
                )

                HomeSmallCard(
                    item = smallItems[3],
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }

    // پنجره تأیید خروج
    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = {
                showExitDialog = false
            },

            title = {
                Text(
                    text = "خروج از برنامه",
                    color = NavyDark,
                    fontWeight = FontWeight.Bold
                )
            },

            text = {
                Text(
                    text = "آیا مطمئن هستید که می‌خواهید از برنامه خارج شوید؟"
                )
            },

            confirmButton = {
                TextButton(
                    onClick = {
                        showExitDialog = false
                        onExit()
                    }
                ) {
                    Text(
                        text = "بله، خروج",
                        color = RoseDark,
                        fontWeight = FontWeight.Bold
                    )
                }
            },

            dismissButton = {
                TextButton(
                    onClick = {
                        showExitDialog = false
                    }
                ) {
                    Text(
                        text = "انصراف",
                        color = NavyDark
                    )
                }
            },

            shape = RoundedCornerShape(22.dp),
            containerColor = Color.White
        )
    }
}

private data class HomeMenuItem(
    val title: String,
    val icon: ImageVector,
    val background: Color,
    val foreground: Color,
    val onClick: () -> Unit
)

// کارت‌های بزرگ
@Composable
private fun HomeFeatureCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    background: Color,
    foreground: Color,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 96.dp)
            .clickable(onClick = onClick),

        shape = RoundedCornerShape(22.dp),

        colors = CardDefaults.cardColors(
            containerColor = background
        ),

        elevation = CardDefaults.cardElevation(
            defaultElevation = 3.dp
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 17.dp, vertical = 17.dp),

            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(15.dp)
        ) {

            // کادر دایره‌ای آیکون
            Surface(
                shape = CircleShape,
                color = Color.White.copy(alpha = 0.72f),
                modifier = Modifier.size(53.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = foreground,
                        modifier = Modifier.size(29.dp)
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Text(
                    text = title,
                    color = foreground,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )

                Text(
                    text = subtitle,
                    color = foreground.copy(alpha = 0.85f),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

// کارت‌های متوسط
@Composable
private fun HomeSmallCard(
    item: HomeMenuItem,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .heightIn(min = 108.dp)
            .clickable(onClick = item.onClick),

        shape = RoundedCornerShape(20.dp),

        colors = CardDefaults.cardColors(
            containerColor = item.background
        ),

        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 15.dp, vertical = 15.dp),

            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(11.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = Color.White.copy(alpha = 0.75f),
                modifier = Modifier.size(39.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.title,
                        tint = item.foreground,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Text(
                text = item.title,
                color = item.foreground,
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.titleSmall
            )
        }
    }
}
