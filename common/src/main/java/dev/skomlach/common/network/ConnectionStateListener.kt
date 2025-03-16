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
package dev.skomlach.common.network

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.ConnectivityManager.NetworkCallback
import android.net.Network
import android.net.NetworkRequest
import android.net.wifi.WifiManager
import android.os.Build
import dev.skomlach.common.contextprovider.AndroidContext.appContext
import dev.skomlach.common.logging.LogCat
import dev.skomlach.common.misc.BroadcastTools
import java.util.concurrent.atomic.AtomicBoolean

class ConnectionStateListener {

    private val isConnectionOk = AtomicBoolean(false)
    private val ping: Ping = Ping(this)
    private var connectivityManager: ConnectivityManager? = null
    private var networkCallback: NetworkCallback? = null
    private var receiverTypeConnection: BroadcastReceiver? = null
    

    init {
        connectivityManager =
            appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager?

        isConnectionOk.set(isConnectionDetected())
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            networkCallback = object : NetworkCallback() {

                override fun onUnavailable() {
                    ping.updateConnectionCheckQuery(1)
                }

                override fun onAvailable(network: Network) {
                    ping.updateConnectionCheckQuery(1)
                }

                override fun onLost(network: Network) {
                    ping.updateConnectionCheckQuery(1)
                }
            }
        }

        //pre-Lollipop devices
        receiverTypeConnection = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                ping.updateConnectionCheckQuery(1)
            }
        }
        ping.updateConnectionCheckQuery(0)
    }

    fun isConnectionDetected() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
        connectivityManager?.isDefaultNetworkActive == true || connectivityManager?.activeNetworkInfo?.isConnectedOrConnecting == true
    else
        connectivityManager?.activeNetworkInfo?.isConnectedOrConnecting == true

    fun startListeners() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                //Looking for the network changes
                networkCallback?.let {
                    connectivityManager?.registerNetworkCallback(
                        NetworkRequest.Builder().build(),
                        it
                    )
                }
            } else {
                //NOTE: almost all intent actions deprecated for latest Android OS versions (05/2019)
                val intentFilter = IntentFilter()
                intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION)
                intentFilter.addAction(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION)
                intentFilter.addAction(WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION)
                intentFilter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION)
                BroadcastTools.registerGlobalBroadcastIntent(
                    appContext,
                    receiverTypeConnection,
                    intentFilter
                )
            }
        } catch (_: Throwable) {
        }
    }

    fun stopListeners() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                networkCallback?.let {
                    networkCallback
                    connectivityManager?.unregisterNetworkCallback(it)
                }
            } else {
                BroadcastTools.unregisterGlobalBroadcastIntent(appContext, receiverTypeConnection)
            }
        } catch (_: Throwable) {
        }
    }

    fun setState(newState: Boolean) {
        //cancel  SocketCheck if it planned
        ping.cancelConnectionCheckQuery()
        if (newState != isConnectionOk.get()) {
            isConnectionOk.set(newState)
            LogCat.log(
                "ConnectionStateListener detected new connection state - $newState"
            )
            Connection.checkConnectionChanged()
        }
    }

    fun updateConnectionCheckQuery(delaySeconds: Long) {
        ping.updateConnectionCheckQuery(delaySeconds)
    }

    val isConnected: Boolean
        get() = isConnectionOk.get()
}