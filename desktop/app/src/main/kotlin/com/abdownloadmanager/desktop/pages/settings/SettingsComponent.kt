package com.abdownloadmanager.desktop.pages.settings

import com.abdownloadmanager.desktop.pages.settings.SettingSections.*
import com.abdownloadmanager.desktop.pages.settings.configurable.*
import com.abdownloadmanager.desktop.repository.AppRepository
import com.abdownloadmanager.desktop.storage.AppSettingsStorage
import com.abdownloadmanager.desktop.ui.icon.IconSource
import com.abdownloadmanager.desktop.ui.icon.MyIcons
import com.abdownloadmanager.desktop.ui.theme.darkColors
import com.abdownloadmanager.desktop.ui.theme.lightColors
import com.abdownloadmanager.desktop.utils.BaseComponent
import com.abdownloadmanager.desktop.utils.convertSpeedToHumanReadable
import ir.amirab.util.flow.mapTwoWayStateFlow
import com.abdownloadmanager.desktop.utils.mvi.ContainsEffects
import com.abdownloadmanager.desktop.utils.mvi.supportEffects
import androidx.compose.runtime.*
import com.arkivanov.decompose.ComponentContext
import ir.amirab.util.FileUtils
import kotlinx.coroutines.CoroutineScope
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

sealed class SettingSections(
    val icon: IconSource,
    val name: String,
) {
    data object Appearance : SettingSections(MyIcons.appearance, "Appearance")
//    TODO ADD Network section (proxy , etc..)
    //    data object Network : SettingSections(MyIcons.network, "Network")
    data object DownloadEngine : SettingSections(MyIcons.downloadEngine, "Download Engine")
    data object BrowserIntegration : SettingSections(MyIcons.network, "Browser Integration")
}

interface SettingSectionGetter {
    operator fun get(key: SettingSections): List<Configurable<*>>
}

fun threadCountConfig(appRepository: AppRepository): IntConfigurable {
    return IntConfigurable(
        title = "Thread Count",
        description = "Maximum download thread per download item",
        backedBy = appRepository.threadCount,
        range = 1..32,
        renderMode = IntConfigurable.RenderMode.TextField,
        describe = {
            "a download can have up to $it thread"
        },
    )
}
fun dynamicPartDownloadConfig(appRepository: AppRepository): BooleanConfigurable {
    return BooleanConfigurable(
        title = "Dynamic part creation",
        description = "When a part is finished create another part by splitting other parts to improve download speed",
        backedBy = appRepository.dynamicPartCreation,
        describe = {
            if (it) {
                "Enabled"
            }else{
                "Disabled"
            }
        },
    )
}

fun speedLimitConfig(appRepository: AppRepository): SpeedLimitConfigurable {
    return SpeedLimitConfigurable(
        title = "Global Speed Limiter",
        description = "Global download speed limit (0 means unlimited)",
        backedBy = appRepository.speedLimiter,
        describe = {
            if (it == 0L) {
                "Unlimited"
            } else {
                convertSpeedToHumanReadable(it)
            }
        }
    )
}

fun useAverageSpeedConfig(appRepository: AppRepository): BooleanConfigurable {
    return BooleanConfigurable(
        title = "Show Average Speed",
        description = "Download speed in average or precision",
        backedBy = appRepository.useAverageSpeed,
        describe = {
            if (it) "Average Speed"
            else "Exact Speed"
        }
    )
}

fun defaultDownloadFolderConfig(appSettings: AppSettingsStorage): FolderConfigurable {
    return FolderConfigurable(
        title = "Default Download Folder",
        description = "When you add new download this location is used by default",
        backedBy = appSettings.defaultDownloadFolder,
        validate = {
            FileUtils.canWriteInThisFolder(it)
        },
        describe = {
            "\"$it\" will be used"
        }
    )
}

/*
fun uiScaleConfig(appSettings: AppSettings): EnumConfigurable<Float?> {
    return EnumConfigurable(
        title = "Ui Scale",
        description = "Scale Ui Elements",
        backedBy = appSettings.uiScale,
        possibleValues = listOf(
            null,
            0.5f,
            0.75f,
            1f,
            1.25f,
            1.5f,
            1.75f,
            2f,
        ),
        renderMode = EnumConfigurable.RenderMode.Spinner,
        describe = {
            if (it == null) {
                "System"
            } else {
                "$it x"
            }
        }
    )
}
*/

