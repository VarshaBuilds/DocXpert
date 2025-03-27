package com.example.docxpert.utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import androidx.documentfile.provider.DocumentFile;

public class SafUtils {
    private static final String TAG = "SafUtils";
    private static final SimpleDateFormat TIMESTAMP_FORMAT = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());

    public interface FileOperationCallback {
        void onSuccess(Uri uri);
        void onFailure(Exception e);
    }

    // Open file for reading
    public static ActivityResultLauncher<String[]> createOpenFileLauncher(
            AppCompatActivity activity, 
            FileOperationCallback callback) {
        return activity.registerForActivityResult(
            new ActivityResultContracts.OpenDocument(),
            uri -> {
                if (uri != null) {
                    try {
                        // Take persist read permission
                        final int takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION;
                        activity.getContentResolver().takePersistableUriPermission(uri, takeFlags);
                        callback.onSuccess(uri);
                    } catch (Exception e) {
                        Log.e(TAG, "Error taking permission", e);
                        callback.onFailure(e);
                    }
                } else {
                    callback.onFailure(new Exception("No file selected"));
                }
            }
        );
    }

    // Create file for writing with timestamp
    public static ActivityResultLauncher<String> createSaveFileLauncher(
            AppCompatActivity activity, 
            String originalFileName,
            String targetExtension,
            FileOperationCallback callback) {
        return activity.registerForActivityResult(
            new ActivityResultContracts.CreateDocument(),
            uri -> {
                if (uri != null) {
                    try {
                        // Take persist write permission
                        final int takeFlags = Intent.FLAG_GRANT_WRITE_URI_PERMISSION;
                        activity.getContentResolver().takePersistableUriPermission(uri, takeFlags);
                        callback.onSuccess(uri);
                    } catch (Exception e) {
                        Log.e(TAG, "Error taking permission", e);
                        callback.onFailure(e);
                    }
                } else {
                    callback.onFailure(new Exception("No file created"));
                }
            }
        );
    }

    // Get input stream
    public static InputStream getInputStream(Context context, Uri uri) throws Exception {
        InputStream inputStream = context.getContentResolver().openInputStream(uri);
        if (inputStream == null) {
            throw new Exception("Could not open input stream");
        }
        return inputStream;
    }

    // Get output stream
    public static OutputStream getOutputStream(Context context, Uri uri) throws Exception {
        OutputStream outputStream = context.getContentResolver().openOutputStream(uri);
        if (outputStream == null) {
            throw new Exception("Could not open output stream");
        }
        return outputStream;
    }

    // Get MIME type
    public static String getMimeType(Context context, Uri uri) {
        return context.getContentResolver().getType(uri);
    }

    // Get file name without extension
    public static String getFileNameWithoutExtension(Context context, Uri uri) {
        String fullName = getFileName(context, uri);
        int lastDot = fullName.lastIndexOf('.');
        return lastDot > 0 ? fullName.substring(0, lastDot) : fullName;
    }

    // Get file extension
    public static String getFileExtension(Context context, Uri uri) {
        String fullName = getFileName(context, uri);
        int lastDot = fullName.lastIndexOf('.');
        return lastDot > 0 ? fullName.substring(lastDot) : "";
    }

    // Get file name with timestamp
    public static String getFileNameWithTimestamp(String originalName, String targetExtension) {
        String timestamp = TIMESTAMP_FORMAT.format(new Date());
        String nameWithoutExt = originalName;
        int lastDot = originalName.lastIndexOf('.');
        if (lastDot > 0) {
            nameWithoutExt = originalName.substring(0, lastDot);
        }
        return nameWithoutExt + "_" + timestamp + targetExtension;
    }

    // Get file name
    public static String getFileName(Context context, Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            try (android.database.Cursor cursor = context.getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int index = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);
                    if (index >= 0) {
                        result = cursor.getString(index);
                    }
                }
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result;
    }

    public static void createFile(ActivityResultLauncher<Intent> launcher, String fileName, String mimeType) {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType(mimeType);
        intent.putExtra(Intent.EXTRA_TITLE, fileName);
        launcher.launch(intent);
    }

    /**
     * Creates a new file in the specified folder using DocumentFile
     * @param context The context
     * @param folderUri The URI of the folder where to create the file
     * @param fileName The name of the file to create
     * @param mimeType The MIME type of the file
     * @return The URI of the created file, or null if creation failed
     */
    public static Uri createFile(Context context, Uri folderUri, String fileName, String mimeType) {
        try {
            DocumentFile folder = DocumentFile.fromTreeUri(context, folderUri);
            if (folder != null) {
                DocumentFile file = folder.createFile(mimeType, fileName);
                if (file != null) {
                    return file.getUri();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error creating file: " + e.getMessage());
        }
        return null;
    }
} 