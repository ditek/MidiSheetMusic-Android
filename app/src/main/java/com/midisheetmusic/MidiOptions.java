/*
- * Copyright (c) 2007-2011 Madhav Vaidyanathan
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
import android.util.Log;
import org.json.*;
import android.graphics.*;

/**
 * Contains the available options for
 * modifying the sheet music and sound.  These options are collected
 * from the SettingsActivity, and are passed to the SheetMusic and
 * MidiPlayer classes.
 */
public class MidiOptions implements Serializable {

    // The possible values for showNoteLetters
    public static final int NoteNameNone           = 0;
    public static final int NoteNameLetter         = 1;
    public static final int NoteNameFixedDoReMi    = 2;
    public static final int NoteNameMovableDoReMi  = 3;
    public static final int NoteNameFixedNumber    = 4;
    public static final int NoteNameMovableNumber  = 5;

    public boolean showPiano;        /** Display the piano */
    public boolean[] tracks;         /** Which tracks to display (true = display) */
    public int[] instruments;        /** Which instruments to use per track */
    public boolean useDefaultInstruments; /** If true, don't change instruments */
    public boolean scrollVert;       /** Whether to scroll vertically or horizontally */
    public boolean largeNoteSize;    /** Display large or small note sizes */
    public boolean twoStaffs;        /** Combine tracks into two staffs ? */
    public int showNoteLetters;      /** Show the letters (A, A#, etc) next to the notes */
    public boolean showLyrics;       /** Show the lyrics under each note */
    public boolean showMeasures;     /** Show the measure numbers for each staff */
    public int shifttime;            /** Shift note starttimes by the given amount */
    public int transpose;            /** Shift note key up/down by given amount */
    public int key;                  /** Use the given KeySignature (NoteScale) */
    public TimeSignature time;       /** Use the given time signature (null for default) */
    public TimeSignature defaultTime;  /** The default time signature */
    public int combineInterval;      /** Combine notes within given time interval (msec) */
    public int shade1Color;   /** The color to use for shading */
    public int shade2Color;   /** The color to use for shading the left hand piano */

    public boolean[] mute;    /** Which tracks to mute (true = mute) */
    public int  tempo;        /** The tempo, in microseconds per quarter note */
    public int  pauseTime;    /** Start the midi music at the given pause time */

    public boolean playMeasuresInLoop; /** Play the selected measures in a loop */
    public int     playMeasuresInLoopStart; /** Start measure to play in loop */
    public int     playMeasuresInLoopEnd;   /** End measure to play in loop */
    public int     lastMeasure;             /** The last measure in the song */

    public boolean useColors;
    public int[] noteColors;
    public int midiShift;

    public MidiOptions() {
    }

    /* Initialize the default settings/options for the given MidiFile */
    public MidiOptions(MidiFile midifile) {
        showPiano = true;
        int num_tracks = midifile.getTracks().size();
        tracks = new boolean[num_tracks];
        mute = new boolean[num_tracks];
        for (int i = 0; i < tracks.length; i++) {
            tracks[i] = true;
            mute[i] = false;
            if (midifile.getTracks().get(i).getInstrumentName().equals("Percussion")) {
                tracks[i] = false;
                mute[i] = true;
            }
        }
        useDefaultInstruments = true;
        instruments = new int[num_tracks];
        for (int i = 0; i < instruments.length; i++) {
            instruments[i] = midifile.getTracks().get(i).getInstrument();
        }
        scrollVert = false;
        largeNoteSize = true;
        twoStaffs = tracks.length != 2;
        showNoteLetters = NoteNameNone;
        showMeasures = false;
        showLyrics = true;
        shifttime = 0;
        transpose = 0;
        midiShift = 0;
        time = null;
        defaultTime = midifile.getTime();
        key = -1;
        combineInterval = 40;
        shade1Color = Color.rgb(210, 205, 220);
        shade2Color = Color.rgb(150, 200, 220);

        useColors = false;
        noteColors = new int[12];
        noteColors[0] = Color.rgb(180, 0, 0);
        noteColors[1] = Color.rgb(230, 0, 0);
        noteColors[2] = Color.rgb(220, 128, 0);
        noteColors[3] = Color.rgb(130, 130, 0);
        noteColors[4] = Color.rgb(187, 187, 0);
        noteColors[5] = Color.rgb(0, 100, 0);
        noteColors[6] = Color.rgb(0, 140, 0);
        noteColors[7] = Color.rgb(0, 180, 180);
        noteColors[8] = Color.rgb(0, 0, 120);
        noteColors[9] = Color.rgb(0, 0, 180);
        noteColors[10] = Color.rgb(88, 0, 147);
        noteColors[11] = Color.rgb(129, 0, 215);

        tempo = midifile.getTime().getTempo();
        pauseTime = 0;
        lastMeasure = midifile.EndTime() / midifile.getTime().getMeasure();
        playMeasuresInLoop = false;
        playMeasuresInLoopStart = 0;
        playMeasuresInLoopEnd = lastMeasure;
    }

