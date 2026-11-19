package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

sealed class MarkdownBlock {
  data class Heading(val level: Int, val text: String) : MarkdownBlock()
  data class BulletItem(val indentLevel: Int, val text: String) : MarkdownBlock()
  data class NumberedItem(val indentLevel: Int, val number: String, val text: String) : MarkdownBlock()
  data class Paragraph(val text: String) : MarkdownBlock()
  data object Divider : MarkdownBlock()
}

private val HEADING_REGEX = Regex("""^\s*(#{1,4})\s+(.+?)\s*#*\s*$""")
private val BULLET_REGEX = Regex("""^(\s*)[*\-+•]\s+(.+)$""")
private val NUMBERED_REGEX = Regex("""^(\s*)(\d+)[.)]\s+(.+)$""")
private val DIVIDER_REGEX = Regex("""^\s*(?:[-*_]\s*){3,}$""")

// Matches currency figures such as ₱100,015,420.00, -₱99,987,920.00, $1,250.50, PHP 2,500.00
private val CURRENCY_MENTION_REGEX = Regex(
  """(?<!\w)[+\-−]?(?:[₱$€£¥₹₩]\s*\d[\d,]*(?:\.\d{1,2})?|(?:PHP|USD|EUR|GBP|JPY|SGD|AUD|CAD)\s+\d[\d,]*(?:\.\d{1,2})?)"""
)

/**
 * Parses a Markdown string into structured blocks (headings, bullet lists with nesting,
 * ordered lists, dividers, and paragraphs). Safe for incomplete streaming chunks.
 */
fun parseMarkdownBlocks(raw: String): List<MarkdownBlock> {
  if (raw.isBlank()) return emptyList()

  val blocks = mutableListOf<MarkdownBlock>()
  val paragraphBuffer = mutableListOf<String>()

  fun flushParagraph() {
    if (paragraphBuffer.isNotEmpty()) {
      blocks += MarkdownBlock.Paragraph(paragraphBuffer.joinToString("\n"))
      paragraphBuffer.clear()
    }
  }

  raw.replace("\r\n", "\n").lines().forEach { line ->
    val trimmed = line.trim()
    if (trimmed.isEmpty()) {
      flushParagraph()
      return@forEach
    }

    if (DIVIDER_REGEX.matches(line)) {
      flushParagraph()
      blocks += MarkdownBlock.Divider
      return@forEach
    }

    val headingMatch = HEADING_REGEX.matchEntire(line)
    if (headingMatch != null) {
      flushParagraph()
      blocks += MarkdownBlock.Heading(
        level = headingMatch.groupValues[1].length,
        text = headingMatch.groupValues[2].trim(),
      )
      return@forEach
    }

    val bulletMatch = BULLET_REGEX.matchEntire(line)
    if (bulletMatch != null) {
      flushParagraph()
      val indentSpaces = bulletMatch.groupValues[1].replace("\t", "  ").length
      val indentLevel = (indentSpaces / 2).coerceIn(0, 3)
      blocks += MarkdownBlock.BulletItem(
        indentLevel = indentLevel,
        text = bulletMatch.groupValues[2].trim(),
      )
      return@forEach
    }

    val numberedMatch = NUMBERED_REGEX.matchEntire(line)
    if (numberedMatch != null) {
      flushParagraph()
      val indentSpaces = numberedMatch.groupValues[1].replace("\t", "  ").length
      val indentLevel = (indentSpaces / 2).coerceIn(0, 3)
      blocks += MarkdownBlock.NumberedItem(
        indentLevel = indentLevel,
        number = numberedMatch.groupValues[2],
        text = numberedMatch.groupValues[3].trim(),
      )
      return@forEach
    }

    paragraphBuffer += trimmed
  }

  flushParagraph()
  return blocks
}

/**
 * Parses inline Markdown spans (`***bold italic***`, `**bold**`, `__bold__`, `*italic*`,
 * `_italic_`, `` `code` ``, `~~strike~~`) and automatically highlights currency amounts into
 * an [AnnotatedString]. Unclosed markers degrade gracefully as literal text during streaming.
 */
