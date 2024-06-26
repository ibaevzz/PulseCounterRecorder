package com.ibaevzz.pcr.presentation.activity

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.ibaevzz.pcr.data.exceptions.BluetoothTurnedOffException
import com.ibaevzz.pcr.data.exceptions.WifiTurnedOffException
import com.ibaevzz.pcr.data.exceptions.WrongWifi
import com.ibaevzz.pcr.databinding.ActivityPcrMenuBinding
import com.ibaevzz.pcr.di.bluetooth.BluetoothComponent
import com.ibaevzz.pcr.di.wifi.WifiComponent
import com.ibaevzz.pcr.presentation.viewmodel.MenuPCRViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import javax.inject.Inject

class MenuPCRActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPcrMenuBinding

    @Inject
    lateinit var appScope: CoroutineScope

    @Inject
    lateinit var viewModelFactory: MenuPCRViewModel.Factory
    private val viewModel by lazy {
        ViewModelProvider(this, viewModelFactory)[MenuPCRViewModel::class.java]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPcrMenuBinding.inflate(layoutInflater)
        setContentView(binding.root)

        buttonClicked(false)

        supportActionBar?.title = "Меню"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val isNetwork = intent.getBooleanExtra(ConnectActivity.IS_NETWORK_EXTRA, false)

        if(isNetwork){
            WifiComponent.init(applicationContext).inject(this)
        }else{
            BluetoothComponent.init(applicationContext).inject(this)
        }

        binding.find.setOnClickListener {
            buttonClicked(false)
            binding.progress.visibility = View.VISIBLE
            appScope.launch(Dispatchers.Default) {
                viewModel.getAddress()
                    .flowOn(Dispatchers.IO)
                    .collect {
                        withContext(Dispatchers.Main) {
                            buttonClicked(true)
                            binding.progress.visibility = View.INVISIBLE
                            if (it != null) {
                                binding.address.text = it.toString()
                            } else {
                                Toast.makeText(this@MenuPCRActivity, "Не удалось найти прибор", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
            }
        }

        binding.writeWeight.setOnClickListener{
            val writeWeightIntent = Intent(this, WriteWeightActivity::class.java)
            writeWeightIntent.putExtra(ConnectActivity.IS_NETWORK_EXTRA, isNetwork)
            startActivity(writeWeightIntent)
        }

        binding.writeChannel.setOnClickListener{
            val writeValueIntent = Intent(this, WriteValuesActivity::class.java)
            writeValueIntent.putExtra(ConnectActivity.IS_NETWORK_EXTRA, isNetwork)
            startActivity(writeValueIntent)
        }

        binding.findChannel.setOnClickListener{
            val findChannelIntent = Intent(this, FindChannelActivity::class.java)
            findChannelIntent.putExtra(ConnectActivity.IS_NETWORK_EXTRA, isNetwork)
            startActivity(findChannelIntent)
        }

        binding.readArchive.setOnClickListener{
            val readArchiveIntent = Intent(this, ArchiveActivity::class.java)
            readArchiveIntent.putExtra(ConnectActivity.IS_NETWORK_EXTRA, isNetwork)
            startActivity(readArchiveIntent)
        }

        lifecycleScope.launch(Dispatchers.Default){
            viewModel.completeSharedFloat.collect{
                withContext(Dispatchers.Main) {
                    binding.address.text = it.toString()
                    buttonClicked(true)
                    binding.progress.visibility = View.INVISIBLE
                }
            }
        }

        lifecycleScope.launch(Dispatchers.Default) {
            viewModel.errorsSharedFlow.collect {
                when(it){
                    is IOException -> {
                        withContext(Dispatchers.Main) {
                            buttonClicked(true)
                            Toast.makeText(this@MenuPCRActivity, "Не удалось найти устройство", Toast.LENGTH_SHORT)
                                .show()
                        }
                    }
                    is BluetoothTurnedOffException -> {
                        withContext(Dispatchers.Main){
                            buttonClicked(true)
                            Toast.makeText(this@MenuPCRActivity, it.message, Toast.LENGTH_SHORT)
                                .show()
                            val intent = Intent(this@MenuPCRActivity, ConnectActivity::class.java)
                            startActivity(intent)
                            finish()
                        }
                    }
                    is WifiTurnedOffException -> {
                        withContext(Dispatchers.Main){
                            buttonClicked(true)
                            Toast.makeText(this@MenuPCRActivity, it.message, Toast.LENGTH_SHORT).show()
                            startActivity(Intent(this@MenuPCRActivity, WifiConnectActivity::class.java))
                            finish()
                        }
                    }
                    is WrongWifi -> {
                        withContext(Dispatchers.Main){
                            buttonClicked(true)
                            Toast.makeText(this@MenuPCRActivity, it.message, Toast.LENGTH_SHORT).show()
                            startActivity(Intent(this@MenuPCRActivity, WifiConnectActivity::class.java))
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

    private fun buttonClicked(click: Boolean){
        binding.find.isEnabled = click
        binding.writeChannel.isEnabled = click
        binding.writeWeight.isEnabled = click
        binding.readArchive.isEnabled = click
        binding.findChannel.isEnabled = click
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if(item.itemId == android.R.id.home){
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}