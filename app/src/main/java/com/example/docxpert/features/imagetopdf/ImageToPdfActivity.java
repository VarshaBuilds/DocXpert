package com.example.docxpert.features.imagetopdf;

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
import android.widget.LinearLayout;
import android.widget.ScrollView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.docxpert.R;
import com.google.android.material.progressindicator.CircularProgressIndicator;

import java.util.ArrayList;
import java.util.List;

public class ImageToPdfActivity extends AppCompatActivity {
    private static final int PICK_PDF_SAVE = 1;
    private List<Uri> selectedImageUris;
    private TextView selectedFileText;
    private Button selectButton;
    private Button convertButton;
    private LinearLayout previewContainer;
    private ScrollView previewScrollView;
    private CircularProgressIndicator progressIndicator;

    private final ActivityResultLauncher<String> imagePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.GetMultipleContents(),
                    uris -> {
                        if (uris != null && !uris.isEmpty()) {
                            selectedImageUris.addAll(uris);
                            updateUIForSelectedImages();
                        }
                    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_to_pdf);

        selectedImageUris = new ArrayList<>();
        setupToolbar();
        initializeViews();
        setupProgressIndicator();
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.image_to_pdf);
        }
    }

    private void initializeViews() {
        selectedFileText = findViewById(R.id.selectedFileText);
        selectButton = findViewById(R.id.selectButton);
        convertButton = findViewById(R.id.convertButton);
        previewContainer = findViewById(R.id.previewContainer);
        previewScrollView = findViewById(R.id.previewScrollView);
        progressIndicator = findViewById(R.id.progressIndicator);

        selectButton.setOnClickListener(v -> imagePickerLauncher.launch("image/*"));
        convertButton.setOnClickListener(v -> {
            if (selectedImageUris.isEmpty()) {
                Toast.makeText(this, R.string.select_image_to_convert, Toast.LENGTH_SHORT).show();
                return;
            }
            createPdf();
        });
        convertButton.setEnabled(false);
    }

    private void setupProgressIndicator() {
        progressIndicator.setVisibility(View.GONE);
    }

    private void updateUIForSelectedImages() {
        if (!selectedImageUris.isEmpty()) {
            StringBuilder fileNames = new StringBuilder();
            for (Uri uri : selectedImageUris) {
                String fileName = getFileName(uri);
                fileNames.append(fileName).append("\n");
            }
            selectedFileText.setText(getString(R.string.files_selected, fileNames.toString().trim()));
            
            // Clear previous previews
            previewContainer.removeAllViews();
            
            // Add preview for each image
            for (Uri uri : selectedImageUris) {
                ImageView imageView = new ImageView(this);
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    getResources().getDimensionPixelSize(R.dimen.preview_image_height)
                );
                params.setMargins(0, 0, 0, 
                    getResources().getDimensionPixelSize(R.dimen.preview_image_margin));
                imageView.setLayoutParams(params);
                imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                imageView.setImageURI(uri);
                previewContainer.addView(imageView);
            }
            
            convertButton.setEnabled(true);
        } else {
            selectedFileText.setText(R.string.no_file_selected);
            previewContainer.removeAllViews();
            convertButton.setEnabled(false);
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

    private void createPdf() {
        if (selectedImageUris.isEmpty()) {
            Toast.makeText(this, R.string.select_image_to_convert, Toast.LENGTH_SHORT).show();
            return;
        }

        String pdfFileName = "converted_images.pdf";

        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/pdf");
        intent.putExtra(Intent.EXTRA_TITLE, pdfFileName);

        startActivityForResult(intent, PICK_PDF_SAVE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_PDF_SAVE && resultCode == RESULT_OK && data != null) {
            Uri outputUri = data.getData();
            if (outputUri != null) {
                startConversion(outputUri);
            }
        }
    }

    private void startConversion(Uri outputUri) {
        progressIndicator.setVisibility(View.VISIBLE);
        convertButton.setEnabled(false);
        selectButton.setEnabled(false);

        new Thread(() -> {
            try {
                ImageToPdfConverter.convertMultiple(this, selectedImageUris, outputUri);
                runOnUiThread(() -> {
                    progressIndicator.setVisibility(View.GONE);
                    convertButton.setEnabled(true);
                    selectButton.setEnabled(true);
                    Toast.makeText(this, R.string.conversion_success, Toast.LENGTH_SHORT).show();
                    finish();
                });
            } catch (Exception e) {
                progressIndicator.setVisibility(View.GONE);
                convertButton.setEnabled(true);
                selectButton.setEnabled(true);
                Toast.makeText(this, R.string.image_to_pdf_error, Toast.LENGTH_SHORT).show();
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