fun parseInlineMarkdown(
  text: String,
  boldColor: Color,
  codeBackground: Color,
  codeColor: Color,
  italicColor: Color = lerp(boldColor, codeColor, 0.35f),
  currencyColor: Color = codeColor,
): AnnotatedString = buildAnnotatedString {
  var i = 0
  val n = text.length

  while (i < n) {
    // 1. Bold + Italic: ***text***
    if (i + 2 < n && text.startsWith("***", i)) {
      val end = text.indexOf("***", startIndex = i + 3)
      if (end > i + 3) {
        val inner = text.substring(i + 3, end)
        pushStyle(
          SpanStyle(
            fontWeight = FontWeight.Bold,
            fontStyle = FontStyle.Italic,
            color = boldColor,
          ),
        )
        append(inner)
        pop()
        i = end + 3
        continue
      }
    }

    // 2. Bold: **text** or __text__
    if (i + 1 < n && (text.startsWith("**", i) || text.startsWith("__", i))) {
      val marker = text.substring(i, i + 2)
      val end = text.indexOf(marker, startIndex = i + 2)
      if (end > i + 2) {
        val inner = text.substring(i + 2, end)
        pushStyle(
          SpanStyle(
            fontWeight = FontWeight.Bold,
            color = boldColor,
          ),
        )
        append(inner)
        pop()
        i = end + 2
        continue
      }
    }

    // 3. Inline code: `code`
    if (text[i] == '`') {
      val end = text.indexOf('`', startIndex = i + 1)
      if (end > i + 1) {
        val inner = text.substring(i + 1, end)
        pushStyle(
          SpanStyle(
            fontFamily = FontFamily.Monospace,
            background = codeBackground,
            color = codeColor,
          ),
        )
        append(" $inner ")
        pop()
        i = end + 1
        continue
      }
    }

    // 4. Strikethrough: ~~text~~
    if (i + 1 < n && text.startsWith("~~", i)) {
      val end = text.indexOf("~~", startIndex = i + 2)
      if (end > i + 2) {
        val inner = text.substring(i + 2, end)
        pushStyle(SpanStyle(textDecoration = TextDecoration.LineThrough))
        append(inner)
        pop()
        i = end + 2
        continue
      }
    }

    // 5. Italic: *text* (only when not adjacent to whitespace inside markers)
    if (text[i] == '*' && i + 1 < n && !text[i + 1].isWhitespace() && text[i + 1] != '*') {
      val end = findClosingSingleMarker(text, i + 1, '*')
      if (end > i + 1) {
        val inner = text.substring(i + 1, end)
        pushStyle(
          SpanStyle(
            fontStyle = FontStyle.Italic,
            color = italicColor,
          ),
        )
        append(inner)
        pop()
        i = end + 1
        continue
      }
    }

    // 6. Italic: _text_ (word-boundary guarded so snake_case identifiers are preserved)
    val prevIsWord = i > 0 && text[i - 1].isLetterOrDigit()
    if (!prevIsWord && text[i] == '_' && i + 1 < n && !text[i + 1].isWhitespace() && text[i + 1] != '_') {
      val end = findClosingSingleMarker(text, i + 1, '_')
      val nextIsWord = end != -1 && end + 1 < n && text[end + 1].isLetterOrDigit()
      if (end > i + 1 && !nextIsWord) {
        val inner = text.substring(i + 1, end)
        pushStyle(
          SpanStyle(
            fontStyle = FontStyle.Italic,
            color = italicColor,
          ),
        )
        append(inner)
        pop()
        i = end + 1
        continue
      }
    }

    append(text[i])
    i++
  }

  // Highlight currency mentions cleanly in emerald/mint accent with tabular digits
  val plainText = toAnnotatedString().text
  CURRENCY_MENTION_REGEX.findAll(plainText).forEach { match ->
    addStyle(
      style = SpanStyle(
        color = currencyColor,
        fontWeight = FontWeight.SemiBold,
        fontFeatureSettings = "tnum",
      ),
      start = match.range.first,
      end = match.range.last + 1,
    )
  }
}

private fun findClosingSingleMarker(text: String, startIndex: Int, marker: Char): Int {
  var idx = startIndex
  while (idx < text.length) {
    if (text[idx] == marker) {
      val isDouble = (idx + 1 < text.length && text[idx + 1] == marker) ||
        (idx > startIndex && text[idx - 1] == marker)
      if (!isDouble && !text[idx - 1].isWhitespace()) {
        return idx
      }
    }
    idx++
  }
  return -1
}

/**
 * Renders formatted Markdown text for BudgetWise assistant bubbles with bold emphasis,
 * emerald-tinted italics, automatic currency highlighting, nested bullet & numbered lists,
 * headings, inline code, and relaxed line-height.
 */
