package com.ibaevzz.pcr.data.repository

import com.ibaevzz.pcr.*
import com.ibaevzz.pcr.data.exceptions.ConnectException
import com.ibaevzz.pcr.data.exceptions.CouldNotDetermineAddress
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream

abstract class PCRRepository{

    protected abstract var inputStream: InputStream?
    protected abstract var outputStream: OutputStream?

    protected abstract fun checkConnection(): Boolean
    abstract suspend fun connect(address: String, port: String = "")
    abstract suspend fun closeConnection()

    private var reqNum = 0
    private var address = 0
    private val tryAttemptsMutex = Mutex()

    private suspend fun sendMessage(message: ByteArray): ByteArray? {
        if(!checkConnection()) throw ConnectException()
        withContext(Dispatchers.IO) {
            outputStream?.write(message)
        }
        return readMessage()
    }

    private suspend fun readMessage(): ByteArray? {
        if(!checkConnection()) throw ConnectException()
        return withContext(Dispatchers.IO) {
            val buffer = ByteArray(256)
            var reqResult: ByteArray? = null
            while (true) {
                delay(500)
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

    private suspend fun tryAttempts(data: ByteArray): Result{
        var status: Status = Status.Failure
        var recData: ByteArray? = null
        var responseError = 0
        tryAttemptsMutex.lock()
        for(i in 0..2) {
            recData = sendMessage(data)
            if (recData != null && recData.isNotEmpty()) {
                if (recData.checkCRC()){
                    status = Status.Success
                    responseError = if(recData[4].toUByte()==0.toUByte()) recData[6].toUByte().toInt() else 0
                    break
                }
            }
        }
        tryAttemptsMutex.unlock()
        return Result(status, recData?:ByteArray(0), responseError)
    }

    private fun decodeInteger(recData: ByteArray) = recData
            .asList()
            .subList(6, recData.size - 4)
            .toByteArray()
            .fromBytes(ByteOrder.Little)

    private fun encodeReqNum(): ByteArray{
        reqNum += 1
        return reqNum.toBytes(2, ByteOrder.Little)
    }

    private fun splitAddressPulsar(address: String): ByteArray{
        return address.toUInt(16).toInt().toBytes(4, ByteOrder.Big)
    }


    suspend fun getPCRAddress(): Int{
        val pAddress = splitAddressPulsar("0")
        val pReqNum = encodeReqNum()
        val reqLength = ((pAddress + READ_PARAM + DEV_ADDRESS + pReqNum).size + 3).toBytes(1, ByteOrder.Little)
        val request = (pAddress + READ_PARAM + reqLength + DEV_ADDRESS + pReqNum).injectCRC()
        val result = tryAttempts(request)
        if(result.status == Status.Success && result.responseError == 0){
            address = decodeInteger(result.result)
            return address
        }
        throw CouldNotDetermineAddress(result.result.size, result.responseError)
    }


    sealed interface Status{
        object Success: Status
        object Failure: Status
    }
    class Result(val status: Status, val result: ByteArray, val responseError: Int)

    companion object{
        private val READ_DATE    = byteArrayOf(0x04)
        private val WRITE_DATE   = byteArrayOf(0x05)
        private val READ_CH      = byteArrayOf(0x01)
        private val WRITE_CH     = byteArrayOf(0x03)
        private val READ_ARCHIVE = byteArrayOf(0x06)
        private val READ_PARAM   = byteArrayOf(0x0a)
        private val WRITE_PARAM  = byteArrayOf(0x0b)

        private val DEV_TYPE    = byteArrayOf(0x00, 0x00)
        private val DEV_ADDRESS = byteArrayOf(0x01, 0x00)

        private val WEIGHT_CHANNEL = arrayOf(
            byteArrayOf(0x20, 0x00),
            byteArrayOf(0x21, 0x00),
            byteArrayOf(0x22, 0x00),
            byteArrayOf(0x23, 0x00),
            byteArrayOf(0x24, 0x00),
            byteArrayOf(0x25, 0x00),
            byteArrayOf(0x26, 0x00),
            byteArrayOf(0x27, 0x00),
            byteArrayOf(0x28, 0x00),
            byteArrayOf(0x29, 0x00),
            byteArrayOf(0x2a, 0x00),
            byteArrayOf(0x2b, 0x00),
            byteArrayOf(0x2c, 0x00),
            byteArrayOf(0x2d, 0x00),
            byteArrayOf(0x2e, 0x00),
            byteArrayOf(0x2f, 0x00),
        )
    }

}