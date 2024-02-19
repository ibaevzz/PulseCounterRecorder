package com.ibaevzz.pcr.data.repository

import java.io.InputStream
import java.io.OutputStream

abstract class PCRRepository{

    protected abstract var inputStream: InputStream?
    protected abstract var outputStream: OutputStream?
    protected abstract val isConnect: Boolean

    abstract suspend fun connect(address: String, port: String = "")
    abstract suspend fun closeConnection()

}