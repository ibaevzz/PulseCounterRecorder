package com.ibaevzz.pcr.presentation.activity

import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.ibaevzz.pcr.DATABASE
import com.ibaevzz.pcr.DATE_ZIP
import com.ibaevzz.pcr.INTERMEDIATE_DATABASE
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
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

class SendDatabaseActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySendDatabaseBinding

    @Inject
    lateinit var viewModelFactory: SendDatabaseViewModel.Factory
    private val viewModel by lazy{
        ViewModelProvider(this, viewModelFactory)[SendDatabaseViewModel::class.java]
    }

    private var doDate = Calendar.getInstance()
    private var otDate = Calendar.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySendDatabaseBinding.inflate(layoutInflater)
        setContentView(binding.root)

        AppComponent.init(applicationContext).inject(this)

        binding.sendAllData.setOnClickListener{
            binding.sendAllData.isEnabled = false
            binding.sendDateData.isEnabled = false
            binding.otButton.isEnabled = false
            binding.doButton.isEnabled = false
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
                    binding.otButton.isEnabled = true
                    binding.doButton.isEnabled = true
                    binding.frame.visibility = View.INVISIBLE
                    binding.progress.visibility = View.INVISIBLE
                }
            }
        }

        binding.otButton.setOnClickListener{
            val datePicker = DatePickerDialog(this)
            datePicker.setOnDateSetListener{_, y, m, d ->
                otDate.set(y, m, d)
                val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
                binding.otText.text = dateFormat.format(otDate.time)
            }
            datePicker.show()
        }

        binding.doButton.setOnClickListener{
            val datePicker = DatePickerDialog(this)
            datePicker.setOnDateSetListener{_, y, m, d ->
                doDate.set(y, m, d)
                val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
                binding.doText.text = dateFormat.format(doDate.time)
            }
            datePicker.show()
        }

        binding.sendDateData.setOnClickListener{
            if(doDate.time <= otDate.time){
                Toast.makeText(this, "Неподходящие даты", Toast.LENGTH_SHORT).show()
            }else{
                binding.sendAllData.isEnabled = false
                binding.sendDateData.isEnabled = false
                binding.otButton.isEnabled = false
                binding.doButton.isEnabled = false
                binding.frame.visibility = View.VISIBLE
                binding.progress.visibility = View.VISIBLE
                lifecycleScope.launch(Dispatchers.IO) {
                    val paths = viewModel.updateIntermediateDatabase(otDate.time, doDate.time)
                    val zip = ZipFile(filesDir.path + "/$DATE_ZIP")
                    val databasePath = getDatabasePath(DATABASE)?.parent ?: ""
                    val fileFilter = ExcludeFileFilter {
                        var filter = true
                        for(i in paths) {
                            filter = !it.path.endsWith(i)
                        }
                        filter
                    }
                    val zipParameters = ZipParameters().apply {
                        excludeFileFilter = fileFilter
                    }
                    zip.apply {
                        addFile("$databasePath/$INTERMEDIATE_DATABASE")
                        addFile("$databasePath/$INTERMEDIATE_DATABASE-shm")
                        addFile("$databasePath/$INTERMEDIATE_DATABASE-wal")
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
                        binding.otButton.isEnabled = true
                        binding.doButton.isEnabled = true
                        binding.frame.visibility = View.INVISIBLE
                        binding.progress.visibility = View.INVISIBLE
                    }
                }
            }
        }
    }
}