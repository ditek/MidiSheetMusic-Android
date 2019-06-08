package com.midisheetmusic;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.snackbar.Snackbar;
import com.midisheetmusic.sheets.ClefSymbol;

/**
 * An activity to be shown when starting the app.
 * It handles checking for the required permissions and preloading the images.
 */
public class SplashActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE_EXT_STORAGE_ = 724;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        loadImages();
        startActivity();
    }

    /** Check for required permissions and start ChooseSongActivity */
    private void startActivity() {
        // Check if we have WRITE_EXTERNAL_STORAGE permission
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    PERMISSION_REQUEST_CODE_EXT_STORAGE_);
            return;
        }

        Intent intent = new Intent(this, ChooseSongActivity.class);
        startActivity(intent);
        finish();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_CODE_EXT_STORAGE_: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission granted
                    startActivity();
                } else {
                    // permission denied
                    Snackbar.make(findViewById(android.R.id.content),
                            R.string.msg_permission_denied, Snackbar.LENGTH_INDEFINITE)
                            .setAction(R.string.msg_permission_denied_retry, view -> startActivity())
                            .show();
                }
            }
        }
    }

    /** Load all the resource images */
    private void loadImages() {
        ClefSymbol.LoadImages(this);
        TimeSigSymbol.LoadImages(this);
    }

    /** Always use landscape mode for this activity. */
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }
}