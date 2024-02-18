package com.ibaevzz.pcr.data.repository

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import com.ibaevzz.pcr.UUID
import com.ibaevzz.pcr.data.exceptions.BluetoothTurnedOffException
import com.ibaevzz.pcr.data.exceptions.ConnectException
import com.ibaevzz.pcr.di.bluetooth.BluetoothScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.*
import javax.inject.Inject

@BluetoothScope
class BluetoothConnectRepository @Inject constructor(private val context: Context): ConnectRepository{

    private var outputStream: OutputStream? = null
    private var inputStream: InputStream? = null
    private var bluetoothSocket: BluetoothSocket? = null
    private lateinit var adapter: BluetoothAdapter
    private val connectMutex = Mutex()
    private val closeMutex = Mutex()

    @SuppressLint("MissingPermission")
    override suspend fun connect(address: String, port: String) {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        adapter = bluetoothManager.adapter

        if(!::adapter.isInitialized) throw IOException()

        if(!adapter.isEnabled) throw BluetoothTurnedOffException()

        val device: BluetoothDevice? = adapter.getRemoteDevice(address)
        bluetoothSocket = device?.createInsecureRfcommSocketToServiceRecord(java.util.UUID.fromString(UUID))

        withContext(Dispatchers.IO) {
            connectMutex.lock()
            try {
                if(bluetoothSocket?.isConnected==false) {
                    bluetoothSocket?.connect()
                    outputStream = bluetoothSocket?.outputStream
                    inputStream = bluetoothSocket?.inputStream
                }
            }catch(ex: Exception){
                throw ex
            }finally {
                connectMutex.unlock()
            }
        }
    }

    override suspend fun closeConnection() {
        if(!adapter.isEnabled) throw BluetoothTurnedOffException()
        if(bluetoothSocket?.isConnected != true) throw ConnectException()

        withContext(Dispatchers.IO) {
            try {
                closeMutex.lock()
                outputStream?.close()
                inputStream?.close()
                bluetoothSocket?.close()
            } catch (ex: Exception) {
                throw ex
            } finally {
                closeMutex.unlock()
            }
        }
    }

}