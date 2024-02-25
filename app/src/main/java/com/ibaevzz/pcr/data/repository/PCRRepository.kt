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
    var address = 0
    var is10 = false
    private val tryAttemptsMutex = Mutex()

    private suspend fun sendMessage(message: ByteArray, time: Long = 20): ByteArray? {
        if(!checkConnection()) throw ConnectException()
        withContext(Dispatchers.IO) {
            outputStream?.write(message)
        }
        return readMessage(time)
    }

    private suspend fun readMessage(time: Long = 20): ByteArray? {
        if(!checkConnection()) throw ConnectException()
        return withContext(Dispatchers.IO) {
            val buffer = ByteArray(256)
            var reqResult: ByteArray? = null
            var iteration = 0
            while (iteration <= 10) {
                delay(time)
                if ((inputStream?.available() ?: 0) > 0) {
                    iteration -= 3
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

    private suspend fun tryAttempts(data: ByteArray, time: Long = 20): Result{
        var status: Status = Status.Failure
        var recData: ByteArray? = null
        var responseError = 0
        tryAttemptsMutex.lock()
        for(i in 0..2) {
            recData = sendMessage(data, time)
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

    //TODO needs to be tested
    private fun decodeArchive(recData: ByteArray, seconds: Int): Map<String, Double>{
        val payload = recData.asList().subList(10, 16)
        val calendar = Calendar.getInstance()
        calendar.set(payload[0].toInt() + 2000,
            payload[1].toInt() - 1,
            payload[2].toInt(),
            payload[3].toInt(),
            payload[4].toInt(),
            payload[5].toInt())
        val date = calendar.time
        val archive = recData.asList().subList(16, recData.size - 4)
        val archiveSlices = mutableListOf<ByteArray>()
        for(i in 4 until archive.size + 4 step 4){
            archiveSlices.add(archive.subList(i - 4, i).toByteArray())
        }
        val result = mutableMapOf<String, Double>()
        val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.getDefault())
        for(slice in archiveSlices){
            result[dateFormat.format(date)] = (slice.toDouble() * 10000000).roundToLong().toDouble() / 10000000.0
            date.time += seconds * 1000
        }
        return result
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
            calendar.get(Calendar.HOUR_OF_DAY).toByte(),
            calendar.get(Calendar.MINUTE).toByte(),
            calendar.get(Calendar.SECOND).toByte())
    }

    private fun getRequestDates(startDate: Date, endDate: Date, seconds: Int): List<Pair<Date, Date>>{
        val delta = (endDate.time - startDate.time) / 1000
        val count = ceil((delta.toDouble() / seconds.toDouble()) / 19).toInt()
        val datesList = mutableListOf<Pair<Date, Date>>()
        if(count >= 2){
            var countSeconds = 0
            var start = startDate.clone() as Date
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

    suspend fun getPCRAddress(): Int?{
        val pAddress = splitAddressPulsar("0")
        val pReqNum = encodeReqNum()
        val reqLength = ((pAddress + READ_PARAM + DEV_ADDRESS + pReqNum).size + 3).toBytes(1, ByteOrder.Little)
        val request = (pAddress + READ_PARAM + reqLength + DEV_ADDRESS + pReqNum).injectCRC()
        for(time in times) {
            val result = tryAttempts(request, time)
            if (result.status == Status.Success && result.responseError == 0) {
                address = decodeInteger(result.result)
                return address
            }
        }
        return null
    }

    suspend fun getDeviceType(_address: Int = address): String?{
        val pAddress = splitAddressPulsar(_address.toString())
        val pReqNum = encodeReqNum()
        val reqLength = ((pAddress + READ_PARAM + DEV_TYPE + pReqNum).size + 3).toBytes(1, ByteOrder.Little)
        val request = (pAddress + READ_PARAM + reqLength + DEV_TYPE + pReqNum).injectCRC()
        for(time in times) {
            val result = tryAttempts(request, time)
            if (result.status == Status.Success && result.responseError == 0) {
                return decodeDeviceType(result.result)
            }
        }
        return null
    }

    suspend fun getChannelsWeights(_address: Int = address): Map<Int, Double?>{
        val weights = mutableMapOf<Int, Double?>()
        for(channel in WEIGHT_CHANNEL.indices){
            if(channel >= 10 && is10) break
            weights[channel] = getChannelWeight(_address, channel)
        }
        is10 = true
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
        for(time in times) {
            if(channel >= 10 && is10) break
            val result = tryAttempts(request, time)
            if (result.status == Status.Success && result.responseError == 0) {
                return decodeFloat(result.result)
            }else if(result.responseError == 4){
                is10 = true
                return null
            }
        }
        return null
    }

    suspend fun getChannelsValues(_address: Int = address, channel: Int = -1): Map<Int, Double>?{
        val pReqNum = encodeReqNum()
        val pAddress = splitAddressPulsar(_address.toString())
        val mask = (if (channel == -1) 0xffff else 1 shl channel)
        for(time in times) {
            if (!is10) {
                val pMask = mask.toBytes(4, ByteOrder.Little)
                val reqLength =
                    ((pAddress + READ_CH + pMask + pReqNum).size + 3).toBytes(1, ByteOrder.Little)
                val request = (pAddress + READ_CH + reqLength + pMask + pReqNum).injectCRC()
                val result = tryAttempts(request, time)
                if (result.status == Status.Success && result.responseError == 0) {
                    return decodeChannel(result.result, mask)
                } else if (result.responseError == 2) {
                    is10 = true
                    val mask10 = mask and 0x03ff
                    val pMask10 = mask10.toBytes(4, ByteOrder.Little)
                    val reqLength10 =
                        ((pAddress + READ_CH + pMask10 + pReqNum).size + 3).toBytes(
                            1,
                            ByteOrder.Little
                        )
                    val request10 =
                        (pAddress + READ_CH + reqLength10 + pMask10 + pReqNum).injectCRC()
                    val result10 = tryAttempts(request10, time)
                    if (result10.status == Status.Success && result10.responseError == 0) {
                        return decodeChannel(result10.result, mask10)
                    }
                }
            } else {
                val mask10 = mask and 0x03ff
                val pMask10 = mask10.toBytes(4, ByteOrder.Little)
                val reqLength10 =
                    ((pAddress + READ_CH + pMask10 + pReqNum).size + 3).toBytes(1, ByteOrder.Little)
                val request10 = (pAddress + READ_CH + reqLength10 + pMask10 + pReqNum).injectCRC()
                val result10 = tryAttempts(request10, time)
                if (result10.status == Status.Success && result10.responseError == 0) {
                    return decodeChannel(result10.result, mask10)
                }
            }
        }
        return null
    }

    suspend fun getDate(_address: Int = address): String?{
        val pReqNum = encodeReqNum()
        val pAddress = splitAddressPulsar(_address.toString())
        val pReqLength = ((pAddress + READ_DATE + pReqNum).size + 3).toBytes(1, ByteOrder.Little)
        val request = (pAddress + READ_DATE + pReqLength + pReqNum).injectCRC()
        for(time in times) {
            val result = tryAttempts(request, time)
            if (result.status == Status.Success && result.responseError == 0) {
                return decodeDate(result.result)
            }
        }
        return null
    }

    suspend fun writeDate(_address: Int = address, date: Date?): Boolean{
        val pDate = date?:Date()
        val pReqNum = encodeReqNum()
        val pAddress = splitAddressPulsar(_address.toString())
        val pPayload = encodeDate(pDate)
        val pReqLength = ((pAddress + WRITE_DATE + pPayload + pReqNum).size + 3).toBytes(1, ByteOrder.Little)
        val request = (pAddress + WRITE_DATE + pReqLength + pPayload + pReqNum).injectCRC()
        for(time in times) {
            val result = tryAttempts(request, time)
            if (result.status == Status.Success && result.responseError == 0) {
                return true
            }
        }
        return false
    }

    //TODO needs to be tested
    suspend fun readArchive(_address: Int = address, channel: Int, startDate: Date, endDate: Date, type: ArchiveTypes): Map<String, Double?>{
        val pAddress = splitAddressPulsar(_address.toString())
        val mask = (1 shl (channel-1)).toBytes(4, ByteOrder.Little)
        val pType = type.type.toBytes(2, ByteOrder.Little)
        val pDates = getRequestDates(startDate, endDate, type.seconds)
        val archive = mutableMapOf<String, Double?>()
        for(dates in pDates){
            val pReqNum = encodeReqNum()
            val pStartDate = encodeDate(dates.first)
            val pEndDate = encodeDate(dates.second)
            val pReqLength = ((pAddress + READ_ARCHIVE + mask + pType + pStartDate + pEndDate + pReqNum).size + 3).toBytes(1, ByteOrder.Little)
            val request = (pAddress + READ_ARCHIVE + pReqLength + mask + pType + pStartDate + pEndDate + pReqNum).injectCRC()
            for(time in times) {
                val result = tryAttempts(request, time)
                if (result.status == Status.Success && result.responseError == 0) {
                    for (i in decodeArchive(result.result, type.seconds)) {
                        archive[i.key] = i.value
                    }
                    break
                } else {
                    val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.getDefault())
                    val start = dates.first.clone() as Date
                    while (start.time < dates.second.time) {
                        archive[dateFormat.format(start)] = null
                        start.time += type.seconds
                    }
                    archive[dateFormat.format(dates.second)] = null
                }
            }
        }
        return archive
    }

    //TODO протестировать и проверить работу при использовании 10-канального счетчика
    suspend fun writeChannelsValues(_address: Int = address, values: Map<Int, Double>): Boolean{
        val pReqNum = encodeReqNum()
        val pAddress = splitAddressPulsar(_address.toString())
        val pPayload = mutableListOf<Byte>()
        var mask = 0
        val sortedValues = values.toSortedMap()
        if(0 in values.keys){
            mask = if(is10) 0x03ff else 0xffff
            for(i in 0..if(is10) 9 else 15){
                for(ii in (values[0]?:0.0).toHex()){
                    pPayload += ii
                }
            }
        }else{
            for(i in sortedValues.keys){
                if(i > 10 && is10) break
                mask += 1 shl (i - 1)
                for(ii in (sortedValues[i]?:0.0).toHex()){
                    pPayload += ii
                }
            }
        }
        val pMask = mask.toBytes(4, ByteOrder.Little)
        val pReqLength = ((pAddress + WRITE_CH + pMask + pPayload + pReqNum).size + 3).toBytes(1, ByteOrder.Little)
        val request = (pAddress + WRITE_CH + pReqLength + pMask + pPayload + pReqNum).injectCRC()
        for(time in times) {
            if(!is10) {
                val result = tryAttempts(request, time)
                if (result.status == Status.Success && result.responseError == 0) {
                    return true
                } else if (result.responseError == 2 && 0 in values.keys) {
                    is10 = true
                    val value = values[0] ?: 0.0
                    val values10 = mutableMapOf<Int, Double>()
                    for (i in 1..10) {
                        values10[i] = value
                    }
                    val res = writeChannelsValues(_address, values10)
                    if (res) return true
                }
            }else{
                val result = tryAttempts(request, time)
                if (result.status == Status.Success && result.responseError == 0) return true
            }
        }
        return false
    }

    //TODO протестировать и проверить работу при использовании 10-канального счетчика
    suspend fun writeChannelValue(_address: Int = address, channel: Int, value: Double): Boolean{
        val pReqNum = encodeReqNum()
        val pAddress = splitAddressPulsar(_address.toString())
        val pMask = (1 shl (channel - 1)).toBytes(4, ByteOrder.Little)
        val payload = value.toHex()
        val pReqLength = ((pAddress + WRITE_CH + pMask + payload + pReqNum).size + 3).toBytes(1, ByteOrder.Little)
        val request = (pAddress + WRITE_CH + pReqLength + pMask + payload + pReqNum).injectCRC()
        if(is10 && channel >= 10) return false
        for(time in times) {
            val result = tryAttempts(request, time)
            if (result.status == Status.Success && result.responseError == 0) {
                return true
            }
        }
        return false
    }

    suspend fun writeChannelsWeights(_address: Int = address, values: Map<Int, Double>): Map<Int, Boolean>{
        val pValues = values.toMutableMap()
        if(0 in values.keys){
            for(i in 1..16){
                pValues[i] = values[0]?:0.0
            }
        }
        val results = mutableMapOf<Int, Boolean>()
        for(i in pValues){
            results[i.key] = writeChannelWeight(_address, i.key - 1, i.value)
        }
        return results
    }

    suspend fun writeChannelWeight(_address: Int = address, channel: Int, weight: Double): Boolean{
        if(is10 && channel>=10) return false
        val pReqNum = encodeReqNum()
        val pAddress = splitAddressPulsar(_address.toString())
        val payload = weight.toHex()
        val pReqLength = ((pAddress + WRITE_PARAM + WEIGHT_CHANNEL[channel] + payload + pReqNum).size + 7).toBytes(1, ByteOrder.Little)
        val request = (pAddress + WRITE_PARAM + pReqLength + WEIGHT_CHANNEL[channel] + payload + byteArrayOf(0, 0, 0, 0) + pReqNum).injectCRC()
        for(time in times) {
            val result = tryAttempts(request, time)
            if (result.status == Status.Success && result.responseError == 0) {
                return true
            }
        }
        return false
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

        enum class ArchiveTypes(val type: Int, val seconds: Int){
            HOUR(1, PCRRepository.HOUR),
            DAY(2, PCRRepository.DAY),
            MONTH(3, PCRRepository.MONTH)
        }

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

        private val times = listOf(20L, 50L, 100L)
    }

}