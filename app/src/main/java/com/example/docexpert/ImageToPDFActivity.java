package com.example.docexpert;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
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
import java.util.ArrayList;

public class ImageToPDFActivity extends AppCompatActivity {

    private Button selectImagesButton, convertToPdfButton;
    private TextView selectedImagesText;
    private ProgressBar progressBar;
    private ArrayList<Uri> imageUris = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_to_pdfactivity);

        selectImagesButton = findViewById(R.id.selectImagesButton);
        convertToPdfButton = findViewById(R.id.convertToPdfButton);
        selectedImagesText = findViewById(R.id.selectedImagesText);
        progressBar = findViewById(R.id.progressBar);
        progressBar.setVisibility(View.GONE);

        ActivityResultLauncher<Intent> imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        if (result.getData().getClipData() != null) { // Multiple images selected
                            int count = result.getData().getClipData().getItemCount();
                            imageUris.clear();
                            for (int i = 0; i < count; i++) {
                                imageUris.add(result.getData().getClipData().getItemAt(i).getUri());
                            }
                        } else if (result.getData().getData() != null) { // Single image selected
                            imageUris.clear();
                            imageUris.add(result.getData().getData());
                        }
                        selectedImagesText.setText(imageUris.size() + " images selected");
                        convertToPdfButton.setEnabled(true);
                    }
                });

        selectImagesButton.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
            imagePickerLauncher.launch(intent);
        });

        convertToPdfButton.setOnClickListener(v -> {
            if (!imageUris.isEmpty()) {
                convertImagesToPDF();
            } else {
                Toast.makeText(this, "Please select images", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void convertImagesToPDF() {
        progressBar.setVisibility(View.VISIBLE);
        convertToPdfButton.setEnabled(false);

        new Thread(() -> {
            try {
                PdfDocument pdfDocument = new PdfDocument();

                for (int i = 0; i < imageUris.size(); i++) {
                    Bitmap bitmap = getBitmapFromUri(imageUris.get(i));
                    if (bitmap == null) continue;

                    PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(bitmap.getWidth(), bitmap.getHeight(), i + 1).create();
                    PdfDocument.Page page = pdfDocument.startPage(pageInfo);
                    page.getCanvas().drawBitmap(bitmap, 0, 0, null);
                    pdfDocument.finishPage(page);
                }

                // 📂 Create a unique filename with timestamp
                String fileName = "Converted_" + System.currentTimeMillis() + ".pdf";

                // 📍 Save in Documents/DocExpert/ImageToPDF/
                File outputDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "DocExpert/ImageToPDF");
                if (!outputDir.exists()) outputDir.mkdirs();

                File pdfFile = new File(outputDir, fileName);
                FileOutputStream outputStream = new FileOutputStream(pdfFile);
                pdfDocument.writeTo(outputStream);
                pdfDocument.close();
                outputStream.close();

                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    convertToPdfButton.setEnabled(true);
                    Toast.makeText(this, "PDF saved at: " + pdfFile.getAbsolutePath(), Toast.LENGTH_LONG).show();
                });

            } catch (Exception e) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    convertToPdfButton.setEnabled(true);
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }


    private Bitmap getBitmapFromUri(Uri uri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            return android.graphics.BitmapFactory.decodeStream(inputStream);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
