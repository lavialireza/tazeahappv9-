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
                                    sectionB?.section?.title.orEmpty()
                                },
                                style = MaterialTheme.typography.titleSmall
                            )

                            Text(rightText)
                        }
                    }
                }
                }
            }
        }
    }
}

