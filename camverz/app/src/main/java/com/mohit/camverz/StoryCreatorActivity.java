package com.mohit.camverz;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.google.gson.JsonObject;
import com.mohit.camverz.api.ApiClient;
import com.mohit.camverz.api.ApiService;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class StoryCreatorActivity extends BaseActivity {

    private TextView btnBack;
    private Button btnPublish;
    private FrameLayout creatorCanvas;
    private ImageView ivMediaPreview;
    private VideoView vvMediaPreview;
    private EditText etStoryText;

    private Button btnTypeText, btnTypeImage, btnTypeVideo;
    private LinearLayout textOptionsLayout;
    private Button btnSelectFile;

    private View themePurple, themeAmber, themeCyan, themeDark;

    private String currentType = "TEXT"; // TEXT, IMAGE, VIDEO
    private Uri selectedFileUri = null;
    private String selectedThemeGradient = "bg_floating_glass";
    private String selectedTextColor = "#FFFFFF";

    private ApiService api;

    private static final int RC_PICK_FILE = 1001;

    private TextView btnToggleScale;
    private boolean isCropFill = true;

    // Gesture Matrix fields for Image Preview
    private android.graphics.Matrix imageMatrix = new android.graphics.Matrix();
    private android.graphics.Matrix savedImageMatrix = new android.graphics.Matrix();
    private static final int GESTURE_NONE = 0;
    private static final int GESTURE_DRAG = 1;
    private static final int GESTURE_ZOOM = 2;
    private int gestureMode = GESTURE_NONE;
    private android.graphics.PointF startTouchPoint = new android.graphics.PointF();
    private android.graphics.PointF midTouchPoint = new android.graphics.PointF();
    private float oldTouchDist = 1f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_story_creator);

        api = ApiClient.getInstance(this).getApi();

        btnBack = findViewById(R.id.btnBack);
        btnPublish = findViewById(R.id.btnPublish);
        creatorCanvas = findViewById(R.id.creatorCanvas);
        ivMediaPreview = findViewById(R.id.ivMediaPreview);
        vvMediaPreview = findViewById(R.id.vvMediaPreview);
        etStoryText = findViewById(R.id.etStoryText);
        btnToggleScale = findViewById(R.id.btnToggleScale);

        btnTypeText = findViewById(R.id.btnTypeText);
        btnTypeImage = findViewById(R.id.btnTypeImage);
        btnTypeVideo = findViewById(R.id.btnTypeVideo);
        textOptionsLayout = findViewById(R.id.textOptionsLayout);
        btnSelectFile = findViewById(R.id.btnSelectFile);

        themePurple = findViewById(R.id.themePurple);
        themeAmber = findViewById(R.id.themeAmber);
        themeCyan = findViewById(R.id.themeCyan);
        themeDark = findViewById(R.id.themeDark);

        btnBack.setOnClickListener(v -> finish());

        btnTypeText.setOnClickListener(v -> switchType("TEXT"));
        btnTypeImage.setOnClickListener(v -> switchType("IMAGE"));
        btnTypeVideo.setOnClickListener(v -> switchType("VIDEO"));

        btnSelectFile.setOnClickListener(v -> selectFileFromGallery());
        btnToggleScale.setOnClickListener(v -> toggleScaleMode());

        // Setup theme selectors for text backgrounds
        themePurple.setOnClickListener(v -> setCanvasTheme("bg_community_hot_gradient"));
        themeAmber.setOnClickListener(v -> setCanvasTheme("bg_neon_amber_button"));
        themeCyan.setOnClickListener(v -> setCanvasTheme("cyan"));
        themeDark.setOnClickListener(v -> setCanvasTheme("dark"));

        btnPosTop = findViewById(R.id.btnPosTop);
        btnPosCenter = findViewById(R.id.btnPosCenter);
        btnPosBottom = findViewById(R.id.btnPosBottom);

        if (btnPosTop != null) btnPosTop.setOnClickListener(v -> snapTextPosition("TOP"));
        if (btnPosCenter != null) btnPosCenter.setOnClickListener(v -> snapTextPosition("CENTER"));
        if (btnPosBottom != null) btnPosBottom.setOnClickListener(v -> snapTextPosition("BOTTOM"));

        btnPublish.setOnClickListener(v -> publishStory());

        applyWindowInsets(findViewById(R.id.headerBar), findViewById(R.id.controlsLayout));

        setupTouchGesture();
        setupDraggableText();

        // Initialize state
        switchType("TEXT");
    }

    private TextView btnPosTop, btnPosCenter, btnPosBottom;
    private float textDX, textDY;

    @android.annotation.SuppressLint("ClickableViewAccessibility")
    private void setupDraggableText() {
        etStoryText.setOnTouchListener((v, event) -> {
            switch (event.getActionMasked()) {
                case android.view.MotionEvent.ACTION_DOWN:
                    textDX = v.getX() - event.getRawX();
                    textDY = v.getY() - event.getRawY();
                    break;
                case android.view.MotionEvent.ACTION_MOVE:
                    float newX = event.getRawX() + textDX;
                    float newY = event.getRawY() + textDY;
                    float maxX = Math.max(0, creatorCanvas.getWidth() - v.getWidth());
                    float maxY = Math.max(0, creatorCanvas.getHeight() - v.getHeight());
                    newX = Math.max(0, Math.min(newX, maxX));
                    newY = Math.max(0, Math.min(newY, maxY));
                    v.setX(newX);
                    v.setY(newY);
                    break;
            }
            return false;
        });
    }

    private void snapTextPosition(String position) {
        if (etStoryText == null || creatorCanvas == null) return;
        creatorCanvas.post(() -> {
            float canvasWidth = creatorCanvas.getWidth();
            float canvasHeight = creatorCanvas.getHeight();
            float textWidth = etStoryText.getWidth();
            float textHeight = etStoryText.getHeight();
            float centerX = Math.max(0, (canvasWidth - textWidth) / 2f);

            if ("TOP".equals(position)) {
                etStoryText.setX(centerX);
                etStoryText.setY(30f);
            } else if ("CENTER".equals(position)) {
                etStoryText.setX(centerX);
                etStoryText.setY(Math.max(0, (canvasHeight - textHeight) / 2f));
            } else if ("BOTTOM".equals(position)) {
                etStoryText.setX(centerX);
                etStoryText.setY(Math.max(0, canvasHeight - textHeight - 40f));
            }
        });
    }

    private void toggleScaleMode() {
        isCropFill = !isCropFill;
        if (currentType.equals("IMAGE")) {
            if (isCropFill) {
                ivMediaPreview.setScaleType(ImageView.ScaleType.CENTER_CROP);
                btnToggleScale.setText("↕️ Fill (Crop)");
            } else {
                ivMediaPreview.setScaleType(ImageView.ScaleType.FIT_CENTER);
                btnToggleScale.setText("↔️ Fit (Full)");
            }
        } else if (currentType.equals("VIDEO")) {
            FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) vvMediaPreview.getLayoutParams();
            if (isCropFill) {
                lp.width = FrameLayout.LayoutParams.MATCH_PARENT;
                lp.height = FrameLayout.LayoutParams.MATCH_PARENT;
                btnToggleScale.setText("↕️ Fill (Crop)");
            } else {
                lp.width = FrameLayout.LayoutParams.WRAP_CONTENT;
                lp.height = FrameLayout.LayoutParams.WRAP_CONTENT;
                lp.gravity = android.view.Gravity.CENTER;
                btnToggleScale.setText("↔️ Fit (Full)");
            }
            vvMediaPreview.setLayoutParams(lp);
        }
    }

    @android.annotation.SuppressLint("ClickableViewAccessibility")
    private void setupTouchGesture() {
        ivMediaPreview.setOnTouchListener((v, event) -> {
            ImageView view = (ImageView) v;
            if (!isCropFill && view.getScaleType() != ImageView.ScaleType.MATRIX) {
                view.setScaleType(ImageView.ScaleType.MATRIX);
                imageMatrix.set(view.getImageMatrix());
            }
            if (view.getScaleType() != ImageView.ScaleType.MATRIX) {
                return false;
            }
            switch (event.getAction() & android.view.MotionEvent.ACTION_MASK) {
                case android.view.MotionEvent.ACTION_DOWN:
                    savedImageMatrix.set(imageMatrix);
                    startTouchPoint.set(event.getX(), event.getY());
                    gestureMode = GESTURE_DRAG;
                    break;
                case android.view.MotionEvent.ACTION_POINTER_DOWN:
                    oldTouchDist = calculateDistance(event);
                    if (oldTouchDist > 10f) {
                        savedImageMatrix.set(imageMatrix);
                        calculateMidPoint(midTouchPoint, event);
                        gestureMode = GESTURE_ZOOM;
                    }
                    break;
                case android.view.MotionEvent.ACTION_UP:
                case android.view.MotionEvent.ACTION_POINTER_UP:
                    gestureMode = GESTURE_NONE;
                    break;
                case android.view.MotionEvent.ACTION_MOVE:
                    if (gestureMode == GESTURE_DRAG) {
                        imageMatrix.set(savedImageMatrix);
                        imageMatrix.postTranslate(event.getX() - startTouchPoint.x, event.getY() - startTouchPoint.y);
                    } else if (gestureMode == GESTURE_ZOOM) {
                        float newDist = calculateDistance(event);
                        if (newDist > 10f) {
                            imageMatrix.set(savedImageMatrix);
                            float scale = newDist / oldTouchDist;
                            imageMatrix.postScale(scale, scale, midTouchPoint.x, midTouchPoint.y);
                        }
                    }
                    break;
            }
            view.setImageMatrix(imageMatrix);
            return true;
        });
    }

    private float calculateDistance(android.view.MotionEvent event) {
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return (float) Math.sqrt(x * x + y * y);
    }

    private void calculateMidPoint(android.graphics.PointF point, android.view.MotionEvent event) {
        float x = event.getX(0) + event.getX(1);
        float y = event.getY(0) + event.getY(1);
        point.set(x / 2, y / 2);
    }

    private void switchType(String type) {
        currentType = type;
        selectedFileUri = null;
        ivMediaPreview.setVisibility(View.GONE);
        vvMediaPreview.setVisibility(View.GONE);
        btnToggleScale.setVisibility(View.GONE);
        etStoryText.setText("");

        // Highlight selected selector chip
        btnTypeText.setBackgroundResource(type.equals("TEXT") ? R.drawable.bg_luxury_chip : R.drawable.bg_luxury_pill_dark);
        btnTypeImage.setBackgroundResource(type.equals("IMAGE") ? R.drawable.bg_luxury_chip : R.drawable.bg_luxury_pill_dark);
        btnTypeVideo.setBackgroundResource(type.equals("VIDEO") ? R.drawable.bg_luxury_chip : R.drawable.bg_luxury_pill_dark);

        if (type.equals("TEXT")) {
            textOptionsLayout.setVisibility(View.VISIBLE);
            btnSelectFile.setVisibility(View.GONE);
            setCanvasTheme("bg_community_hot_gradient");
            etStoryText.setHint("Type text story...");
        } else {
            textOptionsLayout.setVisibility(View.GONE);
            btnSelectFile.setVisibility(View.VISIBLE);
            creatorCanvas.setBackgroundResource(R.drawable.bg_floating_glass);
            etStoryText.setHint("Add optional overlay text...");
            btnSelectFile.setText(type.equals("IMAGE") ? "🖼️ Select Image from Gallery" : "🎥 Select Video from Gallery");
        }
    }

    private void setCanvasTheme(String theme) {
        selectedThemeGradient = theme;
        if (theme.equals("bg_community_hot_gradient")) {
            creatorCanvas.setBackgroundResource(R.drawable.bg_community_hot_gradient);
            selectedTextColor = "#FFFFFF";
            etStoryText.setTextColor(Color.WHITE);
        } else if (theme.equals("bg_neon_amber_button")) {
            creatorCanvas.setBackgroundResource(R.drawable.bg_neon_amber_button);
            selectedTextColor = "#000000";
            etStoryText.setTextColor(Color.BLACK);
        } else if (theme.equals("cyan")) {
            creatorCanvas.setBackgroundColor(Color.parseColor("#00E5FF"));
            selectedTextColor = "#000000";
            etStoryText.setTextColor(Color.BLACK);
        } else if (theme.equals("dark")) {
            creatorCanvas.setBackgroundColor(Color.parseColor("#1A1A1A"));
            selectedTextColor = "#FFFFFF";
            etStoryText.setTextColor(Color.WHITE);
        }
    }

    private void selectFileFromGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        if (currentType.equals("IMAGE")) {
            intent.setType("image/*");
        } else {
            intent.setType("video/*");
        }
        startActivityForResult(intent, RC_PICK_FILE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_PICK_FILE && resultCode == RESULT_OK && data != null && data.getData() != null) {
            selectedFileUri = data.getData();
            btnToggleScale.setVisibility(View.VISIBLE);
            if (currentType.equals("IMAGE")) {
                ivMediaPreview.setVisibility(View.VISIBLE);
                vvMediaPreview.setVisibility(View.GONE);
                ivMediaPreview.setScaleType(ImageView.ScaleType.FIT_CENTER);
                isCropFill = false;
                btnToggleScale.setText("↔️ Fit (Full)");
                Glide.with(this).load(selectedFileUri).into(ivMediaPreview);
            } else {
                ivMediaPreview.setVisibility(View.GONE);
                vvMediaPreview.setVisibility(View.VISIBLE);
                isCropFill = false;
                btnToggleScale.setText("↔️ Fit (Full)");
                vvMediaPreview.setVideoURI(selectedFileUri);
                vvMediaPreview.setOnPreparedListener(mp -> {
                    mp.setLooping(true);
                    int videoWidth = mp.getVideoWidth();
                    int videoHeight = mp.getVideoHeight();
                    if (videoWidth > 0 && videoHeight > 0) {
                        float videoAspect = (float) videoWidth / videoHeight;
                        int containerWidth = creatorCanvas.getWidth();
                        int containerHeight = creatorCanvas.getHeight();
                        if (containerWidth > 0 && containerHeight > 0) {
                            FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) vvMediaPreview.getLayoutParams();
                            float containerAspect = (float) containerWidth / containerHeight;
                            if (videoAspect > containerAspect) {
                                lp.width = containerWidth;
                                lp.height = (int) (containerWidth / videoAspect);
                            } else {
                                lp.height = containerHeight;
                                lp.width = (int) (containerHeight * videoAspect);
                            }
                            lp.gravity = android.view.Gravity.CENTER;
                            vvMediaPreview.setLayoutParams(lp);
                        }
                    }
                    vvMediaPreview.start();
                });
            }
        }
    }

    private void publishStory() {
        String text = etStoryText.getText().toString().trim();

        if (currentType.equals("TEXT") && text.isEmpty()) {
            Toast.makeText(this, "Please enter some text for your story!", Toast.LENGTH_SHORT).show();
            return;
        }

        if ((currentType.equals("IMAGE") || currentType.equals("VIDEO")) && selectedFileUri == null) {
            Toast.makeText(this, "Please select a media file from gallery!", Toast.LENGTH_SHORT).show();
            return;
        }

        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Uploading story...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        if (currentType.equals("TEXT")) {
            // Text Story: Upload via JSON
            Map<String, Object> body = new HashMap<>();
            body.put("type", "TEXT");
            body.put("textContent", text);
            body.put("textColor", selectedTextColor);
            body.put("bgGradient", selectedThemeGradient);

            api.uploadTextStory(body).enqueue(new Callback<JsonObject>() {
                @Override
                public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                    progressDialog.dismiss();
                    if (response.isSuccessful()) {
                        Toast.makeText(StoryCreatorActivity.this, "🎉 Story published successfully!", Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        Toast.makeText(StoryCreatorActivity.this, "Failed to upload story", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<JsonObject> call, Throwable t) {
                    progressDialog.dismiss();
                    Toast.makeText(StoryCreatorActivity.this, "Network error", Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            // Media Story: Upload via Multipart
            File file = getFileFromUri(selectedFileUri);
            if (file == null) {
                progressDialog.dismiss();
                Toast.makeText(this, "Failed to resolve media file", Toast.LENGTH_SHORT).show();
                return;
            }

            String mimeType = getContentResolver().getType(selectedFileUri);
            if (mimeType == null) {
                mimeType = currentType.equals("IMAGE") ? "image/jpeg" : "video/mp4";
            }

            RequestBody requestFile = RequestBody.create(MediaType.parse(mimeType), file);
            MultipartBody.Part bodyMedia = MultipartBody.Part.createFormData("media", file.getName(), requestFile);

            RequestBody requestType = RequestBody.create(MediaType.parse("text/plain"), currentType);
            RequestBody requestText = RequestBody.create(MediaType.parse("text/plain"), text);
            RequestBody requestColor = RequestBody.create(MediaType.parse("text/plain"), "#FFFFFF");
            RequestBody requestTheme = RequestBody.create(MediaType.parse("text/plain"), "");

            api.uploadMediaStory(bodyMedia, requestType, requestText, requestColor, requestTheme).enqueue(new Callback<JsonObject>() {
                @Override
                public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                    progressDialog.dismiss();
                    // Delete cached file
                    try { file.delete(); } catch(Exception e){}

                    if (response.isSuccessful()) {
                        Toast.makeText(StoryCreatorActivity.this, "🎉 Story published successfully!", Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        Toast.makeText(StoryCreatorActivity.this, "Failed to upload story", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<JsonObject> call, Throwable t) {
                    progressDialog.dismiss();
                    try { file.delete(); } catch(Exception e){}
                    Toast.makeText(StoryCreatorActivity.this, "Network error", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private File getFileFromUri(Uri uri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            if (inputStream == null) return null;
            File file = new File(getCacheDir(), "upload_temp_media");
            FileOutputStream outputStream = new FileOutputStream(file);
            byte[] buffer = new byte[4096];
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, read);
            }
            outputStream.flush();
            outputStream.close();
            inputStream.close();
            return file;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
