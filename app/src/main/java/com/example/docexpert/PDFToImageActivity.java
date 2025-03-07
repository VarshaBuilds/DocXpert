package com.example.docexpert;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class PDFToImageActivity extends AppCompatActivity {

    private Button selectPdfButton, convertButton;
    private TextView selectedFileText;
    private ProgressBar progressBar;
    private Uri pdfUri;
    private File pdfFile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pdfto_image);

        selectPdfButton = findViewById(R.id.selectPdfButton);
        convertButton = findViewById(R.id.convertButton);
        selectedFileText = findViewById(R.id.selectedFileText);
        progressBar = findViewById(R.id.progressBar);

        progressBar.setVisibility(View.GONE);
        convertButton.setEnabled(false); // Disable until file is selected

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                startActivity(intent);
                return;
            }
        }

        // File picker launcher
        ActivityResultLauncher<Intent> filePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        pdfUri = result.getData().getData();
                        selectedFileText.setText("Selected File: " + pdfUri.getPath());
                        pdfFile = copyUriToFile(pdfUri);
                        convertButton.setEnabled(pdfFile != null);
                    }
                });

        selectPdfButton.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("application/pdf");
            filePickerLauncher.launch(intent);
        });

        convertButton.setOnClickListener(v -> {
            if (pdfFile != null) {
                convertPdfToImages();
            } else {
                Toast.makeText(this, "Please select a PDF file", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void convertPdfToImages() {
        progressBar.setVisibility(View.VISIBLE);
        convertButton.setEnabled(false);

        new Thread(() -> {
            try {
                ParcelFileDescriptor fileDescriptor = ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY);
                PdfRenderer pdfRenderer = new PdfRenderer(fileDescriptor);

                File outputDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "DocExpert/PDFtoImage");
                if (!outputDir.exists()) outputDir.mkdirs();

                for (int i = 0; i < pdfRenderer.getPageCount(); i++) {
                    PdfRenderer.Page page = pdfRenderer.openPage(i);

                    // High-resolution scaling for better quality
                    int width = page.getWidth() * 2;  // Scale up 2x for better quality
                    int height = page.getHeight() * 2;

                    Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                    Canvas canvas = new Canvas(bitmap);
                    canvas.drawColor(Color.WHITE); // Set white background
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
                    page.close();

                    String fileName = "Page_" + (i + 1) + ".jpg";
                    File imageFile = new File(outputDir, fileName);
                    FileOutputStream outputStream = new FileOutputStream(imageFile);
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream); // 100% quality
                    outputStream.close();
                }

                pdfRenderer.close();
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    convertButton.setEnabled(true);
                    Toast.makeText(this, "Images saved in Downloads", Toast.LENGTH_LONG).show();
                });

            } catch (Exception e) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    convertButton.setEnabled(true);
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    private File copyUriToFile(Uri uri) {
        try {
            File tempFile = new File(getCacheDir(), "selected.pdf");
            InputStream inputStream = getContentResolver().openInputStream(uri);
            FileOutputStream outputStream = new FileOutputStream(tempFile);
            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }
            inputStream.close();
            outputStream.close();
            return tempFile;
        } catch (Exception e) {
            Toast.makeText(this, "Error copying file: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            return null;
        }
    }
}
