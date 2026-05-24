package com.progressvision.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.progressvision.app.ui.navigation.ProgressVisionRoot
import com.progressvision.app.ui.theme.ProgressVisionTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ProgressVisionTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    ProgressVisionRoot()
                }
            }
        }
    }
}