fun themeConfig(appSettings: AppSettingsStorage, scope: CoroutineScope): ThemeConfigurable {
    val defaultTheme = "dark"
    val themes = mapOf(
        "dark" to darkColors,
        "light" to lightColors
    )
    return ThemeConfigurable(
        title = "Theme",
        description = "Select theme",
        //maybe try a better way?
        backedBy = appSettings.theme.mapTwoWayStateFlow({
            themes[it]?:themes[defaultTheme]!!
        }, { myColors ->
            themes.entries.firstOrNull() { it.value == myColors }?.key ?: defaultTheme
        }),
        possibleValues = listOf(darkColors, lightColors),
        describe = { it.name },
    )
}

fun autoStartConfig(appSettings: AppSettingsStorage): BooleanConfigurable {
    return BooleanConfigurable(
        title = "Start On Boot",
        description = "Auto start application on user logins",
        backedBy = appSettings.autoStartOnBoot,
        renderMode = BooleanConfigurable.RenderMode.Switch,
        describe = {
            if (it) {
                "Auto Start Enabled"
            } else {
                "Auto Start Disabled"
            }
        }
    )
}
fun playSoundNotification(appSettings: AppSettingsStorage): BooleanConfigurable {
    return BooleanConfigurable(
        title = "Notification Sound",
        description = "Play sound on new notification",
        backedBy = appSettings.notificationSound,
        renderMode = BooleanConfigurable.RenderMode.Switch,
        describe = {
            if (it) {
                "Play sounds"
            } else {
                "Muted"
            }
        }
    )
}

fun browserIntegrationEnabled(appRepository: AppRepository): BooleanConfigurable {
    return BooleanConfigurable(
        title = "Browser Integration",
        description = "Accept downloads from browsers",
        backedBy = appRepository.integrationEnabled,
        renderMode = BooleanConfigurable.RenderMode.Switch,
        describe = {
            if (it) {
                "Enabled"
            } else {
                "Disabled"
            }
        }
    )
}

fun browserIntegrationPort(appRepository: AppRepository): IntConfigurable {
    return IntConfigurable(
        title = "Server Port",
        description = "port for browser integration",
        backedBy = appRepository.integrationPort,
        describe = {
            "listen to $it"
        },
        range = 0..65000,
    )
}

sealed class SettingPageEffects {
    data object BringToFront : SettingPageEffects()
}

class SettingsComponent(
    ctx: ComponentContext,
) : BaseComponent(ctx),
    KoinComponent,
    ContainsEffects<SettingPageEffects> by supportEffects() {
    val appSettings by inject<AppSettingsStorage>()
    val appRepository by inject<AppRepository>()
    val allConfigs = object : SettingSectionGetter {
        override operator fun get(key: SettingSections): List<Configurable<*>> {
            return when (key) {
                Appearance -> listOf(
                    themeConfig(appSettings, scope),
//                    uiScaleConfig(appSettings),
                    autoStartConfig(appSettings),
                    playSoundNotification(appSettings),
                )

//                Network -> listOf()
                BrowserIntegration -> listOf(
                    browserIntegrationEnabled(appRepository),
                    browserIntegrationPort(appRepository)
                )

                DownloadEngine -> listOf(
                    defaultDownloadFolderConfig(appSettings),
                    useAverageSpeedConfig(appRepository),
                    speedLimitConfig(appRepository),
                    threadCountConfig(appRepository),
                    dynamicPartDownloadConfig(appRepository)
                )
            }
        }
    }

    fun toFront() {
        sendEffect(SettingPageEffects.BringToFront)
    }

    var pages = listOf(
        Appearance,
//        Network,
        DownloadEngine,
        BrowserIntegration,
    )
    var currentPage: SettingSections by mutableStateOf(Appearance)
    val configurables by derivedStateOf {
        allConfigs[currentPage]
    }
}