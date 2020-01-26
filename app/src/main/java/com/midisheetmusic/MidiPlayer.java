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

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.midisheetmusic.sheets.ChordSymbol;
import com.midisheetmusic.sheets.MusicSymbol;
import com.mikepenz.materialdrawer.Drawer;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Locale;

/**
 * The MidiPlayer is the panel at the top used to play the sound
 * of the midi file. It consists of:
 * <ul>
 *  <li/> The Rewind button
 *  <li/> The Play/Pause button
 *  <li/> The Stop button
 *  <li/> The Fast Forward button
 *  <li/> The Playback speed bar
 * </ul>
 *
 * The sound of the midi file depends on
 * <ul>
 *  <li/> The MidiOptions (taken from the menus)
 *  <li/> Which tracks are selected
 *  <li/> How much to transpose the keys by
 *  <li/> What instruments to use per track
 *  <li/> The tempo (from the Speed bar)
 *  <li/> The volume
 * </ul>
 *
 * The MidiFile.ChangeSound() method is used to create a new midi file
 * with these options.  The mciSendString() function is used for 
 * playing, pausing, and stopping the sound.
 * <br/><br/>
 * For shading the notes during playback, the method
 * `SheetMusic.ShadeNotes()` is used.  It takes the current 'pulse time',
 * and determines which notes to shade.
 */
public class MidiPlayer extends LinearLayout {
    private Button midiButton;
    private Button leftHandButton;
    private Button rightHandButton;
    private ImageButton pianoButton;
    /** The "Speed %" label */
    private TextView speedText;
    /** The seekbar for controlling playback speed */
    private SeekBar speedBar;
    private Drawer drawer;

    /** The index corresponding to left/right hand in the track list */
    private static final int LEFT_TRACK = 1;
    private static final int RIGHT_TRACK = 0;

    int playstate;               /** The playing state of the Midi Player */
    final int stopped   = 1;     /** Currently stopped */
    final int playing   = 2;     /** Currently playing music */
    final int paused    = 3;     /** Currently paused */
    final int initStop  = 4;     /** Transitioning from playing to stop */
    final int initPause = 5;     /** Transitioning from playing to pause */
    final int midi      = 6;

    final String tempSoundFile = "playing.mid"; /** The filename to play sound from */

    /** For playing the audio */
    MediaPlayer player;
    /** The midi file to play */
    MidiFile midifile;
    /** The sound options for playing the midi file */
    MidiOptions options;
    /** The sheet music to shade while playing */
    SheetMusic sheet;
    /** The piano to shade while playing */
    Piano piano;
    /** Timer used to update the sheet music while playing */
    Handler timer;
    /** Absolute time when music started playing (msec) */
    long startTime;
    /** The number of pulses per millisec */
    double pulsesPerMsec;
    /** Time (in pulses) when music started playing */
    double startPulseTime;
    /** Time (in pulses) music is currently at */
    double currentPulseTime;
    /** Time (in pulses) music was last at */
    double prevPulseTime;
    /** The parent activity. */
    Activity activity;

    /** A listener that allows us to send a request to update the sheet when needed */
    private SheetUpdateRequestListener mSheetUpdateRequestListener;


    public void setSheetUpdateRequestListener(SheetUpdateRequestListener listener) {
        mSheetUpdateRequestListener = listener;
    }

    /** Create a new MidiPlayer, displaying the play/stop buttons, and the
     *  speed bar.  The midifile and sheetmusic are initially null.
     */
    public MidiPlayer(Activity activity) {
        super(activity);
        this.activity = activity;
        this.midifile = null;
        this.options = null;
        this.sheet = null;
        playstate = stopped;
        startTime = SystemClock.uptimeMillis();
        startPulseTime = 0;
        currentPulseTime = 0;
        prevPulseTime = -10;
        init();

        player = new MediaPlayer();
        setBackgroundColor(Color.BLACK);
    }