    /* Convert this MidiOptions object into a JSON string. */
    public String toJson() {
        try {
            JSONObject json = new JSONObject();
            JSONArray jsonTracks = new JSONArray();
            for (boolean value : tracks) {
                jsonTracks.put(value);
            }
            JSONArray jsonMute = new JSONArray();
            for (boolean value : mute) {
                jsonMute.put(value);
            }
            JSONArray jsonInstruments = new JSONArray();
            for (int value : instruments) {
                jsonInstruments.put(value);
            }
            JSONArray jsonColors = new JSONArray();
            for (int value : noteColors) {
                jsonColors.put(value);
            }
            if (time != null) {
                JSONObject jsonTime = new JSONObject();
                jsonTime.put("numerator", time.getNumerator());
                jsonTime.put("denominator", time.getDenominator());
                jsonTime.put("quarter", time.getQuarter());
                jsonTime.put("tempo", time.getTempo());
                json.put("time", jsonTime);
            }

            json.put("versionCode", 7);
            json.put("tracks", jsonTracks); 
            json.put("mute", jsonMute); 
            json.put("instruments", jsonInstruments); 
            json.put("useDefaultInstruments", useDefaultInstruments);
            json.put("scrollVert", scrollVert);
            json.put("showPiano", showPiano);
            json.put("showLyrics", showLyrics);
            json.put("twoStaffs", twoStaffs);
            json.put("showNoteLetters", showNoteLetters);
            json.put("transpose", transpose);
            json.put("midiShift", midiShift);
            json.put("key", key);
            json.put("combineInterval", combineInterval);
            json.put("shade1Color", shade1Color);
            json.put("shade2Color", shade2Color);
            json.put("useColors", useColors);
            json.put("noteColors", jsonColors);
            json.put("showMeasures", showMeasures);
            json.put("playMeasuresInLoop", playMeasuresInLoop);
            json.put("playMeasuresInLoopStart", playMeasuresInLoopStart);
            json.put("playMeasuresInLoopEnd", playMeasuresInLoopEnd);
            
            return json.toString();
        }
        catch (JSONException e) {
            return null;
        }
        catch (NullPointerException e) {
            return null;
        }
    }

    /* Initialize the options from a json string */
    public static MidiOptions fromJson(String jsonString) {
        if (jsonString == null) {
            return null;
        }
        MidiOptions options = new MidiOptions();
        try {
            JSONObject json = new JSONObject(jsonString);
            JSONArray jsonTracks = json.getJSONArray("tracks");
            options.tracks = new boolean[jsonTracks.length()];
            for (int i = 0; i < options.tracks.length; i++) {
                options.tracks[i] = jsonTracks.getBoolean(i);
            }
 
            JSONArray jsonMute = json.getJSONArray("mute");
            options.mute = new boolean[jsonMute.length()];
            for (int i = 0; i < options.mute.length; i++) {
                options.mute[i] = jsonMute.getBoolean(i);
            } 

            JSONArray jsonInstruments = json.getJSONArray("instruments");
            options.instruments = new int[jsonInstruments.length()];
            for (int i = 0; i < options.instruments.length; i++) {
                options.instruments[i] = jsonInstruments.getInt(i);
            }

            if (json.has("noteColors"))
            {
                JSONArray jsonColors = json.getJSONArray("noteColors");
                options.noteColors = new int[jsonColors.length()];
                for (int i = 0; i < options.noteColors.length; i++) {
                    options.noteColors[i] = jsonColors.getInt(i);
                }
            }

            if (json.has("time")) {
                JSONObject jsonTime = json.getJSONObject("time");
                int numer = jsonTime.getInt("numerator");
                int denom = jsonTime.getInt("denominator");
                int quarter = jsonTime.getInt("quarter");
                int tempo = jsonTime.getInt("tempo");
                options.time = new TimeSignature(numer, denom, quarter, tempo);
            }

            options.useDefaultInstruments = json.getBoolean("useDefaultInstruments");
            options.scrollVert = json.getBoolean("scrollVert");
            options.showPiano = json.getBoolean("showPiano");
            options.showLyrics = json.getBoolean("showLyrics");
            options.twoStaffs = json.getBoolean("twoStaffs");
            options.showNoteLetters = json.getInt("showNoteLetters");
            options.transpose = json.getInt("transpose");
            options.midiShift = json.getInt("midiShift");
            options.key = json.getInt("key");
            options.combineInterval = json.getInt("combineInterval");
            options.shade1Color = json.getInt("shade1Color");
            options.shade2Color = json.getInt("shade2Color");
            if (json.has("useColors")) {
                options.useColors = json.getBoolean("useColors");
            }
            options.showMeasures = json.getBoolean("showMeasures");
            options.playMeasuresInLoop = json.getBoolean("playMeasuresInLoop");
            options.playMeasuresInLoopStart = json.getInt("playMeasuresInLoopStart");
            options.playMeasuresInLoopEnd = json.getInt("playMeasuresInLoopEnd");
        }
        catch (Exception e) {
            return null;
        }
        return options;
    }


