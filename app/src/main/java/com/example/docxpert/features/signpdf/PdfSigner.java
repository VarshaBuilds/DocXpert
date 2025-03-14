package com.example.docxpert.features.signpdf;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;

import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream;
import com.tom_roush.pdfbox.pdmodel.graphics.image.LosslessFactory;
import com.tom_roush.pdfbox.pdmodel.graphics.image.PDImageXObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

public class PdfSigner {
    public boolean signPdf(Context context, Uri inputUri, Uri outputUri, Bitmap signature) {
        try {
            InputStream inputStream = context.getContentResolver().openInputStream(inputUri);
            OutputStream outputStream = context.getContentResolver().openOutputStream(outputUri);

            if (inputStream == null || outputStream == null) {
                return false;
            }

            try {
                PDDocument document = PDDocument.load(inputStream);

                // Convert signature bitmap to bytes
                ByteArrayOutputStream signatureStream = new ByteArrayOutputStream();
                signature.compress(Bitmap.CompressFormat.PNG, 100, signatureStream);

                // Add signature to the last page
                int lastPageIndex = document.getNumberOfPages() - 1;
                PDImageXObject signatureImage = LosslessFactory.createFromImage(document, signature);

                PDPageContentStream contentStream = new PDPageContentStream(
                        document,
                        document.getPage(lastPageIndex),
                        PDPageContentStream.AppendMode.APPEND,
                        true
                );

                // Calculate position (bottom right corner)
                float pageWidth = document.getPage(lastPageIndex).getMediaBox().getWidth();
                float imageWidth = 150f; // Adjust size as needed
                float imageHeight = imageWidth * signature.getHeight() / signature.getWidth();
                float x = pageWidth - imageWidth - 50; // 50 units margin from right
                float y = 50f; // 50 units margin from bottom

                contentStream.drawImage(signatureImage, x, y, imageWidth, imageHeight);
                contentStream.close();

                document.save(outputStream);
                document.close();
                return true;
            } finally {
                inputStream.close();
                outputStream.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
} 