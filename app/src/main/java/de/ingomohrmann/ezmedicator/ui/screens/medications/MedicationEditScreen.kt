package de.ingomohrmann.ezmedicator.ui.screens.medications

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import de.ingomohrmann.ezmedicator.R
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
    val saved by viewModel.saved.collectAsState()

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
                MedicationImage(imagePath = imagePath, size = 80.dp)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { pickImage.launch("image/*") }) {
                        Icon(Icons.Filled.AddPhotoAlternate, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            if (imagePath == null) stringResource(R.string.add_image)
                            else stringResource(R.string.change_image)
                        )
                    }
                    if (imagePath != null) {
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
}
