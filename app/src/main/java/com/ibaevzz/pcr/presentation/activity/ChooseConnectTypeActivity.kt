package com.ibaevzz.pcr.presentation.activity

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.ibaevzz.pcr.databinding.ActivityChooseConnectTypeBinding
import com.ibaevzz.pcr.presentation.SendReportActivity
import com.ibaevzz.pcr.presentation.service.SendErrorService

class ChooseConnectTypeActivity: AppCompatActivity() {

    private lateinit var binding: ActivityChooseConnectTypeBinding
    private lateinit var errorIntent: Intent

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChooseConnectTypeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        errorIntent = Intent(this, SendErrorService::class.java)
        startForegroundService(errorIntent)

        supportActionBar?.title = "Выбор типа подключения"

        binding.bluetooth.setOnClickListener{
            val bluetoothIntent = Intent(this, BluetoothSearchActivity::class.java)
            startActivity(bluetoothIntent)
        }
        binding.tcp.setOnClickListener{
            val wifiIntent = Intent(this, WifiConnectActivity::class.java)
            startActivity(wifiIntent)
        }
        binding.loadDb.setOnClickListener{
            val sendDbIntent = Intent(this, SendDatabaseActivity::class.java)
            startActivity(sendDbIntent)
        }
        binding.sendReport.setOnClickListener{
            val sendReportIntent = Intent(this, SendReportActivity::class.java)
            startActivity(sendReportIntent)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopService(errorIntent)
    }
}