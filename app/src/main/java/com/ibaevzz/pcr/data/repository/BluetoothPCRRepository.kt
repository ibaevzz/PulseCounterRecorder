package com.ibaevzz.pcr.data.repository

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import com.ibaevzz.pcr.UUID
import com.ibaevzz.pcr.domain.repository.ConnectToDevice
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.*
import javax.inject.Inject

class BluetoothPCRRepository @Inject constructor(private val context: Context): ConnectToDevice{

    private var outputStream: OutputStream? = null
    private var inputStream: InputStream? = null

    @SuppressLint("MissingPermission")
    override suspend fun connect(address: String) {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val adapter = bluetoothManager.adapter

        if(!adapter.isEnabled) throw IOException("Нет доступа к Bluetooth адаптеру")

        val device: BluetoothDevice? = adapter.getRemoteDevice(address)
        val bluetoothSocket = device?.createInsecureRfcommSocketToServiceRecord(java.util.UUID.fromString(UUID))

        withContext(Dispatchers.IO) {
            bluetoothSocket?.connect()
            outputStream = bluetoothSocket?.outputStream
            inputStream = bluetoothSocket?.inputStream
        }
    }

}