package io.github.jamiesanson.mammut.feature.themes

import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import io.github.jamiesanson.mammut.R
import io.github.jamiesanson.mammut.repo.PreferencesRepository
import uk.co.chrisjenx.calligraphy.CalligraphyConfig

class ThemeEngine(
        private val preferencesRepository: PreferencesRepository
) {

    private val currentTheme: Theme
       get() = when (preferencesRepository.themeId) {
           StandardTheme.themeId -> StandardTheme
           else -> {
               preferencesRepository.themeId = StandardTheme.themeId
               Log.w("ThemeEngine", "Invalid theme ID. Resetting to standard")
               StandardTheme
           }
       }

    fun apply(activity: AppCompatActivity) {
        activity.setTheme(currentTheme.styleRes)
        updateFontDefaults()
    }

    fun updateFontDefaults() {
        CalligraphyConfig.initDefault(CalligraphyConfig.Builder()
                .setDefaultFontPath(currentTheme.primaryFont.path)
                .setFontAttrId(R.attr.fontPath)
                .build()
        )
    }
}