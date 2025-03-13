package com.example.docxpert;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.example.docxpert.features.mergepdf.MergePdfActivity;
import com.google.android.material.card.MaterialCardView;
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        // Initialize PDFBox
        PDFBoxResourceLoader.init(this);
        
        setupFeatureCards();
    }

    private void setupFeatureCards() {
        MaterialCardView mergePdfCard = findViewById(R.id.mergePdfCard);

        mergePdfCard.setOnClickListener(v -> {
            Intent intent = new Intent(this, MergePdfActivity.class);
            startActivity(intent);
        });
    }
}