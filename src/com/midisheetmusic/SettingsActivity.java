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

import android.app.*;
import android.content.*;
import android.os.*;
import android.preference.*;
import android.graphics.*;
import android.graphics.drawable.ColorDrawable;


/** @class SettingsActivity
 *  This activity is created by the "Settings" menu option.
 *  The user can change settings such as:
 *  - Which tracks to display
 *  - Which tracks to mute
 *  - Which instruments to use during playback
 *  - Whether to scroll horizontally or vertically
 *  - Whether to display the piano or not
 *  - Whether to display note letters or not
 *  - Transpose the notes to another key
 *  - Change the key signature or time signature displayed
 *  - Change how notes are combined into chords (the time interval)
 *  - Change the colors for shading the left/right hands.
 *  - Whether to display measure numbers
 *  - Play selected measures in a loop
 * 
 * When created, pass an Intent parameter containing MidiOptions.
 * When destroyed, this activity passes the result MidiOptions to the Intent.
 */
public class SettingsActivity extends PreferenceActivity 
    implements Preference.OnPreferenceChangeListener, Preference.OnPreferenceClickListener {

    public static final String settingsID = "settings";
    public static final String defaultSettingsID = "defaultSettings";

    private MidiOptions defaultOptions;  /** The initial option values */
    private MidiOptions options;         /** The option values */

    private Preference restoreDefaults;           /** Restore default settings */
    private CheckBoxPreference[] selectTracks;    /** Which tracks to display */
    private CheckBoxPreference[] muteTracks;      /** Which tracks to mute */
    private ListPreference[] selectInstruments;   /** Instruments to use per track */
    private Preference setAllToPiano;             /** Set all instruments to piano */
    private CheckBoxPreference scrollVertically;  /** Scroll vertically/horizontally */
    private CheckBoxPreference showPiano;         /** Show the piano */
    private CheckBoxPreference showMeasures;      /** Show the measure numbers */
    private CheckBoxPreference showLyrics;        /** Show the lyrics */
    private CheckBoxPreference twoStaffs;         /** Combine tracks into two staffs */
    private ListPreference showNoteLetters;       /** Show the note letters */
    private ListPreference transpose;             /** Transpose notes */
    private ListPreference key;                   /** Key Signature to use */
    private ListPreference time;                  /** Time Signature to use */
    private ListPreference combineInterval;       /** Interval (msec) to combine notes */
    
    private ColorPreference shade1Color;          /** Right-hand color */
    private ColorPreference shade2Color;          /** Left-hand color */

    /** Play the measures from start to end in a loop */
    private CheckBoxPreference playMeasuresInLoop;
    private ListPreference loopStart;
    private ListPreference loopEnd;


    /** Create the Settings activity. Retrieve the initial option values
     *  (MidiOptions) from the Intent.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // getWindow().setBackgroundDrawable(new ColorDrawable(Color.rgb(40, 40, 40)));
        // getListView().setBackgroundColor(Color.TRANSPARENT);
        // getListView().setCacheColorHint(Color.TRANSPARENT);
        setTitle("MidiSheetMusic: Settings");
        options = (MidiOptions) this.getIntent().getSerializableExtra(settingsID);
        defaultOptions = (MidiOptions) this.getIntent().getSerializableExtra(defaultSettingsID);
        createView();
    }

    /** Create all the preference widgets in the view */
    private void createView() {
        PreferenceScreen root = getPreferenceManager().createPreferenceScreen(this);
        createRestoreDefaultPrefs(root);
        createTrackPrefs(root);
        createMutePrefs(root);
        createInstrumentPrefs(root);

        PreferenceCategory sheetTitle = new PreferenceCategory(this);
        sheetTitle.setTitle(R.string.sheet_prefs_title);
        root.addPreference(sheetTitle);

        createScrollPrefs(root);
        createShowPianoPrefs(root);
        createShowLyricsPrefs(root);
        if (options.tracks.length != 2) {
            createTwoStaffsPrefs(root);
        }
        createShowLetterPrefs(root);
        createTransposePrefs(root);
        createKeySignaturePrefs(root);
        createTimeSignaturePrefs(root);
        createCombineIntervalPrefs(root);
        createColorPrefs(root);
        createPlayMeasuresInLoopPrefs(root);
        setPreferenceScreen(root);
    }

    /** For each list dialog, we display the value selected in the "summary" text.
     *  When a new value is selected from the list dialog, update the summary
     *  to the selected entry.
     */
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        ListPreference list = (ListPreference) preference;
        int index = list.findIndexOfValue((String)newValue);
        CharSequence entry = list.getEntries()[index];
        preference.setSummary(entry);
        return true;
    }

    /** When the 'restore defaults' preference is clicked, restore the default settings */
    public boolean onPreferenceClick(Preference preference) {
        if (preference == restoreDefaults) {
            options = defaultOptions.copy();
            createView(); 
        }
        else if (preference == setAllToPiano) {
            for (int i = 0; i < options.instruments.length; i++) {
                options.instruments[i] = 0;
            }
            createView();
        }
        return true;
    }


    /** Create the "Select Tracks to Display" checkboxes. */
    private void createTrackPrefs(PreferenceScreen root) {
        PreferenceCategory selectTracksTitle = new PreferenceCategory(this);
        selectTracksTitle.setTitle(R.string.select_tracks_to_display);
        root.addPreference(selectTracksTitle);
        selectTracks = new CheckBoxPreference[options.tracks.length];
        for (int i = 0; i < options.tracks.length; i++) {
            selectTracks[i] = new CheckBoxPreference(this);
            selectTracks[i].setTitle("Track " + i);
            selectTracks[i].setChecked(options.tracks[i]);
            root.addPreference(selectTracks[i]);
        }
    }

    /** Create the "Select Tracks to Mute" checkboxes. */
    private void createMutePrefs(PreferenceScreen root) {
        PreferenceCategory muteTracksTitle = new PreferenceCategory(this);
        muteTracksTitle.setTitle(R.string.select_tracks_to_mute);
        root.addPreference(muteTracksTitle);
        muteTracks = new CheckBoxPreference[options.mute.length];
        for (int i = 0; i < options.mute.length; i++) {
            muteTracks[i] = new CheckBoxPreference(this);
            muteTracks[i].setTitle("Track " + i);
            muteTracks[i].setChecked(options.mute[i]);
            root.addPreference(muteTracks[i]);
        }
    }


    /** Create the "Select Instruments For Each Track " lists.
     *  The list of possible instruments is in MidiFile.java.
     */
    private void createInstrumentPrefs(PreferenceScreen root) {
        PreferenceCategory selectInstrTitle = new PreferenceCategory(this);
        selectInstrTitle.setTitle(R.string.select_instruments_per_track);
        root.addPreference(selectInstrTitle);
        selectInstruments = new ListPreference[options.tracks.length];
        for (int i = 0; i < options.instruments.length; i++) {
            selectInstruments[i] = new ListPreference(this);
            selectInstruments[i].setOnPreferenceChangeListener(this);
            selectInstruments[i].setEntries(MidiFile.Instruments);
            selectInstruments[i].setEntryValues(MidiFile.Instruments);
            selectInstruments[i].setTitle("Track " + i);
            selectInstruments[i].setValueIndex(options.instruments[i]);
            selectInstruments[i].setSummary( selectInstruments[i].getEntry() );
            root.addPreference(selectInstruments[i]);
        }
        setAllToPiano = new Preference(this);
        setAllToPiano.setTitle(R.string.set_all_to_piano);
        setAllToPiano.setOnPreferenceClickListener(this);
        root.addPreference(setAllToPiano);
    }

    /** Create the "Scroll Vertically" preference */
    private void createScrollPrefs(PreferenceScreen root) {
        scrollVertically = new CheckBoxPreference(this);
        scrollVertically.setTitle(R.string.scroll_vertically);
        scrollVertically.setChecked(options.scrollVert);
        root.addPreference(scrollVertically);
    }

    /** Create the "Show Piano" preference */
    private void createShowPianoPrefs(PreferenceScreen root) {
        showPiano = new CheckBoxPreference(this);
        showPiano.setTitle(R.string.show_piano);
        showPiano.setChecked(options.showPiano);
        root.addPreference(showPiano);
    }

    /** Create the "Show Lyrics" preference */
    private void createShowLyricsPrefs(PreferenceScreen root) {
        showLyrics = new CheckBoxPreference(this);
        showLyrics.setTitle(R.string.show_lyrics);
        showLyrics.setChecked(options.showLyrics);
        root.addPreference(showLyrics);
    }

    /** Create the "Show Note Letters" preference */
    private void createShowLetterPrefs(PreferenceScreen root) {
        showNoteLetters = new ListPreference(this);
        showNoteLetters.setOnPreferenceChangeListener(this);
        showNoteLetters.setTitle(R.string.show_note_letters);
        showNoteLetters.setEntries(R.array.show_note_letter_entries);
        showNoteLetters.setEntryValues(R.array.show_note_letter_values);
        showNoteLetters.setValueIndex(options.showNoteLetters);
        showNoteLetters.setSummary(showNoteLetters.getEntry());
        root.addPreference(showNoteLetters);
    }


    /** Create the "Combine to Two Staffs" preference. */
    private void createTwoStaffsPrefs(PreferenceScreen root) {
        twoStaffs = new CheckBoxPreference(this);
        if (options.tracks.length == 1) {
            twoStaffs.setTitle(R.string.split_to_two_staffs);
            twoStaffs.setSummary(R.string.split_to_two_staffs_summary);
        }
        else {
            twoStaffs.setTitle(R.string.combine_to_two_staffs);
            twoStaffs.setSummary(R.string.combine_to_two_staffs_summary);
        }
        twoStaffs.setChecked(options.twoStaffs);
        root.addPreference(twoStaffs);
    }

    /** Create the "Transpose Notes" preference.
     *  The values range from 12, 11, 10, .. -10, -11, -12
     */
    private void createTransposePrefs(PreferenceScreen root) {
        int transposeIndex = 12 - options.transpose;
        transpose = new ListPreference(this);
        transpose.setOnPreferenceChangeListener(this);
        transpose.setTitle(R.string.transpose);
        transpose.setEntries(R.array.transpose_entries);
        transpose.setEntryValues(R.array.transpose_values);
        transpose.setValueIndex(transposeIndex);
        transpose.setSummary(transpose.getEntry());
        root.addPreference(transpose);
    }

    /** Create the "Key Signature" preference */
    private void createKeySignaturePrefs(PreferenceScreen root) {
        key = new ListPreference(this);
        key.setOnPreferenceChangeListener(this);
        key.setTitle(R.string.key_signature);
        key.setEntries(R.array.key_signature_entries);
        key.setEntryValues(R.array.key_signature_values);
        key.setValueIndex(options.key + 1);
        key.setSummary(key.getEntry());
        root.addPreference(key);
    }

    /** Create the "Time Signature" preference */
    private void createTimeSignaturePrefs(PreferenceScreen root) {
        String[] values = { "Default", "3/4", "4/4" };
        int selected = 0;
        if (options.time != null && options.time.getNumerator() == 3)
            selected = 1;
        else if (options.time != null && options.time.getNumerator() == 4)
            selected = 2;

        time = new ListPreference(this);
        time.setOnPreferenceChangeListener(this);
        time.setTitle(R.string.time_signature);
        time.setEntries(values);
        time.setEntryValues(values);
        time.setValueIndex(selected);
        time.setSummary(time.getEntry());
        root.addPreference(time);
    }


    /** Create the "Combine Notes Within Interval"  preference.
     *  Notes within N milliseconds are combined into a single chord,
     *  even though their start times may be slightly different.
     */
    private void createCombineIntervalPrefs(PreferenceScreen root) {
        int selected = options.combineInterval/20  - 1;
        combineInterval = new ListPreference(this);
        combineInterval.setOnPreferenceChangeListener(this);
        combineInterval.setTitle(R.string.combine_interval);
        combineInterval.setEntries(R.array.combine_interval_entries);
        combineInterval.setEntryValues(R.array.combine_interval_values);
        combineInterval.setValueIndex(selected);
        combineInterval.setSummary(combineInterval.getEntry() );
        root.addPreference(combineInterval);
    }


    /* Create the "Left-hand color" and "Right-hand color" preferences */
    private void createColorPrefs(PreferenceScreen root) {
        shade1Color = new ColorPreference(this);
        shade1Color.setColor(options.shade1Color);
        shade1Color.setTitle(R.string.right_hand_color);
        root.addPreference(shade1Color);

        shade2Color = new ColorPreference(this);
        shade2Color.setColor(options.shade2Color);
        shade2Color.setTitle(R.string.left_hand_color);
        root.addPreference(shade2Color);
    }


    /** Create the "Play Measures in a Loop" preference.
     *
     *  Note that we display the measure numbers starting at 1, 
     *  but the actual playMeasuresInLoopStart field starts at 0.
     */
    private void createPlayMeasuresInLoopPrefs(PreferenceScreen root) {
        String[] values = new String[options.lastMeasure + 1];
        for (int measure = 0; measure < values.length; measure++) {
            values[measure] = "" + (measure+1);
        }

        PreferenceCategory playLoopTitle = new PreferenceCategory(this);
        playLoopTitle.setTitle(R.string.play_measures_in_loop_title);
        root.addPreference(playLoopTitle);

        showMeasures = new CheckBoxPreference(this);
        showMeasures.setTitle(R.string.show_measures);
        showMeasures.setChecked(options.showMeasures);
        root.addPreference(showMeasures);

        playMeasuresInLoop = new CheckBoxPreference(this);
        playMeasuresInLoop.setTitle(R.string.play_measures_in_loop);
        playMeasuresInLoop.setChecked(options.playMeasuresInLoop);
        root.addPreference(playMeasuresInLoop);

        loopStart = new ListPreference(this);
        loopStart.setOnPreferenceChangeListener(this);
        loopStart.setTitle(R.string.play_measures_in_loop_start);
        loopStart.setEntries(values);
        loopStart.setEntryValues(values);
        loopStart.setValueIndex(options.playMeasuresInLoopStart);
        loopStart.setSummary(loopStart.getEntry() );
        root.addPreference(loopStart);

        loopEnd = new ListPreference(this);
        loopEnd.setOnPreferenceChangeListener(this);
        loopEnd.setTitle(R.string.play_measures_in_loop_end);
        loopEnd.setEntries(values);
        loopEnd.setEntryValues(values);
        loopEnd.setValueIndex(options.playMeasuresInLoopEnd);
        loopEnd.setSummary(loopEnd.getEntry() );
        root.addPreference(loopEnd);
    }

    /* Create the "Restore Default Settings" preference */
    private void createRestoreDefaultPrefs(PreferenceScreen root) {
        restoreDefaults = new Preference(this);
        restoreDefaults.setTitle(R.string.restore_defaults);
        restoreDefaults.setOnPreferenceClickListener(this);
        root.addPreference(restoreDefaults);
    } 

    /** Update the MidiOptions based on the preferences selected. */
    private void updateOptions() {
        for (int i = 0; i < options.tracks.length; i++) {
            options.tracks[i] = selectTracks[i].isChecked();
        }
        for (int i = 0; i < options.mute.length; i++) {
            options.mute[i] = muteTracks[i].isChecked();
        }
        for (int i = 0; i < options.tracks.length; i++) {
            ListPreference entry = selectInstruments[i];
            options.instruments[i] = entry.findIndexOfValue(entry.getValue());
        }
        options.scrollVert = scrollVertically.isChecked();
        options.showPiano = showPiano.isChecked();
        options.showLyrics = showLyrics.isChecked();
        if (twoStaffs != null)
            options.twoStaffs = twoStaffs.isChecked();
        else
            options.twoStaffs = false;

        options.showNoteLetters = Integer.parseInt(showNoteLetters.getValue());
        options.transpose = Integer.parseInt(transpose.getValue());
        options.key = Integer.parseInt(key.getValue());
        if (time.getValue().equals("Default")) {
            options.time = null;
        }
        else if (time.getValue().equals("3/4")) {
            options.time = new TimeSignature(3, 4, options.defaultTime.getQuarter(),
                                             options.defaultTime.getTempo());
        }
        else if (time.getValue().equals("4/4")) {
            options.time = new TimeSignature(4, 4, options.defaultTime.getQuarter(),
                                             options.defaultTime.getTempo());
        }
        options.combineInterval = Integer.parseInt(combineInterval.getValue());
        options.shade1Color = shade1Color.getColor();
        options.shade2Color = shade2Color.getColor();
        options.showMeasures = showMeasures.isChecked();
        options.playMeasuresInLoop = playMeasuresInLoop.isChecked();
        options.playMeasuresInLoopStart = Integer.parseInt(loopStart.getValue()) - 1;
        options.playMeasuresInLoopEnd = Integer.parseInt(loopEnd.getValue()) - 1;
    }

    /** When the back button is pressed, update the MidiOptions.
     *  Return the updated options as the 'result' of this Activity.
     */
    @Override
    public void onBackPressed() {
        Intent intent = new Intent();
        updateOptions();
        intent.putExtra(SettingsActivity.settingsID, options);
        setResult(Activity.RESULT_OK, intent);
        super.onBackPressed();
    }
        
}


