package com.ibaevzz.pcr.presentation.activity

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.RadioButton
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.github.kotlintelegrambot.Bot
import com.ibaevzz.pcr.ERROR_SET
import com.ibaevzz.pcr.ERROR_SHARED_PREF
import com.ibaevzz.pcr.data.exceptions.BluetoothTurnedOffException
import com.ibaevzz.pcr.data.exceptions.WifiTurnedOffException
import com.ibaevzz.pcr.data.exceptions.WrongWifi
import com.ibaevzz.pcr.databinding.ActivityWriteWeightBinding
import com.ibaevzz.pcr.di.bluetooth.BluetoothComponent
import com.ibaevzz.pcr.di.wifi.WifiComponent
import com.ibaevzz.pcr.presentation.adapter.WeightsChannelsAdapter
import com.ibaevzz.pcr.sendErrorInClass
import com.ibaevzz.pcr.presentation.viewmodel.WriteWeightViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

class WriteWeightActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWriteWeightBinding

    private val date = Calendar.getInstance()

    @Inject
    lateinit var viewModelFactory: WriteWeightViewModel.Factory
    private val viewModel by lazy {
        ViewModelProvider(this, viewModelFactory)[WriteWeightViewModel::class.java]
    }
    @Inject
    lateinit var appScope: CoroutineScope
    @Inject
    lateinit var bot: Bot

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWriteWeightBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.title = "Запись цены/даты-времени"
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

        binding.group.setOnCheckedChangeListener{_, id ->
            val weight = findViewById<RadioButton>(id).text.toString().toDoubleOrNull()?:0.0
            binding.all.isChecked = true
            if(binding.channels.adapter != null) {
                val list = mutableMapOf<Int, Double?>()
                for(i in (binding.channels.adapter as WeightsChannelsAdapter).weights.keys){
                    list[i] = weight
                }
                setAdapter(list)
            }
        }

        binding.write.setOnClickListener{
            if(binding.channels.adapter != null){
                binding.progress.visibility = View.VISIBLE
                binding.frame.visibility = View.VISIBLE
                viewModel.writeChannels((binding.channels.adapter as WeightsChannelsAdapter).getWeightsMap())
                if(binding.syncWithPhone.isChecked){
                    viewModel.writeDate()
                }else{
                    viewModel.writeDate(date.time)
                }
            }
        }

        binding.chooseDate.setOnClickListener{
            val datePicker = DatePickerDialog(this)
            datePicker.setOnDateSetListener{_, y, m, d ->
                val timePicker = TimePickerDialog(this, { _, hourOfDay, minute ->
                    date.set(y, m, d, hourOfDay, minute, 0)
                    val dateFormat = SimpleDateFormat("dd.MM.yyyy  HH:mm", Locale.getDefault())
                    binding.date.text = dateFormat.format(date.time)
                }, 16, 20, true)
                timePicker.show()
            }
            datePicker.show()
        }

        binding.check.setOnClickListener{
            binding.progress.visibility = View.VISIBLE
            binding.frame.visibility = View.VISIBLE
            lifecycleScope.launch(Dispatchers.Default){
                viewModel.getWeights()
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
                                it) { isAll -> binding.all.isChecked = isAll } }
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
            val errorPref = getSharedPreferences(ERROR_SHARED_PREF, MODE_PRIVATE)
            val edit = errorPref.edit()
            viewModel.errorsSharedFlow.collect{
                val errors = errorPref.getStringSet(ERROR_SET, mutableSetOf())?.toMutableSet()?: mutableSetOf()
                errors.add(sendErrorInClass(this@WriteWeightActivity::class, it::class, it.stackTraceToString()))
                edit.putStringSet(ERROR_SET, errors).apply()
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
            viewModel.getWeights()
                .flowOn(Dispatchers.IO)
                .onEach {result ->
                    if(binding.channels.adapter == null) {
                        binding.channels.adapter = WeightsChannelsAdapter(result) {
                            binding.all.isChecked = it
                        }
                    }else{
                        binding.channels.adapter = WeightsChannelsAdapter(result, (binding.channels.adapter as WeightsChannelsAdapter).checkedChannels){
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