package de.ingomohrmann.ezmedicator.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import java.io.File

@Composable
fun MedicationImage(
    imagePath: String?,
    iconName: String? = null,
    iconColor: Int? = null,
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
) {
    val shape = RoundedCornerShape(8.dp)
    if (imagePath != null && File(imagePath).exists()) {
        AsyncImage(
            model = File(imagePath),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier
                .size(size)
                .clip(shape),
        )
    } else {
        val bgColor = if (iconColor != null) Color(iconColor) else MaterialTheme.colorScheme.primaryContainer
        val tint = if (iconColor != null) Color.White else MaterialTheme.colorScheme.onPrimaryContainer
        val iconSource = MedicationIcons.sourceByName(iconName)
        Box(
            contentAlignment = Alignment.Center,
            modifier = modifier
                .size(size)
                .clip(shape)
                .background(bgColor),
        ) {
            if (iconSource != null) {
                MedicationIcons.EntryIcon(
                    source = iconSource,
                    tint = tint,
                    modifier = Modifier.size(size * 0.55f),
                )
            } else {
                Icon(
                    imageVector = Icons.Filled.Medication,
                    contentDescription = null,
                    tint = tint,
                    modifier = Modifier.size(size * 0.55f),
                )
            }
        }
    }
}
