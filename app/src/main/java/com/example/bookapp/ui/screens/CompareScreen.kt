
    
  

                Text(
                    "یکسان: ${diff.same}  |  افزوده: ${diff.added}  |  حذف‌شده: ${diff.removed}",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                if (view == "diff") {

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = granularity == "line",
                            onClick = { granularity = "line" },
                            label = { Text("اختلاف سطری") }
                        )

                        FilterChip(
                            selected = granularity == "word",
                            onClick = { granularity = "word" },
                            label = { Text("اختلاف واژه‌ای") }
                        )
                    }

                    if (granularity == "line") {

                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                        ) {
                            itemsIndexed(diff.lines) { _, line ->

                                val label = when (line.kind) {
                                    DiffKind.SAME -> "="
                                    DiffKind.ADDED -> "+"
                                    DiffKind.REMOVED -> "−"
                                }

                                val color = when (line.kind) {
                                    DiffKind.SAME ->
                                        MaterialTheme.colorScheme.surface

                                    DiffKind.ADDED ->
                                        MaterialTheme.colorScheme.tertiaryContainer

                                    DiffKind.REMOVED ->
                                        MaterialTheme.colorScheme.errorContainer
                                }

                                Row(
                                    Modifier
                                        .fillMaxWidth()
                                        .background(color)
                                        .padding(
                                            horizontal = 8.dp,
                                            vertical = 5.dp
                                        )
                                ) {
                                    Text(
                                        label,
                                        style = MaterialTheme.typography.labelLarge,
                                        modifier = Modifier.width(24.dp)
                                    )

                                    Text(
                                        line.text.ifBlank { " " },
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }

                    } else {

                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .padding(8.dp)
                        ) {
                            itemsIndexed(wordDiff) { _, token ->

                                val color = when (token.change) {
                                    WordChange.SAME ->
                                        MaterialTheme.colorScheme.onSurface

                                    WordChange.ADDED ->
                                        MaterialTheme.colorScheme.primary

                                    WordChange.REMOVED ->
                                        MaterialTheme.colorScheme.error
                                }

                                Text(
                                    token.value,
                                    color = color,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }

                    Text(
                        "راهنما: + افزوده‌شده در متن دوم، − حذف‌شده از متن اول، = یکسان",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 6.dp)
                    )

                } else {

                    Row(
                        Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {

                        Column(
                            Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .verticalScroll(rememberScrollState())
                                .padding(6.dp)
                        ) {
                            Text(
                                if (mode == "role") {
                                    roleA?.title.orEmpty()
                                } else {
                                    sectionA?.section?.title.orEmpty()
                                },
                                style = MaterialTheme.typography.titleSmall
                            )

                            Text(leftText)
                        }

                        Box(
                            Modifier
                                .width(1.dp)
                                .fillMaxHeight()
                                .background(
                                    MaterialTheme.colorScheme.outlineVariant
                                )
                        )

                        Column(
                            Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .background(
                                    MaterialTheme.colorScheme.surfaceVariant
                                )
                                .verticalScroll(rememberScrollState())
                                .padding(6.dp)
                        ) {
                            Text(
                                if (mode == "role") {
                                    roleB?.title.orEmpty()
                                } else {

