# DocXpert

![DocXpert Logo](app/src/main/res/mipmap-xxxhdpi/ic_launcher.png)

## Overview

DocXpert is a comprehensive Android application designed to provide users with a powerful suite of document management and manipulation tools. The application focuses primarily on PDF operations, image processing, and document conversion features, making it a versatile tool for document handling on Android devices.

## Features

### PDF Operations
- **Merge PDF**: Combine multiple PDF files into a single document
- **Split PDF**: Divide a PDF into multiple documents
- **Lock PDF**: Add password protection to PDF files
- **Unlock PDF**: Remove password protection from PDF files
- **Sign PDF**: Add digital signatures to PDF documents
- **Compress PDF**: Reduce PDF file size while maintaining quality

### Image Operations
- **Compress Image**: Reduce image file size
- **Image to PDF**: Convert images to PDF format
- **PDF to Image**: Extract images from PDF files

### Text Operations
- **OCR**: Extract text from images using Optical Character Recognition
- **PDF to Text**: Convert PDF content to text format

### Document Conversion
- **Word to PDF**: Convert Word documents to PDF format

## Screenshots

| Main Screen | Feature Selection | Document Processing |
|-------------|-------------------|---------------------|
| [Screenshot] | [Screenshot] | [Screenshot] |

## Installation

### Prerequisites
- Android Studio (latest version)
- Android SDK (API level 21 or higher)
- Java Development Kit (JDK) 8 or higher

### Setup
1. Clone the repository:
   ```
   git clone https://github.com/yourusername/DocXpert.git
   ```
2. Open the project in Android Studio
3. Sync Gradle files
4. Build and run the application on your device or emulator

## Usage

1. Launch the application
2. Select the desired document operation from the main menu
3. Follow the on-screen instructions to complete the operation
4. Access your processed documents in the app's file manager

## Technical Details

DocXpert is built using:
- **Language**: Java
- **Platform**: Android
- **Libraries**: 
  - PDFBox for PDF manipulation
  - AndroidX for modern UI components
  - Material Design components

## Architecture

The application follows a modular architecture with separate packages for each feature:
- `com.example.docxpert.features.mergepdf`
- `com.example.docxpert.features.splitpdf`
- `com.example.docxpert.features.lockpdf`
- And more...

## Team

- **Adwait Date** (Frontend): Responsible for the user interface design and implementation
- **Varsha Naresh Gupta** (Functionalities): Implemented core PDF and document manipulation features
- **Purva Sagar Kamerkar** (Backend): Developed the backend infrastructure and file handling

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Acknowledgments

- Android Developer Documentation
- PDFBox Documentation
- Material Design Guidelines
- All contributors and supporters of the project

---

**DocXpert** - Your all-in-one document management solution for Android. 