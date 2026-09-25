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
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.MonospaceCodeStyle
import com.example.utils.HapticUtils

object SyntaxHighlighter {

    private val KEYWORDS = setOf(
        "fun", "val", "var", "class", "interface", "object", "package", "import",
        "return", "if", "else", "when", "for", "while", "do", "break", "continue",
        "public", "private", "protected", "internal", "override", "open", "final",
        "abstract", "sealed", "data", "inline", "suspend", "constructor", "init",
        "companion", "null", "true", "false", "this", "super", "try", "catch",
        "finally", "throw", "const", "enum", "typealias", "is", "as", "in",
        "function", "let", "const", "def", "async", "await", "export", "default",
        "from", "import", "struct", "void", "int", "float", "double", "char",
        "bool", "boolean", "static", "goto", "case", "switch", "default", "sizeof"
    )

    fun highlight(code: String, isDark: Boolean, searchQuery: String = ""): AnnotatedString {
        val keywordColor = if (isDark) Color(0xFFFF7B72) else Color(0xFFCF222E)
        val stringColor = if (isDark) Color(0xFFA5D6FF) else Color(0xFF0A3069)
        val numberColor = if (isDark) Color(0xFF79C0FF) else Color(0xFF0550AE)
        val commentColor = if (isDark) Color(0xFF8B949E) else Color(0xFF6E7781)
        val searchHighlightColor = Color(0xFFFFD600).copy(alpha = 0.6f)
        val defaultTextColor = if (isDark) Color(0xFFE6EDF3) else Color(0xFF1F2328)

        val lines = code.split("\n")
        return buildAnnotatedString {
            lines.forEachIndexed { lineIdx, line ->
                val trimmed = line.trimStart()
                if (trimmed.startsWith("//") || trimmed.startsWith("#") || trimmed.startsWith("/*")) {
                    append(AnnotatedString(line, SpanStyle(color = commentColor)))
                } else {
                    var inString = false
                    var stringChar = ' '
                    var buffer = StringBuilder()

                    var i = 0
                    while (i < line.length) {
                        val c = line[i]

                        if (!inString && (c == '"' || c == '\'')) {
                            // Flush buffer
                            flushToken(buffer.toString(), keywordColor, numberColor, defaultTextColor)
                            buffer.clear()
                            inString = true
                            stringChar = c
                            buffer.append(c)
                        } else if (inString && c == stringChar && (i == 0 || line[i - 1] != '\\')) {
                            buffer.append(c)
                            append(AnnotatedString(buffer.toString(), SpanStyle(color = stringColor)))
                            buffer.clear()
                            inString = false
                        } else if (!inString && !c.isLetterOrDigit() && c != '_') {
                            flushToken(buffer.toString(), keywordColor, numberColor, defaultTextColor)
                            buffer.clear()
                            append(c.toString())
                        } else {
                            buffer.append(c)
                        }
                        i++
                    }
                    if (buffer.isNotEmpty()) {
                        if (inString) {
                            append(AnnotatedString(buffer.toString(), SpanStyle(color = stringColor)))
                        } else {
                            flushToken(buffer.toString(), keywordColor, numberColor, defaultTextColor)
                        }
                    }
                }

                if (lineIdx < lines.size - 1) {
                    append("\n")
                }
            }

            // Apply search query highlight if present
            if (searchQuery.isNotBlank()) {
                val fullText = toAnnotatedString().text
                var startIndex = 0
                while (startIndex < fullText.length) {
                    val matchIndex = fullText.indexOf(searchQuery, startIndex, ignoreCase = true)
                    if (matchIndex == -1) break
                    addStyle(
                        style = SpanStyle(background = searchHighlightColor, color = Color.Black),
                        start = matchIndex,
                        end = matchIndex + searchQuery.length
                    )
                    startIndex = matchIndex + searchQuery.length
                }
            }
        }
    }

    private fun AnnotatedString.Builder.flushToken(
        token: String,
        keywordColor: Color,
        numberColor: Color,
        defaultColor: Color
    ) {
        if (token.isEmpty()) return
        when {
            KEYWORDS.contains(token) -> {
                append(AnnotatedString(token, SpanStyle(color = keywordColor, fontWeight = FontWeight.SemiBold)))
            }
            token.all { it.isDigit() } -> {
                append(AnnotatedString(token, SpanStyle(color = numberColor)))
            }
            else -> {
                append(AnnotatedString(token, SpanStyle(color = defaultColor)))
            }
        }
    }
}

@Composable
fun CodeViewer(
    fileName: String,
    code: String,
    modifier: Modifier = Modifier,
    onEditClick: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val isDark = isSystemInDarkTheme()
    var isSearching by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var copyFeedback by remember { mutableStateOf(false) }

    val lines = remember(code) { code.split("\n") }
    val lineCount = lines.size
    val maxLineNumberDigits = lineCount.toString().length

    val annotatedCode = remember(code, isDark, searchQuery) {
        SyntaxHighlighter.highlight(code, isDark, searchQuery)
    }

    val matchCount = remember(code, searchQuery) {
        if (searchQuery.isBlank()) 0
        else {
            var count = 0
            var idx = 0
            while (idx < code.length) {
                val found = code.indexOf(searchQuery, idx, ignoreCase = true)
                if (found == -1) break
                count++
                idx = found + searchQuery.length
            }
            count
        }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) Color(0xFF0D1117) else Color(0xFFF6F8FA)
        ),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column {
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if (isDark) Color(0xFF161B22) else Color(0xFFEAEEF2))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Code,
                        contentDescription = "Code file",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = fileName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "($lineCount lines)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = { isSearching = !isSearching },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Search,
                            contentDescription = "Search in file",
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText(fileName, code))
                            HapticUtils.performSuccess(context)
                            copyFeedback = true
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = if (copyFeedback) Icons.Outlined.Check else Icons.Outlined.ContentCopy,
                            contentDescription = "Copy code",
                            tint = if (copyFeedback) Color(0xFF238636) else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    if (onEditClick != null) {
                        IconButton(
                            onClick = onEditClick,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Edit,
                                contentDescription = "Edit file",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // Search Bar if open
            if (isSearching) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(if (isDark) Color(0xFF21262D) else Color(0xFFE1E4E8))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Find in $fileName...", fontSize = 12.sp) },
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        singleLine = true,
                        textStyle = TextStyle(fontSize = 12.sp)
                    )

                    if (searchQuery.isNotBlank()) {
                        Text(
                            text = "$matchCount matches",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(
                        onClick = {
                            isSearching = false
                            searchQuery = ""
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = "Close search",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Main Code Content: Left Line Numbers + Horizontal Scrollable Code Text
            val horizontalScrollState = rememberScrollState()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                // Line Numbers column
                Column(
                    modifier = Modifier
                        .padding(start = 12.dp, end = 12.dp)
                        .widthIn(min = (maxLineNumberDigits * 10).dp),
                    horizontalAlignment = Alignment.End
                ) {
                    for (i in 1..lineCount) {
                        Text(
                            text = "$i",
                            style = MonospaceCodeStyle,
                            color = if (isDark) Color(0xFF484F58) else Color(0xFF8C959F)
                        )
                    }
                }

                // Vertical Divider line
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .fillMaxHeight()
                        .background(if (isDark) Color(0xFF30363D) else Color(0xFFD0D7DE))
                )

                // Code Text with Horizontal Scrolling
                SelectionContainer(
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(horizontalScrollState)
                        .padding(horizontal = 12.dp)
                ) {
                    Text(
                        text = annotatedCode,
                        style = MonospaceCodeStyle
                    )
                }
            }
        }
    }
}
