package de.ingomohrmann.ezmedicator.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.ingomohrmann.ezmedicator.domain.CronHelper

@Composable
fun CronTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Schedule (cron)",
    hint: String = "e.g. 0 8 * * *",
) {
    val isValid = remember(value) { value.isBlank() || CronHelper.isValid(value) }
    val description = remember(value) {
        if (CronHelper.isValid(value)) CronHelper.describe(value) else null
    }

    val errorColor = MaterialTheme.colorScheme.error
    val outlineColor: Color = if (isValid) MaterialTheme.colorScheme.outline else errorColor
    val labelColor: Color = if (isValid) MaterialTheme.colorScheme.onSurfaceVariant else errorColor

    val highlightTransformation = remember {
        VisualTransformation { text ->
            val annotated = CronHelper.annotate(text.text)
            TransformedText(annotated, OffsetMapping.Identity)
        }
    }

    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = labelColor,
        )
        Spacer(Modifier.height(4.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(width = 1.dp, color = outlineColor, shape = RoundedCornerShape(4.dp))
                .padding(horizontal = 12.dp, vertical = 10.dp),
        ) {
            if (value.isEmpty()) {
                Text(
                    text = hint,
                    style = LocalTextStyle.current.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    ),
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                textStyle = LocalTextStyle.current.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                visualTransformation = highlightTransformation,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        Spacer(Modifier.height(4.dp))

        when {
            !isValid && value.isNotBlank() -> Text(
                text = "Invalid cron expression",
                style = MaterialTheme.typography.labelSmall,
                color = errorColor,
            )
            description != null -> Text(
                text = "Runs: $description",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(Modifier.height(2.dp))
        Text(
            text = "min   hour  dom   month  dow",
            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
        )
    }
}
