package de.ingomohrmann.ezmedicator.ui.navigation

sealed class Screen(val route: String) {
    data object MedicationList : Screen("medication_list")
    data class MedicationDetail(val medicationId: Long = 0) :
        Screen("medication_detail/{medicationId}") {
        fun route(id: Long) = "medication_detail/$id"
    }
    data class MedicationEdit(val medicationId: Long = -1) :
        Screen("medication_edit/{medicationId}") {
        fun route(id: Long = -1) = "medication_edit/$id"
    }
    data class ReminderEdit(val medicationId: Long = 0, val reminderId: Long = -1) :
        Screen("reminder_edit/{medicationId}/{reminderId}") {
        fun route(medicationId: Long, reminderId: Long = -1) =
            "reminder_edit/$medicationId/$reminderId"
    }
    data object Settings : Screen("settings")
}
