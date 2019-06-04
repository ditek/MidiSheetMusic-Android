/*
 * Copyright (c) 2007-2012 Madhav Vaidyanathan
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
import java.nio.charset.StandardCharsets;
import java.util.*;
import android.util.*;

/** @class Pair - A pair of ints */
class PairInt {
    public int low;
    public int high;
}



/* MIDI file format.
 *
 * The Midi File format is described below.  The description uses
 * the following abbreviations.
 *
 * u1     - One byte
 * u2     - Two bytes (big endian)
 * u4     - Four bytes (big endian)
 * varlen - A variable length integer, that can be 1 to 4 bytes. The 
 *          integer ends when you encounter a byte that doesn't have 
 *          the 8th bit set (a byte less than 0x80).
 * len?   - The length of the data depends on some code
 *          
 *
 * The Midi files begins with the main Midi header
 * u4 = The four ascii characters 'MThd'
 * u4 = The length of the MThd header = 6 bytes
 * u2 = 0 if the file contains a single track
 *      1 if the file contains one or more simultaneous tracks
 *      2 if the file contains one or more independent tracks
 * u2 = number of tracks
 * u2 = if >  0, the number of pulses per quarter note
 *      if <= 0, then ???
 *
 * Next come the individual Midi tracks.  The total number of Midi
 * tracks was given above, in the MThd header.  Each track starts
 * with a header:
 *
 * u4 = The four ascii characters 'MTrk'
 * u4 = Amount of track data, in bytes.
 * 
 * The track data consists of a series of Midi events.  Each Midi event
 * has the following format:
 *
 * varlen  - The time between the previous event and this event, measured
 *           in "pulses".  The number of pulses per quarter note is given
 *           in the MThd header.
 * u1      - The Event code, always betwee 0x80 and 0xFF
 * len?    - The event data.  The length of this data is determined by the
 *           event code.  The first byte of the event data is always < 0x80.
 *
 * The event code is optional.  If the event code is missing, then it
 * defaults to the previous event code.  For example:
 *
 *   varlen, eventcode1, eventdata,
 *   varlen, eventcode2, eventdata,
 *   varlen, eventdata,  // eventcode is eventcode2
 *   varlen, eventdata,  // eventcode is eventcode2
 *   varlen, eventcode3, eventdata,
 *   ....
 *
 *   How do you know if the eventcode is there or missing? Well:
 *   - All event codes are between 0x80 and 0xFF
 *   - The first byte of eventdata is always less than 0x80.
 *   So, after the varlen delta time, if the next byte is between 0x80
 *   and 0xFF, its an event code.  Otherwise, its event data.
 *
 * The Event codes and event data for each event code are shown below.
 *
 * Code:  u1 - 0x80 thru 0x8F - Note Off event.
 *             0x80 is for channel 1, 0x8F is for channel 16.
 * Data:  u1 - The note number, 0-127.  Middle C is 60 (0x3C)
 *        u1 - The note velocity.  This should be 0
 * 
 * Code:  u1 - 0x90 thru 0x9F - Note On event.
 *             0x90 is for channel 1, 0x9F is for channel 16.
 * Data:  u1 - The note number, 0-127.  Middle C is 60 (0x3C)
 *        u1 - The note velocity, from 0 (no sound) to 127 (loud).
 *             A value of 0 is equivalent to a Note Off.
 *
 * Code:  u1 - 0xA0 thru 0xAF - Key Pressure
 * Data:  u1 - The note number, 0-127.
 *        u1 - The pressure.
 *
 * Code:  u1 - 0xB0 thru 0xBF - Control Change
 * Data:  u1 - The controller number
 *        u1 - The value
 *
 * Code:  u1 - 0xC0 thru 0xCF - Program Change
 * Data:  u1 - The program number.
 *
 * Code:  u1 - 0xD0 thru 0xDF - Channel Pressure
 *        u1 - The pressure.
 *
 * Code:  u1 - 0xE0 thru 0xEF - Pitch Bend
 * Data:  u2 - Some data
 *
 * Code:  u1     - 0xFF - Meta Event
 * Data:  u1     - Metacode
 *        varlen - Length of meta event
 *        u1[varlen] - Meta event data.
 *
 *
 * The Meta Event codes are listed below:
 *
 * Metacode: u1         - 0x0  Sequence Number
 *           varlen     - 0 or 2
 *           u1[varlen] - Sequence number
 *
 * Metacode: u1         - 0x1  Text
 *           varlen     - Length of text
 *           u1[varlen] - Text
 *
 * Metacode: u1         - 0x2  Copyright
 *           varlen     - Length of text
 *           u1[varlen] - Text
 *
 * Metacode: u1         - 0x3  Track Name
 *           varlen     - Length of name
 *           u1[varlen] - Track Name
 *
 * Metacode: u1         - 0x58  Time Signature
 *           varlen     - 4 
 *           u1         - numerator
 *           u1         - log2(denominator)
 *           u1         - clocks in metronome click
 *           u1         - 32nd notes in quarter note (usually 8)
 *
 * Metacode: u1         - 0x59  Key Signature
 *           varlen     - 2
 *           u1         - if >= 0, then number of sharps
 *                        if < 0, then number of flats * -1
 *           u1         - 0 if major key
 *                        1 if minor key
 *
 * Metacode: u1         - 0x51  Tempo
 *           varlen     - 3  
 *           u3         - quarter note length in microseconds
 */


/** @class MidiFile
 *
 * The MidiFile class contains the parsed data from the Midi File.
 * It contains:
 * - All the tracks in the midi file, including all MidiNotes per track.
 * - The time signature (e.g. 4/4, 3/4, 6/8)
 * - The number of pulses per quarter note.
 * - The tempo (number of microseconds per quarter note).
 *
 * The constructor takes a filename as input, and upon returning,
 * contains the parsed data from the midi file.
 *
 * The methods ReadTrack() and ReadMetaEvent() are helper functions called
 * by the constructor during the parsing.
 *
 * After the MidiFile is parsed and created, the user can retrieve the 
 * tracks and notes by using the property Tracks and Tracks.Notes.
 *
 * There are two methods for modifying the midi data based on the menu
 * options selected:
 *
 * - ChangeMidiNotes()
 *   Apply the menu options to the parsed MidiFile.  This uses the helper functions:
 *     SplitTrack()
 *     CombineToTwoTracks()
 *     ShiftTime()
 *     Transpose()
 *     RoundStartTimes()
 *     RoundDurations()
 *
 * - ChangeSound()
 *   Apply the menu options to the MIDI music data, and save the modified midi data
 *   to a file, for playback. 
 *   
 */

public class MidiFile {
    private FileUri fileuri;          /** The file reference */
    private String filename;          /** The Midi file name */
    private ArrayList<ArrayList<MidiEvent>> allevents; /** The raw MidiEvents, one list per track */
    private ArrayList<MidiTrack> tracks ;  /** The tracks of the midifile that have notes */
    private short trackmode;         /** 0 (single track), 1 (simultaneous tracks) 2 (independent tracks) */
    private TimeSignature timesig;    /** The time signature */
    private int quarternote;          /** The number of pulses per quarter note */
    private int totalpulses;          /** The total length of the song, in pulses */
    private boolean trackPerChannel;  /** True if we've split each channel into a track */

    /* The list of Midi Events */
    public static final byte EventNoteOff         = (byte)0x80;
    public static final byte EventNoteOn          = (byte)0x90;
    public static final byte EventKeyPressure     = (byte)0xA0;
    public static final byte EventControlChange   = (byte)0xB0;
    public static final byte EventProgramChange   = (byte)0xC0;
    public static final byte EventChannelPressure = (byte)0xD0;
    public static final byte EventPitchBend       = (byte)0xE0;
    public static final byte SysexEvent1          = (byte)0xF0;
    public static final byte SysexEvent2          = (byte)0xF7;
    public static final byte MetaEvent            = (byte)0xFF;

