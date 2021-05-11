/*
 *  Copyright (c) 2021 Sergey Komlach aka Salat-Cx65; Original project: https://github.com/Salat-Cx65/AdvancedBiometricPromptCompat
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

package com.example.myapplication

import android.os.Build
import android.util.Base64
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import dev.skomlach.biometric.compat.*
import dev.skomlach.biometric.compat.engine.AuthenticationFailureReason
import dev.skomlach.biometric.compat.utils.logging.BiometricLoggerImpl

private var encrypted: EncryptedData? = null
private val decrypted: String = "Test text"
private var initializationVector: ByteArray? = null

@RequiresApi(Build.VERSION_CODES.M)
fun Fragment.startBiometric(biometricAuthRequest: BiometricAuthRequest) {
    if (!BiometricManagerCompat.hasEnrolled(biometricAuthRequest)) {
        BiometricManagerCompat.openSettings(requireActivity(), biometricAuthRequest)
        return
    }
    val biometricPromptCompat = BiometricPromptCompat.Builder(
        biometricAuthRequest,
        requireActivity()
    )
        .setTitle("Biometric for Fragment")
        .setNegativeButton("Cancel", null)
        .build()

    val context = activity?.applicationContext?:return
    val cipher = if (encrypted == null) {
        BiometricCryptoObject(CryptographyManagerImpl.manager.getInitializedCipherForEncryption("myKey123"))
    } else {
        BiometricCryptoObject(CryptographyManagerImpl.manager.getInitializedCipherForDecryption("myKey123", initializationVector!!))
    }

    biometricPromptCompat.authenticate(
        object : BiometricPromptCompat.Result {
            override fun onSucceeded(confirmed: Map<BiometricType, BiometricCryptoObject?>) {
                BiometricLoggerImpl.e("CheckBiometric.onSucceeded() for $confirmed")
                val biometricType: BiometricType = confirmed.keys.toList()[0]
                confirmed[biometricType]?.let {
                    encrypted = if (encrypted == null) {
                        val s = CryptographyManagerImpl.manager.encryptData(decrypted, it.cipher!!)//IllegalBlockSize on MIUI
                        initializationVector = s.initializationVector
                        Toast.makeText(context, "Succeeded ${Base64.encode(s.ciphertext, Base64.DEFAULT)}", Toast.LENGTH_SHORT).show()
                        s
                    } else {
                        val s = CryptographyManagerImpl.manager.decryptData(encrypted?.ciphertext!!, it.cipher!!)
                        Toast.makeText(context, "Succeeded '${s == decrypted}'/'$s'", Toast.LENGTH_SHORT).show()
                        null
                    }
                }

            }

            override fun onCanceled() {
                BiometricLoggerImpl.e("CheckBiometric.onCanceled()")
                Toast.makeText(context, "Canceled", Toast.LENGTH_SHORT).show()
            }

            override fun onFailed(reason: AuthenticationFailureReason?) {
                BiometricLoggerImpl.e("CheckBiometric.onFailed() - $reason")
                Toast.makeText(context, "Error: $reason", Toast.LENGTH_SHORT).show()
            }

            override fun onUIOpened() {
                BiometricLoggerImpl.e("CheckBiometric.onUIOpened()")
                Toast.makeText(context, "onUIOpened", Toast.LENGTH_SHORT).show()
            }

            override fun onUIClosed() {
                BiometricLoggerImpl.e("CheckBiometric.onUIClosed()")
                Toast.makeText(context, "onUIClosed", Toast.LENGTH_SHORT).show()
            }
        },
        cipher
    )
}