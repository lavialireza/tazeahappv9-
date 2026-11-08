
package com.example.bookapp.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bookapp.data.Prefs
import com.example.bookapp.ui.theme.colorSchemeFor
import androidx.compose.ui.platform.LocalContext

data class MultiRoleSectionContent(
    val id: Long,
    val title: String,
    val content: String
)

data class MultiRoleRoleContent(
    val roleTitle: String,
    val sections: List<MultiRoleSectionContent>
)

@Composable
fun MultiRoleDialogueScreen(
    taziehTitle: String,
    roles: List<MultiRoleRoleContent>,
    darkMode: Boolean,
    onDarkModeChange: (Boolean) -> Unit,
    fontScale: Float,
    onFontScaleChange: (Float) -> Unit,
    themeChoice: String,
    onThemeChoiceChange: (String) -> Unit,
    fontChoice: String,
    onFontChoiceChange: (String) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current

    var lineSpacing by remember {
        mutableFloatStateOf(Prefs.getLineSpacing(context))
    }

    var showFontMenu by remember {
        mutableStateOf(false)
    }

    val selectedFontFamily = when (fontChoice) {
        "serif" -> FontFamily.Serif
        "sans" -> FontFamily.SansSerif
        "cursive" -> FontFamily.Cursive
        else -> FontFamily.Default
    }

    MaterialTheme(
        colorScheme = colorSchemeFor(themeChoice, darkMode)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(onClick = onBack) {
                        Text("بازگشت")
                    }

                    Text(
                        text = "مکالمه چندنقشی",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Text(
                    text = taziehTitle,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("حالت شب")
                            Switch(
                                checked = darkMode,
                                onCheckedChange = onDarkModeChange
                            )
                        }

                        Spacer(Modifier.height(4.dp))

                        Text("اندازه متن")

                        Slider(
                            value = fontScale.coerceIn(0.8f, 1.6f),
                            onValueChange = onFontScaleChange,
                            valueRange = 0.8f..1.6f
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("قلم متن")
                            Spacer(Modifier.weight(1f))

                            TextButton(
                                onClick = { showFontMenu = true }
                            ) {
                                Text(
                                    when (fontChoice) {
                                        "serif" -> "سریف"
                                        "sans" -> "ساده"
                                        "cursive" -> "تحریری"
                                        else -> "پیش‌فرض"
                                    }
                                )
                            }

                            DropdownMenu(
                                expanded = showFontMenu,
                                onDismissRequest = {
                                    showFontMenu = false
                                }
                            ) {
                                listOf(
                                    "titr" to "پیش‌فرض",
                                    "serif" to "سریف",
                                    "sans" to "ساده",
                                    "cursive" to "تحریری"
                                ).forEach { (value, label) ->
                                    DropdownMenuItem(
                                        text = { Text(label) },
                                        onClick = {
                                            onFontChoiceChange(value)
                                            showFontMenu = false
                                        }
                                    )
                                }
                            }
                        }

                        Text("فاصله خطوط")

                        Slider(
                            value = lineSpacing.coerceIn(1f, 2f),
                            onValueChange = {
                                lineSpacing = it
                                Prefs.setLineSpacing(context, it)
                            },
                            valueRange = 1f..2f
                        )

                        Text("رنگ برنامه")

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            TextButton(
                                onClick = {
                                    onThemeChoiceChange("default")
                                }
                            ) {
                                Text("طلایی")
                            }

                            TextButton(
                                onClick = {
                                    onThemeChoiceChange("green")
                                }
                            ) {
                                Text("سبز")
                            }

                            TextButton(
                                onClick = {
                                    onThemeChoiceChange("red")
                                }
                            ) {
                                Text("قرمز")
                            }
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))

                if (roles.isEmpty()) {
                    Text(
                        text = "متنی برای نقش‌های انتخاب‌شده پیدا نشد.",
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        itemsIndexed(
                            roles,
                            key = { index, role -> "${index}_${role.roleTitle}" }
                        ) { index, role ->

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor =
                                        if (index % 2 == 0)
                                            MaterialTheme.colorScheme.surfaceVariant
                                        else
                                            MaterialTheme.colorScheme.surface
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp)
                                ) {
                                    Text(
                                        text = role.roleTitle,
                                        style = MaterialTheme.typography.titleLarge,
                                        color = MaterialTheme.colorScheme.primary
                                    )

                                    Spacer(Modifier.height(8.dp))

                                    if (role.sections.isEmpty()) {
                                        Text("برای این نقش بخشی ثبت نشده است.")
                                    }

                                    role.sections.forEach { section ->
                                        if (section.title.isNotBlank()) {
                                            Text(
                                                text = section.title,
                                                style = MaterialTheme.typography.titleSmall,
                                                color = MaterialTheme.colorScheme.secondary
                                            )
                                            Spacer(Modifier.height(4.dp))
                                        }

                                        Text(
                                            text = section.content,
                                            fontFamily = selectedFontFamily,
                                            fontSize = (20f * fontScale).sp,
                                            lineHeight = (
                                                20f * fontScale * lineSpacing
                                            ).sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )

                                        Spacer(Modifier.height(14.dp))
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
