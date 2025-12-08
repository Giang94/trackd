package com.app.trackd.activity;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.app.trackd.R;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.journeyapps.barcodescanner.DecoratedBarcodeView;

public class BarcodeScanActivity extends AppCompatActivity {

    private DecoratedBarcodeView barcodeView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_barcode_scan);

        barcodeView = findViewById(R.id.barcodeView);

        barcodeView.decodeContinuous(result -> {
            Intent returnIntent = new Intent();
            returnIntent.putExtra("SCAN_RESULT", result.getText());
            setResult(RESULT_OK, returnIntent);
            finish();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        barcodeView.resume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        barcodeView.pause();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result != null) {
            Intent returnIntent = new Intent();
            if (result.getContents() != null) {
                returnIntent.putExtra("SCAN_RESULT", result.getContents());
                setResult(RESULT_OK, returnIntent);
            } else {
                setResult(RESULT_CANCELED);
            }
            finish();
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }
}
