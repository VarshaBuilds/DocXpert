package com.example.docxpert.features.unlockpdf;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.appcompat.app.AppCompatActivity;
import com.example.docxpert.R;
import com.example.docxpert.utils.SafUtils;
import android.app.ProgressDialog;

public class UnlockPdfActivity extends AppCompatActivity {
    private Uri selectedPdfUri;
    private TextView selectedFileText;
    private EditText passwordEditText;
    private Button selectPdfButton;
    private Button unlockButton;
    private ActivityResultLauncher<String[]> pickPdfLauncher;
    private ActivityResultLauncher<String> savePdfLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_unlock_pdf);

        // Initialize views
        selectedFileText = findViewById(R.id.selectedFileText);
        passwordEditText = findViewById(R.id.passwordEditText);
        selectPdfButton = findViewById(R.id.selectPdfButton);
        unlockButton = findViewById(R.id.unlockButton);

        // Set up click listeners
        selectPdfButton.setOnClickListener(v -> pickPdf());
        unlockButton.setOnClickListener(v -> unlockPdf());

        // Initialize PDF picker launcher using SafUtils
        pickPdfLauncher = SafUtils.createOpenFileLauncher(this, new SafUtils.FileOperationCallback() {
            @Override
            public void onSuccess(Uri uri) {
                selectedPdfUri = uri;
                String fileName = SafUtils.getFileName(UnlockPdfActivity.this, uri);
                selectedFileText.setText(getString(R.string.file_selected, fileName));
                unlockButton.setEnabled(true);
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(UnlockPdfActivity.this, 
                    getString(R.string.error_selecting_file, e.getMessage()),
                    Toast.LENGTH_SHORT).show();
            }
        });

        // Initialize save file launcher using SafUtils
        savePdfLauncher = SafUtils.createSaveFileLauncher(this, 
            "unlocked_pdf", 
            ".pdf",
            new SafUtils.FileOperationCallback() {
                @Override
                public void onSuccess(Uri uri) {
                    performUnlock(uri);
                }

                @Override
                public void onFailure(Exception e) {
                    Toast.makeText(UnlockPdfActivity.this,
                        getString(R.string.error_creating_file, e.getMessage()),
                        Toast.LENGTH_SHORT).show();
                }
            });

        // Initially disable unlock button
        unlockButton.setEnabled(false);
    }

    private void pickPdf() {
        pickPdfLauncher.launch(new String[]{"application/pdf"});
    }

    private void unlockPdf() {
        if (selectedPdfUri == null) {
            Toast.makeText(this, R.string.select_pdf_first, Toast.LENGTH_SHORT).show();
            return;
        }

        String password = passwordEditText.getText().toString().trim();
        if (TextUtils.isEmpty(password)) {
            Toast.makeText(this, R.string.enter_pdf_password, Toast.LENGTH_SHORT).show();
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

    private void performUnlock(Uri outputUri) {
        // Show progress dialog
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage(getString(R.string.unlocking_pdf));
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressDialog.setCancelable(false);
        progressDialog.show();

        // Perform unlock in background thread
        new Thread(() -> {
            try {
                String password = passwordEditText.getText().toString().trim();
                PdfUnlocker.unlock(this, selectedPdfUri, outputUri, password);
                runOnUiThread(() -> {
                    progressDialog.dismiss();
                    Toast.makeText(this, R.string.unlock_success, Toast.LENGTH_SHORT).show();
                    finish();
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    progressDialog.dismiss();
                    String errorMessage = e.getMessage();
                    if (errorMessage != null && errorMessage.contains("password")) {
                        Toast.makeText(this, R.string.incorrect_password, Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, getString(R.string.unlock_error) + ": " + errorMessage,
                            Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }).start();
    }
} 