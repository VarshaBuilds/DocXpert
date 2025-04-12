package com.example.docxpert.features.compressimage;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Log;
import android.database.Cursor;
import android.provider.OpenableColumns;
import com.example.docxpert.utils.SafUtils;
import java.io.InputStream;
import java.io.OutputStream;

public class ImageCompressor {
    private static final String TAG = "ImageCompressor";
    private static final int MAX_IMAGE_DIMENSION = 1024;
    private static final int MIN_QUALITY = 40;
    private static final int MAX_QUALITY = 80;
    private static final long MB_THRESHOLD = 1024 * 1024; // 1MB

    public static void compress(Context context, Uri inputUri, Uri outputUri) throws Exception {
        Bitmap bitmap = null;

        try {
            // Get input file size
            long fileSize = getFileSize(context, inputUri);
            // Calculate quality based on file size
            int quality = calculateQuality(fileSize);
            
            Log.d(TAG, "Input file size: " + (fileSize / 1024) + "KB, Using quality: " + quality);

            // First get image bounds
            BitmapFactory.Options boundsOptions = new BitmapFactory.Options();
            boundsOptions.inJustDecodeBounds = true;
            try (InputStream boundsInputStream = SafUtils.getInputStream(context, inputUri)) {
                if (boundsInputStream == null) {
                    throw new Exception("Could not open input file");
                }
                BitmapFactory.decodeStream(boundsInputStream, null, boundsOptions);
            }

            // Calculate sample size based on dimensions
            int sampleSize = calculateSampleSize(boundsOptions.outWidth, boundsOptions.outHeight);

            // Now decode with sample size
            BitmapFactory.Options bitmapOptions = new BitmapFactory.Options();
            bitmapOptions.inSampleSize = sampleSize;
            bitmapOptions.inPreferredConfig = Bitmap.Config.RGB_565;

            try (InputStream bitmapInputStream = SafUtils.getInputStream(context, inputUri)) {
                if (bitmapInputStream == null) {
                    throw new Exception("Could not open input file");
                }
                bitmap = BitmapFactory.decodeStream(bitmapInputStream, null, bitmapOptions);
                if (bitmap == null) {
                    throw new Exception("Failed to decode image");
                }

                // Scale down if needed
                int width = bitmap.getWidth();
                int height = bitmap.getHeight();
                if (width > MAX_IMAGE_DIMENSION || height > MAX_IMAGE_DIMENSION) {
                    float scale = Math.min(
                        (float) MAX_IMAGE_DIMENSION / width,
                        (float) MAX_IMAGE_DIMENSION / height
                    );
                    int newWidth = Math.round(width * scale);
                    int newHeight = Math.round(height * scale);
                    
                    Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);
                    bitmap.recycle();
                    bitmap = scaledBitmap;
                }

                // Compress and save
                try (OutputStream outputStream = SafUtils.getOutputStream(context, outputUri)) {
                    if (outputStream == null) {
                        throw new Exception("Could not open output file");
                    }

                    if (!bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)) {
                        throw new Exception("Failed to compress and save image");
                    }
                }

                Log.d(TAG, "Image compressed successfully");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error compressing image: " + e.getMessage(), e);
            throw new Exception("Error compressing image: " + e.getMessage());
        } finally {
            if (bitmap != null && !bitmap.isRecycled()) {
                bitmap.recycle();
            }
        }
    }

    private static long getFileSize(Context context, Uri uri) {
        try (Cursor cursor = context.getContentResolver().query(uri, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE);
                if (sizeIndex != -1) {
                    return cursor.getLong(sizeIndex);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting file size", e);
        }
        return 0;
    }

    private static int calculateQuality(long fileSize) {
        if (fileSize <= 0) return MAX_QUALITY;
        
        // For files smaller than 1MB, use higher quality
        if (fileSize < MB_THRESHOLD) {
            return MAX_QUALITY;
        }
        
        // For larger files, reduce quality based on size
        // The larger the file, the lower the quality, but never below MIN_QUALITY
        int quality = MAX_QUALITY - (int)((fileSize / MB_THRESHOLD) * 10);
        return Math.max(quality, MIN_QUALITY);
    }

    private static int calculateSampleSize(int width, int height) {
        int maxDimension = Math.max(width, height);
        int sampleSize = 1;

        if (maxDimension > MAX_IMAGE_DIMENSION) {
            final float ratio = (float) maxDimension / MAX_IMAGE_DIMENSION;
            sampleSize = (int) Math.pow(2, (int) Math.ceil(Math.log(ratio) / Math.log(2)));
        }

        return sampleSize;
    }
} 