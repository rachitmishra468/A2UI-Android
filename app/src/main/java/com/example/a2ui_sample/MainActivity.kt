package com.example.a2ui_sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.a2ui_sample.ui.AiRestaurantScreen
import com.example.a2ui_sample.ui.theme.A2UI_SampleTheme

/**
 * File: MainActivity.kt
 * Purpose: Entry point of the application. It sets up the theme and displays the AI Restaurant Screen.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            A2UI_SampleTheme {
                // Launch the AI Restaurant Assistant Screen
               AiRestaurantScreen()
            }
        }
    }
}
