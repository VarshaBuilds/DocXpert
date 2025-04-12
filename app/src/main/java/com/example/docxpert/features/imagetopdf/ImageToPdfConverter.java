package com.example.docxpert.features.imagetopdf;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.util.Log;
import com.example.docxpert.utils.SafUtils;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

public class ImageToPdfConverter {
    private static final String TAG = "ImageToPdfConverter";

    public static void convertMultiple(Context context, List<Uri> imageUris, Uri outputUri) throws IOException {
        PdfDocument document = new PdfDocument();
        
        try {
            int pageNumber = 1;
            for (Uri imageUri : imageUris) {
                // Get input stream for image
                InputStream inputStream = context.getContentResolver().openInputStream(imageUri);
                
                // Decode image size first
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inJustDecodeBounds = true;
                BitmapFactory.decodeStream(inputStream, null, options);
                inputStream.close();
                
                // Calculate sample size to avoid OutOfMemoryError
                options.inSampleSize = calculateInSampleSize(options, 2480, 3508); // A4 size in pixels at 300 DPI
                options.inJustDecodeBounds = false;
                
                // Decode image with calculated sample size
                inputStream = context.getContentResolver().openInputStream(imageUri);
                Bitmap bitmap = BitmapFactory.decodeStream(inputStream, null, options);
                inputStream.close();
                
                if (bitmap != null) {
                    // Create PDF page
                    float pageWidth = 595f; // A4 width in points (72 points per inch)
                    float pageHeight = 842f; // A4 height in points
                    
                    PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder((int)pageWidth, (int)pageHeight, pageNumber).create();
                    PdfDocument.Page page = document.startPage(pageInfo);
                    
                    // Scale bitmap to fit page while maintaining aspect ratio
                    float scale = Math.min(pageWidth / bitmap.getWidth(), pageHeight / bitmap.getHeight());
                    float left = (pageWidth - (bitmap.getWidth() * scale)) / 2;
                    float top = (pageHeight - (bitmap.getHeight() * scale)) / 2;
                    
                    // Draw bitmap on page
                    page.getCanvas().scale(scale, scale);
                    page.getCanvas().translate(left / scale, top / scale);
                    page.getCanvas().drawBitmap(bitmap, 0, 0, null);
                    
                    document.finishPage(page);
                    bitmap.recycle();
                    pageNumber++;
                }
            }
            
            // Write the PDF to output stream
            OutputStream outputStream = context.getContentResolver().openOutputStream(outputUri);
            document.writeTo(outputStream);
            outputStream.close();
            
        } finally {
            document.close();
        }
    }
    
    private static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;
        
        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;
            
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }
        
        return inSampleSize;
    }
    
    // Keep the original convert method for backward compatibility
    public static void convert(Context context, Uri imageUri, Uri outputUri) throws IOException {
        convertMultiple(context, List.of(imageUri), outputUri);
    }
} 