package com.example.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.example.utils.PlayStoreUpdateManager
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import android.app.Activity
import androidx.compose.material3.*
import androidx.compose.foundation.layout.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp

@Composable
fun GooglePlayUpdateEffect() {
    val context = LocalContext.current
    val activity = context as? Activity
    var updateAvailable by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        if (activity != null) {
            PlayStoreUpdateManager.checkForUpdates(activity) { versionCode ->
                updateAvailable = true
            }
        }
    }

    if (updateAvailable) {
        AlertDialog(
            onDismissRequest = { },
            title = { Text("يتوفر تحديث جديد") },
            text = { Text("هناك إصدار أحدث من التطبيق متاح على Google Play.") },
            confirmButton = {
                TextButton(onClick = { 
                    // Update is already started by PlayStoreUpdateManager, just dismiss dialog
                    updateAvailable = false 
                }) {
                    Text("حسناً")
                }
            }
        )
    }
}
