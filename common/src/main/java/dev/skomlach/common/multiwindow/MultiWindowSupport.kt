/*
 *  Copyright (c) 2023 Sergey Komlach aka Salat-Cx65; Original project https://github.com/Salat-Cx65/AdvancedBiometricPromptCompat
 *  All rights reserved.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package dev.skomlach.common.multiwindow

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.Point
import android.graphics.Rect
import android.os.Build
import android.view.KeyCharacterMap
import android.view.KeyEvent
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.view.Window
import dev.skomlach.common.R
import dev.skomlach.common.contextprovider.AndroidContext
import dev.skomlach.common.contextprovider.getFixedContext
import dev.skomlach.common.logging.LogCat
import java.lang.ref.WeakReference

class MultiWindowSupport private constructor(
    private val activityReference: WeakReference<Activity>? = null,
    private val isInBubbleTask: Boolean = false
) {
    private val hostActivity: Activity?
        get() = if (activityReference == null) AndroidContext.activity else activityReference.get()
    private val hostContext: Context
        get() = hostActivity ?: AndroidContext.appContext
    private val isHostTablet: Boolean
        get() = hostContext.resources.configuration.smallestScreenWidthDp >= 600 ||
                hostContext.resources.getBoolean(R.bool.biometric_compat_is_tablet)

    companion object {
        @SuppressLint("StaticFieldLeak")
        private val instance = MultiWindowSupport()
        fun get(): MultiWindowSupport {
            return instance
        }

        /** Use the actual dialog/auth host, not the last resumed Activity in the process. */
        fun get(activity: Activity): MultiWindowSupport = MultiWindowSupport(WeakReference(activity))

        /**
         * Optional task-scoped hint from the host app (e.g. a child Activity in a Bubble task).
         * Presentation only: does not grant authentication or determine biometric UI ownership.
         */
        fun get(activity: Activity, isInBubbleTask: Boolean): MultiWindowSupport =
            MultiWindowSupport(WeakReference(activity), isInBubbleTask)

        fun isTablet(): Boolean {
            val ctx = AndroidContext.activity ?: AndroidContext.appContext
            val resources = ctx.resources
            val configuration = AndroidContext.appConfiguration ?: resources.configuration
            val config = Configuration(configuration)
            return if (Build.VERSION.SDK_INT >= 17) {
                val res = ctx.createConfigurationContext(config).resources
                res.getBoolean(R.bool.biometric_compat_is_tablet) || res.configuration.screenWidthDp >= 600
            } else {
                val oldConfig = Configuration(configuration)
                resources.updateConfiguration(config, resources.displayMetrics)
                val flag =
                    resources.getBoolean(R.bool.biometric_compat_is_tablet) || resources.configuration.screenWidthDp >= 600
                resources.updateConfiguration(oldConfig, resources.displayMetrics)
                flag
            }

        }
    }

    //Unlike Android N method, this one support also non-Nougat+ multiwindow modes (like Samsung/LG/Huawei/etc solutions)
    private fun checkIsInMultiWindow(): Boolean {
        val rect = Rect()
        val decorView = hostActivity?.findViewById<ViewGroup>(Window.ID_ANDROID_CONTENT)
            ?: return false
        decorView.getGlobalVisibleRect(rect)
        if (rect.width() == 0 && rect.height() == 0) {
            return false
        }
        val realScreenSize = realScreenSize
        val statusBarHeight = statusBarHeight
        val navigationBarHeight = navigationBarHeight
        val navigationBarWidth = navigationBarWidth
        var h = realScreenSize.y - rect.height() - statusBarHeight - navigationBarHeight
        var w = realScreenSize.x - rect.width()
        val isSmartphone = !isHostTablet
        if (isSmartphone && screenOrientation == Configuration.ORIENTATION_LANDSCAPE) {
            h += navigationBarHeight
            w -= navigationBarWidth
        }
        val isMultiWindow = h != 0 || w != 0
        val locationOnScreen = IntArray(2)
        decorView.getLocationOnScreen(locationOnScreen)

        val sb = StringBuilder()
        sb.append(hostActivity?.javaClass?.simpleName + " Activity screen:")
        log("isMultiWindow $isMultiWindow", sb)
        log("final " + w + "x" + h + "", sb)
        log("NavBarW/H " + navigationBarWidth + "x" + navigationBarHeight, sb)
        log("statusBarH $statusBarHeight", sb)
        log("View $rect", sb)
        log("realScreenSize $realScreenSize", sb)

        LogCat.logError(sb.toString())
        return isMultiWindow
    }

    private fun log(msg: Any, sb: java.lang.StringBuilder) {
        sb.append(" [").append(msg).append("] ")
    }

    fun isWindowOnScreenBottom(): Boolean {
        val windowState = currentWindowStateWithLegacyFallback()
        val isWindowOnScreenBottom = windowState.isWindowOnScreenBottom
        val sb = StringBuilder()
        sb.append(hostActivity?.javaClass?.simpleName + " Activity screen:")
        log("isWindowOnScreenBottom $isWindowOnScreenBottom", sb)
        log("currentWindow ${windowState.currentBounds}", sb)
        log("maximumWindow ${windowState.maximumBounds}", sb)
        LogCat.logError(sb.toString())
        return isWindowOnScreenBottom
    }

    //Should work on API24+ and support almost all devices types, include Chromebooks and foldable devices
    //http://open-wiki.flyme.cn/index.php?title=%E5%88%86%E5%B1%8F%E9%80%82%E9%85%8D%E6%96%87%E6%A1%A3
    //general way - for OEM devices (Samsung, LG, Huawei) and/or in case API24 not fired for some reasons
    val isInMultiWindow: Boolean
        get() {
            val windowState = currentWindowStateWithLegacyFallback()
            val sb = StringBuilder()
            sb.append(hostActivity?.javaClass?.simpleName + " Activity screen:")
            log("isMultiWindow ${windowState.isInWindowedMode}", sb)
            log("platform ${windowState.isPlatformMultiWindow}", sb)
            log("pip ${windowState.isPictureInPicture}", sb)
            log("bubble ${windowState.isLaunchedFromBubble}", sb)
            log("legacy ${windowState.isLegacyMultiWindow}", sb)
            log("constrained ${windowState.isCurrentWindowConstrained}", sb)
            log("currentWindow ${windowState.currentBounds}", sb)
            log("maximumWindow ${windowState.maximumBounds}", sb)
            LogCat.logError(sb.toString())
            return windowState.isInWindowedMode
        }

    private fun checkLegacyMultiWindow(): Boolean {
        //http://open-wiki.flyme.cn/index.php?title=%E5%88%86%E5%B1%8F%E9%80%82%E9%85%8D%E6%96%87%E6%A1%A3
        try {
            val clazz = Class.forName("meizu.splitmode.FlymeSplitModeManager")
            val b = clazz.getMethod("getInstance", Context::class.java)
            val instance = b.invoke(null, hostActivity ?: return false)
            val m = clazz.getMethod("isSplitMode")
            val isMultiWindow = m.invoke(instance) as Boolean
            val sb = StringBuilder()
            sb.append(hostActivity?.javaClass?.simpleName + " Activity screen:")
            log("isMultiWindow $isMultiWindow", sb)
            LogCat.logError(sb.toString())
            if (isMultiWindow) {
                return true
            }
        } catch (ignore: Throwable) {
        }
        return if (hostActivity != null) {
            //general way - for OEM devices (Samsung, LG, Huawei) and/or in case API24 not fired for some reasons
            checkIsInMultiWindow()
        } else {
            false
        }
    }

    val isLaunchedFromBubble: Boolean
        get() = currentWindowState().isLaunchedFromBubble

    val isInPictureInPicture: Boolean
        get() = currentWindowState().isPictureInPicture

    internal val windowOrientation: WindowOrientation
        get() = currentWindowState().orientation

    val canLockCurrentOrientation: Boolean
        get() {
            val windowState = currentWindowStateWithLegacyFallback()
            return canLockWindowOrientation(windowState, Build.VERSION.SDK_INT,
                hostContext.resources.configuration.smallestScreenWidthDp)
        }

    val requestedScreenOrientation: Int
        get() {
            val requestedOrientation = currentWindowState().requestedOrientation
            return if (requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED) {
                ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            } else {
                requestedOrientation
            }
        }

    fun resolveDialogWidth(configuredWidthPx: Int): Int {
        val windowState = currentWindowState()
        return resolveDialogWidthPx(
            configuredWidthPx = configuredWidthPx,
            currentBounds = windowState.currentBounds,
            safeInsets = windowState.safeInsets
        )
    }

    fun currentWindowSize(): Point {
        val bounds = currentWindowState().currentBounds
        return Point(bounds.width, bounds.height)
    }

    fun getNavBarDividerHeight(): Int {
        val res = hostContext.resources
        val id = res.getIdentifier(
            "navigation_bar_divider_height",
            "dimen",
            "android"
        )
        return if (id > 0) res.getDimensionPixelSize(id) else 0
    }

    val navigationBarHeight: Int
        get() {
            currentWindowState().navigationBarInsets?.let { return maxOf(it.top, it.bottom) }
            if (!hasNavBar()) {
                return 0
            }
            val resources = hostContext.resources
            val orientation = screenOrientation
            val isSmartphone = !isHostTablet
            val resourceId: Int = if (!isSmartphone) {
                resources.getIdentifier(
                    if (orientation == Configuration.ORIENTATION_PORTRAIT) "navigation_bar_height" else "navigation_bar_height_landscape",
                    "dimen",
                    "android"
                )
            } else {
                resources.getIdentifier(
                    if (orientation == Configuration.ORIENTATION_PORTRAIT) "navigation_bar_height" else "navigation_bar_width",
                    "dimen",
                    "android"
                )
            }
            return if (resourceId > 0) {
                resources.getDimensionPixelSize(resourceId) + getNavBarDividerHeight()
            } else 0
        }
    val navigationBarWidth: Int
        get() {
            currentWindowState().navigationBarInsets?.let { return maxOf(it.left, it.right) }
            if (!hasNavBar()) {
                return 0
            }
            val resources = hostContext.resources
            val orientation = screenOrientation
            val isSmartphone = !isHostTablet
            val resourceId: Int = if (!isSmartphone) {
                resources.getIdentifier(
                    if (orientation == Configuration.ORIENTATION_PORTRAIT) "navigation_bar_height_landscape" else "navigation_bar_height",
                    "dimen",
                    "android"
                )
            } else {
                resources.getIdentifier(
                    if (orientation == Configuration.ORIENTATION_PORTRAIT) "navigation_bar_width" else "navigation_bar_height",
                    "dimen",
                    "android"
                )
            }
            return if (resourceId > 0) {
                resources.getDimensionPixelSize(resourceId)
            } else 0
        }

    fun hasNavBar(): Boolean {
        val windowState = currentWindowState()
        windowState.navigationBarInsets?.let { return maxOf(it.left, it.top, it.right, it.bottom) > 0 }
        val realHeight = windowState.physicalDisplayBounds.height
        val realWidth = windowState.physicalDisplayBounds.width
        val displayHeight = windowState.currentBounds.height
        val displayWidth = windowState.currentBounds.width
        if (realWidth - displayWidth > 0 || realHeight - displayHeight > 0) {
            return true
        }
        val hasMenuKey =
            ViewConfiguration.get(
                hostContext.getFixedContext()
            ).hasPermanentMenuKey()
        val hasBackKey = KeyCharacterMap.deviceHasKey(KeyEvent.KEYCODE_BACK)
        val hasHomeKey = KeyCharacterMap.deviceHasKey(KeyEvent.KEYCODE_HOME)
        val hasNoCapacitiveKeys = !hasMenuKey && !hasBackKey && !hasHomeKey
        val resources = hostContext.resources
        val id = resources.getIdentifier("config_showNavigationBar", "bool", "android")
        val hasOnScreenNavBar = id > 0 && resources.getBoolean(id)
        return hasOnScreenNavBar || hasNoCapacitiveKeys
    }

    // status bar height
    val statusBarHeight: Int
        get() {
            currentWindowState().statusBarInsets?.let { return maxOf(it.top, it.bottom) }
            // status bar height
            var statusBarHeight = 0
            val resourceId =
                hostContext.resources.getIdentifier(
                    "status_bar_height",
                    "dimen",
                    "android"
                )
            if (resourceId > 0) {
                statusBarHeight = hostContext.resources.getDimensionPixelSize(resourceId)
            }
            return statusBarHeight
        }//This should be close, as lower API devices should not have window navigation bars//this may not be 100% accurate, but it's all we've got//reflection for this weird in-between time


    //new pleasant way to get real metrics
    val realScreenSize: Point
        get() {
            val bounds = currentWindowState().physicalDisplayBounds
            return Point(bounds.width, bounds.height)
        }
    @get:Suppress("DEPRECATION")
    val screenOrientation: Int
        get() {
            val orientation = currentWindowState().configurationOrientation
            return if (orientation == Configuration.ORIENTATION_SQUARE) {
                Configuration.ORIENTATION_PORTRAIT
            } else {
                orientation
            }
        }

    private fun currentWindowState(
        legacyMultiWindow: Boolean = false
    ): WindowState {
        return buildWindowState(
            activity = hostActivity,
            fallbackContext = hostContext,
            isLegacyMultiWindow = legacyMultiWindow,
            isInBubbleTask = isInBubbleTask
        )
    }

    private fun currentWindowStateWithLegacyFallback(): WindowState {
        val windowState = currentWindowState()
        if (!windowState.needsLegacyWindowDetection || hostActivity == null) {
            return windowState
        }
        val legacyMultiWindow = checkLegacyMultiWindow()
        return if (legacyMultiWindow) {
            currentWindowState(legacyMultiWindow = true)
        } else {
            windowState
        }
    }

}
