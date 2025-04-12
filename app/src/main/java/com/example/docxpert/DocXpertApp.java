package com.example.docxpert;

import android.app.Application;
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader;

public class DocXpertApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        PDFBoxResourceLoader.init(getApplicationContext());
    }
} 