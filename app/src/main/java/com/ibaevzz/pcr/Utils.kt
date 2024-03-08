package com.ibaevzz.pcr

import android.graphics.Bitmap
import android.graphics.Matrix
import com.ibaevzz.pcr.data.exceptions.ToBytesException
import kotlin.math.pow

const val UUID = "00001101-0000-1000-8000-00805F9B34FB"
const val PMSK_PNR = "PMSK_PNR"
const val PORT = 4001
const val DATABASE = "database.sqlite"
const val INTERMEDIATE_DATABASE = "date_database.sqlite"
const val ZIP = "database.zip"

fun Bitmap.rotateBitmap(angle: Float): Bitmap{
    val matrix = Matrix()
    matrix.postRotate(angle)
    return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
}

fun Int.ipToString(): String{
    var result = ""
    for(i in 0 .. 24 step 8){
        result += (this ushr i).toUByte().toString()
        if(i != 24){
            result+="."
        }
    }
    return result
}

fun String.stringToIp(): Int{
    var i = 24
    var result = 0
    split(".").reversed().forEach {
        result += it.toUByte().toInt() shl i
        i-=8
    }
    return result
}

fun ByteArray.checkCRC(): Boolean{
    return toList().subList(0, size-2).toByteArray().ibmCRC16()
        .contentEquals(toList().subList(size-2, size).toByteArray())
}

private fun ByteArray.ibmCRC16(): ByteArray{
    var result: UShort = 0xFFFFu
    for(i in indices){
        val byte = get(i).toUByte().toUShort()
        result = result xor byte
        for(a in 0 until 8){
            result = if(result%2u!=0u){
                (result.toInt() shr 1).toUShort() xor 0xA001u
            }else{
                (result.toInt() shr 1).toUShort()
            }
        }
    }
    return result.toInt().toBytes(2, ByteOrder.Little)
}

fun ByteArray.injectCRC(): ByteArray{
    return this + ibmCRC16()
}

fun Int.toBytes(size: Int, byteOrder: ByteOrder): ByteArray{
    if(toUInt().toString(16).length > size * 2) throw ToBytesException()
    val hexString = toUInt().toString(16).padStart(size * 2, '0')
    var plus = true
    var ii: Int = when(byteOrder){
        ByteOrder.Big -> {
            plus = false
            size - 1
        }
        ByteOrder.Little -> {
            0
        }
    }
    val result = ByteArray(size)
    var i = hexString.length - 1
    while(i > 0){
        result[ii] = (hexString[i-1].toString() + hexString[i].toString()).toUByte(16).toByte()
        i-=2
        ii = if(plus) ii + 1 else ii - 1
    }
    return result
}

fun ByteArray.fromBytes(byteOrder: ByteOrder): Int{
    var s = ""
    val bytes: ByteArray = when(byteOrder){
        ByteOrder.Big -> {
            this
        }
        ByteOrder.Little -> {
            reversed().toByteArray()
        }
    }
    for(i in bytes){
        s += i.toUByte().toString(16).padStart(2, '0')
    }
    return s.toInt(16)
}

sealed interface ByteOrder{
    object Big: ByteOrder
    object Little: ByteOrder
}

fun ByteArray.toDouble(): Double{
    val s = get(3).toUByte().toString(2).padStart(8, '0') +
            get(2).toUByte().toString(2).padStart(8, '0') +
            get(1).toUByte().toString(2).padStart(8, '0') +
            get(0).toUByte().toString(2).padStart(8, '0')
    val z = s[0].digitToInt()
    val step = s.substring(1, 9).toInt(2) - 127
    var mant = "1${s.substring(9)}"
    mant = if(step>=0){
        mant.substring(0, step + 1)+'.'+mant.substring(step+1)
    }else{
        "0." + mant.padStart(23 + kotlin.math.abs(step), '0')
    }
    var ch = 0.0
    var t = 0
    var ii = 0
    for(i in mant){
        if(i=='.'){
            t = ii
            break
        }
        ii+=1
    }
    ii = t - 1
    var ss = 0
    while(ii >= 0){
        ch += mant[ii].digitToInt() * 2.0.pow(ss.toDouble())
        ss += 1
        ii -= 1
    }
    ii = t + 1
    ss = -1
    while(ii<mant.length){
        ch += mant[ii].digitToInt() * 2.0.pow(ss.toDouble())
        ss -= 1
        ii += 1
    }
    return if(z==1) -ch else ch
}

