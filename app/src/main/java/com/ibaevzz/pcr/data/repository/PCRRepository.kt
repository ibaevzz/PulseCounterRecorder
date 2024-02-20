package com.ibaevzz.pcr.data.repository

import kotlinx.coroutines.CoroutineDispatcher
import java.io.InputStream
import java.io.OutputStream

abstract class PCRRepository(private val inputDispatcher: CoroutineDispatcher,
                             private val outputDispatcher: CoroutineDispatcher){

    protected abstract var inputStream: InputStream?
    protected abstract var outputStream: OutputStream?

    protected abstract fun checkConnection(): Boolean
    abstract suspend fun connect(address: String, port: String = "")
    abstract suspend fun closeConnection()

}