package com.ibaevzz.pcr.data.repository

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import com.ibaevzz.pcr.UUID
import com.ibaevzz.pcr.data.exceptions.BluetoothTurnedOffException
import com.ibaevzz.pcr.di.InputQualifier
import com.ibaevzz.pcr.di.OutputQualifier
import com.ibaevzz.pcr.di.bluetooth.BluetoothScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream
import java.util.*
import javax.inject.Inject

@BluetoothScope
class BluetoothPCRRepository @Inject constructor(
    bluetoothManager: BluetoothManager,
    @InputQualifier private val inputDispatcher: CoroutineDispatcher,
    @OutputQualifier private val outputDispatcher: CoroutineDispatcher)
    : PCRRepository(inputDispatcher, outputDispatcher){

    override var outputStream: OutputStream? = null
    override var inputStream: InputStream? = null
    private var bluetoothSocket: BluetoothSocket? = null
    private val isConnect get() = bluetoothSocket?.isConnected ?: false
    private var adapter: BluetoothAdapter = bluetoothManager.adapter
    private val connectMutex = Mutex()
    private val closeMutex = Mutex()

    override fun checkConnection(): Boolean{
        if(!adapter.isEnabled) throw BluetoothTurnedOffException()
        return isConnect
    }

    @SuppressLint("MissingPermission")
    override suspend fun connect(address: String, port: String) {
        if(checkConnection()) return
        withContext(Dispatchers.IO) {
            connectMutex.lock()
            if(isConnect) return@withContext
            try {
                val device: BluetoothDevice? = adapter.getRemoteDevice(address)
                bluetoothSocket = device?.createInsecureRfcommSocketToServiceRecord(java.util.UUID.fromString(UUID))
                if(bluetoothSocket?.isConnected==false) {
                    bluetoothSocket?.connect()
                    outputStream = bluetoothSocket?.outputStream
                    inputStream = bluetoothSocket?.inputStream
                }
            }finally {
                connectMutex.unlock()
            }
        }
    }

    override suspend fun closeConnection() {
        if(!checkConnection()) return
        withContext(Dispatchers.IO) {
            try {
                closeMutex.lock()
                if(!isConnect) return@withContext
                outputStream?.close()
                inputStream?.close()
                bluetoothSocket?.close()
            }finally {
                closeMutex.unlock()
            }
        }
    }

}