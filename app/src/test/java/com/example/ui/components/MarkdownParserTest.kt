package com.example.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MarkdownParserTest {

  @Test
  fun parseMarkdownBlocks_handlesRealChatbotResponseWithNestedBullets() {
    val sample = """
      Yes, Alvin. Your spending is currently unsustainable for November 2026.

      * **Severe Deficit:** Your November expenses are **₱100,015,420.00** against an income of only **₱27,500.00**, leaving you with a net cash flow of **-₱99,987,920.00** and a 0.0% savings rate.
      * **Exceeded Budgets:** Even excluding that outlier, your regular budgets are struggling:
        * **Food and Dining:** Spent ₱3,680.00 of your ₱2,000.00 limit (184.0% — Exceeded)
        * **Transportation:** Spent ₱2,180.00 of your ₱2,000.00 limit
    """.trimIndent()

    val blocks = parseMarkdownBlocks(sample)
    assertEquals(5, blocks.size)
    assertTrue(blocks[0] is MarkdownBlock.Paragraph)
    assertEquals(0, (blocks[1] as MarkdownBlock.BulletItem).indentLevel)
    assertEquals(0, (blocks[2] as MarkdownBlock.BulletItem).indentLevel)
    assertEquals(1, (blocks[3] as MarkdownBlock.BulletItem).indentLevel)
    assertEquals(1, (blocks[4] as MarkdownBlock.BulletItem).indentLevel)
    assertTrue((blocks[3] as MarkdownBlock.BulletItem).text.startsWith("**Food and Dining:**"))
  }

  @Test
  fun parseInlineMarkdown_stripsAsterisksAndAppliesBoldSpans() {
    val input = "**Severe Deficit:** Your expenses are **₱100,015,420.00** and net flow is **-₱99,987,920.00**."
    val annotated = parseInlineMarkdown(
      text = input,
      boldColor = Color.White,
      codeBackground = Color.DarkGray,
      codeColor = Color.Green,
    )

    assertFalse(annotated.text.contains("**"))
    assertEquals(
      "Severe Deficit: Your expenses are ₱100,015,420.00 and net flow is -₱99,987,920.00.",
      annotated.text,
    )
    val boldSpans = annotated.spanStyles.filter { it.item.fontWeight == FontWeight.Bold }
    assertEquals(3, boldSpans.size)
    assertEquals("Severe Deficit:", annotated.text.substring(boldSpans[0].start, boldSpans[0].end))
    assertEquals("₱100,015,420.00", annotated.text.substring(boldSpans[1].start, boldSpans[1].end))
    assertEquals("-₱99,987,920.00", annotated.text.substring(boldSpans[2].start, boldSpans[2].end))
  }

  @Test
  fun parseInlineMarkdown_handlesHeadingsNumberedListsItalicAndPartialStreamingChunks() {
    val blocks = parseMarkdownBlocks(
      """
      ### Monthly Summary
      1. Cut **Food** by 20%
      2. Review *optional* subscriptions
      ---
      Partial **streaming chunk without close
      """.trimIndent(),
    )

    assertEquals(5, blocks.size)
    assertEquals(3, (blocks[0] as MarkdownBlock.Heading).level)
    assertEquals("1", (blocks[1] as MarkdownBlock.NumberedItem).number)
    assertEquals("2", (blocks[2] as MarkdownBlock.NumberedItem).number)
    assertTrue(blocks[3] is MarkdownBlock.Divider)

    val italicAnnotated = parseInlineMarkdown(
      text = (blocks[2] as MarkdownBlock.NumberedItem).text,
      boldColor = Color.White,
      codeBackground = Color.DarkGray,
      codeColor = Color.Green,
    )
    assertEquals("Review optional subscriptions", italicAnnotated.text)
    assertTrue(italicAnnotated.spanStyles.any { it.item.fontStyle == FontStyle.Italic })

    val partialAnnotated = parseInlineMarkdown(
      text = (blocks[4] as MarkdownBlock.Paragraph).text,
      boldColor = Color.White,
      codeBackground = Color.DarkGray,
      codeColor = Color.Green,
    )
    assertEquals("Partial **streaming chunk without close", partialAnnotated.text)
  }

  @Test
  fun parseInlineMarkdown_supportsUnderscoreItalicsAndCurrencyHighlighting() {
    val currencyColor = Color.Cyan
    val italicColor = Color.Yellow
    val input = "Spent ₱3,680.00 and _saved_ -₱99,987,920.00 on food_dining_budget."
    val annotated = parseInlineMarkdown(
      text = input,
      boldColor = Color.White,
      codeBackground = Color.DarkGray,
      codeColor = Color.Green,
      italicColor = italicColor,
      currencyColor = currencyColor,
    )

    assertEquals("Spent ₱3,680.00 and saved -₱99,987,920.00 on food_dining_budget.", annotated.text)
    val italicSpans = annotated.spanStyles.filter { it.item.fontStyle == FontStyle.Italic }
    assertEquals(1, italicSpans.size)
    assertEquals("saved", annotated.text.substring(italicSpans[0].start, italicSpans[0].end))
    assertEquals(italicColor, italicSpans[0].item.color)

    val currencySpans = annotated.spanStyles.filter { it.item.color == currencyColor }
    assertEquals(2, currencySpans.size)
    assertEquals("₱3,680.00", annotated.text.substring(currencySpans[0].start, currencySpans[0].end))
    assertEquals("-₱99,987,920.00", annotated.text.substring(currencySpans[1].start, currencySpans[1].end))
  }
}
