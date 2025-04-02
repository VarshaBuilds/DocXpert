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
import com.itextpdf.text.Element;
import com.itextpdf.text.Chunk;
import com.itextpdf.text.pdf.PdfWriter;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Paragraph;

import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.usermodel.CharacterRun;
import org.apache.poi.hwpf.usermodel.Picture;
import org.apache.poi.hwpf.usermodel.Range;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
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
import java.util.*;

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
            "application/msword",                   // .doc
            "application/vnd.ms-word",             // .doc
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"  // .docx
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

                String fileName = SafUtils.getFileName(this, selectedWordUri);
                if (fileName.toLowerCase().endsWith(".docx")) {
                    processDocx(document, documentContent);
                } else {
                    processDoc(document, documentContent);
                }

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
        
        // Create a list to store all elements with their positions
        List<DocElement> elements = new ArrayList<>();
        
        // Process tables and store their positions
        for (XWPFTable table : docx.getTables()) {
            int tableIndex = docx.getPosOfTable(table);
            elements.add(new DocElement(tableIndex, () -> {
                PdfPTable pdfTable = new PdfPTable(table.getNumberOfRows() > 0 ? table.getRow(0).getTableCells().size() : 1);
                pdfTable.setWidthPercentage(100);
                
                // Process table rows
                for (XWPFTableRow row : table.getRows()) {
                    for (XWPFTableCell cell : row.getTableCells()) {
                        PdfPCell pdfCell = new PdfPCell();
                        
                        // Process cell content
                        for (XWPFParagraph cellParagraph : cell.getParagraphs()) {
                            Paragraph cellContent = new Paragraph();
                            
                            for (XWPFRun run : cellParagraph.getRuns()) {
                                // Process text in cell
                                String text = run.getText(0);
                                if (text != null && !text.trim().isEmpty()) {
                                    Font font = FontFactory.getFont(FontFactory.HELVETICA, 12);
                                    if (run.isBold()) font.setStyle(Font.BOLD);
                                    if (run.isItalic()) font.setStyle(font.getStyle() | Font.ITALIC);
                                    cellContent.add(new Chunk(text, font));
                                }
                                
                                // Process images in cell
                                for (XWPFPicture picture : run.getEmbeddedPictures()) {
                                    try {
                                        byte[] imageData = picture.getPictureData().getData();
                                        Image image = Image.getInstance(imageData);
                                        
                                        // Scale image to fit cell
                                        image.scaleToFit(pdfTable.getAbsoluteWidths()[0] - 10, 100);
                                        cellContent.add(new Chunk(image, 0, 0, true));
                                    } catch (Exception e) {
                                        e.printStackTrace();
                                    }
                                }
                            }
                            
                            pdfCell.addElement(cellContent);
                        }
                        
                        // Set cell alignment
                        pdfCell.setHorizontalAlignment(Element.ALIGN_LEFT);
                        pdfCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                        
                        pdfTable.addCell(pdfCell);
                    }
                }
                
                document.add(pdfTable);
                document.add(new Paragraph("\n"));
            }));
        }
        
        // Process paragraphs and store their positions
        for (XWPFParagraph paragraph : docx.getParagraphs()) {
            int parIndex = docx.getPosOfParagraph(paragraph);
            elements.add(new DocElement(parIndex, () -> {
                Paragraph pdfPar = new Paragraph();
                
                // Process runs (text and inline images)
                for (XWPFRun run : paragraph.getRuns()) {
                    // Process text
                    String text = run.getText(0);
                    if (text != null && !text.trim().isEmpty()) {
                        Font font = FontFactory.getFont(FontFactory.HELVETICA, 12);
                        if (run.isBold()) font.setStyle(Font.BOLD);
                        if (run.isItalic()) font.setStyle(font.getStyle() | Font.ITALIC);
                        if (run.isStrikeThrough()) font.setStyle(font.getStyle() | Font.STRIKETHRU);
                        if (run.getUnderline() != UnderlinePatterns.NONE) font.setStyle(font.getStyle() | Font.UNDERLINE);
                        
                        Chunk chunk = new Chunk(text, font);
                        pdfPar.add(chunk);
                    }
                    
                    // Process inline images
                    List<XWPFPicture> pictures = run.getEmbeddedPictures();
                    for (XWPFPicture picture : pictures) {
                        try {
                            byte[] imageData = picture.getPictureData().getData();
                            Image image = Image.getInstance(imageData);
                            
                            // Try to get original dimensions
                            try {
                                org.apache.xmlbeans.XmlObject xmlObject = picture.getCTPicture();
                                if (xmlObject != null) {
                                    // Try to maintain original size if possible
                                    BitmapFactory.Options options = new BitmapFactory.Options();
                                    options.inJustDecodeBounds = true;
                                    BitmapFactory.decodeByteArray(imageData, 0, imageData.length, options);
                                    
                                    if (options.outWidth > 0 && options.outHeight > 0) {
                                        float width = options.outWidth * 72f / 96f;
                                        float height = options.outHeight * 72f / 96f;
                                        
                                        if (width > PageSize.A4.getWidth() - 100) {
                                            float ratio = (PageSize.A4.getWidth() - 100) / width;
                                            width *= ratio;
                                            height *= ratio;
                                        }
                                        
                                        image.scaleToFit(width, height);
                                    }
                                }
                            } catch (Exception e) {
                                // If we can't get original dimensions, use default scaling
                                image.scaleToFit(300, 300);
                            }
                            
                            // Add image inline with text
                            Chunk imageChunk = new Chunk(image, 0, 0, true);
                            pdfPar.add(imageChunk);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
                
                // Set paragraph alignment
                switch (paragraph.getAlignment()) {
                    case CENTER:
                        pdfPar.setAlignment(Element.ALIGN_CENTER);
                        break;
                    case RIGHT:
                        pdfPar.setAlignment(Element.ALIGN_RIGHT);
                        break;
                    case BOTH:
                        pdfPar.setAlignment(Element.ALIGN_JUSTIFIED);
                        break;
                    default:
                        pdfPar.setAlignment(Element.ALIGN_LEFT);
                }
                
                // Add spacing
                pdfPar.setSpacingBefore(paragraph.getSpacingBefore() / 20f);
                pdfPar.setSpacingAfter(paragraph.getSpacingAfter() / 20f);
                
                if (!pdfPar.isEmpty()) {
                    document.add(pdfPar);
                }
            }));
        }
        
        // Sort elements by their position and add them to the document
        Collections.sort(elements);
        for (DocElement element : elements) {
            element.add();
        }
        
        docx.close();
    }

    private void processDoc(Document document, byte[] content) throws Exception {
        POIFSFileSystem fs = new POIFSFileSystem(new ByteArrayInputStream(content));
        HWPFDocument doc = new HWPFDocument(fs);
        Range range = doc.getRange();

        // Process paragraphs and their content
        for (int i = 0; i < range.numParagraphs(); i++) {
            org.apache.poi.hwpf.usermodel.Paragraph par = range.getParagraph(i);
            com.itextpdf.text.Paragraph pdfPar = new com.itextpdf.text.Paragraph();
            
            // Process text runs
            for (int j = 0; j < par.numCharacterRuns(); j++) {
                CharacterRun run = par.getCharacterRun(j);
                String text = run.text();
                
                if (text != null && !text.trim().isEmpty()) {
                    // Create font with styling
                    Font font = FontFactory.getFont(FontFactory.HELVETICA, 12);
                    if (run.isBold()) font.setStyle(Font.BOLD);
                    if (run.isItalic()) font.setStyle(font.getStyle() | Font.ITALIC);
                    if (run.getUnderlineCode() > 0) font.setStyle(font.getStyle() | Font.UNDERLINE);
                    
                    Chunk chunk = new Chunk(text, font);
                    pdfPar.add(chunk);
                }
            }

            // Add paragraph to document if it has content
            if (!pdfPar.isEmpty()) {
                // Set paragraph alignment
                switch (par.getJustification()) {
                    case 1:
                        pdfPar.setAlignment(Element.ALIGN_CENTER);
                        break;
                    case 2:
                        pdfPar.setAlignment(Element.ALIGN_RIGHT);
                        break;
                    case 3:
                        pdfPar.setAlignment(Element.ALIGN_JUSTIFIED);
                        break;
                    default:
                        pdfPar.setAlignment(Element.ALIGN_LEFT);
                }
                
                // Add spacing
                pdfPar.setSpacingBefore(par.getSpacingBefore() / 20f);
                pdfPar.setSpacingAfter(par.getSpacingAfter() / 20f);
                
                document.add(pdfPar);
            }
        }

        // Process pictures
        List<Picture> pictures = doc.getPicturesTable().getAllPictures();
        if (pictures != null && !pictures.isEmpty()) {
            for (Picture pic : pictures) {
                byte[] imageData = pic.getContent();
                if (imageData != null && imageData.length > 0) {
                    try {
                        Image image = Image.getInstance(imageData);
                        
                        // Get image dimensions using BitmapFactory
                        BitmapFactory.Options options = new BitmapFactory.Options();
                        options.inJustDecodeBounds = true;
                        BitmapFactory.decodeByteArray(imageData, 0, imageData.length, options);
                        
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
                        document.add(image);
                        document.add(new com.itextpdf.text.Paragraph("\n"));
                    } catch (Exception e) {
                        // Skip this image if there's an error
                        e.printStackTrace();
                    }
                }
            }
        }

        doc.close();
        fs.close();
    }

    // Helper class to maintain document element order
    private static class DocElement implements Comparable<DocElement> {
        private final int position;
        private final DocumentElement element;

        DocElement(int position, DocumentElement element) {
            this.position = position;
            this.element = element;
        }

        void add() throws Exception {
            element.add();
        }

        @Override
        public int compareTo(DocElement other) {
            return Integer.compare(this.position, other.position);
        }
    }

    private interface DocumentElement {
        void add() throws Exception;
    }

    private void processRunImages(Document document, XWPFRun run, com.itextpdf.text.Paragraph paragraph) throws Exception {
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