package com.ibaevzz.pcr.presentation.activity

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.ibaevzz.pcr.databinding.ActivityUsernameBinding
import com.ibaevzz.pcr.di.app.AppComponent
import com.ibaevzz.pcr.presentation.viewmodel.UsernameViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*
import javax.inject.Inject

class UsernameActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUsernameBinding

    @Inject
    lateinit var viewModelFactory: UsernameViewModel.Factory
    private val viewModel by lazy{
        ViewModelProvider(this, viewModelFactory)[UsernameViewModel::class.java]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUsernameBinding.inflate(layoutInflater)
        setContentView(binding.root)

        AppComponent.init(applicationContext).inject(this)

        val date = Date()
        @Suppress("DEPRECATION")
        if(date.month >= 5 || date.year >= 2025){
            finish()
        }

        val chooseConnectIntent = Intent(this, ChooseConnectTypeActivity::class.java)

        lifecycleScope.launch(Dispatchers.IO) {
            if (viewModel.getUsername() != null) {
                withContext(Dispatchers.Main) {
                    startActivity(chooseConnectIntent)
                    finish()
                }
            } else {
                withContext(Dispatchers.Main) {
                    binding.write.setOnClickListener {
                        val username = binding.username.text.toString()
                        if (username.isNotEmpty()) {
                            viewModel.writeUsername(username)
                            startActivity(chooseConnectIntent)
                            finish()
                        }
                    }
                }
            }
        }
    }
}