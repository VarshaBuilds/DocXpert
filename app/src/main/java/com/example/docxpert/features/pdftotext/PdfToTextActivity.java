package com.example.docxpert.features.pdftotext;

import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.docxpert.R;
import com.example.docxpert.utils.SafUtils;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.text.PDFTextStripper;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class PdfToTextActivity extends AppCompatActivity {
    private TextView selectedFileText;
    private TextView extractedTextView;
    private Button selectButton;
    private Button saveButton;
    private LinearProgressIndicator progressIndicator;
    private Uri selectedPdfUri;
    private String extractedText;

    private ActivityResultLauncher<String[]> pickPdfLauncher;
    private ActivityResultLauncher<String> saveTextLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pdf_to_text);

        setupToolbar();
        initializeViews();
        setupLaunchers();
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.pdf_to_text);
        }
    }

    private void initializeViews() {
        selectedFileText = findViewById(R.id.selectedFileText);
        extractedTextView = findViewById(R.id.extractedTextView);
        selectButton = findViewById(R.id.selectButton);
        saveButton = findViewById(R.id.saveButton);
        progressIndicator = findViewById(R.id.progressIndicator);

        selectButton.setOnClickListener(v -> pickPdfLauncher.launch(new String[]{"application/pdf"}));
        saveButton.setOnClickListener(v -> saveExtractedText());
        saveButton.setEnabled(false);
    }

    private void setupLaunchers() {
        pickPdfLauncher = SafUtils.createOpenFileLauncher(
            this,
            new SafUtils.FileOperationCallback() {
                @Override
                public void onSuccess(Uri uri) {
                    selectedPdfUri = uri;
                    String fileName = SafUtils.getFileName(PdfToTextActivity.this, uri);
                    selectedFileText.setText(getString(R.string.file_selected, fileName));
                    extractTextFromPdf(uri);
                }

                @Override
                public void onFailure(Exception e) {
                    Toast.makeText(PdfToTextActivity.this, 
                        getString(R.string.error_selecting_file, e.getMessage()), 
                        Toast.LENGTH_SHORT).show();
                }
            }
        );

        saveTextLauncher = SafUtils.createSaveFileLauncher(
            this,
            "extracted_text",  // Default name, will be updated when saving
            ".txt",
            new SafUtils.FileOperationCallback() {
                @Override
                public void onSuccess(Uri uri) {
                    saveTextToFile(uri);
                }

                @Override
                public void onFailure(Exception e) {
                    Toast.makeText(PdfToTextActivity.this, 
                        getString(R.string.error_creating_file, e.getMessage()), 
                        Toast.LENGTH_SHORT).show();
                }
            }
        );
    }

    private void extractTextFromPdf(Uri pdfUri) {
        progressIndicator.setVisibility(View.VISIBLE);
        saveButton.setEnabled(false);
        extractedTextView.setText("");

        new Thread(() -> {
            try {
                InputStream inputStream = SafUtils.getInputStream(this, pdfUri);
                PDDocument document = PDDocument.load(inputStream);
                PDFTextStripper stripper = new PDFTextStripper();
                extractedText = stripper.getText(document);
                document.close();
                inputStream.close();

                runOnUiThread(() -> {
                    extractedTextView.setText(extractedText);
                    saveButton.setEnabled(true);
                    progressIndicator.setVisibility(View.GONE);
                    Toast.makeText(this, R.string.pdf_to_text_success, Toast.LENGTH_SHORT).show();
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    progressIndicator.setVisibility(View.GONE);
                    saveButton.setEnabled(false);
                    Toast.makeText(this, getString(R.string.pdf_to_text_error), Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    private void saveExtractedText() {
        if (extractedText == null || extractedText.isEmpty()) {
            Toast.makeText(this, R.string.select_pdf_to_extract, Toast.LENGTH_SHORT).show();
            return;
        }

        String fileName = "extracted_text.txt";
        if (selectedPdfUri != null) {
            String pdfName = SafUtils.getFileName(this, selectedPdfUri);
            if (pdfName != null && !pdfName.isEmpty()) {
                fileName = pdfName.replaceAll("\\.pdf$", ".txt");
            }
        }
        saveTextLauncher.launch(fileName);
    }

    private void saveTextToFile(Uri uri) {
        try {
            OutputStream outputStream = getContentResolver().openOutputStream(uri);
            if (outputStream != null) {
                outputStream.write(extractedText.getBytes(StandardCharsets.UTF_8));
                outputStream.close();
                Toast.makeText(this, R.string.text_saved, Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, getString(R.string.text_save_error) + ": " + e.getMessage(),
                Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
} 