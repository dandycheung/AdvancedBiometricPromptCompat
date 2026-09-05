/*
 *  Copyright (c) 2021 Sergey Komlach aka Salat-Cx65; Original project https://github.com/Salat-Cx65/AdvancedBiometricPromptCompat
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

package dev.skomlach.biometric.compat.utils.activityView

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.RenderEffect
import android.graphics.Shader
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import androidx.core.graphics.drawable.toDrawable
import androidx.core.view.ViewCompat
import androidx.core.view.doOnAttach
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.findViewTreeLifecycleOwner
import dev.skomlach.biometric.compat.R
import dev.skomlach.biometric.compat.utils.logging.BiometricLoggerImpl
import dev.skomlach.biometric.compat.utils.logging.BiometricLoggerImpl.e
import dev.skomlach.common.blur.BlurUtil
import dev.skomlach.common.blur.DEFAULT_RADIUS
import dev.skomlach.common.misc.ExecutorHelper
import dev.skomlach.common.misc.Utils

class WindowBackgroundBlurring(
    private val parentView: ViewGroup
) {
    private var contentView: ViewGroup? = null
    private var v: View? = null
    private var renderEffect: RenderEffect? = null
    private var isBlurViewAttachedToHost = false
    private val captureLatch = BlurCaptureLatch()
    private var biometricsLayout: View? = null
    private val lifecycleEventObserver = object :
        LifecycleEventObserver {
        override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
            if (event == Lifecycle.Event.ON_DESTROY) {
                e("${this.javaClass.name}.onStateChanged - ON_DESTROY")
                resetListeners()
            }
        }
    }
    private val onDrawListener = ViewTreeObserver.OnPreDrawListener {
        updateBackground()
        true
    }

    init {
        for (i in 0 until parentView.childCount) {
            val v = parentView.getChildAt(i)
            if (v is ViewGroup) {
                contentView = v
            }
        }
    }

    private fun updateBackground() {
        if (!isBlurViewAttachedToHost)
            return
        if (!shouldCaptureBlurBitmap(Utils.isAtLeastS)) {
            setDrawable(null)
            return
        }
        val captureTarget = contentView ?: return
        if (captureTarget.width <= 0 || captureTarget.height <= 0) return
        val captureToken = captureLatch.tryStart() ?: return
        ExecutorHelper.postDelayed(
            { captureLatch.finish(captureToken) },
            BLUR_CAPTURE_TIMEOUT_MS
        )
        BiometricLoggerImpl.d("${this.javaClass.name}.updateBackground")
        try {
            BlurUtil.takeScreenshotAndBlur(captureTarget) { originalBitmap, blurredBitmap ->
                if (captureLatch.finish(captureToken)) {
                    setDrawable(blurredBitmap)
                    if (originalBitmap !== blurredBitmap && !originalBitmap.isRecycled) {
                        originalBitmap.recycle()
                    }
                } else {
                    if (!originalBitmap.isRecycled) originalBitmap.recycle()
                    if (blurredBitmap !== originalBitmap && blurredBitmap?.isRecycled == false) {
                        blurredBitmap.recycle()
                    }
                }
            }
        } catch (e: Throwable) {
            captureLatch.finish(captureToken)
            BiometricLoggerImpl.e(e)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setDrawable(bm: Bitmap?) {
        if (!isBlurViewAttachedToHost)
            return
        BiometricLoggerImpl.d("${this.javaClass.name}.setDrawable")
        try {
            v?.let {
                if (Utils.isAtLeastS) {
                    if (renderEffect == null)
                        renderEffect =
                            RenderEffect.createBlurEffect(
                                DEFAULT_RADIUS.toFloat(),
                                DEFAULT_RADIUS.toFloat(),
                                Shader.TileMode.DECAL
                            )
                    contentView?.setRenderEffect(renderEffect)
                } else
                    ViewCompat.setBackground(it, bm?.toDrawable(it.resources))
            } ?: run {
                v = LayoutInflater.from(parentView.context)
                    .inflate(R.layout.blurred_screen, null, false).apply {
                        tag = this@WindowBackgroundBlurring.javaClass.name
                        alpha = 1f
                        biometricsLayout = findViewById(R.id.biometrics_layout)
                        isFocusable = true
                        isClickable = true
                        isLongClickable = true
                        setOnTouchListener { _, _ ->
                            true
                        }
                        if (Utils.isAtLeastS) {
                            if (renderEffect == null)
                                renderEffect =
                                    RenderEffect.createBlurEffect(
                                        DEFAULT_RADIUS.toFloat(),
                                        DEFAULT_RADIUS.toFloat(),
                                        Shader.TileMode.DECAL
                                    )
                            contentView?.setRenderEffect(renderEffect)
                        } else
                            ViewCompat.setBackground(this, bm?.toDrawable(this.resources))
                        parentView.addView(this)

                    }
            }
        } catch (e: Throwable) {
            BiometricLoggerImpl.e(e)
        }
    }

    fun setupListeners() {
        if (isBlurViewAttachedToHost) return
        isBlurViewAttachedToHost = true
        try {
            if (shouldCaptureBlurBitmap(Utils.isAtLeastS)) {
                updateBackground()
            } else {
                setDrawable(null)
            }
            parentView.doOnAttach {
                parentView.findViewTreeLifecycleOwner()?.lifecycle?.addObserver(
                    lifecycleEventObserver
                )
            }
            if (shouldCaptureBlurBitmap(Utils.isAtLeastS)) {
                parentView.viewTreeObserver.addOnPreDrawListener(onDrawListener)
            }
        } catch (e: Throwable) {
            BiometricLoggerImpl.e(e)
        }
        BiometricLoggerImpl.d("${this.javaClass.name}.setupListeners")

    }

    fun resetListeners() {
        val wasAttached = isBlurViewAttachedToHost
        isBlurViewAttachedToHost = false
        captureLatch.reset()
        if (wasAttached) {
            try {
                parentView.viewTreeObserver.removeOnPreDrawListener(onDrawListener)
                parentView.findViewTreeLifecycleOwner()?.lifecycle?.removeObserver(
                    lifecycleEventObserver
                )
            } catch (e: Throwable) {
                BiometricLoggerImpl.e(e)
            }
        }
        runBlurCleanup(
            clearRenderEffect = {
                if (Utils.isAtLeastS) {
                    contentView?.setRenderEffect(null)
                }
            },
            removeOverlay = {
                v?.let {
                    parentView.removeView(it)
                }
                parentView.findViewWithTag<View?>(this@WindowBackgroundBlurring.javaClass.name)
                    ?.let {
                        parentView.removeView(it)
                    }
            },
            invalidateHost = {
                contentView?.invalidate()
                parentView.invalidate()
            },
            onFailure = { BiometricLoggerImpl.e(it) }
        )
        BiometricLoggerImpl.d("${this.javaClass.name}.resetListeners")
    }

    private companion object {
        const val BLUR_CAPTURE_TIMEOUT_MS = 2_000L
    }

}
