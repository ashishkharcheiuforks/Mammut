package io.github.koss.mammut.feature.settings.model

import com.bluelinelabs.conductor.Controller
import io.github.koss.mammut.feature.contributors.ContributorsController

/**
 * File defining the actions which can be performed on the settings screen.
 */
sealed class SettingsAction

sealed class NavigationAction(val controllerToPush: () -> Controller): SettingsAction() {

    object ViewContributors: NavigationAction(controllerToPush = { ContributorsController() })

}

object ToggleLightDarkMode: SettingsAction()

object ToggleStreaming: SettingsAction()

object TogglePlaceKeeping: SettingsAction()

object ToggleLaunchInstanceBrowser: SettingsAction()

object ViewOssLicenses: SettingsAction()

object ChangeInstance: SettingsAction()

object LogOut: SettingsAction()