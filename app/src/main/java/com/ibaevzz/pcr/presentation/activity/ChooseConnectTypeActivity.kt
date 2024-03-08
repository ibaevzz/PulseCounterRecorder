package com.ibaevzz.pcr.presentation.activity

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.ibaevzz.pcr.DATABASE
import com.ibaevzz.pcr.ZIP
import com.ibaevzz.pcr.databinding.ActivityChooseConnectTypeBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.lingala.zip4j.ZipFile
import net.lingala.zip4j.model.ExcludeFileFilter
import net.lingala.zip4j.model.ZipParameters

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
            lifecycleScope.launch(Dispatchers.IO) {
                val zip = ZipFile(filesDir.path + "/$ZIP")
                val databasePath = getDatabasePath(DATABASE)?.parent ?: ""
                val fileFilter = ExcludeFileFilter {
                    !it.path.endsWith(".jpeg")
                }
                val zipParameters = ZipParameters().apply {
                    excludeFileFilter = fileFilter
                }
                zip.apply {
                    addFile("$databasePath/$DATABASE")
                    addFile("$databasePath/$DATABASE-shm")
                    addFile("$databasePath/$DATABASE-wal")
                    addFolder(filesDir, zipParameters)
                }
                val uri = Uri.parse("content://com.ibaevzz.pcr/$ZIP")
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/zip"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                }
                withContext(Dispatchers.Main) {
                    startActivity(intent)
                }
            }
        }
    }
}