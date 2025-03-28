package com.example.docxpert.features.mergepdf;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.docxpert.R;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

public class MergePdfActivity extends AppCompatActivity {
    private List<Uri> pdfUris = new ArrayList<>();
    private PdfListAdapter adapter;
    private ActivityResultLauncher<Intent> pickPdfLauncher;
    private ActivityResultLauncher<Intent> saveFileLauncher;
    private Button mergeButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_merge_pdf);

        // Initialize RecyclerView
        RecyclerView recyclerView = findViewById(R.id.pdfListRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new PdfListAdapter(this, pdfUris);
        recyclerView.setAdapter(adapter);

        // Initialize merge button
        mergeButton = findViewById(R.id.mergeButton);
        mergeButton.setOnClickListener(v -> mergePdfs());
        mergeButton.setEnabled(false);

        // Initialize FAB for adding PDFs
        FloatingActionButton addPdfFab = findViewById(R.id.addPdfFab);
        addPdfFab.setOnClickListener(v -> pickPdf());

        // Initialize PDF picker launcher
        pickPdfLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    if (uri != null) {
                        // Take persistable permission for reading
                        getContentResolver().takePersistableUriPermission(uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        pdfUris.add(uri);
                        adapter.notifyItemInserted(pdfUris.size() - 1);
                        updateMergeButtonState();
                    }
                }
            }
        );

        // Initialize save file launcher
        saveFileLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    if (uri != null) {
                        // Take persistable permission for writing
                        getContentResolver().takePersistableUriPermission(uri,
                            Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                        performMerge(uri);
                    }
                }
            }
        );
    }

    private void pickPdf() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/pdf");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        pickPdfLauncher.launch(intent);
    }

    public void updateMergeButtonState() {
        mergeButton.setEnabled(pdfUris.size() >= 2);
    }

    private void mergePdfs() {
        if (pdfUris.size() < 2) {
            Toast.makeText(this, "Please select at least 2 PDF files", Toast.LENGTH_SHORT).show();
            return;
        }

        // Create output file name with timestamp
        String fileName = "merged_" + System.currentTimeMillis() + ".pdf";
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/pdf");
        intent.putExtra(Intent.EXTRA_TITLE, fileName);
        saveFileLauncher.launch(intent);
    }

    private void performMerge(Uri outputUri) {
        // Show progress dialog
        android.app.ProgressDialog progressDialog = new android.app.ProgressDialog(this);
        progressDialog.setMessage("Merging PDFs...");
        progressDialog.setProgressStyle(android.app.ProgressDialog.STYLE_SPINNER);
        progressDialog.setCancelable(false);
        progressDialog.show();

        // Perform merge in background thread
        new Thread(() -> {
            try {
                PdfMerger.mergePdfs(this, pdfUris, outputUri);
                runOnUiThread(() -> {
                    progressDialog.dismiss();
                    Toast.makeText(this, R.string.merge_success, Toast.LENGTH_SHORT).show();
                    finish();
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    progressDialog.dismiss();
                    Toast.makeText(this, getString(R.string.merge_error) + ": " + e.getMessage(),
                        Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }
} 