package com.ibaevzz.pcr.presentation.activity

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.lifecycle.ViewModelProvider
import com.ibaevzz.pcr.databinding.ActivityFindChannelBinding
import com.ibaevzz.pcr.di.bluetooth.BluetoothComponent
import com.ibaevzz.pcr.di.wifi.WifiComponent
import com.ibaevzz.pcr.presentation.viewmodel.FindChannelViewModel
import javax.inject.Inject

class FindChannelActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFindChannelBinding

    @Inject
    lateinit var viewModelFactory: FindChannelViewModel.Factory
    private val viewModel by lazy {
        ViewModelProvider(this, viewModelFactory)[FindChannelViewModel::class.java]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFindChannelBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val isNetwork = intent.getBooleanExtra(ConnectActivity.IS_NETWORK_EXTRA, false)

        if(isNetwork){
            WifiComponent.init(applicationContext).inject(this)
        }else{
            BluetoothComponent.init(applicationContext).inject(this)
        }
    }
}