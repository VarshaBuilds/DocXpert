package com.example.docxpert.features.compressimage;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.docxpert.R;
import com.google.android.material.progressindicator.CircularProgressIndicator;

public class CompressImageActivity extends AppCompatActivity {
    private static final int PICK_IMAGE_REQUEST = 1;
    private Uri selectedImageUri;
    private TextView selectedFileText;
    private Button selectImageButton;
    private Button compressButton;
    private ImageView previewImage;
    private CircularProgressIndicator progressIndicator;

    private final ActivityResultLauncher<String> imagePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(),
                    uri -> {
                        if (uri != null) {
                            selectedImageUri = uri;
                            updateUIForSelectedImage();
                        }
                    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_compress_image);

        setupToolbar();
        initializeViews();
        setupProgressIndicator();
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.compress_image);
        }
    }

    private void initializeViews() {
        selectedFileText = findViewById(R.id.selectedFileText);
        selectImageButton = findViewById(R.id.selectImageButton);
        compressButton = findViewById(R.id.compressButton);
        previewImage = findViewById(R.id.previewImage);
        progressIndicator = findViewById(R.id.progressIndicator);

        selectImageButton.setOnClickListener(v -> selectImage());
        compressButton.setOnClickListener(v -> compressImage());
        compressButton.setEnabled(false);
    }

    private void setupProgressIndicator() {
        progressIndicator.setVisibility(View.GONE);
    }

    private void selectImage() {
        imagePickerLauncher.launch("image/*");
    }

    private void updateUIForSelectedImage() {
        if (selectedImageUri != null) {
            String fileName = getFileName(selectedImageUri);
            selectedFileText.setText(getString(R.string.file_selected, fileName));
            previewImage.setImageURI(selectedImageUri);
            compressButton.setEnabled(true);
        }
    }

    private String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            try (android.database.Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (index >= 0) {
                        result = cursor.getString(index);
                    }
                }
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result;
    }

    private void compressImage() {
        if (selectedImageUri == null) {
            Toast.makeText(this, R.string.select_image_to_compress, Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/jpeg");
        intent.putExtra(Intent.EXTRA_TITLE, "compressed_" + getFileName(selectedImageUri));

        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            Uri outputUri = data.getData();
            if (outputUri != null) {
                startCompression(outputUri);
            }
        }
    }

    private void startCompression(Uri outputUri) {
        progressIndicator.setVisibility(View.VISIBLE);
        compressButton.setEnabled(false);
        selectImageButton.setEnabled(false);

        new Thread(() -> {
            try {
                ImageCompressor.compress(this, selectedImageUri, outputUri);
                runOnUiThread(() -> {
                    progressIndicator.setVisibility(View.GONE);
                    compressButton.setEnabled(true);
                    selectImageButton.setEnabled(true);
                    Toast.makeText(this, R.string.compress_success, Toast.LENGTH_SHORT).show();
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    progressIndicator.setVisibility(View.GONE);
                    compressButton.setEnabled(true);
                    selectImageButton.setEnabled(true);
                    Toast.makeText(this, R.string.compress_error, Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
} 