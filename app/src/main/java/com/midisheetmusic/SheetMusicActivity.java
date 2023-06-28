/*
 * Copyright (c) 2011-2012 Madhav Vaidyanathan
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License version 2.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 */

package com.midisheetmusic;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.drawerlayout.widget.DrawerLayout;

import com.midisheetmusic.drawerItems.ExpandableSwitchDrawerItem;
import com.midisheetmusic.sheets.ClefSymbol;
import com.mikepenz.materialdrawer.holder.StringHolder;
import com.mikepenz.materialdrawer.model.DividerDrawerItem;
import com.mikepenz.materialdrawer.model.SecondaryDrawerItem;
import com.mikepenz.materialdrawer.model.SecondarySwitchDrawerItem;
import com.mikepenz.materialdrawer.model.SwitchDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem;
import com.mikepenz.materialdrawer.util.MaterialDrawerSliderViewExtensionsKt;
import com.mikepenz.materialdrawer.util.MenuDrawerUtilsKt;
import com.mikepenz.materialdrawer.widget.MaterialDrawerSliderView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.zip.CRC32;

/**
 * SheetMusicActivity is the main activity. The main components are:
 * <ul>
 *  <li> MidiPlayer : The buttons and speed bar at the top.
 *  <li> Piano : For highlighting the piano notes during playback.
 *  <li> SheetMusic : For highlighting the sheet music notes during playback.
 */
public class SheetMusicActivity extends MidiHandlingActivity {

    public static final String MidiTitleID = "MidiTitleID";
    public static final int settingsRequestCode = 1;
    public static final int ID_LOOP_ENABLE = 10;
    public static final int ID_LOOP_START = 11;
    public static final int ID_LOOP_END = 12;

    private MidiPlayer player;   /* The play/stop/rewind toolbar */
    private Piano piano;         /* The piano at the top */
    private SheetMusic sheet;    /* The sheet music */
    private LinearLayout layout; /* The layout */
    private MidiFile midifile;   /* The midi file to play */
    private MidiOptions options; /* The options for sheet music and sound */
    private long midiCRC;        /* CRC of the midi bytes */
    private MaterialDrawerSliderView drawer;
    private DrawerLayout drawerLayout;

