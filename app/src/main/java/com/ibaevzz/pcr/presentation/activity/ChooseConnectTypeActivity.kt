package com.ibaevzz.pcr.presentation.activity

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.ibaevzz.pcr.DATABASE
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

        binding.loadDb.setOnClickListener{
            //TODO завернуть в zip архив с изображениями
            val uri = Uri.parse("content://com.ibaevzz.pcr/$DATABASE.sqlite")
            val uriShm = Uri.parse("content://com.ibaevzz.pcr/$DATABASE.sqlite-shm")
            val uriWal = Uri.parse("content://com.ibaevzz.pcr/$DATABASE.sqlite-wal")
            val intent = Intent(Intent.ACTION_SEND_MULTIPLE)
            intent.type = "application/octet-stream"
            intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, arrayListOf(uri, uriShm, uriWal))
            intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            startActivity(intent)
        }
    }
}