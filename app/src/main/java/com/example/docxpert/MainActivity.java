package com.example.docxpert;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.docxpert.features.lockpdf.LockPdfActivity;
import com.example.docxpert.features.mergepdf.MergePdfActivity;
import com.example.docxpert.features.signpdf.SignPdfActivity;
import com.example.docxpert.features.splitpdf.SplitPdfActivity;
import com.example.docxpert.features.unlockpdf.UnlockPdfActivity;
import com.example.docxpert.features.compresspdf.CompressPdfActivity;
import com.example.docxpert.features.compressimage.CompressImageActivity;
import com.example.docxpert.features.imagetopdf.ImageToPdfActivity;
import com.example.docxpert.features.pdftoimage.PdfToImageActivity;
import com.example.docxpert.features.ocr.OcrActivity;
import com.example.docxpert.features.pdftotext.PdfToTextActivity;
import com.google.android.material.card.MaterialCardView;
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        // Initialize PDFBox
        PDFBoxResourceLoader.init(getApplicationContext());
        
        setupToolbar();
        setupCardClickListeners();
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.app_name);
        }
    }

    private void setupCardClickListeners() {
        // Setup Merge PDF Card
        MaterialCardView mergePdfCard = findViewById(R.id.mergePdfCard);
        mergePdfCard.setOnClickListener(v -> startActivity(new Intent(this, MergePdfActivity.class)));

        // Setup Split PDF Card
        MaterialCardView splitPdfCard = findViewById(R.id.splitPdfCard);
        splitPdfCard.setOnClickListener(v -> startActivity(new Intent(this, SplitPdfActivity.class)));

        // Setup Unlock PDF Card
        MaterialCardView unlockPdfCard = findViewById(R.id.unlockPdfCard);
        unlockPdfCard.setOnClickListener(v -> startActivity(new Intent(this, UnlockPdfActivity.class)));

        // Setup lock PDF Card
        MaterialCardView lockPdfCard = findViewById(R.id.lockPdfCard);
        lockPdfCard.setOnClickListener(v -> startActivity(new Intent(this, LockPdfActivity.class)));

        // Setup Unlock PDF Card
        MaterialCardView signPdfCard = findViewById(R.id.signPdfCard);
        signPdfCard.setOnClickListener(v -> startActivity(new Intent(this, SignPdfActivity.class)));


        // Setup Compress PDF Card
        MaterialCardView compressPdfCard = findViewById(R.id.compressPdfCard);
        compressPdfCard.setOnClickListener(v -> startActivity(new Intent(this, CompressPdfActivity.class)));

        // Setup Compress Image Card
        MaterialCardView compressImageCard = findViewById(R.id.compressImageCard);
        compressImageCard.setOnClickListener(v -> startActivity(new Intent(this, CompressImageActivity.class)));

        // Setup Image to PDF Card
        MaterialCardView imageToPdfCard = findViewById(R.id.imageToPdfCard);
        imageToPdfCard.setOnClickListener(v -> startActivity(new Intent(this, ImageToPdfActivity.class)));

        // Setup PDF to Image Card
        MaterialCardView pdfToImageCard = findViewById(R.id.pdfToImageCard);
        pdfToImageCard.setOnClickListener(v -> startActivity(new Intent(this, PdfToImageActivity.class)));

        // Setup OCR Card
        MaterialCardView ocrCard = findViewById(R.id.ocrCard);
        ocrCard.setOnClickListener(v -> startActivity(new Intent(this, OcrActivity.class)));

        // Setup PDF to Text Card
        MaterialCardView pdfToTextCard = findViewById(R.id.pdfToTextCard);
        pdfToTextCard.setOnClickListener(v -> startActivity(new Intent(this, PdfToTextActivity.class)));
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}