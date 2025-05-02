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
import android.graphics.drawable.BitmapDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import androidx.core.view.ViewCompat
import dev.skomlach.biometric.compat.R
import dev.skomlach.biometric.compat.utils.logging.BiometricLoggerImpl
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
    private var drawingInProgress = false
    private var biometricsLayout: View? = null

    private val attachStateChangeListener = object : View.OnAttachStateChangeListener {
        override fun onViewAttachedToWindow(v: View) {
            BiometricLoggerImpl.d("${this.javaClass.name}.onViewAttachedToWindow")
        }

        override fun onViewDetachedFromWindow(v: View) {
            BiometricLoggerImpl.d("${this.javaClass.name}.onViewDetachedFromWindow")
            resetListeners()
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
        if (!isBlurViewAttachedToHost || drawingInProgress)
            return
        BiometricLoggerImpl.d("${this.javaClass.name}.updateBackground")
        try {
            contentView?.let {
                BlurUtil.takeScreenshotAndBlur(it) { _, blurredBitmap ->
                    setDrawable(blurredBitmap)
                }
            }
        } catch (e: Throwable) {
            BiometricLoggerImpl.e(e)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setDrawable(bm: Bitmap?) {
        if (!isBlurViewAttachedToHost || drawingInProgress)
            return
        BiometricLoggerImpl.d("${this.javaClass.name}.setDrawable")
        drawingInProgress = true
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
                    ViewCompat.setBackground(it, BitmapDrawable(it.resources, bm))
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
                            ViewCompat.setBackground(this, BitmapDrawable(this.resources, bm))
                        parentView.addView(this)

                    }
            }
        } catch (e: Throwable) {
            BiometricLoggerImpl.e(e)
        }
        ExecutorHelper.post {
            drawingInProgress = false
        }
    }

    fun setupListeners() {
        if (isBlurViewAttachedToHost) return
        isBlurViewAttachedToHost = true
        try {
            updateBackground()
            parentView.addOnAttachStateChangeListener(attachStateChangeListener)
            parentView.viewTreeObserver.addOnPreDrawListener(onDrawListener)
        } catch (e: Throwable) {
            BiometricLoggerImpl.e(e)
        }
        BiometricLoggerImpl.d("${this.javaClass.name}.setupListeners")

    }

    fun resetListeners() {
        if (!isBlurViewAttachedToHost) return
        isBlurViewAttachedToHost = false
        try {
            parentView.removeOnAttachStateChangeListener(attachStateChangeListener)
            parentView.viewTreeObserver.removeOnPreDrawListener(onDrawListener)
        } catch (e: Throwable) {
            BiometricLoggerImpl.e(e)
        }
        try {
            v?.let {
                parentView.removeView(it)
            }
            parentView.findViewWithTag<View?>(this@WindowBackgroundBlurring.javaClass.name)?.let {
                parentView.removeView(it)
            }
        } catch (e: Throwable) {
            BiometricLoggerImpl.e(e)
        } finally {
            try {
                if (Utils.isAtLeastS) {
                    contentView?.setRenderEffect(null)
                }
            } catch (e: Throwable) {
                BiometricLoggerImpl.e(e)
            }
        }
        BiometricLoggerImpl.d("${this.javaClass.name}.resetListeners")
    }

}