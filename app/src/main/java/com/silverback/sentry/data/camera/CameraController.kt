package com.silverback.sentry.data.camera

import android.content.Context
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

// Captured photos are saved to internal app storage (context.filesDir), never
// shared with another app via an Intent, so no FileProvider/content:// URI is
// needed here - a plain file:// URI is readable by this app's own process for
// both display and the later Firebase Storage upload (Phase 4's
// ObservationRemoteDataSourceImpl already accepts either).
@Singleton
class CameraController @Inject constructor() {

    private var imageCapture: ImageCapture? = null

    fun bindToLifecycle(context: Context, lifecycleOwner: LifecycleOwner, previewView: PreviewView) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener(
            {
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }
                val capture = ImageCapture.Builder().build()
                imageCapture = capture

                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    capture,
                )
            },
            ContextCompat.getMainExecutor(context),
        )
    }

    suspend fun takePhoto(context: Context): Result<String> {
        val capture = imageCapture
            ?: return Result.failure(IllegalStateException("Camera is not ready yet"))
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US).format(System.currentTimeMillis())
        val photoFile = File(context.filesDir, "gorilla_$timestamp.jpg")
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        return suspendCancellableCoroutine { continuation ->
            capture.takePicture(
                outputOptions,
                ContextCompat.getMainExecutor(context),
                object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                        continuation.resume(Result.success(photoFile.toUri().toString()))
                    }

                    override fun onError(exception: ImageCaptureException) {
                        continuation.resume(Result.failure(exception))
                    }
                },
            )
        }
    }
}
