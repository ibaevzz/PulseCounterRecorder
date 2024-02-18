package com.ibaevzz.pcr.presentation.activity

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.ibaevzz.pcr.databinding.ActivityChooseConnectTypeBinding

class ChooseConnectTypeActivity: AppCompatActivity() {

    private lateinit var binding: ActivityChooseConnectTypeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChooseConnectTypeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.bluetooth.setOnClickListener{
            val bluetoothIntent = Intent(this, BluetoothSearchActivity::class.java)
            startActivity(bluetoothIntent)
        }
        binding.tcp.setOnClickListener{
            val wifiIntent = Intent(this, WifiConnectActivity::class.java)
            startActivity(wifiIntent)
        }
    }
}