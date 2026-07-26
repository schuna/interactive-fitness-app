package com.openai.interactivefitness

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.openai.interactivefitness.ui.FitnessApp
import com.openai.interactivefitness.ui.theme.FitnessTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FitnessTheme {
                FitnessApp()
            }
        }
    }
}
