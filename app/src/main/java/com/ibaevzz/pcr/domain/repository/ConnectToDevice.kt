package com.ibaevzz.pcr.domain.repository

interface ConnectToDevice{
    suspend fun connect(address: String)
}