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

import java.io.*;
import java.net.*;
import android.app.*;
import android.net.Uri;
import android.os.*;
import android.util.*;
import android.widget.*;
import android.view.*;
import android.graphics.*;
import android.content.*;
import android.content.res.*;
import android.media.*;
import java.util.zip.CRC32;

/** @class SheetMusicActivity
 *
 * The SheetMusicActivity is the main activity. The main components are:
 * - MidiPlayer : The buttons and speed bar at the top.
 * - Piano : For highlighting the piano notes during playback.
 * - SheetMusic : For highlighting the sheet music notes during playback.
 *
 */
public class SheetMusicActivity extends Activity {

    public static final String MidiTitleID = "MidiTitleID";
    public static final int settingsRequestCode = 1;
    
    private MidiPlayer player;   /* The play/stop/rewind toolbar */
    private Piano piano;         /* The piano at the top */
    private SheetMusic sheet;    /* The sheet music */
    private LinearLayout layout; /* THe layout */
    private MidiFile midifile;   /* The midi file to play */
    private MidiOptions options; /* The options for sheet music and sound */
    private long midiCRC;      /* CRC of the midi bytes */

     /** Create this SheetMusicActivity.  
      * The Intent should have two parameters:
      * - data: The uri of the midi file to open.
      * - MidiTitleID: The title of the song (String)
      */
    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);

        ClefSymbol.LoadImages(this);
        TimeSigSymbol.LoadImages(this);
        MidiPlayer.LoadImages(this);

        // Parse the MidiFile from the raw bytes
        Uri uri = this.getIntent().getData();
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
        // If previous settings have been saved, used those
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
        createView();
        createSheetMusic(options);
    }
    
    /* Create the MidiPlayer and Piano views */
    void createView() {
        layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        player = new MidiPlayer(this);
        piano = new Piano(this);
        layout.addView(player);
        layout.addView(piano);
        setContentView(layout);
        player.SetPiano(piano);
        layout.requestLayout();
    }

    /** Create the SheetMusic view with the given options */
    private void 
    createSheetMusic(MidiOptions options) {
        if (sheet != null) {
            layout.removeView(sheet);
        }
        if (!options.showPiano) {
            piano.setVisibility(View.GONE);
        }
        else {
            piano.setVisibility(View.VISIBLE);
        }
        sheet = new SheetMusic(this);
        sheet.init(midifile, options);
        sheet.setPlayer(player);
        layout.addView(sheet);
        piano.SetMidiFile(midifile, options, player);
        piano.SetShadeColors(options.shade1Color, options.shade2Color);
        player.SetMidiFile(midifile, options, sheet);
        layout.requestLayout();
        sheet.callOnDraw();
    }


    /** Always display this activity in landscape mode. */
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    /** When the menu button is pressed, initialize the menus. */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (player != null) {
            player.Pause();
        }
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.sheet_menu, menu);
        return true;
    }

    /** Callback when a menu item is selected.
     *  - Choose Song : Choose a new song
     *  - Song Settings : Adjust the sheet music and sound options
     *  - Save As Images: Save the sheet music as PNG images
     *  - Help : Display the HTML help screen
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
          case R.id.choose_song:
            chooseSong();
            return true;
          case R.id.song_settings:
            changeSettings();
            return true;
          case R.id.save_images:
            showSaveImagesDialog();
            return true;
          case R.id.help:
            showHelp();
            return true;
          default:
            return super.onOptionsItemSelected(item);
        }
    }

    /** To choose a new song, simply finish this activity.
     *  The previous activity is always the ChooseSongActivity.
     */
    private void chooseSong() {
        this.finish();
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
         final View dialogView= inflator.inflate(R.layout.save_images_dialog, null);
         final EditText filenameView = (EditText)dialogView.findViewById(R.id.save_images_filename);
         filenameView.setText(midifile.getFileName().replace("_", " ") );
         AlertDialog.Builder builder = new AlertDialog.Builder(this);
         builder.setTitle(R.string.save_images_str);
         builder.setView(dialogView);
         builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
             public void onClick(DialogInterface builder, int whichButton) {
                 saveAsImages(filenameView.getText().toString());
             }
         });
         builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
             public void onClick(DialogInterface builder, int whichButton) {
             }
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
                image = null;
                stream.close();
    
                // Inform the media scanner about the file
                MediaScannerConnection.scanFile(this, new String[] { file.toString() }, null, null);
            }
        }
        catch (IOException e) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage("Error saving image to file " + Environment.DIRECTORY_PICTURES + "/MidiSheetMusic/" + filename  + ".png");
            builder.setCancelable(false);
            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                }
            });
            AlertDialog alert = builder.create();
            alert.show();
        }
        catch (NullPointerException e) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage("Ran out of memory while saving image to file " + Environment.DIRECTORY_PICTURES + "/MidiSheetMusic/" + filename  + ".png");
            builder.setCancelable(false);
            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                }
            });
            AlertDialog alert = builder.create();
            alert.show();
        }
    }


    /** Show the HTML help screen. */
    private void showHelp() {
        Intent intent = new Intent(this, HelpActivity.class);
        startActivity(intent);
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
        // Save the options. 
        SharedPreferences.Editor editor = getPreferences(0).edit();
        editor.putBoolean("scrollVert", options.scrollVert);
        editor.putInt("shade1Color", options.shade1Color);
        editor.putInt("shade2Color", options.shade2Color);
        editor.putBoolean("showPiano", options.showPiano);
        String json = options.toJson();
        if (json != null) {
            editor.putString("" + midiCRC, json);
        }
        editor.commit();

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
}

