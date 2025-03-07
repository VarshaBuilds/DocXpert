# Tesseract OCR Language Data

This directory should contain Tesseract language data files (.traineddata files).

## Required Files

For the OCR functionality to work, you need to download and place the following files in this directory:

- `eng.traineddata` - English language data

## How to Get Language Files

1. Download the language files from the official Tesseract repository:
   https://github.com/tesseract-ocr/tessdata

2. Place the downloaded `.traineddata` files directly in this directory.

## Supported Languages

You can add additional language files to support OCR in different languages. Common language codes include:

- `eng` - English
- `fra` - French
- `deu` - German
- `spa` - Spanish
- `ita` - Italian
- `por` - Portuguese
- `chi_sim` - Simplified Chinese
- `chi_tra` - Traditional Chinese
- `jpn` - Japanese
- `kor` - Korean

To use multiple languages, modify the Tesseract configuration in the OCRActivity.java file. 