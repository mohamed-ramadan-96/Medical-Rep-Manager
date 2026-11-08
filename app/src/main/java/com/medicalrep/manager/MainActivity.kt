package com.medicalrep.manager

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.medicalrep.manager.ui.MedicalRepApp
import com.medicalrep.manager.ui.theme.MedicalRepTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MedicalRepTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MedicalRepApp()
                }
            }
        }
    }
}
