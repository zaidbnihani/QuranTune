package com.example.utils

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability

object PlayStoreUpdateManager {
    private const val TAG = "PlayStoreUpdateManager"
    private const val FLEXIBLE_UPDATE_REQUEST_CODE = 1001

    fun checkForUpdates(activity: Activity, onUpdateAvailable: (Int) -> Unit) {
        val appUpdateManager = AppUpdateManagerFactory.create(activity)
        val appUpdateInfoTask = appUpdateManager.appUpdateInfo

        appUpdateInfoTask.addOnSuccessListener { appUpdateInfo ->
            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)
            ) {
                onUpdateAvailable(appUpdateInfo.availableVersionCode())
                
                // Start the update
                appUpdateManager.startUpdateFlowForResult(
                    appUpdateInfo,
                    activity,
                    AppUpdateOptions.newBuilder(AppUpdateType.FLEXIBLE).build(),
                    FLEXIBLE_UPDATE_REQUEST_CODE
                )
            }
        }
    }
}
