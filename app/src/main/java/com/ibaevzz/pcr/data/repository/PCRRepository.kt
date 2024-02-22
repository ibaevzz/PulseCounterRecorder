package com.ibaevzz.pcr.data.repository

import com.ibaevzz.pcr.*
import com.ibaevzz.pcr.data.exceptions.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.ceil
import kotlin.math.roundToLong

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
            var iteration = 0
            while (iteration<=20) {
                delay(20)
                if ((inputStream?.available() ?: 0) > 0) {
                    val bytes = inputStream?.read(buffer) ?: -1
                    if (bytes != -1) {
                        val result = ByteArray(bytes)
                        for (i in 0 until bytes) {
                            result[i] = buffer[i]
                        }
                        reqResult = if (reqResult != null) reqResult + result else result
                    }
                } else iteration+=1
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

    private fun decodeFloat(recData: ByteArray, shift: Int = 8): Double{
        val payload = recData.asList().subList(6, recData.size - shift).toByteArray()
        return (payload.toDouble() * 10000000).roundToLong().toDouble() / 10000000.0
    }

    private fun decodeDeviceType(recData: ByteArray): String{
        val payload = recData.asList().subList(6, recData.size - 4).toByteArray()
        return DEV_TYPES[payload.fromBytes(ByteOrder.Little)]?:"Неизвестный прибор"
    }

    private fun decodeChannel(recData: ByteArray, mask: Int): Map<Int, Double>{
        val channelBytes = recData.asList().subList(6, recData.size - 4).toByteArray()
        val channelSlices = mutableListOf<ByteArray>()
        val channelNumber = mutableListOf<Int>()
        val values = mutableMapOf<Int, Double>()
        for(i in 4 until channelBytes.size + 4 step 4){
            channelSlices.add(channelBytes.asList().subList(i - 4, i).toByteArray())
        }
        for(i in 0 until 16){
            if(mask and (1 shl i) == 1 shl i){
                channelNumber.add(i)
            }
        }
        for(i in channelSlices.indices){
            values[channelNumber[i]] = (channelSlices[i].toDouble() * 10000000).roundToLong().toDouble() / 10000000.0
        }
        return values
    }

    private fun decodeDate(recData: ByteArray): String{
        val payload = recData.asList().subList(6, recData.size - 4).toByteArray()
        val calendar = Calendar.getInstance()
        calendar.set(payload[0].toInt() + 2000,
            payload[1].toInt() - 1,
            payload[2].toInt(),
            payload[3].toInt(),
            payload[4].toInt(),
            payload[5].toInt())
        val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.getDefault())
        return dateFormat.format(calendar.time)
    }

    private fun encodeReqNum(): ByteArray{
        reqNum += 1
        return reqNum.toBytes(2, ByteOrder.Little)
    }

    private fun encodeDate(date: Date = Date()): ByteArray{
        val calendar = Calendar.getInstance()
        calendar.time = date
        return byteArrayOf((calendar.get(Calendar.YEAR) - 2000).toByte(),
            (calendar.get(Calendar.MONTH) + 1).toByte(),
            calendar.get(Calendar.DAY_OF_MONTH).toByte(),
            calendar.get(Calendar.HOUR).toByte(),
            calendar.get(Calendar.MINUTE).toByte(),
            calendar.get(Calendar.SECOND).toByte())
    }

    private fun getRequestDates(startDate: Date, endDate: Date, seconds: Int): List<Pair<Date, Date>>{
        val delta = (endDate.time - startDate.time) / 1000
        val count = ceil((delta.toDouble() / seconds.toDouble()) / 19).toInt()
        val datesList = mutableListOf<Pair<Date, Date>>()
        if(count >= 2){
            var countSeconds = 0
            var start = startDate
            for(i in 0 until count){
                countSeconds += seconds * 19
                val end = Date()
                end.time = seconds.toLong() * 19 * 1000L + start.time
                if(end.time >= endDate.time){
                    val dates = Pair(start, endDate)
                    datesList.add(dates)
                    break
                }else{
                    val dates = Pair(start, end)
                    datesList.add(dates)
                    start = end
                }
            }
        }else{
            datesList.add(Pair(startDate, endDate))
        }
        return datesList
    }

    private fun splitAddressPulsar(pAddress: String): ByteArray{
        return pAddress.toUInt(16).toInt().toBytes(4, ByteOrder.Big)
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

    suspend fun getDeviceType(_address: Int = address): String{
        val pAddress = splitAddressPulsar(_address.toString())
        val pReqNum = encodeReqNum()
        val reqLength = ((pAddress + READ_PARAM + DEV_TYPE + pReqNum).size + 3).toBytes(1, ByteOrder.Little)
        val request = (pAddress + READ_PARAM + reqLength + DEV_TYPE + pReqNum).injectCRC()
        val result = tryAttempts(request)
        if(result.status == Status.Success && result.responseError == 0){
            return decodeDeviceType(result.result)
        }
        throw ReadDeviceTypeException()
    }

    suspend fun getChannelsWeights(_address: Int = address): Map<Int, Double?>{
        val weights = mutableMapOf<Int, Double?>()
        for(channel in WEIGHT_CHANNEL.indices){
            weights[channel] = getChannelWeight(_address, channel)
        }
        var is10 = true
        for(channel in 10..15){
            if(weights[channel] != null){
                is10 = false
                break
            }
        }
        if(is10){
            return weights.filterKeys { it < 10 }
        }
        return weights
    }

    suspend fun getChannelWeight(_address: Int = address, channel: Int): Double?{
        val pAddress = splitAddressPulsar(_address.toString())
        val pReqNum = encodeReqNum()
        val reqLength = ((pAddress + READ_PARAM + WEIGHT_CHANNEL[channel] + pReqNum).size + 3).toBytes(1, ByteOrder.Little)
        val request = (pAddress + READ_PARAM + reqLength + WEIGHT_CHANNEL[channel] + pReqNum).injectCRC()
        val result = tryAttempts(request)
        if(result.status == Status.Success && result.responseError == 0){
            return decodeFloat(result.result)
        }
        return null
    }

    suspend fun getChannelsValues(_address: Int = address, channel: Int = -1): Map<Int, Double>{
        val pReqNum = encodeReqNum()
        val pAddress = splitAddressPulsar(_address.toString())
        val mask = (if(channel == -1) 0xffff else 1 shl channel)
        val pMask = mask.toBytes(4, ByteOrder.Little)
        val reqLength = ((pAddress + READ_CH + pMask + pReqNum).size + 3).toBytes(1, ByteOrder.Little)
        val request = (pAddress + READ_CH + reqLength + pMask + pReqNum).injectCRC()
        val result = tryAttempts(request)
        if(result.status == Status.Success && result.responseError == 0){
            return decodeChannel(result.result, mask)
        }else if(result.responseError == 2){
            val mask10 = mask and 0x03ff
            val pMask10 = mask10.toBytes(4, ByteOrder.Little)
            val reqLength10 = ((pAddress + READ_CH + pMask10 + pReqNum).size + 3).toBytes(1, ByteOrder.Little)
            val request10 = (pAddress + READ_CH + reqLength10 + pMask10 + pReqNum).injectCRC()
            val result10 = tryAttempts(request10)
            if(result10.status == Status.Success && result10.responseError == 0) {
                return decodeChannel(result10.result, mask10)
            }
        }
        throw ReadChannelsException()
    }

    //TODO needs to be tested
    suspend fun getDate(_address: Int = address): String{
        val pReqNum = encodeReqNum()
        val pAddress = splitAddressPulsar(_address.toString())
        val pReqLength = ((pAddress + READ_DATE + pReqNum).size + 3).toBytes(1, ByteOrder.Little)
        val request = (pAddress + READ_DATE + pReqLength + pReqNum).injectCRC()
        val result = tryAttempts(request)
        if(result.status == Status.Success && result.responseError == 0){
            return decodeDate(result.result)
        }
        throw ReadDateException()
    }

    //TODO needs to be tested
    suspend fun writeDate(_address: Int = address, date: Date?): Boolean{
        val pDate = date?:Date()
        val pReqNum = encodeReqNum()
        val pAddress = splitAddressPulsar(_address.toString())
        val pPayload = encodeDate(pDate)
        val pReqLength = ((pAddress + WRITE_DATE + pPayload + pReqNum).size + 3).toBytes(1, ByteOrder.Little)
        val request = (pAddress + WRITE_DATE + pReqLength + pPayload + pReqNum).injectCRC()
        val result = tryAttempts(request)
        if(result.status == Status.Success && result.responseError == 0){
            return true
        }
        throw WriteDateException()
    }

    private sealed interface Status{
        object Success: Status
        object Failure: Status
    }
    private class Result(val status: Status, val result: ByteArray, val responseError: Int)

    companion object{

        const val HOUR  = 60 * 60
        const val DAY   = 60 * 60 * 24
        const val MONTH = 60 * 60 * 24 * 30

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

        private val DEV_TYPES = mapOf(
            424 to "Пульсар счетчик импульсов 10/16K v1"
        )
    }

}