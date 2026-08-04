package com.mohitt.camverz;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.content.Intent;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.core.app.ActivityCompat;
import androidx.lifecycle.LifecycleOwner;

import com.google.common.util.concurrent.ListenableFuture;
import com.mohitt.camverz.api.ApiClient;
import com.mohitt.camverz.api.ApiService;
import com.mohitt.camverz.api.TokenManager;
import retrofit2.Call;
import retrofit2.Callback;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import android.media.ExifInterface;
import android.graphics.Matrix;
import android.os.Environment;
import java.io.FileOutputStream;
import android.content.ContentValues;
import android.provider.MediaStore;
import java.io.ByteArrayOutputStream;

import org.json.JSONObject;
import com.google.gson.JsonObject;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;

public class VerificationActivity extends BaseActivity {

    private static final String TAG = "Liveness";
    private static final int COUNTDOWN = 3;

    private PreviewView cameraPreview;
    private TextView instructionText;
    private ImageView arrowImage;
    private TextView countdownText;
    private LinearLayout countdownContainer;
    private View step1;
    private FrameLayout instructionsContainer;
    private FrameLayout resultsContainer;
    private ImageView imageFront;
    private Button submitButton, retakeButton, verifyGenderButton;
    private OvalFrameView ovalFrame;

    private FaceDetector faceDetector;
    private ImageCapture imageCapture;
    private Camera camera; // Reference to control torch
    private ExecutorService cameraExecutor;
    private Handler mainHandler;

    private long stableStartTime = 0;
    private static final long STABILITY_THRESHOLD = 1000; // 1 second in milliseconds
    private Runnable countdownRunnable = null;

    private enum VerificationStep {
        FRONT(0, "LOOK AT CAMERA - CAPTURE YOUR FACE");

        final int index;
        final String instruction;

        VerificationStep(int index, String instruction) {
            this.index = index;
            this.instruction = instruction;
        }
    }

    private VerificationStep currentStep = VerificationStep.FRONT;
    private boolean faceDetected = false;
    private boolean countdownInProgress = false;
    private boolean isReadyToCountdown = false; // Flag to prevent countdown until popup is dismissed
    private ArrayList<Bitmap> capturedFrames = new ArrayList<>();

    // Clean session-based approach (Python style)
    private VerificationSession verificationSession;

    private ApiService api;
    private TokenManager tokenManager;

