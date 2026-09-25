package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.MonospaceCodeStyle

@Composable
fun MarkdownViewer(
    markdown: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isDark = isSystemInDarkTheme()
    val lines = markdown.split("\n")

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        var inCodeBlock = false
        val codeBuffer = StringBuilder()
        var codeBlockLang = ""

        var lineIndex = 0
        while (lineIndex < lines.size) {
            val line = lines[lineIndex]
            val trimmed = line.trim()

            if (trimmed.startsWith("```")) {
                if (inCodeBlock) {
                    // Close code block and render
                    val codeContent = codeBuffer.toString().trimEnd()
                    Card(
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isDark) Color(0xFF161B22) else Color(0xFFF6F8FA)
                        ),
                        border = CardDefaults.outlinedCardBorder(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = codeBlockLang.ifEmpty { "code" },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Medium
                                )
                                IconButton(
                                    onClick = {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        clipboard.setPrimaryClip(ClipData.newPlainText("code", codeContent))
                                    },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.ContentCopy,
                                        contentDescription = "Copy code block",
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            val scrollState = rememberScrollState()
                            Text(
                                text = codeContent,
                                style = MonospaceCodeStyle,
                                modifier = Modifier.horizontalScroll(scrollState)
                            )
                        }
                    }
                    codeBuffer.clear()
                    inCodeBlock = false
                } else {
                    inCodeBlock = true
                    codeBlockLang = trimmed.removePrefix("```").trim()
                }
            } else if (inCodeBlock) {
                codeBuffer.append(line).append("\n")
            } else {
                when {
                    trimmed.startsWith("# ") -> {
                        Text(
                            text = trimmed.removePrefix("# "),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        HorizontalDivider(thickness = 0.5.dp)
                    }
                    trimmed.startsWith("## ") -> {
                        Text(
                            text = trimmed.removePrefix("## "),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        HorizontalDivider(thickness = 0.5.dp)
                    }
                    trimmed.startsWith("### ") -> {
                        Text(
                            text = trimmed.removePrefix("### "),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    trimmed.startsWith("#### ") -> {
                        Text(
                            text = trimmed.removePrefix("#### "),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    trimmed.startsWith("> ") -> {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(4.dp)
                                    .height(24.dp)
                                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(2.dp))
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = trimmed.removePrefix("> "),
                                style = MaterialTheme.typography.bodyMedium,
                                fontStyle = FontStyle.Italic,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    trimmed.startsWith("- ") || trimmed.startsWith("* ") -> {
                        Row(
                            modifier = Modifier.padding(start = 8.dp, top = 2.dp, bottom = 2.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Text("• ", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            Text(
                                text = parseInlineMarkdown(trimmed.substring(2)),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                    trimmed.matches(Regex("^\\d+\\.\\s.*")) -> {
                        val dotIndex = trimmed.indexOf('.')
                        val num = trimmed.substring(0, dotIndex + 1)
                        val content = trimmed.substring(dotIndex + 1).trim()
                        Row(
                            modifier = Modifier.padding(start = 8.dp, top = 2.dp, bottom = 2.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Text("$num ", fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.primary)
                            Text(
                                text = parseInlineMarkdown(content),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                    trimmed.startsWith("|") && trimmed.endsWith("|") -> {
                        // Table row
                        val cells = trimmed.split("|").filter { it.isNotBlank() }.map { it.trim() }
                        if (!cells.all { it.matches(Regex("^-+$")) }) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(if (isDark) Color(0xFF161B22) else Color(0xFFF6F8FA))
                                    .padding(vertical = 6.dp, horizontal = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                cells.forEach { cell ->
                                    Text(
                                        text = parseInlineMarkdown(cell),
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                    trimmed.isBlank() -> {
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                    else -> {
                        Text(
                            text = parseInlineMarkdown(line),
                            style = MaterialTheme.typography.bodyMedium,
                            lineHeight = 22.sp
                        )
                    }
                }
            }
            lineIndex++
        }
    }
}

fun parseInlineMarkdown(text: String): androidx.compose.ui.text.AnnotatedString {
    return buildAnnotatedString {
        var i = 0
        while (i < text.length) {
            when {
                // Bold **text**
                text.startsWith("**", i) -> {
                    val end = text.indexOf("**", i + 2)
                    if (end != -1) {
                        pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                        append(text.substring(i + 2, end))
                        pop()
                        i = end + 2
                    } else {
                        append(text[i])
                        i++
                    }
                }
                // Inline code `code`
                text.startsWith("`", i) -> {
                    val end = text.indexOf("`", i + 1)
                    if (end != -1) {
                        pushStyle(
                            SpanStyle(
                                fontFamily = FontFamily.Monospace,
                                background = Color(0x33888888),
                                fontSize = 12.sp
                            )
                        )
                        append(" " + text.substring(i + 1, end) + " ")
                        pop()
                        i = end + 1
                    } else {
                        append(text[i])
                        i++
                    }
                }
                // Italic *text*
                text.startsWith("*", i) -> {
                    val end = text.indexOf("*", i + 1)
                    if (end != -1) {
                        pushStyle(SpanStyle(fontStyle = FontStyle.Italic))
                        append(text.substring(i + 1, end))
                        pop()
                        i = end + 1
                    } else {
                        append(text[i])
                        i++
                    }
                }
                else -> {
                    append(text[i])
                    i++
                }
            }
        }
    }
}
