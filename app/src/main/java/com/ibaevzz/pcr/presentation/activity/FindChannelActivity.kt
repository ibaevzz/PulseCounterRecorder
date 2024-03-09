package com.ibaevzz.pcr.presentation.activity

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.ibaevzz.pcr.data.exceptions.BluetoothTurnedOffException
import com.ibaevzz.pcr.data.exceptions.WifiTurnedOffException
import com.ibaevzz.pcr.data.exceptions.WrongWifi
import com.ibaevzz.pcr.databinding.ActivityFindChannelBinding
import com.ibaevzz.pcr.di.bluetooth.BluetoothComponent
import com.ibaevzz.pcr.di.wifi.WifiComponent
import com.ibaevzz.pcr.presentation.adapter.ValuesChannelsAdapter
import com.ibaevzz.pcr.presentation.viewmodel.FindChannelViewModel
import kotlinx.coroutines.*
import java.io.IOException
import javax.inject.Inject

class FindChannelActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFindChannelBinding

    @Inject
    lateinit var viewModelFactory: FindChannelViewModel.Factory
    private val viewModel by lazy {
        ViewModelProvider(this, viewModelFactory)[FindChannelViewModel::class.java]
    }
    @Inject
    lateinit var appScope: CoroutineScope

    private var isStop = false
    private var oldValues = mutableMapOf<Int, Double>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFindChannelBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.title = "Поиск канала"

        val isNetwork = intent.getBooleanExtra(ConnectActivity.IS_NETWORK_EXTRA, false)

        if(isNetwork){
            WifiComponent.init(applicationContext).inject(this)
        }else{
            BluetoothComponent.init(applicationContext).inject(this)
        }

        binding.progress.visibility = View.VISIBLE
        binding.frame.visibility = View.VISIBLE

        binding.channels.layoutManager = object: GridLayoutManager(this, 2){
            override fun canScrollVertically(): Boolean {
                return false
            }
        }

        binding.start.setOnClickListener{view ->
            if(binding.start.text == "Стоп"){
                isStop = true
                binding.start.text = "Старт"
                return@setOnClickListener
            }else{
                isStop = false
                var isFind = false
                (view as Button).text = "Стоп"
                binding.channels.adapter = ValuesChannelsAdapter(oldValues, null, ::toChannel)
                appScope.launch(Dispatchers.IO) {
                    val find = mutableListOf<Int>()
                    while  (!isFind && !isStop) {
                        delay(1000)
                        viewModel.getValues().collect{
                            if(it != null) {
                                for (i in it) {
                                    if((oldValues[i.key] ?: 1000000000.0) < i.value){
                                        find.add(i.key)
                                        isFind = true
                                        isStop = true
                                        withContext(Dispatchers.Main){
                                            binding.start.text = "Старт"
                                        }
                                    }
                                }
                                oldValues = it.toMutableMap()
                            }
                            withContext(Dispatchers.Main){
                                binding.channels.adapter = ValuesChannelsAdapter(oldValues, find, ::toChannel)
                            }
                        }
                    }
                }
            }
        }

//        binding.write.setOnClickListener{
//            binding.progress.visibility = View.VISIBLE
//            binding.frame.visibility = View.VISIBLE
//            appScope.launch {
//                viewModel.writeValues((binding.channels.adapter as ValuesChannelsAdapter).getValuesMap()).collect{
//                    withContext(Dispatchers.Main){
//                        binding.progress.visibility = View.INVISIBLE
//                        binding.frame.visibility = View.INVISIBLE
//                    }
//                }
//            }
//        }
//
//        binding.check.setOnClickListener{
//            binding.progress.visibility = View.VISIBLE
//            binding.frame.visibility = View.VISIBLE
//            appScope.launch(Dispatchers.IO){
//                viewModel.getValues().collect{
//                    binding.progress.visibility = View.INVISIBLE
//                    binding.frame.visibility = View.INVISIBLE
//
//                    oldValues = it?.toMutableMap() ?: mutableMapOf()
//                    val adapter = binding.channels.adapter as ValuesChannelsAdapter
//                    val newAdapter = ValuesChannelsAdapter(it, adapter.getChecked(), adapter.getAllValues(), toChannel = ::toChannel)
//                    withContext(Dispatchers.Main) {
//                        binding.channels.adapter = newAdapter
//                    }
//                }
//            }
//        }

        appScope.launch(Dispatchers.IO){
            viewModel.getAddress().collect{
                withContext(Dispatchers.Main) {
                    binding.address.text = (it ?: "Error").toString()
                }
            }
        }

        appScope.launch(Dispatchers.IO){
            viewModel.getValues().collect{
                withContext(Dispatchers.Main){
                    binding.progress.visibility = View.INVISIBLE
                    binding.frame.visibility = View.INVISIBLE

                    oldValues = it?.toMutableMap() ?: mutableMapOf()
                    binding.channels.adapter = ValuesChannelsAdapter(it, toChannel = ::toChannel)
                }
            }
        }

        lifecycleScope.launch(Dispatchers.IO){
            viewModel.errorsSharedFlow.collect{
                when(it){
                    is IOException -> {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@FindChannelActivity, "Ошибка чтения или записи", Toast.LENGTH_SHORT)
                                .show()
                        }
                    }
                    is BluetoothTurnedOffException -> {
                        withContext(Dispatchers.Main){
                            Toast.makeText(this@FindChannelActivity, it.message, Toast.LENGTH_SHORT)
                                .show()
                            val intent = Intent(this@FindChannelActivity, ConnectActivity::class.java)
                            startActivity(intent)
                            finish()
                        }
                    }
                    is WifiTurnedOffException -> {
                        withContext(Dispatchers.Main){
                            Toast.makeText(this@FindChannelActivity, it.message, Toast.LENGTH_SHORT).show()
                            startActivity(Intent(this@FindChannelActivity, WifiConnectActivity::class.java))
                            finish()
                        }
                    }
                    is WrongWifi -> {
                        withContext(Dispatchers.Main){
                            Toast.makeText(this@FindChannelActivity, it.message, Toast.LENGTH_SHORT).show()
                            startActivity(Intent(this@FindChannelActivity, WifiConnectActivity::class.java))
                            finish()
                        }
                    }
                    else -> {
                        throw it
                    }
                }
            }
        }
    }

    private fun toChannel(channel: Int){
        val intent = Intent(this, ChannelActivity::class.java)
        intent.putExtra(ChannelActivity.CHANNEL_EXTRA, channel)
        val isNetwork = intent.getBooleanExtra(ConnectActivity.IS_NETWORK_EXTRA, false)
        intent.putExtra(ConnectActivity.IS_NETWORK_EXTRA, isNetwork)
        startActivity(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        isStop = true
    }
}