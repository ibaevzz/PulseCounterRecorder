package com.ibaevzz.pcr

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