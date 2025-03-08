package com.example.docexpert;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void openMergePDF(View view) {
        startActivity(new Intent(this, MergePDFActivity.class));
    }

    public void openSplitPDF(View view) {
        startActivity(new Intent(this, SplitPDFActivity.class));
    }

    public void openCompressPDF(View view) {
        startActivity(new Intent(this, CompressPDFActivity.class));
    }

    public void openPDFToWord(View view) {
        startActivity(new Intent(this, PDFToWordActivity.class));
    }
    public void openPDFToImage(View view) {
        startActivity(new Intent(this, PDFToImageActivity.class));
    }
    public void openImageToPDF(View view) {
        startActivity(new Intent(this, ImageToPDFActivity.class));
    }
}
