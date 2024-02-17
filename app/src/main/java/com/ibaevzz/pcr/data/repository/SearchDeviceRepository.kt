package com.ibaevzz.pcr.data.repository

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import com.ibaevzz.pcr.domain.entity.Device
import com.ibaevzz.pcr.domain.repository.SearchDevice
import com.ibaevzz.pcr.domain.repository.StopSearch
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

@SuppressLint("MissingPermission")
@Singleton
class SearchDeviceRepository @Inject constructor(private val context: Context): SearchDevice, StopSearch{

    private val _foundDevices = MutableSharedFlow<List<Device>>(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    private val listOfDevices = mutableListOf<Device>()
    private val listOfAddress = mutableListOf<String>()

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

    override fun search(): SharedFlow<List<Device>> {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter.startDiscovery()
        context.registerReceiver(searchBroadcastReceiver, IntentFilter().also {
            it.addAction(BluetoothDevice.ACTION_FOUND)
        })
        return _foundDevices.asSharedFlow()
    }

    override fun stopSearch(){
        context.unregisterReceiver(searchBroadcastReceiver)
    }
}