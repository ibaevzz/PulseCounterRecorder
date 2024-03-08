package com.ibaevzz.pcr.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.ibaevzz.pcr.data.db.IntermediateDatabase
import com.ibaevzz.pcr.data.db.PulsarDatabase
import javax.inject.Inject

class SendDatabaseViewModel(
    private val pulsarDatabase: PulsarDatabase,
    private val intermediateDatabase: IntermediateDatabase
): ViewModel(){

    @Suppress("UNCHECKED_CAST")
    class Factory @Inject constructor(
        private val pulsarDatabase: PulsarDatabase,
        private val intermediateDatabase: IntermediateDatabase)
        : ViewModelProvider.Factory{
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if(modelClass == SendDatabaseViewModel::class.java){
                return SendDatabaseViewModel(pulsarDatabase, intermediateDatabase) as T
            }
            return super.create(modelClass)
        }
    }

}