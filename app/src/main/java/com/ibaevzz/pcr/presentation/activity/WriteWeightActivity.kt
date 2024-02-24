package com.ibaevzz.pcr.presentation.activity

import android.annotation.SuppressLint
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.RadioButton
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.ibaevzz.pcr.data.exceptions.BluetoothTurnedOffException
import com.ibaevzz.pcr.data.exceptions.WifiTurnedOffException
import com.ibaevzz.pcr.data.exceptions.WrongWifi
import com.ibaevzz.pcr.databinding.ActivityWriteWeightBinding
import com.ibaevzz.pcr.di.bluetooth.BluetoothComponent
import com.ibaevzz.pcr.di.wifi.WifiComponent
import com.ibaevzz.pcr.presentation.adapter.WeightsChannelsAdapter
import com.ibaevzz.pcr.presentation.viewmodel.WriteWeightViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
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

        setAdapter()

        binding.channels.layoutManager = GridLayoutManager(this, 2)

        binding.read.setOnClickListener{
            setAdapter()
        }

        binding.all.setOnCheckedChangeListener{_, isChecked ->
            if(isChecked && binding.channels.adapter != null){
                (binding.channels.adapter as WeightsChannelsAdapter).checkAll(true)
            }else if(binding.channels.adapter != null){
                (binding.channels.adapter as WeightsChannelsAdapter).checkAll(false)
            }
        }

        binding.group.setOnCheckedChangeListener{_, id ->
            val weight = findViewById<RadioButton>(id).text.toString().toDoubleOrNull()?:0.0
            if(binding.channels.adapter != null) {
                val list = mutableListOf<Double>()
                for(i in (binding.channels.adapter as WeightsChannelsAdapter).weights.indices){
                    list.add(weight)
                }
                setAdapter(list)
            }
        }

        binding.write.setOnClickListener{
            if(binding.channels.adapter != null){
                viewModel.writeChannels((binding.channels.adapter as WeightsChannelsAdapter).getWeights())
            }
        }

        lifecycleScope.launch(Dispatchers.Default){
            viewModel.writeResult.collect{
                //TODO
                for(i in it) {
                    Log.i("zzz", i.key.toString() + "     " + i.value.toString())
                }
            }
        }
        lifecycleScope.launch(Dispatchers.IO){
            val address = viewModel.getAddress()
            withContext(Dispatchers.Main){
                binding.address.text = address.toString()
            }
        }
        lifecycleScope.launch(Dispatchers.IO){
            viewModel.errorsSharedFlow.collect{
                when(it){
                    is IOException -> {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@WriteWeightActivity, "Ошибка чтения или записи", Toast.LENGTH_SHORT)
                                .show()
                        }
                    }
                    is BluetoothTurnedOffException -> {
                        withContext(Dispatchers.Main){
                            Toast.makeText(this@WriteWeightActivity, it.message, Toast.LENGTH_SHORT)
                                .show()
                            val intent = Intent(this@WriteWeightActivity, ConnectActivity::class.java)
                            startActivity(intent)
                            finish()
                        }
                    }
                    is WifiTurnedOffException -> {
                        withContext(Dispatchers.Main){
                            Toast.makeText(this@WriteWeightActivity, it.message, Toast.LENGTH_SHORT).show()
                            startActivity(Intent(this@WriteWeightActivity, WifiConnectActivity::class.java))
                            finish()
                        }
                    }
                    is WrongWifi -> {
                        withContext(Dispatchers.Main){
                            Toast.makeText(this@WriteWeightActivity, it.message, Toast.LENGTH_SHORT).show()
                            startActivity(Intent(this@WriteWeightActivity, WifiConnectActivity::class.java))
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

    @SuppressLint("NotifyDataSetChanged")
    private fun setAdapter(list: List<Double>? = null){
        if(list != null){
            binding.channels.adapter = WeightsChannelsAdapter(list, (binding.channels.adapter as WeightsChannelsAdapter).checkedChannels){
                binding.all.isChecked = it
            }
            return
        }
        lifecycleScope.launch(Dispatchers.IO){
            viewModel.getWeights()
                .flowOn(Dispatchers.IO)
                .collect{
                    withContext(Dispatchers.Main){
                        if(binding.channels.adapter == null) {
                            binding.channels.adapter = WeightsChannelsAdapter(it.values.toList()) {
                                binding.all.isChecked = it
                            }
                        }else{
                            binding.channels.adapter = WeightsChannelsAdapter(it.values.toList(), (binding.channels.adapter as WeightsChannelsAdapter).checkedChannels){
                                binding.all.isChecked = it
                            }
                        }
                    }
                }
        }
    }
}