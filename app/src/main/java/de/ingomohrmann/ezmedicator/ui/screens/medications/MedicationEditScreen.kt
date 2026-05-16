package de.ingomohrmann.ezmedicator.ui.screens.medications

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import de.ingomohrmann.ezmedicator.R
import de.ingomohrmann.ezmedicator.ui.components.IconSource
import de.ingomohrmann.ezmedicator.ui.components.MedicationIcons
import de.ingomohrmann.ezmedicator.ui.components.MedicationImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicationEditScreen(
    medicationId: Long?,
    onBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: MedicationEditViewModel = hiltViewModel(),
) {
    LaunchedEffect(medicationId) { viewModel.load(medicationId) }

    val title by viewModel.title.collectAsState()
    val imagePath by viewModel.imagePath.collectAsState()
    val iconName by viewModel.iconName.collectAsState()
    val iconColor by viewModel.iconColor.collectAsState()
    val saved by viewModel.saved.collectAsState()
    var showIconPicker by remember { mutableStateOf(false) }

    LaunchedEffect(saved) { if (saved) onSaved() }

    val pickImage = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? -> uri?.let { viewModel.setImage(it) } }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (medicationId == null) stringResource(R.string.add_medication)
                        else stringResource(R.string.edit_medication)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                },
                actions = {
                    TextButton(
                        onClick = { viewModel.save() },
                        enabled = title.isNotBlank(),
                    ) { Text(stringResource(R.string.save)) }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { viewModel.setTitle(it) },
                label = { Text(stringResource(R.string.medication_title)) },
                placeholder = { Text(stringResource(R.string.medication_title_hint)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Text(
                text = stringResource(R.string.medication_image),
                style = MaterialTheme.typography.labelLarge,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                MedicationImage(
                    imagePath = imagePath,
                    iconName = iconName,
                    iconColor = iconColor,
                    size = 80.dp,
                )
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { pickImage.launch("image/*") }) {
                        Icon(Icons.Filled.AddPhotoAlternate, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            if (imagePath == null) stringResource(R.string.add_image)
                            else stringResource(R.string.change_image)
                        )
                    }
                    OutlinedButton(onClick = { showIconPicker = true }) {
                        Icon(Icons.Filled.Medication, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.choose_icon))
                    }
                    if (imagePath != null || iconName != null) {
                        OutlinedButton(
                            onClick = { viewModel.removeImage() },
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error,
                            ),
                        ) {
                            Icon(Icons.Filled.Delete, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.remove_image))
                        }
                    }
                }
            }
        }
    }

    if (showIconPicker) {
        IconPickerDialog(
            currentIconName = iconName,
            currentIconColor = iconColor,
            onDismiss = { showIconPicker = false },
            onConfirm = { name, color ->
                viewModel.setIcon(name, color)
                showIconPicker = false
            },
        )
    }
}

@Composable
private fun IconPickerDialog(
    currentIconName: String?,
    currentIconColor: Int?,
    onDismiss: () -> Unit,
    onConfirm: (name: String, color: Int) -> Unit,
) {
    var selectedIcon by remember {
        mutableStateOf(currentIconName ?: MedicationIcons.all.first().name)
    }
    var selectedColor by remember {
        mutableStateOf(
            if (currentIconColor != null) Color(currentIconColor)
            else MedicationIcons.presetColors.first()
        )
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            tonalElevation = 6.dp,
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    stringResource(R.string.choose_icon),
                    style = MaterialTheme.typography.headlineSmall,
                )

                // Preview
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(selectedColor),
                    ) {
                        val previewSource = MedicationIcons.sourceByName(selectedIcon)
                            ?: IconSource.Vector(Icons.Filled.Medication)
                        MedicationIcons.EntryIcon(
                            source = previewSource,
                            tint = Color.White,
                            modifier = Modifier.size(40.dp),
                        )
                    }
                }

                // Icon grid
                Text(
                    stringResource(R.string.icon_label),
                    style = MaterialTheme.typography.labelLarge,
                )
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    MedicationIcons.all.chunked(4).forEach { row ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            row.forEach { entry ->
                                val isSelected = entry.name == selectedIcon
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (isSelected) MaterialTheme.colorScheme.secondaryContainer
                                            else MaterialTheme.colorScheme.surfaceVariant,
                                        )
                                        .clickable { selectedIcon = entry.name },
                                ) {
                                    MedicationIcons.EntryIcon(
                                        source = entry.source,
                                        tint = if (isSelected)
                                            MaterialTheme.colorScheme.onSecondaryContainer
                                        else
                                            MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(28.dp),
                                    )
                                }
                            }
                            repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }

                // Color swatches
                Text(
                    stringResource(R.string.color_label),
                    style = MaterialTheme.typography.labelLarge,
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                ) {
                    MedicationIcons.presetColors.forEach { color ->
                        val isSelected = color == selectedColor
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(color)
                                .then(
                                    if (isSelected)
                                        Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                    else
                                        Modifier
                                )
                                .clickable { selectedColor = color },
                        )
                    }
                }

                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
                    Spacer(Modifier.width(8.dp))
                    TextButton(onClick = { onConfirm(selectedIcon, selectedColor.toArgb()) }) {
                        Text(stringResource(R.string.confirm))
                    }
                }
            }
        }
    }
}
