package com.example.docxpert.features.pdftoimage;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
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
import androidx.documentfile.provider.DocumentFile;

import com.example.docxpert.R;
import com.example.docxpert.utils.SafUtils;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.tom_roush.pdfbox.pdmodel.PDDocument;

import java.io.InputStream;
import java.util.List;

public class PdfToImageActivity extends AppCompatActivity {
    private Uri selectedPdfUri;
    private Uri outputFolderUri;
    private TextView selectedFileText;
    private TextView pageCountText;
    private Button selectPdfButton;
    private Button convertButton;
    private LinearLayout previewContainer;
    private ScrollView previewScrollView;
    private CircularProgressIndicator progressIndicator;

    private final ActivityResultLauncher<String> pdfPickerLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(),
                    uri -> {
                        if (uri != null) {
                            selectedPdfUri = uri;
                            updateUIForSelectedPdf();
                        }
                    });

    private final ActivityResultLauncher<Uri> folderPickerLauncher =
            registerForActivityResult(new ActivityResultContracts.OpenDocumentTree(),
                    uri -> {
                        if (uri != null) {
                            // Take persistable URI permission
                            final int takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION | 
                                               Intent.FLAG_GRANT_WRITE_URI_PERMISSION;
                            getContentResolver().takePersistableUriPermission(uri, takeFlags);
                            outputFolderUri = uri;
                            startConversion();
                        }
                    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pdf_to_image);

        setupToolbar();
        initializeViews();
        setupProgressIndicator();
        checkSavedOutputFolder();
    }

    private void checkSavedOutputFolder() {
        // Check if we already have persisted permissions for any folder
        try {
            for (android.content.UriPermission permission : getContentResolver().getPersistedUriPermissions()) {
                if (permission.isWritePermission() && permission.isReadPermission()) {
                    outputFolderUri = permission.getUri();
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.pdf_to_image);
        }
    }

    private void initializeViews() {
        selectedFileText = findViewById(R.id.selectedFileText);
        pageCountText = findViewById(R.id.pageCountText);
        selectPdfButton = findViewById(R.id.selectPdfButton);
        convertButton = findViewById(R.id.convertButton);
        previewContainer = findViewById(R.id.previewContainer);
        previewScrollView = findViewById(R.id.previewScrollView);
        progressIndicator = findViewById(R.id.progressIndicator);

        selectPdfButton.setOnClickListener(v -> selectPdf());
        convertButton.setOnClickListener(v -> convertToImages());
        convertButton.setEnabled(false);
    }

    private void setupProgressIndicator() {
        progressIndicator.setVisibility(View.GONE);
    }

    private void selectPdf() {
        pdfPickerLauncher.launch("application/pdf");
    }

    private void updateUIForSelectedPdf() {
        if (selectedPdfUri != null) {
            String fileName = SafUtils.getFileName(this, selectedPdfUri);
            selectedFileText.setText(getString(R.string.file_selected, fileName));
            
            // Get page count
            try (InputStream inputStream = SafUtils.getInputStream(this, selectedPdfUri)) {
                if (inputStream != null) {
                    PDDocument document = PDDocument.load(inputStream);
                    int numPages = document.getNumberOfPages();
                    pageCountText.setVisibility(View.VISIBLE);
                    pageCountText.setText(getString(R.string.page_count, numPages));
                    document.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            
            convertButton.setEnabled(true);
        } else {
            selectedFileText.setText(R.string.no_file_selected);
            pageCountText.setVisibility(View.GONE);
            convertButton.setEnabled(false);
        }
    }

    private void convertToImages() {
        if (selectedPdfUri == null) {
            Toast.makeText(this, R.string.select_pdf_to_convert, Toast.LENGTH_SHORT).show();
            return;
        }

        if (outputFolderUri == null) {
            // If we don't have a saved folder URI, ask user to select one
            folderPickerLauncher.launch(null);
        } else {
            // Verify if the permission is still valid
            try {
                DocumentFile folder = DocumentFile.fromTreeUri(this, outputFolderUri);
                if (folder != null && folder.canWrite()) {
                    startConversion();
                } else {
                    // If permission is no longer valid, ask user to select folder again
                    folderPickerLauncher.launch(null);
                }
            } catch (Exception e) {
                folderPickerLauncher.launch(null);
            }
        }
    }

    private void startConversion() {
        progressIndicator.setVisibility(View.VISIBLE);
        convertButton.setEnabled(false);
        selectPdfButton.setEnabled(false);

        new Thread(() -> {
            try {
                List<Uri> outputImageUris = PdfToImageConverter.convert(this, selectedPdfUri, outputFolderUri);
                runOnUiThread(() -> {
                    progressIndicator.setVisibility(View.GONE);
                    convertButton.setEnabled(true);
                    selectPdfButton.setEnabled(true);
                    Toast.makeText(this, R.string.pdf_to_image_success, Toast.LENGTH_SHORT).show();
                    
                    // Clear selection after successful conversion
                    selectedPdfUri = null;
                    updateUIForSelectedPdf();
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    progressIndicator.setVisibility(View.GONE);
                    convertButton.setEnabled(true);
                    selectPdfButton.setEnabled(true);
                    Toast.makeText(this, getString(R.string.error_converting, e.getMessage()), Toast.LENGTH_SHORT).show();
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