    /* The list of Meta Events */
    public static final byte MetaEventSequence      = (byte)0x0;
    public static final byte MetaEventText          = (byte)0x1;
    public static final byte MetaEventCopyright     = (byte)0x2;
    public static final byte MetaEventSequenceName  = (byte)0x3;
    public static final byte MetaEventInstrument    = (byte)0x4;
    public static final byte MetaEventLyric         = (byte)0x5;
    public static final byte MetaEventMarker        = (byte)0x6;
    public static final byte MetaEventEndOfTrack    = (byte)0x2F;
    public static final byte MetaEventTempo         = (byte)0x51;
    public static final byte MetaEventSMPTEOffset   = (byte)0x54;
    public static final byte MetaEventTimeSignature = (byte)0x58;
    public static final byte MetaEventKeySignature  = (byte)0x59;

    /* The Program Change event gives the instrument that should
     * be used for a particular channel.  The following table
     * maps each instrument number (0 thru 128) to an instrument
     * name.
     */
    public static String[] Instruments = {
        "Acoustic Grand Piano",
        "Bright Acoustic Piano",
        "Electric Grand Piano",
        "Honky-tonk Piano",
        "Electric Piano 1",
        "Electric Piano 2",
        "Harpsichord",
        "Clavi",
        "Celesta",
        "Glockenspiel",
        "Music Box",
        "Vibraphone",
        "Marimba",
        "Xylophone",
        "Tubular Bells",
        "Dulcimer",
        "Drawbar Organ",
        "Percussive Organ",
        "Rock Organ",
        "Church Organ",
        "Reed Organ",
        "Accordion",
        "Harmonica",
        "Tango Accordion",
        "Acoustic Guitar (nylon)",
        "Acoustic Guitar (steel)",
        "Electric Guitar (jazz)",
        "Electric Guitar (clean)",
        "Electric Guitar (muted)",
        "Overdriven Guitar",
        "Distortion Guitar",
        "Guitar harmonics",
        "Acoustic Bass",
        "Electric Bass (finger)",
        "Electric Bass (pick)",
        "Fretless Bass",
        "Slap Bass 1",
        "Slap Bass 2",
        "Synth Bass 1",
        "Synth Bass 2",
        "Violin",
        "Viola",
        "Cello",
        "Contrabass",
        "Tremolo Strings",
        "Pizzicato Strings",
        "Orchestral Harp",
        "Timpani",
        "String Ensemble 1",
        "String Ensemble 2",
        "SynthStrings 1",
        "SynthStrings 2",
        "Choir Aahs",
        "Voice Oohs",
        "Synth Voice",
        "Orchestra Hit",
        "Trumpet",
        "Trombone",
        "Tuba",
        "Muted Trumpet",
        "French Horn",
        "Brass Section",
        "SynthBrass 1",
        "SynthBrass 2",
        "Soprano Sax",
        "Alto Sax",
        "Tenor Sax",
        "Baritone Sax",
        "Oboe",
        "English Horn",
        "Bassoon",
        "Clarinet",
        "Piccolo",
        "Flute",
        "Recorder",
        "Pan Flute",
        "Blown Bottle",
        "Shakuhachi",
        "Whistle",
        "Ocarina",
        "Lead 1 (square)",
        "Lead 2 (sawtooth)",
        "Lead 3 (calliope)",
        "Lead 4 (chiff)",
        "Lead 5 (charang)",
        "Lead 6 (voice)",
        "Lead 7 (fifths)",
        "Lead 8 (bass + lead)",
        "Pad 1 (new age)",
        "Pad 2 (warm)",
        "Pad 3 (polysynth)",
        "Pad 4 (choir)",
        "Pad 5 (bowed)",
        "Pad 6 (metallic)",
        "Pad 7 (halo)",
        "Pad 8 (sweep)",
        "FX 1 (rain)",
        "FX 2 (soundtrack)",
        "FX 3 (crystal)",
        "FX 4 (atmosphere)",
        "FX 5 (brightness)",
        "FX 6 (goblins)",
        "FX 7 (echoes)",
        "FX 8 (sci-fi)",
        "Sitar",
        "Banjo",
        "Shamisen",
        "Koto",
        "Kalimba",
        "Bag pipe",
        "Fiddle",
        "Shanai",
        "Tinkle Bell",
        "Agogo",
        "Steel Drums",
        "Woodblock",
        "Taiko Drum",
        "Melodic Tom",
        "Synth Drum",
        "Reverse Cymbal",
        "Guitar Fret Noise",
        "Breath Noise",
        "Seashore",
        "Bird Tweet",
        "Telephone Ring",
        "Helicopter",
        "Applause",
        "Gunshot",
        "Percussion"
    };
    /* End Instruments */

    /** Return a String representation of a Midi event */
    private String EventName(int ev) {
        if (ev >= EventNoteOff && ev < EventNoteOff + 16)
            return "NoteOff";
        else if (ev >= EventNoteOn && ev < EventNoteOn + 16) 
            return "NoteOn";
        else if (ev >= EventKeyPressure && ev < EventKeyPressure + 16) 
            return "KeyPressure";
        else if (ev >= EventControlChange && ev < EventControlChange + 16) 
            return "ControlChange";
        else if (ev >= EventProgramChange && ev < EventProgramChange + 16) 
            return "ProgramChange";
        else if (ev >= EventChannelPressure && ev < EventChannelPressure + 16)
            return "ChannelPressure";
        else if (ev >= EventPitchBend && ev < EventPitchBend + 16)
            return "PitchBend";
        else if (ev == MetaEvent)
            return "MetaEvent";
        else if (ev == SysexEvent1 || ev == SysexEvent2)
            return "SysexEvent";
        else
            return "Unknown";
    }

    /** Return a String representation of a meta-event */
    private String MetaName(int ev) {
        if (ev == MetaEventSequence)
            return "MetaEventSequence";
        else if (ev == MetaEventText)
            return "MetaEventText";
        else if (ev == MetaEventCopyright)
            return "MetaEventCopyright";
        else if (ev == MetaEventSequenceName)
            return "MetaEventSequenceName";
        else if (ev == MetaEventInstrument)
            return "MetaEventInstrument";
        else if (ev == MetaEventLyric)
            return "MetaEventLyric";
        else if (ev == MetaEventMarker)
            return "MetaEventMarker";
        else if (ev == MetaEventEndOfTrack)
            return "MetaEventEndOfTrack";
        else if (ev == MetaEventTempo)
            return "MetaEventTempo";
        else if (ev == MetaEventSMPTEOffset)
            return "MetaEventSMPTEOffset";
        else if (ev == MetaEventTimeSignature)
            return "MetaEventTimeSignature";
        else if (ev == MetaEventKeySignature)
            return "MetaEventKeySignature";
        else
            return "Unknown";
    }


    /** Get the list of tracks */
    public ArrayList<MidiTrack> getTracks() { return tracks; }

    /** Get the time signature */
    public TimeSignature getTime() { return timesig; }

    /** Get the file name */
    public String getFileName() { return filename; }

    /** Get the total length (in pulses) of the song */
    public int getTotalPulses() { return totalpulses; }


    /** Create a new MidiFile from the byte[] */
    public MidiFile(byte[] rawdata, String filename) {
        this.filename = filename;
        parse(rawdata);
    }