@Composable
fun MarkdownMessageText(
  markdown: String,
  modifier: Modifier = Modifier,
  baseColor: Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.90f),
  boldColor: Color = MaterialTheme.colorScheme.onSurface,
  accentColor: Color = MaterialTheme.colorScheme.primary,
  baseStyle: TextStyle = MaterialTheme.typography.bodyMedium,
) {
  val blocks = remember(markdown) { parseMarkdownBlocks(markdown) }
  val codeBackground = MaterialTheme.colorScheme.surface.copy(alpha = 0.55f)
  val italicColor = remember(boldColor, accentColor) {
    lerp(boldColor, accentColor, 0.35f)
  }
  val relaxedStyle = baseStyle.copy(
    lineHeight = if (baseStyle.fontSize.value > 0f) (baseStyle.fontSize.value * 1.55f).sp else 22.sp,
  )

  Column(
    modifier = modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(0.dp),
  ) {
    blocks.forEachIndexed { index, block ->
      val nextBlock = blocks.getOrNull(index + 1)
      val bottomSpacing = when {
        nextBlock == null -> 0.dp
        block is MarkdownBlock.BulletItem && nextBlock is MarkdownBlock.BulletItem -> 6.dp
        block is MarkdownBlock.NumberedItem && nextBlock is MarkdownBlock.NumberedItem -> 6.dp
        block is MarkdownBlock.Heading -> 6.dp
        else -> 12.dp
      }

      when (block) {
        is MarkdownBlock.Heading -> {
          val headingStyle = when (block.level) {
            1 -> MaterialTheme.typography.titleMedium
            2 -> MaterialTheme.typography.titleSmall
            else -> MaterialTheme.typography.bodyLarge
          }.copy(fontWeight = FontWeight.Bold)
          val annotated = remember(block.text, boldColor, codeBackground, accentColor, italicColor) {
            parseInlineMarkdown(
              text = block.text,
              boldColor = boldColor,
              codeBackground = codeBackground,
              codeColor = accentColor,
              italicColor = italicColor,
              currencyColor = accentColor,
            )
          }
          Text(
            text = annotated,
            style = headingStyle,
            color = boldColor,
            modifier = Modifier
              .padding(top = if (index == 0) 0.dp else 4.dp, bottom = bottomSpacing),
          )
        }

        is MarkdownBlock.BulletItem -> {
          val annotated = remember(block.text, boldColor, codeBackground, accentColor, italicColor) {
            parseInlineMarkdown(
              text = block.text,
              boldColor = boldColor,
              codeBackground = codeBackground,
              codeColor = accentColor,
              italicColor = italicColor,
              currencyColor = accentColor,
            )
          }
          val bulletGlyph = if (block.indentLevel == 0) "•" else "◦"
          val startIndent = (block.indentLevel * 14).dp
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(start = startIndent, bottom = bottomSpacing),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Text(
              text = bulletGlyph,
              style = relaxedStyle.copy(fontWeight = FontWeight.Bold),
              color = if (block.indentLevel == 0) accentColor else baseColor.copy(alpha = 0.75f),
            )
            Text(
              text = annotated,
              style = relaxedStyle,
              color = baseColor,
              modifier = Modifier.weight(1f),
            )
          }
        }

        is MarkdownBlock.NumberedItem -> {
          val annotated = remember(block.text, boldColor, codeBackground, accentColor, italicColor) {
            parseInlineMarkdown(
              text = block.text,
              boldColor = boldColor,
              codeBackground = codeBackground,
              codeColor = accentColor,
              italicColor = italicColor,
              currencyColor = accentColor,
            )
          }
          val startIndent = (block.indentLevel * 14).dp
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(start = startIndent, bottom = bottomSpacing),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Text(
              text = "${block.number}.",
              style = relaxedStyle.copy(fontWeight = FontWeight.SemiBold),
              color = accentColor,
            )
            Text(
              text = annotated,
              style = relaxedStyle,
              color = baseColor,
              modifier = Modifier.weight(1f),
            )
          }
        }

        is MarkdownBlock.Paragraph -> {
          val annotated = remember(block.text, boldColor, codeBackground, accentColor, italicColor) {
            parseInlineMarkdown(
              text = block.text,
              boldColor = boldColor,
              codeBackground = codeBackground,
              codeColor = accentColor,
              italicColor = italicColor,
              currencyColor = accentColor,
            )
          }
          Text(
            text = annotated,
            style = relaxedStyle,
            color = baseColor,
            modifier = Modifier.padding(bottom = bottomSpacing),
          )
        }

        MarkdownBlock.Divider -> {
          HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant,
            modifier = Modifier.padding(vertical = 8.dp),
          )
        }
      }
    }
  }
}
