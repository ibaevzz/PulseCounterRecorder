package com.ibaevzz.pcr.presentation.activity

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import com.ibaevzz.pcr.databinding.ActivityFindChannelBinding
import com.ibaevzz.pcr.di.bluetooth.BluetoothComponent
import com.ibaevzz.pcr.di.wifi.WifiComponent
import com.ibaevzz.pcr.presentation.adapter.ValuesChannelsAdapter
import com.ibaevzz.pcr.presentation.viewmodel.FindChannelViewModel
import kotlinx.coroutines.*
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

        binding.start.setOnClickListener{
            if(binding.start.text == "Стоп"){
                isStop = true
                binding.start.text = "Старт"
                return@setOnClickListener
            }else{
                isStop = false
                if(binding.count.text.toString().isEmpty()) return@setOnClickListener
                val count = binding.count.text.toString().toInt()
                var weight = -1.0
                var value = -1.0
                var isFind = false
                if (count > 0 && binding.channels.adapter != null) {
                    (it as Button).text = "Стоп"
                    appScope.launch(Dispatchers.IO) {
                        while  (!isFind && !isStop) {
                            val adapter = binding.channels.adapter as ValuesChannelsAdapter
                            for (i in 0 until adapter.getSize()) {
                                if(!isStop) {
                                    withContext(Dispatchers.Main) {
                                        adapter.setWatch(i)
                                    }
                                    viewModel.getWeight(i).collect {
                                        weight = it ?: -1.0
                                    }
                                    viewModel.getValue(i).collect {
                                        value = it?.get(i) ?: -1.0
                                    }
                                    if(value != -1.0 && weight != -1.0) {
                                        withContext(Dispatchers.Main) {
                                            adapter.setValue(i, value)
                                            if (((oldValues[i]
                                                    ?: -1.0) <= value - count * weight) && (oldValues[i]
                                                    ?: -1.0) >= value - (count + 0.5) * weight
                                            ) {
                                                adapter.setFind(i)
                                                oldValues[i] = value
                                                isFind = true
                                                isStop = true
                                                binding.start.text = "Старт"
                                            }
                                        }
                                    }
                                }
                            }
                            withContext(Dispatchers.Main){
                                adapter.unwatch()
                            }
                        }
                    }
                }
            }
        }

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
                    binding.channels.adapter = ValuesChannelsAdapter(it, emptySet())
                }
            }
        }

    }

    override fun onDestroy() {
        super.onDestroy()
        isStop = true
    }
}