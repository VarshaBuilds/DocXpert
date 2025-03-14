package com.example.docxpert.features.lockpdf;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
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

public class LockPdfActivity extends AppCompatActivity {
    private Uri selectedPdfUri;
    private TextView selectedFileText;
    private EditText passwordEditText;
    private EditText confirmPasswordEditText;
    private Button selectPdfButton;
    private Button lockButton;
    private ActivityResultLauncher<String[]> pickPdfLauncher;
    private ActivityResultLauncher<String> savePdfLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lock_pdf);

        // Initialize views
        selectedFileText = findViewById(R.id.selectedFileText);
        passwordEditText = findViewById(R.id.passwordEditText);
        confirmPasswordEditText = findViewById(R.id.confirmPasswordEditText);
        selectPdfButton = findViewById(R.id.selectPdfButton);
        lockButton = findViewById(R.id.lockButton);

        // Set up click listeners
        selectPdfButton.setOnClickListener(v -> pickPdf());
        lockButton.setOnClickListener(v -> lockPdf());

        // Initialize PDF picker launcher using SafUtils
        pickPdfLauncher = SafUtils.createOpenFileLauncher(this, new SafUtils.FileOperationCallback() {
            @Override
            public void onSuccess(Uri uri) {
                selectedPdfUri = uri;
                String fileName = SafUtils.getFileName(LockPdfActivity.this, uri);
                selectedFileText.setText(getString(R.string.file_selected, fileName));
                lockButton.setEnabled(true);
            }

            @Override
            public void onFailure(Exception e) {
                Toast.makeText(LockPdfActivity.this, 
                    getString(R.string.error_selecting_file, e.getMessage()),
                    Toast.LENGTH_SHORT).show();
            }
        });

        // Initialize save file launcher using SafUtils
        savePdfLauncher = SafUtils.createSaveFileLauncher(this, 
            "locked_pdf", 
            ".pdf",
            new SafUtils.FileOperationCallback() {
                @Override
                public void onSuccess(Uri uri) {
                    performLock(uri);
                }

                @Override
                public void onFailure(Exception e) {
                    Toast.makeText(LockPdfActivity.this,
                        getString(R.string.error_creating_file, e.getMessage()),
                        Toast.LENGTH_SHORT).show();
                }
            });

        // Initially disable lock button
        lockButton.setEnabled(false);
    }

    private void pickPdf() {
        pickPdfLauncher.launch(new String[]{"application/pdf"});
    }

    private void lockPdf() {
        if (selectedPdfUri == null) {
            Toast.makeText(this, R.string.select_pdf_first, Toast.LENGTH_SHORT).show();
            return;
        }

        String password = passwordEditText.getText().toString().trim();
        String confirmPassword = confirmPasswordEditText.getText().toString().trim();

        // Validate password
        if (TextUtils.isEmpty(password)) {
            Toast.makeText(this, R.string.enter_password, Toast.LENGTH_SHORT).show();
            return;
        }

        if (password.length() < 6) {
            Toast.makeText(this, R.string.password_too_short, Toast.LENGTH_SHORT).show();
            return;
        }

        if (!password.equals(confirmPassword)) {
            Toast.makeText(this, R.string.passwords_dont_match, Toast.LENGTH_SHORT).show();
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

    private void performLock(Uri outputUri) {
        // Show progress dialog
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage(getString(R.string.locking_pdf));
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressDialog.setCancelable(false);
        progressDialog.show();

        // Perform lock in background thread
        new Thread(() -> {
            try {
                String password = passwordEditText.getText().toString().trim();
                PdfLocker.lock(this, selectedPdfUri, outputUri, password);
                runOnUiThread(() -> {
                    progressDialog.dismiss();
                    Toast.makeText(this, R.string.lock_success, Toast.LENGTH_SHORT).show();
                    finish();
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    progressDialog.dismiss();
                    Toast.makeText(this, getString(R.string.lock_error) + ": " + e.getMessage(),
                        Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }
} 