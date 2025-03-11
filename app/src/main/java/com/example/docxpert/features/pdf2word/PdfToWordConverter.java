package com.example.docxpert.features.pdf2word;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;

import java.io.InputStream;
import java.io.OutputStream;

public class PdfToWordConverter {
    private static final String TAG = "PdfToWordConverter";

    public static void convert(Context context, Uri pdfUri, Uri wordUri) throws Exception {
        try (InputStream pdfInputStream = context.getContentResolver().openInputStream(pdfUri);
             OutputStream wordOutputStream = context.getContentResolver().openOutputStream(wordUri)) {

            if (pdfInputStream == null || wordOutputStream == null) {
                throw new Exception("Could not open input/output streams");
            }

            // Load PDF document
            PDDocument pdfDocument = PDDocument.load(pdfInputStream);
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);

            // Create Word document
            XWPFDocument wordDocument = new XWPFDocument();

            // Extract text from PDF and write to Word
            String text = stripper.getText(pdfDocument);
            String[] paragraphs = text.split("\\r?\\n\\r?\\n");

            for (String paragraph : paragraphs) {
                if (!paragraph.trim().isEmpty()) {
                    XWPFParagraph docxParagraph = wordDocument.createParagraph();
                    XWPFRun run = docxParagraph.createRun();
                    run.setText(paragraph.trim());
                    run.addBreak();
                }
            }

            // Save Word document
            wordDocument.write(wordOutputStream);
            wordDocument.close();
            pdfDocument.close();

            Log.d(TAG, "PDF to Word conversion completed successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error converting PDF to Word", e);
            throw e;
        }
    }
} 