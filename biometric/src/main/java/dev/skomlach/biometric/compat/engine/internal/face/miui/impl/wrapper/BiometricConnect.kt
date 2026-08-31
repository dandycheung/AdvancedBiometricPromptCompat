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

package dev.skomlach.biometric.compat.engine.internal.face.miui.impl.wrapper

import android.os.Parcelable
import dev.skomlach.biometric.compat.engine.internal.face.miui.impl.shouldReadOptionalMiuiMessageFields
import dev.skomlach.biometric.compat.utils.logging.BiometricLoggerImpl.e

object BiometricConnect {
    var DEBUG_LOG = false
    var MSG_VER_SER_MAJ: String? = null
    var MSG_VER_SER_MIN: String? = null
    var MSG_VER_MODULE_MAJ: String? = null
    var MSG_VER_MODULE_MIN: String? = null
    var MSG_REPLY_MODULE_ID: String? = null
    var MSG_REPLY_ARG1: String? = null
    var MSG_REPLY_ARG2: String? = null
    var MSG_REPLY_NO_SEND_WAIT: String? = null
    var SERVICE_PACKAGE_NAME: String? = null
    var MSG_CB_BUNDLE_DB_TEMPLATE_ID_MAX: String? = null
    var MSG_CB_BUNDLE_DB_GROUP_ID_MAX: String? = null
    var MSG_CB_BUNDLE_DB_TEMPLATE: String? = null
    var MSG_CB_BUNDLE_ENROLL_PARAM_DETECT_ZONE: String? = null
    var MSG_CB_BUNDLE_ENROLL_PARAM_DETECT_FACE: String? = null
    var MSG_CB_BUNDLE_ENROLL_PARAM_DETECT_DISTANCE: String? = null
    var MSG_CB_BUNDLE_ENROLL_PARAM_WAITING_UI: String? = null
    var MSG_CB_BUNDLE_ENROLL_PARAM_DETECT_DEPTHMAP: String? = null
    var MSG_CB_BUNDLE_FACE_IS_IR: String? = null
    var MSG_CB_BUNDLE_FACE_HAS_FACE: String? = null
    var MSG_CB_BUNDLE_FACE_RECT_BOUND: String? = null
    var MSG_CB_BUNDLE_FACE_FLOAT_YAW: String? = null
    var MSG_CB_BUNDLE_FACE_FLOAT_ROLL: String? = null
    var MSG_CB_BUNDLE_FACE_FLOAT_EYE_DIST: String? = null
    var MSG_CB_BUNDLE_FACE_POINTS_ARRAY: String? = null
    private var clazz: Class<*>? = null
    private var dbtemplateClass: Class<*>? = null
    private var dbgroupClass: Class<*>? = null

