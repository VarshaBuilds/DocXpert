package com.example.docxpert.features.wordtopdf;

import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.docxpert.R;
import com.example.docxpert.utils.SafUtils;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.itextpdf.text.Document;
import com.itextpdf.text.Font;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.Image;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Element;
import com.itextpdf.text.Chunk;
import com.itextpdf.text.pdf.PdfWriter;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.BaseColor;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;
import org.apache.poi.xwpf.usermodel.XWPFPicture;
import org.apache.poi.xwpf.usermodel.UnderlinePatterns;

import java.io.*;
import java.util.List;

public class WordToPdfActivity extends AppCompatActivity {
    private TextView selectedFileText;
    private Button selectButton;
    private Button convertButton;
    private LinearProgressIndicator progressIndicator;
    private Uri selectedWordUri;
    private byte[] documentContent;

    private ActivityResultLauncher<String[]> pickWordLauncher;
    private ActivityResultLauncher<String> savePdfLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_word_to_pdf);

        setupToolbar();
        initializeViews();
        setupLaunchers();
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
    }

    private void initializeViews() {
        selectedFileText = findViewById(R.id.selectedFileText);
        selectButton = findViewById(R.id.selectButton);
        convertButton = findViewById(R.id.convertButton);
        progressIndicator = findViewById(R.id.progressIndicator);

        selectButton.setOnClickListener(v -> pickWordLauncher.launch(new String[]{
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"  // .docx only
        }));
        convertButton.setOnClickListener(v -> convertToPdf());
        convertButton.setEnabled(false);
    }

    private void setupLaunchers() {
        pickWordLauncher = SafUtils.createOpenFileLauncher(
            this,
            new SafUtils.FileOperationCallback() {
                @Override
                public void onSuccess(Uri uri) {
                    selectedWordUri = uri;
                    String fileName = SafUtils.getFileName(WordToPdfActivity.this, uri);
                    selectedFileText.setText(getString(R.string.file_selected, fileName));
                    convertButton.setEnabled(true);
                }

                @Override
                public void onFailure(Exception e) {
                    Toast.makeText(WordToPdfActivity.this, 
                        getString(R.string.error_selecting_file, e.getMessage()), 
                        Toast.LENGTH_SHORT).show();
                }
            }
        );

        savePdfLauncher = SafUtils.createSaveFileLauncher(
            this,
            "converted_document",
            ".pdf",
            new SafUtils.FileOperationCallback() {
                @Override
                public void onSuccess(Uri uri) {
                    savePdfFile(uri);
                }

                @Override
                public void onFailure(Exception e) {
                    Toast.makeText(WordToPdfActivity.this, 
                        getString(R.string.error_creating_file, e.getMessage()), 
                        Toast.LENGTH_SHORT).show();
                }
            }
        );
    }

    private void convertToPdf() {
        if (selectedWordUri == null) {
            Toast.makeText(this, R.string.select_word_to_convert, Toast.LENGTH_SHORT).show();
            return;
        }

        progressIndicator.setVisibility(View.VISIBLE);
        convertButton.setEnabled(false);

        new Thread(() -> {
            try {
                // Read Word file content
                InputStream inputStream = SafUtils.getInputStream(this, selectedWordUri);
                ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                int nRead;
                byte[] data = new byte[1024];
                while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
                    buffer.write(data, 0, nRead);
                }
                buffer.flush();
                documentContent = buffer.toByteArray();
                inputStream.close();
                buffer.close();

                // Launch file saver
                final String fileName = SafUtils.getFileName(this, selectedWordUri);
                if (fileName != null && !fileName.isEmpty()) {
                    final String pdfFileName = fileName.replaceAll("\\.(doc|docx)$", ".pdf");
                    runOnUiThread(() -> savePdfLauncher.launch(pdfFileName));
                }

            } catch (Exception e) {
                runOnUiThread(() -> {
                    progressIndicator.setVisibility(View.GONE);
                    convertButton.setEnabled(true);
                    Toast.makeText(this, getString(R.string.word_to_pdf_error) + ": " + e.getMessage(), 
                        Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    private void savePdfFile(Uri uri) {
        try {
            OutputStream outputStream = SafUtils.getOutputStream(this, uri);
            if (outputStream != null && documentContent != null) {
                Document document = new Document(PageSize.A4, 50, 50, 50, 50);
                PdfWriter.getInstance(document, outputStream);
                document.open();
                processDocx(document, documentContent);
                document.close();
                outputStream.close();
                Toast.makeText(this, R.string.word_to_pdf_success, Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, getString(R.string.word_to_pdf_error) + ": " + e.getMessage(), 
                Toast.LENGTH_SHORT).show();
        } finally {
            progressIndicator.setVisibility(View.GONE);
            convertButton.setEnabled(true);
        }
    }

    private void processDocx(Document document, byte[] content) throws Exception {
        XWPFDocument docx = new XWPFDocument(new ByteArrayInputStream(content));
        
        // Process tables first
        for (XWPFTable table : docx.getTables()) {
            int numColumns = table.getRow(0).getTableCells().size();
            PdfPTable pdfTable = new PdfPTable(numColumns);
            pdfTable.setWidthPercentage(100);
            
            for (XWPFTableRow row : table.getRows()) {
                for (XWPFTableCell cell : row.getTableCells()) {
                    PdfPCell pdfCell = new PdfPCell();
                    
                    for (XWPFParagraph cellParagraph : cell.getParagraphs()) {
                        Paragraph p = new Paragraph();
                        p.setAlignment(getAlignment(cellParagraph.getAlignment()));
                        
                        for (XWPFRun run : cellParagraph.getRuns()) {
                            Font font = createFont(run);
                            String text = run.getText(0);
                            if (text != null) {
                                Chunk chunk = new Chunk(text, font);
                                p.add(chunk);
                            }
                            processRunImages(document, run, p);
                        }
                        pdfCell.addElement(p);
                    }
                    pdfTable.addCell(pdfCell);
                }
            }
            document.add(pdfTable);
            document.add(new Paragraph("\n"));
        }
        
        // Process paragraphs and text with inline images
        for (XWPFParagraph paragraph : docx.getParagraphs()) {
            Paragraph pdfParagraph = new Paragraph();
            pdfParagraph.setAlignment(getAlignment(paragraph.getAlignment()));
            pdfParagraph.setSpacingBefore(paragraph.getSpacingBefore() / 20f);
            pdfParagraph.setSpacingAfter(paragraph.getSpacingAfter() / 20f);
            
            for (XWPFRun run : paragraph.getRuns()) {
                Font font = createFont(run);
                String text = run.getText(0);
                if (text != null) {
                    Chunk chunk = new Chunk(text, font);
                    if (run.getTextPosition() > 0) {
                        chunk.setTextRise(run.getTextPosition() / 2f);
                    }
                    pdfParagraph.add(chunk);
                }
                processRunImages(document, run, pdfParagraph);
            }
            
            if (!pdfParagraph.isEmpty()) {
                document.add(pdfParagraph);
            }
        }
        
        docx.close();
    }

    private void processRunImages(Document document, XWPFRun run, Paragraph paragraph) throws Exception {
        List<XWPFPicture> pictures = run.getEmbeddedPictures();
        if (pictures != null && !pictures.isEmpty()) {
            for (XWPFPicture picture : pictures) {
                byte[] pictureData = picture.getPictureData().getData();
                if (pictureData != null && pictureData.length > 0) {
                    Image image = Image.getInstance(pictureData);
                    
                    // Get image dimensions using BitmapFactory
                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inJustDecodeBounds = true;
                    BitmapFactory.decodeByteArray(pictureData, 0, pictureData.length, options);
                    
                    if (options.outWidth > 0 && options.outHeight > 0) {
                        float width = options.outWidth * 72f / 96f;
                        float height = options.outHeight * 72f / 96f;
                        
                        if (width > PageSize.A4.getWidth() - 100) {
                            float ratio = (PageSize.A4.getWidth() - 100) / width;
                            width *= ratio;
                            height *= ratio;
                        }
                        
                        image.scaleToFit(width, height);
                    } else {
                        image.scaleToFit(300, 300);
                    }
                    
                    image.setAlignment(Image.ALIGN_CENTER);
                    paragraph.add(new Chunk(image, 0, 0, true));
                }
            }
        }
    }

    private int getAlignment(ParagraphAlignment alignment) {
        if (alignment == null) return Element.ALIGN_LEFT;
        
        switch (alignment) {
            case CENTER:
                return Element.ALIGN_CENTER;
            case RIGHT:
                return Element.ALIGN_RIGHT;
            case BOTH:
                return Element.ALIGN_JUSTIFIED;
            default:
                return Element.ALIGN_LEFT;
        }
    }

    private Font createFont(XWPFRun run) {
        Font font = FontFactory.getFont(FontFactory.HELVETICA, 12);
        
        if (run.isBold()) {
            font.setStyle(Font.BOLD);
        }
        if (run.isItalic()) {
            font.setStyle(font.getStyle() | Font.ITALIC);
        }
        if (run.getUnderline() != UnderlinePatterns.NONE) {
            font.setStyle(font.getStyle() | Font.UNDERLINE);
        }
        
        String color = run.getColor();
        if (color != null && !color.isEmpty()) {
            try {
                int colorValue = Integer.parseInt(color.replace("#", ""), 16);
                font.setColor(new BaseColor(colorValue));
            } catch (NumberFormatException e) {
                // Use default color if parsing fails
            }
        }
        
        return font;
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
} 