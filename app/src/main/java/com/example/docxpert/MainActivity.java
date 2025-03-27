package com.example.docxpert;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.example.docxpert.features.mergepdf.MergePdfActivity;
import com.example.docxpert.features.splitpdf.SplitPdfActivity;
import com.example.docxpert.features.lockpdf.LockPdfActivity;
import com.example.docxpert.features.unlockpdf.UnlockPdfActivity;
import com.example.docxpert.features.signpdf.SignPdfActivity;
import com.example.docxpert.features.compresspdf.CompressPdfActivity;
import com.example.docxpert.features.compressimage.CompressImageActivity;
import com.google.android.material.card.MaterialCardView;
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        PDFBoxResourceLoader.init(getApplicationContext());
        
        setupFeatureCards();
    }

    private void setupFeatureCards() {
        // Setup Merge PDF Card
        MaterialCardView mergePdfCard = findViewById(R.id.mergePdfCard);
        mergePdfCard.setOnClickListener(v -> {
            Intent intent = new Intent(this, MergePdfActivity.class);
            startActivity(intent);
        });

        // Setup Split PDF Card
        MaterialCardView splitPdfCard = findViewById(R.id.splitPdfCard);
        splitPdfCard.setOnClickListener(v -> {
            Intent intent = new Intent(this, SplitPdfActivity.class);
            startActivity(intent);
        });

        // Setup Lock PDF Card
        MaterialCardView lockPdfCard = findViewById(R.id.lockPdfCard);
        lockPdfCard.setOnClickListener(v -> {
            Intent intent = new Intent(this, LockPdfActivity.class);
            startActivity(intent);
        });

        // Setup Unlock PDF Card
        MaterialCardView unlockPdfCard = findViewById(R.id.unlockPdfCard);
        unlockPdfCard.setOnClickListener(v -> {
            Intent intent = new Intent(this, UnlockPdfActivity.class);
            startActivity(intent);
        });

        // Setup Sign PDF Card
        MaterialCardView signPdfCard = findViewById(R.id.signPdfCard);
        signPdfCard.setOnClickListener(v -> {
            Intent intent = new Intent(this, SignPdfActivity.class);
            startActivity(intent);
        });

        // Setup Compress PDF Card
        MaterialCardView compressPdfCard = findViewById(R.id.compressPdfCard);
        compressPdfCard.setOnClickListener(v -> {
            Intent intent = new Intent(this, CompressPdfActivity.class);
            startActivity(intent);
        });

        // Setup Compress Image Card
        MaterialCardView compressImageCard = findViewById(R.id.compressImageCard);
        compressImageCard.setOnClickListener(v -> {
            Intent intent = new Intent(this, CompressImageActivity.class);
            startActivity(intent);
        });
    }
}