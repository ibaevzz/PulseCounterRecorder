package com.ibaevzz.pcr.presentation.activity

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.ibaevzz.pcr.databinding.ActivityPhotoBinding

class PhotoActivity : AppCompatActivity() {

    companion object{
        const val DEVICE_INFO_ID_EXTRA = "DEVICE_INFO_ID"
    }

    private lateinit var binding: ActivityPhotoBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPhotoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val devId = intent.getLongExtra(DEVICE_INFO_ID_EXTRA, -1)
    }
}