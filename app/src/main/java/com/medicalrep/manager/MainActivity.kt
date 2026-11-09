package com.medicalrep.manager

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.medicalrep.manager.ui.MedicalRepApp
import com.medicalrep.manager.ui.MapShareState
import com.medicalrep.manager.ui.handleMapShareIntent
import com.medicalrep.manager.ui.theme.MedicalRepTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleMapShareIntent(intent)
        setContent {
            MedicalRepTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MedicalRepApp()
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleMapShareIntent(intent)
    }
}
