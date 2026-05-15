package de.ingomohrmann.ezmedicator.domain

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import com.cronutils.descriptor.CronDescriptor
import com.cronutils.model.CronType
import com.cronutils.model.definition.CronDefinitionBuilder
import com.cronutils.model.time.ExecutionTime
import com.cronutils.parser.CronParser
import java.time.ZonedDateTime
import java.util.Locale

object CronHelper {

    private val definition = CronDefinitionBuilder.instanceDefinitionFor(CronType.UNIX)
    private val parser = CronParser(definition)

    // Colours for each cron field: minute, hour, day-of-month, month, day-of-week
    private val fieldColors = listOf(
        Color(0xFF388E3C), // green   – minute
        Color(0xFF1976D2), // blue    – hour
        Color(0xFFE65100), // orange  – day-of-month
        Color(0xFF7B1FA2), // purple  – month
        Color(0xFFC62828), // red     – day-of-week
    )

    fun isValid(expression: String): Boolean = runCatching {
        parser.parse(expression.trim()).validate()
        true
    }.getOrDefault(false)

    fun nextExecution(expression: String, from: ZonedDateTime = ZonedDateTime.now()): ZonedDateTime? =
        runCatching {
            val cron = parser.parse(expression.trim())
            ExecutionTime.forCron(cron).nextExecution(from).orElse(null)
        }.getOrNull()

    /** Returns the cron occurrence after the very next one (used for "skip next"). */
    fun secondNextExecution(expression: String): ZonedDateTime? =
        nextExecution(expression)?.let { first -> nextExecution(expression, first) }

    fun describe(expression: String): String = runCatching {
        val cron = parser.parse(expression.trim())
        CronDescriptor.instance(Locale.ENGLISH).describe(cron)
    }.getOrDefault("Invalid expression")

    /**
     * Returns the expression as an [AnnotatedString] with each cron field coloured.
     *
     * The output length is always identical to the input length so that
     * [androidx.compose.ui.text.input.OffsetMapping.Identity] remains valid inside
     * a [androidx.compose.ui.text.input.VisualTransformation].
     * Whitespace (leading, trailing, between fields) is preserved verbatim.
     */
    fun annotate(expression: String): AnnotatedString {
        return buildAnnotatedString {
            var fieldIndex = 0
            var pos = 0
            val tokenRegex = "\\S+".toRegex()
            for (match in tokenRegex.findAll(expression)) {
                // Whitespace before this token — append unstyled so length is preserved
                if (match.range.first > pos) {
                    append(expression.substring(pos, match.range.first))
                }
                val color = fieldColors.getOrElse(fieldIndex) { Color.Unspecified }
                pushStyle(SpanStyle(color = color))
                append(match.value)
                pop()
                fieldIndex++
                pos = match.range.last + 1
            }
            // Trailing whitespace (e.g. the space the user just typed)
            if (pos < expression.length) {
                append(expression.substring(pos))
            }
        }
    }
}
