package com.ibaevzz.pcr.presentation.activity

import android.bluetooth.BluetoothAdapter
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.IBinder
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.ibaevzz.pcr.data.exceptions.BluetoothTurnedOffException
import com.ibaevzz.pcr.data.exceptions.WifiTurnedOffException
import com.ibaevzz.pcr.data.exceptions.WrongWifi
import com.ibaevzz.pcr.databinding.ActivityConnectBinding
import com.ibaevzz.pcr.di.bluetooth.BluetoothComponent
import com.ibaevzz.pcr.di.wifi.WifiComponent
import com.ibaevzz.pcr.presentation.service.RssiService
import com.ibaevzz.pcr.presentation.viewmodel.ConnectViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import javax.inject.Inject

class ConnectActivity : AppCompatActivity() {

    companion object{
        const val ADDRESS_EXTRA = "ADDRESS"
        const val IS_NETWORK_EXTRA = "IS_NETWORK"
        const val IP_EXTRA = "IP"
        const val PORT_EXTRA = "PORT"
    }

    private var isConnect = false
    private lateinit var binding: ActivityConnectBinding
    private var isNetwork: Boolean? = null

    @Inject
    lateinit var viewModelFactory: ConnectViewModel.Factory
    private val viewModel by lazy {
        ViewModelProvider(this, viewModelFactory)[ConnectViewModel::class.java]
    }

    private val registerBluetoothEnabled = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){
        if(it.resultCode != RESULT_OK){
            requestBluetoothEnabled()
        }
    }

    private val serviceConnection = object: ServiceConnection{
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            lifecycleScope.launch(Dispatchers.Default){
                (service as RssiService.RssiBinder).rssi.collect{
                    viewModel.sendRssi(it)
                }
            }
        }
        override fun onServiceDisconnected(name: ComponentName?) {}
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityConnectBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val address = intent.getStringExtra(ADDRESS_EXTRA)
        val ip = intent.getStringExtra(IP_EXTRA)
        val port = intent.getStringExtra(PORT_EXTRA)
        isNetwork = intent.getBooleanExtra(IS_NETWORK_EXTRA, false)

        if(address != null) {
            BluetoothComponent.init(applicationContext).inject(this)
            startConnection(address)

            val rssiServiceIntent = Intent(this, RssiService::class.java)
            rssiServiceIntent.putExtra(ADDRESS_EXTRA, address)
            bindService(rssiServiceIntent, serviceConnection, BIND_AUTO_CREATE)

        }else if(isNetwork!! && ip != null && port != null){
            WifiComponent.init(applicationContext).inject(this)
            startConnection(ip, port)
        }
        else{
            finish()
        }
    }

    private fun startConnection(address: String, port: String = ""){
        if(!isNetwork!!) {
            lifecycleScope.launch {
                viewModel.rssi.collect {}//TODO
            }
        }
        lifecycleScope.launch(Dispatchers.Default) {
            viewModel.errorsSharedFlow.collect{
                when(it){
                    is IOException -> {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@ConnectActivity, "Не удалось подключиться", Toast.LENGTH_SHORT)
                                .show()
                        }
                    }
                    is BluetoothTurnedOffException -> {
                        withContext(Dispatchers.Main){
                            requestBluetoothEnabled()
                        }
                    }
                    is WifiTurnedOffException -> {
                        withContext(Dispatchers.Main){
                            Toast.makeText(this@ConnectActivity, "WIFI отключен", Toast.LENGTH_SHORT).show()
                            startActivity(Intent(this@ConnectActivity, WifiConnectActivity::class.java))
                        }
                    }
                    is WrongWifi -> {
                        withContext(Dispatchers.Main){
                            Toast.makeText(this@ConnectActivity, "Точка WIFI была изменена", Toast.LENGTH_SHORT).show()
                            startActivity(Intent(this@ConnectActivity, WifiConnectActivity::class.java))
                        }
                    }
                    else -> {
                        throw it
                    }
                }
            }
        }

        lifecycleScope.launch(Dispatchers.Default){
            viewModel.isConnect.collect{
                when(it){
                    true ->{
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@ConnectActivity, "Подключено", Toast.LENGTH_SHORT)
                                .show()
                            binding.connect.isEnabled = false
                            binding.startWork.isEnabled = true
                        }
                        isConnect = true
                    }
                    false ->{
                        withContext(Dispatchers.Main) {
                            binding.connect.isEnabled = true
                            binding.startWork.isEnabled = false
                        }
                        isConnect = false
                    }
                    null ->{
                        withContext(Dispatchers.Main) {
                            binding.connect.isEnabled = false
                            binding.startWork.isEnabled = false
                        }
                        isConnect = false
                    }
                }
            }
        }

        binding.connect.setOnClickListener {
            viewModel.connect(address, port)
        }

        binding.startWork.setOnClickListener{
            val menuIntent = Intent(this, MenuPCRActivity::class.java)
            startActivity(menuIntent)
        }
    }

    private fun requestBluetoothEnabled(){
        val bluetoothEnabledIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
        registerBluetoothEnabled.launch(bluetoothEnabledIntent)
    }

    override fun onDestroy() {
        super.onDestroy()
        if(!isNetwork!!) {
            unbindService(serviceConnection)
        }
    }
}