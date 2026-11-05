package com.example.bookapp.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.bookapp.data.SearchResult

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryHomeScreen(
    randomVerse: SearchResult?,
    recentItems: List<SearchResult>,
    onOpenTaziehList: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenMyRole: () -> Unit,
    onOpenRehearsal: () -> Unit,
    onOpenGlossary: () -> Unit,
    onOpenGallery: () -> Unit,
    onOpenCompareGuide: () -> Unit,
    onItemClick: (SearchResult) -> Unit,
    showSearch: Boolean = true,
    showMyRole: Boolean = true,
    showRehearsal: Boolean = true,
    showGlossary: Boolean = true,
    showGallery: Boolean = true,
    showCompare: Boolean = true,
    onBack: () -> Unit
) {
    Scaffold(topBar = {
        TopAppBar(title = { Text("کتابخانه تعزیه") }, navigationIcon = {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "بازگشت") }
        })
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (randomVerse != null) {
                Card(Modifier.fillMaxWidth().clickable { onItemClick(randomVerse) }, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer), shape = RoundedCornerShape(18.dp)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("بیت امروز", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimaryContainer, fontWeight = FontWeight.Bold)
                        Text(randomVerse.sectionTitle, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Text("${randomVerse.taziehTitle} ← ${randomVerse.roleTitle}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
            }
            if (showSearch) OutlinedTextField(
                value = "",
                onValueChange = {},
                modifier = Modifier.fillMaxWidth().clickable(onClick = onOpenSearch),
                readOnly = true,
                singleLine = true,
                placeholder = { Text("جستجو در کتابخانه، تعزیه‌ها و متن مجالس") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) }
            )
            LibraryActionCard("فهرست زمینه‌ها و تعزیه‌ها", "زمینه ← تعزیه ← نسخه ← نقش ← متن مجلس", Icons.AutoMirrored.Filled.MenuBook, onOpenTaziehList, Color(0xFFE8E1D2))
            Text("ابزارهای مطالعه و پژوهش", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            if (showMyRole) LibraryActionCard("نقش من", "نقش‌های انتخاب‌شده و اطلاعات مرتبط با کاربر", Icons.Filled.School, onOpenMyRole, Color(0xFFDCE9E4))
            if (showRehearsal) LibraryActionCard("حالت تمرین", "ورود به تمرین حفظ متن نقش‌های انتخاب‌شده", Icons.Filled.School, onOpenRehearsal, Color(0xFFE8E1D2))
            if (showGlossary) LibraryActionCard("دیکشنری و فرهنگ اصطلاحات", "جستجو و مطالعه واژه‌ها و اصطلاحات تعزیه", Icons.AutoMirrored.Filled.MenuBook, onOpenGlossary, Color(0xFFE8EDF8))
            if (showGallery) LibraryActionCard("گالری تصاویر", "مشاهده تصاویر مرتبط با مجموعه و مجالس", Icons.Filled.PhotoLibrary, onOpenGallery, Color(0xFFF4E5EC))
            if (showCompare) LibraryActionCard("مقایسه نقش‌ها", "برای انتخاب تعزیه و مقایسه نقش‌های آن وارد فهرست تعزیه‌ها شوید", Icons.Filled.Search, onOpenCompareGuide, Color(0xFFF8EBDD))
            if (recentItems.isNotEmpty()) {
                Text("اخیراً مشاهده‌شده", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                recentItems.forEach { item ->
                    ListItem(
                        headlineContent = { Text(item.sectionTitle) },
                        supportingContent = { Text("${item.taziehTitle} ← ${item.roleTitle}") },
                        leadingContent = { Icon(Icons.Filled.History, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth().clickable { onItemClick(item) }
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun LibraryActionCard(title: String, subtitle: String, icon: ImageVector, onClick: () -> Unit, tint: Color) {
    Card(Modifier.fillMaxWidth().clickable(onClick = onClick), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = tint)) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(27.dp), tint = MaterialTheme.colorScheme.onSurface)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall)
            }
            Icon(Icons.Filled.ChevronLeft, contentDescription = null)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityCenterScreen(
    onOpenResearch: () -> Unit,
    onOpenExecution: () -> Unit,
    onOpenGroups: () -> Unit,
    onOpenManagement: () -> Unit,
    onOpenTools: () -> Unit,
    showAdminSections: Boolean = true,
    onBack: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    val entries = buildList {
        add(CenterEntry("مرکز پژوهش", "مقایسه نسخه‌ها، بررسی متن‌ها، دانشنامه و دستیار هوشمند پژوهش", Icons.Filled.School, Color(0xFFE8EDF8), onOpenResearch, "پژوهش"))
        add(CenterEntry("مرکز اجرا", "برنامه تمرین و اجرا، تخصیص نقش، گفت‌وگوی چندنقشی و اتاق فرمان", Icons.Filled.Event, Color(0xFFDCE9E4), onOpenExecution, "اجرا"))
        if (showAdminSections) {
            add(CenterEntry("مدیریت مستقل گروه", "گروه‌ها، اعضا، شبیه‌خوان‌ها و سوابق", Icons.Filled.People, Color(0xFFF4E5EC), onOpenGroups, "گروه"))
            add(CenterEntry("مرکز مدیریت و نگهداری محتوا", "ورود Word و JSON، خروجی، سلامت محتوا و پشتیبان‌گیری", Icons.Filled.Storage, Color(0xFFF8EBDD), onOpenManagement, "محتوا"))
        }
        add(CenterEntry("ابزار برنامه", "ابزارهای عمومی و امکانات کمکی برنامه", Icons.Filled.Build, Color(0xFFE8ECEE), onOpenTools, "ابزار"))
    }
    val filtered = entries.filter { query.isBlank() || (it.title + " " + it.subtitle + " " + it.keywords).contains(query.trim(), ignoreCase = true) }
    Scaffold(topBar = { TopAppBar(title = { Text("مرکز فعالیت‌ها") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "بازگشت") } }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth(), singleLine = true, label = { Text("جستجوی بخش‌ها و امکانات") }, leadingIcon = { Icon(Icons.Filled.Search, null) })
            filtered.forEach { entry ->
                Card(Modifier.fillMaxWidth().clickable(onClick = entry.onClick), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = entry.tint)) {
                    Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(entry.icon, contentDescription = null, modifier = Modifier.size(30.dp))
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(entry.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(entry.subtitle, style = MaterialTheme.typography.bodySmall)
                        }
                        Icon(Icons.Filled.ChevronLeft, contentDescription = null)
                    }
                }
            }
            if (filtered.isEmpty()) Text("بخشی با این عبارت پیدا نشد.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResearchCenterScreen(
    showVersionComparison: Boolean,
    showAi: Boolean = true,
    showGlossary: Boolean = true,
    showEncyclopedia: Boolean = true,
    showCorrections: Boolean = true,
    onOpenAi: () -> Unit,
    onOpenVersionComparison: () -> Unit,
    onOpenGlossary: () -> Unit,
    onOpenEncyclopedia: () -> Unit,
    onOpenCorrections: () -> Unit,
    onBack: () -> Unit
) {
    Scaffold(topBar = { TopAppBar(title = { Text("مرکز پژوهش") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "بازگشت") } }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("ابزارهای بررسی، مستندسازی و تحلیل متون تعزیه", style = MaterialTheme.typography.bodyMedium)
            if (showAi) ResearchAction("دستیار هوش مصنوعی پژوهش", "تحلیل ساختار، پیشنهاد گوینده، استخراج واژه و جستجوی معنایی آفلاین در آرشیو", Icons.Filled.School, onOpenAi, Color(0xFFE8E1D2))
            if (showVersionComparison) ResearchAction("مقایسه نسخه‌های یک تعزیه", "بررسی اختلاف نسخه‌ها با حفظ متن و ترتیب منبع", Icons.Filled.Search, onOpenVersionComparison, Color(0xFFE8EDF8))
            if (showEncyclopedia) ResearchAction("دانشنامه تعزیه", "مدخل‌ها، شخصیت‌ها، رویدادها و پیوندهای دانشی", Icons.AutoMirrored.Filled.MenuBook, onOpenEncyclopedia, Color(0xFFDCE9E4))
            if (showGlossary) ResearchAction("دیکشنری و اصطلاحات", "واژه‌ها، صورت‌های نوشتاری و معنی‌های ثبت‌شده", Icons.AutoMirrored.Filled.MenuBook, onOpenGlossary, Color(0xFFF8EBDD))
            if (showCorrections) ResearchAction("ابزار تصحیح متن", "بررسی و مدیریت پیشنهادهای اصلاحی بدون تغییر خودکار متن اصلی", Icons.Filled.Edit, onOpenCorrections, Color(0xFFF4E5EC))
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("اصل اعتبار پژوهش", fontWeight = FontWeight.Bold)
                    Text("پیشنهادهای هوشمند جایگزین منبع تاریخی یا تأیید پژوهشگر نیستند. متن اصلی و ترتیب مستند نسخه‌ها بدون اجازه ویرایش نمی‌شوند.", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun ResearchAction(title: String, subtitle: String, icon: ImageVector, onClick: () -> Unit, tint: Color) {
    Card(Modifier.fillMaxWidth().clickable(onClick = onClick), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = tint)) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(28.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall)
            }
            Icon(Icons.Filled.ChevronLeft, contentDescription = null)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExecutionCenterScreen(showAdminActions: Boolean = true, onOpenGroups: () -> Unit, onOpenExecutionManagement: () -> Unit, onBack: () -> Unit) {
    Scaffold(topBar = { TopAppBar(title = { Text("مرکز اجرا") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "بازگشت") } }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("مدیریت چرخه آماده‌سازی، تمرین و اجرای تعزیه", style = MaterialTheme.typography.bodyMedium)
            if (showAdminActions) {
                ResearchAction("مدیریت گروه‌ها و اعضا", "پرونده گروه، اعضا، شبیه‌خوان‌ها و سوابق", Icons.Filled.People, onOpenGroups, Color(0xFFDCE9E4))
                ResearchAction("برنامه تمرین و اجرا", "زمان‌بندی، تخصیص نقش، یادداشت کارگردان و متن اجرای زنده", Icons.Filled.Event, onOpenExecutionManagement, Color(0xFFE8EDF8))
            } else {
                Text("امکانات مدیریتی مرکز اجرا در نسخه مدیر فعال هستند. امکانات مطالعه و تمرین متن از کتابخانه در دسترس‌اند.", style = MaterialTheme.typography.bodyMedium)
            }
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("پرونده اجرای مستند", fontWeight = FontWeight.Bold)
                    Text("هر اجرا باید به نسخه مشخصی از متن متصل باشد تا تغییرات بعدی نسخه، سوابق اجرای قبلی را تغییر ندهد.", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManagementCenterScreen(onOpenContent: () -> Unit, onOpenAccess: () -> Unit, onOpenSpecialUsers: () -> Unit, onOpenAudit: () -> Unit, onBack: () -> Unit) {
    Scaffold(topBar = { TopAppBar(title = { Text("مرکز مدیریت و نگهداری محتوا") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "بازگشت") } }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            ResearchAction("مدیریت محتوای اصلی", "ورود Word و JSON، خروجی‌ها، کنترل سلامت و پشتیبان‌گیری", Icons.Filled.Storage, onOpenContent, Color(0xFFF8EBDD))
            ResearchAction("مدیریت دسترسی Viewer", "تنظیم و انتقال سیاست‌های دسترسی نسخه عمومی", Icons.Filled.Storage, onOpenAccess, Color(0xFFE8EDF8))
            ResearchAction("مدیریت کاربران ویژه", "تعریف و مدیریت کاربران و مجوزهای اختصاصی", Icons.Filled.People, onOpenSpecialUsers, Color(0xFFF4E5EC))
            ResearchAction("سوابق تغییرات و دسترسی", "مشاهده گزارش رویدادها و تغییرات مدیریتی", Icons.Filled.History, onOpenAudit, Color(0xFFDCE9E4))
        }
    }
}

private data class CenterEntry(val title: String, val subtitle: String, val icon: ImageVector, val tint: Color, val onClick: () -> Unit, val keywords: String)


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolsCenterScreen(onOpenCalendar: () -> Unit, onOpenVersion: () -> Unit, onOpenChangelog: () -> Unit, onBack: () -> Unit) {
    Scaffold(topBar = { TopAppBar(title = { Text("ابزار برنامه") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "بازگشت") } }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            ResearchAction("تقویم محرم", "رویدادها و زمان‌شمار مناسبت‌های محرم", Icons.Filled.Event, onOpenCalendar, Color(0xFFE8E1D2))
            ResearchAction("نسخه برنامه", "اطلاعات نسخه نصب‌شده و وضعیت بروزرسانی", Icons.Filled.Info, onOpenVersion, Color(0xFFE8EDF8))
            ResearchAction("چه چیزی جدید است؟", "مشاهده تغییرات و امکانات نسخه‌ها", Icons.Filled.Info, onOpenChangelog, Color(0xFFDCE9E4))
        }
    }
}
