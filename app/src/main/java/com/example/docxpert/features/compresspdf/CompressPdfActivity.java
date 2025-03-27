package com.example.docxpert.features.compresspdf;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.example.docxpert.R;
import com.example.docxpert.databinding.ActivityCompressPdfBinding;
import com.example.docxpert.utils.SafUtils;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.CircularProgressIndicator;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CompressPdfActivity extends AppCompatActivity {
    private ActivityCompressPdfBinding binding;
    private Uri selectedPdfUri;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private CircularProgressIndicator progressIndicator;

    private final ActivityResultLauncher<Intent> pickPdfLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getData() != null && result.getData().getData() != null) {
                    Uri uri = result.getData().getData();
                    try {
                        // Take persistable URI permission
                        getContentResolver().takePersistableUriPermission(uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        selectedPdfUri = uri;
                        String fileName = SafUtils.getFileName(this, selectedPdfUri);
                        binding.selectedFileText.setText(getString(R.string.file_selected, fileName));
                        binding.compressButton.setEnabled(true);
                    } catch (SecurityException e) {
                        Toast.makeText(this, getString(R.string.error_selecting_file, e.getMessage()),
                            Toast.LENGTH_SHORT).show();
                    }
                }
            }
    );

    private final ActivityResultLauncher<Intent> createCompressedPdfLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getData() != null && result.getData().getData() != null) {
                    Uri uri = result.getData().getData();
                    try {
                        // Take persistable URI permission
                        getContentResolver().takePersistableUriPermission(uri,
                            Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        compressPdf(uri);
                    } catch (SecurityException e) {
                        Toast.makeText(this, getString(R.string.error_creating_file, e.getMessage()),
                            Toast.LENGTH_SHORT).show();
                    }
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCompressPdfBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupToolbar();
        setupButtons();
        setupProgressIndicator();
    }

    private void setupToolbar() {
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void setupProgressIndicator() {
        progressIndicator = new CircularProgressIndicator(this);
        progressIndicator.setIndeterminate(true);
    }

    private void setupButtons() {
        binding.selectPdfButton.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("application/pdf");
            intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            pickPdfLauncher.launch(intent);
        });

        binding.compressButton.setOnClickListener(v -> {
            if (selectedPdfUri == null) {
                Toast.makeText(this, R.string.select_pdf_first, Toast.LENGTH_SHORT).show();
                return;
            }

            createCompressedPdfFile();
        });
    }

    private void createCompressedPdfFile() {
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String fileName = "compressed_" + timestamp + ".pdf";

        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/pdf");
        intent.putExtra(Intent.EXTRA_TITLE, fileName);
        intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        createCompressedPdfLauncher.launch(intent);
    }

    private void compressPdf(Uri outputUri) {
        progressIndicator.show();
        binding.compressButton.setEnabled(false);
        binding.selectPdfButton.setEnabled(false);

        executor.execute(() -> {
            try {
                // Verify we can access both URIs by attempting to open streams
                try (InputStream inputStream = SafUtils.getInputStream(this, selectedPdfUri);
                     OutputStream outputStream = SafUtils.getOutputStream(this, outputUri)) {
                    // Streams opened successfully, we have access
                }

                PdfCompressor.compress(this, selectedPdfUri, outputUri);

                runOnUiThread(() -> {
                    progressIndicator.hide();
                    binding.compressButton.setEnabled(true);
                    binding.selectPdfButton.setEnabled(true);
                    showSuccessDialog();
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    progressIndicator.hide();
                    binding.compressButton.setEnabled(true);
                    binding.selectPdfButton.setEnabled(true);
                    Toast.makeText(this, e.getMessage() != null ? e.getMessage() : getString(R.string.compress_error), Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void showSuccessDialog() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.success)
                .setMessage(R.string.compress_success)
                .setPositiveButton(R.string.ok, (dialog, which) -> {
                    dialog.dismiss();
                    finish();
                })
                .show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
} 