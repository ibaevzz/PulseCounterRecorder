package com.ibaevzz.pcr.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.ibaevzz.pcr.data.db.PulsarDatabase
import com.ibaevzz.pcr.data.db.entity.UserEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

class UsernameViewModel(private val appScope: CoroutineScope,
                        private val pulsarDatabase: PulsarDatabase): ViewModel() {

    @Suppress("UNCHECKED_CAST")
    class Factory @Inject constructor(private val appScope: CoroutineScope,
                                      private val pulsarDatabase: PulsarDatabase)
        : ViewModelProvider.Factory{
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if(modelClass == UsernameViewModel::class.java){
                return UsernameViewModel(appScope, pulsarDatabase) as T
            }
            return super.create(modelClass)
        }
    }

    suspend fun getUsername(): String?{
        return pulsarDatabase.getDao().getUsername()
    }

    fun writeUsername(username: String){
        appScope.launch(Dispatchers.IO){
            pulsarDatabase.getDao().insertUser(UserEntity(Date().time, username))
        }
    }

}