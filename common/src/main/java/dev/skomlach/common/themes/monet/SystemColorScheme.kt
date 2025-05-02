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

package dev.skomlach.common.themes.monet

import androidx.annotation.RequiresApi
import dev.skomlach.common.contextprovider.AndroidContext.appContext
import dev.skomlach.common.themes.monet.colors.Srgb

@RequiresApi(31)
class SystemColorScheme {

    val accent1 = getSwatch(ACCENT1_RES)
    val accent2 = getSwatch(ACCENT2_RES)
    val accent3 = getSwatch(ACCENT3_RES)

    val neutral1 = getSwatch(NEUTRAL1_RES)
    val neutral2 = getSwatch(NEUTRAL2_RES)

    private fun getSwatch(ids: Map<Int, Int>) = ids.map {
        it.key to Srgb(appContext.getColor(it.value))
    }.toMap()

    companion object {
        val ACCENT1_RES = mapOf(
            0 to android.R.color.system_accent1_0,
            10 to android.R.color.system_accent1_10,
            50 to android.R.color.system_accent1_50,
            100 to android.R.color.system_accent1_100,
            200 to android.R.color.system_accent1_200,
            300 to android.R.color.system_accent1_300,
            400 to android.R.color.system_accent1_400,
            500 to android.R.color.system_accent1_500,
            600 to android.R.color.system_accent1_600,
            700 to android.R.color.system_accent1_700,
            800 to android.R.color.system_accent1_800,
            900 to android.R.color.system_accent1_900,
            1000 to android.R.color.system_accent1_1000,
        )

        val ACCENT2_RES = mapOf(
            0 to android.R.color.system_accent2_0,
            10 to android.R.color.system_accent2_10,
            50 to android.R.color.system_accent2_50,
            100 to android.R.color.system_accent2_100,
            200 to android.R.color.system_accent2_200,
            300 to android.R.color.system_accent2_300,
            400 to android.R.color.system_accent2_400,
            500 to android.R.color.system_accent2_500,
            600 to android.R.color.system_accent2_600,
            700 to android.R.color.system_accent2_700,
            800 to android.R.color.system_accent2_800,
            900 to android.R.color.system_accent2_900,
            1000 to android.R.color.system_accent2_1000,
        )

        val ACCENT3_RES = mapOf(
            0 to android.R.color.system_accent3_0,
            10 to android.R.color.system_accent3_10,
            50 to android.R.color.system_accent3_50,
            100 to android.R.color.system_accent3_100,
            200 to android.R.color.system_accent3_200,
            300 to android.R.color.system_accent3_300,
            400 to android.R.color.system_accent3_400,
            500 to android.R.color.system_accent3_500,
            600 to android.R.color.system_accent3_600,
            700 to android.R.color.system_accent3_700,
            800 to android.R.color.system_accent3_800,
            900 to android.R.color.system_accent3_900,
            1000 to android.R.color.system_accent3_1000,
        )

        val NEUTRAL1_RES = mapOf(
            0 to android.R.color.system_neutral1_0,
            10 to android.R.color.system_neutral1_10,
            50 to android.R.color.system_neutral1_50,
            100 to android.R.color.system_neutral1_100,
            200 to android.R.color.system_neutral1_200,
            300 to android.R.color.system_neutral1_300,
            400 to android.R.color.system_neutral1_400,
            500 to android.R.color.system_neutral1_500,
            600 to android.R.color.system_neutral1_600,
            700 to android.R.color.system_neutral1_700,
            800 to android.R.color.system_neutral1_800,
            900 to android.R.color.system_neutral1_900,
            1000 to android.R.color.system_neutral1_1000,
        )

        val NEUTRAL2_RES = mapOf(
            0 to android.R.color.system_neutral2_0,
            10 to android.R.color.system_neutral2_10,
            50 to android.R.color.system_neutral2_50,
            100 to android.R.color.system_neutral2_100,
            200 to android.R.color.system_neutral2_200,
            300 to android.R.color.system_neutral2_300,
            400 to android.R.color.system_neutral2_400,
            500 to android.R.color.system_neutral2_500,
            600 to android.R.color.system_neutral2_600,
            700 to android.R.color.system_neutral2_700,
            800 to android.R.color.system_neutral2_800,
            900 to android.R.color.system_neutral2_900,
            1000 to android.R.color.system_neutral2_1000,
        )
    }
}
