package com.example.docxpert.features.pdf2word;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import com.example.docxpert.R;
import com.example.docxpert.utils.SafUtils;

public class PdfToWordActivity extends AppCompatActivity {
    private Uri selectedPdfUri;
    private TextView selectedFileText;
    private ActivityResultLauncher<String[]> pickPdfLauncher;
    private ActivityResultLauncher<Intent> saveWordLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pdf_to_word);

        selectedFileText = findViewById(R.id.selectedFileText);
        Button selectPdfButton = findViewById(R.id.selectPdfButton);
        Button convertButton = findViewById(R.id.convertButton);

        selectPdfButton.setOnClickListener(v -> pickPdf());
        convertButton.setOnClickListener(v -> convertToWord());

        // Initialize PDF picker launcher
        pickPdfLauncher = registerForActivityResult(
            new ActivityResultContracts.OpenDocument(),
            uri -> {
                if (uri != null) {
                    selectedPdfUri = uri;
                    // Take persistable permission for reading
                    getContentResolver().takePersistableUriPermission(uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    String fileName = SafUtils.getFileName(this, uri);
                    selectedFileText.setText(fileName);
                    convertButton.setEnabled(true);
                }
            }
        );

        // Initialize save file launcher
        saveWordLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    if (uri != null) {
                        // Take persistable permission for writing
                        getContentResolver().takePersistableUriPermission(uri,
                            Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                        performConversion(uri);
                    }
                }
            }
        );
    }

    private void pickPdf() {
        pickPdfLauncher.launch(new String[]{"application/pdf"});
    }

    private void convertToWord() {
        if (selectedPdfUri == null) {
            Toast.makeText(this, R.string.select_pdf_first, Toast.LENGTH_SHORT).show();
            return;
        }

        String originalFileName = SafUtils.getFileName(this, selectedPdfUri);
        String wordFileName = SafUtils.getFileNameWithTimestamp(
            originalFileName.replaceAll("\\.pdf$", ""),
            ".docx"
        );

        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        intent.putExtra(Intent.EXTRA_TITLE, wordFileName);
        saveWordLauncher.launch(intent);
    }

    private void performConversion(Uri outputUri) {
        try {
            PdfToWordConverter.convert(this, selectedPdfUri, outputUri);
            Toast.makeText(this, R.string.conversion_success, Toast.LENGTH_SHORT).show();
            finish();
        } catch (Exception e) {
            Toast.makeText(this, getString(R.string.conversion_error) + ": " + e.getMessage(),
                Toast.LENGTH_SHORT).show();
        }
    }
} 