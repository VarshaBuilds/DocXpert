package com.example.docxpert;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.example.docxpert.databinding.ActivityMainBinding;
import com.example.docxpert.features.pdf2word.PdfToWordActivity;
import com.example.docxpert.features.mergepdf.MergePdfActivity;
import com.google.android.material.card.MaterialCardView;
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader;

public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Initialize PDFBox
        PDFBoxResourceLoader.init(getApplicationContext());

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.app_name);
        }

        // Setup feature cards
        setupFeatureCards();
    }
    
    private void setupFeatureCards() {
        MaterialCardView pdfToWordCard = findViewById(R.id.pdfToWordCard);
        MaterialCardView mergePdfCard = findViewById(R.id.mergePdfCard);

        pdfToWordCard.setOnClickListener(v -> {
            Intent intent = new Intent(this, PdfToWordActivity.class);
            startActivity(intent);
        });

        mergePdfCard.setOnClickListener(v -> {
            Intent intent = new Intent(this, MergePdfActivity.class);
            startActivity(intent);
        });
    }
}