    // Gender verification - AWS Rekognition
    private String selectedGender = ""; // Will be fetched from Firebase
    private boolean isGenderVerified = false; // Track if gender verification passed

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verification);

        // ✅ Request camera permission before proceeding
        requestCameraPermission();

        initializeUI();
        initializeApi();
        initializeFaceDetector();
        fetchSelectedGender();

        // Initialize clean verification session (Python style)
        File sessionDir = new File(getExternalFilesDir(null), "verification_session");
        verificationSession = new VerificationSession(sessionDir);

        cameraExecutor = Executors.newSingleThreadExecutor();
        mainHandler = new Handler(Looper.getMainLooper());

        startCamera();

        // Show instructions popup
        showInstructionsPopup();
    }

    private void showInstructionsPopup() {
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle("Follow the Instructions")
                .setMessage(
                        "• Keep your face in the center\n" +
                        "• Hold still for live capture\n" +
                        "• Only one front image is needed\n\n" +
                        "🔒 Privacy Guarantee:\n" +
                        "This scan is only used for one-time verification. Your photo will never be shown to other users or saved on your profile.")
                .setPositiveButton("Let's Go", (dialog, which) -> {
                    isReadyToCountdown = true; // User dismissed popup - NOW allow countdown to start
                    dialog.dismiss();
                })
                .setNegativeButton("Cancel", (dialog, which) -> {
                    dialog.dismiss();
                    finish();
                })
                .setCancelable(false)
                .show();
    }

    private void initializeUI() {
        cameraPreview = findViewById(R.id.camera_preview);
        instructionText = findViewById(R.id.instruction_text);
        arrowImage = findViewById(R.id.arrow_image);
        countdownText = findViewById(R.id.countdown_text);
        countdownContainer = findViewById(R.id.countdown_container);
        step1 = findViewById(R.id.step_1);
        instructionsContainer = findViewById(R.id.instructions_container);
        resultsContainer = findViewById(R.id.results_container);
        imageFront = findViewById(R.id.image_front);
        submitButton = findViewById(R.id.submit_verification_button);
        retakeButton = findViewById(R.id.retake_button);
        verifyGenderButton = findViewById(R.id.verify_gender_button);
        ovalFrame = findViewById(R.id.oval_frame);

        // Set default cyan color for oval frame
        if (ovalFrame != null) {
            ovalFrame.setStrokeColor(0xFF00D9FF);
        }

        // Initialize button states
        submitButton.setEnabled(false);
        submitButton.setAlpha(0.5f);
        isGenderVerified = false;

        updateInstructionUI();
        updateProgressIndicators();

        findViewById(R.id.back_button).setOnClickListener(v -> finish());
        verifyGenderButton.setOnClickListener(v -> performGenderVerification());
        submitButton.setOnClickListener(v -> submitVerification());
        retakeButton.setOnClickListener(v -> restartVerification());
    }

    private void initializeApi() {
        api = ApiClient.getInstance(this).getApi();
        tokenManager = TokenManager.getInstance(this);
    }

    private void initializeFaceDetector() {
        faceDetector = FaceDetection.getClient();
        Log.d(TAG, "Face detector initialized");
    }

    @androidx.annotation.OptIn(markerClass = androidx.camera.core.ExperimentalGetImage.class)
    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(cameraPreview.getSurfaceProvider());

                imageCapture = new ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .setFlashMode(ImageCapture.FLASH_MODE_ON) // 📸 Always enable flash
                        .build();

                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();

                imageAnalysis.setAnalyzer(cameraExecutor, proxy -> {
                    try {
                        android.media.Image mediaImage = proxy.getImage();
                        int rotationDegrees = proxy.getImageInfo().getRotationDegrees();
                        InputImage image = InputImage.fromMediaImage(mediaImage, rotationDegrees);

                        int frameWidth = proxy.getWidth();
                        int frameHeight = proxy.getHeight();

                        faceDetector.process(image)
                                .addOnSuccessListener(faces -> {
                                    if (!faces.isEmpty()) {
                                        com.google.mlkit.vision.face.Face face = faces.get(0);

                                        boolean isInside = isFaceInsideOval(face, frameWidth, frameHeight);
                                        boolean isBigEnough = isFaceBigEnough(face, frameWidth, frameHeight);
                                        boolean isCorrectDir = isCorrectDirection(face);

                                        mainHandler.post(() -> {
                                            if (isInside && isBigEnough && isCorrectDir) {
                                                showGreenState();

                                                if (stableStartTime == 0) {
                                                    stableStartTime = System.currentTimeMillis();
                                                }

                                                if (System.currentTimeMillis()
                                                        - stableStartTime > STABILITY_THRESHOLD) {
                                                    if (!countdownInProgress && !faceDetected && isReadyToCountdown) {
                                                        faceDetected = true;
                                                        startCountdown();
                                                    }
                                                }
                                            } else {
                                                showRedState();
                                                stableStartTime = 0;

                                                // Cancel countdown if face becomes invalid during countdown
                                                if (countdownInProgress) {
                                                    cancelCountdown();
                                                } else {
                                                    countdownContainer.setVisibility(View.GONE);
                                                }
                                            }
                                        });
                                    } else {
                                        faceDetected = false;
                                        stableStartTime = 0;

                                        // Cancel countdown if face disappears during countdown
                                        if (countdownInProgress) {
                                            mainHandler.post(this::cancelCountdown);
                                        } else {
                                            mainHandler.post(() -> {
                                                countdownContainer.setVisibility(View.GONE);
                                                showRedState();
                                            });
                                        }
                                    }
                                })
                                .addOnFailureListener(e -> Log.e(TAG, "Face detection failed", e))
                                // 🔴 CRITICAL: Only close imageProxy after processing completes (success or
                                // failure)
                                .addOnCompleteListener(task -> {
                                    try {
                                        proxy.close();
                                    } catch (Exception e) {
                                        Log.e(TAG, "Error closing imageProxy", e);
                                    }
                                });
                    } catch (Exception e) {
                        Log.e(TAG, "Error analyzing frame", e);
                        // Close proxy if error occurs before async processing
                        try {
                            proxy.close();
                        } catch (Exception closeErr) {
                            Log.e(TAG, "Error closing proxy in catch block", closeErr);
                        }
                    }
                });

                CameraSelector cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA;

                cameraProvider.unbindAll();
                camera = cameraProvider.bindToLifecycle(
                        (LifecycleOwner) this,
                        cameraSelector,
                        preview,
                        imageCapture,
                        imageAnalysis);

                // 🔦 Enable torch (continuous flash) if device has flash
                if (camera.getCameraInfo().hasFlashUnit()) {
                    try {
                        camera.getCameraControl().enableTorch(true);
                        Log.d(TAG, "✅ Flash/Torch enabled for better image quality");
                    } catch (Exception e) {
                        Log.e(TAG, "Error enabling torch", e);
                    }
                }

                Log.d(TAG, "Camera started successfully");
            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "Error starting camera", e);
                Toast.makeText(this, "Camera initialization failed", Toast.LENGTH_SHORT).show();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void startCountdown() {
        if (countdownInProgress)
            return;
        countdownInProgress = true;
        countdownContainer.setVisibility(View.VISIBLE);

        for (int i = COUNTDOWN; i > 0; i--) {
            final int count = i;
            mainHandler.postDelayed(() -> {
                if (countdownText != null && countdownInProgress) {
                    countdownText.setText(String.valueOf(count));
                }
            }, (COUNTDOWN - i) * 1000L);
        }

        countdownRunnable = this::captureImage;
        mainHandler.postDelayed(countdownRunnable, COUNTDOWN * 1000L);
    }

    private void cancelCountdown() {
        if (!countdownInProgress)
            return;

        countdownInProgress = false;
        stableStartTime = 0;
        faceDetected = false;

        // Remove pending capture callback
        if (countdownRunnable != null) {
            mainHandler.removeCallbacks(countdownRunnable);
            countdownRunnable = null;
        }

        countdownContainer.setVisibility(View.GONE);
        showRedState();

        Log.d(TAG, "Countdown cancelled - face invalid");
    }

    /**
     * Apply EXIF rotation to bitmap (fixes rotated/sideways images)
     * Camera images have EXIF orientation metadata that BitmapFactory ignores
     */
    private Bitmap applyExifRotation(Bitmap bitmap, String imagePath) {
        try {
            ExifInterface exif = new ExifInterface(imagePath);
            int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);

            float rotationDegrees = 0f;
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    rotationDegrees = 90f;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    rotationDegrees = 180f;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    rotationDegrees = 270f;
                    break;
                case ExifInterface.ORIENTATION_NORMAL:
                default:
                    return bitmap; // No rotation needed
            }

            // Apply rotation matrix
            Matrix matrix = new Matrix();
            matrix.postRotate(rotationDegrees);
            Bitmap rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);

            // Recycle original if different
            if (rotated != bitmap) {
                bitmap.recycle();
            }

            Log.d(TAG, "✅ Applied EXIF rotation: " + rotationDegrees + "°");
            return rotated;
        } catch (Exception e) {
            Log.e(TAG, "Error applying EXIF rotation", e);
            return bitmap; // Return original if error
        }
    }

    private Bitmap decodeSampledBitmapFromFile(String pathName, int reqWidth, int reqHeight) {
        final android.graphics.BitmapFactory.Options options = new android.graphics.BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        android.graphics.BitmapFactory.decodeFile(pathName, options);

        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

        options.inJustDecodeBounds = false;
        return android.graphics.BitmapFactory.decodeFile(pathName, options);
    }

    private int calculateInSampleSize(android.graphics.BitmapFactory.Options options, int reqWidth, int reqHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }

    private void captureImage() {
        countdownContainer.setVisibility(View.GONE);
        countdownInProgress = false;
        stableStartTime = 0;
        faceDetected = false;
        countdownRunnable = null;

        // Reset oval frame and instruction text to default colors
        if (ovalFrame != null) {
            ovalFrame.setStrokeColor(0xFF00D9FF);
        }
        if (instructionText != null) {
            instructionText.setTextColor(0xFF00D9FF);
        }

        File outputDir = getCacheDir();
        File photoFile = new File(outputDir, "verification_" + System.currentTimeMillis() + ".jpg");

        ImageCapture.OutputFileOptions outputOptions = new ImageCapture.OutputFileOptions.Builder(photoFile)
                .build();

        imageCapture.takePicture(outputOptions, cameraExecutor, new ImageCapture.OnImageSavedCallback() {
            @Override
            public void onImageSaved(ImageCapture.OutputFileResults output) {
                Bitmap bitmap = decodeSampledBitmapFromFile(photoFile.getAbsolutePath(), 1080, 1080);
                if (bitmap != null) {
                    // Apply EXIF rotation to fix sideways/rotated images
                    bitmap = applyExifRotation(bitmap, photoFile.getAbsolutePath());

                    // Save to session based on current step (Python style)
                    if (currentStep == VerificationStep.FRONT) {
                        verificationSession.setFront(bitmap);
                        Log.d(TAG, "Captured FRONT image");
                    }

                    // Also add to display list
                    capturedFrames.add(bitmap);
                    Log.d(TAG, "Images captured: " + capturedFrames.size() + "/1");

                    if (verificationSession.getFront() != null) {
                        mainHandler.post(VerificationActivity.this::showResults);
                    } else {
                        mainHandler.post(VerificationActivity.this::moveToNextStep);
                    }
                }
                faceDetected = false;
            }

            @Override
            public void onError(ImageCaptureException exception) {
                Log.e(TAG, "Photo capture failed", exception);
                mainHandler.post(() -> {
                    Toast.makeText(VerificationActivity.this, "Failed to capture image. Try again.", Toast.LENGTH_SHORT)
                            .show();
                    faceDetected = false;
                });
            }
        });
    }

    private void moveToNextStep() {
        updateInstructionUI();
        updateProgressIndicators();
    }

    private void updateInstructionUI() {
        instructionText.setText(currentStep.instruction);
        rotateArrow();
    }

    private void rotateArrow() {
        arrowImage.setRotation(180);
    }

    private void updateProgressIndicators() {
        if (step1 != null) {
            step1.setBackground(ContextCompat.getDrawable(this, R.drawable.bg_neon_button_pill));
        }
    }

    private void showResults() {
        // Disable torch when showing results (camera not needed anymore)
        if (camera != null) {
            try {
                camera.getCameraControl().enableTorch(false);
            } catch (Exception e) {
                Log.e(TAG, "Error disabling torch", e);
            }
        }

        instructionsContainer.setVisibility(View.GONE);
        resultsContainer.setVisibility(View.VISIBLE);

        if (!capturedFrames.isEmpty()) {
            imageFront.setImageBitmap(capturedFrames.get(0));
            Log.d(TAG, "Results displayed");

            // 📸 Add long-press listeners to save images
            imageFront.setOnLongClickListener(v -> {
                saveBitmapToGallery(capturedFrames.get(0), "front");
                return true;
            });
        }
    }

    /**
     * Save bitmap to device gallery/Pictures folder
     * Accessible via long-press on the image
     */
    private void saveBitmapToGallery(Bitmap bitmap, String position) {
        try {
            String filename = "Camverz_" + position + "_" + System.currentTimeMillis() + ".jpg";

            // Use MediaStore for Android 10+, or direct file access for older versions
            ContentValues values = new ContentValues();
            values.put(MediaStore.Images.Media.DISPLAY_NAME, filename);
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
            values.put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES);

            android.net.Uri imageUri = this.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    values);

            if (imageUri != null) {
                FileOutputStream fos = (FileOutputStream) this.getContentResolver().openOutputStream(imageUri);
                if (fos != null) {
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 95, fos);
                    fos.close();

                    Log.d(TAG, "✅ Image saved: " + filename);
                    Toast.makeText(this, "✅ " + position + " image saved to Pictures", Toast.LENGTH_SHORT).show();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error saving image: " + e.getMessage());
            Toast.makeText(this, "❌ Failed to save image", Toast.LENGTH_SHORT).show();
        }
    }

    private void submitVerification() {
        // ✅ Gender verification already passed, just submit to Firebase
        if (!isGenderVerified) {
            Toast.makeText(this, "Please verify gender first", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> verificationData = new HashMap<>();
        verificationData.put("verificationStatus", "pending");
        verificationData.put("verificationDate", System.currentTimeMillis());
        verificationData.put("livenessVerified", true);
        verificationData.put("genderVerified", true);
        verificationData.put("verified", true); // ✅ Set verified badge to true

        api.updateMe(verificationData).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, retrofit2.Response<JsonObject> response) {
                if (response.isSuccessful() && response.body() != null && response.body().get("ok").getAsBoolean()) {
                    Toast.makeText(VerificationActivity.this,
                            "✅ Verification submitted successfully! Your profile is now verified.", Toast.LENGTH_SHORT)
                            .show();
                    setResult(RESULT_OK);
                    finish();
                } else {
                    Toast.makeText(VerificationActivity.this, "Failed to submit verification", Toast.LENGTH_SHORT)
                            .show();
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                Log.e(TAG, "Error submitting verification", t);
                Toast.makeText(VerificationActivity.this, "Failed to submit verification", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void restartVerification() {
        // Re-enable torch for retry
        if (camera != null) {
            try {
                camera.getCameraControl().enableTorch(true);
            } catch (Exception e) {
                Log.e(TAG, "Error re-enabling torch", e);
            }
        }

        // Clear session and delete files
        verificationSession.clear();
        verificationSession.deleteFiles();
        capturedFrames.clear();
        currentStep = VerificationStep.FRONT;
        updateInstructionUI();
        updateProgressIndicators();
        resultsContainer.setVisibility(View.GONE);
        instructionsContainer.setVisibility(View.VISIBLE);
        faceDetected = false;
        countdownInProgress = false;
        stableStartTime = 0;
        isReadyToCountdown = false; // Reset the flag - user must confirm popup again
        isGenderVerified = false; // Reset gender verification status

        // Reset button states
        submitButton.setEnabled(false);
        submitButton.setAlpha(0.5f);
        retakeButton.setEnabled(true); // ✅ Re-enable retake button
        retakeButton.setAlpha(1f);
        verifyGenderButton.setText("Verify Gender");
        verifyGenderButton.setEnabled(true);

        // Cancel any pending countdown
        if (countdownRunnable != null) {
            mainHandler.removeCallbacks(countdownRunnable);
            countdownRunnable = null;
        }

        // Reset oval frame to default cyan color
        if (ovalFrame != null) {
            ovalFrame.setStrokeColor(0xFF00D9FF);
        }

        // Reset instruction text color
        if (instructionText != null) {
            instructionText.setTextColor(0xFF00D9FF);
        }

        // Show instructions popup again
        showInstructionsPopup();
    }

    // ========== OVAL FRAME LOGIC ==========

    /**
     * Check if face center is inside the oval boundary
     */
    private boolean isFaceInsideOval(com.google.mlkit.vision.face.Face face, int previewWidth, int previewHeight) {
        float centerX = face.getBoundingBox().centerX();
        float centerY = face.getBoundingBox().centerY();

        float ovalCenterX = previewWidth / 2f;
        float ovalCenterY = previewHeight / 2f;

        float ovalRadiusX = previewWidth * 0.35f;
        float ovalRadiusY = previewHeight * 0.45f;

        // Ellipse equation: (x/a)² + (y/b)² <= 1
        float dx = (centerX - ovalCenterX) / ovalRadiusX;
        float dy = (centerY - ovalCenterY) / ovalRadiusY;

        boolean inside = (dx * dx + dy * dy) <= 1;
        Log.d(TAG, "Face center: (" + centerX + ", " + centerY + "), Inside oval: " + inside);
        return inside;
    }

    /**
     * Check if face size is sufficient (at least 25% of frame area)
     */
    private boolean isFaceBigEnough(com.google.mlkit.vision.face.Face face, int frameWidth, int frameHeight) {
        float faceWidth = face.getBoundingBox().width();
        float faceHeight = face.getBoundingBox().height();
        float faceArea = faceWidth * faceHeight;
        float frameArea = frameWidth * frameHeight;
        float widthRatio = faceWidth / frameWidth;
        float heightRatio = faceHeight / frameHeight;

        boolean bigEnough = (faceArea / frameArea) > 0.25f && widthRatio > 0.30f && heightRatio > 0.40f;
        Log.d(TAG, "Face area ratio: " + (faceArea / frameArea) + ", width ratio: " + widthRatio + ", height ratio: "
                + heightRatio + ", Big enough: " + bigEnough);
        return bigEnough;
    }

    /**
     * Check if face is looking in the correct direction
     * Accounts for front camera mirroring
     */
    private boolean isCorrectDirection(com.google.mlkit.vision.face.Face face) {
        float yaw = -face.getHeadEulerAngleY(); // Negate for mirrored front camera
        float pitch = face.getHeadEulerAngleX();
        boolean frontOk = Math.abs(yaw) < 20 && Math.abs(pitch) < 20;
        Log.d(TAG, "FRONT check - yaw: " + yaw + ", pitch: " + pitch + ", OK: " + frontOk);
        return frontOk;
    }

    /**
     * Show green feedback - face is in correct position
     */
    private void showGreenState() {
        if (ovalFrame != null) {
            ovalFrame.setStrokeColor(0xFF00FF00); // Green
        }
        if (instructionText != null) {
            instructionText.setTextColor(0xFF00FF00); // Green
        }
    }

    /**
     * Show red feedback - face is not in correct position
     */
    private void showRedState() {
        if (ovalFrame != null) {
            ovalFrame.setStrokeColor(0xFFFF0000); // Red
        }
        if (instructionText != null) {
            instructionText.setTextColor(0xFFFF0000); // Red
        }
    }

    // ========== GENDER VERIFICATION LOGIC ==========

    // ✅ Request camera permission with simple popup (only Allow button)
    private void requestCameraPermission() {
        if (ContextCompat.checkSelfPermission(this,
                android.Manifest.permission.CAMERA) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            // Permission not granted - show popup and request
            androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
            builder.setTitle("Camera Permission Required")
                    .setMessage(
                            "We need camera access to verify your identity. Please allow camera permission to proceed.")
                    .setPositiveButton("Allow", (dialog, which) -> {
                        dialog.dismiss();
                        // Request permission
                        ActivityCompat.requestPermissions(this,
                                new String[] { android.Manifest.permission.CAMERA },
                                100);
                    })
                    .setCancelable(false)
                    .show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 100) {
            if (grantResults.length > 0 && grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "✅ Camera permission granted");
            } else {
                Log.e(TAG, "❌ Camera permission denied");
                Toast.makeText(this, "Camera permission is required for verification", Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

    private void fetchSelectedGender() {
        api.getMe().enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, retrofit2.Response<JsonObject> response) {
                if (response.isSuccessful() && response.body() != null) {
                    JsonObject data = response.body();
                    if (data.has("ok") && data.get("ok").getAsBoolean() && data.has("user")) {
                        JsonObject user = data.getAsJsonObject("user");
                        if (user.has("gender") && !user.get("gender").isJsonNull()) {
                            selectedGender = user.get("gender").getAsString().toLowerCase().trim();
                            Log.d(TAG, "Selected gender fetched: " + selectedGender);
                        } else {
                            Log.w(TAG, "Gender not set in profile");
                            selectedGender = "";
                        }
                    }
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                Log.e(TAG, "Error fetching selected gender", t);
                Toast.makeText(VerificationActivity.this, "Failed to fetch profile info", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Handle "Verify Gender" button click
     * Performs gender verification using captured front image
     */
    private void performGenderVerification() {
        if ("Change Your Gender".equalsIgnoreCase(verifyGenderButton.getText().toString().trim())) {
            Intent intent = new Intent(this, ProfileActivity.class);
            intent.putExtra("userId", tokenManager.getUserId());
            startActivity(intent);
            finish();
            return;
        }

        if (selectedGender == null || selectedGender.isEmpty()) {
            Toast.makeText(this, "Selected gender not found in profile", Toast.LENGTH_SHORT).show();
            return;
        }

        if ("male".equalsIgnoreCase(selectedGender)) {
            isGenderVerified = true;
            submitButton.setEnabled(true);
            submitButton.setAlpha(1f);
            retakeButton.setEnabled(false);
            retakeButton.setAlpha(0.5f);
            verifyGenderButton.setText("Auto-Verified ✓");
            verifyGenderButton.setEnabled(false);
            Toast.makeText(this, "Male profile auto-verified", Toast.LENGTH_SHORT).show();
            return;
        }

        Bitmap frontImage = verificationSession.getFront();
        if (frontImage == null) {
            Toast.makeText(this, "Please capture front image first", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!tokenManager.isLoggedIn()) {
            Toast.makeText(this, "Login required", Toast.LENGTH_SHORT).show();
            return;
        }

        verifyGenderButton.setEnabled(false);
        verifyGenderButton.setText("Verifying...");

        new Thread(() -> {
            try {
                byte[] imageBytes = createCompressedThumbnail(frontImage);
                RequestBody fileBody = RequestBody.create(MediaType.parse("image/jpeg"), imageBytes);
                MultipartBody.Part imagePart = MultipartBody.Part.createFormData("image", "front.jpg", fileBody);

                api.verifyGender(imagePart).enqueue(new Callback<JsonObject>() {
                    @Override
                    public void onResponse(Call<JsonObject> call, retrofit2.Response<JsonObject> response) {
                        mainHandler.post(() -> {
                            if (!response.isSuccessful() || response.body() == null) {
                                String errorMsg = "Verification failed";
                                try {
                                    if (response.errorBody() != null) {
                                        String errorJson = response.errorBody().string();
                                        com.google.gson.JsonObject errObj = com.google.gson.JsonParser.parseString(errorJson).getAsJsonObject();
                                        if (errObj.has("error")) {
                                            errorMsg = errObj.get("error").getAsString();
                                        }
                                    }
                                } catch (Exception ignored) { }
                                
                                verifyGenderButton.setEnabled(true);
                                verifyGenderButton.setText("Verify Gender");
                                Toast.makeText(VerificationActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                                return;
                            }

                            JsonObject json = response.body();
                            boolean ok = json.has("ok") && json.get("ok").getAsBoolean();
                            boolean verified = json.has("verified") && json.get("verified").getAsBoolean();
                            String error = json.has("error") ? json.get("error").getAsString() : "Verification failed";
                            String detectedGender = json.has("detectedGender")
                                    ? json.get("detectedGender").getAsString()
                                    : "unknown";
                            double confidence = json.has("confidence") ? json.get("confidence").getAsDouble() : 0.0;

                            if (!ok) {
                                verifyGenderButton.setEnabled(true);
                                verifyGenderButton.setText("Verify Gender");
                                Toast.makeText(VerificationActivity.this, error, Toast.LENGTH_LONG).show();
                                return;
                            }

                            if (verified) {
                                isGenderVerified = true;
                                submitButton.setEnabled(true);
                                submitButton.setAlpha(1f);
                                retakeButton.setEnabled(false);
                                retakeButton.setAlpha(0.5f);
                                verifyGenderButton.setText("Verified ✓");
                                verifyGenderButton.setEnabled(false);
                                Toast.makeText(
                                        VerificationActivity.this,
                                        "✅ Verified (" + detectedGender + ", " + String.format("%.1f", confidence)
                                                + "%)",
                                        Toast.LENGTH_SHORT).show();
                            } else {
                                isGenderVerified = false;
                                submitButton.setEnabled(false);
                                submitButton.setAlpha(0.5f);
                                verifyGenderButton.setEnabled(true);
                                verifyGenderButton.setText("Change Your Gender");
                                
                                String errorMsg = "Verification failed. Please retake.";
                                if ("female".equalsIgnoreCase(selectedGender) && "male".equalsIgnoreCase(detectedGender)) {
                                    errorMsg = "We detected you as a boy.";
                                }
                                Toast.makeText(VerificationActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                            }
                        });
                    }

                    @Override
                    public void onFailure(Call<JsonObject> call, Throwable t) {
                        mainHandler.post(() -> {
                            verifyGenderButton.setEnabled(true);
                            verifyGenderButton.setText("Verify Gender");
                            Toast.makeText(VerificationActivity.this, "Verification error: " + t.getMessage(),
                                    Toast.LENGTH_SHORT).show();
                        });
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Error calling backend verification", e);
                mainHandler.post(() -> {
                    verifyGenderButton.setEnabled(true);
                    verifyGenderButton.setText("Verify Gender");
                    Toast.makeText(VerificationActivity.this, "Verification error: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    private byte[] createCompressedThumbnail(Bitmap source) {
        int maxSize = 400;
        int width = source.getWidth();
        int height = source.getHeight();
        float scale = Math.min((float) maxSize / width, (float) maxSize / height);
        if (scale > 1f) {
            scale = 1f;
        }

        int newWidth = Math.max(1, Math.round(width * scale));
        int newHeight = Math.max(1, Math.round(height * scale));
        Bitmap resized = Bitmap.createScaledBitmap(source, newWidth, newHeight, true);

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        resized.compress(Bitmap.CompressFormat.JPEG, 65, output);

        if (resized != source) {
            resized.recycle();
        }

        return output.toByteArray();
    }

    private void showSwitchGenderDialog() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Change Gender to Male?")
                .setMessage("We detected your face as Male. Would you like to update your profile gender to Male?\n\n⚠️ WARNING: Once updated, your profile will be auto-verified as Male and you will NOT be able to change your gender again.")
                .setPositiveButton("Change to Male", (dialog, which) -> {
                    dialog.dismiss();
                    updateProfileToMale();
                })
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void updateProfileToMale() {
        Map<String, Object> updateData = new HashMap<>();
        updateData.put("gender", "male");
        updateData.put("verified", true);
        updateData.put("verificationStatus", "approved");

        api.updateMe(updateData).enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, retrofit2.Response<JsonObject> response) {
                if (response.isSuccessful() && response.body() != null && response.body().get("ok").getAsBoolean()) {
                    Toast.makeText(VerificationActivity.this, "Profile updated to Male successfully!", Toast.LENGTH_SHORT).show();
                    
                    // Update locally saved user info in TokenManager
                    String userId = tokenManager.getUserId();
                    String name = tokenManager.getUserName();
                    String email = tokenManager.getUserEmail();
                    String avatar = tokenManager.getUserAvatar();
                    tokenManager.saveUser(userId, name, email, "male", avatar, true);
                    
                    setResult(RESULT_OK);
                    finish();
                } else {
                    Toast.makeText(VerificationActivity.this, "Failed to update profile gender", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                Toast.makeText(VerificationActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Disable torch to save battery
        if (camera != null) {
            try {
                camera.getCameraControl().enableTorch(false);
                Log.d(TAG, "🔦 Torch disabled");
            } catch (Exception e) {
                Log.e(TAG, "Error disabling torch", e);
            }
        }

        // Cancel any pending countdown
        if (countdownRunnable != null) {
            mainHandler.removeCallbacks(countdownRunnable);
            countdownRunnable = null;
        }

        if (faceDetector != null) {
            try {
                faceDetector.close();
            } catch (Exception e) {
                Log.e(TAG, "Error closing face detector", e);
            }
        }

        cameraExecutor.shutdown();
    }
}