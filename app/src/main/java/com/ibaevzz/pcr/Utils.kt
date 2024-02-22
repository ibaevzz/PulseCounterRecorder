package com.ibaevzz.pcr

import com.ibaevzz.pcr.data.exceptions.ToBytesException

const val UUID = "00001101-0000-1000-8000-00805F9B34FB"
const val PMSK_PNR = "PMSK_PNR"
const val PORT = 4001

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