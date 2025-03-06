package com.example.docexpert;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.WriterProperties;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class CompressPDFActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_PICK_PDF = 101;
    private Uri selectedFileUri;
    private TextView tvSelectedPDF;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_compress_pdfactivity);

        Button btnSelectPDF = findViewById(R.id.btnSelectPDF);
        Button btnCompressPDF = findViewById(R.id.btnCompressPDF);
        tvSelectedPDF = findViewById(R.id.tvSelectedPDF);

        btnSelectPDF.setOnClickListener(v -> selectPDF());
        btnCompressPDF.setOnClickListener(v -> {
            if (selectedFileUri != null) {
                compressPDF();
            } else {
                Toast.makeText(this, "Please select a PDF file first.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void selectPDF() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("application/pdf");
        startActivityForResult(Intent.createChooser(intent, "Select a PDF"), REQUEST_CODE_PICK_PDF);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_PICK_PDF && resultCode == Activity.RESULT_OK && data.getData() != null) {
            selectedFileUri = data.getData();
            tvSelectedPDF.setText("Selected File:\n✔ " + selectedFileUri.getLastPathSegment());
        }
    }

    private void compressPDF() {
        if (selectedFileUri == null) {
            Toast.makeText(this, "No PDF selected!", Toast.LENGTH_SHORT).show();
            return;
        }

        File outputFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                "CompressedPDF_" + System.currentTimeMillis() + ".pdf");

        try (InputStream inputStream = getContentResolver().openInputStream(selectedFileUri);
             PdfReader reader = new PdfReader(inputStream);
             PdfWriter writer = new PdfWriter(new FileOutputStream(outputFile), new WriterProperties().setCompressionLevel(9));
             PdfDocument pdfDoc = new PdfDocument(reader, writer)) {

            Toast.makeText(this, "PDF compressed successfully!\nSaved in Downloads folder.", Toast.LENGTH_LONG).show();
            Log.d("DEBUG", "Compressed PDF saved at: " + outputFile.getAbsolutePath());

        } catch (Exception e) {
            Log.e("ERROR", "Error compressing PDF: " + e.getMessage());
            Toast.makeText(this, "Compression failed! Please try again.", Toast.LENGTH_LONG).show();
        }
    }
}
