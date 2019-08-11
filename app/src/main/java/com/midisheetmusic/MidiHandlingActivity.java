package com.midisheetmusic;

import android.graphics.Color;
import android.hardware.usb.UsbDevice;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;

import jp.kshoji.driver.midi.activity.AbstractSingleMidiActivity;
import jp.kshoji.driver.midi.device.MidiInputDevice;
import jp.kshoji.driver.midi.device.MidiOutputDevice;

public abstract class MidiHandlingActivity extends AbstractSingleMidiActivity {
    void log(String s) {
        this.runOnUiThread(() ->
                Toast.makeText(MidiHandlingActivity.this, s, Toast.LENGTH_SHORT).show()
        );
    }

    abstract void OnMidiDeviceStatus(boolean connected);
    abstract void OnMidiNote(int note, boolean pressed);


    @Override
    public void onDeviceAttached(@NonNull UsbDevice usbDevice) {
        //deprecated
    }

    @Override
    public void onMidiInputDeviceAttached(@NonNull MidiInputDevice midiInputDevice) {
        OnMidiDeviceStatus(true);
        ((Button)this.findViewById(R.id.btn_midi)).setTextColor(Color.BLUE);
        log("MIDI Input device connected: " + midiInputDevice.getManufacturerName() + " - " + midiInputDevice.getProductName());
    }

    @Override
    public void onMidiOutputDeviceAttached(@NonNull MidiOutputDevice midiOutputDevice) {
    }

    @Override
    public void onDeviceDetached(@NonNull UsbDevice usbDevice) {
        //deprecated
    }

    @Override
    public void onMidiInputDeviceDetached(@NonNull MidiInputDevice midiInputDevice) {
        OnMidiDeviceStatus(false);
        log("MIDI Input device disconnected");
    }

    @Override
    public void onMidiOutputDeviceDetached(@NonNull MidiOutputDevice midiOutputDevice) {

    }

    @Override
    public void onMidiMiscellaneousFunctionCodes(@NonNull MidiInputDevice midiInputDevice, int i, int i1, int i2, int i3) {

    }

    @Override
    public void onMidiCableEvents(@NonNull MidiInputDevice midiInputDevice, int i, int i1, int i2, int i3) {

    }

    @Override
    public void onMidiSystemCommonMessage(@NonNull MidiInputDevice midiInputDevice, int i, byte[] bytes) {

    }

    @Override
    public void onMidiSystemExclusive(@NonNull MidiInputDevice midiInputDevice, int i, byte[] bytes) {

    }

    @Override
    public void onMidiNoteOff(@NonNull MidiInputDevice midiInputDevice, int i, int i1, int note, int velocity) {
        //OnMidiNote(note, false);
    }

    @Override
    public void onMidiNoteOn(@NonNull MidiInputDevice midiInputDevice, int i, int i1, int note, int velocity) {
        OnMidiNote(note, true);
    }

    @Override
    public void onMidiPolyphonicAftertouch(@NonNull MidiInputDevice midiInputDevice, int i, int i1, int i2, int i3) {

    }

    @Override
    public void onMidiControlChange(@NonNull MidiInputDevice midiInputDevice, int i, int i1, int i2, int i3) {

    }

    @Override
    public void onMidiProgramChange(@NonNull MidiInputDevice midiInputDevice, int i, int i1, int i2) {

    }

    @Override
    public void onMidiChannelAftertouch(@NonNull MidiInputDevice midiInputDevice, int i, int i1, int i2) {

    }

    @Override
    public void onMidiPitchWheel(@NonNull MidiInputDevice midiInputDevice, int i, int i1, int i2) {

    }

    @Override
    public void onMidiSingleByte(@NonNull MidiInputDevice midiInputDevice, int i, int i1) {

    }

    @Override
    public void onMidiTimeCodeQuarterFrame(@NonNull MidiInputDevice midiInputDevice, int i, int i1) {

    }

    @Override
    public void onMidiSongSelect(@NonNull MidiInputDevice midiInputDevice, int i, int i1) {

    }

    @Override
    public void onMidiSongPositionPointer(@NonNull MidiInputDevice midiInputDevice, int i, int i1) {

    }

    @Override
    public void onMidiTuneRequest(@NonNull MidiInputDevice midiInputDevice, int i) {

    }

    @Override
    public void onMidiTimingClock(@NonNull MidiInputDevice midiInputDevice, int i) {

    }

    @Override
    public void onMidiStart(@NonNull MidiInputDevice midiInputDevice, int i) {

    }

    @Override
    public void onMidiContinue(@NonNull MidiInputDevice midiInputDevice, int i) {

    }

    @Override
    public void onMidiStop(@NonNull MidiInputDevice midiInputDevice, int i) {

    }

    @Override
    public void onMidiActiveSensing(@NonNull MidiInputDevice midiInputDevice, int i) {

    }

    @Override
    public void onMidiReset(@NonNull MidiInputDevice midiInputDevice, int i) {

    }
}
