package com.midisheetmusic;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.splashscreen.SplashScreen;

import com.google.android.material.snackbar.Snackbar;
import com.midisheetmusic.sheets.ClefSymbol;

/**
 * An activity to be shown when starting the app.
 * It handles checking for the required permissions and preloading the images.
 */
public class SplashActivity extends AppCompatActivity {

    private SplashScreen splashScreen;
    private final String requestPermission = Manifest.permission.WRITE_EXTERNAL_STORAGE;
    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.RequestPermission(),
                    this::checkPermissionApi21AfterResult);
    private boolean waitForPermission = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        splashScreen = SplashScreen.installSplashScreen(this);
        super.onCreate(savedInstanceState);
        loadImages();
        waitForPermission = true;
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (!waitForPermission)
            return;
        waitForPermission = false;

        splashScreen.setKeepOnScreenCondition(() -> {
            checkPermission(
                (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
                    ? Environment.isExternalStorageManager()
                    : (ContextCompat.checkSelfPermission(this, requestPermission)
                        == PackageManager.PERMISSION_GRANTED)
            );
            return false;
        });
    }

    /** check permission **/
    private void checkPermission(boolean granted) {
        if (granted) {
            Intent intent = new Intent(this, ChooseSongActivity.class);
            startActivity(intent);
            finish();
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
                requestPermissionApi30();
            else
                requestPermissionLauncher.launch(requestPermission);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.R)
    private void requestPermissionApi30() {
        Uri uri = Uri.parse("package:" + BuildConfig.APPLICATION_ID);
        startActivity(new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, uri));
        waitForPermission = true;

        Snackbar.make(findViewById(android.R.id.content),
                        R.string.msg_permission_denied, Snackbar.LENGTH_INDEFINITE)
                .setAction(R.string.msg_permission_denied_retry, view -> requestPermissionApi30())
                .show();
    }

    private void checkPermissionApi21AfterResult(boolean granted) {
        if (granted)
            checkPermission(true);
        else
            Snackbar.make(findViewById(android.R.id.content),
                            R.string.msg_permission_denied, Snackbar.LENGTH_INDEFINITE)
                    .setAction(R.string.msg_permission_denied_retry, view -> {
                        requestPermissionApi21AfterResult();
                    })
                    .show();
    }

    private void requestPermissionApi21AfterResult() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(SplashActivity.this, requestPermission)) {
            requestPermissionLauncher.launch(requestPermission);
        } else {
            Uri uri = Uri.parse("package:" + BuildConfig.APPLICATION_ID);
            startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, uri));
            waitForPermission = true;
        }
    }

    /** Load all the resource images */
    private void loadImages() {
        ClefSymbol.LoadImages(this);
        TimeSigSymbol.LoadImages(this);
    }

    /** Always use landscape mode for this activity. */
    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }
}