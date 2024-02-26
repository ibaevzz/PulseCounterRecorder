package com.ibaevzz.pcr.presentation.activity

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.lifecycle.ViewModelProvider
import com.ibaevzz.pcr.databinding.ActivityChannelBinding
import com.ibaevzz.pcr.di.bluetooth.BluetoothComponent
import com.ibaevzz.pcr.di.wifi.WifiComponent
import com.ibaevzz.pcr.presentation.viewmodel.ChannelViewModel
import kotlinx.coroutines.CoroutineScope
import javax.inject.Inject

class ChannelActivity : AppCompatActivity() {

    companion object{
        const val CHANNEL_EXTRA = "CHANNEL"
    }

    private lateinit var binding: ActivityChannelBinding

    @Inject
    lateinit var viewModelFactory: ChannelViewModel.Factory
    private val viewModel by lazy {
        ViewModelProvider(this, viewModelFactory)[ChannelViewModel::class.java]
    }
    @Inject
    lateinit var appScope: CoroutineScope

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChannelBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val channel = intent.getIntExtra(CHANNEL_EXTRA, -1)
        val isNetwork = intent.getBooleanExtra(ConnectActivity.IS_NETWORK_EXTRA, false)
        if(channel == -1){
            finish()
        }

        if(isNetwork){
            WifiComponent.init(applicationContext).inject(this)
        }else{
            BluetoothComponent.init(applicationContext).inject(this)
        }
    }
}