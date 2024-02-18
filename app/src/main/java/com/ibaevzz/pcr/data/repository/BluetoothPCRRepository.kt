package com.ibaevzz.pcr.data.repository

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import com.ibaevzz.pcr.UUID
import com.ibaevzz.pcr.data.exceptions.BluetoothTurnedOffException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.ConnectException
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BluetoothPCRRepository @Inject constructor(private val context: Context): PCRRepository{

    private var outputStream: OutputStream? = null
    private var inputStream: InputStream? = null
    private var bluetoothSocket: BluetoothSocket? = null
    private lateinit var adapter: BluetoothAdapter
    private val mutex = Mutex()

    @SuppressLint("MissingPermission")
    override suspend fun connect(address: String) {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        adapter = bluetoothManager.adapter

        if(!::adapter.isInitialized) throw IOException()

        if(!adapter.isEnabled) throw BluetoothTurnedOffException()

        val device: BluetoothDevice? = adapter.getRemoteDevice(address)
        bluetoothSocket = device?.createInsecureRfcommSocketToServiceRecord(java.util.UUID.fromString(UUID))

        withContext(Dispatchers.IO) {
            mutex.lock()
            if(bluetoothSocket?.isConnected==false) {
                bluetoothSocket?.connect()
                outputStream = bluetoothSocket?.outputStream
                inputStream = bluetoothSocket?.inputStream
            }
            mutex.unlock()
        }
    }

    override fun closeConnection() {
        if(!adapter.isEnabled) throw BluetoothTurnedOffException()
        if(bluetoothSocket?.isConnected != true) throw ConnectException()

        outputStream?.close()
        inputStream?.close()
        bluetoothSocket?.close()
    }

}