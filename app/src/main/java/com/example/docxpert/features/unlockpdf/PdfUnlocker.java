package com.example.docxpert.features.unlockpdf;

import android.content.Context;
import android.net.Uri;
import android.util.Log;
import com.example.docxpert.utils.SafUtils;
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader;
import com.tom_roush.pdfbox.pdmodel.PDDocument;
import java.io.InputStream;
import java.io.OutputStream;

public class PdfUnlocker {
    private static final String TAG = "PdfUnlocker";

    public static void unlock(Context context, Uri inputUri, Uri outputUri, String password) throws Exception {
        // Initialize PDFBox
        PDFBoxResourceLoader.init(context);

        try (InputStream inputStream = SafUtils.getInputStream(context, inputUri)) {
            // Load the PDF document with password
            PDDocument document = PDDocument.load(inputStream, password);

            try {
                // If we got here, the password was correct
                // Create a new document without encryption
                document.setAllSecurityToBeRemoved(true);

                // Save the unprotected document
                try (OutputStream outputStream = SafUtils.getOutputStream(context, outputUri)) {
                    document.save(outputStream);
                    Log.d(TAG, "PDF unlocked successfully");
                }
            } finally {
                document.close();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error unlocking PDF", e);
            throw e;
        }
    }
} 