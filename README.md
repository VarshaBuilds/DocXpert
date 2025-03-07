# DocXpert

DocXpert is an Android application for PDF document manipulation and processing.

## Features

- **Merge PDF**: Combine multiple PDF files into a single document
- **Split PDF**: Divide a PDF document into multiple files
- **Compress PDF**: Reduce the file size of PDF documents
- **PDF to Word**: Convert PDF documents to Word format
- **PDF to Image**: Convert PDF pages to image files
- **OCR (Optical Character Recognition)**: Extract text from PDF documents

## OCR Functionality

The OCR feature allows you to extract text from PDF documents, even if they contain scanned images or non-selectable text. This is particularly useful for:

- Extracting text from scanned documents
- Making text-based searches in image-based PDFs
- Converting image-based PDFs to editable text

### About the OCR Technology

This app uses Google ML Kit for text recognition, which provides powerful on-device OCR capabilities without requiring additional downloads or setup. The OCR functionality works as follows:

1. First, the app attempts to extract embedded text directly from the PDF
2. If little or no text is found, it renders the PDF pages as images and applies OCR
3. The extracted text is saved as a text file in your Documents folder

### Using OCR

1. Open the app and tap on "OCR - Extract Text"
2. Select a PDF file from your device
3. Tap "Extract Text" to begin the OCR process
4. The extracted text will be saved as a text file in your Documents folder

### Troubleshooting OCR

If you encounter issues with the OCR functionality:

1. **Extraction quality**: OCR works best with clear, high-resolution documents
2. **Performance**: OCR processing can be resource-intensive and may take time for large documents
3. **Language support**: The OCR is optimized for Latin-based languages by default

## Requirements

- Android 8.0 (API level 26) or higher
- Storage permissions for reading and writing files
- Google Play Services (for ML Kit text recognition)

## Development

This project uses:
- Kotlin DSL for Gradle build scripts
- iText for PDF processing
- Google ML Kit for OCR functionality
- Material Design components for UI

## Building from GitHub

If you've cloned this repository from GitHub, make sure to:

1. Open the project in Android Studio
2. Sync the project with Gradle files
3. Ensure all dependencies are properly resolved
4. Build and run the application

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.