    init {
        clazz = loadClass("android.miui.BiometricConnect")
        SERVICE_PACKAGE_NAME = readStringField("SERVICE_PACKAGE_NAME")
        if (
            shouldReadOptionalMiuiMessageFields()
        ) {
            dbtemplateClass = loadClass("android.miui.BiometricConnect\$DBTemplate")
            dbgroupClass = loadClass("android.miui.BiometricConnect\$DBGroup")
            DEBUG_LOG = readBooleanField("DEBUG_LOG")
            MSG_VER_SER_MAJ = readStringField("MSG_VER_SER_MAJ")
            MSG_VER_SER_MIN = readStringField("MSG_VER_SER_MIN")
            MSG_VER_MODULE_MAJ = readStringField("MSG_VER_MODULE_MAJ")
            MSG_VER_MODULE_MIN = readStringField("MSG_VER_MODULE_MIN")
            MSG_REPLY_MODULE_ID = readStringField("MSG_REPLY_MODULE_ID")
            MSG_REPLY_NO_SEND_WAIT = readStringField("MSG_REPLY_NO_SEND_WAIT")
            MSG_REPLY_ARG1 = readStringField("MSG_REPLY_ARG1")
            MSG_REPLY_ARG2 = readStringField("MSG_REPLY_ARG2")
            MSG_CB_BUNDLE_DB_TEMPLATE_ID_MAX = readStringField("MSG_CB_BUNDLE_DB_TEMPLATE_ID_MAX")
            MSG_CB_BUNDLE_DB_GROUP_ID_MAX = readStringField("MSG_CB_BUNDLE_DB_GROUP_ID_MAX")
            MSG_CB_BUNDLE_DB_TEMPLATE = readStringField("MSG_CB_BUNDLE_DB_TEMPLATE")
            MSG_CB_BUNDLE_ENROLL_PARAM_DETECT_ZONE = readStringField("MSG_CB_BUNDLE_ENROLL_PARAM_DETECT_ZONE")
            MSG_CB_BUNDLE_ENROLL_PARAM_DETECT_FACE = readStringField("MSG_CB_BUNDLE_ENROLL_PARAM_DETECT_FACE")
            MSG_CB_BUNDLE_ENROLL_PARAM_DETECT_DISTANCE = readStringField("MSG_CB_BUNDLE_ENROLL_PARAM_DETECT_DISTANCE")
            MSG_CB_BUNDLE_ENROLL_PARAM_WAITING_UI = readStringField("MSG_CB_BUNDLE_ENROLL_PARAM_WAITING_UI")
            MSG_CB_BUNDLE_ENROLL_PARAM_DETECT_DEPTHMAP = readStringField("MSG_CB_BUNDLE_ENROLL_PARAM_DETECT_DEPTHMAP")
            MSG_CB_BUNDLE_FACE_IS_IR = readStringField("MSG_CB_BUNDLE_FACE_IS_IR")
            MSG_CB_BUNDLE_FACE_HAS_FACE = readStringField("MSG_CB_BUNDLE_FACE_HAS_FACE")
            MSG_CB_BUNDLE_FACE_RECT_BOUND = readStringField("MSG_CB_BUNDLE_FACE_RECT_BOUND")
            MSG_CB_BUNDLE_FACE_FLOAT_YAW = readStringField("MSG_CB_BUNDLE_FACE_FLOAT_YAW")
            MSG_CB_BUNDLE_FACE_FLOAT_ROLL = readStringField("MSG_CB_BUNDLE_FACE_FLOAT_ROLL")
            MSG_CB_BUNDLE_FACE_FLOAT_EYE_DIST = readStringField("MSG_CB_BUNDLE_FACE_FLOAT_EYE_DIST")
            MSG_CB_BUNDLE_FACE_POINTS_ARRAY = readStringField("MSG_CB_BUNDLE_FACE_POINTS_ARRAY")
        }
    }

    private fun loadClass(className: String): Class<*>? {
        return try {
            Class.forName(className)
        } catch (_: ClassNotFoundException) {
            null
        } catch (throwable: Throwable) {
            e(throwable)
            null
        }
    }

    private fun readBooleanField(fieldName: String): Boolean {
        return try {
            clazz?.getField(fieldName)?.getBoolean(null) == true
        } catch (_: NoSuchFieldException) {
            false
        } catch (throwable: Throwable) {
            e(throwable)
            false
        }
    }

    private fun readStringField(fieldName: String): String? {
        return try {
            clazz?.getField(fieldName)?.get(null) as? String
        } catch (_: NoSuchFieldException) {
            null
        } catch (throwable: Throwable) {
            e(throwable)
            null
        }
    }

    fun syncDebugLog() {
        try {
            clazz?.getMethod("syncDebugLog")?.invoke(null)
        } catch (_: NoSuchMethodException) {
        } catch (e: Throwable) {
            e(e)
        }
    }

    fun getDBTemplate(id: Int, name: String?, Data: String?, group_id: Int): Parcelable? {
        return try {
            dbtemplateClass?.getConstructor(
                Int::class.javaPrimitiveType,
                String::class.java,
                String::class.java,
                Int::class.javaPrimitiveType
            )?.newInstance(id, name, Data, group_id) as Parcelable
        } catch (e: Throwable) {
            e(e)
            null
        }
    }

    fun getDBGroup(id: Int, name: String?): Parcelable? {
        return try {
            dbgroupClass?.getConstructor(Int::class.javaPrimitiveType, String::class.java)
                ?.newInstance(id, name) as Parcelable
        } catch (e: Throwable) {
            e(e)
            null
        }
    }
}
