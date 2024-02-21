package com.ibaevzz.pcr.data.repository

import com.ibaevzz.pcr.ByteOrder
import com.ibaevzz.pcr.checkCRC
import com.ibaevzz.pcr.data.exceptions.ConnectException
import com.ibaevzz.pcr.toBytes
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream

abstract class PCRRepository(private val inputDispatcher: CoroutineDispatcher,
                             private val outputDispatcher: CoroutineDispatcher){

    protected abstract var inputStream: InputStream?
    protected abstract var outputStream: OutputStream?

    protected abstract fun checkConnection(): Boolean
    abstract suspend fun connect(address: String, port: String = "")
    abstract suspend fun closeConnection()

    private suspend fun sendMessage(message: ByteArray): ByteArray? {
        if(!checkConnection()) throw ConnectException()
        withContext(outputDispatcher) {
            outputStream?.write(message)
        }
        return readMessage()
    }

    private suspend fun readMessage(): ByteArray? {
        if(!checkConnection()) throw ConnectException()
        return withContext(inputDispatcher) {
            val buffer = ByteArray(256)
            var reqResult: ByteArray? = null
            while (true) {
                delay(1000)
                if ((inputStream?.available() ?: 0) > 0) {
                    val bytes = inputStream?.read(buffer) ?: -1
                    if (bytes != -1) {
                        val result = ByteArray(bytes)
                        for (i in 0 until bytes) {
                            result[i] = buffer[i]
                        }
                        reqResult = if (reqResult != null) reqResult + result else result
                    }
                } else break
            }
            return@withContext reqResult
        }
    }

    private suspend fun tryAttempts(data: ByteArray): Pair<Status, ByteArray>{
        var status: Status = Status.Failure
        var recData: ByteArray? = null
        for(i in 0..2) {
            recData = sendMessage(data)
            if (recData != null && recData.isNotEmpty()) {
                if (recData.checkCRC()){
                    status = Status.Success
                    break
                }
            }
        }
        return status to (recData?:ByteArray(0))
    }

    private fun isValidAddress(address: String) = try {address.toInt()}
    catch (_: NumberFormatException){-1} in 0..99999999

    private fun splitAddressPulsar(address: String): ByteArray{
        return address.toInt(16).toBytes(4, ByteOrder.Big)
    }

    sealed interface Status{
        object Success: Status
        object Failure: Status
    }
}