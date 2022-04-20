package com.example.android.camera2basic;

import android.Manifest;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;

import androidx.appcompat.app.AppCompatActivity;

import com.google.zxing.Result;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionDeniedResponse;
import com.karumi.dexter.listener.PermissionGrantedResponse;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.single.PermissionListener;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;

import me.dm7.barcodescanner.zxing.ZXingScannerView;

public class scannerView extends AppCompatActivity implements ZXingScannerView.ResultHandler
{
   ZXingScannerView scannerView;
    public static TextToSpeech textToSpeech;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        scannerView=new ZXingScannerView(this);
        setContentView(scannerView);
        Dexter.withContext(getApplicationContext())
                .withPermission(Manifest.permission.CAMERA)
                .withListener(new PermissionListener() {
                    @Override
                    public void onPermissionGranted(PermissionGrantedResponse permissionGrantedResponse) {
                        scannerView.startCamera();
                    }

                    @Override
                    public void onPermissionDenied(PermissionDeniedResponse permissionDeniedResponse) {

                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(PermissionRequest permissionRequest, PermissionToken permissionToken) {
                     permissionToken.continuePermissionRequest();
                    }
                }).check();
    }

    @Override
    public void handleResult(Result rawResult) {
       MainActivity.scantext.setText(rawResult.getText());
//        String domname=domain(rawResult.getText());
//        textToSpeech.speak("You are at the website of "+ domname, TextToSpeech.QUEUE_FLUSH, null, null);
        Uri uri = Uri.parse(rawResult.getText());
        startActivity(new Intent(Intent.ACTION_VIEW, uri));
        onBackPressed();
    }
//    public String domain(String url)
//    {
//        if (url.startsWith("http:/")) {
//            if (!url.contains("http://")) {
//                url = url.replaceAll("http:/", "http://");
//            }
//        } else {
//            url = "http://" + url;
//        }
//        URI uri = null;
//        try {
//            uri = new URI(url);
//        } catch (URISyntaxException e) {
//            e.printStackTrace();
//        }
//        assert uri != null;
//        String domain = uri.getHost();
//        return domain.startsWith("www.") ? domain.substring(3) : domain;
//    }
    @Override
    protected void onPause() {
        super.onPause();
        scannerView.stopCamera();
    }

    @Override
    protected void onResume() {
        super.onResume();
        scannerView.setResultHandler(this);
        scannerView.startCamera();
    }
}