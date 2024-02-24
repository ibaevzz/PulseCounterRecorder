package com.ibaevzz.pcr.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.ibaevzz.pcr.data.repository.PCRRepository
import javax.inject.Inject

class ArchiveViewModel(private val PCRRepository: PCRRepository): ViewModel(){

    class Factory @Inject constructor(private val PCRRepository: PCRRepository)
        : ViewModelProvider.Factory{
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if(modelClass == ArchiveViewModel::class.java){
                return ArchiveViewModel(PCRRepository) as T
            }
            return super.create(modelClass)
        }
    }

}