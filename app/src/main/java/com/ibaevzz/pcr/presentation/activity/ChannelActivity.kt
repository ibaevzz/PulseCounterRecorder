package com.ibaevzz.pcr.presentation.activity

import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import com.ibaevzz.pcr.R
import com.ibaevzz.pcr.databinding.ActivityChannelBinding
import com.ibaevzz.pcr.di.bluetooth.BluetoothComponent
import com.ibaevzz.pcr.di.wifi.WifiComponent
import com.ibaevzz.pcr.presentation.viewmodel.ChannelViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@Suppress("DEPRECATION")
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

        supportActionBar?.title = "Канал ${channel + 1}"

        if(isNetwork){
            WifiComponent.init(applicationContext).inject(this)
        }else{
            BluetoothComponent.init(applicationContext).inject(this)
        }

        binding.readValue.setOnClickListener{
            binding.frame.visibility = View.VISIBLE
            binding.progress.visibility = View.VISIBLE
            enableButtons(false)
            binding.value.setTextColor(resources.getColor(R.color.black))
            appScope.launch(Dispatchers.IO){
                viewModel.getValue(channel).collect{
                    if(it == null){
                        withContext(Dispatchers.Main){
                            Toast.makeText(this@ChannelActivity, "Не удалось получить значение", Toast.LENGTH_SHORT).show()
                        }
                    }else{
                        withContext(Dispatchers.Main){
                            binding.value.setText(it.toString())
                        }
                    }
                    withContext(Dispatchers.Main){
                        binding.frame.visibility = View.INVISIBLE
                        binding.progress.visibility = View.INVISIBLE
                        enableButtons(true)
                    }
                }
            }
        }

        binding.writeValue.setOnClickListener{
            binding.frame.visibility = View.VISIBLE
            binding.progress.visibility = View.VISIBLE
            enableButtons(false)
            binding.value.setTextColor(resources.getColor(R.color.black))
            val text = binding.value.text.toString()
            val value = try {
                text.toDouble()
            }catch (_: Exception){
                null
            }
            appScope.launch(Dispatchers.IO){
                if(value!=null) {
                    viewModel.writeValue(channel + 1, value).collect{
                        withContext(Dispatchers.Main){
                            binding.frame.visibility = View.INVISIBLE
                            binding.progress.visibility = View.INVISIBLE
                            enableButtons(true)
                        }
                    }
                }else{
                    withContext(Dispatchers.Main){
                        binding.frame.visibility = View.INVISIBLE
                        binding.progress.visibility = View.INVISIBLE
                        enableButtons(true)
                        Toast.makeText(this@ChannelActivity, "Введено некорректное значение", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        binding.checkValue.setOnClickListener{
            binding.frame.visibility = View.VISIBLE
            binding.progress.visibility = View.VISIBLE
            enableButtons(false)
            val text = binding.value.text.toString()
            appScope.launch(Dispatchers.IO) {
                viewModel.getValue(channel).collect {
                    if(it==null){
                        withContext(Dispatchers.Main){
                            Toast.makeText(this@ChannelActivity, "Не удалось получить значение", Toast.LENGTH_SHORT).show()
                        }
                    }else{
                        withContext(Dispatchers.Main) {
                            binding.value.setText(it.toString())
                            if (it.toString() == text) {
                                binding.value.setTextColor(Color.GREEN)
                            } else {
                                binding.value.setTextColor(Color.RED)
                            }
                        }
                    }
                    withContext(Dispatchers.Main){
                        binding.frame.visibility = View.INVISIBLE
                        binding.progress.visibility = View.INVISIBLE
                        enableButtons(true)
                    }
                }
            }
        }

        binding.readWeight.setOnClickListener{
            binding.frame.visibility = View.VISIBLE
            binding.progress.visibility = View.VISIBLE
            enableButtons(false)
            binding.weight.setTextColor(resources.getColor(R.color.black))
            appScope.launch(Dispatchers.IO){
                viewModel.getWeight(channel).collect{
                    if(it == null){
                        withContext(Dispatchers.Main){
                            Toast.makeText(this@ChannelActivity, "Не удалось получить вес", Toast.LENGTH_SHORT).show()
                        }
                    }else{
                        withContext(Dispatchers.Main) {
                            binding.weight.setText(it.toString())
                        }
                    }
                    withContext(Dispatchers.Main){
                        binding.frame.visibility = View.INVISIBLE
                        binding.progress.visibility = View.INVISIBLE
                        enableButtons(true)
                    }
                }
            }
        }

        binding.writeWeight.setOnClickListener{
            binding.frame.visibility = View.VISIBLE
            binding.progress.visibility = View.VISIBLE
            enableButtons(false)
            binding.weight.setTextColor(resources.getColor(R.color.black))
            val text = binding.weight.text.toString()
            val weight = try {
                text.toDouble()
            }catch (_: Exception){
                null
            }
            appScope.launch(Dispatchers.IO){
                if(weight!=null) {
                    viewModel.writeWeight(channel, weight).collect{
                        withContext(Dispatchers.Main){
                            binding.frame.visibility = View.INVISIBLE
                            binding.progress.visibility = View.INVISIBLE
                            enableButtons(true)
                        }
                    }
                }else{
                    withContext(Dispatchers.Main){
                        binding.frame.visibility = View.INVISIBLE
                        binding.progress.visibility = View.INVISIBLE
                        enableButtons(true)
                        Toast.makeText(this@ChannelActivity, "Введено некорректное значение", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        binding.checkWeight.setOnClickListener{
            binding.frame.visibility = View.VISIBLE
            binding.progress.visibility = View.VISIBLE
            enableButtons(false)
            val text = binding.weight.text.toString()
            appScope.launch(Dispatchers.IO) {
                viewModel.getWeight(channel).collect {
                    if(it==null){
                        withContext(Dispatchers.Main){
                            Toast.makeText(this@ChannelActivity, "Не удалось получить значение", Toast.LENGTH_SHORT).show()
                        }
                    }else{
                        withContext(Dispatchers.Main) {
                            binding.weight.setText(it.toString())
                            if (it.toString() == text) {
                                binding.weight.setTextColor(Color.GREEN)
                            } else {
                                binding.weight.setTextColor(Color.RED)
                            }
                        }
                    }
                    withContext(Dispatchers.Main){
                        binding.frame.visibility = View.INVISIBLE
                        binding.progress.visibility = View.INVISIBLE
                        enableButtons(true)
                    }
                }
            }
        }

        appScope.launch(Dispatchers.IO){
            viewModel.getAddress().collect{
                binding.address.text = it.toString()
                withContext(Dispatchers.Main){
                    binding.frame.visibility = View.INVISIBLE
                    binding.progress.visibility = View.INVISIBLE
                }
            }
        }
    }

    private fun enableButtons(enabled: Boolean){
        binding.readValue.isEnabled = enabled
        binding.writeValue.isEnabled = enabled
        binding.checkValue.isEnabled = enabled
        binding.readWeight.isEnabled = enabled
        binding.writeWeight.isEnabled = enabled
        binding.checkWeight.isEnabled = enabled
    }
}