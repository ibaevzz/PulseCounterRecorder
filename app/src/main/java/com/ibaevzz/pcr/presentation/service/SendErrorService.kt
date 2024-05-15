package com.ibaevzz.pcr.presentation.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.entities.ChatId
import com.ibaevzz.pcr.*
import com.ibaevzz.pcr.di.app.AppComponent
import kotlinx.coroutines.*
import javax.inject.Inject

class SendErrorService : Service() {

    @Inject
    lateinit var bot: Bot
    @Inject
    lateinit var appScope: CoroutineScope

    override fun onBind(intent: Intent) = null

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        AppComponent.init(this).inject(this)

        createNotificationChannel()
        val notification = createNotification()
        startForeground(NOTIFICATION_ID, notification)

        appScope.launch{
            while(!isClose) {
                val errorPref = getSharedPreferences(ERROR_SHARED_PREF, MODE_PRIVATE)
                val errors = errorPref.getStringSet(ERROR_SET, mutableSetOf())?.toMutableSet()?: mutableSetOf()
                val err = errors.toSet()
                for (i in err) {
                    bot.sendMessage(
                        ChatId.fromId(IBAEVZZ_ID),
                        "----------------------------------------------------"
                    )
                    val msg = bot.sendMessage(ChatId.fromId(IBAEVZZ_ID), i)
                    bot.sendMessage(
                        ChatId.fromId(IBAEVZZ_ID),
                        "----------------------------------------------------"
                    )
                    for(ii in 1..1000) {
                        if(msg.isSuccess) {
                            errors.remove(i)
                            errorPref.edit().putStringSet(ERROR_SET, errors).apply()
                        }
                    }
                }
            }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    private fun createNotificationChannel() {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            )
            channel.description = CHANNEL_DESCRIPTION

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
    }

    private fun createNotification(): Notification {
        val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.send)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(CHANNEL_NAME)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        return notificationBuilder.build()
    }

    companion object{
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "1"
        private const val CHANNEL_NAME = "Отправка отчета об ошибках"
        private const val CHANNEL_DESCRIPTION = "Идет отправка отчета об ошибке"
    }
}