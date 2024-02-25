package com.ibaevzz.pcr.presentation.activity

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.ibaevzz.pcr.R
import com.ibaevzz.pcr.data.exceptions.BluetoothTurnedOffException
import com.ibaevzz.pcr.data.exceptions.WifiTurnedOffException
import com.ibaevzz.pcr.data.exceptions.WrongWifi
import com.ibaevzz.pcr.data.repository.PCRRepository
import com.ibaevzz.pcr.databinding.ActivityArchiveBinding
import com.ibaevzz.pcr.di.bluetooth.BluetoothComponent
import com.ibaevzz.pcr.di.wifi.WifiComponent
import com.ibaevzz.pcr.presentation.adapter.ArchiveAdapter
import com.ibaevzz.pcr.presentation.viewmodel.ArchiveViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.*
import javax.inject.Inject

class ArchiveActivity : AppCompatActivity() {

    private lateinit var binding: ActivityArchiveBinding

    @Inject
    lateinit var viewModelFactory: ArchiveViewModel.Factory
    private val viewModel by lazy {
        ViewModelProvider(this, viewModelFactory)[ArchiveViewModel::class.java]
    }
    @Inject
    lateinit var appScope: CoroutineScope

    private val hourArchive = mutableListOf<Pair<Date, Double?>>()
    private val dayArchive = mutableListOf<Pair<Date, Double?>>()
    private val monthArchive = mutableListOf<Pair<Date, Double?>>()

    private var endDateHour = Date()
    private var startDateHour = endDateHour.clone().also {
        (it as Date).time = it.time - PCRRepository.HOUR * 20 * 1000
    } as Date

    private var endDateDay = Date()
    private var startDateDay = endDateDay.clone().also {
        (it as Date).time = it.time - PCRRepository.DAY * 20 * 1000
    } as Date

    private var endDateMonth = Date()
    private var startDateMonth = endDateMonth.clone().also {
        (it as Date).time = it.time - PCRRepository.MONTH.toLong() * 20L * 1000L
    } as Date

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityArchiveBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val isNetwork = intent.getBooleanExtra(ConnectActivity.IS_NETWORK_EXTRA, false)

        if(isNetwork){
            WifiComponent.init(applicationContext).inject(this)
        }else{
            BluetoothComponent.init(applicationContext).inject(this)
        }

        binding.archive.layoutManager = LinearLayoutManager(this)

        binding.read.setOnClickListener{view ->
            val channel = binding.channel.selectedItemId + 1
            val type = when(binding.group.checkedRadioButtonId){
                R.id.hour -> {PCRRepository.Companion.ArchiveTypes.HOUR}
                R.id.day -> {PCRRepository.Companion.ArchiveTypes.DAY}
                R.id.month -> {PCRRepository.Companion.ArchiveTypes.MONTH}
                else -> null
            }
            view.isEnabled = false
            if(type != null){
                when(type){
                    PCRRepository.Companion.ArchiveTypes.HOUR -> {
                        appScope.launch {
                            viewModel.getArchive(channel.toInt(), startDateHour, endDateHour, type)
                                .collect {
                                    withContext(Dispatchers.Main){
                                        startDateHour.time -= PCRRepository.HOUR * 1000 * 20
                                        endDateHour.time -= PCRRepository.HOUR * 1000 * 20
                                        val list = mutableListOf<Pair<Date, Double?>>()
                                        for(i in it) {
                                            list.add(Pair(i.key, i.value))
                                        }
                                        for(i in list){
                                            hourArchive.add(i)
                                        }
                                        hourArchive.sortBy { it.first.time }
                                        hourArchive.reverse()
                                        binding.archive.adapter = ArchiveAdapter(hourArchive)
                                        view.isEnabled = true
                                    }
                                }
                        }
                    }
                    PCRRepository.Companion.ArchiveTypes.DAY -> {
                        appScope.launch {
                            viewModel.getArchive(channel.toInt(), startDateDay, endDateDay, type)
                                .collect {
                                    withContext(Dispatchers.Main){
                                        startDateDay.time -= PCRRepository.DAY * 20 * 1000
                                        endDateDay.time -= PCRRepository.DAY * 1000 * 20
                                        val list = mutableListOf<Pair<Date, Double?>>()
                                        for(i in it) {
                                            list.add(Pair(i.key, i.value))
                                        }
                                        for(i in list){
                                            dayArchive.add(i)
                                        }
                                        dayArchive.sortBy { it.first.time }
                                        dayArchive.reverse()
                                        binding.archive.adapter = ArchiveAdapter(dayArchive)
                                        view.isEnabled = true
                                    }
                                }
                        }
                    }
                    PCRRepository.Companion.ArchiveTypes.MONTH -> {
                        appScope.launch {
                            viewModel.getArchive(channel.toInt(), startDateMonth, endDateMonth, type)
                                .collect {
                                    withContext(Dispatchers.Main){
                                        startDateMonth.time -= PCRRepository.MONTH.toLong() * 1000L * 20L
                                        endDateMonth.time -= PCRRepository.MONTH.toLong() * 1000L * 20L
                                        val list = mutableListOf<Pair<Date, Double?>>()
                                        for(i in it) {
                                            list.add(Pair(i.key, i.value))
                                        }
                                        for(i in list){
                                            monthArchive.add(i)
                                        }
                                        monthArchive.sortBy { it.first.time }
                                        monthArchive.reverse()
                                        binding.archive.adapter = ArchiveAdapter(monthArchive)
                                        view.isEnabled = true
                                    }
                                }
                        }
                    }
                }
            }else{
                view.isEnabled = true
            }
        }

        val channels = mutableListOf<String>()
        for(i in 1..viewModel.getCount()){
            channels.add("$i           ")
        }
        val arrayAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, channels)
        binding.channel.adapter = arrayAdapter

        binding.frame.visibility = View.VISIBLE
        binding.progress.visibility = View.VISIBLE

        appScope.launch(Dispatchers.IO){
            viewModel.getAddress().collect{
                withContext(Dispatchers.Main){
                    binding.frame.visibility = View.INVISIBLE
                    binding.progress.visibility = View.INVISIBLE
                    binding.address.text = it.toString()
                }
            }
        }

        lifecycleScope.launch(Dispatchers.IO){
            viewModel.errorsSharedFlow.collect{
                when(it){
                    is IOException -> {
                        withContext(Dispatchers.Main) {
                            binding.read.isEnabled = true
                            Toast.makeText(this@ArchiveActivity, "Ошибка чтения или записи", Toast.LENGTH_SHORT)
                                .show()
                        }
                    }
                    is BluetoothTurnedOffException -> {
                        withContext(Dispatchers.Main){
                            Toast.makeText(this@ArchiveActivity, it.message, Toast.LENGTH_SHORT)
                                .show()
                            val intent = Intent(this@ArchiveActivity, ConnectActivity::class.java)
                            startActivity(intent)
                            finish()
                        }
                    }
                    is WifiTurnedOffException -> {
                        withContext(Dispatchers.Main){
                            Toast.makeText(this@ArchiveActivity, it.message, Toast.LENGTH_SHORT).show()
                            startActivity(Intent(this@ArchiveActivity, WifiConnectActivity::class.java))
                            finish()
                        }
                    }
                    is WrongWifi -> {
                        withContext(Dispatchers.Main){
                            Toast.makeText(this@ArchiveActivity, it.message, Toast.LENGTH_SHORT).show()
                            startActivity(Intent(this@ArchiveActivity, WifiConnectActivity::class.java))
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
}