    public MidiPlayer(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public MidiPlayer(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    void OnMidiDeviceStatus(boolean connected) {
        midiButton.setEnabled(connected);
        midiButton.setTextColor(connected ? Color.BLUE : Color.RED);
    }

    int prevWrongMidi = 0;

    void OnMidiNote(int note, boolean pressed) {
        if (!pressed) return;
        MusicSymbol nextNote = this.sheet.getCurrentNote((int) currentPulseTime);
        int midiNote = ((ChordSymbol) nextNote).getNotedata()[0].number;
        note += options.midiShift;
        if (midiNote != note) {
            piano.UnShadeOneNote(prevWrongMidi);
            piano.ShadeOneNote(note, Color.RED);
            prevWrongMidi = note;
        } else {
            prevPulseTime = currentPulseTime;
            currentPulseTime = sheet.getCurrentNote(nextNote.getStartTime() + 1).getStartTime();
            piano.UnShadeOneNote(prevWrongMidi);
        }
        sheet.ShadeNotes((int) currentPulseTime, (int) prevPulseTime, SheetMusic.ImmediateScroll);
        piano.ShadeNotes((int) currentPulseTime, (int) prevPulseTime);
    }

    /** Create the rewind, play, stop, and fast forward buttons */
    void init() {
        inflate(activity, R.layout.player_toolbar, this);

        ImageButton backButton = findViewById(R.id.btn_back);
        ImageButton rewindButton = findViewById(R.id.btn_rewind);
        ImageButton resetButton = findViewById(R.id.btn_replay);
        ImageButton playButton = findViewById(R.id.btn_play);
        ImageButton fastFwdButton = findViewById(R.id.btn_forward);
        ImageButton settingsButton = findViewById(R.id.btn_settings);
        leftHandButton = findViewById(R.id.btn_left);
        rightHandButton = findViewById(R.id.btn_right);
        midiButton = findViewById(R.id.btn_midi);
        pianoButton = findViewById(R.id.btn_piano);
        speedText = findViewById(R.id.txt_speed);
        speedBar = findViewById(R.id.speed_bar);

        backButton.setOnClickListener(v -> activity.onBackPressed());
        rewindButton.setOnClickListener(v -> Rewind());
        resetButton.setOnClickListener(v -> Reset());
        playButton.setOnClickListener(v -> Play());
        fastFwdButton.setOnClickListener(v -> FastForward());
        settingsButton.setOnClickListener(v -> {
            drawer.deselect();
            drawer.openDrawer();
        });
        midiButton.setOnClickListener(v -> toggleMidi());
        leftHandButton.setOnClickListener(v -> toggleTrack(LEFT_TRACK));
        rightHandButton.setOnClickListener(v -> toggleTrack(RIGHT_TRACK));
        pianoButton.setOnClickListener(v -> togglePiano());

        // Resize the speedBar so all toolbar icons fit on the screen
        speedBar.post(
                () -> {
                    int iconsWidth = backButton.getWidth() + resetButton.getWidth() + playButton.getWidth() +
                            rewindButton.getWidth() + fastFwdButton.getWidth() + midiButton.getWidth() +
                            leftHandButton.getWidth() + rightHandButton.getWidth() + pianoButton.getWidth() +
                            settingsButton.getWidth();
                    int screenwidth = activity.getWindowManager().getDefaultDisplay().getWidth();
                    speedBar.setLayoutParams(
                            new LayoutParams(screenwidth - iconsWidth - 16, speedBar.getHeight()));
                }
        );

        speedBar.getProgressDrawable().setColorFilter(Color.parseColor("#00BB87"), PorterDuff.Mode.SRC_IN);
        speedBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
                // If speed bar is between 97 and 103 approximate it to 100
                if(97 < progress && progress < 103) {
                    progress = 100;
                    bar.setProgress(progress);
                }
                speedText.setText(String.format(Locale.US, "%3d", progress) + "%");
            }
            public void onStartTrackingTouch(SeekBar bar) {
            }
            public void onStopTrackingTouch(SeekBar bar) {
            }
        });