    /** Parse the given Midi file, and return an instance of this MidiFile
     * class.  After reading the midi file, this object will contain:
     * - The raw list of midi events
     * - The Time Signature of the song
     * - All the tracks in the song which contain notes. 
     * - The number, starttime, and duration of each note.
     */
    private void parse(byte[] rawdata) {
        String id;
        int len;

        tracks = new ArrayList<MidiTrack>();
        trackPerChannel = false;

        MidiFileReader file = new MidiFileReader(rawdata);
        id = file.ReadAscii(4);
        if (!id.equals("MThd")) {
            throw new MidiFileException("Doesn't start with MThd", 0);
        }
        len = file.ReadInt(); 
        if (len !=  6) {
            throw new MidiFileException("Bad MThd header", 4);
        }
        trackmode = (short) file.ReadShort();
        int num_tracks = file.ReadShort();
        quarternote = file.ReadShort(); 

        allevents = new ArrayList<ArrayList<MidiEvent>>();
        for (int tracknum = 0; tracknum < num_tracks; tracknum++) {
            allevents.add(ReadTrack(file));
            MidiTrack track = new MidiTrack(allevents.get(tracknum), tracknum);
            if (track.getNotes().size() > 0) {
                tracks.add(track);
            }
        }

        /* Get the length of the song in pulses */
        for (MidiTrack track : tracks) {
            MidiNote last = track.getNotes().get( track.getNotes().size()-1 );
            if (this.totalpulses < last.getStartTime() + last.getDuration()) {
                this.totalpulses = last.getStartTime() + last.getDuration();
            }
        }

        /* If we only have one track with multiple channels, then treat
         * each channel as a separate track.
         */
        if (tracks.size() == 1 && HasMultipleChannels(tracks.get(0))) {
            tracks = SplitChannels(tracks.get(0), allevents.get(tracks.get(0).trackNumber() ));
            trackPerChannel = true;
        }

        CheckStartTimes(tracks);

        /* Determine the time signature */
        int tempoCount = 0;
        long tempo = 0;
        int numer = 0;
        int denom = 0;
        for (ArrayList<MidiEvent> list : allevents) {
            for (MidiEvent mevent : list) {
                if (mevent.Metaevent == MetaEventTempo) {
                    // Take average of all tempos
                    tempo += mevent.Tempo;
                    tempoCount++;
                }
                if (mevent.Metaevent == MetaEventTimeSignature && numer == 0) {
                    numer = mevent.Numerator;
                    denom = mevent.Denominator;
                }
            }
        }
        if (tempo == 0) {
            tempo = 500000; /* 500,000 microseconds = 0.05 sec */
        }
        else {
            tempo = tempo / tempoCount;
        }
        if (numer == 0) {
            numer = 4; denom = 4;
        }
        timesig = new TimeSignature(numer, denom, quarternote, (int)tempo);
    }

    /** Parse a single Midi track into a list of MidiEvents.
     * Entering this function, the file offset should be at the start of
     * the MTrk header.  Upon exiting, the file offset should be at the
     * start of the next MTrk header.
     */
    private ArrayList<MidiEvent> ReadTrack(MidiFileReader file) {
        ArrayList<MidiEvent> result = new ArrayList<MidiEvent>(20);
        int starttime = 0;
        String id = file.ReadAscii(4);

        if (!id.equals("MTrk")) {
            throw new MidiFileException("Bad MTrk header", file.GetOffset() - 4);
        }
        int tracklen = file.ReadInt();
        int trackend = tracklen + file.GetOffset();

        byte eventflag = 0;

        while (file.GetOffset() < trackend) {

            // If the midi file is truncated here, we can still recover.
            // Just return what we've parsed so far.

            int startoffset, deltatime;
            byte peekevent;
            try {
                startoffset = file.GetOffset();
                deltatime = file.ReadVarlen();
                starttime += deltatime;
                peekevent = file.Peek();
            }
            catch (MidiFileException e) {
                return result;
            }

            MidiEvent mevent = new MidiEvent();
            result.add(mevent);
            mevent.DeltaTime = deltatime;
            mevent.StartTime = starttime;

            // if (peekevent >= EventNoteOff) { 
            if (peekevent < 0) {
                mevent.HasEventflag = true; 
                eventflag = file.ReadByte();
            }

            //Log.e("debug",  "offset " + startoffset + 
            //                " event " + eventflag + " " + EventName(eventflag) +
            //                " start " + starttime + " delta " + mevent.DeltaTime);

            if (eventflag >= EventNoteOn && eventflag < EventNoteOn + 16) {
                mevent.EventFlag = EventNoteOn;
                mevent.Channel = ((byte)(eventflag - EventNoteOn));
                mevent.Notenumber = file.ReadByte();
                mevent.Velocity = file.ReadByte();
            }
            else if (eventflag >= EventNoteOff && eventflag < EventNoteOff + 16) {
                mevent.EventFlag = EventNoteOff;
                mevent.Channel = ((byte)(eventflag - EventNoteOff));
                mevent.Notenumber = file.ReadByte();
                mevent.Velocity = file.ReadByte();
            }
            else if (eventflag >= EventKeyPressure && 
                     eventflag < EventKeyPressure + 16) {
                mevent.EventFlag = EventKeyPressure;
                mevent.Channel = ((byte)(eventflag - EventKeyPressure));
                mevent.Notenumber = file.ReadByte();
                mevent.KeyPressure = file.ReadByte();
            }
            else if (eventflag >= EventControlChange && 
                     eventflag < EventControlChange + 16) {
                mevent.EventFlag = EventControlChange;
                mevent.Channel = ((byte)(eventflag - EventControlChange));
                mevent.ControlNum = file.ReadByte();
                mevent.ControlValue = file.ReadByte();
            }
            else if (eventflag >= EventProgramChange && 
                     eventflag < EventProgramChange + 16) {
                mevent.EventFlag = EventProgramChange;
                mevent.Channel = ((byte)(eventflag - EventProgramChange));
                mevent.Instrument = file.ReadByte();
            }
            else if (eventflag >= EventChannelPressure && 
                     eventflag < EventChannelPressure + 16) {
                mevent.EventFlag = EventChannelPressure;
                mevent.Channel = ((byte)(eventflag - EventChannelPressure));
                mevent.ChanPressure = file.ReadByte();
            }
            else if (eventflag >= EventPitchBend && 
                     eventflag < EventPitchBend + 16) {
                mevent.EventFlag = EventPitchBend;
                mevent.Channel = ((byte)(eventflag - EventPitchBend));
                mevent.PitchBend = (short) file.ReadShort();
            }
            else if (eventflag == SysexEvent1) {
                mevent.EventFlag = SysexEvent1;
                mevent.Metalength = file.ReadVarlen();
                mevent.Value = file.ReadBytes(mevent.Metalength);
            }
            else if (eventflag == SysexEvent2) {
                mevent.EventFlag = SysexEvent2;
                mevent.Metalength = file.ReadVarlen();
                mevent.Value = file.ReadBytes(mevent.Metalength);
            }
            else if (eventflag == MetaEvent) {
                mevent.EventFlag = MetaEvent;
                mevent.Metaevent = file.ReadByte();
                mevent.Metalength = file.ReadVarlen();
                mevent.Value = file.ReadBytes(mevent.Metalength);
                if (mevent.Metaevent == MetaEventTimeSignature) {
                    if (mevent.Metalength < 2) {
                        throw new MidiFileException(
                          "Meta Event Time Signature len == " + mevent.Metalength  + 
                          " != 4", file.GetOffset());
                    }
                    else {
                        mevent.Numerator = ((byte)mevent.Value[0]);
                        mevent.Denominator = ((byte)Math.pow(2, mevent.Value[1]));
                    }
                }
                else if (mevent.Metaevent == MetaEventTempo) {
                    if (mevent.Metalength != 3) {
                        throw new MidiFileException(
                          "Meta Event Tempo len == " + mevent.Metalength +
                          " != 3", file.GetOffset());
                    }
                    mevent.Tempo = ((mevent.Value[0] & 0xFF) << 16) | 
                                   ((mevent.Value[1] & 0xFF) << 8) | 
                                    (mevent.Value[2] & 0xFF);
                }
                else if (mevent.Metaevent == MetaEventEndOfTrack) {
                    /* break;  */
                }
            }
            else {
                throw new MidiFileException("Unknown event " + mevent.EventFlag,
                                             file.GetOffset()-1); 
            }
        }

        return result;
    }

