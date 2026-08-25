package com.mohitt.camverz;

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
import com.mohitt.camverz.api.ApiClient;
import com.mohitt.camverz.api.ApiService;

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

        // Setup theme selectors for text backgrounds
        themePurple.setOnClickListener(v -> setCanvasTheme("bg_community_hot_gradient"));
        themeAmber.setOnClickListener(v -> setCanvasTheme("bg_neon_amber_button"));
        themeCyan.setOnClickListener(v -> setCanvasTheme("cyan"));
        themeDark.setOnClickListener(v -> setCanvasTheme("dark"));

        btnPublish.setOnClickListener(v -> publishStory());

        // Initialize state
        switchType("TEXT");
    }

    private void switchType(String type) {
        currentType = type;
        selectedFileUri = null;
        ivMediaPreview.setVisibility(View.GONE);
        vvMediaPreview.setVisibility(View.GONE);
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
            if (currentType.equals("IMAGE")) {
                ivMediaPreview.setVisibility(View.VISIBLE);
                vvMediaPreview.setVisibility(View.GONE);
                Glide.with(this).load(selectedFileUri).into(ivMediaPreview);
            } else {
                ivMediaPreview.setVisibility(View.GONE);
                vvMediaPreview.setVisibility(View.VISIBLE);
                vvMediaPreview.setVideoURI(selectedFileUri);
                vvMediaPreview.setOnPreparedListener(mp -> {
                    mp.setLooping(true);
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