        /* Initialize the timer used for playback, but don't start
         * the timer yet (enabled = false).
         */
        timer = new Handler();
    }

    private void toggleMidi() {
        sheet.ShadeNotes(-10, (int)prevPulseTime, SheetMusic.DontScroll);
        sheet.ShadeNotes(-10, (int)currentPulseTime, SheetMusic.DontScroll);
        piano.ShadeNotes(-10, (int)prevPulseTime);
        piano.ShadeNotes(-10, (int)currentPulseTime);
        piano.UnShadeOneNote(prevWrongMidi);
        if (playstate != midi) {
            playstate = midi;
            currentPulseTime = 0;
            prevPulseTime = 0;
        } else {
            playstate = paused;
        }
        this.setVisibility(View.GONE);
        timer.removeCallbacks(TimerCallback);
        timer.postDelayed(ReShade, 500);
    }


    /** Show/hide treble and bass clefs */
    private void toggleTrack(int track) {
        if (track < options.tracks.length) {
            options.tracks[track] = !options.tracks[track];
            options.mute[track] = !options.tracks[track];
            if (mSheetUpdateRequestListener != null) {
                mSheetUpdateRequestListener.onSheetUpdateRequest();
            }
            updateToolbarButtons();
        }
    }

    /** Show/hide the piano */
    private void togglePiano() {
        options.showPiano = !options.showPiano;
        piano.setVisibility(options.showPiano ? View.VISIBLE : View.GONE);
        updateToolbarButtons();
    }

    /** Update the status of the toolbar buttons (show, hide, opacity, etc.) */
    public void updateToolbarButtons(){
        pianoButton.setAlpha(options.showPiano ? (float) 1.0 : (float) 0.5);

        float leftAlpha = (float) 0.5;
        float rightAlpha = (float) 0.5;
        if (LEFT_TRACK < options.tracks.length) {
            leftAlpha = options.tracks[LEFT_TRACK] ? (float) 1.0 : (float) 0.5;
        }
        if (RIGHT_TRACK < options.tracks.length) {
            rightAlpha = options.tracks[RIGHT_TRACK] ? (float) 1.0 : (float) 0.5;
        }
        leftHandButton.setVisibility(LEFT_TRACK < options.tracks.length ? View.VISIBLE : View.GONE);
        rightHandButton.setVisibility(RIGHT_TRACK < options.tracks.length ? View.VISIBLE : View.GONE);
        leftHandButton.setAlpha(leftAlpha);
        rightHandButton.setAlpha(rightAlpha);
    }

    /** Get the preferred width/height given the screen width/height */
    public static Point getPreferredSize(int screenwidth, int screenheight) {
        int height = (int) (5.0 * screenwidth / ( 2 + Piano.KeysPerOctave * Piano.MaxOctave));
        height = height * 2/3 ;
        return new Point(screenwidth, height);
    }

    /** Determine the measured width and height.
     *  Resize the individual buttons according to the new width/height.
     */
    @Override
    protected void onMeasure(int widthspec, int heightspec) {
        super.onMeasure(widthspec, heightspec);
        int screenwidth = MeasureSpec.getSize(widthspec);
        /* Make the button height 2/3 the piano WhiteKeyHeight */
        int height = (int) (5.0 * screenwidth / ( 2 + Piano.KeysPerOctave * Piano.MaxOctave));
        height = height * 2/3;
        setMeasuredDimension(screenwidth, height);
    }

    public void SetPiano(Piano p) {
        piano = p;
    }

    /** The MidiFile and/or SheetMusic has changed. Stop any playback sound,
     *  and store the current midifile and sheet music.
     */
    public void SetMidiFile(MidiFile file, MidiOptions opt, SheetMusic s) {

        /* If we're paused, and using the same midi file, redraw the
         * highlighted notes.
         */
        if ((file == midifile && midifile != null && playstate == paused)) {
            options = opt;
            sheet = s;
            sheet.ShadeNotes((int)currentPulseTime, (int)-1, SheetMusic.DontScroll);

            /* We have to wait some time (200 msec) for the sheet music
             * to scroll and redraw, before we can re-shade.
             */
            timer.removeCallbacks(TimerCallback);
            timer.postDelayed(ReShade, 500);
        }
        else {
            Reset();
            midifile = file;
            options = opt;
            sheet = s;
            ScrollToStart();
        }
    }

    /** If we're paused, reshade the sheet music and piano. */
    Runnable ReShade = new Runnable() {
      public void run() {
        if (playstate == paused || playstate == stopped) {
            sheet.ShadeNotes((int)currentPulseTime, (int)-10, SheetMusic.ImmediateScroll);
            piano.ShadeNotes((int)currentPulseTime, (int)prevPulseTime);
        }
      }
    };


    /** Return the number of tracks selected in the MidiOptions.
     *  If the number of tracks is 0, there is no sound to play.
     */
    private int numberTracks() {
        int count = 0;
        for (int i = 0; i < options.tracks.length; i++) {
            if (options.tracks[i] && !options.mute[i]) {
                count += 1;
            }
        }
        return count;
    }

    /** Create a new midi file with all the MidiOptions incorporated.
     *  Save the new file to playing.mid, and store
     *  this temporary filename in tempSoundFile.
     */ 
    private void CreateMidiFile() {
        double inverse_tempo = 1.0 / midifile.getTime().getTempo();
        double inverse_tempo_scaled = inverse_tempo * speedBar.getProgress() / 100.0;
        // double inverse_tempo_scaled = inverse_tempo * 100.0 / 100.0;
        options.tempo = (int)(1.0 / inverse_tempo_scaled);
        pulsesPerMsec = midifile.getTime().getQuarter() * (1000.0 / options.tempo);

        try {
            FileOutputStream dest = activity.openFileOutput(tempSoundFile, Context.MODE_PRIVATE);
            midifile.ChangeSound(dest, options);
            dest.close();
            // checkFile(tempSoundFile);
        }
        catch (IOException e) {
            Toast toast = Toast.makeText(activity, "Error: Unable to create MIDI file for playing.", Toast.LENGTH_LONG);
            toast.show();
        }
    }

    private void checkFile(String name) {
        try {
            FileInputStream in = activity.openFileInput(name);
            byte[] data = new byte[4096];
            int total = 0, len = 0;
            while (true) {
                len = in.read(data, 0, 4096);
                if (len > 0)
                    total += len;
                else
                    break;
            } 
            in.close();
            data = new byte[total];
            in = activity.openFileInput(name);
            int offset = 0;
            while (offset < total) {
                len = in.read(data, offset, total - offset);
                if (len > 0)
                    offset += len;
            }
            in.close();
        }
        catch (IOException e) {
            Toast toast = Toast.makeText(activity, "CheckFile: " + e.toString(), Toast.LENGTH_LONG);
            toast.show();
        }
        catch (MidiFileException e) {
            Toast toast = Toast.makeText(activity, "CheckFile midi: " + e.toString(), Toast.LENGTH_LONG);
            toast.show();
        } 
    }


    /** Play the sound for the given MIDI file */
    private void PlaySound(String filename) {
        if (player == null)
            return;
        try {
            FileInputStream input = activity.openFileInput(filename);
            player.reset();
            player.setDataSource(input.getFD());
            input.close();
            player.prepare();
            player.start();
        }
        catch (IOException e) {
            Toast toast = Toast.makeText(activity, "Error: Unable to play MIDI sound", Toast.LENGTH_LONG);
            toast.show();
        }
    }

    /** Stop playing the MIDI music */
    private void StopSound() {
        if (player == null)
            return;
        player.stop();
        player.reset();
    }


    /** The callback for the play button.
     *  If we're stopped or pause, then play the midi file.
     */
    private void Play() {
        if (midifile == null || sheet == null || numberTracks() == 0) {
            return;
        }
        else if (playstate == initStop || playstate == initPause || playstate == playing) {
            return;
        }
        // playstate is stopped or paused

        // Hide the midi player, wait a little for the view
        // to refresh, and then start playing
        this.setVisibility(View.GONE);
        RemoveShading();
        timer.removeCallbacks(TimerCallback);
        timer.postDelayed(DoPlay, 1000);
    }

    Runnable DoPlay = new Runnable() {
      public void run() {
        /* The startPulseTime is the pulse time of the midi file when
         * we first start playing the music.  It's used during shading.
         */
        if (options.playMeasuresInLoop) {
            /* If we're playing measures in a loop, make sure the
             * currentPulseTime is somewhere inside the loop measures.
             */
            int measure = (int)(currentPulseTime / midifile.getTime().getMeasure());
            if ((measure < options.playMeasuresInLoopStart) ||
                (measure > options.playMeasuresInLoopEnd)) {
                currentPulseTime = options.playMeasuresInLoopStart * midifile.getTime().getMeasure();
            }
            startPulseTime = currentPulseTime;
            options.pauseTime = (int)(currentPulseTime - options.shifttime);
        }
        else if (playstate == paused) {
            startPulseTime = currentPulseTime;
            options.pauseTime = (int)(currentPulseTime - options.shifttime);
        }
        else {
            options.pauseTime = 0;
            startPulseTime = options.shifttime;
            currentPulseTime = options.shifttime;
            prevPulseTime = options.shifttime - midifile.getTime().getQuarter();
        }

        CreateMidiFile();
        playstate = playing;
        PlaySound(tempSoundFile);
        startTime = SystemClock.uptimeMillis();

        timer.removeCallbacks(TimerCallback);
        timer.removeCallbacks(ReShade);
        timer.postDelayed(TimerCallback, 100);

        sheet.ShadeNotes((int)currentPulseTime, (int)prevPulseTime, SheetMusic.GradualScroll);
        piano.ShadeNotes((int)currentPulseTime, (int)prevPulseTime);
      }
    };


    /** The callback for pausing playback.
     *  If we're currently playing, pause the music.
     *  The actual pause is done when the timer is invoked.
     */
    public void Pause() {
        this.setVisibility(View.VISIBLE);
        LinearLayout layout = (LinearLayout)this.getParent();
        layout.requestLayout();
        this.requestLayout();
        this.invalidate();

        if (midifile == null || sheet == null || numberTracks() == 0) {
            return;
        }

        // Cancel pending play events
        timer.removeCallbacks(DoPlay);

        if (playstate == playing) {
            playstate = initPause;
        }
        else if (playstate == midi) {
            playstate = paused;
        }
    }


    /** The callback for the Reset button.
     *  If playing, initiate a stop and wait for the timer to finish.
     *  Then do the actual stop.
     */
    void Reset() {
        if (midifile == null || sheet == null) {
            return;
        }

        if (playstate == stopped) {
            RemoveShading();
            ScrollToStart();
        }
        else if (playstate == initPause || playstate == initStop || playstate == playing) {
            /* Wait for timer to finish */
            playstate = initStop;
            DoStop();
        }
        else if (playstate == paused) {
            DoStop();
        }
    }

    /** Perform the actual stop, by stopping the sound,
     *  removing any shading, and clearing the state.
     */
    void DoStop() { 
        playstate = stopped;
        timer.removeCallbacks(TimerCallback);
        RemoveShading();
        ScrollToStart();
        setVisibility(View.VISIBLE);
        StopSound();
    }

    void RemoveShading() {
        sheet.ShadeNotes(-10, (int)prevPulseTime, SheetMusic.DontScroll);
        sheet.ShadeNotes(-10, (int)currentPulseTime, SheetMusic.DontScroll);
        piano.ShadeNotes(-10, (int)prevPulseTime);
        piano.ShadeNotes(-10, (int)currentPulseTime);
    }

    /**
     * Scroll to the beginning of the sheet or to options.playMeasuresInLoopStart if enabled
     */
    void ScrollToStart() {
        startPulseTime = options.playMeasuresInLoop ?
                options.playMeasuresInLoopStart * midifile.getTime().getMeasure(): 0;
        currentPulseTime = startPulseTime;
        prevPulseTime = -10;
        sheet.ShadeNotes((int)currentPulseTime, (int)prevPulseTime, SheetMusic.ImmediateScroll);
        piano.ShadeNotes((int)currentPulseTime, (int)prevPulseTime);
    }

    /** Rewind the midi music back one measure.
     *  The music must be in the paused/stopped state.
     *  When we resume in playPause, we start at the currentPulseTime.
     *  So to rewind, just decrease the currentPulseTime,
     *  and re-shade the sheet music.
     */
    void Rewind() {
        if (midifile == null || sheet == null) {
            return;
        }
        if (playstate != paused && playstate != stopped) {
            return;
        }

        /* Remove any highlighted notes */
        sheet.ShadeNotes(-10, (int)currentPulseTime, SheetMusic.DontScroll);
        piano.ShadeNotes(-10, (int)currentPulseTime);

        prevPulseTime = currentPulseTime; 
        currentPulseTime -= midifile.getTime().getMeasure();
        if (currentPulseTime < 0) {
            currentPulseTime = 0;
            prevPulseTime = -10;
        }
        else if (currentPulseTime < options.shifttime) {
            currentPulseTime = options.shifttime;
        }
        sheet.ShadeNotes((int)currentPulseTime, (int)prevPulseTime, SheetMusic.ImmediateScroll);
        piano.ShadeNotes((int)currentPulseTime, (int)prevPulseTime);
    }
    
    /** Fast forward the midi music by one measure.
     *  The music must be in the paused/stopped state.
     *  When we resume in playPause, we start at the currentPulseTime.
     *  So to fast forward, just increase the currentPulseTime,
     *  and re-shade the sheet music.
     */
    void FastForward() {
        if (midifile == null || sheet == null) {
            return;
        }
        if (playstate != paused && playstate != stopped) {
            return;
        }
        playstate = paused;

        /* Remove any highlighted notes */
        sheet.ShadeNotes(-10, (int)currentPulseTime, SheetMusic.DontScroll);
        piano.ShadeNotes(-10, (int)currentPulseTime);
   
        prevPulseTime = currentPulseTime; 
        currentPulseTime += midifile.getTime().getMeasure();
        if (currentPulseTime > midifile.getTotalPulses()) {
            currentPulseTime -= midifile.getTime().getMeasure();
        }
        sheet.ShadeNotes((int)currentPulseTime, (int)prevPulseTime, SheetMusic.ImmediateScroll);
        piano.ShadeNotes((int)currentPulseTime, (int)prevPulseTime);
    }


    /** Move the current position to the location clicked.
     *  The music must be in the paused/stopped state.
     *  When we resume in playPause, we start at the currentPulseTime.
     *  So, set the currentPulseTime to the position clicked.
     */
    public void MoveToClicked(int x, int y) {
        if (midifile == null || sheet == null) {
            return;
        }
        if (playstate != paused && playstate != stopped && playstate != midi) {
            return;
        }
        if (playstate != midi) {
            playstate = paused;
        }

        /* Remove any highlighted notes */
        sheet.ShadeNotes(-10, (int)currentPulseTime, SheetMusic.DontScroll);
        piano.ShadeNotes(-10, (int)currentPulseTime);

        currentPulseTime = sheet.PulseTimeForPoint(new Point(x, y));
        prevPulseTime = currentPulseTime - midifile.getTime().getMeasure();
        if (currentPulseTime > midifile.getTotalPulses()) {
            currentPulseTime -= midifile.getTime().getMeasure();
        }
        sheet.ShadeNotes((int)currentPulseTime, (int)prevPulseTime, SheetMusic.DontScroll);
        piano.ShadeNotes((int)currentPulseTime, (int)prevPulseTime);
    }


    /** The callback for the timer. If the midi is still playing, 
     *  update the currentPulseTime and shade the sheet music.
     *  If a stop or pause has been initiated (by someone clicking
     *  the stop or pause button), then stop the timer.
     */
    Runnable TimerCallback = new Runnable() {
      public void run() {
        if (midifile == null || sheet == null) {
            playstate = stopped;
        }
        else if (playstate == stopped || playstate == paused) {
            /* This case should never happen */
            return;
        }
        else if (playstate == initStop) {
            return;
        }
        else if (playstate == playing) {
            long msec = SystemClock.uptimeMillis() - startTime;
            prevPulseTime = currentPulseTime;
            currentPulseTime = startPulseTime + msec * pulsesPerMsec;

            /* If we're playing in a loop, stop and restart */
            if (options.playMeasuresInLoop) {
                double nearEndTime = currentPulseTime + pulsesPerMsec*10;
                int measure = (int)(nearEndTime / midifile.getTime().getMeasure());
                if (measure > options.playMeasuresInLoopEnd) {
                    RestartPlayMeasuresInLoop();
                    return;
                }
            }

            /* Stop if we've reached the end of the song */
            if (currentPulseTime > midifile.getTotalPulses()) {
                DoStop();
                return;
            }
            sheet.ShadeNotes((int)currentPulseTime, (int)prevPulseTime, SheetMusic.GradualScroll);
            piano.ShadeNotes((int)currentPulseTime, (int)prevPulseTime);
            timer.postDelayed(TimerCallback, 100);
        }
        else if (playstate == initPause) {
            long msec = SystemClock.uptimeMillis() - startTime;
            StopSound();

            prevPulseTime = currentPulseTime;
            currentPulseTime = startPulseTime + msec * pulsesPerMsec;
            sheet.ShadeNotes((int)currentPulseTime, (int)prevPulseTime, SheetMusic.ImmediateScroll);
            piano.ShadeNotes((int)currentPulseTime, (int)prevPulseTime);
            playstate = paused;
            timer.postDelayed(ReShade, 1000);
        }
      }
    };


    /** The "Play Measures in a Loop" feature is enabled, and we've reached
     *  the last measure. Stop the sound, unshade the music, and then
     *  start playing again.
     */
    private void RestartPlayMeasuresInLoop() {
        playstate = stopped;
        piano.ShadeNotes(-10, (int)prevPulseTime);
        sheet.ShadeNotes(-10, (int)prevPulseTime, SheetMusic.DontScroll);
        currentPulseTime = 0;
        prevPulseTime = -1;
        StopSound();
        timer.postDelayed(DoPlay, 300);
    }

    public boolean isInMidiMode() {
        return playstate == midi;
    }

    public void setDrawer(Drawer drawer) {
        this.drawer = drawer;
    }
}


