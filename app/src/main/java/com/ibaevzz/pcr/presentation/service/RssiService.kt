package com.ibaevzz.pcr.presentation.service

import android.annotation.SuppressLint
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Binder
import com.ibaevzz.pcr.di.bluetooth.BluetoothComponent
import com.ibaevzz.pcr.presentation.activity.ConnectActivity
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject

@SuppressLint("MissingPermission")
class RssiService: Service(){

    private val rssi = MutableSharedFlow<Short>(replay = 1)
    private val scope = CoroutineScope(Dispatchers.Default)
    private var address: String? = null

    @Inject
    lateinit var bluetoothManager: BluetoothManager

    private var rssiBroadcastReceiver = RssiBroadcastReceiver()
    private val discoveryStartedReceiver = DiscoveryStartedReceiver()

    override fun onCreate() {
        super.onCreate()
        BluetoothComponent.init(applicationContext).inject(this)
    }

    override fun onBind(intent: Intent): Binder {
        address = intent.getStringExtra(ConnectActivity.ADDRESS_EXTRA)
        registerReceiver(discoveryStartedReceiver, IntentFilter().also {
            it.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED)
            it.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        })
        registerReceiver(rssiBroadcastReceiver, IntentFilter().also {
            it.addAction(BluetoothDevice.ACTION_FOUND)
        })
        bluetoothManager.adapter.startDiscovery()
        scope.launch {
            rssi.collect{
                unregisterReceiver(rssiBroadcastReceiver)
                rssiBroadcastReceiver = RssiBroadcastReceiver()
                if(bluetoothManager.adapter.isDiscovering) {
                    bluetoothManager.adapter.cancelDiscovery()
                }
                bluetoothManager.adapter.startDiscovery()
                registerReceiver(rssiBroadcastReceiver, IntentFilter().also {
                    it.addAction(BluetoothDevice.ACTION_FOUND)
                })
            }
        }
        return RssiBinder(rssi.asSharedFlow())
    }

    override fun onDestroy() {
        scope.cancel()
        try {
            unregisterReceiver(rssiBroadcastReceiver)
            unregisterReceiver(discoveryStartedReceiver)
        }catch (_: Exception){}
        super.onDestroy()
    }

    class RssiBinder(val rssi: SharedFlow<Short>): Binder()

    inner class RssiBroadcastReceiver: BroadcastReceiver(){
        override fun onReceive(context: Context, intent: Intent) {
            if(intent.action == BluetoothDevice.ACTION_FOUND){
                val value = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, 0)
                val device: BluetoothDevice? = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                if((device?.address ?: "") == address) {
                    scope.launch {
                        rssi.emit(value)
                    }
                }
            }
        }
    }

    inner class DiscoveryStartedReceiver: BroadcastReceiver(){
        override fun onReceive(context: Context, intent: Intent) {
            if(intent.action == BluetoothAdapter.ACTION_DISCOVERY_FINISHED){
                bluetoothManager.adapter.startDiscovery()
            }else if(intent.action == BluetoothAdapter.ACTION_DISCOVERY_STARTED){
                this@RssiService.registerReceiver(rssiBroadcastReceiver, IntentFilter().also {
                    it.addAction(BluetoothDevice.ACTION_FOUND)
                })
            }
        }
    }
}