    /** Return true if this track contains multiple channels.
     * If a MidiFile contains only one track, and it has multiple channels,
     * then we treat each channel as a separate track.
     */
    static boolean HasMultipleChannels(MidiTrack track) {
        int channel = track.getNotes().get(0).getChannel();
        for (MidiNote note : track.getNotes()) {
            if (note.getChannel() != channel) {
                return true;
            }
        }
        return false;
    }

    /** Write a variable length number to the buffer at the given offset.
     * Return the number of bytes written.
     */
    static int VarlenToBytes(int num, byte[] buf, int offset) {
        byte b1 = (byte) ((num >> 21) & 0x7F);
        byte b2 = (byte) ((num >> 14) & 0x7F);
        byte b3 = (byte) ((num >>  7) & 0x7F);
        byte b4 = (byte) (num & 0x7F);

        if (b1 > 0) {
            buf[offset]   = (byte)(b1 | 0x80);
            buf[offset+1] = (byte)(b2 | 0x80);
            buf[offset+2] = (byte)(b3 | 0x80);
            buf[offset+3] = b4;
            return 4;
        }
        else if (b2 > 0) {
            buf[offset]   = (byte)(b2 | 0x80);
            buf[offset+1] = (byte)(b3 | 0x80);
            buf[offset+2] = b4;
            return 3;
        }
        else if (b3 > 0) {
            buf[offset]   = (byte)(b3 | 0x80);
            buf[offset+1] = b4;
            return 2;
        }
        else {
            buf[offset] = b4;
            return 1;
        }
    }

    /** Write a 4-byte integer to data[offset : offset+4] */
    private static void IntToBytes(int value, byte[] data, int offset) {
        data[offset] = (byte)( (value >> 24) & 0xFF );
        data[offset+1] = (byte)( (value >> 16) & 0xFF );
        data[offset+2] = (byte)( (value >> 8) & 0xFF );
        data[offset+3] = (byte)( value & 0xFF );
    }

    /** Calculate the track length (in bytes) given a list of Midi events */
    private static int GetTrackLength(ArrayList<MidiEvent> events) {
        int len = 0;
        byte[] buf = new byte[1024];
        for (MidiEvent mevent : events) {
            len += VarlenToBytes(mevent.DeltaTime, buf, 0);
            len += 1;  /* for eventflag */
            switch (mevent.EventFlag) {
                case EventNoteOn: len += 2; break;
                case EventNoteOff: len += 2; break;
                case EventKeyPressure: len += 2; break;
                case EventControlChange: len += 2; break;
                case EventProgramChange: len += 1; break;
                case EventChannelPressure: len += 1; break;
                case EventPitchBend: len += 2; break;

                case SysexEvent1: 
                case SysexEvent2:
                    len += VarlenToBytes(mevent.Metalength, buf, 0); 
                    len += mevent.Metalength;
                    break;
                case MetaEvent: 
                    len += 1; 
                    len += VarlenToBytes(mevent.Metalength, buf, 0); 
                    len += mevent.Metalength;
                    break;
                default: break;
            }
        }
        return len;
    }


    /** Copy len bytes from src to dest, at the given offsets */
    private static void
    ArrayCopy(byte[] src, int srcoffset, byte[] dest, int destoffset, int len) {
        if (len >= 0)
            System.arraycopy(src, srcoffset, dest, destoffset, len);
    }

            
    /** Write the given list of Midi events to a stream/file.
     *  This method is used for sound playback, for creating new Midi files
     *  with the tempo, transpose, etc changed.
     *
     *  Return true on success, and false on error.
     */
    private static void
    WriteEvents(FileOutputStream file, ArrayList<ArrayList<MidiEvent>> allevents, 
                  int trackmode, int quarter) throws IOException {

        byte[] buf = new byte[16384];

        /* Write the MThd, len = 6, track mode, number tracks, quarter note */
        file.write("MThd".getBytes(StandardCharsets.US_ASCII), 0, 4);
        IntToBytes(6, buf, 0);
        file.write(buf, 0, 4);
        buf[0] = (byte)(trackmode >> 8); 
        buf[1] = (byte)(trackmode & 0xFF);
        file.write(buf, 0, 2);
        buf[0] = 0; 
        buf[1] = (byte)allevents.size();
        file.write(buf, 0, 2);
        buf[0] = (byte)(quarter >> 8); 
        buf[1] = (byte)(quarter & 0xFF);
        file.write(buf, 0, 2);

        for (ArrayList<MidiEvent> list : allevents) {
            /* Write the MTrk header and track length */
            file.write("MTrk".getBytes(StandardCharsets.US_ASCII), 0, 4);
            int len = GetTrackLength(list);
            IntToBytes(len, buf, 0);
            file.write(buf, 0, 4);

            for (MidiEvent mevent : list) {
                int varlen = VarlenToBytes(mevent.DeltaTime, buf, 0);
                file.write(buf, 0, varlen);

                if (mevent.EventFlag == SysexEvent1 ||
                    mevent.EventFlag == SysexEvent2 ||
                    mevent.EventFlag == MetaEvent) {
                    buf[0] = mevent.EventFlag;
                }
                else {
                    buf[0] = (byte)(mevent.EventFlag + mevent.Channel);
                }
                file.write(buf, 0, 1);

                if (mevent.EventFlag == EventNoteOn) {
                    buf[0] = mevent.Notenumber;
                    buf[1] = mevent.Velocity;
                    file.write(buf, 0, 2);
                }
                else if (mevent.EventFlag == EventNoteOff) {
                    buf[0] = mevent.Notenumber;
                    buf[1] = mevent.Velocity;
                    file.write(buf, 0, 2);
                }
                else if (mevent.EventFlag == EventKeyPressure) {
                    buf[0] = mevent.Notenumber;
                    buf[1] = mevent.KeyPressure;
                    file.write(buf, 0, 2);
                }
                else if (mevent.EventFlag == EventControlChange) {
                    buf[0] = mevent.ControlNum;
                    buf[1] = mevent.ControlValue;
                    file.write(buf, 0, 2);
                }
                else if (mevent.EventFlag == EventProgramChange) {
                    buf[0] = mevent.Instrument;
                    file.write(buf, 0, 1);
                }
                else if (mevent.EventFlag == EventChannelPressure) {
                    buf[0] = mevent.ChanPressure;
                    file.write(buf, 0, 1);
                }
                else if (mevent.EventFlag == EventPitchBend) {
                    buf[0] = (byte)(mevent.PitchBend >> 8);
                    buf[1] = (byte)(mevent.PitchBend & 0xFF);
                    file.write(buf, 0, 2);
                }
                else if (mevent.EventFlag == SysexEvent1) {
                    int offset = VarlenToBytes(mevent.Metalength, buf, 0);
                    ArrayCopy(mevent.Value, 0, buf, offset, mevent.Value.length);
                    file.write(buf, 0, offset + mevent.Value.length);
                }
                else if (mevent.EventFlag == SysexEvent2) {
                    int offset = VarlenToBytes(mevent.Metalength, buf, 0);
                    ArrayCopy(mevent.Value, 0, buf, offset, mevent.Value.length);
                    file.write(buf, 0, offset + mevent.Value.length);
                }
                else if (mevent.EventFlag == MetaEvent && mevent.Metaevent == MetaEventTempo) {
                    buf[0] = mevent.Metaevent;
                    buf[1] = 3;
                    buf[2] = (byte)((mevent.Tempo >> 16) & 0xFF);
                    buf[3] = (byte)((mevent.Tempo >> 8) & 0xFF);
                    buf[4] = (byte)(mevent.Tempo & 0xFF);
                    file.write(buf, 0, 5);
                }
                else if (mevent.EventFlag == MetaEvent) {
                    buf[0] = mevent.Metaevent;
                    int offset = VarlenToBytes(mevent.Metalength, buf, 1) + 1;
                    ArrayCopy(mevent.Value, 0, buf, offset, mevent.Value.length);
                    file.write(buf, 0, offset + mevent.Value.length);
                }
            }
        }
        file.close();
    }


