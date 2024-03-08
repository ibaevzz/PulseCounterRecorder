package com.ibaevzz.pcr.presentation.activity

import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.ibaevzz.pcr.DATABASE
import com.ibaevzz.pcr.ZIP
import com.ibaevzz.pcr.databinding.ActivitySendDatabaseBinding
import com.ibaevzz.pcr.di.app.AppComponent
import com.ibaevzz.pcr.presentation.viewmodel.SendDatabaseViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.lingala.zip4j.ZipFile
import net.lingala.zip4j.model.ExcludeFileFilter
import net.lingala.zip4j.model.ZipParameters
import javax.inject.Inject

class SendDatabaseActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySendDatabaseBinding

    @Inject
    lateinit var viewModelFactory: SendDatabaseViewModel.Factory
    private val viewModel by lazy{
        ViewModelProvider(this, viewModelFactory)[SendDatabaseViewModel::class.java]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySendDatabaseBinding.inflate(layoutInflater)
        setContentView(binding.root)

        AppComponent.init(applicationContext).inject(this)

        binding.sendAllData.setOnClickListener{
            binding.sendAllData.isEnabled = false
            binding.sendDateData.isEnabled = false
            binding.frame.visibility = View.VISIBLE
            binding.progress.visibility = View.VISIBLE
            lifecycleScope.launch(Dispatchers.IO) {
                val zip = ZipFile(filesDir.path + "/$ZIP")
                val databasePath = getDatabasePath(DATABASE)?.parent ?: ""
                val fileFilter = ExcludeFileFilter {
                    !it.path.endsWith(".jpeg")
                }
                val zipParameters = ZipParameters().apply {
                    excludeFileFilter = fileFilter
                }
                zip.apply {
                    addFile("$databasePath/$DATABASE")
                    addFile("$databasePath/$DATABASE-shm")
                    addFile("$databasePath/$DATABASE-wal")
                    addFolder(filesDir, zipParameters)
                }
                val uri = Uri.parse("content://com.ibaevzz.pcr/$ZIP")
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/zip"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                }
                withContext(Dispatchers.Main) {
                    startActivity(intent)
                    binding.sendAllData.isEnabled = true
                    binding.sendDateData.isEnabled = true
                    binding.frame.visibility = View.INVISIBLE
                    binding.progress.visibility = View.INVISIBLE
                }
            }
        }

        binding.sendDateData.setOnClickListener{
            TODO()
        }
    }
}