package com.example.docxpert.features.compresspdf;

import android.content.Context;
import android.net.Uri;
import android.util.Log;
import com.example.docxpert.utils.SafUtils;
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader;
import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.pdmodel.PDPage;
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream;
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle;
import com.tom_roush.pdfbox.pdmodel.graphics.image.JPEGFactory;
import com.tom_roush.pdfbox.pdmodel.graphics.image.PDImageXObject;
import com.tom_roush.pdfbox.rendering.PDFRenderer;
import com.tom_roush.pdfbox.rendering.ImageType;
import com.tom_roush.pdfbox.io.MemoryUsageSetting;

import android.graphics.Bitmap;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.ByteArrayOutputStream;

public class PdfCompressor {
    private static final String TAG = "PdfCompressor";
    private static final float IMAGE_QUALITY = 0.8f; // Increased quality (0.1 to 1.0)
    private static final int DPI = 200; // Increased DPI for better quality
    private static final Bitmap.CompressFormat COMPRESS_FORMAT = Bitmap.CompressFormat.JPEG;

    public static void compress(Context context, Uri inputUri, Uri outputUri) throws Exception {
        // Initialize PDFBox
        PDFBoxResourceLoader.init(context);

        // Configure memory settings for large files
        MemoryUsageSetting memoryUsageSetting = MemoryUsageSetting.setupMixed(50 * 1024 * 1024) // 50MB in memory
                .setTempDir(context.getCacheDir());

        try (InputStream inputStream = SafUtils.getInputStream(context, inputUri);
             OutputStream outputStream = SafUtils.getOutputStream(context, outputUri)) {

            // Load the PDF document with memory settings
            PDDocument document = PDDocument.load(inputStream, memoryUsageSetting);

            try {
                // Create a new document for the compressed version
                PDDocument compressedDoc = new PDDocument();
                PDFRenderer pdfRenderer = new PDFRenderer(document);

                // Process each page
                for (int i = 0; i < document.getNumberOfPages(); i++) {
                    // Render the page to an image with higher quality settings
                    Bitmap pageBitmap = pdfRenderer.renderImageWithDPI(i, DPI, ImageType.RGB);
                    
                    // Create a new page
                    PDPage newPage = new PDPage(document.getPage(i).getMediaBox());
                    compressedDoc.addPage(newPage);

                    // Convert bitmap to compressed image with higher quality
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    pageBitmap.compress(COMPRESS_FORMAT, (int)(IMAGE_QUALITY * 100), baos);
                    
                    // Create PDImageXObject from compressed image
                    PDImageXObject image = JPEGFactory.createFromByteArray(compressedDoc, baos.toByteArray());

                    // Draw the image on the page
                    PDPageContentStream contentStream = new PDPageContentStream(
                            compressedDoc, 
                            newPage, 
                            PDPageContentStream.AppendMode.OVERWRITE, 
                            true, 
                            true);

                    // Scale image to fit page
                    PDRectangle mediaBox = newPage.getMediaBox();
                    contentStream.drawImage(image, 0, 0, mediaBox.getWidth(), mediaBox.getHeight());
                    contentStream.close();

                    // Clean up
                    pageBitmap.recycle();
                    baos.close();
                }

                // Save with compression
                compressedDoc.save(outputStream);
                compressedDoc.close();
                Log.d(TAG, "PDF compressed successfully");

            } finally {
                document.close();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error compressing PDF", e);
            throw e;
        }
    }
} 