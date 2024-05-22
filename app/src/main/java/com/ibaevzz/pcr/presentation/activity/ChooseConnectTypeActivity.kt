package com.ibaevzz.pcr.presentation.activity

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import com.ibaevzz.pcr.databinding.ActivityChooseConnectTypeBinding

class ChooseConnectTypeActivity: AppCompatActivity() {

    private lateinit var binding: ActivityChooseConnectTypeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChooseConnectTypeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.title = "Выбор типа подключения"

        binding.bluetooth.setOnClickListener {
            val bluetoothIntent = Intent(this, BluetoothSearchActivity::class.java)
            startActivity(bluetoothIntent)
        }
        binding.tcp.setOnClickListener {
            val wifiIntent = Intent(this, WifiConnectActivity::class.java)
            startActivity(wifiIntent)
        }

        binding.loadDb.setOnClickListener {
            val sendDbIntent = Intent(this, SendDatabaseActivity::class.java)
            startActivity(sendDbIntent)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

}