fun ByteArray.toDDouble(): Double{
    val s = get(7).toUByte().toString(2).padStart(8, '0')+
            get(6).toUByte().toString(2).padStart(8, '0')+
            get(5).toUByte().toString(2).padStart(8, '0')+
            get(4).toUByte().toString(2).padStart(8, '0')+
            get(3).toUByte().toString(2).padStart(8, '0') +
            get(2).toUByte().toString(2).padStart(8, '0') +
            get(1).toUByte().toString(2).padStart(8, '0') +
            get(0).toUByte().toString(2).padStart(8, '0')
    val z = s[0].digitToInt()
    val step = s.substring(1, 12).toInt(2) - 1023
    var mant = "1${s.substring(12)}"
    mant = if(step>=0){
        mant.substring(0, step + 1)+'.'+mant.substring(step+1)
    }else{
        "0." + mant.padStart(52 + kotlin.math.abs(step), '0')
    }
    var ch = 0.0
    var t = 0
    var ii = 0
    for(i in mant){
        if(i=='.'){
            t = ii
            break
        }
        ii+=1
    }
    ii = t - 1
    var ss = 0
    while(ii >= 0){
        ch += mant[ii].digitToInt() * 2.0.pow(ss.toDouble())
        ss += 1
        ii -= 1
    }
    ii = t + 1
    ss = -1
    while(ii<mant.length){
        ch += mant[ii].digitToInt() * 2.0.pow(ss.toDouble())
        ss -= 1
        ii += 1
    }
    return if(z==1) -ch else ch
}

fun Double.toHex(): ByteArray{
    val ch = kotlin.math.abs(this)
    var m = 0
    if(ch==0.0){
        return byteArrayOf(0, 0, 0, 0)
    }else if(ch.toInt() == 0){
        var mm = -1
        for(i in fractionalToBin(ch)){
            if(i == '1'){
                m = mm
                break
            }
            mm -= 1
        }
    }else{
        m = intToBin(ch).length - 1
    }
    val z = if(this<0) 1 else 0
    val step = (m + 127).toString(2).padStart(8, '0')
    val mant = if(m==0){
        fractionalToBin(ch).padEnd(23, '0')
    }else if(m>0){
        (intToBin(ch).substring(1) + fractionalToBin(ch)).padEnd(23, '0')
    }else{
        if(fractionalToBin(ch).length<=23)
            fractionalToBin(ch).substring(kotlin.math.abs(m)).padEnd(23, '0')
        else
            fractionalToBin(ch).substring(kotlin.math.abs(m), 23 + kotlin.math.abs(m))
    }
    var res = ""
    var i = 0
    val s = z.toString() + step + mant.substring(0, 23)
    while(i < s.length){
        res += s.substring(i..i+3).toInt(2).toString(16)
        i += 4
    }
    i = res.length-1
    var ii = 0
    val resB = ByteArray(4)
    while(i>0){
        resB[ii] = (res[i-1].toString() + res[i].toString()).toUByte(16).toByte()
        ii += 1
        i -= 2
    }
    return resB
}

fun Double.toDHex(): ByteArray{
    val ch = kotlin.math.abs(this)
    var m = 0
    if(ch==0.0){
        return byteArrayOf(0, 0, 0, 0, 0, 0, 0, 0)
    }else if(ch.toInt() == 0){
        var mm = -1
        for(i in fractionalToBin(ch)){
            if(i == '1'){
                m = mm
                break
            }
            mm -= 1
        }
    }else{
        m = intToBin(ch).length - 1
    }
    val z = if(this<0) 1 else 0
    val step = (m + 1023).toString(2).padStart(11, '0')
    val mant = if(m==0){
        fractionalToBin(ch).padEnd(52, '0')
    }else if(m>0){
        (intToBin(ch).substring(1) + fractionalToBin(ch)).padEnd(52, '0')
    }else{
        if(fractionalToBin(ch).length<=52)
            fractionalToBin(ch).substring(kotlin.math.abs(m)).padEnd(52, '0')
        else
            fractionalToBin(ch).substring(kotlin.math.abs(m), 52 + kotlin.math.abs(m))
    }
    var res = ""
    var i = 0
    val s = z.toString() + step + mant.substring(0, 52)
    while(i < s.length){
        res += s.substring(i..i+3).toInt(2).toString(16)
        i += 4
    }
    i = res.length-1
    var ii = 0
    val resB = ByteArray(8)
    while(i>0){
        resB[ii] = (res[i-1].toString() + res[i].toString()).toUByte(16).toByte()
        ii += 1
        i -= 2
    }
    return resB
}

private fun intToBin(a: Double): String{
    val ch: Int = a.toInt()
    return ch.toString(2)
}

private fun fractionalToBin(a: Double): String{
    var ch = a % 1
    var s = ""
    for(i in 0..60){
        ch *= 2
        if (ch>=1){
            s+="1"
            ch-=1
        }else{
            s+="0"
        }
        if(ch==0.0){
            break
        }
    }
    return s
}