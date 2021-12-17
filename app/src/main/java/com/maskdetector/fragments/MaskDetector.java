package com.maskdetector.fragments;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.camera.core.AspectRatio;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraInfoUnavailableException;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LifecycleEventObserver;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.common.util.concurrent.ListenableFuture;
import com.maskdetector.R;
import com.maskdetector.detection.env.YuvToRgbConverter;
import com.maskdetector.ml.FaceMaskDetection;

import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.label.Category;
import org.tensorflow.lite.support.model.Model;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MaskDetector extends Fragment {

    private static final String TAG = "FACE_MASK_DETECTOR";
    private static final Integer REQUEST_CODE_PERMISSIONS = 0x98;
    private static final String[] REQUIRED_PERMISSIONS = new String[] {Manifest.permission.CAMERA};
    private static final Double RATIO_4_3_VALUE = 4.0 / 3.0;
    private static final Double RATIO_16_9_VALUE = 16.0 / 9.0;
    private static final int TF_NUM_THREADS = 5;

    private Preview preview = null;
    private ImageAnalysis imageAnalyzer = null;
    private ProcessCameraProvider cameraProvider = null;

    private Integer lensFacing = CameraSelector.LENS_FACING_BACK;

    private Camera camera = null;

    private ExecutorService cameraExecutor;

    private FrameLayout maskDetectorFrameLayout;
    private PreviewView previewView;
    private FloatingActionButton cameraSwitcher;
    private TextView detectionTxtOutput;

    private FaceMaskDetection faceMaskDetection;

    private ActivityResultContracts.RequestMultiplePermissions requestMultiplePermissions;
    private ActivityResultLauncher<String[]> multiplePermissionActivityResultLauncher;

    public MaskDetector() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        ViewGroup root = (ViewGroup) inflater.inflate(R.layout.fragment_mask_detector, container, false);

        setupViewAttributes(root);
        setupActivityResult();

        setupML();
        setupCameraThread();
        setupCameraControllers();
        requireCameraPermission();

        return root;
    }

    private void setupActivityResult() {
        requestMultiplePermissions = new ActivityResultContracts.RequestMultiplePermissions();
        multiplePermissionActivityResultLauncher = registerForActivityResult(requestMultiplePermissions, this::grantedCameraPermission);
    }

    private void setupViewAttributes(ViewGroup root) {
        maskDetectorFrameLayout = root.findViewById(R.id.mask_detector_frame_layout);
        previewView = root.findViewById(R.id.preview_view);
        cameraSwitcher = root.findViewById(R.id.camera_switcher);
        detectionTxtOutput = root.findViewById(R.id.detection_txt_output);
    }

    private void setupML() {
        Model.Options options = new Model.Options.Builder()
            .setDevice(Model.Device.GPU)
            .setNumThreads(TF_NUM_THREADS)
            .build();
        try {
            faceMaskDetection = FaceMaskDetection.newInstance(requireContext(), options);
        } catch (IOException exception) {
            Log.e(TAG, "Could not load the tensorflow-lite model.", exception);
        }
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private void setupMLOutput(Bitmap bitmap) {
        TensorImage tfImage = TensorImage.fromBitmap(bitmap);
        FaceMaskDetection.Outputs result = faceMaskDetection.process(tfImage);
        List<Category> output = result.getProbabilityAsCategoryList();

        new Handler(Looper.getMainLooper()).post(() -> getLifecycle().addObserver((LifecycleEventObserver) (source, event) -> {
            int supIndex = output.get(0).getScore() > output.get(1).getScore() ? 0 : 1;

            boolean isMaskOn = output.get(supIndex)
                    .getLabel()
                    .equals("with_mask");
            float score = output.get(supIndex).getScore();

            String message = String.valueOf(isMaskOn ?
                    requireContext().getText(R.string.label_with_mask) :
                    requireContext().getText(R.string.label_without_mask));
            message += " - " + Double.valueOf(score*100).intValue() + "%";
            detectionTxtOutput.setText(message);

            int color = isMaskOn ?
                    R.color.blue_400 :
                    R.color.red_600;
            detectionTxtOutput.setTextColor(requireContext().getColor(color));

            int border = isMaskOn ?
                    R.drawable.with_mask_border :
                    R.drawable.without_mask_border;
            maskDetectorFrameLayout.setBackground(requireContext().getDrawable(border));
        }));
    }

    private void setupCameraThread() {
        cameraExecutor = Executors.newSingleThreadExecutor();
    }

    private void setupCameraControllers() {
        setLensButtonIcon();
        cameraSwitcher.setOnClickListener(it -> {
            lensFacing = lensFacing == CameraSelector.LENS_FACING_FRONT ?
                CameraSelector.LENS_FACING_BACK :
                CameraSelector.LENS_FACING_FRONT;

            setLensButtonIcon();
            setupCameraUseCases();
        });

        try {
            cameraSwitcher.setEnabled(hasBackCamera() && hasFrontCamera());
        } catch (CameraInfoUnavailableException exception) {
            cameraSwitcher.setEnabled(false);
        }
    }

    private void setupCameraUseCases() {
        CameraSelector cameraSelector = new CameraSelector.Builder().requireLensFacing(lensFacing).build();

        DisplayMetrics metrics = new DisplayMetrics();
        previewView.getDisplay().getRealMetrics(metrics);

        int rotation = previewView.getDisplay().getRotation();
        int screenAspectRatio = aspectRatio(metrics.widthPixels, metrics.heightPixels);

        preview = new Preview.Builder()
            .setTargetAspectRatio(screenAspectRatio)
            .setTargetRotation(rotation)
            .build();

        imageAnalyzer = new ImageAnalysis.Builder()
            .setTargetAspectRatio(screenAspectRatio)
            .setTargetRotation(rotation)
            .build();
        imageAnalyzer.setAnalyzer(cameraExecutor, new BitmapOutputAnalysis(getContext()));

        if (cameraProvider != null) {
            cameraProvider.unbindAll();

            try {
                camera  = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalyzer);
                preview.setSurfaceProvider(previewView.createSurfaceProvider());

            } catch (Exception exception) {
                Log.e(TAG, "Use case binding failure.", exception);
            }
        }
    }

    private void requireCameraPermission() {
        if (!checkIfAllPermissionsGranted()) {
            multiplePermissionActivityResultLauncher.launch(REQUIRED_PERMISSIONS);
        } else {
            setupCamera();
        }
    }

    private void grantedCameraPermission(Map<String, Boolean> isGranted) {
        if (isGranted.containsValue(false)) {
            Snackbar.make(
                requireView(),
                requireContext().getString(R.string.permissions_not_granted_snackbar),
                Snackbar.LENGTH_LONG
            )
            .setAction(R.string.grant_permission_action, v -> {
                requireCameraPermission();
            })
            .show();

            Log.i(TAG, "Permissions are not granted.");
        } else {
            setupCamera();
        }
    }

    private void setupCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
            ProcessCameraProvider.getInstance(requireContext());
        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();
                if (hasFrontCamera()) {
                    lensFacing = CameraSelector.LENS_FACING_FRONT;
                } else if (hasBackCamera()) {
                    lensFacing = CameraSelector.LENS_FACING_BACK;
                } else {
                    throw new IllegalStateException("No cameras are available.");
                }

                setupCameraControllers();
                setupCameraUseCases();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(requireContext()));
    }

    private void setLensButtonIcon() {
        int icon = lensFacing == CameraSelector.LENS_FACING_FRONT ?
            R.drawable.ic_baseline_camera_rear_24 :
            R.drawable.ic_baseline_camera_front_24;

        cameraSwitcher.setImageDrawable(AppCompatResources.getDrawable(requireContext(), icon));
    }

    private Boolean checkIfAllPermissionsGranted() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(requireContext(), permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }

        return true;
    }

    private Boolean hasFrontCamera() throws CameraInfoUnavailableException {
        if (cameraProvider == null) {
            return false;
        }

        return cameraProvider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA);
    }

    private Boolean hasBackCamera() throws CameraInfoUnavailableException {
        if (cameraProvider == null) {
            return false;
        }

        return cameraProvider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA);
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        setupCameraControllers();
    }

    private Integer aspectRatio(Integer width, Integer height) {
        Double previewRation = Integer.valueOf(Math.max(width, height)).doubleValue() / Math.min(width, height);

        if (Math.abs(previewRation - RATIO_4_3_VALUE) <= Math.abs(previewRation - RATIO_16_9_VALUE)) {
            return AspectRatio.RATIO_4_3;
        }

        return AspectRatio.RATIO_16_9;
    }

    private class BitmapOutputAnalysis implements ImageAnalysis.Analyzer {
        private final Context context;
        private final YuvToRgbConverter yuvToRgbConverter;

        private Bitmap bitmapBuffer;
        private Matrix rotationMatrix;

        BitmapOutputAnalysis(Context context) {
            this.context = context;
            yuvToRgbConverter = new YuvToRgbConverter(context);
        }

        @Override
        public void analyze(@NonNull ImageProxy imageProxy) {
            try {
                Bitmap bitmap = toBitmap(imageProxy);
                setupMLOutput(bitmap);
            } catch (Exception exception) {
                Log.e(TAG, "An error occurred within the bitmap output analysis.", exception);
            } finally {
                imageProxy.close();
            }
        }

        @SuppressLint({"UnsafeExperimentalUsageError", "UnsafeOptInUsageError"})
        private Bitmap toBitmap(ImageProxy imageProxy) throws Exception {

            if (bitmapBuffer == null) {
                rotationMatrix = new Matrix();
                rotationMatrix.postRotate(Integer.valueOf(imageProxy.getImageInfo().getRotationDegrees()).floatValue());
                bitmapBuffer = Bitmap.createBitmap(imageProxy.getWidth(), imageProxy.getHeight(), Bitmap.Config.ARGB_8888);
            }

            yuvToRgbConverter.yuvToRgb(imageProxy.getImage(), bitmapBuffer);

            return Bitmap.createBitmap(
                bitmapBuffer,
                0,
                0,
                bitmapBuffer.getWidth(),
                bitmapBuffer.getHeight(),
                rotationMatrix,
                false
            );
        }
    }
}