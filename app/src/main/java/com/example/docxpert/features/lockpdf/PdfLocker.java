package com.example.docxpert.features.lockpdf;

import android.content.Context;
import android.net.Uri;
import android.util.Log;
import com.example.docxpert.utils.SafUtils;
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader;
import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.pdmodel.encryption.AccessPermission;
import com.tom_roush.pdfbox.pdmodel.encryption.StandardProtectionPolicy;
import java.io.InputStream;
import java.io.OutputStream;

public class PdfLocker {
    private static final String TAG = "PdfLocker";
    private static final int KEY_LENGTH = 128; // 128-bit encryption

    public static void lock(Context context, Uri inputUri, Uri outputUri, String password) throws Exception {
        // Initialize PDFBox
        PDFBoxResourceLoader.init(context);

        try (InputStream inputStream = SafUtils.getInputStream(context, inputUri);
             OutputStream outputStream = SafUtils.getOutputStream(context, outputUri)) {

            // Load the PDF document
            PDDocument document = PDDocument.load(inputStream);

            try {
                // Create access permissions
                AccessPermission accessPermission = new AccessPermission();
                accessPermission.setCanPrint(false);
                accessPermission.setCanModify(false);
                accessPermission.setCanExtractContent(false);
                accessPermission.setCanExtractForAccessibility(false);
                accessPermission.setCanFillInForm(false);
                accessPermission.setCanModifyAnnotations(false);

                // Create protection policy
                StandardProtectionPolicy protectionPolicy = new StandardProtectionPolicy(
                    password, // Owner password
                    password, // User password
                    accessPermission
                );
                protectionPolicy.setEncryptionKeyLength(KEY_LENGTH);

                // Apply protection
                document.protect(protectionPolicy);

                // Save the protected document
                document.save(outputStream);
                Log.d(TAG, "PDF locked successfully");

            } finally {
                document.close();
            }

        } catch (Exception e) {
            Log.e(TAG, "Error locking PDF", e);
            throw e;
        }
    }
} 