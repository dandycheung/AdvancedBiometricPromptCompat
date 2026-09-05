package dev.skomlach.common.multiwindow

import android.app.Activity
import android.content.Context
import android.graphics.Point
import android.graphics.Rect
import android.os.Build
import android.view.Surface
import android.view.WindowManager
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.window.layout.WindowMetricsCalculator
import dev.skomlach.common.contextprovider.getFixedContext

internal fun buildWindowState(
    activity: Activity?,
    fallbackContext: Context,
    isLegacyMultiWindow: Boolean = false,
    isInBubbleTask: Boolean = false
): WindowState {
    val currentMetrics = if (Build.VERSION.SDK_INT >= 30 && activity != null) {
        runCatching { activity.windowManager.currentWindowMetrics }.getOrNull()
    } else null
    val maximumMetrics = if (Build.VERSION.SDK_INT >= 30 && activity != null) {
        runCatching { activity.windowManager.maximumWindowMetrics }.getOrNull()
    } else null
    val currentBounds = if (Build.VERSION.SDK_INT >= 30) currentMetrics?.bounds?.toWindowBoundsPx()
        ?.takeUnless { it.isEmpty } ?: getCurrentWindowBounds(activity, fallbackContext)
    else getCurrentWindowBounds(activity, fallbackContext)
    val maximumBounds = if (Build.VERSION.SDK_INT >= 30) maximumMetrics?.bounds?.toWindowBoundsPx()
        ?.takeUnless { it.isEmpty } ?: getMaximumWindowBounds(activity, fallbackContext)
    else getMaximumWindowBounds(activity, fallbackContext)
    val reliableMetrics = Build.VERSION.SDK_INT >= 30 && currentMetrics != null && maximumMetrics != null &&
            !currentMetrics.bounds.isEmpty && !maximumMetrics.bounds.isEmpty
    val metricInsets = if (Build.VERSION.SDK_INT >= 30) currentMetrics?.windowInsets?.let {
            WindowInsetsCompat.toWindowInsetsCompat(it)
        } else null
    val insets = metricInsets ?: activity?.window?.decorView?.let { ViewCompat.getRootWindowInsets(it) }
    val physicalBounds = getPhysicalDisplayBounds(activity, fallbackContext)
    return calculateWindowState(
        currentBounds = currentBounds,
        maximumBounds = maximumBounds,
        physicalDisplayBounds = physicalBounds,
        // Caption bars, cutouts and stable system bars belong to the window; IME does not
        // turn a full-screen activity into a split-screen activity.
        safeInsets = insets.toInsetsPx(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
            ?: WindowInsetsPx.EMPTY,
        isPlatformMultiWindow = isActivityInMultiWindow(activity),
        isPictureInPicture = isActivityInPictureInPicture(activity),
        isLaunchedFromBubble = isInBubbleTask || isActivityLaunchedFromBubble(activity),
        isLegacyMultiWindow = isLegacyMultiWindow,
        hasReliableWindowMetrics = reliableMetrics,
        navigationBarInsets = insets.toInsetsPx(WindowInsetsCompat.Type.navigationBars()),
        statusBarInsets = insets.toInsetsPx(WindowInsetsCompat.Type.statusBars())
    )
}

internal fun Rect.toWindowBoundsPx(): WindowBoundsPx {
    return WindowBoundsPx(
        left = left,
        top = top,
        width = width(),
        height = height()
    )
}

private fun isActivityInMultiWindow(activity: Activity?): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        runCatching { activity?.isInMultiWindowMode == true }.getOrDefault(false)
    } else {
        false
    }
}

private fun isActivityInPictureInPicture(activity: Activity?): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        runCatching { activity?.isInPictureInPictureMode == true }.getOrDefault(false)
    } else {
        false
    }
}

private fun isActivityLaunchedFromBubble(activity: Activity?): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        runCatching { activity?.isLaunchedFromBubble == true }.getOrDefault(false)
    } else {
        false
    }
}

private fun getCurrentWindowBounds(activity: Activity?, fallbackContext: Context): WindowBoundsPx {
    activity?.let {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                return it.windowManager.currentWindowMetrics.bounds.toWindowBoundsPx()
            } catch (ignore: Throwable) {
            }
        }
        try {
            return WindowMetricsCalculator.getOrCreate()
                .computeCurrentWindowMetrics(it)
                .bounds
                .toWindowBoundsPx()
        } catch (ignore: Throwable) {
        }
    }
    return getDisplayRectBounds(fallbackContext)
}

private fun getMaximumWindowBounds(activity: Activity?, fallbackContext: Context): WindowBoundsPx {
    activity?.let {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                return it.windowManager.maximumWindowMetrics.bounds.toWindowBoundsPx()
            } catch (ignore: Throwable) {
            }
        }
        try {
            return WindowMetricsCalculator.getOrCreate()
                .computeMaximumWindowMetrics(it)
                .bounds
                .toWindowBoundsPx()
        } catch (ignore: Throwable) {
        }
    }
    return getDisplayRectBounds(fallbackContext)
}

private fun getPhysicalDisplayBounds(activity: Activity?, fallbackContext: Context): WindowBoundsPx {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        val context = activity ?: fallbackContext
        try {
            val display = activity?.display ?: context.display
            display.mode.let { mode ->
                val rotation = display.rotation
                val nativeWidth = mode.physicalWidth
                val nativeHeight = mode.physicalHeight
                val point = when (rotation) {
                    Surface.ROTATION_90, Surface.ROTATION_270 -> Point(nativeHeight, nativeWidth)
                    else -> Point(nativeWidth, nativeHeight)
                }
                return WindowBoundsPx(left = 0, top = 0, width = point.x, height = point.y)
            }
        } catch (ignore: Throwable) {
        }
    }

    @Suppress("DEPRECATION")
    return try {
        val windowManager =
            fallbackContext.getFixedContext().getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val point = Point()
        windowManager.defaultDisplay.getRealSize(point)
        WindowBoundsPx(left = 0, top = 0, width = point.x, height = point.y)
    } catch (ignore: Throwable) {
        val metrics = fallbackContext.resources.displayMetrics
        WindowBoundsPx(left = 0, top = 0, width = metrics.widthPixels, height = metrics.heightPixels)
    }
}

private fun getDisplayRectBounds(context: Context): WindowBoundsPx {
    val bounds = Rect()
    @Suppress("DEPRECATION")
    try {
        val windowManager = context.getFixedContext()
            .getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val display = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            context.display
        } else {
            windowManager.defaultDisplay
        }
        display.getRectSize(bounds)
    } catch (ignore: Throwable) {
    }
    if (!bounds.isEmpty) {
        return bounds.toWindowBoundsPx()
    }
    val metrics = context.resources.displayMetrics
    return WindowBoundsPx(left = 0, top = 0, width = metrics.widthPixels, height = metrics.heightPixels)
}

private fun WindowInsetsCompat?.toInsetsPx(types: Int): WindowInsetsPx? {
    val insets = this?.getInsetsIgnoringVisibility(types) ?: return null
    return WindowInsetsPx(
        left = insets.left,
        top = insets.top,
        right = insets.right,
        bottom = insets.bottom
    )
}
