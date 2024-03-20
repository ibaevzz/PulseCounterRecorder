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
    var countOfChannels = -1
    private var isLegacy = false
    private var devType = -1
    private val tryAttemptsMutex = Mutex()

    fun clearDevice(){
        reqNum = 0
        address = 0
        countOfChannels = -1
        isLegacy = false
        devType = -1
    }

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
        .subList(6, recData.size - 8)
        .toByteArray()
        .fromBytes(ByteOrder.Little)

    private fun legacyDecodeInteger(recData: ByteArray): Int{
        val r = recData.asList().subList(4, recData.size - 2).toByteArray()
        var res = ""
        for(i in r){
            res += i.toUByte().toString(16).padStart(2, '0')
        }
        return res.toInt()
    }

    private fun decodeFloat(recData: ByteArray, shift: Int = 8): Double{
        val payload = recData.asList().subList(6, recData.size - shift).toByteArray()
        return (payload.toDouble() * 10000000).roundToLong().toDouble() / 10000000.0
    }

    private fun decodeDeviceType(recData: ByteArray): String{
        val payload = recData.asList().subList(6, recData.size - 8).toByteArray()
        devType = payload.fromBytes(ByteOrder.Little)
        countOfChannels = (SPEC_PROP[devType]?: DEFAULT_SPEC_PROP)[2] as Int
        return DEV_TYPES[devType]?:"Неизвестный прибор"
    }

    private fun legacyDecodeDeviceType(recData: ByteArray): String{
        val payload = recData.asList().subList(6, recData.size - 2).toByteArray()
        devType = payload.fromBytes(ByteOrder.Little)
        countOfChannels = (SPEC_PROP[devType]?: DEFAULT_SPEC_PROP)[2] as Int
        return DEV_TYPES[devType]?:"Неизвестный прибор"
    }

    private fun decodeChannel(recData: ByteArray, mask: Int): Map<Int, Double>{
        val byteCount = (SPEC_PROP[devType] ?: DEFAULT_SPEC_PROP)[0] as Int
        val cType = (SPEC_PROP[devType] ?: DEFAULT_SPEC_PROP)[1] as Char
        val channels = (SPEC_PROP[devType] ?: DEFAULT_SPEC_PROP)[2] as Int
        val channelBytes = recData.asList().subList(6, recData.size - 4).toByteArray()
        val channelSlices = mutableListOf<ByteArray>()
        val channelNumber = mutableListOf<Int>()
        val values = mutableMapOf<Int, Double>()
        for(i in byteCount until channelBytes.size + byteCount step byteCount){
            channelSlices.add(channelBytes.asList().subList(i - byteCount, i).toByteArray())
        }
        for(i in 0 until channels){
            if(mask and (1 shl i) == 1 shl i){
                channelNumber.add(i)
            }
        }
        for(i in channelSlices.indices){
            if(cType == 'f') {
                values[channelNumber[i]] =
                    (channelSlices[i].toDouble() * 10000000).roundToLong().toDouble() / 10000000.0
            }else if(cType == 'd'){
                values[channelNumber[i]] =
                    (channelSlices[i].toDDouble() * 10000000).roundToLong().toDouble() / 10000000.0
            }
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

    private fun decodeArchive(recData: ByteArray, seconds: Long): Map<Date, Double?>{
        val payload = recData.asList().subList(10, 16)
        val calendar = Calendar.getInstance()
        calendar.set(payload[0].toInt() + 2000,
            payload[1].toInt() - 1,
            payload[2].toInt(),
            payload[3].toInt(),
            payload[4].toInt(),
            payload[5].toInt())
        var date = calendar.time
        val archive = recData.asList().subList(16, recData.size - 4)
        val archiveSlices = mutableListOf<ByteArray>()
        for(i in 4 until archive.size + 4 step 4){
            archiveSlices.add(archive.subList(i - 4, i).toByteArray())
        }
        val result = mutableMapOf<Date, Double?>()
        for(slice in archiveSlices){
            try {
                result[date] =
                    (slice.toDouble() * 10000000).roundToLong().toDouble() / 10000000.0
            }catch (_: Exception){
                result[date] = null
            }
            date = date.clone() as Date
            if(seconds == ArchiveTypes.MONTH.seconds) {
                val c = Calendar.getInstance()
                c.time = date
                c.add(Calendar.MONTH, 1)
                date.time = c.time.time
            }else {
                date.time += 1000 * seconds
            }
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

    private fun getRequestDates(startDate: Date, endDate: Date, seconds: Long): List<Pair<Date, Date>>{
        val delta = (endDate.time - startDate.time) / 1000
        val count = ceil((delta.toDouble() / seconds.toDouble()) / 19).toInt()
        val datesList = mutableListOf<Pair<Date, Date>>()
        if(count >= 2){
            var countSeconds = 0L
            var start = startDate.clone() as Date
            for(i in 0 until count){
                countSeconds += seconds * 19
                val end = Date()
                end.time = seconds * 19 * 1000L + start.time
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
        if(address != 0) return address
        val pAddress = splitAddressPulsar("0")
        val pReqNum = encodeReqNum()
        val reqLength = ((pAddress + READ_PARAM + DEV_ADDRESS + pReqNum).size + 3).toBytes(1, ByteOrder.Little)
        val request = (pAddress + READ_PARAM + reqLength + DEV_ADDRESS + pReqNum).injectCRC()
        for(time in times) {
            val result = tryAttempts(request, time)
            if (result.status == Status.Success && result.responseError == 0) {
                address = decodeInteger(result.result)
                return address
            }else{
                val legacyRequest = byteArrayOf(0xf0.toByte(), 0x0f, 0x0f, 0xf0.toByte(), 0, 0, 0, 0, 0).injectCRC()
                val legacyResult = tryAttempts(legacyRequest, time)
                if(legacyResult.status == Status.Success) {
                    address = legacyDecodeInteger(legacyResult.result)
                    isLegacy = true
                    return address
                }
            }
        }
        return null
    }

    suspend fun getDeviceType(_address: Int = address): String?{
        val pAddress = splitAddressPulsar(_address.toString())
        val pReqNum = encodeReqNum()
        val reqLength = ((pAddress + READ_PARAM + DEV_TYPE + pReqNum).size + 3).toBytes(1, ByteOrder.Little)
        val request = if(!isLegacy){
            (pAddress + READ_PARAM + reqLength + DEV_TYPE + pReqNum).injectCRC()
        }else{
            (pAddress + byteArrayOf(0x03, 0x02, 0x46, 0x00, 0x01)).injectCRC()
        }
        for(time in times) {
            val result = tryAttempts(request, time)
            if (result.status == Status.Success && result.responseError == 0) {
                return if(!isLegacy){
                    decodeDeviceType(result.result)
                }else{
                    legacyDecodeDeviceType(result.result)
                }
            }
        }
        return null
    }

    suspend fun getChannelsWeights(_address: Int = address): Map<Int, Double?>{
        val weights = mutableMapOf<Int, Double?>()
        for(channel in 0 until countOfChannels){
            weights[channel] = getChannelWeight(_address, channel)
        }
        return weights
    }

    suspend fun getChannelWeight(_address: Int = address, channel: Int): Double?{
        val shift = if(devType == 424) 8 else 4
        val pCode = when(devType){
            424 -> WEIGHT_CHANNEL[channel]
            12  -> LEGACY_WEIGHT_CHANNEL10[channel]
            101 -> LEGACY_WEIGHT_CHANNEL16[channel]
            else -> byteArrayOf(0, 0)
        }
        val param = when(devType){
            12  -> byteArrayOf(0x07)
            101 -> byteArrayOf(0x07)
            else -> READ_PARAM
        }
        val pAddress = splitAddressPulsar(_address.toString())
        val pReqNum = encodeReqNum()
        val reqLength = ((pAddress + param + pCode + pReqNum).size + 3).toBytes(1, ByteOrder.Little)
        val request = (pAddress + param + reqLength + pCode + pReqNum).injectCRC()

        for(time in times) {
            if(channel >= countOfChannels) break
            val result = tryAttempts(request, time)
            if (result.status == Status.Success && result.responseError == 0) {
                return decodeFloat(result.result, shift)
            }
        }
        return null
    }

    suspend fun getChannelsValues(_address: Int = address, channel: Int = -1): Map<Int, Double>?{
        val pReqNum = encodeReqNum()
        val pAddress = splitAddressPulsar(_address.toString())
        val maskList = (if (channel == -1) (SPEC_PROP[devType]?: DEFAULT_SPEC_PROP)[3] as Array<Int> else arrayOf(1 shl channel))
        val resultList = mutableMapOf<Int, Double>()
        for(time in times) {
            for(mask in maskList) {
                val pMask = mask.toBytes(4, ByteOrder.Little)
                val reqLength =
                    ((pAddress + READ_CH + pMask + pReqNum).size + 3).toBytes(1, ByteOrder.Little)
                val request = (pAddress + READ_CH + reqLength + pMask + pReqNum).injectCRC()
                val result = tryAttempts(request, time)
                if (result.status == Status.Success && result.responseError == 0) {
                    try {
                        resultList.putAll(decodeChannel(result.result, mask))
                    }catch (_: Exception){}
                }
            }
            if(resultList.isNotEmpty()) break
        }
        if(devType == 424 && resultList.size <= 10) countOfChannels = 10
        if(devType == 424 && resultList.size >= 11) countOfChannels = 16
        return resultList.ifEmpty { null }
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

    suspend fun readArchive(_address: Int = address, channel: Int, startDate: Date, endDate: Date, type: ArchiveTypes): Map<Date, Double?>{
        val pAddress = splitAddressPulsar(_address.toString())
        val mask = (1 shl (channel-1)).toBytes(4, ByteOrder.Little)
        val pType = type.type.toBytes(2, ByteOrder.Little)
        val pDates = getRequestDates(startDate, endDate, type.seconds)
        val archive = mutableMapOf<Date, Double?>()
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
                }
            }
        }
        return archive
    }

    suspend fun writeChannelsValues(_address: Int = address, values: Map<Int, Double>): Boolean{
        for(i in values){
            writeChannelValue(_address, i.key + 1, i.value)
        }
        return true
    }

    suspend fun writeChannelValue(_address: Int = address, channel: Int, value: Double): Boolean{
        val pReqNum = encodeReqNum()
        val pAddress = splitAddressPulsar(_address.toString())
        val pMask = (1 shl (channel - 1)).toBytes(4, ByteOrder.Little)
        val payload = if(devType == 424) value.toHex() else value.toDHex()
        val pReqLength = ((pAddress + WRITE_CH + pMask + payload + pReqNum).size + 3).toBytes(1, ByteOrder.Little)
        val request = (pAddress + WRITE_CH + pReqLength + pMask + payload + pReqNum).injectCRC()
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
        val pCode = when(devType){
            424 -> WEIGHT_CHANNEL[channel]
            12  -> LEGACY_WEIGHT_CHANNEL10[channel]
            101 -> LEGACY_WEIGHT_CHANNEL16[channel]
            else -> byteArrayOf(0, 0)
        }
        val param = when(devType){
            12  -> byteArrayOf(0x08)
            101 -> byteArrayOf(0x08)
            else -> WRITE_PARAM
        }
        val fill = when(devType){
            424 -> byteArrayOf(0, 0, 0, 0)
            else -> byteArrayOf()
        }
        val pReqNum = encodeReqNum()
        val pAddress = splitAddressPulsar(_address.toString())
        val payload = weight.toHex() + fill
        val pReqLength = ((pAddress + param + pCode + payload + pReqNum).size + 3).toBytes(1, ByteOrder.Little)
        val request = (pAddress + param + pReqLength + pCode + payload + pReqNum).injectCRC()
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

        const val HOUR  = 60L * 60L
        const val DAY   = 60L * 60L * 24L
        const val MONTH = 60L * 60L * 24L * 30L

        enum class ArchiveTypes(val type: Int, val seconds: Long){
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

        private val LEGACY_WEIGHT_CHANNEL16 = arrayOf(
            byteArrayOf(0x01, 0x00, 0x00, 0x00),
            byteArrayOf(0x02, 0x00, 0x00, 0x00),
            byteArrayOf(0x04, 0x00, 0x00, 0x00),
            byteArrayOf(0x08, 0x00, 0x00, 0x00),
            byteArrayOf(0x10, 0x00, 0x00, 0x00),
            byteArrayOf(0x20, 0x00, 0x00, 0x00),
            byteArrayOf(0x40, 0x00, 0x00, 0x00),
            byteArrayOf(0x80.toByte(), 0x00, 0x00, 0x00),
            byteArrayOf(0x00, 0x01, 0x00, 0x00),
            byteArrayOf(0x00, 0x02, 0x00, 0x00),
            byteArrayOf(0x00, 0x04, 0x00, 0x00),
            byteArrayOf(0x00, 0x08, 0x00, 0x00),
            byteArrayOf(0x00, 0x10, 0x00, 0x00),
            byteArrayOf(0x00, 0x20, 0x00, 0x00),
            byteArrayOf(0x00, 0x40, 0x00, 0x00),
            byteArrayOf(0x00, 0x80.toByte(), 0x00, 0x00)
        )

        private val LEGACY_WEIGHT_CHANNEL10 = arrayOf(
            byteArrayOf(0x01, 0x00, 0x00, 0x00),
            byteArrayOf(0x02, 0x00, 0x00, 0x00),
            byteArrayOf(0x04, 0x00, 0x00, 0x00),
            byteArrayOf(0x08, 0x00, 0x00, 0x00),
            byteArrayOf(0x10, 0x00, 0x00, 0x00),
            byteArrayOf(0x20, 0x00, 0x00, 0x00),
            byteArrayOf(0x40, 0x00, 0x00, 0x00),
            byteArrayOf(0x80.toByte(), 0x00, 0x00, 0x00),
            byteArrayOf(0x00, 0x01, 0x00, 0x00),
            byteArrayOf(0x00, 0x02, 0x00, 0x00),
        )

        private val SPEC_PROP = mapOf(
            424 to arrayOf(4, 'f', 16, arrayOf(0xfc00, 0x03ff)),
            154 to arrayOf(8, 'd', 2, arrayOf(0x03)),
            153 to arrayOf(8, 'd', 4, arrayOf(0x0f)),
            12  to arrayOf(8, 'd', 10, arrayOf(0x03ff)),
            101 to arrayOf(8, 'd', 16, arrayOf(0xc0ff, 0x3f00))
        )

        private val DEFAULT_SPEC_PROP = arrayOf(8, 'd', 1, arrayOf(1))

        private val DEV_TYPES = mapOf(
            424 to "Пульсар счетчик импульсов 10/16K v1",
            154 to "Пульсар2М",
            153 to "Пульсар4М",
            172 to "Пульсар6М",
            12  to "Пульсар10-М",
            101 to "Пульсар16-М"
        )

        private val times = listOf(20L, 50L)
    }

}