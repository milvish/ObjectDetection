// Copyright (c) 2020 Facebook, Inc. and its affiliates.
// All rights reserved.
//
// This source code is licensed under the BSD-style license found in the
// LICENSE file in the root directory of this source tree.

package org.pytorch.demo.objectdetection;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Size;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.annotation.UiThread;
import androidx.annotation.WorkerThread;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.camera.view.PreviewView;

import java.util.concurrent.ExecutionException;

public abstract class AbstractCameraXActivity<R> extends BaseModuleActivity {
    private static final int REQUEST_CODE_CAMERA_PERMISSION = 200;
    private static final String[] PERMISSIONS = {Manifest.permission.CAMERA};

    private long mLastAnalysisResultTime;

    protected abstract int getContentViewLayoutId();

    protected PreviewView getCameraPreviewTextureView(){
        return findViewById(org.pytorch.demo.objectdetection.R.id.object_detection_texture_view);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(getContentViewLayoutId());

        startBackgroundThread();

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    this,
                    PERMISSIONS,
                    REQUEST_CODE_CAMERA_PERMISSION);
        } else {
            setupCameraX();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_CAMERA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_DENIED) {
                Toast.makeText(
                                this,
                                "You can't use object detection example without granting CAMERA permission",
                                Toast.LENGTH_LONG)
                        .show();
                finish();
            } else {
                setupCameraX();
            }
        }
    }

    @OptIn(markerClass = ExperimentalGetImage.class)
    private void setupCameraX() {
        final PreviewView previewView = getCameraPreviewTextureView();

        // Создание объекта Preview
        final Preview preview = new Preview.Builder().build();
        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        // Создание объекта ImageAnalysis
        final ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                .setTargetResolution(new Size(640, 640))
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();

        /*
        imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(this), (imageProxy, rotationDegrees) -> {
            if (SystemClock.elapsedRealtime() - mLastAnalysisResultTime < 10) {
                imageProxy.close();
                return;
            }



            final R result = analyzeImage(imageProxy, rotationDegrees);
            if (result != null) {
                mLastAnalysisResultTime = SystemClock.elapsedRealtime();
                runOnUiThread(() -> applyToUiAnalyzeImageResult(result));
            }

            imageProxy.close();
        });

         */

        imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(this), new ImageAnalysis.Analyzer() {
            @Override
            public void analyze(@NonNull ImageProxy imageProxy) {
                int rotationDegrees = imageProxy.getImageInfo().getRotationDegrees();
                // insert your code here.
                if (SystemClock.elapsedRealtime() - mLastAnalysisResultTime < 10) {
                    imageProxy.close();
                    return;
                }



                final R result = analyzeImage(imageProxy, rotationDegrees);
                if (result != null) {
                    mLastAnalysisResultTime = SystemClock.elapsedRealtime();
                    runOnUiThread(() -> applyToUiAnalyzeImageResult(result));
                }

                imageProxy.close();
                // after done, release the ImageProxy object
            }
        });


        // Привязка жизненного цикла
        ProcessCameraProvider cameraProvider;
        try {
            cameraProvider = ProcessCameraProvider.getInstance(this).get();
            CameraSelector cameraSelector = new CameraSelector.Builder()
                    .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                    .build();

            cameraProvider.unbindAll(); // Отвязываем все предыдущие привязки
            cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis);
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    @WorkerThread
    @Nullable
    protected abstract R analyzeImage(ImageProxy image, int rotationDegrees);

    @UiThread
    protected abstract void applyToUiAnalyzeImageResult(R result);
}