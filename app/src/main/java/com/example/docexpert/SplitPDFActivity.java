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
import java.util.Date;
import java.util.Locale;

public class SplitPDFActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_PICK_PDF = 101;
    private Uri selectedFile;
    private TextView tvSelectedFile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_split_pdfactivity);

        tvSelectedFile = findViewById(R.id.tvSelectedFile);
        Button btnSelectFile = findViewById(R.id.btnSelectFile);
        Button btnSplit = findViewById(R.id.btnSplit);

        btnSelectFile.setOnClickListener(v -> selectPDF());
        btnSplit.setOnClickListener(v -> {
            if (selectedFile != null) {
                splitPDF();
            } else {
                Toast.makeText(this, "Please select a PDF first.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void selectPDF() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("application/pdf");
        startActivityForResult(Intent.createChooser(intent, "Select PDF"), REQUEST_CODE_PICK_PDF);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_PICK_PDF && resultCode == Activity.RESULT_OK && data.getData() != null) {
            selectedFile = data.getData();
            tvSelectedFile.setText("Selected File:\n✔ " + getFileName(selectedFile));
        }
    }

    private void splitPDF() {
        try {
            if (selectedFile == null) {
                Toast.makeText(this, "No PDF selected!", Toast.LENGTH_SHORT).show();
                return;
            }

            InputStream inputStream = getContentResolver().openInputStream(selectedFile);
            if (inputStream == null) {
                Toast.makeText(this, "Failed to open PDF!", Toast.LENGTH_SHORT).show();
                return;
            }

            PdfReader reader = new PdfReader(inputStream);
            PdfDocument sourcePdf = new PdfDocument(reader);
            int totalPages = sourcePdf.getNumberOfPages();

            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            File outputDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);

            for (int i = 1; i <= totalPages; i++) {
                String fileName = "SplitPage_" + i + "_" + timestamp + ".pdf";
                File outputFile = new File(outputDir, fileName);

                PdfWriter writer = new PdfWriter(new FileOutputStream(outputFile));
                PdfDocument newPdf = new PdfDocument(writer);

                sourcePdf.copyPagesTo(i, i, newPdf);
                newPdf.close();

                Log.d("DEBUG", "Saved: " + outputFile.getAbsolutePath());
            }

            sourcePdf.close();
            inputStream.close();

            Toast.makeText(this, "PDF split successfully! Pages saved in Downloads.", Toast.LENGTH_LONG).show();

        } catch (Exception e) {
            Log.e("ERROR", "Error splitting PDF: " + e.getMessage());
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