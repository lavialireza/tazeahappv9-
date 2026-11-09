
package com.example.bookapp.ui.screens

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.NavigateBefore
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RemoveRedEye
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import com.example.bookapp.data.SectionEntity
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RehearsalScreen(
    roleTitle: String,
    sections: List<SectionEntity>,
    startIndex: Int = 0,
    onBack: () -> Unit
) {
    val context = LocalContext.current

    // شناسه نقش برای جداسازی پیشرفت نقش‌های مختلف
    val roleId = sections.firstOrNull()?.roleId
    val progressPrefs = remember(roleId) {
        context.getSharedPreferences(
            "rehearsal_progress",
            Context.MODE_PRIVATE
        )
    }

    val progressKey = roleId?.toString() ?: roleTitle

    // بازیابی آخرین بخش تمرین‌شده
    var currentIndex by remember(progressKey, sections.size) {
        mutableStateOf(
            progressPrefs.getInt(
                "last_index_$progressKey",
                startIndex
            ).coerceIn(
                0,
                (sections.size - 1).coerceAtLeast(0)
            )
        )
    }

    // بازیابی بخش‌های حفظ‌شده
    var learnedSectionIds by remember(progressKey) {
        mutableStateOf(
            progressPrefs.getStringSet(
                "learned_sections_$progressKey",
                emptySet()
            )?.toSet() ?: emptySet()
        )
    }

    // ذخیره آخرین موقعیت تمرین
    LaunchedEffect(progressKey, currentIndex) {
        progressPrefs.edit()
            .putInt("last_index_$progressKey", currentIndex)
            .apply()
    }

    var revealedLines by remember(currentIndex) {
        mutableStateOf(0)
    }

    var autoPlay by remember {
        mutableStateOf(false)
    }

    var autoSpeedSeconds by remember {
        mutableStateOf(3)
    }

    val section = sections.getOrNull(currentIndex)

    val lines = remember(section) {
        section?.content
            ?.split("\n")
            ?.map { it.trimEnd('\r') }
            ?.filter { it.isNotBlank() }
            ?: emptyList()
    }

    val textScrollState = rememberScrollState()

    // تشخیص روشن یا تاریک بودن پوسته برنامه
    val isLightTheme =
        MaterialTheme.colorScheme.background.luminance() > 0.5f

    val pageBackground = if (isLightTheme) {
        Color(0xFFF2F7EF)
    } else {
        MaterialTheme.colorScheme.background
    }

    val cardBackground = if (isLightTheme) {
        Color(0xFFFFFEFA)
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }

    val softGreen = Color(0xFFE5EFE0)
    val darkGreen = Color(0xFF55775B)
    val gold = Color(0xFFB79B55)
    val softBorder = Color(0xFFDCE8D8)

    val learnedCount = sections.count {
        it.id.toString() in learnedSectionIds
    }

    val totalProgress = if (sections.isNotEmpty()) {
        learnedCount.toFloat() / sections.size.toFloat()
    } else {
        0f
    }

    val currentSectionKey = section?.id?.toString()
    val isCurrentLearned =
        currentSectionKey != null &&
            currentSectionKey in learnedSectionIds

    // خودخوان: آشکارکردن خط‌ها با فاصله زمانی انتخاب‌شده
    // پس از آخرین خط متوقف می‌شود و به بخش بعد نمی‌رود.
    LaunchedEffect(
        autoPlay,
        currentIndex,
        autoSpeedSeconds,
        lines.size
    ) {
        if (!autoPlay) return@LaunchedEffect

        if (lines.isEmpty()) {
            autoPlay = false
            return@LaunchedEffect
        }

        while (revealedLines < lines.size) {
            delay(autoSpeedSeconds * 1000L)

            if (revealedLines < lines.size) {
                revealedLines =
                    (revealedLines + 1).coerceAtMost(lines.size)
            }
        }

        autoPlay = false
    }

    // اسکرول خودکار هنگام آشکارشدن متن
    LaunchedEffect(currentIndex, revealedLines) {
        if (revealedLines == 0) {
            textScrollState.animateScrollTo(0)
        } else {
            withFrameNanos { }

            textScrollState.animateScrollTo(
                textScrollState.maxValue
            )
        }
    }

    Scaffold(
        containerColor = pageBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "تمرین: $roleTitle",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "بازگشت"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = pageBackground
                )
            )
        }
    ) { padding ->

        if (section == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("بخشی برای تمرین وجود ندارد")
            }

            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(
                    horizontal = 12.dp,
                    vertical = 8.dp
                )
        ) {

            // عنوان بخش و وضعیت حفظ‌شدن
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        "بخش ${currentIndex + 1} از ${sections.size}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(Modifier.height(3.dp))

                    Text(
                        section.title,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (isLightTheme) {
                            Color(0xFF344C3A)
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                    )
                }

                Spacer(Modifier.width(6.dp))

                OutlinedButton(
                    onClick = {
                        val key = currentSectionKey
                            ?: return@OutlinedButton

                        val updated = if (key in learnedSectionIds) {
                            learnedSectionIds - key
                        } else {
                            learnedSectionIds + key
                        }

                        learnedSectionIds = updated

                        progressPrefs.edit()
                            .putStringSet(
                                "learned_sections_$progressKey",
                                updated
                            )
                            .apply()
                    },
                    contentPadding = PaddingValues(
                        horizontal = 8.dp,
                        vertical = 5.dp
                    ),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(
                        1.dp,
                        if (isCurrentLearned) darkGreen else softBorder
                    ),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = if (isCurrentLearned) {
                            darkGreen
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                    )
                ) {
                    if (isCurrentLearned) {
                        Icon(
                            Icons.Filled.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Spacer(Modifier.width(4.dp))

                    Text(
                        if (isCurrentLearned) {
                            "حفظ شده"
                        } else {
                            "حفظ شدم"
                        },
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // پیشرفت حفظ کل نقش
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = cardBackground
                ),
                border = BorderStroke(
                    1.dp,
                    softBorder.copy(alpha = 0.7f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(
                        horizontal = 12.dp,
                        vertical = 8.dp
                    )
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "پیشرفت حفظ نقش",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Text(
                            "$learnedCount از ${sections.size} بخش · ${(totalProgress * 100).toInt()}٪",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isLightTheme) darkGreen
                            else MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(Modifier.height(6.dp))

                    LinearProgressIndicator(
                        progress = { totalProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp),
                        color = if (isLightTheme) gold
                        else MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // چهار کنترل هم‌اندازه خودخوان و سرعت
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = autoPlay,
                    enabled = lines.isNotEmpty(),
                    onClick = {
                        autoPlay = !autoPlay
                    },
                    modifier = Modifier.weight(1.35f),
                    label = {
                        Text(
                            if (autoPlay) "توقف" else "خودخوان",
                            fontSize = 11.sp,
                            maxLines = 1
                        )
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = softGreen,
                        selectedLabelColor = darkGreen
                    )
                )

                listOf(2, 3, 5).forEach { sec ->
                    FilterChip(
                        selected = autoSpeedSeconds == sec,
                        enabled = lines.isNotEmpty(),
                        onClick = {
                            autoSpeedSeconds = sec
                        },
                        modifier = Modifier.weight(1f),
                        label = {
                            Text(
                                "${sec}ث",
                                fontSize = 11.sp,
                                maxLines = 1
                            )
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = softGreen,
                            selectedLabelColor = darkGreen
                        )
                    )
                }
            }

            Spacer(Modifier.height(6.dp))

            // کادر اصلی شعر؛ بیشترین فضای صفحه به این بخش اختصاص دارد
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = cardBackground
                ),
                border = BorderStroke(
                    1.dp,
                    softBorder
                ),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = 1.dp
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(textScrollState)
                        .padding(
                            horizontal = 18.dp,
                            vertical = 16.dp
                        )
                ) {

                    if (lines.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 30.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "متنی برای تمرین این بخش ثبت نشده است.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else if (revealedLines == 0) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "برای شروع، سعی کن خط اول را از حفظ بگویی؛ سپس آن را آشکار کن.",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        lines.take(revealedLines)
                            .forEachIndexed { index, line ->

                                val isLatest =
                                    index == revealedLines - 1

                                AnimatedVisibility(
                                    visible = true,
                                    enter = if (isLatest) {
                                        fadeIn(tween(450)) +
                                            expandVertically(tween(450))
                                    } else {
                                        fadeIn(tween(0))
                                    }
                                ) {
                                    Column {
                                        Text(
                                            text = line,
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            lineHeight = MaterialTheme.typography.bodyLarge.lineHeight
                                        )

                                        Spacer(
                                            Modifier.height(10.dp)
                                        )
                                    }
                                }
                            }
                    }
                }
            }

            Spacer(Modifier.height(7.dp))

            // شمارش خط‌های آشکارشده
            if (lines.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "${revealedLines} از ${lines.size} خط",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        "${(revealedLines * 100) / lines.size}٪",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isLightTheme) gold
                        else MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(Modifier.height(4.dp))

                LinearProgressIndicator(
                    progress = {
                        revealedLines.toFloat() / lines.size.toFloat()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp),
                    color = if (isLightTheme) gold
                    else MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }

            Spacer(Modifier.height(8.dp))

            // دکمه‌های کوچک‌تر تمرین
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        autoPlay = false
                        revealedLines = 0
                    },
                    enabled = revealedLines > 0,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 38.dp),
                    contentPadding = PaddingValues(
                        horizontal = 4.dp,
                        vertical = 5.dp
                    ),
                    shape = RoundedCornerShape(11.dp)
                ) {
                    Icon(
                        Icons.Filled.Replay,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )

                    Spacer(Modifier.width(3.dp))

                    Text("پنهان", fontSize = 11.sp)
                }

                Button(
                    onClick = {
                        if (revealedLines < lines.size) {
                            revealedLines++
                        }
                    },
                    enabled = revealedLines < lines.size,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 38.dp),
                    contentPadding = PaddingValues(
                        horizontal = 4.dp,
                        vertical = 5.dp
                    ),
                    shape = RoundedCornerShape(11.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = darkGreen,
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        Icons.Filled.RemoveRedEye,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )

                    Spacer(Modifier.width(3.dp))

                    Text("خط بعد", fontSize = 11.sp)
                }

                OutlinedButton(
                    onClick = {
                        autoPlay = false
                        revealedLines = lines.size
                    },
                    enabled = lines.isNotEmpty() &&
                        revealedLines < lines.size,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 38.dp),
                    contentPadding = PaddingValues(
                        horizontal = 4.dp,
                        vertical = 5.dp
                    ),
                    shape = RoundedCornerShape(11.dp)
                ) {
                    Icon(
                        Icons.Filled.Visibility,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )

                    Spacer(Modifier.width(3.dp))

                    Text("نمایش کامل", fontSize = 10.sp)
                }
            }

            Spacer(Modifier.height(3.dp))

            // حرکت بین بخش‌ها
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(
                    onClick = {
                        autoPlay = false
                        if (currentIndex > 0) {
                            currentIndex--
                        }
                    },
                    enabled = currentIndex > 0,
                    contentPadding = PaddingValues(
                        horizontal = 8.dp,
                        vertical = 2.dp
                    )
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.NavigateBefore,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )

                    Text("بخش قبل", fontSize = 12.sp)
                }

                TextButton(
                    onClick = {
                        autoPlay = false
                        if (currentIndex < sections.size - 1) {
                            currentIndex++
                        }
                    },
                    enabled = currentIndex < sections.size - 1,
                    contentPadding = PaddingValues(
                        horizontal = 8.dp,
                        vertical = 2.dp
                    )
                ) {
                    Text("بخش بعد", fontSize = 12.sp)

                    Icon(
                        Icons.AutoMirrored.Filled.NavigateNext,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
