package com.ibaevzz.pcr.presentation.activity

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.ibaevzz.pcr.databinding.ActivityPhotoBinding
import com.ibaevzz.pcr.databinding.ImagePreviewViewBinding
import com.ibaevzz.pcr.di.app.AppComponent
import com.ibaevzz.pcr.presentation.viewmodel.PhotoViewModel
import com.ibaevzz.pcr.rotateBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import javax.inject.Inject

class PhotoActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPhotoBinding
    private lateinit var bindingImagePreview: ImagePreviewViewBinding

    private var imageId: Long? = null
    private var devInfoId: Long? = null
    private var address: Long? = null
    private var channel: Long? = null

    private var outputImage: Bitmap? = null

    private var imageCapture: ImageCapture? = null
    private val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()

    private val activityResultLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()){
            if(allPermissionsGranted()){
                startCamera()
            }else{
                requestPermissions()
            }
        }

    @Inject
    lateinit var viewModelFactory: PhotoViewModel.Factory
    private val viewModel by lazy {
        ViewModelProvider(this, viewModelFactory)[PhotoViewModel::class.java]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPhotoBinding.inflate(layoutInflater)
        bindingImagePreview = ImagePreviewViewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        AppComponent.init(applicationContext).inject(this)

        devInfoId = intent.getLongExtra(DEVICE_INFO_ID_EXTRA, -1)
        address = intent.getLongExtra(DEVICE_ADDRESS_EXTRA, -1)
        channel = intent.getLongExtra(CHANNEL_EXTRA, -1)

        if(devInfoId == -1L || address == -1L || channel == -1L) finish()

        lifecycleScope.launch(Dispatchers.IO){
            imageId = viewModel.getIdForImage()
        }

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            requestPermissions()
        }

        bindingImagePreview.save.setOnClickListener{
            bindingImagePreview.save.isEnabled = false
            bindingImagePreview.dont.isEnabled = false
            try {
                val path = "${address}_${channel}_${imageId}.jpeg"
                val fileOutputStream = openFileOutput(path, Context.MODE_PRIVATE)
                if (outputImage != null) {
                    lifecycleScope.launch(Dispatchers.IO){
                        outputImage?.compress(Bitmap.CompressFormat.JPEG, 80, fileOutputStream)
                        viewModel.writeToDb(imageId!!, devInfoId!!, path)
                        fileOutputStream.close()
                        withContext(Dispatchers.Main){
                            finish()
                        }
                    }
                }else{
                    Toast.makeText(this, "Ошибка сохранения", Toast.LENGTH_SHORT).show()
                    setContentView(binding.root)
                    bindingImagePreview.save.isEnabled = false
                    bindingImagePreview.dont.isEnabled = false
                }
            }catch (ex: Exception){
                Toast.makeText(this, "Ошибка сохранения", Toast.LENGTH_SHORT).show()
                setContentView(binding.root)
                outputImage = null
                bindingImagePreview.save.isEnabled = false
                bindingImagePreview.dont.isEnabled = false
            }
        }

        bindingImagePreview.dont.setOnClickListener{
            setContentView(binding.root)
            outputImage = null
        }

        binding.photo.setOnClickListener{
            makePhoto()
        }

    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(binding.preview.surfaceProvider)
                }
            imageCapture = ImageCapture.Builder()
                .build()
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture)

            } catch(_: Exception) {}

        }, ContextCompat.getMainExecutor(this))
    }

    private fun makePhoto(){
        imageCapture?.takePicture(ContextCompat.getMainExecutor(this),
        object: ImageCapture.OnImageCapturedCallback() {
            @androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
            override fun onCaptureSuccess(imageProxy: ImageProxy) {
                val image = imageProxy.image
                val buffer = image?.planes?.get(0)?.buffer
                val bytes = if(buffer!=null) {
                    ByteArray(buffer.capacity())
                }else{
                    null
                }
                if(bytes != null) {
                    buffer?.get(bytes)
                    val rotateOutputImage = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, null)
                    outputImage = rotateOutputImage.rotateBitmap(90f)
                    bindingImagePreview.previewImage.setImageBitmap(outputImage)
                    setContentView(bindingImagePreview.root)
                }
                imageProxy.close()
            }
        })
    }

    private fun requestPermissions() {
        activityResultLauncher.launch(REQUIRED_PERMISSIONS)
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
    }

    companion object {
        const val DEVICE_INFO_ID_EXTRA = "DEVICE_INFO_ID"
        const val DEVICE_ADDRESS_EXTRA = "DEVICE_ADDRESS"
        const val CHANNEL_EXTRA = "CHANNEL"
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }
}