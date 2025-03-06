package com.example.docexpert;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.itextpdf.kernel.pdf.*;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class MergePDFActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_PICK_PDF = 100;
    private ArrayList<Uri> selectedFiles;
    private TextView tvSelectedFiles;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_merge_pdfactivity);

        selectedFiles = new ArrayList<>();
        Button btnSelectFiles = findViewById(R.id.btnSelectFiles);
        Button btnMerge = findViewById(R.id.btnMerge);
        tvSelectedFiles = findViewById(R.id.tvSelectedFiles);

        Log.d("DEBUG", "UI Initialized in MergePDFActivity");

        btnSelectFiles.setOnClickListener(v -> selectPDFs());
        btnMerge.setOnClickListener(v -> {
            if (selectedFiles.size() > 1) {
                mergePDFs();
            } else {
                Toast.makeText(this, "Select at least two PDFs.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void selectPDFs() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("application/pdf");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        startActivityForResult(Intent.createChooser(intent, "Select PDFs"), REQUEST_CODE_PICK_PDF);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_PICK_PDF && resultCode == Activity.RESULT_OK) {
            selectedFiles.clear();
            StringBuilder fileNames = new StringBuilder();

            if (data.getClipData() != null) {
                for (int i = 0; i < data.getClipData().getItemCount(); i++) {
                    Uri fileUri = data.getClipData().getItemAt(i).getUri();
                    selectedFiles.add(fileUri);
                    fileNames.append("✔ ").append(getFileName(fileUri)).append("\n");
                }
            } else if (data.getData() != null) {
                Uri fileUri = data.getData();
                selectedFiles.add(fileUri);
                fileNames.append("✔ ").append(getFileName(fileUri)).append("\n");
            }

            tvSelectedFiles.setText("Selected Files:\n" + fileNames.toString());
            Log.d("DEBUG", "Selected Files: " + selectedFiles.size());
        }
    }

    private void mergePDFs() {
        try {
            if (selectedFiles.isEmpty()) {
                Toast.makeText(this, "No PDFs selected!", Toast.LENGTH_SHORT).show();
                return;
            }

            // Generate unique filename with timestamp
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            String fileName = "MergedPDF_" + timestamp + ".pdf";

            // Save in Downloads folder (public storage)
            File outputFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileName);

            PdfWriter writer = new PdfWriter(new FileOutputStream(outputFile));
            PdfDocument pdfDoc = new PdfDocument(writer);

            for (Uri uri : selectedFiles) {
                InputStream inputStream = getContentResolver().openInputStream(uri);
                if (inputStream != null) {
                    PdfReader reader = new PdfReader(inputStream);
                    PdfDocument sourcePdf = new PdfDocument(reader);
                    sourcePdf.copyPagesTo(1, sourcePdf.getNumberOfPages(), pdfDoc);
                    sourcePdf.close();
                    inputStream.close();
                }
            }

            pdfDoc.close();
            Toast.makeText(this, "Merged PDF saved at:\n" + outputFile.getAbsolutePath(), Toast.LENGTH_LONG).show();
            Log.d("DEBUG", "Merged PDF saved at: " + outputFile.getAbsolutePath());

        } catch (Exception e) {
            Log.e("ERROR", "Error merging PDFs: " + e.getMessage());
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    // Helper method to get file name from URI
    private String getFileName(Uri uri) {
        String path = uri.getPath();
        if (path == null) return "Unknown.pdf";
        return path.substring(path.lastIndexOf("/") + 1);
    }
}
