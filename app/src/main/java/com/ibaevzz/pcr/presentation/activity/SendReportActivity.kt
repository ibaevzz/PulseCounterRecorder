package com.ibaevzz.pcr.presentation.activity

import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.entities.ChatId
import com.ibaevzz.pcr.IBAEVZZ_ID
import com.ibaevzz.pcr.databinding.ActivitySendReportBinding
import com.ibaevzz.pcr.di.app.AppComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

class SendReportActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySendReportBinding

    @Inject
    lateinit var appScope: CoroutineScope
    @Inject
    lateinit var bot: Bot

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySendReportBinding.inflate(layoutInflater)
        setContentView(binding.root)

        AppComponent.init(applicationContext).inject(this)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        binding.send.setOnClickListener{
            val message = binding.report.text.toString()
            if(message.isNotEmpty()) {
                val model = Build.MODEL
                val version = Build.VERSION.RELEASE
                val fullMessage = "Пометка: @report\nВерсия и тип устройства: $model  $version\nСообщение: $message"
                binding.send.isEnabled = false
                appScope.launch(Dispatchers.IO) {
                    val msg = bot.sendMessage(ChatId.fromId(IBAEVZZ_ID), fullMessage)
                    while (!msg.isSuccess) {}
                    withContext(Dispatchers.Main) {
                        binding.send.isEnabled = true
                        Toast.makeText(this@SendReportActivity, "Отправлено", Toast.LENGTH_SHORT)
                            .show()
                    }
                }
            }
        }
    }
}