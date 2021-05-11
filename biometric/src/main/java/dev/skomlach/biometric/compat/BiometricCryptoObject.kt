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

package dev.skomlach.biometric.compat
import java.security.Signature
import javax.crypto.Cipher
import javax.crypto.Mac

class BiometricCryptoObject {
    val signature: Signature?
    val cipher: Cipher?
    val mac: Mac?

    /**
     * Creates a crypto object that wraps the given signature object.
     *
     * @param signature The signature to be associated with this crypto object.
     */
    constructor(signature: Signature) {
        this.signature = signature
        cipher = null
        mac = null
    }

    /**
     * Creates a crypto object that wraps the given cipher object.
     *
     * @param cipher The cipher to be associated with this crypto object.
     */
    constructor(cipher: Cipher) {
        signature = null
        this.cipher = cipher
        mac = null
    }

    /**
     * Creates a crypto object that wraps the given MAC object.
     *
     * @param mac The MAC to be associated with this crypto object.
     */
    constructor(mac: Mac) {
        signature = null
        cipher = null
        this.mac = mac
    }

}