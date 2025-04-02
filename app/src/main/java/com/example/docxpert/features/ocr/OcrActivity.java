package com.example.docxpert.features.ocr;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.docxpert.R;
import com.google.android.material.progressindicator.CircularProgressIndicator;

public class OcrActivity extends AppCompatActivity {
    private TextView selectedFileText;
    private TextView extractedText;
    private Button selectImageButton;
    private Button extractTextButton;
    private CircularProgressIndicator progressIndicator;
    private Uri selectedImageUri;
    private OcrProcessor ocrProcessor;

    private final ActivityResultLauncher<String> imagePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(),
                    uri -> {
                        if (uri != null) {
                            selectedImageUri = uri;
                            String fileName = uri.getLastPathSegment();
                            selectedFileText.setText(getString(R.string.file_selected, fileName));
                            extractTextButton.setEnabled(true);
                        }
                    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ocr);

        setupToolbar();
        initializeViews();
        ocrProcessor = new OcrProcessor();
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.ocr_extract_text);
        }
    }

    private void initializeViews() {
        selectedFileText = findViewById(R.id.selectedFileText);
        extractedText = findViewById(R.id.extractedText);
        selectImageButton = findViewById(R.id.selectImageButton);
        extractTextButton = findViewById(R.id.extractTextButton);
        progressIndicator = findViewById(R.id.progressIndicator);

        selectImageButton.setOnClickListener(v -> selectImage());
        extractTextButton.setOnClickListener(v -> extractText());
        extractTextButton.setEnabled(false);
    }

    private void selectImage() {
        imagePickerLauncher.launch("image/*");
    }

    private void extractText() {
        if (selectedImageUri == null) {
            Toast.makeText(this, R.string.ocr_select_image, Toast.LENGTH_SHORT).show();
            return;
        }

        progressIndicator.setVisibility(View.VISIBLE);
        extractTextButton.setEnabled(false);

        ocrProcessor.processImage(this, selectedImageUri, new OcrProcessor.OcrCallback() {
            @Override
            public void onSuccess(String extractedText) {
                runOnUiThread(() -> {
                    progressIndicator.setVisibility(View.GONE);
                    extractTextButton.setEnabled(true);
                    OcrActivity.this.extractedText.setText(extractedText);
                    Toast.makeText(OcrActivity.this, R.string.ocr_success, Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onError(Exception e) {
                runOnUiThread(() -> {
                    progressIndicator.setVisibility(View.GONE);
                    extractTextButton.setEnabled(true);
                    Toast.makeText(OcrActivity.this, 
                            getString(R.string.ocr_error, e.getMessage()), 
                            Toast.LENGTH_SHORT).show();
                });
            }
        });
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