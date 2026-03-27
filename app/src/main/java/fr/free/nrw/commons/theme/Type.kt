package fr.free.nrw.commons.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp

enum class CommonsTextPreset { Headline, Title, Body, Caption }

@Composable
fun CommonsText(
    text: String,
    preset: CommonsTextPreset,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    textAlign: TextAlign? = null,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
    fontWeight: FontWeight? = null
) {
    CommonsText(AnnotatedString(text), preset, modifier, color, textAlign, maxLines, overflow, fontWeight)
}

@Composable
fun CommonsText(
    text: AnnotatedString,
    preset: CommonsTextPreset,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    textAlign: TextAlign? = null,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
    fontWeight: FontWeight? = null
) {
    val style = when (preset) {
        CommonsTextPreset.Headline -> TextStyle(fontSize = 24.sp, fontWeight = fontWeight ?: FontWeight.Bold)
        CommonsTextPreset.Title -> TextStyle(fontSize = 20.sp, fontWeight = fontWeight ?: FontWeight.Medium)
        CommonsTextPreset.Body -> TextStyle(fontSize = 16.sp, fontWeight = fontWeight ?: FontWeight.Normal)
        CommonsTextPreset.Caption -> TextStyle(fontSize = 14.sp, fontWeight = fontWeight ?: FontWeight.Normal)
    }
    Text(
        text = text,
        modifier = modifier,
        style = style,
        color = if (color == Color.Unspecified) MaterialTheme.colorScheme.onBackground else color,
        textAlign = textAlign,
        maxLines = maxLines,
        overflow = overflow
    )
}