    /** Clone the list of MidiEvents */
    private static ArrayList<ArrayList<MidiEvent>> 
    CloneMidiEvents(ArrayList<ArrayList<MidiEvent>> origlist) {
        ArrayList<ArrayList<MidiEvent>> newlist = 
           new ArrayList<ArrayList<MidiEvent>>(origlist.size());
        for (int tracknum = 0; tracknum < origlist.size(); tracknum++) {
            ArrayList<MidiEvent> origevents = origlist.get(tracknum);
            ArrayList<MidiEvent> newevents = new ArrayList<MidiEvent>(origevents.size());
            newlist.add(newevents);
            for (MidiEvent mevent : origevents) {
                newevents.add( mevent.Clone() );
            }
        }
        return newlist;
    }

    /** Create a new Midi tempo event, with the given tempo  */
    private static MidiEvent CreateTempoEvent(int tempo) {
        MidiEvent mevent = new MidiEvent();
        mevent.DeltaTime = 0;
        mevent.StartTime = 0;
        mevent.HasEventflag = true;
        mevent.EventFlag = MetaEvent;
        mevent.Metaevent = MetaEventTempo;
        mevent.Metalength = 3;
        mevent.Tempo = tempo;
        return mevent;
    }


    /** Search the events for a ControlChange event with the same
     *  channel and control number.  If a matching event is found,
     *  update the control value.  Else, add a new ControlChange event.
     */
    private static void
    UpdateControlChange(ArrayList<MidiEvent> newevents, MidiEvent changeEvent) {
        for (MidiEvent mevent : newevents) {
            if ((mevent.EventFlag == changeEvent.EventFlag) &&
                (mevent.Channel == changeEvent.Channel) &&
                (mevent.ControlNum == changeEvent.ControlNum)) {

                mevent.ControlValue = changeEvent.ControlValue;
                return;
            }
        }
        newevents.add(changeEvent);
    }


    /** Start the Midi music at the given pause time (in pulses).
     *  Remove any NoteOn/NoteOff events that occur before the pause time.
     *  For other events, change the delta-time to 0 if they occur
     *  before the pause time.  Return the modified Midi Events.
     */
    private static 
    ArrayList<ArrayList<MidiEvent>> StartAtPauseTime(ArrayList<ArrayList<MidiEvent>> list, int pauseTime) {
        ArrayList<ArrayList<MidiEvent>> newlist = new ArrayList<ArrayList<MidiEvent>>(list.size());
        for (int tracknum = 0; tracknum < list.size(); tracknum++) {
            ArrayList<MidiEvent> events = list.get(tracknum);
            ArrayList<MidiEvent> newevents = new ArrayList<MidiEvent>(events.size());
            newlist.add(newevents);

            boolean foundEventAfterPause = false;
            for (MidiEvent mevent : events) {

                if (mevent.StartTime < pauseTime) {
                    if (mevent.EventFlag == EventNoteOn ||
                        mevent.EventFlag == EventNoteOff) {

                        /* Skip NoteOn/NoteOff event */
                    }
                    else if (mevent.EventFlag == EventControlChange) {
                        mevent.DeltaTime = 0;
                        UpdateControlChange(newevents, mevent);
                    }
                    else {
                        mevent.DeltaTime = 0;
                        newevents.add(mevent);
                    }
                }
                else if (!foundEventAfterPause) {
                    mevent.DeltaTime = (mevent.StartTime - pauseTime);
                    newevents.add(mevent);
                    foundEventAfterPause = true;
                }
                else {
                    newevents.add(mevent);
                }
            }
        }
        return newlist;
    }

    /** Write this Midi file to the given file.
     * If options is not null, apply those options to the midi events
     * before performing the write.
     * Return true if the file was saved successfully, else false.
     */
    public void ChangeSound(FileOutputStream destfile, MidiOptions options)
      throws IOException {
        Write(destfile, options);
    }

    public void Write(FileOutputStream destfile, MidiOptions options) 
      throws IOException {
        ArrayList<ArrayList<MidiEvent>> newevents = allevents;
        if (options != null) {
            newevents = ApplyOptionsToEvents(options);
        }
        WriteEvents(destfile, newevents, trackmode, quarternote);
    }

    /** Apply the following sound options to the midi events:
     * - The tempo (the microseconds per pulse)
     * - The instruments per track
     * - The note number (transpose value)
     * - The tracks to include
     * Return the modified list of midi events.
     */
    public ArrayList<ArrayList<MidiEvent>>
    ApplyOptionsToEvents(MidiOptions options) {
        int i;
        if (trackPerChannel) {
            return ApplyOptionsPerChannel(options);
        }

        /* A midifile can contain tracks with notes and tracks without notes.
         * The options.tracks and options.instruments are for tracks with notes.
         * So the track numbers in 'options' may not match correctly if the
         * midi file has tracks without notes. Re-compute the instruments, and 
         * tracks to keep.
         */
        int num_tracks = allevents.size();
        int[] instruments = new int[num_tracks];
        boolean[] keeptracks = new boolean[num_tracks];
        for (i = 0; i < num_tracks; i++) {
            instruments[i] = 0;
            keeptracks[i] = true;
        }
        for (int tracknum = 0; tracknum < tracks.size(); tracknum++) {
            MidiTrack track = tracks.get(tracknum);
            int realtrack = track.trackNumber();
            instruments[realtrack] = options.instruments[tracknum];
            if (!options.tracks[tracknum] || options.mute[tracknum]) {
                keeptracks[realtrack] = false;
            }
        }

        ArrayList<ArrayList<MidiEvent>> newevents = CloneMidiEvents(allevents);

        /* Set the tempo at the beginning of each track */
        for (int tracknum = 0; tracknum < newevents.size(); tracknum++) {
            MidiEvent mevent = CreateTempoEvent(options.tempo);
            newevents.get(tracknum).add(0, mevent);
        }

        /* Change the note number (transpose), instrument, and tempo */
        for (int tracknum = 0; tracknum < newevents.size(); tracknum++) {
            for (MidiEvent mevent : newevents.get(tracknum)) {
                int num = mevent.Notenumber + options.transpose;
                if (num < 0)
                    num = 0;
                if (num > 127)
                    num = 127;
                mevent.Notenumber = (byte)num;
                if (!options.useDefaultInstruments) {
                    mevent.Instrument = (byte)instruments[tracknum];
                }
                mevent.Tempo = options.tempo;
            }
        }

        if (options.pauseTime != 0) {
            newevents = StartAtPauseTime(newevents, options.pauseTime);
        }

        /* Change the tracks to include */
        int count = 0;
        for (boolean keeptrack : keeptracks) {
            if (keeptrack) {
                count++;
            }
        }
        ArrayList<ArrayList<MidiEvent>> result = new ArrayList<ArrayList<MidiEvent>>(count);
        i = 0;
        for (int tracknum = 0; tracknum < keeptracks.length; tracknum++) {
            if (keeptracks[tracknum]) {
                result.add(newevents.get(tracknum));
                i++;
            }
        }
        return result;
    }


