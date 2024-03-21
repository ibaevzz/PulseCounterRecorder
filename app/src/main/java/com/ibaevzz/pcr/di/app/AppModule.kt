package com.ibaevzz.pcr.di.app

import android.content.Context
import androidx.room.Room
import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.bot
import com.github.kotlintelegrambot.entities.ChatId
import com.ibaevzz.pcr.DATABASE
import com.ibaevzz.pcr.INTERMEDIATE_DATABASE
import com.ibaevzz.pcr.MIGRATION_1_2
import com.ibaevzz.pcr.data.db.IntermediateDatabase
import com.ibaevzz.pcr.data.db.PulsarDatabase
import com.ibaevzz.pcr.di.bluetooth.BluetoothComponent
import com.ibaevzz.pcr.di.wifi.WifiComponent
import com.ibaevzz.pcr.IBAEVZZ_ID
import com.ibaevzz.pcr.TOKEN
import dagger.Module
import dagger.Provides
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import java.util.concurrent.Executors
import javax.inject.Singleton

@Module(subcomponents = [WifiComponent::class, BluetoothComponent::class])
class AppModule{
    @Provides
    @Singleton
    fun provideAppScope(bot: Bot): CoroutineScope{
        return CoroutineScope(Executors.newFixedThreadPool(16).asCoroutineDispatcher() +
                CoroutineExceptionHandler{_, throwable ->
                    val message = "Тип ошибки: ${throwable::class.simpleName}\nТекст ошибки: ${throwable.stackTraceToString()}"
                    bot.sendMessage(ChatId.fromId(IBAEVZZ_ID), "----------------------------------------------------")
                    bot.sendMessage(ChatId.fromId(IBAEVZZ_ID), message)
                    val msg = bot.sendMessage(ChatId.fromId(IBAEVZZ_ID), "----------------------------------------------------")
                    for(i in 1..1000){
                        if(msg.isSuccess){
                            throw throwable
                        }
                    }
                    throw throwable
                })
    }
    @Provides
    @Singleton
    fun provideDatabase(context: Context): PulsarDatabase{
        return Room.databaseBuilder(context, PulsarDatabase::class.java, DATABASE)
            .addMigrations(MIGRATION_1_2)
            .build()
    }
    @Provides
    @Singleton
    fun provideIntermediateDatabase(context: Context): IntermediateDatabase{
        return Room.databaseBuilder(context, IntermediateDatabase::class.java, INTERMEDIATE_DATABASE)
            .addMigrations(MIGRATION_1_2)
            .build()
    }
    @Provides
    @Singleton
    fun provideBot(): Bot {
        return bot{token = TOKEN }.apply {
            startPolling()
        }
    }
}