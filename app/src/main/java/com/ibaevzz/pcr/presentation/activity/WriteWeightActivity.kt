package com.ibaevzz.pcr.presentation.activity

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.ibaevzz.pcr.databinding.ActivityWriteWeightBinding
import com.ibaevzz.pcr.di.bluetooth.BluetoothComponent
import com.ibaevzz.pcr.di.wifi.WifiComponent
import com.ibaevzz.pcr.presentation.adapter.WeightsChannelsAdapter
import com.ibaevzz.pcr.presentation.viewmodel.WriteWeightViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

class WriteWeightActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWriteWeightBinding

    @Inject
    lateinit var viewModelFactory: WriteWeightViewModel.Factory
    private val viewModel by lazy {
        ViewModelProvider(this, viewModelFactory)[WriteWeightViewModel::class.java]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWriteWeightBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val isNetwork = intent.getBooleanExtra(ConnectActivity.IS_NETWORK_EXTRA, false)

        if(isNetwork){
            WifiComponent.init(applicationContext).inject(this)
        }else{
            BluetoothComponent.init(applicationContext).inject(this)
        }

        binding.channels.layoutManager = GridLayoutManager(this, 2)
        //TODO binding.channels.adapter

        binding.all.setOnCheckedChangeListener{_, isChecked ->
            if(isChecked){
                (binding.channels.adapter as WeightsChannelsAdapter).checkAll(true)
            }else{
                (binding.channels.adapter as WeightsChannelsAdapter).checkAll(false)
            }
        }

        lifecycleScope.launch(Dispatchers.IO){
            val address = viewModel.getAddress()
            withContext(Dispatchers.Main){
                binding.address.text = address.toString()
            }
        }
    }
}