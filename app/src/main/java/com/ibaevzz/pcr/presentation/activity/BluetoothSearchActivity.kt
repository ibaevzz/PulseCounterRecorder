package com.ibaevzz.pcr.presentation.activity

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.ibaevzz.pcr.databinding.ActivityBluetoothConnectBinding
import com.ibaevzz.pcr.di.bluetooth.BluetoothComponent
import com.ibaevzz.pcr.presentation.adapter.DeviceListAdapter
import com.ibaevzz.pcr.presentation.viewmodel.BluetoothSearchViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

class BluetoothSearchActivity : AppCompatActivity() {

    companion object {
        private const val PERMISSION_REQUEST_CODE = 0
    }

    private lateinit var binding: ActivityBluetoothConnectBinding
    private lateinit var adapter: DeviceListAdapter

    @Inject
    lateinit var bluetoothManager: BluetoothManager

    @Inject
    lateinit var viewModelFactory: BluetoothSearchViewModel.Factory
    private val viewModel by lazy {
        ViewModelProvider(this, viewModelFactory)[BluetoothSearchViewModel::class.java]
    }

    private val registerBluetoothEnabled = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){
        if(it.resultCode == RESULT_OK){
            registerReceiver(bluetoothEnabledBroadcastReceiver, IntentFilter().also {filter ->
                filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
            })
            showDevices()
            viewModel.getDevices()
        }else{
            requestBluetoothEnabled()
        }
    }

    private val bluetoothEnabledBroadcastReceiver = object: BroadcastReceiver(){
        override fun onReceive(context: Context, intent: Intent) {
            if(intent.action == BluetoothAdapter.ACTION_STATE_CHANGED){
                if(intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR) == BluetoothAdapter.STATE_TURNING_OFF){
                    requestBluetoothEnabled()
                }
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBluetoothConnectBinding.inflate(layoutInflater)
        setContentView(binding.root)

        BluetoothComponent.init(applicationContext).inject(this)

        adapter = DeviceListAdapter(viewModel::callback)

        if(!checkPermissions()){
            requestPermissions()
        }else if(!bluetoothManager.adapter.isEnabled){
            requestBluetoothEnabled()
        }else{
            registerReceiver(bluetoothEnabledBroadcastReceiver, IntentFilter().also {
                it.addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
            })
            showDevices()
            viewModel.getDevices()
        }

        binding.root.setOnRefreshListener{
            adapter.submitList(emptyList())
            adapter.notifyDataSetChanged()
            viewModel.restart()
            binding.root.isRefreshing = false
        }

        lifecycleScope.launch(Dispatchers.Default) {
            viewModel.startConnectActivity.collect {address ->
                withContext(Dispatchers.Main){
                    if(address != null) {
                        val intent =
                            Intent(this@BluetoothSearchActivity, ConnectActivity::class.java)
                                .also { intent ->
                                    intent.putExtra(ConnectActivity.ADDRESS_EXTRA, address)
                                }
                        unregisterReceiver(bluetoothEnabledBroadcastReceiver)
                        viewModel.stopSearch()
                        startActivity(intent)
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        registerReceiver(bluetoothEnabledBroadcastReceiver, IntentFilter().also {filter ->
            filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
        })
        viewModel.getDevices()
        if(!bluetoothManager.adapter.isEnabled && checkPermissions()) requestBluetoothEnabled()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray){
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if(requestCode == PERMISSION_REQUEST_CODE){
            if(checkPermissions()){
                if(!bluetoothManager.adapter.isEnabled) {
                    requestBluetoothEnabled()
                }
            }else{
                requestPermissions()
            }
        }
    }

    private fun checkPermissions(): Boolean{
        return if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) ==
                    PackageManager.PERMISSION_GRANTED && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) ==
                    PackageManager.PERMISSION_GRANTED && checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) ==
                    PackageManager.PERMISSION_GRANTED && checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) ==
                    PackageManager.PERMISSION_GRANTED
        else{
            checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) ==
                    PackageManager.PERMISSION_GRANTED && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) ==
                    PackageManager.PERMISSION_GRANTED && checkSelfPermission(Manifest.permission.BLUETOOTH) ==
                    PackageManager.PERMISSION_GRANTED && checkSelfPermission(Manifest.permission.BLUETOOTH_ADMIN) ==
                    PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestPermissions(){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            requestPermissions(arrayOf(Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION),
                PERMISSION_REQUEST_CODE)
        else{
            requestPermissions(arrayOf(Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION),
                PERMISSION_REQUEST_CODE)
        }
    }

    private fun requestBluetoothEnabled(){
        val bluetoothEnabledIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
        registerBluetoothEnabled.launch(bluetoothEnabledIntent)
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun showDevices(){
        binding.devices.layoutManager = LinearLayoutManager(this)
        binding.devices.adapter = adapter
        lifecycleScope.launch(Dispatchers.Default){
            viewModel.getDevices().collect{
                withContext(Dispatchers.Main){
                    adapter.submitList(it)
                    adapter.notifyDataSetChanged()
                }
            }
        }
    }
}