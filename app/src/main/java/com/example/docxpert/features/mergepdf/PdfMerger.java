package com.example.docxpert.features.mergepdf;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.tom_roush.pdfbox.android.PDFBoxResourceLoader;
import com.tom_roush.pdfbox.io.MemoryUsageSetting;
import com.tom_roush.pdfbox.multipdf.PDFMergerUtility;
import com.tom_roush.pdfbox.pdmodel.PDDocument;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class PdfMerger {
    private static final String TAG = "PdfMerger";

    public static void mergePdfs(Context context, List<Uri> pdfUris, Uri outputUri) throws Exception {
        // Initialize PDFBox
        PDFBoxResourceLoader.init(context);
        
        // List to hold PDF byte arrays
        List<byte[]> pdfBytesList = new ArrayList<>();
        
        try {
            // Create merger utility
            PDFMergerUtility mergerUtility = new PDFMergerUtility();

            // First, load all PDFs into memory
            for (Uri pdfUri : pdfUris) {
                byte[] pdfBytes;
                try (InputStream inputStream = context.getContentResolver().openInputStream(pdfUri)) {
                    if (inputStream == null) {
                        throw new Exception("Could not open PDF file");
                    }
                    
                    // Read PDF into byte array
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    byte[] buffer = new byte[8192];
                    int bytesRead;
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        baos.write(buffer, 0, bytesRead);
                    }
                    pdfBytes = baos.toByteArray();
                    pdfBytesList.add(pdfBytes);
                }
            }

            // Set up output stream
            try (OutputStream outputStream = context.getContentResolver().openOutputStream(outputUri)) {
                if (outputStream == null) {
                    throw new Exception("Could not open output file");
                }
                mergerUtility.setDestinationStream(outputStream);

                // Add each PDF as a source
                for (byte[] pdfBytes : pdfBytesList) {
                    ByteArrayInputStream bais = new ByteArrayInputStream(pdfBytes);
                    mergerUtility.addSource(bais);
                }

                // Perform merge operation
                mergerUtility.mergeDocuments(MemoryUsageSetting.setupTempFileOnly());
                Log.d(TAG, "PDFs merged successfully");
            }

        } catch (Exception e) {
            Log.e(TAG, "Error merging PDFs", e);
            throw e;
        }
    }
} 