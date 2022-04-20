
package com.example.android.camera2basic;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.speech.tts.TextToSpeech;

import android.text.TextUtils;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.WorkerThread;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;


import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.parser.PdfTextExtractor;

import org.apache.commons.io.IOUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Locale;
import java.util.Objects;

public class Docs extends AppCompatActivity {
    private TextToSpeech textToSpeech;
    private static final int READ_REQUEST_CODE = 42;
    private static final String PRIMARY = "primary";
    private static final String LOCAL_STORAGE = "/storage/self/primary/";
    private static final String EXT_STORAGE = "/storage/7764-A034/";
    private static final String COLON = ":";
    TextView outputTextView;
    private Intent intent;
    Button stop;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pdf_reader);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = findViewById(R.id.fab);
  outputTextView = findViewById(R.id.output_text);
  stop=findViewById(R.id.stop_btn);

        outputTextView.setMovementMethod(new ScrollingMovementMethod());

        textToSpeech = new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                textToSpeech.setLanguage(Locale.US);
            }
        });

        /* getting user permission for external storage */
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE}, PackageManager.PERMISSION_GRANTED);

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.setType("*/*");
                startActivityForResult(intent, READ_REQUEST_CODE);
            }
        });
        stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                textToSpeech.stop();
                textToSpeech.shutdown();
            }
        });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == READ_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            try {
                if (data != null) {
                    Uri uri = data.getData();
//                Toast.makeText(this, uri.getPath(), Toast.LENGTH_SHORT).show();
                    assert uri != null;
                    Log.v("URI", uri.getPath());
                    readPdfFile(uri);
                }
                super.onActivityResult(requestCode, resultCode, data);
            } catch (Exception e) {
                Log.v("URI", e.toString());
            }

        }
    }

    public void readPdfFile(@NonNull Uri uri) {

        String fullPath=FileUtils.getReadablePathFromUri(this,uri);
        Log.v("URIRead", fullPath);
        String parsedText = "";
        try {

            PdfReader reader = new PdfReader(fullPath);
            int n = reader.getNumberOfPages();
            for (int i = 0; i < n; i++) {
                parsedText = parsedText + PdfTextExtractor.getTextFromPage(reader, i + 1).trim() + "\n"; //Extracting the content from the different pages


            }
            outputTextView.setText(parsedText);
            textToSpeech.setSpeechRate(1.5f);
            textToSpeech.speak(parsedText, TextToSpeech.QUEUE_FLUSH, null, null);
            reader.close();
        } catch (Exception e) {
            Log.v("URIRead", e.toString());
            e.printStackTrace();
        }
        Log.v("URIRead", parsedText.toString());

    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        textToSpeech.stop();
        textToSpeech.shutdown();
        Intent in = new Intent(this, CameraActivity.class);
        in.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(in);
        finish();
    }

    public static class FileUtils {
        private static final String TAG = "FileUtils";
        @WorkerThread
        @Nullable
        public static String getReadablePathFromUri(Context context, Uri uri) {

            String path = null;
            if ("file".equalsIgnoreCase(uri.getScheme())) {
                path = uri.getPath();
            }

            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT) {
                path = getPath(context, uri);
            }

            if (TextUtils.isEmpty(path)) {
                return path;
            }

            Log.d(TAG, "get path from uri: " + path);
            if (!isReadablePath(path)) {
                int index = path.lastIndexOf("/");
                String name = path.substring(index + 1);
                String dstPath = context.getCacheDir().getAbsolutePath() + File.separator + name;
                if (copyFile(context, uri, dstPath)) {
                    path = dstPath;
                    Log.d(TAG, "copy file success: " + path);
                } else {
                    Log.d(TAG, "copy file fail!");
                }
            }
            return path;
        }

        public static String getPath(final Context context, final Uri uri) {
            final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;
            if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
                if (isExternalStorageDocument(uri)) {
                    final String docId = DocumentsContract.getDocumentId(uri);
                    Log.d("External Storage", docId);
                    final String[] split = docId.split(":");
                    final String type = split[0];

                    if ("primary".equalsIgnoreCase(type)) {
                        return Environment.getExternalStorageDirectory() + "/" + split[1];
                    }
                } else if (isDownloadsDocument(uri)) {

                    String dstPath = context.getCacheDir().getAbsolutePath() + File.separator + getFileName(context,uri);

                    if (copyFile(context, uri, dstPath)) {
                        Log.d(TAG, "copy file success: " + dstPath);
                        return dstPath;

                    } else {
                        Log.d(TAG, "copy file fail!");
                    }


                } else if (isMediaDocument(uri)) {
                    final String docId = DocumentsContract.getDocumentId(uri);
                    final String[] split = docId.split(":");
                    final String type = split[0];

                    Uri contentUri = null;
                    if ("image".equals(type)) {
                        contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                    } else if ("video".equals(type)) {
                        contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                    } else if ("audio".equals(type)) {
                        contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                    }

                    final String selection = "_id=?";
                    final String[] selectionArgs = new String[]{split[1]};
                    return getDataColumn(context, contentUri, selection, selectionArgs);
                }
            } else if ("content".equalsIgnoreCase(uri.getScheme())) {
                return getDataColumn(context, uri, null, null);
            } else if ("file".equalsIgnoreCase(uri.getScheme())) {
                return uri.getPath();
            }
            return null;
        }

        public static String getFileName(Context context, Uri uri) {

            Cursor cursor = context.getContentResolver().query(uri,null,null,null,null);
            int nameindex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
            cursor.moveToFirst();

            return  cursor.getString(nameindex);
        }


        private static String getDataColumn(Context context, Uri uri, String selection,
                                            String[] selectionArgs) {
            Cursor cursor = null;
            final String column = "_data";
            final String[] projection = {column};

            try {
                cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs,
                        null);
                if (cursor != null && cursor.moveToFirst()) {
                    final int column_index = cursor.getColumnIndexOrThrow(column);
                    return cursor.getString(column_index);
                }
            } finally {
                if (cursor != null)
                    cursor.close();
            }
            return null;
        }

        private static boolean isExternalStorageDocument(Uri uri) {
            return "com.android.externalstorage.documents".equals(uri.getAuthority());
        }

        private static boolean isDownloadsDocument(Uri uri) {
            return "com.android.providers.downloads.documents".equals(uri.getAuthority());
        }

        private static boolean isMediaDocument(Uri uri) {
            return "com.android.providers.media.documents".equals(uri.getAuthority());
        }

        private static boolean isReadablePath(@Nullable String path) {
            if (TextUtils.isEmpty(path)) {
                return false;
            }
            boolean isLocalPath;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                if (!TextUtils.isEmpty(path)) {
                    File localFile = new File(path);
                    isLocalPath = localFile.exists() && localFile.canRead();
                } else {
                    isLocalPath = false;
                }
            } else {
                isLocalPath = path.startsWith(File.separator);
            }
            return isLocalPath;
        }

        private static boolean copyFile(Context context, Uri uri, String dstPath) {
            InputStream inputStream = null;
            OutputStream outputStream = null;
            try {
                inputStream = context.getContentResolver().openInputStream(uri);
                outputStream = new FileOutputStream(dstPath);

                byte[] buff = new byte[100 * 1024];
                int len;
                while ((len = inputStream.read(buff)) != -1) {
                    outputStream.write(buff, 0, len);
                }
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            } finally {
                if (inputStream != null) {
                    try {
                        inputStream.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                if (outputStream != null) {
                    try {
                        outputStream.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
            return true;
        }

    }
}