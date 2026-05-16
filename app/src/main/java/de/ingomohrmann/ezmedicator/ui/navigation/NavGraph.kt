package de.ingomohrmann.ezmedicator.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import de.ingomohrmann.ezmedicator.ui.screens.medications.MedicationDetailScreen
import de.ingomohrmann.ezmedicator.ui.screens.medications.MedicationEditScreen
import de.ingomohrmann.ezmedicator.ui.screens.medications.MedicationListScreen
import de.ingomohrmann.ezmedicator.ui.screens.reminders.ReminderEditScreen
import de.ingomohrmann.ezmedicator.ui.screens.log.LogScreen
import de.ingomohrmann.ezmedicator.ui.screens.settings.SettingsScreen

@Composable
fun AppNavGraph() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Screen.MedicationList.route) {

        composable(Screen.MedicationList.route) {
            MedicationListScreen(
                onAdd = { navController.navigate(Screen.MedicationEdit().route()) },
                onEdit = { id -> navController.navigate(Screen.MedicationEdit().route(id)) },
                onOpen = { id -> navController.navigate(Screen.MedicationDetail().route(id)) },
                onAddReminder = { medicationId ->
                    navController.navigate(Screen.ReminderEdit().route(medicationId))
                },
                onSettings = { navController.navigate(Screen.Settings.route) },
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onActivityLog = { navController.navigate(Screen.ActivityLog.route) },
            )
        }

        composable(Screen.ActivityLog.route) {
            LogScreen(onBack = { navController.popBackStack() })
        }

        composable(
            route = Screen.MedicationDetail(0).route,
            arguments = listOf(navArgument("medicationId") { type = NavType.LongType }),
        ) { back ->
            val medicationId = back.arguments!!.getLong("medicationId")
            MedicationDetailScreen(
                medicationId = medicationId,
                onBack = { navController.popBackStack() },
                onEditMedication = { navController.navigate(Screen.MedicationEdit().route(medicationId)) },
                onAddReminder = { navController.navigate(Screen.ReminderEdit().route(medicationId)) },
                onEditReminder = { remId ->
                    navController.navigate(Screen.ReminderEdit().route(medicationId, remId))
                },
            )
        }

        composable(
            route = Screen.MedicationEdit(0).route,
            arguments = listOf(navArgument("medicationId") { type = NavType.LongType }),
        ) { back ->
            val medicationId = back.arguments!!.getLong("medicationId")
            MedicationEditScreen(
                medicationId = if (medicationId == -1L) null else medicationId,
                onBack = { navController.popBackStack() },
                onSaved = { navController.popBackStack() },
            )
        }

        composable(
            route = Screen.ReminderEdit(0, 0).route,
            arguments = listOf(
                navArgument("medicationId") { type = NavType.LongType },
                navArgument("reminderId") { type = NavType.LongType },
            ),
        ) { back ->
            val medicationId = back.arguments!!.getLong("medicationId")
            val reminderId = back.arguments!!.getLong("reminderId")
            ReminderEditScreen(
                medicationId = medicationId,
                reminderId = if (reminderId == -1L) null else reminderId,
                onBack = { navController.popBackStack() },
                onSaved = { navController.popBackStack() },
            )
        }
    }
}
