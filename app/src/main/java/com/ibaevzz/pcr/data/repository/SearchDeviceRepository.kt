package com.ibaevzz.pcr.data.repository

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import com.ibaevzz.pcr.data.dto.Device
import com.ibaevzz.pcr.di.bluetooth.BluetoothScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject

@SuppressLint("MissingPermission")
@BluetoothScope
class SearchDeviceRepository @Inject constructor(private val bluetoothManager: BluetoothManager,
                                                 private val context: Context){

    private val _foundDevices = MutableSharedFlow<List<Device>>(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    private var listOfDevices = mutableListOf<Device>()
    private var listOfAddress = mutableListOf<String>()

    private val searchBroadcastReceiver = object: BroadcastReceiver(){
        override fun onReceive(context: Context, intent: Intent) {
            val action: String? = intent.action
            if(BluetoothDevice.ACTION_FOUND == action){
                @Suppress("DEPRECATION")
                val device: BluetoothDevice? = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                if (device?.name == null || device.address == null) return
                if (device.name.isNotEmpty() && device.address !in listOfAddress) {
                    listOfDevices.add(Device(device.name, device.address))
                    listOfAddress.add(device.address)
                    _foundDevices.tryEmit(listOfDevices)
                }
            }
        }
    }

    private val discoveryStartedReceiver = object: BroadcastReceiver(){
        override fun onReceive(context: Context, intent: Intent) {
            if(intent.action == BluetoothAdapter.ACTION_DISCOVERY_FINISHED){
                try {
                    context.unregisterReceiver(searchBroadcastReceiver)
                }catch (_: Exception){}
                bluetoothManager.adapter.startDiscovery()
            }else if(intent.action == BluetoothAdapter.ACTION_DISCOVERY_STARTED){
                context.registerReceiver(searchBroadcastReceiver, IntentFilter().also {
                    it.addAction(BluetoothDevice.ACTION_FOUND)
                })
            }
        }
    }

    fun search(): SharedFlow<List<Device>> {
        context.registerReceiver(discoveryStartedReceiver, IntentFilter().also{
            it.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED)
            it.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        })
        bluetoothManager.adapter.startDiscovery()
        return _foundDevices.asSharedFlow()
    }

    fun restart(){
        bluetoothManager.adapter.cancelDiscovery()
        listOfDevices = mutableListOf()
        listOfAddress = mutableListOf()
    }

    fun stopSearch(){
        try {
            context.unregisterReceiver(searchBroadcastReceiver)
            context.unregisterReceiver(discoveryStartedReceiver)
            bluetoothManager.adapter.cancelDiscovery()
        }catch (_: Exception){}
    }
}