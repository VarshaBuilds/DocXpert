package com.example.docexpert;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.itextpdf.kernel.pdf.*;
import com.itextpdf.kernel.pdf.canvas.parser.PdfTextExtractor;
import com.itextpdf.kernel.pdf.xobject.PdfImageXObject;
import org.apache.poi.util.Units;
import org.apache.poi.xwpf.usermodel.*;

import java.io.*;
import java.util.Map;

public class PDFToWordActivity extends AppCompatActivity {
    private static final int REQUEST_CODE_PICK_PDF = 101;
    private Uri selectedFileUri;
    private TextView tvSelectedFile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pdfto_word);

        tvSelectedFile = findViewById(R.id.tvSelectedFile);
        Button btnSelectPDF = findViewById(R.id.btnSelectPDF);
        Button btnConvert = findViewById(R.id.btnConvert);

        btnSelectPDF.setOnClickListener(v -> selectPDF());
        btnConvert.setOnClickListener(v -> {
            if (selectedFileUri != null) {
                convertPDFToWord();
            } else {
                Toast.makeText(this, "Please select a PDF first!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void selectPDF() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("application/pdf");
        startActivityForResult(Intent.createChooser(intent, "Select PDF"), REQUEST_CODE_PICK_PDF);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_PICK_PDF && resultCode == Activity.RESULT_OK && data.getData() != null) {
            selectedFileUri = data.getData();
            tvSelectedFile.setText("Selected File: " + selectedFileUri.getLastPathSegment());
        }
    }

    private void convertPDFToWord() {
        try {
            InputStream inputStream = getContentResolver().openInputStream(selectedFileUri);
            if (inputStream == null) {
                Toast.makeText(this, "Error reading file!", Toast.LENGTH_SHORT).show();
                return;
            }

            PdfReader reader = new PdfReader(inputStream);
            PdfDocument pdfDocument = new PdfDocument(reader);
            XWPFDocument wordDocument = new XWPFDocument();
            File outputFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "ConvertedWord.docx");

            FileOutputStream out = new FileOutputStream(outputFile);

            for (int i = 1; i <= pdfDocument.getNumberOfPages(); i++) {
                // Extract text
                String text = PdfTextExtractor.getTextFromPage(pdfDocument.getPage(i));
                XWPFParagraph paragraph = wordDocument.createParagraph();
                XWPFRun run = paragraph.createRun();
                run.setText(text);
                run.addBreak();

                // Extract images
                PdfDictionary pageDict = pdfDocument.getPage(i).getPdfObject();
                PdfDictionary resources = pageDict.getAsDictionary(PdfName.Resources);
                if (resources != null) {
                    PdfDictionary xObjects = resources.getAsDictionary(PdfName.XObject);
                    if (xObjects != null) {
                        for (PdfName key : xObjects.keySet()) {
                            PdfObject obj = xObjects.get(key);
                            if (obj instanceof PdfStream) {
                                PdfStream stream = (PdfStream) obj;
                                if (PdfName.Image.equals(stream.getAsName(PdfName.Subtype))) {
                                    PdfImageXObject image = new PdfImageXObject(stream);
                                    byte[] imageBytes = image.getImageBytes();

                                    // Save image temporarily
                                    File imageFile = new File(getCacheDir(), "temp_image.jpg");
                                    FileOutputStream imageOut = new FileOutputStream(imageFile);
                                    imageOut.write(imageBytes);
                                    imageOut.close();

                                    // Add image to Word
                                    InputStream imageStream = new FileInputStream(imageFile);
                                    XWPFRun imageRun = wordDocument.createParagraph().createRun();
                                    int format = XWPFDocument.PICTURE_TYPE_JPEG;

                                    imageRun.addPicture(imageStream, format, "Image", Units.toEMU(300), Units.toEMU(200));
                                    imageStream.close();
                                }
                            }
                        }
                    }
                }
            }

            wordDocument.write(out);
            out.close();
            reader.close();

            Toast.makeText(this, "Converted! File saved at:\n" + outputFile.getAbsolutePath(), Toast.LENGTH_LONG).show();
            Log.d("DEBUG", "Word file saved at: " + outputFile.getAbsolutePath());
        } catch (Exception e) {
            Log.e("ERROR", "Conversion failed: " + e.getMessage());
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}
