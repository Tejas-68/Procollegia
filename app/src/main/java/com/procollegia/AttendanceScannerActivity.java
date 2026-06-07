package com.procollegia;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Vibrator;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AttendanceScannerActivity extends AppCompatActivity {

    private PreviewView previewView;
    private View llScanResult;
    private TextView tvName;
    private final HashSet<String> scannedIds = new HashSet<>();
    private java.util.HashMap<String, String> uucmsMap = new java.util.HashMap<>();
    private final ArrayList<String> newlyScanned = new ArrayList<>();
    
    private ExecutorService cameraExecutor;
    private BarcodeScanner scanner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_attendance_scanner);

        previewView  = findViewById(R.id.previewView);
        llScanResult = findViewById(R.id.llScanResult);
        tvName       = findViewById(R.id.tvScannedName);

        // Get student map to match against
        java.io.Serializable map = getIntent().getSerializableExtra("uucmsMap");
        if (map instanceof java.util.HashMap) {
            uucmsMap = (java.util.HashMap<String, String>) map;
        }

        cameraExecutor = Executors.newSingleThreadExecutor();
        scanner = BarcodeScanning.getClient(new BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
                .build());

        findViewById(R.id.btnDoneScanning).setOnClickListener(v -> finishWithResult());

        startCamera();
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> future = ProcessCameraProvider.getInstance(this);
        future.addListener(() -> {
            try {
                ProcessCameraProvider cp = future.get();
                bindPreview(cp);
            } catch (ExecutionException | InterruptedException e) { /* ignore */ }
        }, ContextCompat.getMainExecutor(this));
    }

    private void bindPreview(@NonNull ProcessCameraProvider cp) {
        Preview preview = new Preview.Builder().build();
        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        CameraSelector selector = CameraSelector.DEFAULT_BACK_CAMERA;

        ImageAnalysis analysis = new ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();

        analysis.setAnalyzer(cameraExecutor, this::processImageProxy);

        cp.unbindAll();
        cp.bindToLifecycle(this, selector, preview, analysis);
    }

    @SuppressLint("UnsafeOptInUsageError")
    private void processImageProxy(ImageProxy proxy) {
        if (proxy.getImage() == null) return;
        InputImage image = InputImage.fromMediaImage(proxy.getImage(), proxy.getImageInfo().getRotationDegrees());

        scanner.process(image)
                .addOnSuccessListener(barcodes -> {
                    for (Barcode b : barcodes) {
                        String raw = b.getRawValue();
                        if (raw != null) handleScan(raw);
                    }
                })
                .addOnCompleteListener(t -> proxy.close());
    }

    private synchronized void handleScan(String id) {
        if (scannedIds.contains(id)) return;

        if (uucmsMap.containsKey(id)) {
            scannedIds.add(id);
            newlyScanned.add(id);
            
            String studentName = uucmsMap.get(id);
            runOnUiThread(() -> showSuccess(id, studentName));
            
            Vibrator v = (Vibrator) getSystemService(VIBRATOR_SERVICE);
            if (v != null) v.vibrate(100);
        }
    }

    private void showSuccess(String id, String name) {
        tvName.setText("Marked: " + (name != null ? name : id)); 
        llScanResult.setVisibility(View.VISIBLE);
        llScanResult.postDelayed(() -> llScanResult.setVisibility(View.GONE), 2000);
    }

    private void finishWithResult() {
        Intent data = new Intent();
        data.putStringArrayListExtra("scanned", newlyScanned);
        setResult(RESULT_OK, data);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraExecutor.shutdown();
        scanner.close();
    }
}
