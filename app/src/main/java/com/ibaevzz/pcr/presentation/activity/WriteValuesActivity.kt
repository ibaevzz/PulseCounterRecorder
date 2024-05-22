package com.ibaevzz.pcr.presentation.activity

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.ibaevzz.pcr.data.exceptions.BluetoothTurnedOffException
import com.ibaevzz.pcr.data.exceptions.WifiTurnedOffException
import com.ibaevzz.pcr.data.exceptions.WrongWifi
import com.ibaevzz.pcr.databinding.ActivityWriteValuesBinding
import com.ibaevzz.pcr.di.bluetooth.BluetoothComponent
import com.ibaevzz.pcr.di.wifi.WifiComponent
import com.ibaevzz.pcr.presentation.adapter.WeightsChannelsAdapter
import com.ibaevzz.pcr.presentation.viewmodel.WriteValuesViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import javax.inject.Inject

class WriteValuesActivity: AppCompatActivity(){

    private lateinit var binding: ActivityWriteValuesBinding

    @Inject
    lateinit var viewModelFactory: WriteValuesViewModel.Factory
    private val viewModel by lazy {
        ViewModelProvider(this, viewModelFactory)[WriteValuesViewModel::class.java]
    }
    @Inject
    lateinit var appScope: CoroutineScope

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWriteValuesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.title = "Запись/чтение значений"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val isNetwork = intent.getBooleanExtra(ConnectActivity.IS_NETWORK_EXTRA, false)

        if(isNetwork){
            WifiComponent.init(applicationContext).inject(this)
        }else{
            BluetoothComponent.init(applicationContext).inject(this)
        }

        binding.progress.visibility = View.VISIBLE
        binding.frame.visibility = View.VISIBLE
        setAdapter()

        binding.channels.layoutManager = object: GridLayoutManager(this, 2){
            override fun canScrollVertically(): Boolean {
                return false
            }
        }

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

        binding.write.setOnClickListener{
            if(binding.channels.adapter != null){
                binding.progress.visibility = View.VISIBLE
                binding.frame.visibility = View.VISIBLE
                viewModel.writeValues((binding.channels.adapter as WeightsChannelsAdapter).getWeightsMap())
            }
        }

        binding.check.setOnClickListener{
            binding.progress.visibility = View.VISIBLE
            binding.frame.visibility = View.VISIBLE
            lifecycleScope.launch(Dispatchers.Default){
                viewModel.getValues()
                    .onEach {
                        if(binding.channels.adapter != null){
                            val list = (binding.channels.adapter as WeightsChannelsAdapter).getAllWeights()
                            val map = mutableMapOf<Int, Double?>()
                            for(i in list.keys){
                                try {
                                    map[i] = list[i]?.toDouble()
                                }catch(_: Exception){
                                    map[i] = null
                                }
                            }
                            binding.channels.adapter = WeightsChannelsAdapter(
                                map,
                                (binding.channels.adapter as WeightsChannelsAdapter).checkedChannels,
                                it!!) { isAll -> binding.all.isChecked = isAll } }
                        binding.progress.visibility = View.INVISIBLE
                        binding.frame.visibility = View.INVISIBLE
                    }.flowOn(Dispatchers.Main)
                    .launchIn(appScope)
            }
        }

        lifecycleScope.launch(Dispatchers.Default){
            viewModel.writeResult.collect{
                withContext(Dispatchers.Main){
                    binding.progress.visibility = View.INVISIBLE
                    binding.frame.visibility = View.INVISIBLE
                }
            }
        }
        appScope.launch(Dispatchers.IO){
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
                            Toast.makeText(this@WriteValuesActivity, "Ошибка чтения или записи", Toast.LENGTH_SHORT)
                                .show()
                        }
                    }
                    is BluetoothTurnedOffException -> {
                        withContext(Dispatchers.Main){
                            Toast.makeText(this@WriteValuesActivity, it.message, Toast.LENGTH_SHORT)
                                .show()
                            val intent = Intent(this@WriteValuesActivity, ConnectActivity::class.java)
                            startActivity(intent)
                            finish()
                        }
                    }
                    is WifiTurnedOffException -> {
                        withContext(Dispatchers.Main){
                            Toast.makeText(this@WriteValuesActivity, it.message, Toast.LENGTH_SHORT).show()
                            startActivity(Intent(this@WriteValuesActivity, WifiConnectActivity::class.java))
                            finish()
                        }
                    }
                    is WrongWifi -> {
                        withContext(Dispatchers.Main){
                            Toast.makeText(this@WriteValuesActivity, it.message, Toast.LENGTH_SHORT).show()
                            startActivity(Intent(this@WriteValuesActivity, WifiConnectActivity::class.java))
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
    private fun setAdapter(list: Map<Int, Double?>? = null){
        if(list != null){
            binding.channels.adapter = WeightsChannelsAdapter(list, (binding.channels.adapter as WeightsChannelsAdapter).checkedChannels){
                binding.all.isChecked = it
            }
            return
        }
        binding.progress.visibility = View.VISIBLE
        binding.frame.visibility = View.VISIBLE
        lifecycleScope.launch(Dispatchers.IO){
            viewModel.getValues()
                .flowOn(Dispatchers.IO)
                .onEach {result ->
                    if(binding.channels.adapter == null) {
                        binding.channels.adapter = WeightsChannelsAdapter(result!!) {
                            binding.all.isChecked = it
                        }
                    }else{
                        binding.channels.adapter = WeightsChannelsAdapter(result!!, (binding.channels.adapter as WeightsChannelsAdapter).checkedChannels){
                            binding.all.isChecked = it
                        }
                    }
                    binding.progress.visibility = View.INVISIBLE
                    binding.frame.visibility = View.INVISIBLE
                }
                .flowOn(Dispatchers.Main)
                .launchIn(appScope)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if(item.itemId == android.R.id.home){
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}