     /** Create this SheetMusicActivity.
      * The Intent should have two parameters:
      * - data: The uri of the midi file to open.
      * - MidiTitleID: The title of the song (String)
      */
    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);

        // Hide the navigation bar before the views are laid out
        hideSystemUI();

        setContentView(R.layout.sheet_music_layout);

        // Keep screen on
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        ClefSymbol.LoadImages(this);
        TimeSigSymbol.LoadImages(this);

        // Parse the MidiFile from the raw bytes
        Uri uri = this.getIntent().getData();
        if (uri == null) {
            this.finish();
            return;
        }
        String title = this.getIntent().getStringExtra(MidiTitleID);
        if (title == null) {
            title = uri.getLastPathSegment();
        }
        FileUri file = new FileUri(uri, title);
        this.setTitle("MidiSheetMusic: " + title);
        byte[] data;
        try {
            data = file.getData(this);
            midifile = new MidiFile(data, title);
        }
        catch (MidiFileException e) {
            this.finish();
            return;
        }

        // Initialize the settings (MidiOptions).
        // If previous settings have been saved, use those
        options = new MidiOptions(midifile);
        CRC32 crc = new CRC32();
        crc.update(data);
        midiCRC = crc.getValue();
        SharedPreferences settings = getPreferences(0);
        options.scrollVert = settings.getBoolean("scrollVert", false);
        options.shade1Color = settings.getInt("shade1Color", options.shade1Color);
        options.shade2Color = settings.getInt("shade2Color", options.shade2Color);
        options.showPiano = settings.getBoolean("showPiano", true);
        String json = settings.getString("" + midiCRC, null);
        MidiOptions savedOptions = MidiOptions.fromJson(json);
        if (savedOptions != null) {
            options.merge(savedOptions);
        }

        createViews();
    }

    /* Create the MidiPlayer and Piano views */
    void createViews() {
        layout = findViewById(R.id.sheet_content);
        drawer = findViewById(R.id.sheet_content_drawer);
        drawerLayout = findViewById(R.id.sheet_content_drawer_layout);

        SwitchDrawerItem scrollVertically = new SwitchDrawerItem();
        scrollVertically.setName(new StringHolder(R.string.scroll_vertically));
        scrollVertically.setChecked(options.scrollVert);
        scrollVertically.setOnCheckedChangeListener((iDrawerItem, compoundButton, isChecked) -> {
            options.scrollVert = isChecked;
            createSheetMusic(options);
        });

        SwitchDrawerItem colorAccidentals = new SwitchDrawerItem();

        SwitchDrawerItem useColors = new SwitchDrawerItem();
        useColors.setName(new StringHolder(R.string.use_note_colors));
        useColors.setChecked(options.useColors);
        useColors.setOnCheckedChangeListener((iDrawerItem, compoundButton, isChecked) -> {
            if (isChecked) {
                options.colorAccidentals = false;
            }

            colorAccidentals.setChecked(options.colorAccidentals);
            MaterialDrawerSliderViewExtensionsKt.updateItem(drawer, colorAccidentals);
            options.useColors = isChecked;
            createSheetMusic(options);
        });

        colorAccidentals.setName(new StringHolder(R.string.use_accidental_colors));
        colorAccidentals.setChecked(options.colorAccidentals);
        colorAccidentals.setOnCheckedChangeListener((iDrawerItem, compoundButton, isChecked) -> {
            if (isChecked) {
                options.useColors = false;
            }

            useColors.setChecked(options.useColors);
            MaterialDrawerSliderViewExtensionsKt.updateItem(drawer, useColors);
            options.colorAccidentals = isChecked;
            createSheetMusic(options);
        });

        SecondarySwitchDrawerItem showMeasures = new SecondarySwitchDrawerItem();
        showMeasures.setName(new StringHolder(R.string.show_measures));
        showMeasures.setLevel(2);
        showMeasures.setChecked(options.showMeasures);
        showMeasures.setOnCheckedChangeListener((iDrawerItem, compoundButton, isChecked) -> {
            options.showMeasures = isChecked;
            createSheetMusic(options);
        });

        SecondaryDrawerItem loopStart = new SecondaryDrawerItem();
        loopStart.setIdentifier(ID_LOOP_START);
        loopStart.setBadge(new StringHolder(Integer.toString(options.playMeasuresInLoopStart + 1)));
        loopStart.setName(new StringHolder(R.string.play_measures_in_loop_start));
        loopStart.setLevel(2);

        SecondaryDrawerItem loopEnd = new SecondaryDrawerItem();
        loopEnd.setIdentifier(ID_LOOP_END);
        loopEnd.setBadge(new StringHolder(Integer.toString(options.playMeasuresInLoopEnd + 1)));
        loopEnd.setName(new StringHolder(R.string.play_measures_in_loop_end));
        loopEnd.setLevel(2);

        ExpandableSwitchDrawerItem loopSettings = new ExpandableSwitchDrawerItem();
        loopSettings.setIdentifier(ID_LOOP_ENABLE);
        loopSettings.setName(new StringHolder(R.string.loop_on_measures));
        loopSettings.setChecked(options.playMeasuresInLoop);
        loopSettings.setOnCheckedChangeListener((iDrawerItem, compoundButton, isChecked) -> {
            options.playMeasuresInLoop = isChecked;
        });
        loopSettings.setSubItems(showMeasures, loopStart, loopEnd);

        drawer.setInnerShadow(true);
        MaterialDrawerSliderViewExtensionsKt.addItems(drawer,
                scrollVertically,
                useColors,
                colorAccidentals,
                loopSettings,
                new DividerDrawerItem());
        MenuDrawerUtilsKt.inflateMenu(drawer, R.menu.sheet_menu);
        drawer.setOnDrawerItemClickListener((view, i, item) -> drawerItemClickListener(i));

        // Lock the drawer so swiping doesn't open it
        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);

        player = new MidiPlayer(this);
        player.setDrawer(drawer, drawerLayout);
        layout.addView(player);

        piano = new Piano(this);
        layout.addView(piano);
        player.SetPiano(piano);
        layout.requestLayout();

        player.setSheetUpdateRequestListener(() -> createSheetMusic(options));
        createSheetMusic(options);
    }

    /** Create the SheetMusic view with the given options */
    private void 
    createSheetMusic(MidiOptions options) {
        if (sheet != null) {
            layout.removeView(sheet);
        }

        piano.setVisibility(options.showPiano ? View.VISIBLE : View.GONE);
        sheet = new SheetMusic(this);
        sheet.init(midifile, options);
        sheet.setPlayer(player);
        layout.addView(sheet);
        piano.SetMidiFile(midifile, options, player);
        piano.SetShadeColors(options.shade1Color, options.shade2Color);

        player.SetMidiFile(midifile, options, sheet);
        player.updateToolbarButtons();
        layout.requestLayout();
        sheet.draw();
    }


    /** Always display this activity in landscape mode. */
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }


    /** Create a string list of the numbers between listStart and listEnd (inclusive) */
    private String[] makeStringList(int listStart, int listEnd) {
        String[] list = new String[listEnd];
        for (int i = 0; i < list.length; i++) {
            list[i] = Integer.toString(i + listStart);
        }
        return list;
    }


    /** Handle clicks on the drawer menu */
    public boolean drawerItemClickListener(IDrawerItem item) {
        switch ((int)item.getIdentifier()) {
            case R.id.song_settings:
                changeSettings();
                drawerLayout.closeDrawer(drawer);
                break;
            case R.id.save_images:
                showSaveImagesDialog();
                drawerLayout.closeDrawer(drawer);
                break;
            case ID_LOOP_START:
                // Note that we display the measure numbers starting at 1,
                // but the actual playMeasuresInLoopStart field starts at 0.
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(R.string.play_measures_in_loop_start);
                String[] items = makeStringList(1, options.lastMeasure + 1);
                builder.setItems(items, (dialog, i) -> {
                    options.playMeasuresInLoopStart = Integer.parseInt(items[i]) - 1;
                    // Make sure End is not smaller than Start
                    if (options.playMeasuresInLoopStart > options.playMeasuresInLoopEnd) {
                        options.playMeasuresInLoopEnd = options.playMeasuresInLoopStart;
                        MaterialDrawerSliderViewExtensionsKt.updateBadge(
                                drawer, ID_LOOP_END, new StringHolder(items[i]));
                    }
                    ((SecondaryDrawerItem) item).setBadge(new StringHolder(items[i]));
                    MaterialDrawerSliderViewExtensionsKt.updateItem(drawer, item);
                });
                AlertDialog alertDialog = builder.create();
                alertDialog.show();
                alertDialog.getListView().setSelection(options.playMeasuresInLoopStart);
                break;
            case ID_LOOP_END:
                // Note that we display the measure numbers starting at 1,
                // but the actual playMeasuresInLoopEnd field starts at 0.
                builder = new AlertDialog.Builder(this);
                builder.setTitle(R.string.play_measures_in_loop_end);
                items = makeStringList(1, options.lastMeasure + 1);
                builder.setItems(items, (dialog, i) -> {
                    options.playMeasuresInLoopEnd = Integer.parseInt(items[i]) - 1;
                    // Make sure End is not smaller than Start
                    if (options.playMeasuresInLoopStart > options.playMeasuresInLoopEnd) {
                        options.playMeasuresInLoopStart = options.playMeasuresInLoopEnd;
                        MaterialDrawerSliderViewExtensionsKt.updateBadge(
                                drawer, ID_LOOP_START, new StringHolder(items[i]));
                    }
                    ((SecondaryDrawerItem) item).setBadge(new StringHolder(items[i]));
                    MaterialDrawerSliderViewExtensionsKt.updateItem(drawer, item);
                });
                alertDialog = builder.create();
                alertDialog.show();
                alertDialog.getListView().setSelection(options.playMeasuresInLoopEnd);
                break;
        }
        return true;
    }


    /** To change the sheet music options, start the SettingsActivity.
     *  Pass the current MidiOptions as a parameter to the Intent.
     *  Also pass the 'default' MidiOptions as a parameter to the Intent.
     *  When the SettingsActivity has finished, the onActivityResult()
     *  method will be called.
     */
    private void changeSettings() {
        MidiOptions defaultOptions = new MidiOptions(midifile);
        Intent intent = new Intent(this, SettingsActivity.class);
        intent.putExtra(SettingsActivity.settingsID, options);
        intent.putExtra(SettingsActivity.defaultSettingsID, defaultOptions);
        startActivityForResult(intent, settingsRequestCode);
    }


    /* Show the "Save As Images" dialog */
    private void showSaveImagesDialog() {
         LayoutInflater inflator = LayoutInflater.from(this);
         final View dialogView= inflator.inflate(R.layout.save_images_dialog, layout, false);
         final EditText filenameView = dialogView.findViewById(R.id.save_images_filename);
         filenameView.setText(midifile.getFileName().replace("_", " ") );
         AlertDialog.Builder builder = new AlertDialog.Builder(this);
         builder.setTitle(R.string.save_images_str);
         builder.setView(dialogView);
         builder.setPositiveButton("OK",
                 (builder1, whichButton) -> saveAsImages(filenameView.getText().toString()));
         builder.setNegativeButton("Cancel",
                 (builder12, whichButton) -> {
         });
         AlertDialog dialog = builder.create();
         dialog.show();
    }


    /* Save the current sheet music as PNG images. */
    private void saveAsImages(String name) {
        String filename = name;
        try {
            filename = URLEncoder.encode(name, "utf-8");
        }
        catch (UnsupportedEncodingException e) {
            Toast.makeText(this, "Error: unsupported encoding in filename", Toast.LENGTH_SHORT).show();
        }
        if (!options.scrollVert) {
            options.scrollVert = true;
            createSheetMusic(options);
        }
        try {
            int numpages = sheet.GetTotalPages();
            for (int page = 1; page <= numpages; page++) {
                Bitmap image= Bitmap.createBitmap(SheetMusic.PageWidth + 40, SheetMusic.PageHeight + 40, Bitmap.Config.ARGB_8888);
                Canvas imageCanvas = new Canvas(image);
                sheet.DrawPage(imageCanvas, page);
                File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES + "/MidiSheetMusic");
                File file = new File(path, "" + filename + page + ".png");
                path.mkdirs();
                OutputStream stream = new FileOutputStream(file);
                image.compress(Bitmap.CompressFormat.PNG, 0, stream);
                stream.close();

                // Inform the media scanner about the file
                MediaScannerConnection.scanFile(this, new String[] { file.toString() }, null, null);
            }
        }
        catch (IOException e) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage("Error saving image to file " + Environment.DIRECTORY_PICTURES + "/MidiSheetMusic/" + filename  + ".png");
            builder.setCancelable(false);
            builder.setPositiveButton("OK", (dialog, id) -> { });
            AlertDialog alert = builder.create();
            alert.show();
        }
        catch (NullPointerException e) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage("Ran out of memory while saving image to file " + Environment.DIRECTORY_PICTURES + "/MidiSheetMusic/" + filename  + ".png");
            builder.setCancelable(false);
            builder.setPositiveButton("OK", (dialog, id) -> {});
            AlertDialog alert = builder.create();
            alert.show();
        }
    }


    /** Show the HTML help screen. */
    private void showHelp() {
        Intent intent = new Intent(this, HelpActivity.class);
        startActivity(intent);
    }

    /** Save the options in the SharedPreferences */
    private void saveOptions() {
        SharedPreferences.Editor editor = getPreferences(0).edit();
        editor.putBoolean("scrollVert", options.scrollVert);
        editor.putInt("shade1Color", options.shade1Color);
        editor.putInt("shade2Color", options.shade2Color);
        editor.putBoolean("showPiano", options.showPiano);
        for (int i = 0; i < options.noteColors.length; i++) {
            editor.putInt("noteColor" + i, options.noteColors[i]);
        }
        String json = options.toJson();
        if (json != null) {
            editor.putString("" + midiCRC, json);
        }
        editor.apply();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        saveOptions();
    }

    /** This is the callback when the SettingsActivity is finished.
     *  Get the modified MidiOptions (passed as a parameter in the Intent).
     *  Save the MidiOptions.  The key is the CRC checksum of the midi data,
     *  and the value is a JSON dump of the MidiOptions.
     *  Finally, re-create the SheetMusic View with the new options.
     */
    @Override
    protected void onActivityResult (int requestCode, int resultCode, Intent intent) {
        if (requestCode != settingsRequestCode) {
            return;
        }
        options = (MidiOptions) 
            intent.getSerializableExtra(SettingsActivity.settingsID);

        // Check whether the default instruments have changed.
        for (int i = 0; i < options.instruments.length; i++) {
            if (options.instruments[i] !=  
                midifile.getTracks().get(i).getInstrument()) {
                options.useDefaultInstruments = false;
            }
        }

        saveOptions();

        // Recreate the sheet music with the new options
        createSheetMusic(options);
    }

    /** When this activity resumes, redraw all the views */
    @Override
    protected void onResume() {
        super.onResume();
        layout.requestLayout();
        player.invalidate();
        piano.invalidate();
        if (sheet != null) {
            sheet.invalidate();
        }
        layout.requestLayout();
    }

    /** When this activity pauses, stop the music */
    @Override
    protected void onPause() {
        if (player != null) {
            player.Pause();
        }
        super.onPause();
    }

    @Override
    void OnMidiDeviceStatus(boolean connected) {
        player.OnMidiDeviceStatus(connected);
    }

    @Override
    void OnMidiNote(int note, boolean pressed) {
        player.OnMidiNote(note, pressed);
    }

    /************************** Hide navigation buttons **************************/

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            hideSystemUI();
        }
    }

    private void hideSystemUI() {
        // Enables sticky immersive mode.
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        // Set the content to appear under the system bars so that the
                        // content doesn't resize when the system bars hide and show.
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        // Hide the nav bar and status bar
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN);
    }
}

