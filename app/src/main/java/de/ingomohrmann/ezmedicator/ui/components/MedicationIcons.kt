package de.ingomohrmann.ezmedicator.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Healing
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.LocalPharmacy
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Vaccines
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import de.ingomohrmann.ezmedicator.R

sealed interface IconSource {
    data class Vector(val vector: ImageVector) : IconSource
    data class Resource(@DrawableRes val resId: Int) : IconSource
}

object MedicationIcons {

    data class IconEntry(val name: String, val source: IconSource)

    val all: List<IconEntry> = listOf(
        IconEntry("Medication", IconSource.Vector(Icons.Filled.Medication)),
        IconEntry("MedicalServices", IconSource.Vector(Icons.Filled.MedicalServices)),
        IconEntry("PillRound", IconSource.Resource(R.drawable.ic_pill_round)),
        IconEntry("CapsuleV", IconSource.Resource(R.drawable.ic_pill_capsule_v)),
        IconEntry("PillBottle", IconSource.Resource(R.drawable.ic_pill_bottle)),
        IconEntry("LocalPharmacy", IconSource.Vector(Icons.Filled.LocalPharmacy)),
        IconEntry("Healing", IconSource.Vector(Icons.Filled.Healing)),
        IconEntry("MonitorHeart", IconSource.Vector(Icons.Filled.MonitorHeart)),
        IconEntry("Vaccines", IconSource.Vector(Icons.Filled.Vaccines)),
        IconEntry("Spa", IconSource.Vector(Icons.Filled.Spa)),
        IconEntry("Science", IconSource.Vector(Icons.Filled.Science)),
        IconEntry("Favorite", IconSource.Vector(Icons.Filled.Favorite)),
        IconEntry("WaterDrop", IconSource.Vector(Icons.Filled.WaterDrop)),
        IconEntry("FitnessCenter", IconSource.Vector(Icons.Filled.FitnessCenter)),
        IconEntry("HealthAndSafety", IconSource.Vector(Icons.Filled.HealthAndSafety)),
        IconEntry("Star", IconSource.Vector(Icons.Filled.Star)),
    )

    val presetColors: List<Color> = listOf(
        Color(0xFFE53935),
        Color(0xFFD81B60),
        Color(0xFF8E24AA),
        Color(0xFF3949AB),
        Color(0xFF1E88E5),
        Color(0xFF039BE5),
        Color(0xFF00897B),
        Color(0xFF43A047),
        Color(0xFFFB8C00),
        Color(0xFFFF5722),
        Color(0xFF795548),
        Color(0xFF546E7A),
    )

    fun sourceByName(name: String?): IconSource? =
        all.firstOrNull { it.name == name }?.source

    @Composable
    fun EntryIcon(
        source: IconSource,
        modifier: Modifier = Modifier,
        tint: Color = LocalContentColor.current,
    ) {
        when (source) {
            is IconSource.Vector ->
                Icon(imageVector = source.vector, contentDescription = null, tint = tint, modifier = modifier)
            is IconSource.Resource ->
                Icon(painter = painterResource(source.resId), contentDescription = null, tint = tint, modifier = modifier)
        }
    }
}
