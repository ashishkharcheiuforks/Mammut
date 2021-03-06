package io.github.koss.mammut.base.themes

interface ThemeConfig {

    var currentThemeId: String?

    val darkModeFollowSystem: Boolean
    val darkModeEnabled: Boolean
}