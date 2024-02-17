package com.ibaevzz.pcr.presentation.activity

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.ibaevzz.pcr.databinding.ActivityTcpConnectBinding

class TCPConnectActivity: AppCompatActivity() {

    private lateinit var binding: ActivityTcpConnectBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTcpConnectBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }
}