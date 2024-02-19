package com.ibaevzz.pcr.data.repository

import android.net.wifi.WifiManager
import com.ibaevzz.pcr.data.exceptions.WifiTurnedOffException
import com.ibaevzz.pcr.di.wifi.WifiScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.InetAddress
import java.net.Socket
import java.net.SocketAddress
import java.net.SocketException
import javax.inject.Inject

@WifiScope
class WifiPCRRepository @Inject constructor(private val wifiManager: WifiManager): PCRRepository() {

    override var inputStream: InputStream? = null
    override var outputStream: OutputStream? = null
    private var socket: Socket? = null
    override val isConnect get() = socket?.isConnected?:false
    private val connectMutex = Mutex()
    private val closeMutex = Mutex()

    override suspend fun connect(address: String, port: String) {
        if(isConnect) return
        withContext(Dispatchers.IO) {
            connectMutex.lock()
            try {
                closeConnection()

                val ip = address.toInt()

                socket = Socket(InetAddress.getByAddress(byteArrayOf(
                    (ip).toByte(),
                    (ip ushr 8).toByte(),
                    (ip ushr 16).toByte(),
                    (ip ushr 24).toByte())), port.toInt())

                socket?.connect(object : SocketAddress() {})
                if (socket?.isConnected != true) throw IOException()
                inputStream = socket?.getInputStream()
                outputStream = socket?.getOutputStream()
            }catch (_: SocketException){ }
            finally {
                connectMutex.unlock()
            }
        }
    }

    override suspend fun closeConnection() {
        if(!wifiManager.isWifiEnabled) throw WifiTurnedOffException()
        if(!isConnect) return

        withContext(Dispatchers.IO){
            try {
                closeMutex.lock()
                outputStream?.close()
                inputStream?.close()
                socket?.close()
            } catch (ex: Exception) {
                throw ex
            } finally {
                closeMutex.unlock()
            }
        }
    }

}