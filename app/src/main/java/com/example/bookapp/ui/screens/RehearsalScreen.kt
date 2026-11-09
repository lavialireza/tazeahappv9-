
package com.example.bookapp.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.NavigateBefore
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material.icons.filled.RemoveRedEye
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.bookapp.data.SectionEntity
import kotlinx.coroutines.delay

/**
 * حالت تمرین برای حفظ‌کردن متن یک نقش.
 *
 * ابتدا فقط عنوان بخش دیده می‌شود.
 * با لمس «خط بعد» یا فعال‌کردن خودخوان،
 * خط‌های شعر به‌تدریج آشکار می‌شوند.
 *
 * هنگام آشکارشدن خط جدید، متن به‌صورت خودکار اسکرول می‌شود.
 */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RehearsalScreen(
    roleTitle: String,
    sections: List<SectionEntity>,
    startIndex: Int = 0,
    onBack: () -> Unit
) {
    var currentIndex by remember {
        mutableStateOf(
            startIndex.coerceIn(
                0,
                (sections.size - 1).coerceAtLeast(0)
            )
        )
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

    // وضعیت اسکرول کادر متن
    val textScrollState = rememberScrollState()

    // خودخوان: آشکارکردن تدریجی خط‌ها
    // پس از آشکارشدن آخرین خط، خودخوان متوقف می‌شود.
    // انتقال خودکار به بخش بعد انجام نمی‌شود.
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

        // پایان تمرین خودخوان در همین بخش
        autoPlay = false
    }

    // اسکرول خودکار پس از آشکارشدن هر خط
    // هنگام پنهان‌کردن متن، به ابتدای کادر برمی‌گردیم.
    LaunchedEffect(currentIndex, revealedLines) {
        if (revealedLines == 0) {
            textScrollState.animateScrollTo(0)
        } else {
            // فرصت می‌دهیم خط جدید ابتدا در صفحه رسم شود.
            withFrameNanos { }

            // سپس به انتهای متن آشکارشده می‌رویم.
            textScrollState.animateScrollTo(
                textScrollState.maxValue
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("تمرین: $roleTitle")
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
                .padding(16.dp)
        ) {

            // شماره بخش جاری
            Text(
                "بخش ${currentIndex + 1} از ${sections.size}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(4.dp))

            // عنوان بخش
            Text(
                section.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(8.dp))

            // تنظیمات خودخوان و سرعت
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {

                FilterChip(
                    selected = autoPlay,
                    enabled = lines.isNotEmpty(),
                    onClick = {
                        autoPlay = !autoPlay
                    },
                    label = {
                        Text(
                            if (autoPlay) {
                                "⏸ توقف خودخوان"
                            } else {
                                "▶ حالت خودخوان"
                            }
                        )
                    }
                )

                listOf(2, 3, 5).forEach { sec ->

                    FilterChip(
                        selected = autoSpeedSeconds == sec,
                        enabled = lines.isNotEmpty(),
                        onClick = {
                            autoSpeedSeconds = sec
                        },
                        label = {
                            Text("${sec}ث")
                        }
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // کادر نمایش متن
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(textScrollState)
                        .padding(16.dp)
                ) {

                    if (lines.isEmpty()) {

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "متنی برای تمرین این بخش ثبت نشده است.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                    } else if (revealedLines == 0) {

                        Text(
                            "برای شروع، سعی کن خط اول را از حفظ بگویی، سپس آن را آشکار کن.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

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
                                            style = MaterialTheme.typography.bodyLarge
                                        )

                                        Spacer(
                                            Modifier.height(6.dp)
                                        )
                                    }
                                }
                            }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // شمارش و پیشرفت آشکارسازی خط‌ها
            if (lines.isNotEmpty()) {

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "${revealedLines} از ${lines.size} خط آشکار شده",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        "${(revealedLines * 100) / lines.size}٪",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(Modifier.height(6.dp))

                LinearProgressIndicator(
                    progress = {
                        revealedLines.toFloat() / lines.size.toFloat()
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(12.dp))
            }

            // دکمه‌های کنترل آشکارسازی
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {

                OutlinedButton(
                    onClick = {
                        autoPlay = false
                        revealedLines = 0
                    },
                    enabled = revealedLines > 0,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        Icons.Filled.Replay,
                        contentDescription = null
                    )

                    Spacer(Modifier.width(6.dp))

                    Text("پنهان کن")
                }

                Button(
                    onClick = {
                        if (revealedLines < lines.size) {
                            revealedLines++
                        }
                    },
                    enabled = revealedLines < lines.size,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        Icons.Filled.RemoveRedEye,
                        contentDescription = null
                    )

                    Spacer(Modifier.width(6.dp))

                    Text("خط بعد")
                }

                OutlinedButton(
                    onClick = {
                        autoPlay = false
                        revealedLines = lines.size
                    },
                    enabled = lines.isNotEmpty() &&
                        revealedLines < lines.size,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        Icons.Filled.Visibility,
                        contentDescription = null
                    )

                    Spacer(Modifier.width(6.dp))

                    Text("نمایش کامل")
                }
            }

            Spacer(Modifier.height(8.dp))

            // جابه‌جایی بین بخش‌ها
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
                    enabled = currentIndex > 0
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.NavigateBefore,
                        contentDescription = null
                    )

                    Text("بخش قبل")
                }

                TextButton(
                    onClick = {
                        autoPlay = false
                        if (currentIndex < sections.size - 1) {
                            currentIndex++
                        }
                    },
                    enabled = currentIndex < sections.size - 1
                ) {
                    Text("بخش بعد")

                    Icon(
                        Icons.AutoMirrored.Filled.NavigateNext,
                        contentDescription = null
                    )
                }
            }
        }
    }
}