    /** Apply the following sound options to the midi events:
     * - The tempo (the microseconds per pulse)
     * - The instruments per track
     * - The note number (transpose value)
     * - The tracks to include
     * Return the modified list of midi events.
     *
     * This Midi file only has one actual track, but we've split that
     * into multiple fake tracks, one per channel, and displayed that
     * to the end-user.  So changing the instrument, and tracks to
     * include, is implemented differently than the ApplyOptionsToEvents() method:
     *
     * - We change the instrument based on the channel, not the track.
     * - We include/exclude channels, not tracks.
     * - We exclude a channel by setting the note volume/velocity to 0.
     */
    public ArrayList<ArrayList<MidiEvent>>
    ApplyOptionsPerChannel(MidiOptions options) {
        /* Determine which channels to include/exclude.
         * Also, determine the instruments for each channel.
         */
        int[] instruments = new int[16];
        boolean[] keepchannel = new boolean[16];
        for (int i = 0; i < 16; i++) {
            instruments[i] = 0;
            keepchannel[i] = true;
        }
        for (int tracknum = 0; tracknum < tracks.size(); tracknum++) {
            MidiTrack track = tracks.get(tracknum);
            int channel = track.getNotes().get(0).getChannel();
            instruments[channel] = options.instruments[tracknum];
            if (!options.tracks[tracknum] || options.mute[tracknum]) {
                keepchannel[channel] = false;
            }
        }
        
        ArrayList<ArrayList<MidiEvent>> newevents = CloneMidiEvents(allevents);

        /* Set the tempo at the beginning of each track */
        for (int tracknum = 0; tracknum < newevents.size(); tracknum++) {
            MidiEvent mevent = CreateTempoEvent(options.tempo);
            newevents.get(tracknum).add(0, mevent);
        }

        /* Change the note number (transpose), instrument, and tempo */
        for (int tracknum = 0; tracknum < newevents.size(); tracknum++) {
            for (MidiEvent mevent : newevents.get(tracknum)) {
                int num = mevent.Notenumber + options.transpose;
                if (num < 0)
                    num = 0;
                if (num > 127)
                    num = 127;
                mevent.Notenumber = (byte)num;
                if (!keepchannel[mevent.Channel]) {
                    mevent.Velocity = 0;
                }
                if (!options.useDefaultInstruments) {
                    mevent.Instrument = (byte)instruments[mevent.Channel];
                }
                mevent.Tempo = options.tempo;
            }
        }
        if (options.pauseTime != 0) {
            newevents = StartAtPauseTime(newevents, options.pauseTime);
        }
        return newevents;
    }


    /** Apply the given sheet music options to the MidiNotes.
     *  Return the midi tracks with the changes applied.
     */
    public ArrayList<MidiTrack> ChangeMidiNotes(MidiOptions options) {
        ArrayList<MidiTrack> newtracks = new ArrayList<MidiTrack>();

        for (int track = 0; track < tracks.size(); track++) {
            if (options.tracks[track]) {
                newtracks.add(tracks.get(track).Clone() );
            }
        }

        /* To make the sheet music look nicer, we round the start times
         * so that notes close together appear as a single chord.  We
         * also extend the note durations, so that we have longer notes
         * and fewer rest symbols.
         */
        TimeSignature time = timesig;
        if (options.time != null) {
            time = options.time;
        }
        MidiFile.RoundStartTimes(newtracks, options.combineInterval, timesig);
        MidiFile.RoundDurations(newtracks, time.getQuarter());

        if (options.twoStaffs) {
            newtracks = MidiFile.CombineToTwoTracks(newtracks, timesig.getMeasure());
        }
        if (options.shifttime != 0) {
            MidiFile.ShiftTime(newtracks, options.shifttime);
        }
        if (options.transpose != 0) {
            MidiFile.Transpose(newtracks, options.transpose);
        }

        return newtracks;
    }


    /** Shift the starttime of the notes by the given amount.
     * This is used by the Shift Notes menu to shift notes left/right.
     */
    public static void
    ShiftTime(ArrayList<MidiTrack> tracks, int amount)
    {
        for (MidiTrack track : tracks) {
            for (MidiNote note : track.getNotes()) {
                note.setStartTime(note.getStartTime() + amount);
            }
        }
    }

    /** Shift the note keys up/down by the given amount */
    public static void
    Transpose(ArrayList<MidiTrack> tracks, int amount)
    {
        for (MidiTrack track : tracks) {
            for (MidiNote note : track.getNotes()) {
                note.setNumber(note.getNumber() + amount);
                if (note.getNumber() < 0) {
                    note.setNumber(0);
                }
            }
        }
    }

   
    /* Find the highest and lowest notes that overlap this interval (starttime to endtime).
     * This method is used by SplitTrack to determine which staff (top or bottom) a note
     * should go to.
     *
     * For more accurate SplitTrack() results, we limit the interval/duration of this note 
     * (and other notes) to one measure. We care only about high/low notes that are
     * reasonably close to this note.
     */
    private static void
    FindHighLowNotes(ArrayList<MidiNote> notes, int measurelen, int startindex, 
                     int starttime, int endtime, PairInt pair) {

        int i = startindex;
        if (starttime + measurelen < endtime) {
            endtime = starttime + measurelen;
        }

        while (i < notes.size() && notes.get(i).getStartTime() < endtime) {
            if (notes.get(i).getEndTime() < starttime) {
                i++;
                continue;
            }
            if (notes.get(i).getStartTime() + measurelen < starttime) {
                i++;
                continue;
            }
            if (pair.high < notes.get(i).getNumber()) {
                pair.high = notes.get(i).getNumber();
            }
            if (pair.low > notes.get(i).getNumber()) {
                pair.low = notes.get(i).getNumber();
            }
            i++;
        }
    }

