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

/** @class MidiOptions
 * The MidiOptions class contains the available options for
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
        scrollVert = true;
        largeNoteSize = true;
        if (tracks.length != 2) {
            twoStaffs = true;
        }
        else {
            twoStaffs = false;
        }
        showNoteLetters = NoteNameNone;
        showMeasures = false;
        showLyrics = true;
        shifttime = 0;
        transpose = 0;
        time = null;
        defaultTime = midifile.getTime();
        key = -1;
        combineInterval = 40;
        shade1Color = Color.rgb(210, 205, 220);
        shade2Color = Color.rgb(150, 200, 220);
        

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
            json.put("key", key);
            json.put("combineInterval", combineInterval);
            json.put("shade1Color", shade1Color);
            json.put("shade2Color", shade2Color);
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
            options.key = json.getInt("key");
            options.combineInterval = json.getInt("combineInterval");
            options.shade1Color = json.getInt("shade1Color");
            options.shade2Color = json.getInt("shade2Color");
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
            for (int i = 0; i < tracks.length; i++) {
                tracks[i] = saved.tracks[i];
            }
        }
        if (saved.mute.length == mute.length) {
            for (int i = 0; i < mute.length; i++) {
                mute[i] = saved.mute[i];
            }
        }
        if (saved.instruments.length == instruments.length) {
            for (int i = 0; i < instruments.length; i++) {
                instruments[i] = saved.instruments[i];
            }
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
        key = saved.key;
        combineInterval = saved.combineInterval;
        shade1Color = saved.shade1Color;
        shade2Color = saved.shade2Color;
        showMeasures = saved.showMeasures;
        playMeasuresInLoop = saved.playMeasuresInLoop;
        playMeasuresInLoopStart = saved.playMeasuresInLoopStart;
        playMeasuresInLoopEnd = saved.playMeasuresInLoopEnd;
    }
 

    @Override
    public String toString() {
        String result = "MidiOptions: tracks: ";
        for (int i = 0; i < tracks.length; i++) {
            result += tracks[i] + ", ";
        }
        result += " Instruments: ";
        for (int i = 0; i < instruments.length; i++) {
            result += instruments[i] + ", ";
        }
        result += " scrollVert " + scrollVert;
        result += " twoStaffs " + twoStaffs;
        result += " transpose" + transpose;
        result += " key " + key;
        result += " combine " + combineInterval;
        result += " tempo " + tempo;
        result += " pauseTime " + pauseTime;
        if (time != null) {
            result += " time " + time.toString();
        }
        return result;
    }

    public MidiOptions copy() {
        MidiOptions options = new MidiOptions();
        options.tracks = new boolean[tracks.length];
        for (int i = 0; i < tracks.length; i++) {
            options.tracks[i] = tracks[i];
        }
        options.mute = new boolean[mute.length];
        for (int i = 0; i < mute.length; i++) {
            options.mute[i] = mute[i];
        }
        options.instruments = new int[instruments.length];
        for (int i = 0; i < instruments.length; i++) {
            options.instruments[i] = instruments[i];
        }
        options.defaultTime = defaultTime;
        options.time = time;
        options.useDefaultInstruments = useDefaultInstruments;
        options.scrollVert = scrollVert;
        options.showPiano = showPiano;
        options.showLyrics = showLyrics;
        options.twoStaffs = twoStaffs;
        options.showNoteLetters = showNoteLetters;
        options.transpose = transpose;
        options.key = key;
        options.combineInterval = combineInterval;
        options.shade1Color = shade1Color;
        options.shade2Color = shade2Color;
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



