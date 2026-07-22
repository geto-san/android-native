package com.silverback.sentry.ui.addsighting

import android.content.Context
import androidx.camera.view.PreviewView
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import com.silverback.sentry.data.camera.CameraController
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

// Thin pass-through so CameraCaptureScreen accesses the camera only through a
// ViewModel, same as every other screen - CameraController itself is a plain
// injected singleton, not a ViewModel, so it can't be obtained via
// hiltViewModel() directly from a Composable.
@HiltViewModel
class CameraCaptureViewModel @Inject constructor(
    private val cameraController: CameraController,
) : ViewModel() {

    fun bindToLifecycle(context: Context, lifecycleOwner: LifecycleOwner, previewView: PreviewView) {
        cameraController.bindToLifecycle(context, lifecycleOwner, previewView)
    }

    suspend fun takePhoto(context: Context): Result<String> = cameraController.takePhoto(context)
}
