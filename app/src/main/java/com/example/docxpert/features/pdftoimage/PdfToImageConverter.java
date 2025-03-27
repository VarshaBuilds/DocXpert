package com.example.docxpert.features.pdftoimage;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import androidx.documentfile.provider.DocumentFile;
import com.example.docxpert.utils.SafUtils;
import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.rendering.PDFRenderer;
import com.tom_roush.pdfbox.rendering.ImageType;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class PdfToImageConverter {
    private static final float IMAGE_DPI = 300; // Higher DPI for better quality
    private static final String IMAGE_PREFIX = "page_";
    private static final String IMAGE_EXTENSION = ".png";

    public static List<Uri> convert(Context context, Uri pdfUri, Uri outputFolderUri) throws Exception {
        List<Uri> outputImageUris = new ArrayList<>();
        
        try (InputStream inputStream = SafUtils.getInputStream(context, pdfUri)) {
            if (inputStream == null) {
                throw new Exception("Could not open PDF file");
            }

            PDDocument document = PDDocument.load(inputStream);
            PDFRenderer pdfRenderer = new PDFRenderer(document);
            DocumentFile outputFolder = DocumentFile.fromTreeUri(context, outputFolderUri);

            if (outputFolder == null) {
                throw new Exception("Could not access output folder");
            }

            int pageCount = document.getNumberOfPages();
            
            for (int pageIndex = 0; pageIndex < pageCount; pageIndex++) {
                // Create a white background bitmap with ARGB_8888 for best quality
                Bitmap bitmap = pdfRenderer.renderImageWithDPI(pageIndex, IMAGE_DPI, ImageType.RGB);
                
                // Create a new bitmap with ARGB_8888 config and white background
                Bitmap finalBitmap = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);
                finalBitmap.eraseColor(Color.WHITE);
                
                // Draw the rendered page on top of the white background
                android.graphics.Canvas canvas = new android.graphics.Canvas(finalBitmap);
                canvas.drawBitmap(bitmap, 0, 0, null);
                
                // Create output file
                String fileName = IMAGE_PREFIX + (pageIndex + 1) + IMAGE_EXTENSION;
                DocumentFile outputFile = outputFolder.createFile("image/png", fileName);
                
                if (outputFile == null) {
                    throw new Exception("Could not create output file: " + fileName);
                }

                // Save the image
                try (OutputStream outputStream = SafUtils.getOutputStream(context, outputFile.getUri())) {
                    if (outputStream == null) {
                        throw new Exception("Could not open output stream for: " + fileName);
                    }
                    
                    finalBitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
                    outputImageUris.add(outputFile.getUri());
                }

                // Clean up bitmaps
                bitmap.recycle();
                finalBitmap.recycle();
            }

            document.close();
        }

        return outputImageUris;
    }
} 