    /* Merge in the saved options to this MidiOptions.*/
    void merge(MidiOptions saved) {
        if (saved.tracks.length == tracks.length) {
            System.arraycopy(saved.tracks, 0, tracks, 0, tracks.length);
        }
        if (saved.mute.length == mute.length) {
            System.arraycopy(saved.mute, 0, mute, 0, mute.length);
        }
        if (saved.instruments.length == instruments.length) {
            System.arraycopy(saved.instruments, 0, instruments, 0, instruments.length);
        }
        if (saved.mute.length == mute.length) {
            System.arraycopy(saved.mute, 0, mute, 0, mute.length);
        }
        if (saved.useColors && saved.noteColors != null) {
            noteColors = saved.noteColors;
        }
        if (saved.time != null) {
            time = new TimeSignature(saved.time.getNumerator(), saved.time.getDenominator(), 
                    saved.time.getQuarter(), saved.time.getTempo());
        }

        useDefaultInstruments = saved.useDefaultInstruments;
        scrollVert = saved.scrollVert;
        showPiano = saved.showPiano;
        showLyrics = saved.showLyrics;
        twoStaffs = saved.twoStaffs;
        showNoteLetters = saved.showNoteLetters;
        transpose = saved.transpose;
        midiShift = saved.midiShift;
        key = saved.key;
        combineInterval = saved.combineInterval;
        shade1Color = saved.shade1Color;
        shade2Color = saved.shade2Color;
        useColors = saved.useColors;
        showMeasures = saved.showMeasures;
        playMeasuresInLoop = saved.playMeasuresInLoop;
        playMeasuresInLoopStart = saved.playMeasuresInLoopStart;
        playMeasuresInLoopEnd = saved.playMeasuresInLoopEnd;
    }
 

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder("MidiOptions: tracks: ");
        for (boolean track : tracks) {
            result.append(track).append(", ");
        }
        result.append(" Instruments: ");
        for (int instrument : instruments) {
            result.append(instrument).append(", ");
        }
        result.append(" scrollVert ").append(scrollVert);
        result.append(" twoStaffs ").append(twoStaffs);
        result.append(" transpose").append(transpose);
        result.append(" midiShift").append(midiShift);
        result.append(" key ").append(key);
        result.append(" combine ").append(combineInterval);
        result.append(" tempo ").append(tempo);
        result.append(" pauseTime ").append(pauseTime);
        if (time != null) {
            result.append(" time ").append(time.toString());
        }
        return result.toString();
    }

    public MidiOptions copy() {
        MidiOptions options = new MidiOptions();
        options.tracks = new boolean[tracks.length];
        System.arraycopy(tracks, 0, options.tracks, 0, tracks.length);
        options.mute = new boolean[mute.length];
        System.arraycopy(mute, 0, options.mute, 0, mute.length);
        options.instruments = new int[instruments.length];
        System.arraycopy(instruments, 0, options.instruments, 0, instruments.length);
        options.noteColors = new int[noteColors.length];
        System.arraycopy(noteColors, 0, options.noteColors, 0, noteColors.length);

        options.defaultTime = defaultTime;
        options.time = time;
        options.useDefaultInstruments = useDefaultInstruments;
        options.scrollVert = scrollVert;
        options.showPiano = showPiano;
        options.showLyrics = showLyrics;
        options.twoStaffs = twoStaffs;
        options.showNoteLetters = showNoteLetters;
        options.transpose = transpose;
        options.midiShift = midiShift;
        options.key = key;
        options.combineInterval = combineInterval;
        options.shade1Color = shade1Color;
        options.shade2Color = shade2Color;
        options.useColors = useColors;
        options.showMeasures = showMeasures;
        options.playMeasuresInLoop = playMeasuresInLoop;
        options.playMeasuresInLoopStart = playMeasuresInLoopStart;
        options.playMeasuresInLoopEnd = playMeasuresInLoopEnd;
        options.lastMeasure = lastMeasure;
        options.tempo = tempo;
        options.pauseTime = pauseTime;
        
        options.shifttime = shifttime;
        options.largeNoteSize = largeNoteSize;
        return options; 
    }
}