    /* Find the highest and lowest notes that start at this exact start time */
    private static void
    FindExactHighLowNotes(ArrayList<MidiNote> notes, int startindex, int starttime,
                          PairInt pair) {

        int i = startindex;

        while (notes.get(i).getStartTime() < starttime) {
            i++;
        }

        while (i < notes.size() && notes.get(i).getStartTime() == starttime) {
            if (pair.high < notes.get(i).getNumber()) {
                pair.high = notes.get(i).getNumber();
            }
            if (pair.low > notes.get(i).getNumber()) {
                pair.low = notes.get(i).getNumber();
            }
            i++;
        }
    }


 
    /* Split the given MidiTrack into two tracks, top and bottom.
     * The highest notes will go into top, the lowest into bottom.
     * This function is used to split piano songs into left-hand (bottom)
     * and right-hand (top) tracks.
     */
    public static ArrayList<MidiTrack> SplitTrack(MidiTrack track, int measurelen) {
        ArrayList<MidiNote> notes = track.getNotes();
        int count = notes.size();

        MidiTrack top = new MidiTrack(1);
        MidiTrack bottom = new MidiTrack(2);
        ArrayList<MidiTrack> result = new ArrayList<MidiTrack>(2);
        result.add(top); result.add(bottom);

        if (count == 0)
            return result;

        int prevhigh  = 76; /* E5, top of treble staff */
        int prevlow   = 45; /* A3, bottom of bass staff */
        int startindex = 0;

        for (MidiNote note : notes) {
            int high, low, highExact, lowExact;
            
            int number = note.getNumber();
            high = low = highExact = lowExact = number;

            while (notes.get(startindex).getEndTime() < note.getStartTime()) {
                startindex++;
            }

            /* I've tried several algorithms for splitting a track in two,
             * and the one below seems to work the best:
             * - If this note is more than an octave from the high/low notes
             *   (that start exactly at this start time), choose the closest one.
             * - If this note is more than an octave from the high/low notes
             *   (in this note's time duration), choose the closest one.
             * - If the high and low notes (that start exactly at this starttime)
             *   are more than an octave apart, choose the closest note.
             * - If the high and low notes (that overlap this starttime)
             *   are more than an octave apart, choose the closest note.
             * - Else, look at the previous high/low notes that were more than an 
             *   octave apart.  Choose the closeset note.
             */
            PairInt pair = new PairInt();
            pair.high = high; pair.low = low;
            PairInt pairExact = new PairInt();
            pairExact.high = highExact; pairExact.low = lowExact;

            FindHighLowNotes(notes, measurelen, startindex, note.getStartTime(), note.getEndTime(), pair);
            FindExactHighLowNotes(notes, startindex, note.getStartTime(), pairExact);

            high = pair.high; low = pair.low;
            highExact = pairExact.high; lowExact = pairExact.low;

            if (highExact - number > 12 || number - lowExact > 12) {
                if (highExact - number <= number - lowExact) {
                    top.AddNote(note);
                }
                else {
                    bottom.AddNote(note);
                }
            } 
            else if (high - number > 12 || number - low > 12) {
                if (high - number <= number - low) {
                    top.AddNote(note);
                }
                else {
                    bottom.AddNote(note);
                }
            } 
            else if (highExact - lowExact > 12) {
                if (highExact - number <= number - lowExact) {
                    top.AddNote(note);
                }
                else {
                    bottom.AddNote(note);
                }
            }
            else if (high - low > 12) {
                if (high - number <= number - low) {
                    top.AddNote(note);
                }
                else {
                    bottom.AddNote(note);
                }
            }
            else {
                if (prevhigh - number <= number - prevlow) {
                    top.AddNote(note);
                }
                else {
                    bottom.AddNote(note);
                }
            }

            /* The prevhigh/prevlow are set to the last high/low
             * that are more than an octave apart.
             */
            if (high - low > 12) {
                prevhigh = high;
                prevlow = low;
            }
        }

        Collections.sort(top.getNotes(), track.getNotes().get(0) );
        Collections.sort(bottom.getNotes(), track.getNotes().get(0) );

        return result;
    }


    /** Combine the notes in the given tracks into a single MidiTrack. 
     *  The individual tracks are already sorted.  To merge them, we
     *  use a mergesort-like algorithm.
     */
    public static MidiTrack CombineToSingleTrack(ArrayList<MidiTrack> tracks)
    {
        /* Add all notes into one track */
        MidiTrack result = new MidiTrack(1);

        if (tracks.size() == 0) {
            return result;
        }
        else if (tracks.size() == 1) {
            MidiTrack track = tracks.get(0);
            for (MidiNote note : track.getNotes()) {
                result.AddNote(note);
            }
            return result;
        }

        int[] noteindex = new int[tracks.size() + 1];
        int[] notecount = new int[tracks.size() + 1];

        for (int tracknum = 0; tracknum < tracks.size(); tracknum++) {
            noteindex[tracknum] = 0;
            notecount[tracknum] = tracks.get(tracknum).getNotes().size();
        }
        MidiNote prevnote = null;
        while (true) {
            MidiNote lowestnote = null;
            int lowestTrack = -1;
            for (int tracknum = 0; tracknum < tracks.size(); tracknum++) {
                MidiTrack track = tracks.get(tracknum);
                if (noteindex[tracknum] >= notecount[tracknum]) {
                    continue;
                }
                MidiNote note = track.getNotes().get( noteindex[tracknum] );
                if (lowestnote == null) {
                    lowestnote = note;
                    lowestTrack = tracknum;
                }
                else if (note.getStartTime() < lowestnote.getStartTime()) {
                    lowestnote = note;
                    lowestTrack = tracknum;
                }
                else if (note.getStartTime() == lowestnote.getStartTime() && note.getNumber() < lowestnote.getNumber()) {
                    lowestnote = note;
                    lowestTrack = tracknum;
                }
            }
            if (lowestnote == null) {
                /* We've finished the merge */
                break;
            }
            noteindex[lowestTrack]++;
            if ((prevnote != null) && (prevnote.getStartTime() == lowestnote.getStartTime()) &&
                (prevnote.getNumber() == lowestnote.getNumber()) ) {

                /* Don't add duplicate notes, with the same start time and number */        
                if (lowestnote.getDuration() > prevnote.getDuration()) {
                    prevnote.setDuration(lowestnote.getDuration());
                }
            }
            else {
                result.AddNote(lowestnote);
                prevnote = lowestnote;
            }
        }
    
        return result;
    }


    /** Combine the notes in all the tracks given into two MidiTracks,
     * and return them.
     * 
     * This function is intended for piano songs, when we want to display
     * a left-hand track and a right-hand track.  The lower notes go into 
     * the left-hand track, and the higher notes go into the right hand 
     * track.
     */
    public static ArrayList<MidiTrack> CombineToTwoTracks(ArrayList<MidiTrack> tracks, int measurelen)
    {
        MidiTrack single = CombineToSingleTrack(tracks);
        ArrayList<MidiTrack> result = SplitTrack(single, measurelen);

        ArrayList<MidiEvent> lyrics = new ArrayList<MidiEvent>();
        for (MidiTrack track : tracks) {
            if (track.getLyrics() != null) {
                lyrics.addAll(track.getLyrics());
            }
        }
        if (lyrics.size() > 0) {
            Collections.sort(lyrics, lyrics.get(0));
            result.get(0).setLyrics(lyrics);
        }
        return result;
    }


    /** Check that the MidiNote start times are in increasing order.
     * This is for debugging purposes.
     */
    private static void CheckStartTimes(ArrayList<MidiTrack> tracks) {
        for (MidiTrack track : tracks) {
            int prevtime = -1;
            for (MidiNote note : track.getNotes()) {
                if (note.getStartTime() < prevtime) {
                    throw new MidiFileException("Internal parsing error", 0);
                }
                prevtime = note.getStartTime();
            }
        }
    }


    /** In Midi Files, time is measured in pulses.  Notes that have
     * pulse times that are close together (like within 10 pulses)
     * will sound like they're the same chord.  We want to draw
     * these notes as a single chord, it makes the sheet music much
     * easier to read.  We don't want to draw notes that are close
     * together as two separate chords.
     *
     * The SymbolSpacing class only aligns notes that have exactly the same
     * start times.  Notes with slightly different start times will
     * appear in separate vertical columns.  This isn't what we want.
     * We want to align notes with approximately the same start times.
     * So, this function is used to assign the same starttime for notes
     * that are close together (timewise).
     */
    public static void
    RoundStartTimes(ArrayList<MidiTrack> tracks, int millisec, TimeSignature time) {
        /* Get all the starttimes in all tracks, in sorted order */
        ListInt starttimes = new ListInt();
        for (MidiTrack track : tracks) {
            for (MidiNote note : track.getNotes()) {
                starttimes.add(note.getStartTime());
            }
        }
        starttimes.sort();

        /* Notes within "millisec" milliseconds apart will be combined. */
        int interval = time.getQuarter() * millisec * 1000 / time.getTempo();

        /* If two starttimes are within interval millisec, make them the same */
        for (int i = 0; i < starttimes.size() - 1; i++) {
            if (starttimes.get(i+1) - starttimes.get(i) <= interval) {
                starttimes.set(i+1, starttimes.get(i));
            }
        }

        CheckStartTimes(tracks);

        /* Adjust the note starttimes, so that it matches one of the starttimes values */
        for (MidiTrack track : tracks) {
            int i = 0;

            for (MidiNote note : track.getNotes()) {
                while (i < starttimes.size() &&
                       note.getStartTime() - interval > starttimes.get(i)) {
                    i++;
                }

                if (note.getStartTime() > starttimes.get(i) &&
                    note.getStartTime() - starttimes.get(i) <= interval) {

                    note.setStartTime(starttimes.get(i));
                }
            }
            Collections.sort(track.getNotes(), track.getNotes().get(0));
        }
    }


