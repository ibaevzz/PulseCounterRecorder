package com.ibaevzz.pcr.presentation.activity

import android.bluetooth.BluetoothAdapter
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.ibaevzz.pcr.App
import com.ibaevzz.pcr.data.exceptions.BluetoothTurnedOffException
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
        const val IS_NETWORK_EXTRA = "IS_NETWORK"
    }

    private var isConnect = false
    private lateinit var binding: ActivityConnectBinding

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityConnectBinding.inflate(layoutInflater)
        setContentView(binding.root)

        (applicationContext as App).appComponent.inject(this)

        val address = intent.getStringExtra(ADDRESS_EXTRA)
        val isNetwork = intent.getBooleanExtra(IS_NETWORK_EXTRA, false)

        if(address != null) {
            binding.connect.setOnClickListener {
                lifecycleScope.launch(Dispatchers.IO) {
                    try {
                        viewModel.connect(address)
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@ConnectActivity, "Подключено", Toast.LENGTH_SHORT)
                                .show()
                            connect()
                        }
                    }catch (_: BluetoothTurnedOffException){
                        withContext(Dispatchers.Main){
                            requestBluetoothEnabled()
                            disconnect()
                        }
                    } catch (_: IOException) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@ConnectActivity, "Не удалось подключиться", Toast.LENGTH_SHORT)
                                .show()
                            disconnect()
                        }
                    }
                }
            }
            binding.startWork.setOnClickListener{
                val menuIntent = Intent(this, MenuPCRActivity::class.java)
                startActivity(menuIntent)
            }
        }else if(isNetwork){
            //TODO
        }
        else{
            finish()
        }
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
        if(isConnect) {
            try{
                viewModel.closeConnection()
            }catch (_: Exception) {}
        }
    }
}