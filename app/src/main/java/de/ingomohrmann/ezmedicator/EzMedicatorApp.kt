package de.ingomohrmann.ezmedicator

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import de.ingomohrmann.ezmedicator.notification.NotificationHelper
import javax.inject.Inject

@HiltAndroidApp
class EzMedicatorApp : Application() {

    @Inject lateinit var notificationHelper: NotificationHelper

    override fun onCreate() {
        super.onCreate()
        notificationHelper.createChannel()
    }
}
