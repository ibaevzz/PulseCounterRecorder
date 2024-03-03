package com.ibaevzz.pcr.presentation.viewmodel

import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.ibaevzz.pcr.USERNAME
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

class UsernameViewModel(private val sharedPreferences: SharedPreferences,
                        private val appScope: CoroutineScope): ViewModel() {

    @Suppress("UNCHECKED_CAST")
    class Factory @Inject constructor(private val sharedPreferences: SharedPreferences,
                                      private val appScope: CoroutineScope)
        : ViewModelProvider.Factory{
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if(modelClass == UsernameViewModel::class.java){
                return UsernameViewModel(sharedPreferences, appScope) as T
            }
            return super.create(modelClass)
        }
    }

    fun getUsername(): String?{
        return sharedPreferences.getString(USERNAME, null)
    }

    fun writeUsername(username: String){
        appScope.launch(Dispatchers.IO){
            sharedPreferences.edit().putString(USERNAME, username).apply()
        }
    }

}