package com.namgyun.tamakitchen.ui.shopping;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.util.Size;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraControl;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.FocusMeteringAction;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.MeteringPoint;
import androidx.camera.core.MeteringPointFactory;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.namgyun.tamakitchen.R;
import com.namgyun.tamakitchen.ui.common.AppToast;
import com.google.android.material.button.MaterialButton;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class ReceiptScannerActivity extends AppCompatActivity {

    private static final String TAG = "ReceiptScannerActivity";
    private static final int REQ_CAMERA = 2101;

    private static final long OCR_INTERVAL_MS = 650;
    private static final long QUALITY_INTERVAL_MS = 250;

    private static final int TRIGGER_STREAK = 3;
    private static final int CLEAR_STREAK = 3;
    private static final int READY_STREAK = 3;
    private static final int MIN_RAW_LEN_FOR_READY = 60;

    private PreviewView previewView;
    private TextView tvGuide, tvStatus, tvHint;
    private MaterialButton btnCancel, btnDone;
    private ImageButton btnClose, btnTorch, btnZoom;

    private ExecutorService cameraExecutor;
    private TextRecognizer recognizer;

    private volatile String lastRecognizedText = "";
    private final AtomicBoolean isProcessing = new AtomicBoolean(false);

    private Camera camera;
    private CameraControl cameraControl;
    private ScaleGestureDetector scaleGestureDetector;

    private long lastOcrTime = 0L;
    private long lastQualityTime = 0L;

    private boolean torchOn = false;
    private boolean zoom2x = false;

    private enum HintType {
        NONE,
        TOO_DARK,
        TOO_BRIGHT,
        BLUR_SHAKE,
        TOO_FAR
    }

    private HintType shownHint = HintType.NONE;
    private HintType pendingHint = HintType.NONE;
    private int pendingStreak = 0;
    private int clearStreak = 0;

    private boolean isReadyShown = false;
    private int readyStreak = 0;

    private HintType lastOcrHint = HintType.NONE;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_receipt_scanner);

        previewView = findViewById(R.id.previewView);
        tvGuide = findViewById(R.id.tvGuide);
        tvStatus = findViewById(R.id.tvStatus);
        tvHint = findViewById(R.id.tvHint);

        btnCancel = findViewById(R.id.btnCancel);
        btnDone = findViewById(R.id.btnDone);

        btnClose = findViewById(R.id.btnClose);
        btnTorch = findViewById(R.id.btnTorch);
        btnZoom = findViewById(R.id.btnZoom);

        cameraExecutor = Executors.newSingleThreadExecutor();
        recognizer = TextRecognition.getClient(
                new KoreanTextRecognizerOptions.Builder().build()
        );

        btnClose.setOnClickListener(v -> {
            setResult(RESULT_CANCELED);
            finish();
        });

        btnCancel.setOnClickListener(v -> {
            setResult(RESULT_CANCELED);
            finish();
        });

        btnDone.setOnClickListener(v -> {
            if (lastRecognizedText == null || lastRecognizedText.trim().isEmpty()) {
                AppToast.show(this, "아직 인식된 텍스트가 없어요.");
                return;
            }

            Intent result = new Intent();
            result.putExtra(ReceiptReviewActivity.EXTRA_RAW_TEXT, lastRecognizedText);
            setResult(RESULT_OK, result);
            finish();
        });

        btnTorch.setOnClickListener(v -> toggleTorch());
        btnZoom.setOnClickListener(v -> toggleZoom());

        scaleGestureDetector = new ScaleGestureDetector(this,
                new ScaleGestureDetector.SimpleOnScaleGestureListener() {
                    @Override
                    public boolean onScale(ScaleGestureDetector detector) {
                        if (cameraControl == null || camera == null) return false;

                        float scale = detector.getScaleFactor();
                        Float current = camera.getCameraInfo()
                                .getZoomState()
                                .getValue()
                                .getZoomRatio();

                        if (current != null) {
                            cameraControl.setZoomRatio(current * scale);
                        }
                        return true;
                    }
                });

        previewView.setOnTouchListener((v, event) -> {
            scaleGestureDetector.onTouchEvent(event);

            if (event.getAction() == MotionEvent.ACTION_UP) {
                tapToFocus(event.getX(), event.getY());
            }
            return true;
        });

        tvGuide.setText("영수증을 프레임 안에 맞춰주세요");
        tvStatus.setText("텍스트 인식 중...");
        showHint(HintType.NONE);
        showReady(false);

        if (hasCameraPermission()) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.CAMERA},
                    REQ_CAMERA
            );
        }
    }

    private boolean hasCameraPermission() {
        return ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED;
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> future =
                ProcessCameraProvider.getInstance(this);

        future.addListener(() -> {
            try {
                ProcessCameraProvider provider = future.get();

                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                ImageAnalysis analysis = new ImageAnalysis.Builder()
                        .setTargetResolution(new Size(1280, 720))
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();

                analysis.setAnalyzer(cameraExecutor, this::analyzeImage);

                provider.unbindAll();

                camera = provider.bindToLifecycle(
                        this,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        analysis
                );

                cameraControl = camera.getCameraControl();
                cameraControl.setZoomRatio(1.2f);

            } catch (Exception e) {
                Log.e(TAG, "Camera start error", e);
                tvStatus.setText("카메라 시작 실패");
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void toggleTorch() {
        if (cameraControl == null) return;
        torchOn = !torchOn;
        cameraControl.enableTorch(torchOn);
    }

    private void toggleZoom() {
        if (cameraControl == null || camera == null) return;

        zoom2x = !zoom2x;
        float target = zoom2x ? 2.0f : 1.2f;
        cameraControl.setZoomRatio(target);
    }

    private void tapToFocus(float x, float y) {
        if (cameraControl == null) return;

        MeteringPointFactory factory = previewView.getMeteringPointFactory();
        MeteringPoint point = factory.createPoint(x, y);

        FocusMeteringAction action =
                new FocusMeteringAction.Builder(point, FocusMeteringAction.FLAG_AF)
                        .setAutoCancelDuration(2, TimeUnit.SECONDS)
                        .build();

        cameraControl.startFocusAndMetering(action);
    }

    private void analyzeImage(@NonNull ImageProxy imageProxy) {
        long now = SystemClock.elapsedRealtime();

        if (now - lastQualityTime >= QUALITY_INTERVAL_MS) {
            lastQualityTime = now;

            HintType qualityHint = evaluateFrameQuality(imageProxy);

            if (qualityHint != HintType.NONE) {
                updateHintStateMachine(qualityHint);
            } else {
                if (lastOcrHint == HintType.TOO_FAR) {
                    updateHintStateMachine(HintType.TOO_FAR);
                } else {
                    updateHintStateMachine(HintType.NONE);
                }
            }

            updateReadyState(qualityHint);
        }

        if (now - lastOcrTime < OCR_INTERVAL_MS) {
            imageProxy.close();
            return;
        }
        lastOcrTime = now;

        if (isProcessing.getAndSet(true)) {
            imageProxy.close();
            return;
        }

        try {
            if (imageProxy.getImage() == null) {
                isProcessing.set(false);
                imageProxy.close();
                return;
            }

            InputImage image = InputImage.fromMediaImage(
                    imageProxy.getImage(),
                    imageProxy.getImageInfo().getRotationDegrees()
            );

            recognizer.process(image)
                    .addOnSuccessListener(result -> {
                        String rowRaw = ReceiptOcrRowFormatter.formatToRows(result);

                        if (rowRaw != null && rowRaw.trim().length() > 20) {
                            lastRecognizedText = rowRaw;
                            int lines = rowRaw.split("\\r?\\n").length;
                            tvStatus.setText("텍스트 인식됨 (행 " + lines + "줄 / " + rowRaw.length() + "자)");
                        } else {
                            String plain = (result == null) ? "" : result.getText();
                            if (plain != null && plain.trim().length() > 20) {
                                lastRecognizedText = plain;
                                tvStatus.setText("텍스트 인식됨 (" + plain.length() + "자)");
                            }
                        }

                        lastOcrHint = evaluateOcrCoverageHint(result);
                        updateReadyStateFromOcr();
                    })
                    .addOnCompleteListener(task -> {
                        isProcessing.set(false);
                        imageProxy.close();
                    });

        } catch (Exception e) {
            isProcessing.set(false);
            imageProxy.close();
        }
    }

    private void updateHintStateMachine(@NonNull HintType observed) {
        if (observed == HintType.NONE) {
            clearStreak++;
            pendingHint = HintType.NONE;
            pendingStreak = 0;

            if (shownHint != HintType.NONE && clearStreak >= CLEAR_STREAK) {
                showHint(HintType.NONE);
            }
            return;
        }

        clearStreak = 0;

        if (shownHint == observed) {
            pendingHint = HintType.NONE;
            pendingStreak = 0;
            return;
        }

        if (pendingHint != observed) {
            pendingHint = observed;
            pendingStreak = 1;
        } else {
            pendingStreak++;
        }

        if (pendingStreak >= TRIGGER_STREAK) {
            showHint(observed);
            pendingHint = HintType.NONE;
            pendingStreak = 0;
        }
    }

    private void showHint(@NonNull HintType type) {
        if (shownHint == type) return;
        shownHint = type;

        runOnUiThread(() -> {
            switch (type) {
                case NONE:
                    tvHint.setText("");
                    tvHint.setAlpha(0f);
                    break;

                case BLUR_SHAKE:
                    tvHint.setText("조금만 멈춰주시면 선명하게 인식돼요.");
                    tvHint.setAlpha(1f);
                    break;

                case TOO_DARK:
                    tvHint.setText("조금 어두워요. 손전등을 켜주시면 더 잘 인식돼요.");
                    tvHint.setAlpha(1f);
                    break;

                case TOO_BRIGHT:
                    tvHint.setText("빛 반사가 있어요. 각도를 살짝만 바꿔주시면 좋아요.");
                    tvHint.setAlpha(1f);
                    break;

                case TOO_FAR:
                    tvHint.setText("영수증이 작게 보여요. 조금만 가까이 가져와 주세요.");
                    tvHint.setAlpha(1f);
                    break;
            }
        });
    }

    private void updateReadyState(@NonNull HintType qualityHint) {
        boolean qualityOk = (qualityHint == HintType.NONE);
        boolean farOk = (lastOcrHint != HintType.TOO_FAR);
        boolean textOk = (lastRecognizedText != null && lastRecognizedText.trim().length() >= MIN_RAW_LEN_FOR_READY);

        boolean readyNow = qualityOk && farOk && textOk;

        if (readyNow) {
            readyStreak++;
            if (readyStreak >= READY_STREAK) showReady(true);
        } else {
            readyStreak = 0;
            showReady(false);
        }
    }

    private void updateReadyStateFromOcr() {
        boolean farOk = (lastOcrHint != HintType.TOO_FAR);
        boolean textOk = (lastRecognizedText != null && lastRecognizedText.trim().length() >= MIN_RAW_LEN_FOR_READY);

        if (!farOk || !textOk) {
            readyStreak = 0;
            showReady(false);
        }
    }

    private void showReady(boolean show) {
        if (isReadyShown == show) return;
        isReadyShown = show;

        runOnUiThread(() -> {
            if (show) {
                tvGuide.setText("지금 촬영하세요");
            } else {
                tvGuide.setText("영수증을 프레임 안에 맞춰주세요");
            }
        });
    }

    private HintType evaluateFrameQuality(@NonNull ImageProxy imageProxy) {
        try {
            ImageProxy.PlaneProxy planeY = imageProxy.getPlanes()[0];
            ByteBuffer buffer = planeY.getBuffer();
            int width = imageProxy.getWidth();
            int height = imageProxy.getHeight();
            int rowStride = planeY.getRowStride();

            final int step = 4;

            long sum = 0;
            long cnt = 0;
            long brightCount = 0;
            long darkCount = 0;
            long edgeScore = 0;

            byte[] row = new byte[rowStride];

            for (int y = 0; y < height; y += step) {
                int rowPos = y * rowStride;
                if (rowPos >= buffer.limit()) break;

                buffer.position(rowPos);
                int read = Math.min(rowStride, buffer.remaining());
                buffer.get(row, 0, read);

                int prev = -1;
                for (int x = 0; x < width; x += step) {
                    if (x >= read) break;

                    int v = row[x] & 0xFF;

                    sum += v;
                    cnt++;

                    if (v >= 245) brightCount++;
                    if (v <= 40) darkCount++;

                    if (prev >= 0) edgeScore += Math.abs(v - prev);
                    prev = v;
                }
            }

            if (cnt == 0) return HintType.NONE;

            double mean = (double) sum / (double) cnt;
            double brightRatio = (double) brightCount / (double) cnt;
            double darkRatio = (double) darkCount / (double) cnt;

            if (edgeScore < 120000) {
                return HintType.BLUR_SHAKE;
            }

            if (mean < 70 || darkRatio > 0.25) {
                return HintType.TOO_DARK;
            }

            if (mean > 185 || brightRatio > 0.12) {
                return HintType.TOO_BRIGHT;
            }

            return HintType.NONE;

        } catch (Exception ignore) {
            return HintType.NONE;
        }
    }

    private HintType evaluateOcrCoverageHint(Text result) {
        try {
            if (result == null) return HintType.NONE;
            List<Text.TextBlock> blocks = result.getTextBlocks();
            if (blocks == null || blocks.isEmpty()) return HintType.NONE;

            int minL = Integer.MAX_VALUE, minT = Integer.MAX_VALUE;
            int maxR = Integer.MIN_VALUE, maxB = Integer.MIN_VALUE;

            for (Text.TextBlock b : blocks) {
                if (b.getBoundingBox() == null) continue;
                minL = Math.min(minL, b.getBoundingBox().left);
                minT = Math.min(minT, b.getBoundingBox().top);
                maxR = Math.max(maxR, b.getBoundingBox().right);
                maxB = Math.max(maxB, b.getBoundingBox().bottom);
            }

            if (minL == Integer.MAX_VALUE) return HintType.NONE;

            int bw = Math.max(1, maxR - minL);
            int bh = Math.max(1, maxB - minT);

            double area = (double) bw * (double) bh;
            double full = (double) imageWidthHint() * (double) imageHeightHint();
            double ratio = area / full;

            if (ratio < 0.10) return HintType.TOO_FAR;
            return HintType.NONE;

        } catch (Exception ignore) {
            return HintType.NONE;
        }
    }

    private int imageWidthHint() {
        int w = previewView.getWidth();
        return (w > 0) ? w : 1000;
    }

    private int imageHeightHint() {
        int h = previewView.getHeight();
        return (h > 0) ? h : 1600;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraExecutor.shutdown();
        try {
            recognizer.close();
        } catch (Exception ignored) {
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] results) {
        super.onRequestPermissionsResult(requestCode, permissions, results);

        if (requestCode == REQ_CAMERA &&
                results.length > 0 &&
                results[0] == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            finish();
        }
    }
}