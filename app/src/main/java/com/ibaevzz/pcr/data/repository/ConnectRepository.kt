package com.ibaevzz.pcr.data.repository

interface ConnectRepository{
    suspend fun connect(address: String, port: String = "")
    suspend fun closeConnection()
}