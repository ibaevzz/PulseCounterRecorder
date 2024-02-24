package com.ibaevzz.pcr.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.ibaevzz.pcr.data.repository.PCRRepository
import kotlinx.coroutines.CoroutineScope
import javax.inject.Inject

class FindChannelViewModel(private val PCRRepository: PCRRepository,
                           private val appScope: CoroutineScope): ViewModel(){

    class Factory @Inject constructor(private val PCRRepository: PCRRepository,
                                      private val appScope: CoroutineScope)
        : ViewModelProvider.Factory{
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if(modelClass == FindChannelViewModel::class.java){
                return FindChannelViewModel(PCRRepository, appScope) as T
            }
            return super.create(modelClass)
        }
    }

}