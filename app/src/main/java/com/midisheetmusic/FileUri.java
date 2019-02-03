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

import android.content.*;
import android.content.res.*;
import java.io.*;
import java.util.*;
import android.net.*;
import android.provider.*;
import org.json.*;
import android.app.Activity;

/** @class FileUri
 * Represents a reference to a file.
 * The file could be either in the /assets directory, 
 * the internal storage, or the external storage.
 */
public class FileUri implements Comparator<FileUri> {
    private Uri uri;                  /** The URI path to the file */
    private String displayName;       /** The name to display */

    /** Create a Uri with the given display name */
    public FileUri(Uri uri, String path) {
        this.uri = uri;
        if (path == null) {
            path = uri.getLastPathSegment();
        }
        displayName = displayNameFromPath(path);
    }

    /** Given a path name, return a display name */
    public static String displayNameFromPath(String path) {
        String displayName = path;
        displayName = displayName.replace("__", ": ");
        displayName = displayName.replace("_", " ");
        displayName = displayName.replace(".mid", "");
        return displayName;
    }

    /** Return the display name */
    public String toString() {
        return displayName;
    }

    /** Return true if this is a directory */
    public boolean isDirectory() {
        String path = uri.getPath();
        return path != null && path.endsWith("/");
    }

    /** Return the uri */
    public Uri getUri() {
        return uri;
    }

    /** Compare two files by their display name */
    public int compare(FileUri f1, FileUri f2) {
        return f1.displayName.compareToIgnoreCase(f2.displayName);
    }

    /** Return the file contents as a byte array.
     *  If any IO error occurs, return null.
     */
    public byte[] getData(Activity activity) {
        try {
            byte[] data;
            int totallen, len, offset;
        
            // First, determine the file length
            data = new byte[4096];
            InputStream file;
            String uriString = uri.toString();
            if (uriString.startsWith("file:///android_asset/")) {
                AssetManager asset = activity.getResources().getAssets();
                String filepath = uriString.replace("file:///android_asset/", "");
                file = asset.open(filepath);
            }
            else if (uriString.startsWith("content://")) {
                ContentResolver resolver = activity.getContentResolver(); 
                file = resolver.openInputStream(uri);
            }
            else {
                file = new FileInputStream(uri.getPath());
            }
            totallen = 0;
            len = file.read(data, 0, 4096);
            while (len > 0) {
                totallen += len;
                len = file.read(data, 0, 4096);
            }
            file.close();
        
            // Now read in the data
            offset = 0;
            data = new byte[totallen];

            if (uriString.startsWith("file:///android_asset/")) {
                AssetManager asset = activity.getResources().getAssets();
                String filepath = uriString.replace("file:///android_asset/", "");
                file = asset.open(filepath);
            }
            else if (uriString.startsWith("content://")) {
                ContentResolver resolver = activity.getContentResolver(); 
                file = resolver.openInputStream(uri);
            }
            else {
                file = new FileInputStream(uri.getPath());
            }
            while (offset < totallen) {
                len = file.read(data, offset, totallen - offset);
                if (len <= 0) {
                    throw new MidiFileException("Error reading midi file", offset);
                }
                offset += len;
            }
            return data;
        }
        catch (Exception e) {
            return null;
        }
    }

    /* Convert this URI to a JSON string */
    public JSONObject toJson() {
        try {
            JSONObject json = new JSONObject();
            json.put("uri", uri.toString());
            json.put("displayName", displayName);
            return json;
        }
        catch (JSONException e) {
            return null;
        }
        catch (NullPointerException e) {
            return null;
        }
    }

    /* Initialize this URI from a json string */
    public static FileUri fromJson(JSONObject obj, Activity activity) {
        try {
            String displayName = obj.optString("displayName", null);
            String uriString = obj.optString("uri", null);

            if (displayName == null || uriString == null) {
                return null;
            }
            Uri uri = Uri.parse(uriString);
            return new FileUri(uri, displayName);
        }
        catch (Exception e) {
            return null;
        }
    }
    
    public static boolean equalStrings(String s1, String s2) {
        if ((s1 == null && s2 != null) ||
            (s1 != null && s2 == null)) {
            return false;
        }
        if (s1 == null && s2 == null) {
            return true;
        }
        return s1.equals(s2);
    }
    
    /* Return true if the two FileUri json objects are equal */
    public static boolean equalJson(JSONObject obj1, JSONObject obj2) {
        String displayName1 = obj1.optString("displayName", null);
        String uriString1 = obj1.optString("uri", null);

        String displayName2 = obj2.optString("displayName", null);
        String uriString2 = obj2.optString("uri", null);

        return (equalStrings(displayName1, displayName2) &&
                equalStrings(uriString1, uriString2) );
    }
}

