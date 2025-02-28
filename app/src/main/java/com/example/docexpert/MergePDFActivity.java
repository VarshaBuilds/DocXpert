package com.example.docexpert;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.itextpdf.kernel.pdf.*;
import java.io.*;
import java.util.ArrayList;

public class MergePDFActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_PICK_PDF = 100;
    private ArrayList<Uri> selectedFiles;
    private TextView tvSelectedFiles;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_merge_pdfactivity);

        Button btnSelectFiles = findViewById(R.id.btnSelectFiles);
        Button btnMerge = findViewById(R.id.btnMerge);
        tvSelectedFiles = findViewById(R.id.tvSelectedFiles);

        if (btnSelectFiles == null || btnMerge == null || tvSelectedFiles == null) {
            throw new RuntimeException("UI elements not found! Check your XML layout file.");
        }
        selectedFiles = new ArrayList<>();
        btnSelectFiles.setOnClickListener(v -> selectPDFs());
        btnMerge.setOnClickListener(v -> {
            if (selectedFiles.size() > 1) {
                mergePDFs();
            } else {
                tvSelectedFiles.setText("Select at least two PDFs.");
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
            if (data.getClipData() != null) {
                for (int i = 0; i < data.getClipData().getItemCount(); i++) {
                    selectedFiles.add(data.getClipData().getItemAt(i).getUri());
                }
            } else if (data.getData() != null) {
                selectedFiles.add(data.getData());
            }
            tvSelectedFiles.setText("Selected Files: " + selectedFiles.size());
        }
    }

    private void mergePDFs() {
        try {
            File outputFile = new File(getExternalFilesDir(null), "MergedPDF.pdf");
            PdfWriter writer = new PdfWriter(outputFile);
            PdfDocument pdfDoc = new PdfDocument(writer);

            for (Uri uri : selectedFiles) {
                InputStream inputStream = getContentResolver().openInputStream(uri);
                if (inputStream == null) continue;

                PdfReader reader = new PdfReader(inputStream);
                PdfDocument sourcePdf = new PdfDocument(reader);
                sourcePdf.copyPagesTo(1, sourcePdf.getNumberOfPages(), pdfDoc);
                sourcePdf.close();
                inputStream.close();
            }

            pdfDoc.close();
            tvSelectedFiles.setText("Merged PDF saved at:\n" + outputFile.getAbsolutePath());
        } catch (Exception e) {
            tvSelectedFiles.setText("Error merging PDFs: " + e.getMessage());
        }
    }
}