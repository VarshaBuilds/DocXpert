package com.example.docexpert;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.pdf.PdfRenderer;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

// Google ML Kit for text recognition
import com.google.android.gms.tasks.Task;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import com.google.android.material.snackbar.Snackbar;

// iText 7 imports
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.canvas.parser.PdfTextExtractor;
import com.itextpdf.kernel.pdf.canvas.parser.listener.LocationTextExtractionStrategy;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class OCRActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 100;
    private static final String[] REQUIRED_PERMISSIONS = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    private static final String[] REQUIRED_PERMISSIONS_API_33 = {
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VIDEO
    };

    private Uri selectedPdfUri;
    private TextView selectedFileTextView;
    private Button selectFileButton;
    private Button extractTextButton;
    private ProgressBar progressBar;
    private TextView statusTextView;
    private TextRecognizer textRecognizer;
    private ExecutorService executorService;

    private final ActivityResultLauncher<String> selectPdfLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    selectedPdfUri = uri;
                    String fileName = getFileNameFromUri(uri);
                    selectedFileTextView.setText("Selected file: " + fileName);
                    extractTextButton.setEnabled(true);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ocr);

        // Initialize ML Kit Text Recognizer
        textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

        // Initialize views
        selectedFileTextView = findViewById(R.id.selectedFileTextView);
        selectFileButton = findViewById(R.id.selectFileButton);
        extractTextButton = findViewById(R.id.extractTextButton);
        progressBar = findViewById(R.id.progressBar);
        statusTextView = findViewById(R.id.infoTextView);

        // Set initial state
        extractTextButton.setEnabled(false);
        progressBar.setVisibility(View.GONE);

        // Set up button click listeners
        selectFileButton.setOnClickListener(v -> checkPermissionsAndSelectFile());
        extractTextButton.setOnClickListener(v -> checkPermissionsAndExtractText());

        // Initialize executor service for background tasks
        executorService = Executors.newSingleThreadExecutor();

        // Check permissions on startup
        checkAndRequestPermissions();
    }

    private void checkAndRequestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // For Android 13+
            if (!hasPermissions(REQUIRED_PERMISSIONS_API_33)) {
                ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS_API_33, PERMISSION_REQUEST_CODE);
            }
        } else {
            // For Android 12 and below
            if (!hasPermissions(REQUIRED_PERMISSIONS)) {
                ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, PERMISSION_REQUEST_CODE);
            }
        }
    }

    private boolean hasPermissions(String[] permissions) {
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }

            if (!allGranted) {
                Toast.makeText(this, "Storage permissions are required for this app to function properly",
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    private void checkPermissionsAndSelectFile() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (hasPermissions(REQUIRED_PERMISSIONS_API_33)) {
                selectPdfFile();
            } else {
                checkAndRequestPermissions();
                Toast.makeText(this, "Storage permissions are required to select files", Toast.LENGTH_SHORT).show();
            }
        } else {
            if (hasPermissions(REQUIRED_PERMISSIONS)) {
                selectPdfFile();
            } else {
                checkAndRequestPermissions();
                Toast.makeText(this, "Storage permissions are required to select files", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void checkPermissionsAndExtractText() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (hasPermissions(REQUIRED_PERMISSIONS_API_33)) {
                extractTextFromPdf();
            } else {
                checkAndRequestPermissions();
                Toast.makeText(this, "Storage permissions are required to extract text", Toast.LENGTH_SHORT).show();
            }
        } else {
            if (hasPermissions(REQUIRED_PERMISSIONS)) {
                extractTextFromPdf();
            } else {
                checkAndRequestPermissions();
                Toast.makeText(this, "Storage permissions are required to extract text", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void selectPdfFile() {
        selectPdfLauncher.launch("application/pdf");
    }

    private String getFileNameFromUri(Uri uri) {
        String result = uri.getPath();
        int cut = result.lastIndexOf('/');
        if (cut != -1) {
            result = result.substring(cut + 1);
        }
        return result;
    }

    private void extractTextFromPdf() {
        if (selectedPdfUri == null) {
            Toast.makeText(this, "Please select a PDF file first", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        extractTextButton.setEnabled(false);
        statusTextView.setText("Processing PDF...");

        executorService.execute(() -> {
            StringBuilder textBuilder = new StringBuilder();

            try {
                // First try to extract text directly from PDF
                runOnUiThread(() -> statusTextView.setText("Extracting embedded text from PDF..."));
                extractTextDirectly(textBuilder);

                // If direct extraction yields little text, use OCR on rendered pages
                if (textBuilder.length() < 100) {
                    textBuilder.setLength(0); // Clear previous results
                    runOnUiThread(
                            () -> statusTextView.setText("PDF appears to be scanned or has little text. Using OCR..."));
                    extractTextWithOCR(textBuilder);
                }

                // Check if we got any text
                if (textBuilder.length() == 0) {
                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        extractTextButton.setEnabled(true);
                        statusTextView.setText("No text could be extracted from this PDF.");
                        Toast.makeText(OCRActivity.this,
                                "No text could be extracted from this PDF.",
                                Toast.LENGTH_LONG).show();
                    });
                    return;
                }

                // Save extracted text to file
                runOnUiThread(() -> statusTextView.setText("Saving extracted text..."));
                String extractedText = textBuilder.toString();
                File outputFile = saveTextToFile(extractedText);

                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    extractTextButton.setEnabled(true);

                    if (outputFile != null) {
                        Snackbar.make(findViewById(android.R.id.content),
                                "Text extracted successfully!",
                                Snackbar.LENGTH_LONG)
                                .setAction("Open", v -> openTextFile(outputFile))
                                .show();
                    } else {
                        Toast.makeText(OCRActivity.this,
                                "Failed to save extracted text",
                                Toast.LENGTH_SHORT).show();
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    extractTextButton.setEnabled(true);
                    statusTextView.setText("Error occurred during text extraction: " + e.getMessage());
                    Toast.makeText(OCRActivity.this,
                            "Error extracting text: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void extractTextDirectly(StringBuilder textBuilder) throws IOException {
        try {
            // Get input stream from URI
            InputStream inputStream = getContentResolver().openInputStream(selectedPdfUri);
            if (inputStream == null) {
                throw new IOException("Could not open PDF file");
            }

            // Create PDF reader and document
            PdfReader reader = new PdfReader(inputStream);
            PdfDocument pdfDoc = new PdfDocument(reader);

            // Extract text from each page
            int pageCount = pdfDoc.getNumberOfPages();
            for (int i = 1; i <= pageCount; i++) {
                final int pageNum = i;
                runOnUiThread(
                        () -> statusTextView.setText("Extracting text from page " + pageNum + " of " + pageCount));

                // Use LocationTextExtractionStrategy for better text extraction
                LocationTextExtractionStrategy strategy = new LocationTextExtractionStrategy();
                String pageText = PdfTextExtractor.getTextFromPage(pdfDoc.getPage(i), strategy);
                textBuilder.append(pageText).append("\n\n");
            }

            // Close resources
            pdfDoc.close();
            reader.close();
            inputStream.close();
        } catch (Exception e) {
            Log.e("OCRActivity", "Error extracting text directly", e);
            throw new IOException("Error extracting text: " + e.getMessage(), e);
        }
    }

    private void extractTextWithOCR(StringBuilder textBuilder) throws IOException {
        ParcelFileDescriptor fileDescriptor = getContentResolver().openFileDescriptor(selectedPdfUri, "r");
        if (fileDescriptor == null) {
            throw new IOException("Could not open PDF file");
        }

        PdfRenderer pdfRenderer = new PdfRenderer(fileDescriptor);
        int pageCount = pdfRenderer.getPageCount();

        for (int i = 0; i < pageCount; i++) {
            final int pageNum = i + 1;
            runOnUiThread(() -> statusTextView.setText("OCR processing page " + pageNum + " of " + pageCount));

            PdfRenderer.Page page = pdfRenderer.openPage(i);

            // Create bitmap with appropriate resolution for OCR
            Bitmap bitmap = Bitmap.createBitmap(
                    page.getWidth() * 2,
                    page.getHeight() * 2,
                    Bitmap.Config.ARGB_8888);

            // Render the page to the bitmap
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT);

            // Perform OCR on the bitmap using ML Kit
            InputImage image = InputImage.fromBitmap(bitmap, 0);
            Task<Text> result = textRecognizer.process(image);

            // Wait for the OCR task to complete
            while (!result.isComplete()) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IOException("OCR processing interrupted", e);
                }
            }

            // Get the OCR result
            if (result.isSuccessful()) {
                Text recognizedText = result.getResult();
                textBuilder.append(recognizedText.getText()).append("\n\n");
            } else {
                Exception exception = result.getException();
                if (exception != null) {
                    Log.e("OCRActivity", "Text recognition failed", exception);
                }
                textBuilder.append("OCR failed for page ").append(pageNum).append("\n\n");
            }

            // Clean up
            bitmap.recycle();
            page.close();
        }

        pdfRenderer.close();
        fileDescriptor.close();
    }

    private File saveTextToFile(String text) {
        try {
            File outputFile;

            // For Android 10 (Q) and above, use app-specific storage
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                // Save to app's external files directory
                File appDir = new File(getExternalFilesDir(null), "OCR_Results");
                if (!appDir.exists()) {
                    appDir.mkdirs();
                }

                // Create a unique filename with timestamp
                String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
                outputFile = new File(appDir, "OCR_" + timeStamp + ".txt");

                // Write the text to the file
                FileOutputStream fos = new FileOutputStream(outputFile);
                fos.write(text.getBytes());
                fos.close();

                // Make the file visible in the media store
                MediaScannerConnection.scanFile(this,
                        new String[] { outputFile.getAbsolutePath() },
                        new String[] { "text/plain" },
                        null);
            } else {
                // For older Android versions, try to save to Downloads folder
                File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                if (!downloadsDir.exists()) {
                    // Fall back to Documents folder if Downloads is not available
                    downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
                    if (!downloadsDir.exists()) {
                        // Create the directory
                        downloadsDir.mkdirs();
                    }
                }

                // Create a unique filename with timestamp
                String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
                outputFile = new File(downloadsDir, "OCR_" + timeStamp + ".txt");

                // Write the text to the file
                FileOutputStream fos = new FileOutputStream(outputFile);
                fos.write(text.getBytes());
                fos.close();

                // Make the file visible in the media store
                MediaScannerConnection.scanFile(this,
                        new String[] { outputFile.getAbsolutePath() },
                        new String[] { "text/plain" },
                        null);
            }

            // Notify the user where the file was saved
            final String finalPath = outputFile.getAbsolutePath();
            runOnUiThread(() -> {
                statusTextView.setText("Text saved to: " + finalPath);
            });

            return outputFile;
        } catch (IOException e) {
            e.printStackTrace();
            runOnUiThread(() -> {
                Toast.makeText(OCRActivity.this,
                        "Error saving file: " + e.getMessage(),
                        Toast.LENGTH_LONG).show();
            });
            return null;
        }
    }

    private void openTextFile(File file) {
        try {
            // Create a content URI using FileProvider
            Uri contentUri = androidx.core.content.FileProvider.getUriForFile(
                    this,
                    getApplicationContext().getPackageName() + ".fileprovider",
                    file);

            // Create an intent to view the file
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(contentUri, "text/plain");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            // Check if there's an app that can handle this intent
            if (intent.resolveActivity(getPackageManager()) != null) {
                startActivity(Intent.createChooser(intent, "Open with"));
            } else {
                // Try with a more generic mime type
                Intent genericIntent = new Intent(Intent.ACTION_VIEW);
                genericIntent.setDataAndType(contentUri, "*/*");
                genericIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                if (genericIntent.resolveActivity(getPackageManager()) != null) {
                    startActivity(Intent.createChooser(genericIntent, "Open with"));
                } else {
                    // No app found, show a more helpful message
                    Toast.makeText(this,
                            "No app found to open text files. Please install a text editor from the Play Store.",
                            Toast.LENGTH_LONG).show();

                    // Optionally, open Play Store to search for text editors
                    try {
                        Intent marketIntent = new Intent(Intent.ACTION_VIEW);
                        marketIntent.setData(Uri.parse("market://search?q=text editor"));
                        startActivity(marketIntent);
                    } catch (Exception e) {
                        // If Play Store app is not available, open in browser
                        Intent browserIntent = new Intent(Intent.ACTION_VIEW);
                        browserIntent.setData(Uri.parse("https://play.google.com/store/search?q=text editor"));
                        startActivity(browserIntent);
                    }
                }
            }
        } catch (Exception e) {
            Log.e("OCRActivity", "Error opening text file", e);
            Toast.makeText(this,
                    "Error opening file: " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executorService != null) {
            executorService.shutdown();
        }
        if (textRecognizer != null) {
            textRecognizer.close();
        }
    }
}