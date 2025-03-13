package com.example.docxpert.features.splitpdf;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.appcompat.app.AppCompatActivity;
import com.example.docxpert.R;
import com.example.docxpert.utils.SafUtils;
import android.app.ProgressDialog;
import com.tom_roush.pdfbox.pdmodel.PDDocument;
import java.io.InputStream;

public class SplitPdfActivity extends AppCompatActivity {
    private Uri selectedPdfUri;
    private TextView selectedFileText;
    private TextView totalPagesText;
    private EditText pageRangeEditText;
    private Button selectPdfButton;
    private Button splitButton;
    private ActivityResultLauncher<String[]> pickPdfLauncher;
    private ActivityResultLauncher<String> savePdfLauncher;
    private int totalPages = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_split_pdf);

        // Initialize views
        selectedFileText = findViewById(R.id.selectedFileText);
        totalPagesText = findViewById(R.id.totalPagesText);
        pageRangeEditText = findViewById(R.id.pageRangeEditText);
        selectPdfButton = findViewById(R.id.selectPdfButton);
        splitButton = findViewById(R.id.splitButton);

        // Set up click listeners
        selectPdfButton.setOnClickListener(v -> pickPdf());
        splitButton.setOnClickListener(v -> splitPdf());

        // Initialize PDF picker launcher using SafUtils
        pickPdfLauncher = SafUtils.createOpenFileLauncher(this, new SafUtils.FileOperationCallback() {
            @Override
            public void onSuccess(Uri uri) {
                selectedPdfUri = uri;
                String fileName = SafUtils.getFileName(SplitPdfActivity.this, uri);
                selectedFileText.setText(getString(R.string.file_selected, fileName));
                loadPdfInfo(uri);
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(SplitPdfActivity.this, 
                    getString(R.string.error_selecting_file, e.getMessage()),
                    Toast.LENGTH_SHORT).show();
            }
        });

        // Initialize save file launcher using SafUtils
        savePdfLauncher = SafUtils.createSaveFileLauncher(this, 
            "split_pdf", 
            ".pdf",
            new SafUtils.FileOperationCallback() {
                @Override
                public void onSuccess(Uri uri) {
                    performSplit(uri);
                }

                @Override
                public void onFailure(Exception e) {
                    Toast.makeText(SplitPdfActivity.this,
                        getString(R.string.error_creating_file, e.getMessage()),
                        Toast.LENGTH_SHORT).show();
                }
            });

        // Initially disable split button
        splitButton.setEnabled(false);
    }

    private void loadPdfInfo(Uri uri) {
        new Thread(() -> {
            try (InputStream inputStream = SafUtils.getInputStream(this, uri)) {
                PDDocument document = PDDocument.load(inputStream);
                totalPages = document.getNumberOfPages();
                document.close();
                
                runOnUiThread(() -> {
                    totalPagesText.setVisibility(View.VISIBLE);
                    totalPagesText.setText(getString(R.string.total_pages, totalPages));
                    splitButton.setEnabled(true);
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    Toast.makeText(this, getString(R.string.error_selecting_file, e.getMessage()),
                        Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    private void pickPdf() {
        pickPdfLauncher.launch(new String[]{"application/pdf"});
    }

    private void splitPdf() {
        if (selectedPdfUri == null) {
            Toast.makeText(this, R.string.select_pdf_first, Toast.LENGTH_SHORT).show();
            return;
        }

        String pageRange = pageRangeEditText.getText().toString().trim();
        if (pageRange.isEmpty()) {
            Toast.makeText(this, R.string.enter_page_range, Toast.LENGTH_SHORT).show();
            return;
        }

        // Validate page range format and values
        if (!isValidPageRange(pageRange)) {
            Toast.makeText(this, R.string.invalid_page_range, Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            validatePageNumbers(pageRange);
        } catch (Exception e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
            return;
        }

        // Create output file name with timestamp
        String originalFileName = SafUtils.getFileName(this, selectedPdfUri);
        String outputFileName = SafUtils.getFileNameWithTimestamp(originalFileName, ".pdf");

        // Create output file using SAF
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/pdf");
        intent.putExtra(Intent.EXTRA_TITLE, outputFileName);
        savePdfLauncher.launch(outputFileName);
    }

    private void validatePageNumbers(String pageRange) throws Exception {
        String[] ranges = pageRange.split(",");
        for (String range : ranges) {
            if (range.contains("-")) {
                String[] bounds = range.split("-");
                int start = Integer.parseInt(bounds[0]);
                int end = Integer.parseInt(bounds[1]);
                if (start > totalPages || end > totalPages) {
                    throw new Exception("Page numbers cannot exceed " + totalPages);
                }
            } else {
                int page = Integer.parseInt(range);
                if (page > totalPages) {
                    throw new Exception("Page numbers cannot exceed " + totalPages);
                }
            }
        }
    }

    private void performSplit(Uri outputUri) {
        // Show progress dialog
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage(getString(R.string.splitting_pdf));
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressDialog.setCancelable(false);
        progressDialog.show();

        // Perform split in background thread
        new Thread(() -> {
            try {
                String pageRange = pageRangeEditText.getText().toString().trim();
                PdfSplitter.split(this, selectedPdfUri, outputUri, pageRange);
                runOnUiThread(() -> {
                    progressDialog.dismiss();
                    Toast.makeText(this, R.string.split_success, Toast.LENGTH_SHORT).show();
                    finish();
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    progressDialog.dismiss();
                    Toast.makeText(this, getString(R.string.split_error) + ": " + e.getMessage(),
                        Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    private boolean isValidPageRange(String pageRange) {
        // Accept formats like: "1-5" or "1,3,5-7"
        return pageRange.matches("^\\d+(-\\d+)?(,\\d+(-\\d+)?)*$");
    }
} 