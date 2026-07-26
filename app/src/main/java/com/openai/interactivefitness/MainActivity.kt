package com.openai.interactivefitness

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.openai.interactivefitness.ui.FitnessApp
import com.openai.interactivefitness.ui.theme.FitnessTheme
import com.openai.interactivefitness.ui.theme.ThemeMode

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var themeMode by remember { mutableStateOf(loadThemeMode()) }
            val useDarkTheme = when (themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            FitnessTheme(darkTheme = useDarkTheme) {
                FitnessApp(
                    themeMode = themeMode,
                    onThemeModeChange = {
                        themeMode = it
                        getSharedPreferences(THEME_PREFERENCES, MODE_PRIVATE)
                            .edit()
                            .putString(THEME_MODE, it.name)
                            .apply()
                    },
                )
            }
        }
    }

    private fun loadThemeMode(): ThemeMode {
        val saved = getSharedPreferences(THEME_PREFERENCES, MODE_PRIVATE)
            .getString(THEME_MODE, ThemeMode.SYSTEM.name)
        return runCatching { ThemeMode.valueOf(saved.orEmpty()) }
            .getOrDefault(ThemeMode.SYSTEM)
    }

    private companion object {
        const val THEME_PREFERENCES = "appearance"
        const val THEME_MODE = "theme_mode"
    }
}