    /** We want note durations to span up to the next note in general.
     * The sheet music looks nicer that way.  In contrast, sheet music
     * with lots of 16th/32nd notes separated by small rests doesn't
     * look as nice.  Having nice looking sheet music is more important
     * than faithfully representing the Midi File data.
     *
     * Therefore, this function rounds the duration of MidiNotes up to
     * the next note where possible.
     */
    public static void
    RoundDurations(ArrayList<MidiTrack> tracks, int quarternote) {

        for (MidiTrack track : tracks ) {
            MidiNote prevNote = null;
            for (int i = 0; i < track.getNotes().size() - 1; i++) {
                MidiNote note1 = track.getNotes().get(i);
                if (prevNote == null) {
                    prevNote = note1;
                }

                /* Get the next note that has a different start time */
                MidiNote note2 = note1;
                for (int j = i+1; j < track.getNotes().size(); j++) {
                    note2 = track.getNotes().get(j);
                    if (note1.getStartTime() < note2.getStartTime()) {
                        break;
                    }
                }
                int maxduration = note2.getStartTime() - note1.getStartTime();

                int dur = 0;
                if (quarternote <= maxduration)
                    dur = quarternote;
                else if (quarternote/2 <= maxduration)
                    dur = quarternote/2;
                else if (quarternote/3 <= maxduration)
                    dur = quarternote/3;
                else if (quarternote/4 <= maxduration)
                    dur = quarternote/4;


                if (dur < note1.getDuration()) {
                    dur = note1.getDuration();
                }

                /* Special case: If the previous note's duration
                 * matches this note's duration, we can make a notepair.
                 * So don't expand the duration in that case.
                 */
                if ((prevNote.getStartTime() + prevNote.getDuration() == note1.getStartTime()) &&
                    (prevNote.getDuration() == note1.getDuration())) {


                    dur = note1.getDuration();
                }
                note1.setDuration(dur);
                if (track.getNotes().get(i+1).getStartTime() != note1.getStartTime()) {
                    prevNote = note1;
                }
            }
        }
    }

    /** Split the given track into multiple tracks, separating each
     * channel into a separate track.
     */
    private static ArrayList<MidiTrack> 
    SplitChannels(MidiTrack origtrack, ArrayList<MidiEvent> events) {

        /* Find the instrument used for each channel */
        int[] channelInstruments = new int[16];
        for (MidiEvent mevent : events) {
            if (mevent.EventFlag == EventProgramChange) {
                channelInstruments[mevent.Channel] = mevent.Instrument;
            }
        }
        channelInstruments[9] = 128; /* Channel 9 = Percussion */

        ArrayList<MidiTrack> result = new ArrayList<MidiTrack>();
        for (MidiNote note : origtrack.getNotes()) {
            boolean foundchannel = false;
            for (MidiTrack track : result) {
                if (note.getChannel() == track.getNotes().get(0).getChannel()) {
                    foundchannel = true;
                    track.AddNote(note); 
                }
            }
            if (!foundchannel) {
                MidiTrack track = new MidiTrack(result.size() + 1);
                track.AddNote(note);
                track.setInstrument(channelInstruments[note.getChannel()]);
                result.add(track);
            }
        }
        ArrayList<MidiEvent> lyrics = origtrack.getLyrics(); 
        if (lyrics != null) {
            for (MidiEvent lyricEvent : lyrics) {
                for (MidiTrack track : result) {
                    if (lyricEvent.Channel == track.getNotes().get(0).getChannel() ) {
                        track.AddLyric(lyricEvent);
                    }
                }
            }
        }
        return result;
    }


    /** Guess the measure length.  We assume that the measure
     * length must be between 0.5 seconds and 4 seconds.
     * Take all the note start times that fall between 0.5 and 
     * 4 seconds, and return the starttimes.
     */
    public ListInt
    GuessMeasureLength() {
        ListInt result = new ListInt();

        int pulses_per_second = (int) (1000000.0 / timesig.getTempo() * timesig.getQuarter());
        int minmeasure = pulses_per_second / 2;  /* The minimum measure length in pulses */
        int maxmeasure = pulses_per_second * 4;  /* The maximum measure length in pulses */

        /* Get the start time of the first note in the midi file. */
        int firstnote = timesig.getMeasure() * 5;
        for (MidiTrack track : tracks) {
            if (firstnote > track.getNotes().get(0).getStartTime()) {
                firstnote = track.getNotes().get(0).getStartTime();
            }
        }

        /* interval = 0.06 seconds, converted into pulses */
        int interval = timesig.getQuarter() * 60000 / timesig.getTempo();

        for (MidiTrack track : tracks) {
            int prevtime = 0;
            for (MidiNote note : track.getNotes()) {
                if (note.getStartTime() - prevtime <= interval)
                    continue;

                prevtime = note.getStartTime();

                int time_from_firstnote = note.getStartTime() - firstnote;

                /* Round the time down to a multiple of 4 */
                time_from_firstnote = time_from_firstnote / 4 * 4;
                if (time_from_firstnote < minmeasure)
                    continue;
                if (time_from_firstnote > maxmeasure)
                    break;

                if (!result.contains(time_from_firstnote)) {
                    result.add(time_from_firstnote);
                }
            }
        }
        result.sort();
        return result;
    }

    /** Return the last start time */
    public int EndTime() {
        int lastStart = 0;
        for (MidiTrack track : tracks) {
            if (track.getNotes().size() == 0) {
                continue;
            }
            int last = track.getNotes().get(track.getNotes().size()-1).getStartTime();
            lastStart = Math.max(last, lastStart);
        }
        return lastStart;
    }


    /** Return true if this midi file has lyrics */
    public boolean hasLyrics() {
        for (MidiTrack track : tracks) {
            if (track.getLyrics() != null) {
                return true;
            }
        }
        return false;
    }

    /** Return true if the data starts with the header MTrk */
    public static boolean hasMidiHeader(byte[] data) {
        String s;
        s = new String(data, 0, 4, StandardCharsets.US_ASCII);
        return s.equals("MThd");
    }


    @Override
    public String toString() {
        StringBuilder result = new StringBuilder(
                "Midi File tracks=" + tracks.size() + " quarter=" + quarternote + "\n");
        result.append(timesig.toString()).append("\n");
        for(MidiTrack track : tracks) {
            result.append(track.toString());
        }
        return result.toString();
    }

    /* Command-line program to print out a parsed Midi file. Used for debugging.
     * To run:
     * - Change main2 to main
     * - javac MidiFile.java
     * - java MidiFile file.mid
     *
     */
    public static void main2(String[] args) {
        /*
        if (args.length == 0) {
            System.out.println("Usage: MidiFile <filename>");
            return;
        }
        String filename = args[0];
        byte[] data;
        try {
            File info = new File(filename);
            FileInputStream file = new FileInputStream(filename);

            data = new byte[ (int)info.length() ];
            int offset = 0;
            int len = (int)info.length();
            while (true) {
                if (offset == len)
                    break;
                int n = file.read(data, offset, len- offset);
                if (n <= 0)
                    break;
                offset += n;
            }
            file.close();
        }
        catch(IOException e) {
            return;
        }

        MidiFile f = new MidiFile(data, "");
        System.out.print(f.toString());
        */
    }

}  /* End class MidiFile */



