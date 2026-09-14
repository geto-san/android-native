package com.silversentry.sentry.feature.report

import android.content.Context
import androidx.camera.view.PreviewView
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import com.silversentry.sentry.core.data.camera.CameraController
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class CameraCaptureViewModel @Inject constructor(
    private val cameraController: CameraController,
) : ViewModel() {

    fun bindToLifecycle(context: Context, lifecycleOwner: LifecycleOwner, previewView: PreviewView) {
        cameraController.bindToLifecycle(context, lifecycleOwner, previewView)
    }

    suspend fun takePhoto(context: Context): Result<String> {
        return cameraController.takePhoto(context)
    }
}
