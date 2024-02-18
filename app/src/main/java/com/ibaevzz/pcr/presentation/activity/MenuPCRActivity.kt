package com.ibaevzz.pcr.presentation.activity

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.ibaevzz.pcr.databinding.ActivityMenuPcractivityBinding

class MenuPCRActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMenuPcractivityBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMenuPcractivityBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }
}