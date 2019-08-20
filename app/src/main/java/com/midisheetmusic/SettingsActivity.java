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
import android.view.MenuItem;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.preference.*;

/**
 * This activity is created by the "Settings" menu option.
 * The user can change settings such as: <ul>
 *  <li/> Which tracks to display
 *  <li/> Which tracks to mute
 *  <li/> Which instruments to use during playback
 *  <li/> Whether to scroll horizontally or vertically
 *  <li/> Whether to display the piano or not
 *  <li/> Whether to display note letters or not
 *  <li/> Transpose the notes to another key
 *  <li/> Change the key signature or time signature displayed
 *  <li/> Change how notes are combined into chords (the time interval)
 *  <li/> Change the colors for shading the left/right hands.
 *  <li/> Whether to display measure numbers
 *  <li/> Play selected measures in a loop
 * </ul>
 *
 * When created, pass an Intent parameter containing MidiOptions.
 * When destroyed, this activity passes the result MidiOptions to the Intent.
 */
public class SettingsActivity extends AppCompatActivity {

    public static final String settingsID = "settings";
    public static final String defaultSettingsID = "defaultSettings";

    private MidiOptions defaultOptions;  /** The initial option values */
    private MidiOptions options;         /** The option values */

    /** Create the Settings activity. Retrieve the initial option values
     *  (MidiOptions) from the Intent.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);
        options = (MidiOptions) getIntent().getSerializableExtra(settingsID);
        defaultOptions = (MidiOptions) getIntent().getSerializableExtra(defaultSettingsID);

        // Pass options to the fragment
        Fragment settingsFragment = new SettingsFragment();
        Bundle bundle = new Bundle();
        bundle.putSerializable(SettingsActivity.settingsID, options);
        bundle.putSerializable(SettingsActivity.defaultSettingsID, defaultOptions);
        settingsFragment.setArguments(bundle);

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.settings, settingsFragment)
                .commit();
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    /** Handle 'Up' button press */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
        }
        return super.onOptionsItemSelected(item);
    }

    /** When the back button is pressed, update the MidiOptions.
     *  Return the updated options as the 'result' of this Activity.
     */
    @Override
    public void onBackPressed() {
        // Make sure `options` is updated with the changes
        SettingsFragment settingsFragment = (SettingsFragment)getSupportFragmentManager()
                .findFragmentById(R.id.settings);
        if (settingsFragment != null) {
            settingsFragment.updateOptions();
        }

        Intent intent = new Intent();
        intent.putExtra(SettingsActivity.settingsID, options);
        setResult(Activity.RESULT_OK, intent);
        super.onBackPressed();
    }

    public static class SettingsFragment extends PreferenceFragmentCompat
            implements Preference.OnPreferenceChangeListener, Preference.OnPreferenceClickListener {

        private MidiOptions defaultOptions;  /** The initial option values */
        private MidiOptions options;         /** The option values */

        private Preference restoreDefaults;           /** Restore default settings */
        private SwitchPreferenceCompat[] selectTracks;    /** Which tracks to display */
        private SwitchPreferenceCompat[] playTracks;      /** Which tracks to play */
        private ListPreference[] selectInstruments;   /** Instruments to use per track */
        private Preference setAllToPiano;             /** Set all instruments to piano */
        private SwitchPreferenceCompat showLyrics;        /** Show the lyrics */
        private SwitchPreferenceCompat twoStaffs;         /** Combine tracks into two staffs */
        private ListPreference showNoteLetters;       /** Show the note letters */
        private ListPreference transpose;             /** Transpose notes */
        private ListPreference midiShift;             /** Control MIDI shift */
        private ListPreference key;                   /** Key Signature to use */
        private ListPreference time;                  /** Time Signature to use */
        private ListPreference combineInterval;       /** Interval (msec) to combine notes */

        private ColorPreference[] noteColors;
        private SwitchPreferenceCompat useColors;

        private ColorPreference shade1Color;          /** Right-hand color */
        private ColorPreference shade2Color;          /** Left-hand color */

        private Context context;

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            if (getArguments() != null) {
                options = (MidiOptions) getArguments().getSerializable(SettingsActivity.settingsID);
                defaultOptions = (MidiOptions) getArguments().getSerializable(SettingsActivity.defaultSettingsID);
            }
            context = getPreferenceManager().getContext();
            createView();
        }

        /** Create all the preference widgets in the view */
        private void createView() {
            PreferenceScreen root = getPreferenceManager().createPreferenceScreen(context);
            createRestoreDefaultPrefs(root);
            createDisplayTrackPrefs(root);
            createPlayTrackPrefs(root);
            createInstrumentPrefs(root);

            PreferenceCategory sheetTitle = new PreferenceCategory(context);
            sheetTitle.setTitle(R.string.sheet_prefs_title);
            root.addPreference(sheetTitle);

            createShowLyricsPrefs(root);
            if (options.tracks.length != 2) {
                createTwoStaffsPrefs(root);
            }
            createShowLetterPrefs(root);
            createTransposePrefs(root);
            createMidiShiftPrefs(root);
            createKeySignaturePrefs(root);
            createTimeSignaturePrefs(root);
            createCombineIntervalPrefs(root);
            createColorPrefs(root);
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
        private void createDisplayTrackPrefs(PreferenceScreen root) {
            PreferenceCategory displayTracksTitle = new PreferenceCategory(context);
            displayTracksTitle.setTitle(R.string.select_tracks_to_display);
            root.addPreference(displayTracksTitle);
            selectTracks = new SwitchPreferenceCompat[options.tracks.length];
            for (int i = 0; i < options.tracks.length; i++) {
                selectTracks[i] = new SwitchPreferenceCompat(context);
                selectTracks[i].setTitle("Track " + i);
                selectTracks[i].setChecked(options.tracks[i]);
                root.addPreference(selectTracks[i]);
            }
        }

        /** Create the "Select Tracks to Play" checkboxes. */
        private void createPlayTrackPrefs(PreferenceScreen root) {
            PreferenceCategory playTracksTitle = new PreferenceCategory(context);
            playTracksTitle.setTitle(R.string.select_tracks_to_play);
            root.addPreference(playTracksTitle);
            playTracks = new SwitchPreferenceCompat[options.mute.length];
            for (int i = 0; i < options.mute.length; i++) {
                playTracks[i] = new SwitchPreferenceCompat(context);
                playTracks[i].setTitle("Track " + i);
                playTracks[i].setChecked(!options.mute[i]);
                root.addPreference(playTracks[i]);
            }
        }


        /** Create the "Select Instruments For Each Track " lists.
         *  The list of possible instruments is in MidiFile.java.
         */
        private void createInstrumentPrefs(PreferenceScreen root) {
            PreferenceCategory selectInstrTitle = new PreferenceCategory(context);
            selectInstrTitle.setTitle(R.string.select_instruments_per_track);
            root.addPreference(selectInstrTitle);
            selectInstruments = new ListPreference[options.tracks.length];
            for (int i = 0; i < options.instruments.length; i++) {
                selectInstruments[i] = new ListPreference(context);
                selectInstruments[i].setOnPreferenceChangeListener(this);
                selectInstruments[i].setEntries(MidiFile.Instruments);
                selectInstruments[i].setKey("select_instruments_" + i);
                selectInstruments[i].setEntryValues(MidiFile.Instruments);
                selectInstruments[i].setTitle("Track " + i);
                selectInstruments[i].setValueIndex(options.instruments[i]);
                selectInstruments[i].setSummary( selectInstruments[i].getEntry() );
                root.addPreference(selectInstruments[i]);
            }
            setAllToPiano = new Preference(context);
            setAllToPiano.setTitle(R.string.set_all_to_piano);
            setAllToPiano.setOnPreferenceClickListener(this);
            root.addPreference(setAllToPiano);
        }

        /** Create the "Show Lyrics" preference */
        private void createShowLyricsPrefs(PreferenceScreen root) {
            showLyrics = new SwitchPreferenceCompat(context);
            showLyrics.setTitle(R.string.show_lyrics);
            showLyrics.setChecked(options.showLyrics);
            root.addPreference(showLyrics);
        }

        /** Create the "Show Note Letters" preference */
        private void createShowLetterPrefs(PreferenceScreen root) {
            showNoteLetters = new ListPreference(context);
            showNoteLetters.setOnPreferenceChangeListener(this);
            showNoteLetters.setKey("show_note_letters");
            showNoteLetters.setTitle(R.string.show_note_letters);
            showNoteLetters.setEntries(R.array.show_note_letter_entries);
            showNoteLetters.setEntryValues(R.array.show_note_letter_values);
            showNoteLetters.setValueIndex(options.showNoteLetters);
            showNoteLetters.setSummary(showNoteLetters.getEntry());
//            DialogPreference x = new DialogPreference(context);
            root.addPreference(showNoteLetters);
        }


        /** Create the "Combine to Two Staffs" preference. */
        private void createTwoStaffsPrefs(PreferenceScreen root) {
            twoStaffs = new SwitchPreferenceCompat(context);
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
            transpose = new ListPreference(context);
            transpose.setKey("transpose");
            transpose.setOnPreferenceChangeListener(this);
            transpose.setTitle(R.string.transpose);
            transpose.setEntries(R.array.transpose_entries);
            transpose.setEntryValues(R.array.transpose_values);
            transpose.setValueIndex(transposeIndex);
            transpose.setSummary(transpose.getEntry());
            root.addPreference(transpose);
        }

        /** Create the "Shift MIDI Input" preference.
         *  It shifts the input received via MIDI interface with
         *  a value in the range 12, 11, 10, .. -10, -11, -12
         */
        private void createMidiShiftPrefs(PreferenceScreen root) {
            int midiShiftIndex = 12 - options.midiShift;
            midiShift = new ListPreference(context);
            midiShift.setKey("midi_shift");
            midiShift.setOnPreferenceChangeListener(this);
            midiShift.setTitle(R.string.midiShift);
            midiShift.setEntries(R.array.transpose_entries);
            midiShift.setEntryValues(R.array.transpose_values);
            midiShift.setValueIndex(midiShiftIndex);
            midiShift.setSummary(midiShift.getEntry());
            root.addPreference(midiShift);
        }

        /** Create the "Key Signature" preference */
        private void createKeySignaturePrefs(PreferenceScreen root) {
            key = new ListPreference(context);
            key.setOnPreferenceChangeListener(this);
            key.setKey("key_signature");
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

            time = new ListPreference(context);
            time.setKey("time_signature");
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
            combineInterval = new ListPreference(context);
            combineInterval.setKey("combine_interval");
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
            PreferenceCategory localPreferenceCategory = new PreferenceCategory(context);
            localPreferenceCategory.setTitle("Select Colors");
            root.addPreference(localPreferenceCategory);

            shade1Color = new ColorPreference(context);
            shade1Color.setColor(options.shade1Color);
            shade1Color.setTitle(R.string.right_hand_color);
            root.addPreference(shade1Color);

            shade2Color = new ColorPreference(context);
            shade2Color.setColor(options.shade2Color);
            shade2Color.setTitle(R.string.left_hand_color);
            root.addPreference(shade2Color);

            useColors = new SwitchPreferenceCompat(context);
            useColors.setTitle(R.string.use_note_colors);
            useColors.setChecked(options.useColors);
            useColors.setOnPreferenceChangeListener((preference, isChecked) -> {
                for (ColorPreference noteColorPref : noteColors) {
                    noteColorPref.setVisible((boolean)isChecked);
                }
                return true;
            });
            root.addPreference(useColors);

            noteColors = new ColorPreference[options.noteColors.length];
            for (int i = 0; i < 12; i++) {
                noteColors[i] = new ColorPreference(context);
                noteColors[i].setColor(options.noteColors[i]);
                noteColors[i].setTitle(new String[]
                        {"A", "A#", "B", "C", "C#", "D", "D#", "E", "F", "F#", "G", "G#"}[i]);
                noteColors[i].setVisible(options.useColors);
                root.addPreference(noteColors[i]);
            }
        }

        /** Create the "Restore Default Settings" preference */
        private void createRestoreDefaultPrefs(PreferenceScreen root) {
            restoreDefaults = new Preference(context);
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
                options.mute[i] = !playTracks[i].isChecked();
            }
            for (int i = 0; i < options.noteColors.length; i++) {
                options.noteColors[i] = noteColors[i].getColor();
            }
            for (int i = 0; i < options.tracks.length; i++) {
                ListPreference entry = selectInstruments[i];
                options.instruments[i] = entry.findIndexOfValue(entry.getValue());
            }
            options.showLyrics = showLyrics.isChecked();
            if (twoStaffs != null)
                options.twoStaffs = twoStaffs.isChecked();
            else
                options.twoStaffs = false;

            options.showNoteLetters = Integer.parseInt(showNoteLetters.getValue());
            options.transpose = Integer.parseInt(transpose.getValue());
            options.midiShift = Integer.parseInt(midiShift.getValue());
            options.key = Integer.parseInt(key.getValue());
            switch (time.getValue()) {
                case "Default":
                    options.time = null;
                    break;
                case "3/4":
                    options.time = new TimeSignature(3, 4, options.defaultTime.getQuarter(),
                            options.defaultTime.getTempo());
                    break;
                case "4/4":
                    options.time = new TimeSignature(4, 4, options.defaultTime.getQuarter(),
                            options.defaultTime.getTempo());
                    break;
            }
            options.combineInterval = Integer.parseInt(combineInterval.getValue());
            options.shade1Color = shade1Color.getColor();
            options.shade2Color = shade2Color.getColor();
            options.useColors = useColors.isChecked();
        }

        @Override
        public void onStop() {
            updateOptions();
            super.onStop();
        }

        @Override
        public void onPause() {
            updateOptions();
            super.onPause();
        }
    }
}


