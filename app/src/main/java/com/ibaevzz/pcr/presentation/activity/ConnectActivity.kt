package com.ibaevzz.pcr.presentation.activity

import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.ibaevzz.pcr.App
import com.ibaevzz.pcr.databinding.ActivityConnectBinding
import com.ibaevzz.pcr.presentation.viewmodel.ConnectViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import javax.inject.Inject

class ConnectActivity : AppCompatActivity() {

    companion object{
        const val ADDRESS_EXTRA = "ADDRESS"
    }

    private var isConnect = false
    private lateinit var binding: ActivityConnectBinding
    private var address: String? = null

    @Inject
    lateinit var viewModelFactory: ConnectViewModel.Factory
    private val viewModel by lazy {
        ViewModelProvider(this, viewModelFactory)[ConnectViewModel::class.java]
    }

    private val registerBluetoothEnabled = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){
        if(it.resultCode == RESULT_OK){
            registerReceiver(bluetoothEnabledBroadcastReceiver, IntentFilter().also { filter ->
                filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
            })
        }else{
            requestBluetoothEnabled()
        }
    }

    private val bluetoothEnabledBroadcastReceiver = object: BroadcastReceiver(){
        override fun onReceive(context: Context, intent: Intent) {
            if(intent.action == BluetoothAdapter.ACTION_STATE_CHANGED){
                if(intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR) == BluetoothAdapter.STATE_TURNING_OFF){
                    requestBluetoothEnabled()
                    disconnect()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityConnectBinding.inflate(layoutInflater)
        setContentView(binding.root)

        (applicationContext as App).appComponent.inject(this)

        if(address==null) address = intent.getStringExtra(ADDRESS_EXTRA)
        if(address != null) {
            registerReceiver(bluetoothEnabledBroadcastReceiver, IntentFilter().also {
                it.addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
            })

            binding.connect.setOnClickListener {
                lifecycleScope.launch(Dispatchers.IO) {
                    try {
                        viewModel.connect(address!!)
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@ConnectActivity, "Подключено", Toast.LENGTH_SHORT)
                                .show()
                            connect()
                        }
                    } catch (ex: IOException) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@ConnectActivity, "Не удалось подключиться", Toast.LENGTH_SHORT)
                                .show()
                            disconnect()
                        }
                    }
                }
            }

            binding.startWork.setOnClickListener{
                //TODO
            }
        }else{
            finish()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        //TODO
    }

    private fun connect(){
        isConnect = true
        binding.connect.isEnabled = false
        binding.startWork.isEnabled = true
    }

    private fun disconnect(){
        isConnect = false
        binding.connect.isEnabled = true
        binding.startWork.isEnabled = false
    }

    private fun requestBluetoothEnabled(){
        val bluetoothEnabledIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
        registerBluetoothEnabled.launch(bluetoothEnabledIntent)
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(bluetoothEnabledBroadcastReceiver)
    }
}