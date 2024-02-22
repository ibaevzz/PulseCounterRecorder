package com.ibaevzz.pcr.data.exceptions

class WriteValuesException(channels: List<Int>): PCRException(with(channels){
    var s = "Ошибка при записи на каналы: "
    for(i in channels){
        s += i.toString()+"__"
    }
    s
})