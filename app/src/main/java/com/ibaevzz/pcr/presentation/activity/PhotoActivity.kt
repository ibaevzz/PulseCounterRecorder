package com.ibaevzz.pcr.presentation.activity

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.ibaevzz.pcr.databinding.ActivityPhotoBinding
import com.ibaevzz.pcr.di.app.AppComponent
import com.ibaevzz.pcr.presentation.viewmodel.PhotoViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

class PhotoActivity : AppCompatActivity() {

    companion object{
        const val DEVICE_INFO_ID_EXTRA = "DEVICE_INFO_ID"
    }

    private lateinit var binding: ActivityPhotoBinding

    @Inject
    lateinit var viewModelFactory: PhotoViewModel.Factory
    private val viewModel by lazy {
        ViewModelProvider(this, viewModelFactory)[PhotoViewModel::class.java]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPhotoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        AppComponent.init(applicationContext).inject(this)

        val devInfoId = intent.getLongExtra(DEVICE_INFO_ID_EXTRA, -1)
        var imageId: Long?
        lifecycleScope.launch(Dispatchers.IO){
            imageId = viewModel.getIdForImage(devInfoId)
        }
    }
}