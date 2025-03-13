package com.example.docxpert.features.splitpdf;

import android.content.Context;
import android.net.Uri;
import android.util.Log;
import com.example.docxpert.utils.SafUtils;
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader;
import com.tom_roush.pdfbox.io.MemoryUsageSetting;
import com.tom_roush.pdfbox.multipdf.Splitter;
import com.tom_roush.pdfbox.pdmodel.PDDocument;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class PdfSplitter {
    private static final String TAG = "PdfSplitter";

    public static void split(Context context, Uri inputUri, Uri outputUri, String pageRange) throws Exception {
        // Initialize PDFBox
        PDFBoxResourceLoader.init(context);

        try (InputStream inputStream = SafUtils.getInputStream(context, inputUri);
             OutputStream outputStream = SafUtils.getOutputStream(context, outputUri)) {

            if (inputStream == null || outputStream == null) {
                throw new Exception("Could not open input/output streams");
            }

            // Load the PDF document
            PDDocument document = PDDocument.load(inputStream, MemoryUsageSetting.setupTempFileOnly());

            // Parse page ranges
            List<Integer> pages = parsePageRange(pageRange, document.getNumberOfPages());

            // Create a new document with selected pages
            PDDocument outputDocument = new PDDocument();
            
            try {
                // Add selected pages to new document
                for (int pageNum : pages) {
                    outputDocument.addPage(document.getPage(pageNum - 1)); // Convert to 0-based index
                }

                // Save the new document
                outputDocument.save(outputStream);
                Log.d(TAG, "PDF split successfully");

            } finally {
                // Clean up resources
                outputDocument.close();
                document.close();
            }

        } catch (Exception e) {
            Log.e(TAG, "Error splitting PDF", e);
            throw e;
        }
    }

    private static List<Integer> parsePageRange(String pageRange, int totalPages) throws Exception {
        List<Integer> pages = new ArrayList<>();
        String[] ranges = pageRange.split(",");

        for (String range : ranges) {
            if (range.contains("-")) {
                // Handle range (e.g., "1-5")
                String[] bounds = range.split("-");
                int start = Integer.parseInt(bounds[0]);
                int end = Integer.parseInt(bounds[1]);

                if (start < 1 || end > totalPages || start > end) {
                    throw new Exception("Invalid page range: " + range);
                }

                for (int i = start; i <= end; i++) {
                    if (!pages.contains(i)) {
                        pages.add(i);
                    }
                }
            } else {
                // Handle single page
                int page = Integer.parseInt(range);
                if (page < 1 || page > totalPages) {
                    throw new Exception("Invalid page number: " + page);
                }
                if (!pages.contains(page)) {
                    pages.add(page);
                }
            }
        }

        return pages;
    }
} 