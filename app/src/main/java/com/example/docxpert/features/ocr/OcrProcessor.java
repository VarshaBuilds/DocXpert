package com.example.docxpert.features.ocr;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Log;

import com.example.docxpert.utils.SafUtils;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class OcrProcessor {
    private static final String TAG = "OcrProcessor";
    private final TextRecognizer textRecognizer;

    public OcrProcessor() {
        textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
    }

    public interface OcrCallback {
        void onSuccess(String extractedText);
        void onError(Exception e);
    }

    public void processImage(Context context, Uri imageUri, OcrCallback callback) {
        try {
            // Load image from URI using SAF
            InputStream inputStream = SafUtils.getInputStream(context, imageUri);
            if (inputStream == null) {
                throw new Exception("Could not open image file");
            }

            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            if (bitmap == null) {
                throw new Exception("Could not decode image");
            }

            // Create InputImage from bitmap
            InputImage image = InputImage.fromBitmap(bitmap, 0);

            // Process the image
            textRecognizer.process(image)
                    .addOnSuccessListener(text -> {
                        String extractedText = processTextBlocks(text);
                        callback.onSuccess(extractedText);
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "OCR failed", e);
                        callback.onError(e);
                    });

        } catch (Exception e) {
            Log.e(TAG, "Error processing image", e);
            callback.onError(e);
        }
    }

    private String processTextBlocks(Text text) {
        StringBuilder result = new StringBuilder();
        
        for (Text.TextBlock block : text.getTextBlocks()) {
            result.append(block.getText()).append("\n\n");
            
            // Process lines within each block
            for (Text.Line line : block.getLines()) {
                result.append(line.getText()).append("\n");
            }
        }
        
        return result.toString().trim();
    }
} 